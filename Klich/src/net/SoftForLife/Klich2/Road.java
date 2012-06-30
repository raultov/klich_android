package net.SoftForLife.Klich2;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.content.Context;
import android.location.Location;
import android.text.format.Time;
import android.util.Log;

import net.SoftForLife.Klich2.Communication.*;
import net.SoftForLife.Klich2.model.GeopointMobile;
import net.softforlife.klich.model.Status;

public class Road {
        public String mName;
        public String mDescription;
        public ArrayList<GeopointMobile> route;
        private ArrayList<GeopointMobile> candidates;
        private Context act;
        private int numDiscards;
        private static QueueSent queue = null;
        private double speed_candidates = 0.0; // Candidates per second
        private long time_before;
        private int candidates_acumulated = 0;
        private final static int MIN_THRESHOLD_CANDIDATES = 2;
        private final static int THRESHOLD_DISCARDS = 5;
        private final static int AMOUNT_CANDIDATES_SPEED = 3;
        private final static double TIME_CANDIDATE_ADDED = 30.0;
        
        private static boolean modeSend = false;
        private static boolean newTrack = false;
        
        private boolean replace = false;
        
        private int threshold_candidates = MIN_THRESHOLD_CANDIDATES;
        
        public static final String LOG_TAG = "Road_tag";  
        
        public Road(Context activity, String name, String description) {
        	mName = new String(name);
        	mDescription = new String(description);
        	route = new ArrayList<GeopointMobile>();
        	candidates = new ArrayList<GeopointMobile>();
        	act = activity;
        	numDiscards = 0;
        	queue = new QueueSent(act, this);
        	
        	Time time = new Time();
			time.setToNow();
        	time_before = time.toMillis(false);
        }
        
        public static void enableSendMode() {
        	modeSend = true;
        	newTrack = true;
        	queue.setPriority(Thread.MIN_PRIORITY);
        	queue.start();
        }
        
        public static void disableSendMode() {
        	modeSend = false;
        	newTrack = false;
        	queue.stopThread();
        }
        
        public void addPointToRoute(double latitude, double longitude, float accuracy, Status type) {
        	Log.d(LOG_TAG, "Vamos a anadir un nuevo punto");
        	
        	GeopointMobile gm = new GeopointMobile();
        	gm.setAccuracy(accuracy / 1000.0f); // Pasamos la precisión a kilómetros
        	gm.setLatitude(latitude);
        	gm.setLongitude(longitude);
        	gm.setTypeGeopoint(type);
        	//GeopointMobile r = new Route(latitude, longitude,  accuracy, type);
        	candidates.add(gm);
        	
        	if(candidates_acumulated >= AMOUNT_CANDIDATES_SPEED) {
        		Time time = new Time();
        		time.setToNow();
        		long now = time.toMillis(false);
        		speed_candidates = (AMOUNT_CANDIDATES_SPEED*1000.0) / (now - time_before);
        		time_before = now;
        		candidates_acumulated = 0;
        	}
        	
        	candidates_acumulated++;
        	
        	threshold_candidates = (int) (speed_candidates * TIME_CANDIDATE_ADDED);
        	
        	if(threshold_candidates < MIN_THRESHOLD_CANDIDATES)
        		threshold_candidates = MIN_THRESHOLD_CANDIDATES;
        	
        	Log.d(LOG_TAG, "threshold_candidates = " + threshold_candidates + " y speed = "+ speed_candidates);
        	 	
        	GeopointMobile c = determineBestCandidate();
        	
        	if(c != null) {
        		if(modeSend == true) {
        			if(newTrack == true) {
        				newTrack = false;
        				route.add(c);
        				queue.addOperation(c, QueueSent.NEW_TRACK, route.size()-1);
        			}
        			else {
        				if(!replace) {
        					Iterator<GeopointMobile> itr = route.iterator();
        					GeopointMobile element = null;
        					
        					boolean found = false;
        				    while (itr.hasNext() && !found) {
        				        element = itr.next();
        				        if((element.getLatitude() == c.getLatitude()) && 
        				        		(element.getLongitude() == c.getLongitude()) && 
        				        			(element.getTypeGeopoint() == c.getTypeGeopoint()) && 
        				        				(element.getSent() == true)) {
        				        	found = true;
        				        	element.setAccuracy(c.getAccuracy());
        				        	element.setDate(new Date());
        				        }  	
        				    }
        				    
        				    if(found) {
        				    	c.setGeopointId(element.getGeopointId());
        				    	queue.addOperation(c, QueueSent.REPLACE_GEOPOINT, route.indexOf(element));
        				    } else {
        				    	route.add(c);
        				    	queue.addOperation(c, QueueSent.ADD_GEOPOINT, route.size()-1);
        				    }
        					
        					
        				}
        				else {
        					route.get(route.size()-1).setAccuracy(c.getAccuracy());
        					route.get(route.size()-1).setLatitude(c.getLatitude());
        					route.get(route.size()-1).setLongitude(c.getLongitude());
        					route.get(route.size()-1).setTypeGeopoint(c.getTypeGeopoint());
        					//route.get(route.size()-1).mColor = c.getColor();
        					//route.get(route.size()-1).mWidth = c.getStrokeWidth();
        					queue.addOperation(c, QueueSent.REPLACE_LAST_GEOPOINT, route.size()-1);
        				}
        			}
        		}
        	}    	
        }
        
        private GeopointMobile determineBestCandidate() {
        	GeopointMobile gm = null;
	
        	if(candidates.size() >= threshold_candidates) {
        		float min = Float.MAX_VALUE;
        		int index=0;
        		float [] results = new float[1];
        		
        		for(int i=0;i < candidates.size();i++) {
        			if(candidates.get(i).getAccuracy() < min) {
        				min = candidates.get(i).getAccuracy();
        				index = i;
        			}
        		}
        		
        		Log.d(LOG_TAG, "El menor es el "+index);
        		
        		replace = false;
        		
        		if(route.size() > 0) {
        			Location.distanceBetween(candidates.get(index).getLatitude(), 
        									 candidates.get(index).getLongitude(),
        									 route.get(route.size()-1).getLatitude(), 
        									 route.get(route.size()-1).getLongitude(), results);
        			
            		//if((results[0] > route.get(route.size()-1).getAccuracy()) && (candidates.get(index).getAccuracy() <= results[0])) {
        			results[0] = results[0] / 1000.0f;
        			
            		if((route.get(route.size()-1).getAccuracy() + candidates.get(index).getAccuracy()) <= results[0]) {
            			Log.d(LOG_TAG, "Tenemos un candidato apaniao");
            			gm = new GeopointMobile();
            			gm.setLatitude(candidates.get(index).getLatitude());
            			gm.setLongitude(candidates.get(index).getLongitude());
            			gm.setAccuracy(candidates.get(index).getAccuracy());
            			gm.setTypeGeopoint(candidates.get(index).getTypeGeopoint());
            			gm.setDate(new Date());
            			//r = new Route(candidates.get(index).la, candidates.get(index).lo, candidates.get(index).accuracy, candidates.get(index).type);
            		}
            		else {
            			if(candidates.get(index).getAccuracy() <= route.get(route.size()-1).getAccuracy()) {
            				Log.d(LOG_TAG, "Reemplazamos el anterior por uno igual o mas apaniao");
            				gm = new GeopointMobile();
                			gm.setLatitude(candidates.get(index).getLatitude());
                			gm.setLongitude(candidates.get(index).getLongitude());
                			gm.setAccuracy(candidates.get(index).getAccuracy());
                			gm.setTypeGeopoint(candidates.get(index).getTypeGeopoint());
                			gm.setDate(new Date());
            				//r = new Route(candidates.get(index).la, candidates.get(index).lo, candidates.get(index).accuracy, candidates.get(index).type);
            				replace = true;
            			}
            			/*else {
            				numDiscards++;
            				if(numDiscards > THRESHOLD_DISCARDS) {
            					Log.d(LOG_TAG, "A falta de apaniaos descartamos el anterior por uno nuevo");
            					gm = new GeopointMobile();
                    			gm.setLatitude(candidates.get(index).getLatitude());
                    			gm.setLongitude(candidates.get(index).getLongitude());
                    			gm.setAccuracy(candidates.get(index).getAccuracy());
                    			gm.setTypeGeopoint(candidates.get(index).getTypeGeopoint());
                				//r = new Route(candidates.get(index).la, candidates.get(index).lo, candidates.get(index).accuracy, candidates.get(index).type);
                				replace = true;
            					numDiscards = 0;
            				}
            			} */
            		}
        		} else {
        			Log.d(LOG_TAG, "Primer candidato a la olla");
        			gm = new GeopointMobile();
        			gm.setLatitude(candidates.get(index).getLatitude());
        			gm.setLongitude(candidates.get(index).getLongitude());
        			gm.setAccuracy(candidates.get(index).getAccuracy());
        			gm.setTypeGeopoint(candidates.get(index).getTypeGeopoint());
        			gm.setDate(new Date());
        			//r = new Route(candidates.get(index).la, candidates.get(index).lo, candidates.get(index).accuracy, candidates.get(index).type);
        		}

        		candidates.clear();
        	}
        	
        	return gm;
        }
        
        public GeopointMobile getRouteAt(int i) {
        	return route.get(i);
        }
        
        public void restart() {
        	Log.d(LOG_TAG, "Reiniciamos la Road");
        	Log.d(LOG_TAG, "El tamanio Road: " + route.size()+ " y el de candidatos: " + candidates.size());
        	numDiscards = 0;
        	candidates.clear();
        	route.clear();
        	queue.stopThread();
        }
};





