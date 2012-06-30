package net.SoftForLife.Klich2;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class CallStateListener extends PhoneStateListener
{
	private int asu;
	private int Cid;
	private int lac;
	
	private boolean LocationChanged;
	private boolean strengthSignalChanged;
	
	CallStateListener() {
		asu = 0;
		Cid = -1;
		lac = -1;
		LocationChanged = false;
		strengthSignalChanged = false;
	}
	
	public void   onSignalStrengthChanged(int asu) {
		this.asu = asu;
		strengthSignalChanged = true;
	}
	
    public void onCellLocationChanged(CellLocation  location)
    {
    	 GsmCellLocation locationGsm = (GsmCellLocation) location;
		 Cid = locationGsm.getCid();
		 lac = locationGsm.getLac();
		 LocationChanged = true;
    	 Log.d("ListenCell", "cellID="+Cid+" lac="+lac);
    	 
    }
    
    public boolean isLocationObtained() {
    	return LocationChanged;
    }
    
    public boolean isStrengthChanged() {
    	return strengthSignalChanged;
    }
    
    public int getAsu() {
    		return asu;
    }
    
    public int getCid() {
    	return Cid;
    }
    
    public int getLac() {
    	return lac;
    }
};


