import java.io.*;
import javax.bluetooth.*;
import com.atinav.standardedition.io.*;


public class SecurityServer {

   public static void main(String args[]){
      SecurityServer a = new SecurityServer();
      L2CAPConnection con = null;
      L2CAPConnectionNotifier service = null;
      InputStream in = null;
      OutputStream out = null;
      String serviceURL = "btl2cap://localhost:1111;name=ATINAV;" + 
                          "authorize=true;authenticate=true;encrypt=false";
      LocalDevice local = null;

      try {
         local = LocalDevice.getLocalDevice();
         System.out.println("\n Atinav aveLink Bluetooth Server Application \n");
         System.out.println("__________________________________________\n");
         System.out.println("My BDAddress: " + local.getBluetoothAddress());
         System.out.println("__________________________________________\n");
         service = (L2CAPConnectionNotifier)Connector.open(serviceURL);

         //
         // Add the service record to the SDDB and 
         // accept a client Connection
         //

         con = service.acceptAndOpen();
         System.out.println("\nConnection established to the remote device\n");

         byte[] data = new byte[1000];
         while (!con.ready()){
            try{
               Thread.sleep(1);
            }catch(InterruptedException ie){}
         }
         con.receive(data);
         System.out.println("Data received at the Server Side " + 
                            new String(data));

         String strData= "This is the Data From Server Application to " + 
                        Client " + "Application";
         byte[] datax = strData.getBytes();
         con.send(datax);
         //System.out.println("Data sent from the server side." + strData);
         try{
            Thread.sleep(10);
            }catch(Exception e){}
      }catch(Exception e){
            e.printStackTrace();
      }

   } 
}

