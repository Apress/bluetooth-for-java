import java.io.*;
import javax.print.*;
import javax.print.event.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

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

public class JPSPrint {

	public static void main(String args[]) throws FileNotFoundException{
	
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
		FileInputStream fInput = new FileInputStream("nicePic.gif");
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
   }
}




