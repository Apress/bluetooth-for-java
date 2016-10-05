/*==========================================================================*/
/*             |                                                            */
/* Filename    | SPP2COMM.java                                              */
/*             |                                                            */
/* Purpose     | This demo application will perform device and service      */
/*             | discovery in order to find a peer SPP device upon          */
/*             | reception of data on the serial port. At the same time     */
/*             | an SPP server will wait to be connected from another       */
/*             | SPP client device. The Bluetooth connection will be        */
/*             | established in either way, whichever occurs first.         */
/*             | Data is send from the serial port to the BT SPP            */
/*             | connection and vice versa.                                 */
/*             |                                                            */
/* Remarks     | Needs Java API for Bluetooth (JSR-82) to operate.          */ 
/*             |                                                            */
/* Copyright   | (c) 2002, Smart Network Devices GmbH, Germany              */
/*             |                                                            */
/* Created     | July 27, 2002                                              */
/*             |                                                            */
/* Last Change | October 21, 2002 by Peter Duchemin                         */
/*             |                                                            */
/*==========================================================================*/

import java.lang.*;
import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.bluetooth.*;

public class SPP2COMM implements DiscoveryListener 
{
    // The connection to the serial port
    static StreamConnection serialport = null;

    // The Input/Output streams to the local serial port
    static OutputStream ser_out = null;
    static InputStream  ser_in = null;

    // The Bluetooth connection to the peer device
    static StreamConnection bluetoothport = null;

    // The Input/Output streams to the Bluetooth connection
    static OutputStream bt_out = null;
    static InputStream  bt_in = null;

    // The DiscoveryAgent for the local Bluetooth device.
    private DiscoveryAgent agent;

    // The max number of service searches that can occur at any one time.
    private int maxServiceSearches = 0;

    // The number of service searches that are presently in progress.
    private int serviceSearchCount;

    // Keeps track of the transaction IDs returned from searchServices.
    private int transactionID[];

    // The service record to an cable replacement service
    private ServiceRecord record;

    // Keeps track of the devices found during an inquiry.
    private Vector deviceList;

    // The constructor: creates an SPP2COMM and prepares the object 
    // for device discovery and service searching.
    public SPP2COMM() throws BluetoothStateException 
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
    // devices are searched or until a cable replacement peer is found that this 
    // application can connect to.
    private boolean searchServices( RemoteDevice[] devList ) 
    {
        UUID[] searchList = new UUID[2];

        // Add the UUID for L2CAP to make sure that the service record
        // found will support L2CAP.  This value is defined in the
        // Bluetooth Assigned Numbers document.
        searchList[0] = new UUID( 0x0100 );

        // Add the UUID for the cable replacement service that we are going to use to
        // the list of UUIDs to search for. (a fictional cable replacement service UUID)
        searchList[1] = new UUID( "FFEEDDCCBBAA998877665544332211", false );

        // Start a search on as many devices as the system can support.
        for (int i = 0; i < devList.length; i++) 
        {
            System.out.println( "Length = " + devList.length );

            // If we found a service record for the cable replacement service, then
            // we can end the search.
            if (record != null) 
            {
                System.out.println( "Record is not null" );
                return true;
            }

            try 
            {
                System.out.println( "Starting Service Search on " + devList[i].getBluetoothAddress() );
                int trans = agent.searchServices(null, searchList, devList[i], this );
                System.out.println( "Starting Service Search " + trans );
                addToTransactionTable( trans );
            } 
            catch (BluetoothStateException e) 
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

    // Finds the first cable replacement peer that is available to connect to.
    public ServiceRecord findCableReplacementService() 
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

        // Did not find any cable replacement peer from the list of cached devices.
        // Will try to find an cable replacement peer in the list of pre-known devices.
        devList = agent.retrieveDevices( DiscoveryAgent.PREKNOWN );
        if( devList != null ) 
        {
            if( searchServices(devList) ) 
            {
                return record;
            }
        }

        // Did not find an cable replacement peer in the list of pre-known or cached
        // devices. So start an inquiry to find all devices that could be 
        // an cable replacement peer and do a search on those devices.
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
        SPP2COMM client = null;
        SppServerProcess server = null;
        int baudrate;

        // Validate the proper number of arguments exist when starting this application.
        if( (args == null) || (args.length != 1) ) 
        {
            System.out.println( "usage: java SPP2COMM <baudrate>" );
            return;
        }

        // Create a new SPP2COMM object.
        try 
        {
            client = new SPP2COMM();
        } 
        catch( BluetoothStateException e ) 
        {
            System.out.println( "Failed to start Bluetooth System" );
            System.out.println( "BluetoothStateException: " + e.getMessage() );
        }

        // get the baudrate for the serial port from the command line
        baudrate = Integer.parseInt( args[0] );

        // make the connection to the serial port
        try {
            // get the connection
            serialport = (StreamConnection)Connector.open( "comm:1;baudrate=" + baudrate, Connector.READ_WRITE, true );
        }
        catch( Exception e ) 
        {
            System.out.println( "serial port open exception: " + e );
            System.exit( 0 );
        }

        // open the serial port's output stream
        try 
        {
            ser_out = serialport.openOutputStream();
        }
        catch( Exception e ) 
        {
            System.out.println( "serial output stream open exception: " + e );
            System.exit( 0 );
        }

        // open the serial port's input stream
        try 
        {
            ser_in = serialport.openInputStream();
        }
        catch( Exception e ) 
        {
            System.out.println( "serial input stream open exception: " + e );
            System.exit( 0 );
        }

        // Create a new SPP server object.
        try 
        {
            server = new SppServerProcess();
            server.start();
        } 
        catch( Exception e ) 
        {
            System.out.println( "Failed to start Spp Server" + e );
            System.exit( 0 );
        }

        // the main loop runs forever. However, it can be stopped
        // by terminating the KVM from the command line
        while( true )
        {
            // Create buffer to receive data from the serial port
            byte[] rxdata = new byte[64];
            int    rxlen=0;
            int    data;

            try 
            {
                // read in as many bytes from the serial port
                // as currently available but do not exceed the
                // current buffer length.
                // The read() method blocks but is periodically released
                // by an InterruptedIOException in order to allow other
                // things to happen meanwhile
                while( true ) 
                {
                    data = ser_in.read();
                    rxdata[rxlen] = (byte)data;
                    rxlen++;
                    if( rxlen >= 64 || data == -1 )
                        break;
                }
                System.out.println( "data received from serial port, len=" + rxlen );

            }
            catch( InterruptedIOException e ) 
            {
                System.out.println( "serial port receive timeout: " + e );
            }
            catch( Exception e ) 
            {
                System.out.println( "serial port receive exception: " + e );
            }

            // Did we get any data from the serial port?
            if( rxlen > 0 )
            {
                // Do we have a Bluetooth connection already?
                if( bluetoothport != null )
                {
                    // Do we have an OutputStream on the BT connection already?
                    if( bt_out == null )
                    {
                        // no, then create one
                        try 
                        {
                            bt_out = bluetoothport.openOutputStream();
                        }
                        catch( Exception e ) 
                        {
                            System.out.println( "Bluetooth output stream open exception: " + e );
                        }
                    }
                    System.out.println( "send serial data on Bluetooth link" );
                    try
                    {
                        bt_out.write( rxdata );
                        bt_out.flush();
                    }
                    catch( Exception e ) 
                    {
                        System.out.println( "Bluetooth output stream write exception: " + e );
                    }
                }
                else
                {
                    System.out.println( "No Bluetooth link: try to establish one..." );

                    // Find a cable replacement service in the local area
                    ServiceRecord cableReplacementService = client.findCableReplacementService();

                    if( cableReplacementService != null ) 
                    {
                        // retrieve the connection URL string
                        String conURL = cableReplacementService.getConnectionURL( ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false );
            
                        try
                        {
                            // Create a connection to the SPP peer
                            bluetoothport = (StreamConnection)Connector.open( conURL );
                        }
                        catch( Exception e )
                        {
                            System.out.println( "Failed to establish Bluetooth link: " + e );
                        }

                        if( bluetoothport != null )
                        {
                            try 
                            {
                                // open an OutputStream on the Bluetooth connection
                                bt_out = bluetoothport.openOutputStream();
                            }
                            catch( Exception e ) 
                            {
                                System.out.println( "Bluetooth output stream open exception: " + e );
                            }

                            // and send the data from the serial port
                            System.out.println( "send serial data on Bluetooth link" );
                            try
                            {
                                bt_out.write( rxdata );
                                bt_out.flush();
                            }
                            catch( Exception e ) 
                            {
                                System.out.println( "Bluetooth output stream write exception: " + e );
                            }
                        }
                    }
                    else 
                    {
                        System.out.println( "No SPP peer found" );
                    }
                }
            }

            // do we have a Bluetooth connection already?
            if( bluetoothport != null )
            {
                // do we have an InputStream on the Bluetooth connection already?
                if( bt_in == null )
                {
                    // no, then create one
                    try 
                    {
                        bt_in = bluetoothport.openInputStream();
                    }
                    catch( Exception e ) 
                    {
                        System.out.println( "Bluetooth output stream open exception: " + e );
                    }
                }

                // listen on the bluetooth connection
                rxlen = 0;
                try 
                {
                    // read in as many bytes from the serial port
                    // as currently available but do not exceed the
                    // current buffer length.
                    // The read() method blocks but is periodically released
                    // by an InterruptedIOException in order to allow other
                    // things to happen meanwhile
                    while( true ) 
                    {
                        data = bt_in.read();
                        rxdata[rxlen] = (byte)data;
                        rxlen++;
                        if( rxlen >= 64 || data == -1 )
                            break;
                    }
                    System.out.println( "data received from bluetooth port, len=" + rxlen );

                }
                catch( InterruptedIOException e ) 
                {
                    System.out.println( "Bluetooth port receive timeout: " + e );
                }
                catch( Exception e ) 
                {
                    System.out.println( "Bluetooth port receive exception: " + e );
                }

                try
                {
                    System.out.println( "send Bluetooth data on serial link" );
                    ser_out.write( rxdata );
                    ser_out.flush();
                }
                catch( Exception e )
                {
                    System.out.println( "Bluetooth output stream write exception: " + e );
                }
            }
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
            System.out.println( "After this" );
            if( record == null ) 
            {
                System.out.println( "The Seocnd try was null" );
            }

            // Cancel all the service searches that are presently
            // being performed.
            for( int i=0; i<transactionID.length; i++ ) 
            {
                if( transactionID[i] != -1 ) 
                {
                    System.out.println(agent.cancelServiceSearch(transactionID[i]));
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

class SppServerProcess extends Thread
{
    /* the constructor */
    SppServerProcess()
    {
    }

    public void run() 
    {
        StreamConnectionNotifier Server = null;

        try 
        {
            LocalDevice local = LocalDevice.getLocalDevice();
            local.setDiscoverable( DiscoveryAgent.GIAC );
        } 
        catch( BluetoothStateException e ) 
        {
            System.err.println( "Failed to start service" );
            System.err.println( "BluetoothStateException: " + e.getMessage() );
            return;
        }

        try 
        {
            // start the SPP server (with a fictional UUID)
            Server = (StreamConnectionNotifier)Connector.open( "btspp://localhost:FFEEDDCCBBAA99887766554433221100" );
        } 
        catch( IOException e ) 
        {
            System.err.println( "Failed to start service" );
            System.err.println( "IOException: " + e.getMessage() );
            return;
        }

        System.out.println( "Starting SPP Server" );

        while( true ) 
        {
            // accept connections only if we are not yet connected
            if( SPP2COMM.bluetoothport == null )
            {
                try 
                {
                    // wait for incoming client connections (blocking method)
                    SPP2COMM.bluetoothport = Server.acceptAndOpen();
                } 
                catch( IOException e ) 
                {
                    System.out.println("IOException: " + e.getMessage());
                } 
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch( Exception e ) { }
        }
    }
}

