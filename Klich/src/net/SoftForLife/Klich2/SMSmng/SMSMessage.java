package net.SoftForLife.Klich2.SMSmng;

import android.telephony.PhoneNumberUtils;

public class SMSMessage {
	  // Message types
	  public static final int MESSAGE_TYPE_SMS = 0;
	  public static final int MESSAGE_TYPE_MMS = 1;
	  public static final int MESSAGE_TYPE_MESSAGE = 2;

	  // Timestamp compare buffer for incoming messages
	  public static final int MESSAGE_COMPARE_TIME_BUFFER = 5000; // 5 seconds

	  // Main message object private vars
	  private String fromAddress = null;
	  private String messageBody = null;
	  private long timestamp = 0;
	  private int unreadCount = 0;
	  private long threadId = 0;
	  private String contactId = null;
	  private String contactName = null;
	  private long messageId = 0;
	  private boolean fromEmailGateway = false;

	  /**
	   * Construct SmsMmsMessage for getSmsDetails() - info fetched from the SMS
	   * database table
	   */
	  public SMSMessage(String _fromAddress, String _contactID, String _messageBody,
			  		    long _timestamp, long _threadId, int _unreadCount, long _messageId) {
		  
	    fromAddress = _fromAddress;
	    setMessageBody(_messageBody);
	    setTimestamp(_timestamp);


	    if (PhoneNumberUtils.isWellFormedSmsAddress(fromAddress)) {
	      setContactName(PhoneNumberUtils.formatNumber(fromAddress));
	      setFromEmailGateway(false);
	    } else {
	      setContactName(fromAddress);
	      setFromEmailGateway(true);
	    }

	    setUnreadCount(_unreadCount);
	    setThreadId(_threadId);
	    setMessageId(_messageId);


	  }

	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}

	public int getUnreadCount() {
		return unreadCount;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactName() {
		return contactName;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setFromEmailGateway(boolean fromEmailGateway) {
		this.fromEmailGateway = fromEmailGateway;
	}

	public boolean isFromEmailGateway() {
		return fromEmailGateway;
	}

}
