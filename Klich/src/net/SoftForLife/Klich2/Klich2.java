package net.SoftForLife.Klich2;

//import net.SoftForLife.Klich2.Communication.Client;

import net.SoftForLife.Klich2.ServiceBackground.ServiceBG;
import net.SoftForLife.Klich2.identification.LoginActivity;
import net.SoftForLife.Klich2.identification.RegisterActivity;
import net.SoftForLife.Klich2.identification.RememberActivity;
import net.SoftForLife.Klich2.model.TuserMobile;
import net.SoftForLife.Klich2.service.UserService;
import net.softforlife.klich.model.Device;
import net.softforlife.klich.model.Track;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.code.apndroid.ApplicationConstants;

public class Klich2 extends Activity 
{
	public static final String LOG_TAG = "Klich";
    public static final int APN_STATE_REQUEST = 0;
    public static final int APN_CHANGE_REQUEST = 1;
    public static Device myDevice = null;
   // public static TuserMobile userDevice = null;
    public static Track currentTrack = null;
    public Activity activity;
    /*
    static {
    	userDevice = new TuserMobile();
    	userDevice.setActive(true);
    	userDevice.setCreationDate(new Date());
    	userDevice.setEmail("hola@adios.com");
    	userDevice.setIdx(0);
    	userDevice.setLogin("login");
    	userDevice.setName("Fulanito de tal");
    	userDevice.setPassword("password");
    	userDevice.setPhone("666666666");
    	userDevice.setRecovery("");
    	userDevice.setUserId(1);
    }
    */
    static {
    	myDevice = new Device();
    	myDevice.setBrand("Samsung");
    	myDevice.setDeviceId(1L);
    	myDevice.setModel("Galaxy S2");
    	myDevice.setType("Mobile");
    	myDevice.setUserId(null);
    }
    
    static {
    	currentTrack = new Track();
    	currentTrack.setTrackId(-1L);
    }

    //public static Context context;
    
    private static Activity act = null;

    private Button button_background;
    private Button button_track;
    
    private Button button_login;
    private Button button_register;
    private Button button_remember;
    
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        activity = this;
        UserService userService = new UserService(activity);
        
        //userService.deleteUsersTable();
        
        TuserMobile userDevice = userService.getUserRegistered();
        /*
        if (userDevice != null) {
        	Log.i(LOG_TAG, "El usuario está registrado");
        	
        	setContentView(R.layout.main); 
        	
            initialize();
             
            button_background = (Button) findViewById(R.id.Button01); 
            button_track = (Button) findViewById(R.id.Button02);
            
            if(ServiceBG.isRunning()) {
            	button_background.setText(new String("Parar").toCharArray(), 0, 5);
            	
            	if(ServiceBG.isTracking()) {
            		button_track.setText(new String("Parar").toCharArray(), 0, 5);
            	} 
            	else {
            		button_track.setText(new String("Empezar").toCharArray(), 0, 7);
            	}
            }
            else {
            	button_background.setText(new String("Empezar").toCharArray(), 0, 7);
            	button_track.setText(new String("Empezar").toCharArray(), 0, 7);
            }
            

            button_background.setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
            		if(ServiceBG.isRunning()) {
            			Intent svc = new Intent(act, ServiceBG.class);
               		 	stopService(svc);
            			
               		 button_background.setText(new String("Empezar").toCharArray(), 0, 7);
            		}
            		else {
            	        try {
            	        	ServiceBG.setMainActivity(act);
            	        	Intent svc = new Intent(act, ServiceBG.class);
            	        	startService(svc);
            	        }
            	        catch (Exception e) {
            	            Log.e(LOG_TAG, "Service creation problems", e);
            	         }    
               		 	
            	        button_background.setText(new String("Parar").toCharArray(), 0, 5);
            		}
            		
            		
            		//Client c = new Client(null , null);
            		//c.initClient();
            	}
            });  
            
            button_track.setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
            		if(ServiceBG.isTracking()) {
            			ServiceBG.stopTracking();
            			button_track.setText(new String("Empezar").toCharArray(), 0, 7);
            		}
            		else {
               		 	ServiceBG.startTracking();
               		 button_track.setText(new String("Parar").toCharArray(), 0, 5);
            		}
            		
            	}
            });          	
        	
        } else*/ {
        	// Presentamos la pantalla de login-registro-recordar clave
        	Log.i(LOG_TAG, "El usuario NO está registrado");
        	
        	setContentView(R.layout.index);
        	
        	button_login = (Button) findViewById(R.id.button_login);
        	button_register = (Button) findViewById(R.id.button_register);
        	button_remember = (Button) findViewById(R.id.button_remember);
        	
        	button_login.setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
            		Log.i(LOG_TAG, "Mostramos pantalla de logueo");
            		Intent intent = new Intent(activity, LoginActivity.class);
            		
                    // the results are called on widgetActivityCallback
                    activity.startActivityForResult(intent, 0x55555);
            	}
            });
        	
        	button_register.setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
            		Log.i(LOG_TAG, "Mostramos pantalla de registro");
            		Intent intent = new Intent(activity, RegisterActivity.class);
            		
            		activity.startActivityForResult(intent, 0x44444);
            	}
            }); 
        	
        	button_remember.setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
            		Log.i(LOG_TAG, "Mostramos pantalla de recordar clave");		
            		Intent intent = new Intent(activity, RememberActivity.class);
            		
            		activity.startActivityForResult(intent, 0x66666);
            	}
            }); 
        }
        
        
    }
	
	public void initialize() {
		act = this;
	}
	
	public void onPause() {
		super.onPause();
	}
	
	public void onResume() {
		super.onResume();
	}
	
	public void onStop() {
		super.onStop();
	}
	
	public void onDestroy() {
		super.onDestroy();
	}
	
	protected void onActivityResult(int requestedCode, int resultCode, Intent intent) {
		super.onActivityResult(requestedCode, resultCode, intent);

		switch (requestedCode) {
			case APN_CHANGE_REQUEST:
				if (resultCode == RESULT_OK && intent != null) {
					if (ApplicationConstants.APN_DROID_RESULT.equals(intent.getAction())) {
						ToggleInetMobile.setResultReceived(true);
						ToggleInetMobile.setResultValue(intent.getBooleanExtra(ApplicationConstants.RESPONSE_SWITCH_SUCCESS, true));
					}
				}
				break;
		}
	}
           
};

