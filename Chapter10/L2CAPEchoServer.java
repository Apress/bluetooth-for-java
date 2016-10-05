/*==========================================================================*/
/*             |                                                            */
/* Filename    | L2CAPEchoServer.java                                       */
/*             |                                                            */
/* Purpose     | This demo application will start an L2CAP service that     */
/*             | will accept connections from remote L2CAP clients,         */
/*             | receive data from them and echo it back on the same link.  */
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
import javax.microedition.io.*;
import javax.bluetooth.*;

public class L2CAPEchoServer 
{
    static ClientProcess Client = null;

    public static void main(String[] args) 
    {
        L2CAPConnectionNotifier Server = null;

        try 
        {
            LocalDevice local = LocalDevice.getLocalDevice();
            local.setDiscoverable( DiscoveryAgent.GIAC );
        } 
        catch (BluetoothStateException e) 
        {
            System.err.println( "Failed to start service" );
            System.err.println( "BluetoothStateException: " + e.getMessage() );
            return;
        }

        try 
        {
            // start the echo server (with a fictional UUID)
            Server = (L2CAPConnectionNotifier)Connector.open( "btl2cap://localhost:00112233445566778899AABBCCDDEEFF" );
        } 
        catch (IOException e) 
        {
            System.err.println( "Failed to start service" );
            System.err.println( "IOException: " + e.getMessage() );
            return;
        }

        System.out.println( "Starting L2CAP Echo Server" );

        // This server actually runs forever. However, it can be stopped
        // by terminating the KVM from the command line
        // The server can terminate client connections by 
        // setting the client connections public variable "end" to "true"
        // like: L2CAPEchoServer.Client.end = true;
        while( true ) 
        {
            L2CAPConnection conn = null;

            try 
            {
                // wait for incoming client connections (blocking method)
                conn = Server.acceptAndOpen();

                // here we've got one, start it in a separate thread
                L2CAPEchoServer.Client = new ClientProcess( conn );
                L2CAPEchoServer.Client.start(); 
            } 
            catch (IOException e) 
            {
                System.out.println("IOException: " + e.getMessage());
            } 
        }
    }
}

class ClientProcess extends Thread
{
    static L2CAPConnection clientconn;
    public boolean         end;

    // the constructor
    ClientProcess( L2CAPConnection conn )
    {
       this.clientconn = conn;
       this.end = false;
    }

    // start the communication with the client
    public void run()
    {
        byte[] data = null;
        int length;

        System.out.println( "Client is connected" );

        while( !end )
        {
            try
            {
                // prepare a receive buffer
                length = clientconn.getReceiveMTU();
                data = new byte[length];

                // read in the data sent by the client (method blocks!)
                length = clientconn.receive(data);
                System.out.println( "Received " + length + " bytes from client" );

                // and immediately send it back on the same connection (echo)
                clientconn.send(data);
            }
            catch( IOException e )
            {
                System.out.println("IOException: " + e.getMessage());
            }
        }
        try
        {
            clientconn.close();
        }
        catch( IOException e )
        {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
