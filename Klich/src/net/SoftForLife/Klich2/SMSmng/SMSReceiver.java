package net.SoftForLife.Klich2.SMSmng;

//import android.app.Activity;
import net.SoftForLife.Klich2.ServiceBackground.ServiceBG;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver   {
	//private Activity act;
	public static final String LOG_TAG = "SMS_tag";
	static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

	public void onReceive(Context context, Intent intent) {
        //---get the SMSReceiver message passed in---
		if (intent.getAction().equals(ACTION)) {
			Log.d(SMSReceiver.LOG_TAG, "Hemos recibido un sms");
		
			Bundle bundle = intent.getExtras();        
			SmsMessage[] msgs = null;
			String str = "";            
			if (bundle != null)
			{
				//---retrieve the SMSReceiver message received---
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];            
				for (int i=0;i < msgs.length; i++){
					msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
					str += "SMSReceiver from " + msgs[i].getOriginatingAddress();                     
					str += ": ";
					str += msgs[i].getMessageBody().toString();

					str += " Numero de mensajes: " + msgs.length;
						str += "\n";        
						
						
					if(SMSManagement.manage(context, msgs[i].getMessageBody().toString())) {
						this.abortBroadcast();
						Log.d(SMSReceiver.LOG_TAG, "Hemos entrado porque la palabra es buena");
						
						if(ServiceBG.isTracking() == false)
							ServiceBG.startTracking();
						else
							ServiceBG.stopTracking();
					}
				}
            
				Log.d(SMSReceiver.LOG_TAG, str);
	
			}  		
		}	
	}
/*	
    //---sends an SMSReceiver message to another device---
    @SuppressWarnings("unused")
	private void sendSMS(String phoneNumber, String message)
    {        
       // PendingIntent pi = PendingIntent.getActivity(act, 0, new Intent(act, Klich.class), 0);                
       // SmsManager sms = SmsManager.getDefault();
       // sms.sendTextMessage(phoneNumber, null, message, pi, null);        
    }    
*/
	

}
