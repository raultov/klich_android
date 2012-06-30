package net.SoftForLife.Klich2.service;

//import net.SoftForLife.Klich2.service.UserService.DatabaseHelper;
import net.SoftForLife.Klich2.model.TuserMobile;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.math.BigInteger;
import java.security.*;
import java.sql.Date;

public class UserService {

	private SQLiteDatabase myDB;
	private DatabaseHelper myDBHelper;
	
	public UserService(Activity act) {
		myDBHelper = new DatabaseHelper(act);
	}
	
	public TuserMobile getUserRegistered() {
		TuserMobile user = null;
		openDB();

        Cursor mCursor = myDB.query(true, DatabaseHelper.MY_DATABASE_TABLE_USERS, new String[] {
        																						DatabaseHelper.USER_ID_USER, 
        																						DatabaseHelper.USER_EMAIL,
        																						DatabaseHelper.USER_NAME,
        																						DatabaseHelper.USER_PHONE,
        																						DatabaseHelper.USER_CREATION_DATE,
        																						DatabaseHelper.USER_PASSWORD
        																						}, 
        																						null, null, null, null, null, null);
        
        if (mCursor.getCount() > 0) {
        	mCursor.moveToFirst();
        	user = new TuserMobile();
        	user.getUserId().setUserId(mCursor.getInt(0));
        	user.setEmail(mCursor.getString(1));
        	user.setName(mCursor.getString(2));
        	user.setPhone(mCursor.getString(3));
        	String strDate = mCursor.getString(4);

        	try {
        		long time = Long.valueOf(strDate);
        		Date date = new Date(time);
        		user.setCreationDate(date);
        	} catch(NumberFormatException e) {
        		user.setCreationDate(null);
        	}
        	
        	user.setPassword(mCursor.getString(5));
        } 
        
        closeDB();
        
        return user;
	}
	
	public void insertUser(TuserMobile user) {
		openDB();
		
        ContentValues initialValues = new ContentValues();
        initialValues.put(DatabaseHelper.USER_ID_USER, user.getUserId().getUserId());
        initialValues.put(DatabaseHelper.USER_EMAIL, user.getEmail());
        initialValues.put(DatabaseHelper.USER_NAME, user.getName());
        initialValues.put(DatabaseHelper.USER_PHONE, user.getPhone());
        Long time = user.getCreationDate().getTime();
        initialValues.put(DatabaseHelper.USER_CREATION_DATE, time.toString());
        initialValues.put(DatabaseHelper.USER_PASSWORD, user.getPassword());
        myDB.insert(DatabaseHelper.MY_DATABASE_TABLE_USERS, null, initialValues);
        
        closeDB();
	}
	
	public boolean deleteUser(TuserMobile user) {
		openDB();
		
        boolean ret = myDB.delete(DatabaseHelper.MY_DATABASE_TABLE_USERS, 
				   		DatabaseHelper.USER_ID_USER + 
				   		"=" + user.getUserId(), null) > 0;		
		
		closeDB();
		
		return ret;
	}
	
	public static String encodeMD5(String str) {
		String result = null;
		
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			
			messageDigest.update(str.getBytes(), 0, str.length());
			result = new BigInteger(1, messageDigest.digest()).toString(16); 
			
			if (result.length() < 32) {
				result = "0" + result; 
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	    return result;
	}
	
	public void deleteUsersTable() {
		openDB();
		
		myDB.execSQL("DROP TABLE IF EXISTS " + DatabaseHelper.MY_DATABASE_TABLE_USERS);
		myDBHelper.onCreate(myDB);
		
		closeDB();
	}	
	
	   //---opens the database---
	private SQLiteDatabase openDB() throws SQLException {
		Log.d(DatabaseHelper.DB_TAG, "Opening DB");
		myDB = myDBHelper.getWritableDatabase();
		
		return myDB;
	}

	//---closes the database---    
	private void closeDB() {
		Log.d(DatabaseHelper.DB_TAG, "Closing DB");
		myDBHelper.close();
	}

	public SQLiteDatabase getMyDB() {
		return myDB;
	}

	public void setMyDB(SQLiteDatabase myDB) {
		this.myDB = myDB;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private final static String MY_DATABASE_NAME = "klichDB";
		private final static String MY_DATABASE_TABLE_USERS = "users";
		private static final int DATABASE_VERSION = 2;
  
		private static final String USER_ID_USER = "id";
		private static final String USER_EMAIL = "email";
		private static final String USER_NAME = "name";
		private static final String USER_PHONE = "phone";
		private static final String USER_CREATION_DATE = "creation_date";
		private static final String USER_PASSWORD = "password";
  
		private static final String DB_TAG = "DBAdapter";

		private static final String DATABASE_CREATE =
				"create table "+ MY_DATABASE_TABLE_USERS +" ("+USER_ID_USER+" INTEGER primary key, "
						+USER_EMAIL+" TEXT not null, "
						+USER_NAME+" TEXT, "
						+USER_PHONE+" TEXT, "
						+USER_CREATION_DATE+" TEXT, "
						+USER_PASSWORD+" TEXT not null);";

		DatabaseHelper(Context context) {
			super(context, MY_DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(DB_TAG, "Upgrading database from version " + oldVersion 
					+ " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + MY_DATABASE_TABLE_USERS);
			onCreate(db);
		}
	}  	
}
