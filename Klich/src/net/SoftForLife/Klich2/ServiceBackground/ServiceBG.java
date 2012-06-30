package net.SoftForLife.Klich2.ServiceBackground;

import java.util.Timer;
import java.util.TimerTask;

import net.SoftForLife.Klich2.Road;
import net.SoftForLife.Klich2.ShyHook;
import net.SoftForLife.Klich2.ToggleInetMobile;
import net.SoftForLife.Klich2.model.GeopointMobile;
import net.softforlife.klich.enumeration.CLAVE_GEOPOINT;
//import net.SoftForLife.Klich2.Trilateration;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;

public class ServiceBG extends Service {
	private static final int INTERVAL = 1000; // 1 Second
	private static final int INTERVAL_GPS = 15000; // 15 Seconds
	private static final int DELAY_WIFI = 15000; // 15 Seconds
	private static final int INTERVAL_WIFI = 60000; // 5 Minutes
	private static final int TIMEOUT_WIFI_ENABLING = INTERVAL_WIFI / 5; // Timeout Wifi Enabling
	private static final int TIMEOUT_WIFI_CONNECTING = INTERVAL_WIFI / 5; // Timeout Wifi Connecting
	@SuppressWarnings("unused")
	private static Activity MAIN_ACTIVITY;
	
	private static Context act  = null;
	private static WifiManager wifiManager;

	
	private Timer timer = new Timer();
	private static Timer timer_GPS_Check;
	private static Timer timer_WIFI_Check;
	private static ToggleInetMobile toggleInetMobile;
	private static Road road;
	private static ShyHook sh;
	private static LocationListenerGPS locationListener_gps;
	private static LocationManager lm_gps;	
	private static LocationManager lm_net;
	private static LocationListenerNET locationListener_net;
	//private static Trilateration tc;	
	
	public static String LOG_TAG = "SERVICE";
	
	private static boolean isRunning;
	private static boolean isTracking;
	
	
	private static Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
        	Log.d(LOG_TAG, "El handler recibio un mesg con what = " + msg.what);
        	synchronized(road) {
        		switch(msg.what) {
        			case 0:
        				//Route r = (Route) msg.obj;
        				GeopointMobile gm = (GeopointMobile) msg.obj;
        				Log.d(LOG_TAG, "Entraremos en addPoint con acc = " + gm.getAccuracy());
        				road.addPointToRoute(gm.getLatitude(), gm.getLongitude(), gm.getAccuracy(), 
        									gm.getTypeGeopoint());
        				break;
        			case 1:
        				road.restart();
        				break;
        			default:
        				Log.d(LOG_TAG, "Algo muy jodido esta pasando");
        		}
        	}
        };
	};
	
    public class LocalBinder extends Binder {
    	ServiceBG getService() {
            return ServiceBG.this;
        }
    }	

	public void onCreate() {
		super.onCreate();

		initialize();
        road = new Road(this, "Tracker","Service that pretends to track mobile movement");
        toggleInetMobile = new ToggleInetMobile(this);	
        
        wifiManager=(WifiManager)act.getSystemService(Context.WIFI_SERVICE);
        
        timer_GPS_Check = new Timer();
        timer_WIFI_Check = new Timer();
        
        isRunning = true;

		//startservice();
	}
	
	public static void startTracking() {
		
//		ConnectivityManager cm = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
//		cm.setNetworkPreference(ConnectivityManager.TYPE_WIFI);
		
		toggleInetMobile.start();
		
		// Start ShyHook tracker method
		sh = new ShyHook(act, "raulto", "raulto", mHandler);
		
		//---use the LocationManager class to obtain GPS locations---
        lm_gps = (LocationManager) 
        act.getSystemService(Context.LOCATION_SERVICE);   
        locationListener_gps = new LocationListenerGPS();
    
        lm_gps.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 
            0, 
            2.0f, 
            locationListener_gps); 
        
	      //---use the LocationManager class to obtain Network locations---
        lm_net = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);  
        locationListener_net = new LocationListenerNET();
        
        lm_net.requestLocationUpdates(
            	LocationManager.NETWORK_PROVIDER, 
            	0, 
            	2.0f, 
            	locationListener_net);   
         
        //tc = new Trilateration(act, mHandler); 
        Road.enableSendMode();    
        
        isTracking = true;
        
        // Check wether GPS is connected otherwise it shows a dialog in order to activate it from time to time
        timer_GPS_Check.scheduleAtFixedRate( new TimerTask() {

    		public void run() {
    			if (!lm_gps.isProviderEnabled(LocationManager.GPS_PROVIDER)){  
    				Log.d(LOG_TAG,"Vamos a mostrar el dialogo de GPS");
    				
    				Intent gpsOptionsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
    				gpsOptionsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				act.startActivity(gpsOptionsIntent); 
    			}
    		}

    		}, INTERVAL_GPS, INTERVAL_GPS);
        
        // Check wether we are connected to any WIFI network otherwise we turn off WIFI until next checking
        timer_WIFI_Check.scheduleAtFixedRate( new TimerTask() {

    		public void run() {
    			wifiManager.setWifiEnabled(true);
    			Time time = new Time();
    			time.setToNow();
    			long before = time.toMillis(false);
    			
    			Log.d(LOG_TAG,"Estado wifi: "+wifiManager.getWifiState());
    			
    			boolean enabled = true;
    			while(wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
    				time.setToNow();
    				long now = time.toMillis(false);
    				
    				if((before + TIMEOUT_WIFI_ENABLING) < now) {//Timeout to exit in case of wifi still is not enabled
    					Log.d(LOG_TAG, "Se cumplio el timeout de Wifi para puesta en marcha");
    					enabled = false;
    					break;
    				}
    			}
    			
    			Log.d(LOG_TAG,"Estado wifi tras el bucle de conexion: "+wifiManager.getWifiState());
    			
    			if(enabled) {
    				wifiManager.startScan();
    				wifiManager.reassociate();
    			}
    			
    			time.setToNow();
    			before = time.toMillis(false);
    			while((wifiManager.getConnectionInfo().getNetworkId() == -1) && enabled) {
    				time.setToNow();
    				long now = time.toMillis(false);
    				
    				if((before + TIMEOUT_WIFI_CONNECTING) < now) {//Timeout to exit in case of wifi still is not enabled
    					Log.d(LOG_TAG, "Se cumplio el timeout de Wifi para conexion");
    					break;
    				}
    			}    			
		
    			
    			if(wifiManager.getConnectionInfo().getNetworkId() == -1)
    				wifiManager.setWifiEnabled(false);

    			Log.d(LOG_TAG, "ID de la Wifi: "+wifiManager.getConnectionInfo().getNetworkId());
    		}

    		}, DELAY_WIFI, INTERVAL_WIFI);        
	}
	
	public static void stopTracking() {
		if(timer_GPS_Check != null) {
			timer_GPS_Check.cancel();
		}	
		
		if(timer_WIFI_Check != null) {
			timer_WIFI_Check.cancel();
		}			
		
		Road.disableSendMode();
		
		lm_gps.removeUpdates(locationListener_gps);
		lm_gps = null;	
		
		lm_net.removeUpdates(locationListener_net);
		lm_net = null;		
		
		sh.abort();
		sh = null;
		
		//tc.stop();
		//tc = null;		

		//Route r = new Route(0.0, 0.0, 0.0f, Route.TYPE_POINT.GPS);
		GeopointMobile gm = new GeopointMobile();
		gm.setLatitude(0.0);
		gm.setLongitude(0.0);
		gm.setAccuracy(0.0f);
		gm.setTypeGeopoint(CLAVE_GEOPOINT.GPS.getObject());
		android.os.Message msg = new android.os.Message();
        msg.obj = (GeopointMobile) gm;
        msg.what = 1; //restart Road
        mHandler.sendMessage(msg);
		
		//Disable WIFI
		wifiManager.setWifiEnabled(false);
		
		toggleInetMobile.stop();	
		isTracking = false;
	}
	
	@SuppressWarnings("unused")
	private void startservice() {

		timer.scheduleAtFixedRate( new TimerTask() {

		public void run() {
			Log.d("SERVICE", "Hola");
		}

		}, 0, INTERVAL);
		
		Log.d("SERVICE", "Service started!!!");
	}
	

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void onDestroy() {
		super.onDestroy();
		if(timer != null) {
			timer.cancel();
		}
		
		//stopTracking();
		isRunning = false;
	}
	
	public void initialize() {
		act = this;
	}	
	
	public static void setMainActivity(Activity activity) {
		  MAIN_ACTIVITY = activity;
	}
	

	public static boolean isRunning() {
		return isRunning;
	}
	
	public static boolean isTracking() {
		return isTracking;
	}


	public static class LocationListenerGPS implements LocationListener 
    {
		@Override
        public void onLocationChanged(Location loc) {
            if (loc != null) {                      
                //Route r = new Route(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), Route.TYPE_POINT.GPS);
            	GeopointMobile gm = new GeopointMobile();
            	gm.setLatitude(loc.getLatitude());
            	gm.setLongitude(loc.getLongitude());
            	gm.setAccuracy(loc.getAccuracy());
            	gm.setTypeGeopoint(CLAVE_GEOPOINT.GPS.getObject());
                android.os.Message msg = new android.os.Message();
                msg.obj = (GeopointMobile) gm;
                msg.what = 0;
                mHandler.sendMessage(msg);           
                
                Log.d("RelocationGPS", "Precision del GPS es "+loc.getAccuracy());        
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        
        	if(status != android.location.LocationProvider.AVAILABLE) {
		
        	}
        	else {
        		//mThread.stop();
        	}
        }        
        
    }  	
    
    
    public static class LocationListenerNET implements LocationListener 
    {
		@Override
        public void onLocationChanged(Location loc) {
            if (loc != null) {                              
                //Route r = new Route(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), Route.TYPE_POINT.NETWORK);
            	GeopointMobile gm = new GeopointMobile();
            	gm.setLatitude(loc.getLatitude());
            	gm.setLongitude(loc.getLongitude());
            	gm.setAccuracy(loc.getAccuracy());
            	gm.setTypeGeopoint(CLAVE_GEOPOINT.NETWORK.getObject());
                android.os.Message msg = new android.os.Message();
                msg.obj = (GeopointMobile) gm;
                msg.what = 0;
                mHandler.sendMessage(msg);    
                
                Log.d("RelocationNET", "Precision del Network Service es "+loc.getAccuracy());  
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }        
        
    }     

};

