import java.io.*;
import java.util.*;
import com.atinav.standardedition.io.*;
import javax.bluetooth.*;
import javax.obex.*;
import com.atinav.bcc.*;

public class JPSBluetoothPrintClient implements DiscoveryListener {
    LocalDevice    local 	 = null;
    DiscoveryAgent agent 	 = null;
    int[] 	   attrSet 	 = null;
    RemoteDevice   btDev   	 = null;
    String 	   serviceURL 	 = null;
    L2CAPConnection  l2capConn	 	 = null;
    
    public JPSBluetoothPrintClient() throws BluetoothStateException {
    
        local = LocalDevice.getLocalDevice();
     	agent = local.getDiscoveryAgent();
     	
     	agent.startInquiry(DiscoveryAgent.GIAC, this);
     	
         synchronized(this) //Waiting for Device Inquiry to be completed
         
           {
     	 	try{
     	  	  this.wait();   
     		   }catch(Exception IntE)
     		   {
         	     System.out.println(IntE.getMessage());	 	
     		   }//End of try block	
      	   } //End of synchronized(this)
      	   
      	   
     
 }
	
     public void deviceDiscovered(RemoteDevice btDevice,DeviceClass cod){
     	if ("011114378000".indexOf(btDevice.getBluetoothAddress())> -1) {btDev = btDevice;System.out.println("Assigned");}
     	System.out.println("Device discovered "+btDevice.getBluetoothAddress());
     	
     }
     public void servicesDiscovered(int transID, ServiceRecord[] servRecord){
     	System.out.println("Discovered a service ....");
     	for(int i =0; i < servRecord.length; i++){
     	  serviceURL = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT,true);	
     	  System.out.println("The service URL is "+serviceURL);	
     	}
     }
     public void serviceSearchCompleted(int transID, int respCode){
     	System.out.println("Service search completed ........... ");
     	
     	synchronized(this) //Unblocking the wait for Service search complete
           {
     	 	try{
     	  	    this.notifyAll();   
     		   }catch(Exception IntE)
     		   {
         	     System.out.println(IntE.getMessage());	 	
     		   }//End of try block	
      	   } //End of synchronized(this)
 
          
     }
     
     public void inquiryCompleted(int discType){
     	
     	 System.out.println("Inquiry completed ... ");
     	 
     	   synchronized(this) //Unblocking the wait for inquiry complete
           {
     	 	try{
     	  	    this.notifyAll();   
     		   }catch(Exception IntE)
     		   {
         	     System.out.println(IntE.getMessage());	 	
     		   }//End of try block	
      	   } //End of synchronized(this)
     	
     	
     
     }

     public void getServices()
     {
     
     	UUID[] uuids = new UUID[1];
        //uuids[0] = new UUID("1106",true);
        uuids[0] = new UUID("6666", true);
        try{
        	if(btDev == null){
	     		System.out.println("No device has been discovered, hence not worth proceeding exiting .... ");
	     		//BCC.disconnectPort();
	     		System.exit(1);
     		}
     	 System.out.println("Now searching for services ........ ");	
     	 agent.searchServices(attrSet, uuids, btDev, this);
     	      	 
     	}
     	catch(BluetoothStateException e) {
     		System.out.println(e.getMessage());
     		System.out.println("Got an exception, so closing port and exiting ...");
		System.exit(1);
     	}
         
         synchronized(this) //Waiting for Service Search to be completed
           {
     	 	try{
     	  	  this.wait();   
     		   }catch(Exception IntE)
     		   {
         	     System.out.println(IntE.getMessage());	 	
     		   }//End of try block	
      	   } //End of synchronized(this)     	
     }
     


     public boolean sendFile(String fileName) {
	try {
     		l2capConn = (L2CAPConnection)Connector.open(serviceURL);

		try {		
			InputConnection inConn  = (InputConnection)Connector.open("file://name="+fileName+";mode=r");
	    		InputStream fileReader = inConn.openInputStream();
	    		
	    		int maxSendMTU = l2capConn.getTransmitMTU();
	    		byte [] buffer = new byte[maxSendMTU];
	    		
	    		//sending fileName
	    		//assuming for the time being that the fileName will not be greater than 48 bytes
	    		l2capConn.send(fileName.getBytes());
	    		System.out.println("Send the file Name = "+fileName);
	    		
	    		// sending fileContent
	    		// after the whole file gets transferred, an empty packet is sent.
	    		int actualDataSize = -1;
	    		byte [] pkt = null;
	    		while((actualDataSize = fileReader.read(buffer)) != -1) {
	    			pkt = new byte[actualDataSize];
	    			//arraycopy(Object src, int src_position, Object dst, int dst_position, int length);
	    			System.arraycopy(buffer, 0, pkt, 0, actualDataSize);
	    			l2capConn.send(pkt);
	    		}
	    		System.out.println("Completed sendng body of file = "+fileName);
	    		//sending empty packet signaling file End
	    		l2capConn.send(new byte[0]);

			fileReader.close();
			return true;	    		
		}
		finally {
			System.out.println("Closing connection");
			try {l2capConn.close();}catch(Exception genx) {}
		}		
     	 }
     	 catch(IOException e){
     	 	System.out.println(e.getMessage());
     	 	return false;
     	 }     	
     }
     

     
	public static void main(String args[]) throws Exception  {
     		JPSBluetoothPrintClient client = new JPSBluetoothPrintClient();
     		client.getServices();
     		System.out.println(client.sendFile(args[0]));
     	}//End of main	
     	
 }//End of class