/*==========================================================================*/
/*             |                                                            */
/* Filename    | L2CAPEchoClient.java                                       */
/*             |                                                            */
/* Purpose     | This demo application will perform device and service      */
/*             | discovery and communicates with an echo server.            */
/*             |                                                            */
/* Remarks     | Needs Java API for Bluetooth (JSR-82) to operate.          */ 
/*             |                                                            */
/* Copyright   | (c) 2002, Smart Network Devices GmbH, Germany              */
/*             |                                                            */
/* Created     | July 15, 2002                                              */
/*             |                                                            */
/* Last Change | October 21, 2002 by Peter Duchemin                         */
/*             |                                                            */
/*==========================================================================*/

import java.lang.*;
import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.bluetooth.*;

public class L2CAPEchoClient implements DiscoveryListener 
{
    // The DiscoveryAgent for the local Bluetooth device.
    private DiscoveryAgent agent;

    // The max number of service searches that can occur at any one time.
    private int maxServiceSearches = 0;

    // The number of service searches that are presently in progress.
    private int serviceSearchCount;

    // Keeps track of the transaction IDs returned from searchServices.
    private int transactionID[];

    // The service record to an echo server that can reply to the message
    // provided at the command line.
    private ServiceRecord record;

    // Keeps track of the devices found during an inquiry.
    private Vector deviceList;

    // The constructor: creates an L2CAPEchoClient object and prepares the object 
    // for device discovery and service searching.
    public L2CAPEchoClient() throws BluetoothStateException 
    {
        // Retrieve the local Bluetooth device object.
        LocalDevice local = LocalDevice.getLocalDevice();

        // Retrieve the DiscoveryAgent object that allows us to perform device
        // and service discovery.
        agent = local.getDiscoveryAgent();

        // Retrieve the max number of concurrent service searches that can
        // exist at any one time.
        try 
        {
            maxServiceSearches = Integer.parseInt( LocalDevice.getProperty("bluetooth.sd.trans.max") );
        } 
        catch( NumberFormatException e ) 
        {
            System.out.println( "General Application Error" );
            System.out.println( "NumberFormatException: " + e.getMessage() );
        }

        transactionID = new int[maxServiceSearches];

        // Initialize the transaction list
        for( int i=0; i<maxServiceSearches; i++ ) 
        {
            transactionID[i] = -1;
        }

        record = null;
        deviceList = new Vector();
    }

    // Adds the transaction table with the transaction ID provided.
    private void addToTransactionTable( int trans ) 
    {
        for( int i=0; i<transactionID.length; i++ ) 
        {
            if( transactionID[i] == -1 ) 
            {
                transactionID[i] = trans;
                return;
            }
        }
    }

    // Removes the transaction from the transaction ID table.
    private void removeFromTransactionTable( int trans ) 
    {
        for( int i=0; i<transactionID.length; i++ ) 
        {
            if( transactionID[i] == trans ) 
            {
                transactionID[i] = -1;
                return;
            }
        }
    }

    // Completes a service search on each remote device in the list until all
    // devices are searched or until an echo server is found that this application
    // can send messages to.
    private boolean searchServices( RemoteDevice[] devList ) 
    {
        UUID[] searchList = new UUID[2];

        // Add the UUID for L2CAP to make sure that the service record
        // found will support L2CAP.  This value is defined in the
        // Bluetooth Assigned Numbers document.
        searchList[0] = new UUID( 0x0100 );

        // Add the UUID for the echo service that we are going to use to
        // the list of UUIDs to search for. (a fictional echo service UUID)
        searchList[1] = new UUID( "00112233445566778899AABBCCDDEEFF", false );

        // Start a search on as many devices as the system can support.
        for( int i=0; i<devList.length; i++ ) 
        {
            System.out.println( "Length = " + devList.length );

            // If we found a service record for the echo service, then
            // we can end the search.
            if( record != null ) 
            {
                System.out.println( "Record is not null" );
                return true;
            }

            try 
            {
                System.out.println( "Starting Service Search on " + devList[i].getBluetoothAddress() );
                int trans = agent.searchServices( null, searchList, devList[i], this );
                System.out.println( "Starting Service Search " + trans );
                addToTransactionTable( trans );
            } 
            catch( BluetoothStateException e ) 
            {
                // Failed to start the search on this device, try another device.
                System.out.println( "BluetoothStateException: " + e.getMessage() );
            }

            // Determine if another search can be started. If not, wait for
            // a service search to end.
            synchronized( this ) 
            {
                serviceSearchCount++;
                System.out.println( "maxServiceSearches = " + maxServiceSearches );
                System.out.println( "serviceSearchCount = " + serviceSearchCount );
                if( serviceSearchCount == maxServiceSearches ) 
                {
                    System.out.println( "Waiting" );
                    try 
                    {
                        this.wait();
                    } 
                    catch( Exception e ) {}
                }
                System.out.println( "Done Waiting " + serviceSearchCount );
            }
        }

        // Wait until all the service searches have completed.
        while( serviceSearchCount > 0 ) 
        {
            synchronized (this) 
            {
                try 
                {
                    this.wait();
                } 
                catch (Exception e) {}
            }
        }

        if( record != null ) 
        {
            System.out.println( "Record is not null" );
            return true;
        } 
        else 
        {
            System.out.println( "Record is null" );
            return false;
        }
    }

    // Finds the first echo server that is available to send messages to.
    public ServiceRecord findEchoServer() 
    {
        // If there are any devices that have been found by a recent inquiry,
        // we don't need to spend the time to complete an inquiry.
        RemoteDevice[] devList = agent.retrieveDevices( DiscoveryAgent.CACHED );
        if( devList != null ) 
        {
            if( searchServices(devList) ) 
            {
                return record;
            }
        }

        // Did not find any echo servers from the list of cached devices.
        // Will try to find an echo server in the list of pre-known devices.
        devList = agent.retrieveDevices( DiscoveryAgent.PREKNOWN );
        if( devList != null ) 
        {
            if( searchServices(devList) ) 
            {
                return record;
            }
        }

        // Did not find an echo server in the list of pre-known or cached
        // devices. So start an inquiry to find all devices that could be 
        // an echo server and do a search on those devices.
        try 
        {
            agent.startInquiry(DiscoveryAgent.GIAC, this);

            // Wait until all the devices are found before trying to start the
            // service search.
            synchronized( this ) 
            {
                try 
                {
                    this.wait();
                } 
                catch (Exception e) {}
            }
        } 
        catch( BluetoothStateException e ) 
        {
            System.out.println( "Unable to find devices to search" );
        }

        if( deviceList.size() > 0 ) 
        {
            devList = new RemoteDevice[deviceList.size()];
            deviceList.copyInto( devList );
            if( searchServices(devList) ) 
            {
                return record;
            }
        }

        return null;
    }

    // This is the main method of this application.
    public static void main(String[] args) 
    {
        L2CAPEchoClient client = null;

        // Validate the proper number of arguments exist when starting this application.
        if( (args == null) || (args.length != 1) ) 
        {
            System.out.println( "usage: java L2CAPEchoClient <message>" );
            return;
        }

        // Create a new EchoClient object.
        try 
        {
            client = new L2CAPEchoClient();
        } 
        catch( BluetoothStateException e ) 
        {
            System.out.println( "Failed to start Bluetooth System" );
            System.out.println( "BluetoothStateException: " + e.getMessage() );
        }

        // Find an Echo Server in the local area
        ServiceRecord echoService = client.findEchoServer();

        if( echoService != null ) 
        {
            // retrieve the connection URL string
            String conURL = echoService.getConnectionURL( ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false );

            // create a new client instance
            EchoClient echoClient = new EchoClient( conURL );

            // and send the message give on the command line    
            echoClient.sendMessage( args[0] );
        }
        else 
        {
            System.out.println( "No Echo Server was found" );
        }
    }

    // Called when a device was found during an inquiry.  An inquiry
    // searches for devices that are discoverable.  The same device may
    // be returned multiple times.
    public void deviceDiscovered( RemoteDevice btDevice, DeviceClass cod ) 
    {
        System.out.println( "Found device = " + btDevice.getBluetoothAddress() );
        deviceList.addElement( btDevice );
    }

    // The following method is called when a service search is completed or
    // was terminated because of an error. Legal values include:
    // SERVICE_SEARCH_COMPLETED, SERVICE_SEARCH_TERMINATED,
    // SERVICE_SEARCH_ERROR, SERVICE_SEARCH_DEVICE_NOT_REACHABLE 
    // and SERVICE_SEARCH_NO_RECORDS
    public void serviceSearchCompleted( int transID, int respCode ) 
    {
        System.out.println( "serviceSearchCompleted(" + transID + ", " + respCode + ")" );

        // Removes the transaction ID from the transaction table.
        removeFromTransactionTable( transID );

        serviceSearchCount--;

        synchronized( this ) 
        {
            this.notifyAll();
        }
    }

    // Called when service(s) are found during a service search.
    // This method provides the array of services that have been found.
    public void servicesDiscovered( int transID, ServiceRecord[] servRecord ) 
    {
        // If this is the first record found, then store this record
        // and cancel the remaining searches.
        if( record == null ) 
        {
            System.out.println( "Found a service " + transID );
            System.out.println( "Length of array = " + servRecord.length );
            if( servRecord[0] == null ) 
            {
                System.out.println( "The service record is null" );
            }
            record = servRecord[0];
            if( record == null ) 
            {
                System.out.println( "The second try was null" );
            }

            // Cancel all the service searches that are presently
            // being performed.
            for( int i=0; i<transactionID.length; i++ ) 
            {
                if( transactionID[i] != -1 ) 
                {
                    System.out.println( agent.cancelServiceSearch(transactionID[i]) );
                }
            }
        }
    }

    // Called when a device discovery transaction is
    // completed. The <code>discType</code> will be
    // INQUIRY_COMPLETED if the device discovery transactions ended normally,
    // INQUIRY_ERROR if the device discovery transaction failed to complete normally,
    // INQUIRY_TERMINATED if the device discovery transaction was canceled by calling
    // DiscoveryAgent.cancelInquiry().
    public void inquiryCompleted( int discType ) 
    {
        synchronized( this ) 
        {
            try 
            {
                this.notifyAll();
            } 
            catch (Exception e) {}
        }
    }
}

// The EchoClient will make a connection using the connection string
// provided and send a message to the server to print the data sent.
class EchoClient 
{
    // Keeps the connection string in case the application would like to make
    // multiple connections to an echo server.
    private String serverConnectionString;

    // The constructor: creates an EchoClient object that will allow an 
    // application to send multiple messages to an echo server.
    EchoClient( String server ) 
    {
        serverConnectionString = server;
    }

    // Sends a message to the server.  
    public boolean sendMessage( String msg ) 
    {
        L2CAPConnection con = null;
        byte[] data = null;
        int index = 0;
        byte[] temp = null;

        try 
        {
            // Create a connection to the server
            con = (L2CAPConnection)Connector.open( serverConnectionString );

            // Determine the maximum amount of data I can send to the server.
            int MaxOutBufSize = con.getTransmitMTU();
            temp = new byte[MaxOutBufSize];

            // Send as many packets as are needed to send the data
            data = msg.getBytes();

            while( index < data.length ) 
            {
                // Determine if this is the last packet to send or if there
                // will be additional packets
                if( (data.length - index) < MaxOutBufSize ) 
                {
                    temp = new byte[data.length - index];
                    System.arraycopy( data, index, temp, 0, data.length-index );
                } 
                else 
                {
                    temp = new byte[MaxOutBufSize];
                    System.arraycopy( data, index, temp, 0, MaxOutBufSize );
                }
                con.send(temp);
                index += MaxOutBufSize;
            }

            // Prepare a receive buffer
            int rxlen = con.getReceiveMTU();
            byte[] rxdata = new byte[rxlen];

            // Wait to receive the server's reply (method blocks!)
            rxlen = con.receive( rxdata );

            // Here, we've got it
            String message = new String( rxdata, 0, rxlen );
            System.out.println( "Server replied: " + message );

            // Close the connection to the server
            con.close();
        } 
        catch( BluetoothConnectionException e ) 
        {
            System.out.println( "Failed to send the message" );
            System.out.println( "BluetoothConnectionException: " + e.getMessage() );
            System.out.println( "Status: " + e.getStatus() );
        } 
        catch( IOException e ) 
        {
            System.out.println( "Failed to send the message" );
            System.out.println( "IOException: " + e.getMessage() );
            return false;
        }

        return true;
    }
}


