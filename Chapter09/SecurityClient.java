import java.io.*;
import javax.bluetooth.*;
import com.atinav.standardedition.io.*;

public class SecurityClient implements DiscoveryListener{
   
   private static LocalDevice localDevice = null;
   private DiscoveryAgent discoveryAgent = null;
   private String connectionURL = null;
   private RemoteDevice[] device = null;
   private ServiceRecord[] records = null;
   private boolean inquiryCompl = false;
   int count = 0;
   int maxSearches = 10;



   public SecurityClient(){
      System.out.println("\n Atinav aveLink Bluetooth Client Application \n");
      System.out.println("________________________________________\n");

      try{
         localDevice = LocalDevice.getLocalDevice();
         discoveryAgent = localDevice.getDiscoveryAgent();
         device = new RemoteDevice[10];
         System.out.println("_______________________________________\n");
         System.out.println("My BDAddress: " + 
                            localDevice.getBluetoothAddress());
         System.out.println(" ____________________________________\n");

         // Starts inquiry for devices in the proximity and waits till the 
         // inquiry is completed.

         System.out.println("\nSearching for Devices...\n");
         discoveryAgent.startInquiry(DiscoveryAgent.GIAC,this);

         synchronized(this){
            this.wait();
         }

         // Once the Device inquiry is completed it starts searching for the 
         // required service. Service search is done with the given uuid. After
         // starting each search it waits for the result. 

         // If the connection URL is null, i.e., if No service Records obtained, 
         // then it continues search in the next device detected.

         int[] attrSet = {0,3,4,0x100};
         UUID[] uuids = new UUID[1];
         uuids[0] = new UUID("1111",true);
         System.out.println("\nSearching for Service...\n");

         for(int i = 0; i< count; i++){

            int transactionid = 
               discoveryAgent.searchServices(attrSet,uuids,device[i],this);
            if(transactionid != -1){
               synchronized(this){
                  this.wait();
               }
            }

            if(connectionURL != null)
               break;
         }

     }catch(Exception ie){
         ie.printStackTrace();
     }

     // If the URL of the device begins with btl2cap, then we call the 
     // getConnection method which establishes a connection with the L2CAPServer 
     // and returns it. 
     // Connection returned is of type L2CAPConnection A piece of raw data is 
     // being sent over L2CAP.


     if(connectionURL == null)
        System.out.println("No service available...........");

     else if(connectionURL.startsWith("btl2cap")){
        try{
           L2CAPConnection connection = getconnection();

           System.out.println("\nConnection established to the remote device\n");
           String strData = "This is the Data From Client Application " +
                            "to Server Application";
           byte[] data = strData.getBytes();
           connection.send(data);
           try{
              Thread.sleep(10);
           }catch(Exception e){}

           while(true){
              byte[] datax = new byte[1000];

              while (!connection.ready()){
                 try{
                    Thread.sleep(1);
                 }catch(InterruptedException ie){}
              }
              connection.receive(datax);
              System.out.println("Data received at the Client Side " +
                                 new String(datax));
           }
           //   connection.close();

        }catch(Exception ioe){
           ioe.printStackTrace();
        }

     }// end of else if

   }


   //
   //  When a device is discovered it is added to the remote device table.
   //
   public synchronized void deviceDiscovered(RemoteDevice btDevice, 
                                             DeviceClass cod){

      device[count++] = btDevice;
      System.out.println("New Device discovered : " + 
                          btDevice.getBluetoothAddress());
   }

   //
   // When a service is discovered in a particular device and the connection URL 
   // is not null then the thread that is waiting in the main is notified.
   //
   public synchronized void servicesDiscovered(int transID, 
                                               ServiceRecord[] servRecords){
      records = new ServiceRecord[servRecords.length];
      records = servRecords;
      for(int i=0;i<servRecords.length;i++){

         int[] atrids = servRecords[i].getAttributeIDs();
         String servName = 
      (String)((DataElement)servRecords[i].getAttributeValue(0x100)).getValue();
         System.out.println("Service Name : "+ servName);
         connectionURL = servRecords[i].getConnectionURL(1,true);
         System.out.println("Connection url :" + connectionURL);
         if(connectionURL != null){
            synchronized(this){
               this.notify();
            }
         break;
         }
      }
    }

   // 
   // This function notifies the Thread waiting in main if a service  
   // search is terminated,ie,ig the responsecode
   // is SERVICE_SEARCH_COMPLETED or SERVICE_SEARCH_NO_RECORDS
   //
   public synchronized void serviceSearchCompleted(int transID, int respCode){

      if(respCode==SERVICE_SEARCH_ERROR)
          System.out.println("\nSERVICE_SEARCH_ERROR\n");

      if(respCode==SERVICE_SEARCH_COMPLETED)
          System.out.println("\nSERVICE_SEARCH_COMPLETED\n");

      if(respCode==SERVICE_SEARCH_TERMINATED)
          System.out.println("\n SERVICE_SEARCH_TERMINATED\n");

      if(respCode == SERVICE_SEARCH_NO_RECORDS){
          synchronized(this){
             this.notify();
          }
          System.out.println("\n SERVICE_SEARCH_NO_RECORDS\n");
      }

      if(respCode == SERVICE_SEARCH_DEVICE_NOT_REACHABLE)
          System.out.println("\n SERVICE_SEARCH_DEVICE_NOT_REACHABLE\n");
   }

   //
   // Once the device inquiry is completed it notifies the Thread 
   //  that waits in the Main.
   //
   public synchronized void inquiryCompleted(int discType){
      this.notify();
   }


   //
   //  Opens the connection to the Server.
   //
   L2CAPConnection getconnection() throws IOException{
      return (L2CAPConnection)Connector.open(connectionURL);
   }


   public static void main(String[] args){
      SecurityClient client = new SecurityClient();
   }


}
