package net.SoftForLife.Klich2.Communication;

import net.SoftForLife.Klich2.model.TuserMobile;
import net.softforlife.klich.model.Tuser;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.util.Log;

public class RegisterWS {
	
	public static final String LOG_TAG = "RegisterWS_tag";

	public RegisterWS() {
		
	}
	
	public void register(TuserMobile user) {
		SoapObject requestUser = new SoapObject(CommonWS.NAMESPACE, "register");
		PropertyInfo pUser  = new PropertyInfo();
		pUser.setName("arg0");
		String strg = user.toString();
		pUser.setValue(strg);
		pUser.setType(String.class);
		requestUser.addProperty(pUser);
		SoapSerializationEnvelope envelopeTrack = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	
		envelopeTrack.setOutputSoapObject(requestUser);
	
		HttpTransportSE httpTransportSE = new HttpTransportSE(CommonWS.URL);
	
        try {
        	httpTransportSE.debug = true;
        	httpTransportSE.call(CommonWS.SOAP_ACTION, envelopeTrack);
        	
        	SoapObject result = (SoapObject) envelopeTrack.getResponse();
        	String resultRequestSOAP = result.getPropertyAsString("message");
        	
        	if (!resultRequestSOAP.equals("FAIL")) {
        		Tuser registerUser = Tuser.parse(resultRequestSOAP);
        		user.setUserId(registerUser.getUserId());
        	} else {
        		user.getUserId().setUserId(-1);
        	}
        } catch (NumberFormatException nfe) {
        	Log.e(LOG_TAG, "No se pudo extraer el track Id: " + nfe.getMessage());
        	user.getUserId().setUserId(-2);
        } catch (Exception ex) {
        	String dump = httpTransportSE.requestDump;
        	user.setRegistered(false);   	
        	Log.e(LOG_TAG, "Request Dump: " + dump);
        	Log.e(LOG_TAG, ex.toString());
        }
	}
        
}
