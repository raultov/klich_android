package net.SoftForLife.Klich2.Communication;

import net.SoftForLife.Klich2.Klich2;
import net.SoftForLife.Klich2.Road;
import net.SoftForLife.Klich2.model.GeopointMobile;
import net.softforlife.klich.model.Track;

import android.content.Context;
import android.util.Log;

import java.util.Date;
 
//import net.softforlife.klich.model.Track;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;


public class TrackingWS {
	public static final String LOG_TAG = "TrackingWS_tag";
	
	private boolean sending;
    

	public TrackingWS(Context act, Road road) {
		setSending(false);
	}
	
	public void initClient() {

	}
	
	public void startNewTrack(GeopointMobile gm, int index) {
		setSending(true);
		
		Track track = new Track();
		track.setDate(new Date());
		track.setDeviceId(Klich2.myDevice);
		SoapObject requestTrack = new SoapObject(CommonWS.NAMESPACE, "startNewTrack");
		PropertyInfo pTrack  = new PropertyInfo();
		pTrack.setName("arg0");
		String strg = track.toString();
		pTrack.setValue(strg);
		pTrack.setType(String.class);
		requestTrack.addProperty(pTrack);
		SoapSerializationEnvelope envelopeTrack = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		
		envelopeTrack.setOutputSoapObject(requestTrack);
		
		HttpTransportSE httpTransportSE = new HttpTransportSE(CommonWS.URL);
        try
        {
        	httpTransportSE.debug = true;
        	httpTransportSE.call(CommonWS.SOAP_ACTION, envelopeTrack);
        	
        	SoapObject result = (SoapObject) envelopeTrack.getResponse();
        	String resultRequestSOAP = result.getPropertyAsString("message");
        	
        	if(!resultRequestSOAP.equals("FAIL")) {
        		track.setTrackId(Long.parseLong(resultRequestSOAP));
        		Klich2.currentTrack.setTrackId(Long.parseLong(resultRequestSOAP));
        	}
        	
        }
        catch (NumberFormatException nfe) {
        	Log.e(LOG_TAG, "No se pudo extraer el track Id: " + nfe.getMessage());
        	track.setTrackId(null);
        	Klich2.currentTrack.setTrackId(-1L);
        	//gm.setSent(false);
        }
        catch (Exception aE)
        {
        	String dump = httpTransportSE.requestDump;
        	//gm.setSent(false);
        	Log.e(LOG_TAG, "Request Dump: " + dump);
        	Log.e(LOG_TAG, aE.toString());
        }

		
		gm.setDate(new Date());
		gm.setGeopointId(null);
		gm.setTrackId(track);
        
        SoapObject request = new SoapObject(CommonWS.NAMESPACE, "startNewGeopoint");
        PropertyInfo p = new PropertyInfo();
        
        p.setName("arg0");
        strg = gm.toString();
        p.setValue(strg);
        //p.setType(String.class);
        p.setType(String.class);
        request.addProperty(p);
        
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        
        envelope.setOutputSoapObject(request);
        
        try
        {
        	httpTransportSE.debug = true;
        	httpTransportSE.call(CommonWS.SOAP_ACTION, envelope);

        	SoapObject result = (SoapObject) envelopeTrack.getResponse();
        	String resultRequestSOAP = result.getPropertyAsString("message");
        	
            if(!resultRequestSOAP.equals("FAIL")) {
            	gm.setSent(true);
            	gm.setGeopointId(Long.parseLong(resultRequestSOAP));
            }
        }
        catch (Exception aE)
        {
        	gm.setSent(false);
        	String hola = httpTransportSE.requestDump;
        	Log.e(LOG_TAG, "Request Dump: " + hola);
            aE.printStackTrace ();
        }
		
        setSending(false);
	}
	
	public void sendNormalGeoPoint(GeopointMobile gm, int index) {
		setSending(true);
		
		gm.setGeopointId(null);
		gm.setDate(new Date());
		gm.setTrackId(Klich2.currentTrack);
        
        SoapObject request = new SoapObject(CommonWS.NAMESPACE, "sendNormalGeopoint");
        PropertyInfo p = new PropertyInfo();
        
        p.setName("arg0");
        String strg = gm.toString();
        p.setValue(strg);
        //p.setType(String.class);
        p.setType(String.class);
        request.addProperty(p);
        
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        
        envelope.setOutputSoapObject(request);
        
        HttpTransportSE httpTransportSE = new HttpTransportSE(CommonWS.URL);
        try
        {
        	httpTransportSE.debug = true;
        	httpTransportSE.call(CommonWS.SOAP_ACTION, envelope);
            String resultRequestSOAP =  (String) envelope.getResponse();
            
            if(!resultRequestSOAP.equals("FAIL")) {
            	gm.setSent(true);
            	gm.setGeopointId(Long.parseLong(resultRequestSOAP));
            }
        }
        catch (Exception aE)
        {
        	gm.setSent(false);
        	String hola = httpTransportSE.requestDump;
        	Log.e(LOG_TAG, "Request Dump: " + hola);
        	Log.e(LOG_TAG, "Traza: " + aE.toString());
            //aE.printStackTrace ();
        }

        setSending(false);
	}
	
	public void replaceLastGeoPoint(GeopointMobile gm, int index) {
		setSending(true);
		
		gm.setGeopointId(null);
		gm.setDate(new Date());
		gm.setTrackId(Klich2.currentTrack);
        
        SoapObject request = new SoapObject(CommonWS.NAMESPACE, "replaceLastGeopoint");
        PropertyInfo p = new PropertyInfo();
        
        p.setName("arg0");
        String strg = gm.toString();
        p.setValue(strg);
        p.setType(String.class);
        request.addProperty(p);
        
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        
        envelope.setOutputSoapObject(request);
        
        HttpTransportSE httpTransportSE = new HttpTransportSE(CommonWS.URL);
        try
        {
        	httpTransportSE.debug = true;
        	httpTransportSE.call(CommonWS.SOAP_ACTION, envelope);

        	SoapObject result = (SoapObject) envelope.getResponse();
        	String resultRequestSOAP = result.getPropertyAsString("message");

            if(!resultRequestSOAP.equals("FAIL")) {
            	gm.setSent(true);
            	gm.setGeopointId(Long.parseLong(resultRequestSOAP));
            }
        }
        catch (Exception aE)
        {
        	gm.setSent(false);
        	String hola = httpTransportSE.requestDump;
        	Log.e(LOG_TAG, "Request Dump: " + hola);
        	Log.e(LOG_TAG, "Traza: " + aE.toString());
        }
        
        setSending(false);
	}	
	
	public void replaceGeoPoint(GeopointMobile gm, int index) {
		setSending(true);
		
		gm.setDate(new Date());
		gm.setTrackId(Klich2.currentTrack);
		
        SoapObject request = new SoapObject(CommonWS.NAMESPACE, "replaceGeopoint");
        PropertyInfo p = new PropertyInfo();
        
        p.setName("arg0");
        String strg = gm.toString();
        p.setValue(strg);
        p.setType(String.class);
        request.addProperty(p);
        
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        
        envelope.setOutputSoapObject(request);
        
        HttpTransportSE httpTransportSE = new HttpTransportSE(CommonWS.URL);
        try
        {
        	httpTransportSE.debug = true;
        	httpTransportSE.call(CommonWS.SOAP_ACTION, envelope);

        	SoapObject result = (SoapObject) envelope.getResponse();
        	String resultRequestSOAP = result.getPropertyAsString("message");

            if(!resultRequestSOAP.equals("FAIL")) {
            	gm.setSent(true);
            	gm.setGeopointId(Long.parseLong(resultRequestSOAP));
            }
        }
        catch (Exception aE)
        {
        	gm.setSent(false);
        	String hola = httpTransportSE.requestDump;
        	Log.e(LOG_TAG, "Request Dump: " + hola);
        	Log.e(LOG_TAG, "Traza: " + aE.toString());
        }
        
        setSending(false);
	}

    private void setSending(boolean sending) {
		this.sending = sending;
	}

	public boolean isSending() {
		return sending;
	}

}
