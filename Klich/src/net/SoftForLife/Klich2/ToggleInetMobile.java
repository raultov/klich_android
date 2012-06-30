package net.SoftForLife.Klich2;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import com.google.code.apndroid.ApplicationConstants;


public class ToggleInetMobile {
    private Context act;
    
    private boolean started;
    private Thread t;
    
    public static boolean inet_result = false;
    public static boolean result_received = false;
    
	public ToggleInetMobile(Context act) {
        this.act = act;
		started=false;
	}
	
	public static synchronized void setResultValue(boolean state) {
		inet_result = state;
	}
	
	public static synchronized void setResultReceived(boolean state) {
		result_received = state;
	}

	public void start()  {
		inet_result = false;
	    result_received = false;

        t = new Thread() {

            public void run() {
        
        Looper.prepare();

		Intent intent = new Intent(ApplicationConstants.CHANGE_STATUS_REQUEST);
		intent.putExtra(ApplicationConstants.TARGET_APN_STATE, ApplicationConstants.State.ON);
		intent.putExtra(ApplicationConstants.SHOW_NOTIFICATION, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
/*
		act.startActivity(intent);
		//((Activity) act).startActivityForResult(intent, Klich.APN_CHANGE_REQUEST);
		
		boolean exit=false;
		while(!exit) {
			synchronized(this) {
				if(result_received) {
					exit = true;
					Log.d(Klich2.LOG_TAG, "Tenemos Internet abierto = "+inet_result);
				}
			}
		}
*/
        started = true;
            }
        };
          t.setPriority(Thread.MIN_PRIORITY);
          t.start();
	}
	
	public void stop() {
		inet_result = false;
	    result_received = false;
		
        t = new Thread() {

            public void run() {
        
        Looper.prepare();
		
		Intent intent = new Intent(ApplicationConstants.CHANGE_STATUS_REQUEST);
		intent.putExtra(ApplicationConstants.TARGET_APN_STATE, ApplicationConstants.State.OFF);
		intent.putExtra(ApplicationConstants.SHOW_NOTIFICATION, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		act.startActivity(intent);
		//((Activity) act).startActivityForResult(intent, Klich.APN_CHANGE_REQUEST);
		
		boolean exit=false;
		while(!exit) {
			synchronized(this) {
				if(result_received) {
					exit = true;
					Log.d(Klich2.LOG_TAG, "Tenemos Internet cerrado = " + inet_result);
				}
			}
		}

		started = false;
            }
        };
          t.setPriority(Thread.MIN_PRIORITY);
          t.start();
	}
	
	
	public boolean isStarted() {
		return started;
	}
	
}
