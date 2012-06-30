package net.SoftForLife.Klich2.SMSmng;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SMSUtils {
	//Content URIs for SMS app, these may change in future SDK
    public static final Uri MMS_SMS_CONTENT_URI = Uri.parse("content://mms-sms/");
	
    public static final int READ_THREAD = 1;
    public static final int MESSAGE_TYPE_SMS = 1;
    public static final int MESSAGE_TYPE_MMS = 2;
    
    public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
    
    public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    public static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "inbox");
    
    
    public static final Uri CONVERSATION_CONTENT_URI =
        Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "conversations");

    
    public static final String LOG_TAG = "SMSUtils";
    


    /**
     * Tries to delete a message from the system database, given the thread id,
     * the timestamp of the message and the message type (sms/mms).
     */
    public static void deleteMessage(Context context, long messageId, long threadId, int messageType) {

      if (messageId > 0) {
        Log.d(LOG_TAG, "id of message to delete is " + messageId);

        // We need to mark this message read first to ensure the entire thread is marked as read
        setMessageRead(context, messageId, messageType);

        // Construct delete message uri
        Uri deleteUri;

        if (SMSUtils.MESSAGE_TYPE_MMS == messageType) {
          deleteUri = Uri.withAppendedPath(MMS_CONTENT_URI, String.valueOf(messageId));
        } else if (SMSUtils.MESSAGE_TYPE_SMS == messageType) {
          deleteUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
        } else {
          return;
        }
        int count = context.getContentResolver().delete(deleteUri, null, null);
        Log.d(LOG_TAG, "Messages deleted: " + count);
        if (count == 1) {
          //TODO: should only set the thread read if there are no more unread messages
          setThreadRead(context, threadId);
        }
      }
    }
    
    /**
     * Marks a specific message as read
     */
    public static void setMessageRead(Context context, long messageId, int messageType) {

     /* SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
      boolean markRead = myPrefs.getBoolean(
          context.getString(R.string.pref_markread_key),
          Boolean.valueOf(context.getString(R.string.pref_markread_default)));
      if (!markRead) return;
*/
      if (messageId > 0) {
        ContentValues values = new ContentValues(1);
        values.put("read", READ_THREAD);

        Uri messageUri;

        if (SMSUtils.MESSAGE_TYPE_MMS == messageType) {
          // Used to use URI of MMS_CONTENT_URI and it wasn't working, not sure why
          // this is diff to SMS
          messageUri = Uri.withAppendedPath(MMS_INBOX_CONTENT_URI, String.valueOf(messageId));
        } else if (SMSUtils.MESSAGE_TYPE_SMS == messageType) {
          messageUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
        } else {
          return;
        }

        Log.d(LOG_TAG, "messageUri for marking message read: " + messageUri.toString());

        ContentResolver cr = context.getContentResolver();

		int result;
        try {
          result = cr.update(messageUri, values, null, null);
        } catch (Exception e) {
          result = 0;
        }
       Log.d(LOG_TAG, String.format("message id = %s marked as read, result = %s", messageId, result ));
      }
    }


    
    /**
     * Marks a specific message thread as read - all messages in the thread will
     * be marked read
     */
    public static void setThreadRead(Context context, long threadId) {
          /*  SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean markRead = myPrefs.getBoolean(
                            context.getString(R.string.pref_markread_key),
                            Boolean.valueOf(context.getString(R.string.pref_markread_default)));
            
            if (!markRead) return;
            */
            if (threadId > 0) {             
                    ContentValues values = new ContentValues(1);
                    values.put("read", READ_THREAD);
                    
                    ContentResolver cr = context.getContentResolver(); 
                    @SuppressWarnings("unused")
					int result = 0;
                    try {           
                            result = cr.update(
                                    ContentUris.withAppendedId(CONVERSATION_CONTENT_URI, threadId),
                                    values, null, null);
                    } catch (Exception e) {
                            Log.d(LOG_TAG, "error marking thread read");
                    }
            }
    }
    
    /**
     * Tries to locate the message id (from the system database), given the message
     * thread id, the timestamp of the message and the type of message (sms/mms)
     */
    public static long findMessageId(Context context, long threadId, long _timestamp, int messageType) {
            long id = 0;
            long timestamp = _timestamp;
            if (threadId > 0) {
                    //Log.v("Trying to find message ID");
                    // It seems MMS timestamps are stored in a seconds, whereas SMS
                    // timestamps are in millis
                    if (SMSUtils.MESSAGE_TYPE_MMS == messageType) {
                            timestamp = _timestamp / 1000;
//                          Log.v("adjusted timestmap for MMS (" + _timestamp + " -> " + timestamp + ")");
                    }
                    
                    Cursor cursor = context.getContentResolver().query(
                                    ContentUris.withAppendedId(CONVERSATION_CONTENT_URI, threadId),
                                    new String[] { "_id", "date", "thread_id" },
                                    //"thread_id=" + threadId + " and " + "date=" + timestamp,
                                    "date=" + timestamp,
                                    null, "date desc");
                    
                    if (cursor != null) {
                            try {
                                    if (cursor.moveToFirst()) {
                                            id = cursor.getLong(0);
                                            //Log.v("Message id found = " + id);                                              
                                    }
                            } finally {
                                    cursor.close();
                            }
                    }                       
            }
            return id;
    }
    
    public static SMSMessage getSmsDetails(Context context,
            long ignoreThreadId, boolean unreadOnly) {
    
    String SMS_READ_COLUMN = "read";
    String WHERE_CONDITION = unreadOnly ? SMS_READ_COLUMN + " = 0" : null;
    String SORT_ORDER = "date DESC";
    int count = 0;
    
    //Log.v(WHERE_CONDITION);
    
    if (ignoreThreadId > 0) {
    	Log.d(LOG_TAG, "Ignoring sms threadId = " + ignoreThreadId);
    	WHERE_CONDITION += " AND thread_id != " + ignoreThreadId;
    }
    
    //WHERE_CONDITION += " AND date = " + String.valueOf(date);
    
    Log.d(LOG_TAG, "Condicion WHERE: " + WHERE_CONDITION);

    Cursor cursor = context.getContentResolver().query(
                    SMS_INBOX_CONTENT_URI,
          new String[] { "_id", "thread_id", "address", "person", "date", "body" },
                    WHERE_CONDITION,
                    null,
                    SORT_ORDER);

    if (cursor != null) {
            try {
                    count = cursor.getCount();
                    if (count > 0) {
                            cursor.moveToFirst();
                            
                            Log.d(LOG_TAG, "count: " + count);
                            
                            long messageId = cursor.getLong(0);
                            long threadId = cursor.getLong(1);
                            String address = cursor.getString(2);
                            long contactId = cursor.getLong(3);
                            String contactId_string = String.valueOf(contactId);
                            long timestamp = cursor.getLong(4);
                            
                            String body = cursor.getString(5);
                            
                            if (!unreadOnly) {
                                    count = 0;
                            }
                            
                            SMSMessage smsMessage = new SMSMessage(address, contactId_string, body, timestamp,
                                            					   threadId, count, messageId);
                            
                            int i=0;
                            cursor.moveToFirst();
                            while(i < cursor.getCount()) {
                            	Log.d(LOG_TAG, "Body de "+i+": " + cursor.getString(5));
                            	cursor.moveToNext();
                            	i++;
                            }                            
                            
                            return smsMessage;

                    }
            } finally {
                    cursor.close();
            }
    }               
    return null;
}


}
