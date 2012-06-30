package net.SoftForLife.Klich2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class GSMLocation {
	private double lat, lng;
	
	GSMLocation(int cellID, int lac) throws Exception  {
        String urlString = "http://www.google.com/glm/mmap";            
        
        //---open a connection to Google Maps API---
        URL url = new URL(urlString); 
        URLConnection conn = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) conn;        
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true); 
        httpConn.setDoInput(true);
        httpConn.connect(); 
        
        //---write some custom data to Google Maps API---
        OutputStream outputStream = httpConn.getOutputStream();
		
		WriteData(outputStream,  cellID, lac);
		
        //---get the response---
        InputStream inputStream = httpConn.getInputStream();  
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        
        //---interpret the response obtained---
        dataInputStream.readShort();
        dataInputStream.readByte();     
        int code = dataInputStream.readInt();

        if (code == 0) {
            lat = (double) dataInputStream.readInt() / 1000000D;
            lng = (double) dataInputStream.readInt() / 1000000D;
            dataInputStream.readInt();
            dataInputStream.readInt();
            dataInputStream.readUTF();
        }
        else {
        	lat = 0.0;
        	lng = 0.0;
        }
        	
	}
	
	public double getLatitude() {
		return lat;
	}
	
	public double getLongitude() {
		return lng;
	}
	
    private void WriteData(OutputStream out, int cellID, int lac) 
    throws IOException
    {    	
        DataOutputStream dataOutputStream = new DataOutputStream(out);
        dataOutputStream.writeShort(21);
        dataOutputStream.writeLong(0);
        dataOutputStream.writeUTF("es");
        dataOutputStream.writeUTF("Android");
        dataOutputStream.writeUTF("1.6");
        dataOutputStream.writeUTF("Web");
        dataOutputStream.writeByte(27);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(3);
        dataOutputStream.writeUTF("");

        dataOutputStream.writeInt(cellID);  
        dataOutputStream.writeInt(lac);     

        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.flush();    	
    }	
	
};


