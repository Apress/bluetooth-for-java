import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.bluetooth.*;
import javax.obex.*;

public class FTClient implements DiscoveryListener {
    LocalDevice local = null;
    DiscoveryAgent agent = null;
    int[] attrSet = null;
    RemoteDevice btDev = null;
    String serviceURL = null;
    ClientSession con = null;
    HeaderSet hdr = null;
    
     public FTClient() throws BluetoothStateException{
 
        // initialize the stack, if needed
	  local = LocalDevice.getLocalDevice();
     	  agent = local.getDiscoveryAgent();
     	  agent.startInquiry(DiscoveryAgent.GIAC, this);
     }
	
     public void deviceDiscovered(RemoteDevice btDevice,DeviceClass cod){
     	  btDev = btDevice;
     	  System.out.println("Device discovered " + 
          btDevice.getBluetoothAddress());
     }

     public void servicesDiscovered(int transID, ServiceRecord[] servRecord){
     	  System.out.println("Discovered a service ....");
     	  for(int i =0; i < servRecord.length; i++){
     	     serviceURL = 
          servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
             true);	
     	     System.out.println("The service URL is " + serviceURL);	
     	  }
     }

     public void serviceSearchCompleted(int transID, int respCode){
     	  System.out.println("Service search completed ........... ");
     	  System.out.println("Opening a connection with the server ....");
     	  try{
     	    con = (ClientSession)Connector.open(serviceURL);
     	    hdr = con.connect(hdr);
     	    System.out.println("Response code of the server after connect..." +
            hdr.getResponseCode());


     	    //Sending a request to server for file Hello.txt
     	    hdr = con.createHeaderSet(); 
     	    hdr.setHeader(HeaderSet.TYPE,"text/vCard");
     	    hdr.setHeader(HeaderSet.NAME,"Hello.txt");
     	    Operation op = con.get(hdr);

     	    //The server is now sending the file 
     	    InputStream in = op.openInputStream();


     	    // Writing the file from server to local file system. 
	    StreamConnection filestream = 
          (StreamConnection)Connector.open("file://name=HelloFile.txt;mode=w");
	    OutputStream out = filestream.openOutputStream();

            //read and write the data
            int data = in.read();
            while(data != -1){
               out.write((byte)data);
               data = in.read();
            }

            // send the DISCONNECT Operation
            con.disconnect();

            // cleanup
	      op.close();
            in.close();
            out.close();
     	 }
     	 catch(IOException e){
     	 	System.out.println(e.getMessage());
     	 }
     }
     public void inquiryCompleted(int discType){
     	  System.out.println("Inquiry completed ... ");
     	  UUID[] uuids = new UUID[1];
        uuids[0] = new UUID("1106",true);
        try{
           if(btDev == null){
	       System.out.println("No device has been discovered, " +
              "hence not worth proceeding exiting .... ");
	       System.exit(1);
     	   }
     	   System.out.println("Now searching for services ........ ");	
     	   agent.searchServices(attrSet, uuids, btDev, this);
     	}
     	catch(BluetoothStateException e) {System.out.println(e.getMessage());}
     }

     public static void main(String args[]) throws IOException {
     	  FTClient client = new FTClient();
     }	
}
