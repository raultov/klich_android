package net.SoftForLife.Klich2.SMSmng;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused")
public class SMSManagement {
	private static final String KEY_ACTIVATION = "klich";
	
	
	SMSManagement() {
		
	}
	
	public static boolean manage(Context ctx, String msg) {
		if(msg.toLowerCase().startsWith(KEY_ACTIVATION)) {			
			// Delete SMS from inbox
			/*SMSMessage sms;
			Log.d(SMSReceiver.LOG_TAG, "Hemos recibido un klich");
			sms = SMSUtils.getSmsDetails(ctx, 0, false);
			
			if(sms != null) {
				Log.d(SMSReceiver.LOG_TAG, "Procedemos a borrar el SMS: "+sms.getMessageBody());
				SMSUtils.deleteMessage(ctx, sms.getMessageId(), sms.getThreadId(), SMSUtils.MESSAGE_TYPE_SMS);
			}
			*/
			// Start mechanisms of location
			
			return true;
		}
		else
			return false;
	}
}
