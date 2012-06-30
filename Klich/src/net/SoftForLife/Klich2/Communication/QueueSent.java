package net.SoftForLife.Klich2.Communication;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.os.Looper;

import net.SoftForLife.Klich2.Road;
import net.SoftForLife.Klich2.model.GeopointMobile;

public class QueueSent extends Thread {
	private Queue <ElementToBeSent> queue;
	private TrackingWS trackingWS;
	private boolean continue_thread;
	
	public static final int NEW_TRACK = 1;
	public static final int REPLACE_GEOPOINT = 2;
	public static final int REPLACE_LAST_GEOPOINT = 3;
	public static final int ADD_GEOPOINT = 4;
	
	public QueueSent(Context ctx, Road road) {
		super();
		queue = new LinkedList<ElementToBeSent>();
		trackingWS = new TrackingWS(ctx, road);
		trackingWS.initClient();
		continue_thread = true;
	}
	
	public void run() {
		Looper.prepare();
		while (continue_thread) {
			synchronized(queue) {
				if ((queue.size() > 0) && (!trackingWS.isSending())) {
					ElementToBeSent etbs = queue.remove();
			
					switch(etbs.cod_operation) {
						case NEW_TRACK:
							trackingWS.startNewTrack(etbs.getR(), etbs.getIndex());
							break;
						case REPLACE_GEOPOINT:
							trackingWS.replaceGeoPoint(etbs.getR(), etbs.getIndex());
							break;
						case REPLACE_LAST_GEOPOINT:
							trackingWS.replaceLastGeoPoint(etbs.getR(), etbs.getIndex());
							break;
						case ADD_GEOPOINT:
							trackingWS.sendNormalGeoPoint(etbs.getR(), etbs.getIndex());
							break;
					}
				}
			}
		}
	}
	
	public void addOperation(GeopointMobile gm, int cod_op, int index) {
		ElementToBeSent etbs = new ElementToBeSent(gm, cod_op, index);
		synchronized(queue) {
			queue.add(etbs);
		}
	}
	
	public void stopThread() {
		continue_thread = false;
	}
	
    protected void finalize () throws Throwable {
    	continue_thread = false;
    }	
	
	private class ElementToBeSent {
		public int cod_operation;
		private GeopointMobile gm;
		private int index;
		
		public ElementToBeSent(GeopointMobile gm, int op_cod, int index) {
			setR(gm);
			cod_operation = op_cod;
			setIndex(index);
		}
		
		public void setIndex(int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return index;
		}

		public void setR(GeopointMobile gm) {
			this.gm = gm;
		}

		public GeopointMobile getR() {
			return gm;
		}
	}
}
