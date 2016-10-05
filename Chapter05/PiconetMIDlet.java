import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.bluetooth.*;
import java.io.*;
import javax.microedition.io.*;
import com.atinav.bcc.*;

public class PiconetMIDlet extends javax.microedition.midlet.MIDlet implements 
                                                CommandListener,DiscoveryListener{
    private LocalDevice localDevice=null;
    private RemoteDevice device=null;
    private DiscoveryAgent discoveryAgent = null;
    private Command exitCommand; // The exit command
    private Command srchCommand; //The search command
    private Command backCommand;
   
    private Display display;    // The display for this MIDlet
    private Form frm;
    private List deviceLst;
    private List ServiceLst;

    int count = 0;
   
    private String[] dev = null;
    private Image img[] =null;
    private String[] services=null;

    public void startApp() {
 
        display = Display.getDisplay(this);
        exitCommand = new Command("Exit", Command.EXIT, 1);
        srchCommand=new Command("Search",Command.SCREEN,1);
        backCommand= new Command("Back",Command.BACK,1);
        frm=new Form("Piconet Browser");
        frm.addCommand(srchCommand);
        frm.addCommand(exitCommand);
        frm.setCommandListener(this);
        display.setCurrent(frm);
  
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
    }
    public void commandAction(Command c, Displayable s) {
        
        if (c == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
        }  
        if (c == srchCommand) {
           try{
            BCC.setPortName("COM1");
            BCC.setBaudRate(57600);
            BCC.setConnectable(false);
            BCC.setDiscoverable(DiscoveryAgent.NOT_DISCOVERABLE);
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            //device = new RemoteDevice[10];
            discoveryAgent = localDevice.getDiscoveryAgent();
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC,this);
        }catch(BluetoothStateException btstateex)
        {
          btstateex.printStackTrace();
        }
            
        try{
           
            deviceLst=new List("Devices",List.IMPLICIT,dev,img);
            deviceLst.addCommand(exitCommand);
            srchCommand=null;
            srchCommand=new Command("Refresh",Command.SCREEN,1);
            deviceLst.addCommand(srchCommand);
            deviceLst.setCommandListener(this);
            display.setCurrent(deviceLst);
            System.out.println(deviceLst.getSelectedIndex());
        }catch (Exception e){e.printStackTrace();}
        }
        if (c==List.SELECT_COMMAND){
            int index=deviceLst.getSelectedIndex();
           
            //do service search for device[index]
            int[] attrSet = {100};
            UUID[] uuids = new UUID[1];
            uuids[0] = new UUID("9856",true);
            services=null;
            int transId=discoveryAgent.searchServices(attrSet,uuids,
                                                        device[i],PiconetMIDlet);

            ServiceLst=new List("Service",List.IMPLICIT);
            for (int k=0;k<services.length;k++)
                ServiceLst.append(services[k],null);
            ServiceLst.addCommand(backCommand);
            ServiceLst.setCommandListener(this);
            display.setCurrent(ServiceLst);
        }
            if (c == backCommand) {
                display.setCurrent(deviceLst);
            }
    }

 
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod)
  {
        /* Store the device address in the array which will be 
         * used to create the device list.
         * the getBluetoothAddress() returns the Bluetooth address as a string.
         */
        device[count]=btDevice;
        //Check the type of device so that the appropriate image can be selected
       try{
        if (cod.getMinorDeviceClass()==0x04) 
            img[count]=Image.createImage("/phone.png");
        else if (cod.getMinorDeviceClass()==0x0C)
            img[count]={Image.createImage("/laptop.png")};
        else img[count]={Image.createImage("/misc.png")};
            
       } catch (Exception e){e.printStackTrace();}
     count++;  
  }

  public void servicesDiscovered(int transID,ServiceRecord[] servRecords)
  { 
            
              for(int i=0;i<servRecords.length;i++)
                services[i]=servRecords[i].getAttributeValue(0x0100);
                synchronized(this){
                this.notify();
           }
   }


  public void serviceSearchCompleted(int transID, int respCode)
  {if(respCode==SERVICE_SEARCH_ERROR){
          System.out.println("\nSERVICE_SEARCH_ERROR\n");
    }
    if(respCode==SERVICE_SEARCH_COMPLETED){
      // System.out.println("\nSERVICE_SEARCH_COMPLETED\n");

    }
    if(respCode==SERVICE_SEARCH_TERMINATED){
         System.out.println("\n SERVICE_SEARCH_TERMINATED\n");
    }
    if(respCode == SERVICE_SEARCH_NO_RECORDS){
         services[0]="None";
        synchronized(this){
                      this.notify();
             }
        System.out.println("\n SERVICE_SEARCH_NO_RECORDS\n");
    }
     if(respCode == SERVICE_SEARCH_DEVICE_NOT_REACHABLE)
        System.out.println("\n SERVICE_SEARCH_DEVICE_NOT_REACHABLE\n");
  }

  public void inquiryCompleted(int discType)
  {
                this.notify();
  }
  
}
