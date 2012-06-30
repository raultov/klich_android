package net.SoftForLife.Klich2.Communication;

import java.util.Date;

import net.SoftForLife.Klich2.model.TuserMobile;
import net.softforlife.klich.model.Tuser;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.util.Log;

public class LoginWS {
	public static final String LOG_TAG = "LoginWS_tag";
    
	public LoginWS() {

	}
	
    public void login(TuserMobile tuserMobile) {
    	Tuser user = new Tuser();
    	user.setActive(true);
    	user.setCreationDate(new Date());
    	user.setEmail(tuserMobile.getEmail());
    	user.setIdx(tuserMobile.getIdx());
    	user.getUserId().setLogin(tuserMobile.getUserId().getLogin());
    	user.setName(tuserMobile.getName());
    	user.setPassword(tuserMobile.getPassword());
    	user.setPhone(tuserMobile.getPhone());
    	user.setRecovery(tuserMobile.getRecovery());
    	user.setTuserTroleCollection(tuserMobile.getTuserTroleCollection());
    	user.setDeviceCollection(tuserMobile.getDeviceCollection());
    	
		SoapObject requestUser = new SoapObject(CommonWS.NAMESPACE, "login");
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
        		tuserMobile.setUserId(registerUser.getUserId());
        		tuserMobile.setRegistered(true);
        		tuserMobile.setEmail(registerUser.getEmail());
        		tuserMobile.setActive(registerUser.getActive());
        		tuserMobile.setCreationDate(registerUser.getCreationDate());
        		tuserMobile.setIdx(registerUser.getIdx());
        		tuserMobile.getUserId().setLogin(registerUser.getUserId().getLogin());
        		tuserMobile.setName(registerUser.getName());
        		tuserMobile.setPassword(registerUser.getPassword());
        		tuserMobile.setPhone(registerUser.getPhone());
        		tuserMobile.setRecovery(registerUser.getRecovery());
        	} else {
           		tuserMobile.getUserId().setUserId(-1);
        		tuserMobile.setRegistered(false);
        	}
        	
        } catch (NumberFormatException nfe) {
        	Log.e(LOG_TAG, "No se pudo extraer el track Id: " + nfe.getMessage());
        	tuserMobile.getUserId().setUserId(-2);
        	tuserMobile.setRegistered(false);
        } catch (Exception aE) {
        	String dump = httpTransportSE.requestDump;
        	tuserMobile.setRegistered(false);
        	tuserMobile.getUserId().setUserId(-3);     	
        	Log.e(LOG_TAG, "Request Dump: " + dump);
        	Log.e(LOG_TAG, aE.toString());
        }

    	
    }
}
