package net.SoftForLife.Klich2;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import net.SoftForLife.Klich2.model.GeopointMobile;
import net.softforlife.klich.enumeration.CLAVE_GEOPOINT;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Handler;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class Trilateration  {
	private Thread t;
	private TelephonyManager  tm;
	private CallStateListener csl;
	private List <NeighboringCellInfo> lnci;
	private int lac;
	private ArrayList <CellLoc> arrayCellLoc;
	private ArrayList <Circle> circles;
	private Handler mhandler;
	private DatabaseHelper myDBHelper;
	private SQLiteDatabase myDB;
	private Cursor cur;
	private boolean continue_thread;
	
	public static final double E = 8.1819190842622e-2; // Elipsoide de WGS84
	public static final double E_SQR = 6.69437999014e-3; // Elipsoide de WGS84
	public static final double E2 = 8.2094437949696e-2; // Elipsoide de WGS84
	public static final double E2_SQR = 6.73949674228e-3; // Elipsoide de WGS84
	//public static final double E2_SQR = 0.00676817; // Elipsoide de Hayford 
	public static final double C = 6399593.6258;  // Elipsoide de WGS84
	//public static final double C = 6399936.608; // Elipsoide de Hayford
	public static final double SQRT_3 = 1.7320508075688772935274463415059;
	
	public static final float FACTOR_MUL = 1.2f;	
	
	public Trilateration(Context act, Handler handler) {
        tm  = (TelephonyManager) act.getSystemService(Context.TELEPHONY_SERVICE);    
        assert(tm != null);
        
        continue_thread = true;
        
        csl = new CallStateListener();
        tm.listen(csl, PhoneStateListener.LISTEN_CELL_LOCATION);
        circles = new ArrayList<Circle>();
        arrayCellLoc = new ArrayList<CellLoc>();
        mhandler = handler;
        myDBHelper = new DatabaseHelper(act);

        t = new Thread() {

            public void run() {
                while (continue_thread) {
                	if(csl.isLocationObtained() && (csl.getLac() != -1)) {
                		
                		openDB();
                		lac = csl.getLac();
                		lnci = tm.getNeighboringCellInfo();
                		
                		Log.d(Klich2.LOG_TAG, "Principal: Lac = "+lac+" Cid = "+csl.getCid());
                		
                		if(csl.isStrengthChanged()) {
                			GSMLocation gsmL = null;
                			try {
                				cur = getTowerFromDB(csl.getCid(), lac);
                				if(cur.getCount() <= 0) {
                					gsmL = new GSMLocation(csl.getCid(), lac);
                					insertTowerIntoDB(csl.getCid(), lac, gsmL.getLatitude(), gsmL.getLongitude());
                					arrayCellLoc.add(new CellLoc(gsmL.getLatitude(), gsmL.getLongitude(), csl.getAsu()));
                					Log.d(Klich2.LOG_TAG, "Principal tower to DB: Lat: "+arrayCellLoc.get(0).getLatitude()+
       					             " Long: "+arrayCellLoc.get(0).getLongitude()+
       					             " asu: "+arrayCellLoc.get(0).getAsu());   
                				}
                				else {
                					arrayCellLoc.add(new CellLoc(cur.getDouble(2), cur.getDouble(3), csl.getAsu()));
                					Log.d(Klich2.LOG_TAG, "Principal tower from DB: Lat: "+arrayCellLoc.get(0).getLatitude()+
              					             " Long: "+arrayCellLoc.get(0).getLongitude()+
              					             " asu: "+arrayCellLoc.get(0).getAsu());                  					
                				}
                			} catch (Exception e) {
                				// TODO Auto-generated catch block
                				e.printStackTrace();
                			} 
                		}
                		
                		for(int i=0;i < lnci.size();i++) {
                			NeighboringCellInfo nci = (NeighboringCellInfo) lnci.get(i);
                			GSMLocation gsmL = null;
                			
                			try {
                				cur = getTowerFromDB(nci.getCid(), lac);
                				if(cur.getCount() <= 0) {
                					Log.d(Klich2.LOG_TAG, "Tower to DB"+i+": Lac = "+lac+" Cid = "+nci.getCid());
                					gsmL = new GSMLocation(nci.getCid(), lac);
                					insertTowerIntoDB(nci.getCid(), lac, gsmL.getLatitude(), gsmL.getLongitude());
                					arrayCellLoc.add(new CellLoc(gsmL.getLatitude(), gsmL.getLongitude(), nci.getRssi()));
                				}
                				else {
                					arrayCellLoc.add(new CellLoc(cur.getDouble(2), cur.getDouble(3), nci.getRssi()));
                					Log.d(Klich2.LOG_TAG, "Tower from DB "+i+": Lac = "+lac+" Cid = "+nci.getCid());
                				}
                			} catch (Exception e) {
                				// TODO Auto-generated catch block
                				e.printStackTrace();
                			} 

                		}
              
                		closeDB();
                		
                		findRadiusTowers();
                		findDistanceFromTowersToUs();
                		Log.d(Klich2.LOG_TAG, "Numero de towers antes: "+arrayCellLoc.size());
                		supressTooFarTowers();
                		Log.d(Klich2.LOG_TAG, "Numero de towers despues: "+arrayCellLoc.size());
                		
                		// Create circles associated to every towers
                		for(int i=0;i < arrayCellLoc.size();i++) {
                			circles.add(new Circle(arrayCellLoc.get(i)));
                		}
          
                		
                		boolean isActive = false;
                		Circle ci, cj;
                		
                		do {
                			isActive = false;
                			for(int i=0;i < circles.size();i++) {
                				ci = circles.get(i);
                				for(int j=i+1;j < circles.size();j++) {
                					cj = circles.get(j);
                					double dist = Circle.calculateDBTS(ci, cj);
                					double sum = ci.getD() + cj.getD();
                					Log.d(Klich2.LOG_TAG, "dist="+dist+", sum="+sum);
                					if((Circle.calculateDBTS(ci, cj) < (ci.getD() + cj.getD())) &&
                					   (Circle.calculateDBTS(ci, cj) > Math.abs(ci.getD() - cj.getD()))) {
                						
                						Log.d(Klich2.LOG_TAG, "circulo nuevo entre"+convertFromUTMToGeo(ci).getLongitude()
                								+" y "+convertFromUTMToGeo(cj).getLongitude());
                						
                						Circle c_inter = Circle.calculateIntersectingCircle(ci, cj);
                						
                						Log.d(Klich2.LOG_TAG, "Es el: "+convertFromUTMToGeo(c_inter).getLongitude());
                						
                						//circles2.add(c_inter);
                						circles.add(c_inter);
                						circles.remove(ci);
                						circles.remove(cj);
                						isActive = true;
                					}
                					
                				}
                			}
                			
                		} while(isActive);
                		
                		arrayCellLoc.clear();
                		
                		float min_radius = 1000000.0f;
                		int index_tower=0;
                		for(int i=0;i < circles.size();i++) {
                			arrayCellLoc.add(convertFromUTMToGeo(circles.get(i)));
                			arrayCellLoc.get(i).setRadius(arrayCellLoc.get(i).getRadius() * FACTOR_MUL);
                			
                			Log.d(Klich2.LOG_TAG, "Lat: "+arrayCellLoc.get(i).getLatitude()+
   					             " Long: "+arrayCellLoc.get(i).getLongitude()+
   					             " asu: "+arrayCellLoc.get(i).getAsu()+
   					             " radius: "+arrayCellLoc.get(i).getRadius()+
   					             " dist: "+arrayCellLoc.get(i).getDistanceToUs());                			
                			
                			if((arrayCellLoc.get(i).getRadius() < min_radius) && 
                			   (arrayCellLoc.get(i).getRadius() > 0.0f)) {
                				min_radius = arrayCellLoc.get(i).getRadius();
                				index_tower = i;
                			}
                		}
                		
                		if(circles.size() > 0) {
                			if(arrayCellLoc.get(index_tower).getRadius() > 0.0f) {
                				GeopointMobile gm = new GeopointMobile();
                				gm.setLatitude(arrayCellLoc.get(index_tower).getLatitude());
                				gm.setLongitude(arrayCellLoc.get(index_tower).getLongitude());
                				gm.setAccuracy(arrayCellLoc.get(index_tower).getRadius());
                				gm.setTypeGeopoint(CLAVE_GEOPOINT.ANTENNAS.getObject());
                				/*r = new Route(arrayCellLoc.get(index_tower).getLatitude(), 
                						arrayCellLoc.get(index_tower).getLongitude(), 
                						arrayCellLoc.get(index_tower).getRadius(), 
                						Route.TYPE_POINT.ANTENNAS);
                			*/
                				android.os.Message msg = new android.os.Message();
                				msg.obj = (GeopointMobile) gm;
                				msg.what = 0;
                				mhandler.sendMessage(msg); 
                			}
                		}
                		
                		arrayCellLoc.clear();
                		circles.clear();
                		//Send update to the main thread

                		try {
                			sleep(5000);
                		} catch (InterruptedException e) {
                			// TODO Auto-generated catch block
                			e.printStackTrace();
                		}
                	}
                }

            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();        
	}
	
    protected void finalize () throws Throwable {
    	continue_thread = false;
    }	
    
    public void stop() {
    	continue_thread = false;
    }
	
	public static Circle convertFromGeoToUTM(CellLoc orig) {
		Circle c = new Circle(0.0, 0.0, orig.getDistanceToUs());
		
		double lon_rad, lat_rad, lambda0, lambdaInc, huso;
		double A, simb1, simb2, simb3, simb4, A1, A2, J2, J4, J6;
		double alpha, beta, gamma, betaSubTita;
		double X, Y;
		
		lon_rad = (orig.getLongitude()*Math.PI) / 180.0;
		lat_rad = (orig.getLatitude()*Math.PI) / 180.0;
	
		huso = (int) ((orig.getLongitude() / 6.0) + 31.0);
		c.setHuso((int) huso);
		
		lambda0 = (huso*6) - 183;
		lambdaInc = (double) (lon_rad -((lambda0*Math.PI)/180.0));
		
		// Ecuaciones de Coticchia-Surace
		A = Math.cos(lat_rad) * Math.sin(lambdaInc);
		simb1 = 0.5 * Math.log(Math.abs((1+A)/(1-A)));
		simb2 = Math.atan(Math.tan(lat_rad)/Math.cos(lambdaInc)) - lat_rad;
		simb3 = (C/Math.sqrt(1 + (E2_SQR*Math.cos(lat_rad)*Math.cos(lat_rad))))*0.9996;
		simb4 = (E2_SQR/2.0)*simb1*simb1*Math.cos(lat_rad)*Math.cos(lat_rad);
		A1 = Math.sin(2*lat_rad);
		A2 = A1*Math.cos(lat_rad)*Math.cos(lat_rad);
		J2 = lat_rad + (A1/2.0);
		J4 = ((3.0*J2)+A2)/4.0;
		J6 = ((5.0*J4)+(A2*Math.cos(lat_rad)*Math.cos(lat_rad)))/3.0;
		alpha = 0.75*E2_SQR;
		beta = (5.0/3.0)*alpha*alpha;
		gamma = (35.0/27.0)*alpha*alpha*alpha;
		betaSubTita = 0.9996*C*(lat_rad - (alpha*J2) + (beta*J4) - (gamma*J6));
		
		//Calculo Final de Coordenadas
		X = (simb1*simb3*(1.0 + (simb4/3.0))) + 500000.0;
		Y = (simb2*simb3*(1.0 + simb4)) + betaSubTita;
		
		if(orig.getLatitude() < 0.0) {
			Y = Y + 10000000.0;
			c.setHS(true);
		}
		else
			c.setHS(false);
		
		c.setX(X);
		c.setY(Y);
/*
		Log.d(Klich.LOG_TAG, "PARAMETROS GEO a UTM:");
		Log.d(Klich.LOG_TAG,"lon_rad: "+lon_rad+" lat_rad: "+lat_rad+" lambda0: "+lambda0+" lambdaInc: "+lambdaInc);
		Log.d(Klich.LOG_TAG,"A: "+A+" simb1: "+simb1+" simb2: "+simb2+" simb3: "+simb3+" simb4: "+simb4);
		Log.d(Klich.LOG_TAG,"A1: "+A1+" A2: "+A2+" J2: "+J2+" J4: "+J4+" J6: "+J6);
		Log.d(Klich.LOG_TAG,"alpha: "+alpha+" beta: "+beta+" gamma: "+gamma+" betaSubTita: "+betaSubTita);
		
		Log.d(Klich.LOG_TAG, "X: "+X+" Y: "+Y);
*/
		return c;
	}
	
	public static CellLoc convertFromUTMToGeo(Circle orig) {
		CellLoc c = null;
		
		double X, Y;
		double lambda0, simb1, simb2, a, A1, A2, J2, J4, J6;
		double alpha, beta, gamma, betaSubTita, b, simb3, simb4, simb5;
		double senHsimb4, lambdaInc, simb6;
		double longitude, latitude;
		
		X = orig.getX() - 500000.0;
		if(orig.isHS()) {
			Y = orig.getY() - 10000000.0;
		}
		else
			Y = orig.getY();
		
		lambda0 = (orig.getHuso()*6.0) - 183.0;
		
		//Ecuaciones de Coticchia-Surace para el Problema Inverso
		simb1 = Y/(6366197.724*0.9996);
		simb2 = (C/Math.sqrt(1 + (E2_SQR*Math.cos(simb1)*Math.cos(simb1))))*0.9996;
		a = X/simb2;
		A1 = Math.sin(2.0*simb1);
		A2 = A1*Math.cos(simb1)*Math.cos(simb1);
		J2 = simb1 + (A1/2.0);
		J4 = ((3.0*J2) + A2)/4.0;
		J6 = ((5.0*J4) + (A2*Math.cos(simb1)*Math.cos(simb1)))/3.0;
		alpha = 0.75*E2_SQR;
		beta = (5.0/3.0)*alpha*alpha;
		gamma = (35.0/27.0)*alpha*alpha*alpha;
		betaSubTita = 0.9996*C*(simb1 - (alpha*J2) + (beta*J4) - (gamma*J6));
		b = (Y - betaSubTita)/simb2;
		simb3 = ((E2_SQR*a*a)/2.0)*Math.cos(simb1)*Math.cos(simb1);
		simb4 = a * Math.abs(1 - (simb3/3.0));
		simb5 = (b * (1 - simb3)) + simb1;
		senHsimb4 = (Math.pow(Math.E, simb4) - Math.pow(Math.E, -simb4))/2.0;
		lambdaInc = Math.atan(senHsimb4/Math.cos(simb5));
		simb6 = Math.atan(Math.cos(lambdaInc)*Math.tan(simb5));
		
		c = new CellLoc();
		
		longitude = ((lambdaInc/Math.PI)*180.0) + lambda0;
		c.setLongitude(longitude);
		
		latitude = simb1 + (Math.abs(1 + (E2_SQR*Math.cos(simb1)*Math.cos(simb1)) - (0.75*E2_SQR*Math.sin(simb1)*
				   Math.cos(simb1)*(simb6-simb1))) * (simb6-simb1));
		
		latitude = (latitude/Math.PI) * 180.0;
		
		c.setLatitude(latitude);
		c.setRadius(orig.getD());
			
		/*
		Log.d(Klich.LOG_TAG, "PARAMETROS UTM a GEO:");
		Log.d(Klich.LOG_TAG,"X: "+X+" Y: "+Y+ " Huso: "+orig.getHuso()+" lambda0: "+lambda0+" simb1: "+simb1+" simb2: "+simb2);
		Log.d(Klich.LOG_TAG,"a: "+a+" A1: "+A1+" A2: "+A2+" J2: "+J2+" J4: "+J4+" J6: "+J6);
		Log.d(Klich.LOG_TAG,"alpha: "+alpha+" beta: "+beta+" gamma: "+gamma+" betaSubTita: "+betaSubTita+" b: "+b);
		Log.d(Klich.LOG_TAG,"simb3: "+simb3+" simb4: "+simb4+" simb5: "+simb5+" senHsimb4: "+senHsimb4);
		Log.d(Klich.LOG_TAG,"lambdaInc: "+lambdaInc+" simb6: "+simb6+" longitude: "+longitude+" latitude: "+latitude);	
			*/
		
		return c;
	}
	
	private void findRadiusTowers() {
		float[] result = new float[1];
		CellLoc ti, tj;
		
		for(int i=0;i < arrayCellLoc.size();i++) {
			ti = arrayCellLoc.get(i);
			ti.setRadius(Float.MAX_VALUE);

			for(int j=0;j < arrayCellLoc.size();j++) {
				if(i != j) {
					tj = arrayCellLoc.get(j);
					Location.distanceBetween(ti.getLatitude(), ti.getLongitude(), 
											 tj.getLatitude(), tj.getLongitude(), 
											 result);
				
					if(result[0] < ti.getRadius()) 
						ti.setRadius((float) result[0]);
				}
			}
			ti.setRadius((float)(ti.getRadius()*SQRT_3));
		}
	}
	
	private void findDistanceFromTowersToUs() {
		CellLoc t;
		for(int i=0;i < arrayCellLoc.size();i++) {
			t = arrayCellLoc.get(i);
			
			float stretch = t.getRadius() / 32.0f;
			t.setDistanceToUs(stretch*(32-t.getAsu()));
		}
	}
	
	private void supressTooFarTowers() {
		CellLoc c;
		GroupTowers g;
		float threshold = 2000.0f;
		ArrayList <GroupTowers> gt = new ArrayList <GroupTowers>();
		ArrayList <CellLoc> arrayCellLoc2;
		
		if(arrayCellLoc.size() > 2) {
			GroupTowers initial = new GroupTowers();
			initial.addTower(0, arrayCellLoc.get(0).getLatitude(), arrayCellLoc.get(0).getLongitude());
			gt.add(initial);
			
			for(int i=1;i < arrayCellLoc.size();i++) {
				c = arrayCellLoc.get(i);
			
				int index_group=-1;
				float min_distance = Float.MAX_VALUE;
				for(int j=0;j < gt.size();j++) {
					g = gt.get(j);
					float [] result = new float[1];
					Location.distanceBetween(g.getLatitudeAverage(), g.getLongitudeAverage(), 
							 c.getLatitude(), c.getLongitude(), 
							 result);
					
					if(result[0] < threshold) {
						if(result[0] < min_distance) {
							min_distance = result[0];
							index_group = j;
						}
					}
				}
				
				if(index_group == -1) {
					GroupTowers g_new = new GroupTowers();
					g_new.addTower(i, c.getLatitude(), c.getLongitude());
					gt.add(g_new);
				}
				else {
					gt.get(index_group).addTower(i, c.getLatitude(), c.getLongitude());
				}
			}
			
			int max=0;
			int index=0;
			for(int i=0;i < gt.size();i++) { // Obtain the bigger group of towers
				if(gt.get(i).numTowers() > max) {
					max = gt.get(i).numTowers();
					index = i;
				}
			}
			
			arrayCellLoc2 = new ArrayList<CellLoc>();
			
			for(int i=0;i < max;i++) {
				arrayCellLoc2.add(arrayCellLoc.get((int) gt.get(index).getTower(i)));
			}
			
			arrayCellLoc.clear();
			arrayCellLoc = arrayCellLoc2;
		}
	}
	
	   //---opens the database---
    public void openDB() throws SQLException 
    {
    	Log.d(DatabaseHelper.DB_TAG, "Opening DB");
    	myDB = myDBHelper.getWritableDatabase();
    }
    
    //---closes the database---    
    public void closeDB() 
    {
    	Log.d(DatabaseHelper.DB_TAG, "Closing DB");
    	myDBHelper.close();
    }

    //---insert a title into the database---
    public long insertTowerIntoDB(int cid, int lac, double latitude, double longitude) 
    {
    	Log.d(DatabaseHelper.DB_TAG, "Inserting into DB values cid="+cid+", lac="+lac+
    			                     ", latitude="+latitude+", longitude="+longitude);
        ContentValues initialValues = new ContentValues();
        initialValues.put(DatabaseHelper.CID, cid);
        initialValues.put(DatabaseHelper.LAC, lac);
        initialValues.put(DatabaseHelper.LATITUDE, latitude);
        initialValues.put(DatabaseHelper.LONGITUDE, longitude);
        return myDB.insert(DatabaseHelper.MY_DATABASE_TABLE, null, initialValues);
    }
    
    //---deletes a particular title---
    public boolean deleteTowerFromDB(int cid, int lac) 
    {
    	Log.d(DatabaseHelper.DB_TAG, "Deleting from DB values cid="+cid+", lac="+lac);
        return myDB.delete(DatabaseHelper.MY_DATABASE_TABLE, 
        				   DatabaseHelper.CID + 
        				   "=" + cid + " and " + DatabaseHelper.LAC +"=" + lac,
        				   null) > 0;
    }
    

    //---retrieves all the titles---
    public Cursor getAllTowersFromDB() 
    {
    	Log.d(DatabaseHelper.DB_TAG, "Getting all values from DB");
        return myDB.query(DatabaseHelper.MY_DATABASE_TABLE, new String[] {
        		DatabaseHelper.CID, 
        		DatabaseHelper.LAC,
        		DatabaseHelper.LATITUDE,
        		DatabaseHelper.LONGITUDE}, 
                null, 
                null, 
                null, 
                null, 
                null);
    }

    //---retrieves a particular title---
    public Cursor getTowerFromDB(int cid, int lac) throws SQLException 
    {
    	Log.d(DatabaseHelper.DB_TAG, "Getting from DB values cid="+cid+", lac="+lac);
        Cursor mCursor =
                myDB.query(true, DatabaseHelper.MY_DATABASE_TABLE, new String[] {
                		DatabaseHelper.CID, 
                		DatabaseHelper.LAC,
                		DatabaseHelper.LATITUDE,
                		DatabaseHelper.LONGITUDE
                		}, 
                		DatabaseHelper.CID + "=" + cid + " and " + DatabaseHelper.LAC + "=" + lac, 
                		null,
                		null, 
                		null, 
                		null, 
                		null);
        
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

	
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        private final static String MY_DATABASE_NAME = "klichDB";
        private final static String MY_DATABASE_TABLE = "t_Towers";
        private static final int DATABASE_VERSION = 1;
        private static final String CID = "_cid";
        private static final String LAC = "_lac";
        private static final String LATITUDE = "latitude";
        private static final String LONGITUDE = "longitude";
        private static final String DB_TAG = "DBAdapter";

        private static final String DATABASE_CREATE =
            "create table "+MY_DATABASE_TABLE+" ("+CID+" INTEGER primary key, "
            +LAC+" INTEGER not null, "
            + LATITUDE+" DOUBLE not null, "
            + LONGITUDE+" DOUBLE not null);";

        DatabaseHelper(Context context) 
        {
            super(context, MY_DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
                              int newVersion) 
        {
            Log.d(DB_TAG, "Upgrading database from version " + oldVersion 
                  + " to "
                  + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS "+MY_DATABASE_TABLE);
            onCreate(db);
        }
    }  
	
	private static class GroupTowers{
		Vector <Integer> group;
		private double la, lo;
		private int num_Towers;
		
		GroupTowers() {
			group = new Vector<Integer>();
			num_Towers = 0;
			la = lo = 0.0;
		}
		
		void addTower(int i, double la, double lo) {
			group.add((int) i);
			num_Towers++;
			this.la = (la + this.la) / num_Towers;
			this.lo = (lo + this.lo) / num_Towers;
		}
		
		int getTower(int i) {
			return (int) group.get(i);
		}
		
		int numTowers() {
			return num_Towers;
		}
		
		double getLatitudeAverage() {
			return la;
		}
		
		double getLongitudeAverage() {
			return lo;
		}
	}
	
	
    @SuppressWarnings("unused")
	private int convertRSSIToASU(int mSignal) {
        /*
         * ASU is reported by the signal strength with a range of 0-31
         * RSSI = -113 + 2 * ASU
         * NeighboringCellInfo reports a positive RSSI
         * and needs to be converted to ASU
         * ASU = ((-1 * RSSI) + 113) / 2
         */
    	return mSignal > 31 ? Math.round(((-1 * mSignal) + 113) / 2) : mSignal;
    }
    
    @SuppressWarnings("unused")
	private int convertASUToRSSI(int asu) {
    	return (-113 + (asu*2));
    }
    
    private static class Circle {
    	private double y;
		private double x;
    	private float d;
    	private int huso; 
    	private boolean HS;
    	
    	Circle(CellLoc cl) {
    		Circle temp;

    		temp = convertFromGeoToUTM(cl);
    		setX(temp.getX());
    		setY(temp.getY());
    		setD(temp.getD());
    		setHuso(temp.getHuso());
    	}
    	
    	Circle(double x, double y, float d) {
    		this.x = x;
    		this.y = y;
    		this.d = d;
    	}

		public void setD(float f) {
			this.d = f;
		}

		public float getD() {
			return d;
		}

		public void setX(double e) {
			this.x = e;
		}

		public double getX() {
			return x;
		}

		public void setY(double e) {
			this.y = e;
		}

		public double getY() {
			return y;
		}
		
		private static double calculateDBTS(Circle c1, Circle c2) {
			double dbts = 0.0, x, y;
			
			x = (c2.getX() - c1.getX());
			y = (c2.getY() - c1.getY());
			x = x*x;
			y = y*y;
			dbts = Math.sqrt(x+y);
			
			return dbts;
		}
		
		public static Circle calculateIntersectingCircle(Circle c1, Circle c2) {
			Circle res = null;
			
			double dbts = calculateDBTS(c1, c2);
			//Log.d(Klich.LOG_TAG, "c1-x: "+c1.getX()+" c1-y: "+c1.getY()+" c1-d: "+c1.getD()+" -- c2-x: "+c2.getX()+" c2-y: "+c2.getY()+" c2-d: "+c2.getD());
			
			/*
			double l1 = c1.getD()+ c2.getD() - dbts;
			double l2 = Math.sqrt((c2.getD()*c2.getD()) - (l1*l1));
			double sinA = (c2.getY() - c1.getY()) / dbts;
			double cosA = (c2.getX() - c1.getX()) / dbts;
			
			if(l1 > l2) {
				double temp = l1;
				l1 = l2;
				l2 = temp;
				
				temp = sinA;
				sinA = cosA;
				cosA = temp;
			}
		
			double xp = c2.getX() - (l1*cosA);
			double yp = c2.getY() - (l1*sinA);
			//Log.d(Klich.LOG_TAG, "DBTS: "+dbts+" l1: "+l1+" l2: "+l2+" sinA: "+sinA+" cosA: "+cosA+" xp: "+xp+" yp: "+yp);
			
			double x1 = xp - (l2*sinA);
			double y1 = yp + (l2*cosA);
			
			double x2 = xp + (l2*sinA);
			double y2 = yp - (l2*cosA);
			
			//Log.d(Klich.LOG_TAG, "Lat1: "+y1+" Long1: "+x1+ " -- Lat2: "+y2+" Long2: "+x2);
			
			double xcenter = (x1 + x2) / 2.0;
			double ycenter = (y1 + y2) / 2.0;
			
			double x = xcenter - x1;
			double y = ycenter - y1;
			x = x*x;
			y = y*y;
			float radius = (float) Math.sqrt((x+y));
			
			res = new Circle(xcenter, ycenter, radius);
			res.setHuso(c1.getHuso());
			res.setHS(c1.isHS());
			
			//Log.d(Klich.LOG_TAG, "Xcenter: "+xcenter+" Ycenter: "+ycenter+ " -- Radius: "+radius);
			*/
			
			double k;
			
			k = 0.25 * Math.sqrt((dbts+c1.getD()+c2.getD()) * 
								 (-dbts+c1.getD()+c2.getD()) * 
								 (dbts-c1.getD()+c2.getD()) *
								 (dbts+c1.getD()-c2.getD()));
			
			double xcenter = (0.5*(c2.getX() + c1.getX())) + ((0.5*(c2.getX() - c1.getX())*
					         ((c1.getD()*c1.getD()) - (c2.getD()*c2.getD()))) / (dbts*dbts));
			
			double ycenter = (0.5*(c2.getY() + c1.getY())) + ((0.5*(c2.getY() - c1.getY())*
			                 ((c1.getD()*c1.getD()) - (c2.getD()*c2.getD()))) / (dbts*dbts));
			
			double xe = xcenter + (2*(c2.getY()-c1.getY())*k)/(dbts*dbts);
			double ye = ycenter + (-2*(c2.getX()-c1.getX())*k)/(dbts*dbts);
			
			float radius = (float) calculateDBTS(new Circle(xcenter, ycenter, 0.0f), new Circle(xe, ye, 0.0f));
			
			res = new Circle(xcenter, ycenter, radius);
			res.setHuso(c1.getHuso());
			res.setHS(c1.isHS());
			Log.d(Klich2.LOG_TAG, "Xcenter: "+xcenter+" Ycenter: "+ycenter+ " -- Radius: "+radius);
			
			return res;
		}

		public void setHuso(int huso) {
			this.huso = huso;
		}

		public int getHuso() {
			return huso;
		}

		public void setHS(boolean hS) {
			HS = hS;
		}

		public boolean isHS() {
			return HS;
		}
    }
	
	
	private static class CellLoc {
		private double longitude;
		private double latitude;
		private int asu;
		private float radius;
		private float distanceToUs;
		
		CellLoc(double latitude, double longitude, int asu) {
			setLatitude(latitude);
			setLongitude(longitude);
			setAsu(asu);
			radius = 0.0f;
			setDistanceToUs(0.0f);;
		}
		
		CellLoc() {
			setLatitude(0.0);
			setLongitude(0.0);
			setAsu(0);
			radius = 0.0f;
			setDistanceToUs(0.0f);;			
		}

		public void setAsu(int asu) {
			this.asu = asu;
		}

		public int getAsu() {
			return asu;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLatitude() {
			return latitude;
		}

		public void setRadius(float radius) {
			this.radius = radius;
		}

		public float getRadius() {
			return radius;
		}

		public void setDistanceToUs(float distanceToUs) {
			this.distanceToUs = distanceToUs;
		}

		public float getDistanceToUs() {
			return distanceToUs;
		}

	}
};

