import java.io.*;
import javax.print.*;
import javax.print.event.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.bluetooth.*;
import com.atinav.standardedition.io.*;

class PrintStatus implements PrintJobListener {
 	
 	public void printDataTransferCompleted(PrintJobEvent pje) {
 		System.out.println("Data delivered to printer succesfully ...");
 	}
 	public void printJobCanceled(PrintJobEvent pje) {
		System.out.println("The print job has been cancelled ..."); 		
 	}
 	public void printJobCompleted(PrintJobEvent pje) {
 		System.out.println("The print job completed successfully ...");
 	}
 	public void printJobFailed(PrintJobEvent pje) {
 		System.out.println("The document failed to print ..");
 	}
 	public void printJobNoMoreEvents(PrintJobEvent pje) {
 		System.out.println("No more events to deliver ...");
 	}
 	public void printJobRequiresAttention(PrintJobEvent pje) {
 		System.out.println("Some thing terrible happend which requires attention ...");
 	}
}


public class JPSBluetoothPrint implements Runnable {
	
	L2CAPConnection l2capConn = null;
	private int maxRecv = -1;
		
	private boolean printFile(String fileName) throws FileNotFoundException{
		
		System.out.println("Invoking Common printAPI for printing file : "+fileName);
		
		PrintStatus status = new PrintStatus();
	
		/*Create the DocFlavor for GIF */
		DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
		/* Create an attribute set comprising of the print instructions*/
		PrintRequestAttributeSet attribSet = new HashPrintRequestAttributeSet();
		attribSet.add(new Copies(1));
		attribSet.add(MediaSizeName.ISO_A4);
	
		/* Locate print services, which can print a GIF in the manner specified */
		PrintService[] pservices = PrintServiceLookup.lookupPrintServices(flavor, attribSet);
	
		if (pservices.length > 0) {
	
			DocPrintJob job = pservices[0].createPrintJob();
			/* Adding a PrintStatus Listener */
			job.addPrintJobListener(status);	
			/* Create a Doc implementation to pass the print data */
			FileInputStream fInput = new FileInputStream(fileName);
			Doc doc = new SimpleDoc(fInput, flavor, null);
	
			/* Print the doc as specified */
			try {
				job.print(doc, attribSet);
			} 
			catch (PrintException e) { 
				System.err.println(e);
			}
	} 
	else 
	 System.err.println("No suitable printers");
	return true;
	}

	public void connectToClientAndPrint() throws Exception {
		System.out.println("Host Device = " + LocalDevice.getLocalDevice().getBluetoothAddress());;
		
		UUID uuid = new UUID("6666", true);
		L2CAPConnectionNotifier l2capNotifier =  
			(L2CAPConnectionNotifier) Connector.open("btl2cap://localhost:"+uuid+";name=simplePrintServer");	//here
		l2capConn = l2capNotifier.acceptAndOpen();
		maxRecv = l2capConn.getReceiveMTU();
		System.out.println("Connected to a client..Recieve buffer Size is :"+maxRecv);
		new Thread(this).start();
	} 
	
	public void run() {
		try {
			// packet recieved
			byte [] data = 	new byte[maxRecv];
			
			// Reading fileName
			int dataSize = l2capConn.receive(data); //blocks assuming fileName always less than 48 bytes
			byte [] fileNameAsBytes = new byte[dataSize];	
			//arraycopy(Object src,int src_position,Object dst, int dst_position,int length)		
			System.arraycopy(data,0,fileNameAsBytes, 0,dataSize);		
			String fileName = new String(fileNameAsBytes);
			System.out.println("File Name is = " + fileName);
								
			// for File
			//OutputConnection toFileConn = (OutputConnection)Connector.open("file://name="+fileName+";mode=w");
			//OutputStream toFileStrm = toFileConn.openOutputStream();
			FileOutputStream toFileStrm = new FileOutputStream(new File(fileName));
			
			try {
				
				System.out.println("Starting to Recieve file Body");
				// recieve File body
				while(true) {
					if (l2capConn.ready()) {
						dataSize = l2capConn.receive(data); 
						// after the whole file, an empty packet is sent from the other end
						if (dataSize == 0) {
							System.out.println("Signal to Stop recieved");
							toFileStrm.close();
							toFileStrm = null;
							printFile(fileName);
							break;
						}	
						toFileStrm.write(data, 0, dataSize);
					}
					try {Thread.currentThread().sleep(10);}catch(Exception genExp) {}	 
				}
			}
			finally	{
				try {l2capConn.close();}catch(Exception genExp) {}
			}
		}
		catch(Exception genEx) {
		}
	}
	
	public static void main(String [] args) throws Exception {
		
		JPSBluetoothPrint srv =	new JPSBluetoothPrint();
		srv.connectToClientAndPrint();
	}
}

