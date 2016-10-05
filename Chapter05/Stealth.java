import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.bluetooth.*;
import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.atinav.bcc.*;

/* The Timer and the BeamTsk class is used to create the beam of
 * 3 concentric circles blinking. It has no purpose except visual enhancement.
 */
public class Stealth extends MIDlet {
    private Display display;
    private GUI canvas;
    private Timer tm; 
    private BeamTsk tsk;
    private String dev;
    private RemoteDevice device[];
    
    public Stealth()
    {
        display=Display.getDisplay(this);
        canvas=new GUI(this);
        tm=new Timer();
        tsk=new BeamTsk(canvas);
        tm.schedule(tsk,1000,1000);
        
    }
    
    protected void startApp()
    {
        display.setCurrent(canvas);
    }
    
    protected void pauseApp()
    {
    }
    
    public void destroyApp(boolean unconditional) {
    }
    
    public void exitStealth()
    {        
        destroyApp(true);
        notifyDestroyed();
    }
    public void exitTimer(){
        tm.cancel();
        tsk.cancel() ;
    }
}

class GUI extends Canvas implements CommandListener{
    private Command exitCommand;
    private Image img=null;
    private Image imgArc=null;
    private Stealth midlet;
    public int i=0; // used for creating the beam
    public int count=0; // used to create the blinking 
    public boolean cancel=false;
    int x=30;
    int y=30;
    int wd=5;
    int ht=10;
    public GUI(Stealth midlet){
        this.midlet=midlet;
        exitCommand=new Command("Exit",Command.EXIT,1);
        addCommand(exitCommand);
        setCommandListener(this);
   
        try {
             img=Image.createImage("/phone.png");
        }
        catch (java.io.IOException e){
            System.err.println("Unnable to locate or read image (.png) file");
        }
        try{
            BCC.setPortName("COM1");
            BCC.setBaudRate(57600);
            BCC.setConnectable(false);
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            discoveryAgent = localDevice.getDiscoveryAgent();
            device = new RemoteDevice[10];
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC,this);
         }catch(BluetoothStateException btstateex)
         {
            btstateex.printStackTrace();
         }
        
    }
     public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod)
        {
          /* The method is fired every time a device is discovered. 
              * The inquiry is cancelled after the first device is discovered.
              */
            BCC.setDiscoverable(DiscoveryAgent.NOT_DISCOVERABLE);
            cancelInquiry(discoveryAgent);
        }
      public void inquiryCompleted(int discType)
     {  
        cancel=true;
         this.notify();
    }

    /**
     * paint
     */
    public void paint(Graphics g) {
          
     if (i==0){
            // Used to clear the portion of the screen
            g.setColor(255,255,255);
            g.fillRect(25,10,50,70);
        }
     else {
        // draw the image of phone at given 
        // coordinates at the top left of the screen
        g.drawImage(img,10,30,Graphics.LEFT|Graphics.TOP);
        // draw a string at the bottom left
        g.drawString("Me",10,45+img.getHeight(),Graphics.LEFT|
                                 Graphics.BOTTOM);
        if (!cancel){
           // draw an arc at given coordinates
           g.drawArc(x,y,wd,ht,270,180);
        }
        else{
            
            g.drawImage(img,90,30,Graphics.RIGHT|Graphics.TOP);
            g.drawString("I am in Stealth Mode",2,100,Graphics.LEFT|
                                       Graphics.BOTTOM);
            try {
            img=Image.createImage("/phonegray.png");
            }catch (Exception e){e.printStackTrace();}
            g.drawImage(img,10,30,Graphics.LEFT|Graphics.TOP);

            midlet.exitTimer() ;
        }
             
      }  
    }
    
    public void commandAction(Command c, Displayable s) {
        
        if (c == exitCommand) 
            midlet.exitStealth();
    }
    
}
