import java.io.*;
import javax.bluetooth.*;
import com.rococosoft.io.*;

class Client implements DiscoveryListener{

private static LocalDevice localDevice = null;
private DiscoveryAgent discoveryAgent = null;
private String connectionURL = null;
private RemoteDevice[] device = null;
private ServiceRecord[] records = null;
private boolean inquiryCompl = false;
int count = 0;
int maxSearches =  10;
InputStream ip  = null;
OutputStream op = null;
public Client() throws  IOException, InterruptedException{
   localDevice = LocalDevice.getLocalDevice();
   discoveryAgent = localDevice.getDiscoveryAgent();
   device = new RemoteDevice[10]
// Starts inquiry for devices in the proximity and waits till the 
//inquiry is completed.
   System.out.println("\nSearching for Devices...\n");
   discoveryAgent.startInquiry(DiscoveryAgent.GIAC,this);
   synchronized(this){
   this.wait();
   }

//Once the Device inquiry is completed it starts searching for the 
//required service. service search is done with the given uuid. 
//After starting each search it waits for the result. If the
//connectionURL is null, ie, if No service Records obtained, then 
//it continues search in the next device detected.
 
   int[] attrSet = {0,3,4,0x100};
   UUID[] uuids = new UUID[1];
   uuids[0] = new UUID("1111",true);
   for(int i = 0; i< count;i++) {
   int transactionid = discoveryAgent.searchServices
                                  (attrSet,uuids,device[i],this);
   if(transactionid != -1){
   synchronized(this){
   this.wait();
   }
   }
   if(connectionURL != null)
   break;
   }// end of forloop
//If the URL of the device begins with btspp, ie,of an SPP server then 
//we call the getConnection meethod which
//establishes a connection with the SPPServer and returns it. Connection
// returned is of type  StreamConnection.
//A piece of raw data is being sent over RFCOMM.

   if(connectionURL == null)
   System.out.println("No service available...........");
   else if(connectionURL.startsWith("btspp")){
   StreamConnection connection = getconnection();
   op  = connection.openOutputStream();
   ip  = connection.openInputStream();	
   }        
   WriteThread wrthr = new WriteThread(op);
   wrthr.start();
   readData();
}
private void readData()throws IOException{
   char data;
   int i =    0;
   do{
   data = (char)ip.read();
   System.out.print(data);
   if(data == 0x0d)System.out.println();
   }while(true);
}

//When a device is discovered it is added to the remote device table.

public synchronized void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod)
{
   System.out.println("New Device discovered : "+btDevice.getBluetoothAddress());
   device[count++] = btDevice;
}


//When a service is discovered in a particular device and the connection url is not null //then the thread that is waiting in the main is notified.

public synchronized void servicesDiscovered(int transID,
                                             ServiceRecord[] servRecords) {
   
   records = new ServiceRecord[servRecords.length];
   records = servRecords;
   for(int i=0;i<servRecords.length;i++) {
   int[] atrids = servRecords[i].getAttributeIDs();
   String servName = (String)((DataElement)servRecords[i].getAttributeValue(0x100)).getValue();
   System.out.println("Service Name : "+servName);
   connectionURL = servRecords[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT,true);
   System.out.println("Connection url :" + connectionURL);
   if(connectionURL != null) {
   synchronized(this) {
   this.notify();
   }
   break;
   }
   }   
}
//This function notifies the Thread waiting in main if a service search is terminated,ie,ig //the responsecode is SERVICE_SEARCH_COMPLETED or //SERVICE_SEARCH_NO_RECORDS

public synchronized void serviceSearchCompleted(int transID, int respCode)   {
      
   if(respCode==SERVICE_SEARCH_ERROR)
   System.out.println("\nSERVICE_SEARCH_ERROR\n");
   if(respCode==SERVICE_SEARCH_COMPLETED)
   System.out.println("\nSERVICE_SEARCH_COMPLETED\n");
   if(respCode==SERVICE_SEARCH_TERMINATED)
   System.out.println("\n SERVICE_SEARCH_TERMINATED\n");
   if(respCode == SERVICE_SEARCH_NO_RECORDS){
   synchronized(this) {
   this.notify();
   }
   System.out.println("\n SERVICE_SEARCH_NO_RECORDS\n");
   }
   if(respCode == SERVICE_SEARCH_DEVICE_NOT_REACHABLE)
   System.out.println("\n SERVICE_SEARCH_DEVICE_NOT_REACHABLE\n");
}
//Once the device inquiry is completed it notifies the Thread that waits in the Main.
   public synchronized void inquiryCompleted(int discType)      {
   this.notify();
   }
   StreamConnection getconnection() throws IOException {
   return (StreamConnection)Connector.open(connectionURL);
   }      
}



class WriteThread extends Thread {
   OutputStream op = null;
   public WriteThread(OutputStream oup){
   op = oup;
   }
   public void run() {
   int data = 0;
   int i =    0;
   do{
   try{
   data = System.in.read();
   op.write(data);
   }catch(IOException e){}
   }while(true);
   }
}
public class ChatClient {
   public static void main(String args[]) throws IOException,InterruptedException
 {
   System.setProperty("improntolocaldevice.friendlyname", "ChatClient");
   Client chatClient = new Client();
   }
}
