package net.SoftForLife.Klich2;

import net.SoftForLife.Klich2.model.GeopointMobile;
import net.softforlife.klich.enumeration.CLAVE_GEOPOINT;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.skyhookwireless.wps.*;

public class ShyHook {
    private XPS xps;
    private Handler mhandler;
    private MyLocationCallback callback;
    private WPSAuthentication auth;
    private boolean again;

    public ShyHook(Context act, String username, String realm, Handler handler) {
    	mhandler = handler;
    	auth = new WPSAuthentication(username, realm);
    	callback = new MyLocationCallback();
    	
    	xps = new XPS(act);
        xps.getPeriodicLocation(auth, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP, 5000, 0,callback); 
        again = false; 
    }
    
    public void searchAgain() {
    	xps.getPeriodicLocation(auth, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP, 5000, 0,callback);
    	again = false;
    	Log.d(Klich2.LOG_TAG, "Restarting search");
    }
    
    public boolean isStopped() {
    	return again;
    }
    
    public void abort() {
    	xps.abort();
    }
    
    protected void finalize () throws Throwable {
    	xps.abort();
    }

    
    
    /**
     * A single callback class that will be used to handle all notifications
     * sent by WPS to our app.
     */
    private class MyLocationCallback
        implements WPSPeriodicLocationCallback
    {
        public void done()
        {
            // tell the UI thread to re-enable the buttons
            //_handler.sendMessage(_handler.obtainMessage(DONE_MESSAGE));
        	//xps.getIPLocation(auth, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP, callback);
        	
        	Log.d(Klich2.LOG_TAG, "Method done reached");
        	again = true;
        }

		@Override
		public WPSContinuation handleError(WPSReturnCode arg0) {
			// TODO Auto-generated method stub
			return WPSContinuation.WPS_CONTINUE;
		}
		@Override
		public WPSContinuation handleWPSPeriodicLocation(WPSLocation arg0) {
			// TODO Auto-generated method stub
			
            //Route r = new Route(arg0.getLatitude(), arg0.getLongitude(), arg0.getHPE(), Route.TYPE_POINT.SHYHOOK);
			GeopointMobile gm = new GeopointMobile();
			gm.setLatitude(arg0.getLatitude());
			gm.setLongitude(arg0.getLongitude());
			gm.setAccuracy(new Float(arg0.getHPE()));
			gm.setTypeGeopoint(CLAVE_GEOPOINT.SHYHOOK.getObject());
            android.os.Message msg = new android.os.Message();
            msg.obj = (GeopointMobile) gm;
            msg.what = 0;
            mhandler.sendMessage(msg);  
            
            Log.d(Klich2.LOG_TAG, "Precision del ShyHook periodico es "+arg0.getHPE());  
            
			return WPSContinuation.WPS_CONTINUE;
		}
    }    
}
