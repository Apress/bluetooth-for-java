import java.io.*;
import javax.bluetooth.*;
import com.rococosoft.io.*;

class Server {
           
   StreamConnection con = null;
   StreamConnectionNotifier service= null;
   InputStream ip  = null;
   OutputStream op = null;
   String serviceURL = "btspp://localhost:1111;name=ChatServer";

public Server() throws  IOException{
//Extends a stream for client to connect
   service = (StreamConnectionNotifier)Connector.open(serviceURL);
//Server waiting for client to connect
   con = service.acceptAndOpen();
//Open streams for two way communication.
   ip = con.openInputStream();
   op = con.openOutputStream();
//Starts a new thread for reading data from inputstream
//while the present thread, goes forward and write data to outputstream
//thus enabling a two way communication with the client
   ReadThread rdthr = new ReadThread(ip);
   rdthr.start();
   writeData();	
}
private void writeData() throws IOException{
   int data = 0;
   do{
   try{
   data = System.in.read();
   op.write(data);
   }catch(IOException e){}
   }while(true);	
   }
}
class ReadThread extends Thread {
   InputStream ip = null;
   public ReadThread(InputStream inp){
   ip = inp;
   }
public void run() {
   char data;
   int i =    0;
   do{
   try{
//Read data from the stream 
   data = (char)ip.read();
   System.out.print(data);
//This is bit sneaky and hard to explain. 
//comment this line to see the difference in how
//the application behaves.
   if(data == 0x0d)System.out.println();
}
   catch(IOException e){}
   }while(true);
   }
}

public class ChatServer {
   public static void main(String args[]) throws IOException {
   System.setProperty("improntolocaldevice.friendlyname", "ChatServer");
   Server chatServer = new Server();
   }
}

