package com.ericsson.blipnet.samples;

import com.ericsson.blipnet.api.event.*;
import com.ericsson.blipnet.api.blipserver.*;
import com.ericsson.blipnet.api.bluetooth.BluetoothAddress;
import com.ericsson.blipnet.api.blipserver.BlipNode;

import java.util.Hashtable;

/**
 * This Tracking application demonstrates BlipNets ability to track the movement of Bluetooth terminals/devices.
 * At least two BlipNodes must be attached to BlipNet for the Tracking application to make sense (else movement cannot
 * be seen). The BlipNodes should be placed so that they will not discover the same terminal at the same time.
 * If a terminal can be discovered by more than one BlipNode it will seem that the terminal moves back-and-forth between
 * the BlipNodes.
 * The BlipNodes being used for tracking must all be configured with the "Discover Devices" setting via BlipManager.
 * That way the BlipNodes will only perform inquiry (device discovery).
 * Each time a BlipNode discovers a device an Event is sent to this Tracking application, which prints out whether
 * the device is new on the system, or whether it has moved.
 * The application must be started with a group name as commandline parameter. The group must be defined in BlipManager,
 * and must contain at least two BlipNods (but can contain an infinite number of BlipNodes).
 * Optionally a number of devices to discover can be entered as commandline parameters (TerminalIDs).
 * Starting the application without parameters or with invalid parameters displays a help text.
 * The application requires that a user has been created via the BlipManager (in BlipServer Properties under Applications)
 * The username, password must be: Tracking, Trakcing (as seen underneath).
 * The application requires that the BlipServer runs on localhost, if not please edit.
 */
public class Tracking {
    private BlipServerConnection blipServerConnection;
    private Hashtable terminalLastSeenOnThisBlipNode = new Hashtable();

    public Tracking(String discoverBlipNodeGroup, 
                    BluetoothAddress[] terminalsToTrack) {

        // Get a connection to the server
        initBlipServerConnection();

        // We are only interested in TERMINAL_DETECTED events from 
        // the BlipNodes mentioned at startup.
        // If any terminals have been specified at startup, only events 
        // from them will be seen here.
        // Therefore a filter is created.
        BlipServerEventFilter blipServerEventFilter = 
                    getEventFilter(discoverBlipNodeGroup, terminalsToTrack);

        try {
            // Register the event Listener with the generated filter
            blipServerConnection.addEventListener(new TrackingEventListener(),
                                                  blipServerEventFilter);
        } catch (BlipServerConnectionException e) {
            System.out.println("Error attaching listener");
            e.printStackTrace();
            System.exit(-1);
        }

    }

    private void initBlipServerConnection() {
        try {
            // Username, password must be created in BlipManager 
            // under BlipServer Properties, Application
            // BlipServer must run on localhost - if not please edit 
            // to specify correct server.
            blipServerConnection = BlipServer.getConnection("Tracking",
                                                     "Tracy", "localhost");
        } catch (BlipServerConnectionException e) {
            System.out.println("Error connecting to server");
            e.printStackTrace();
            System.exit(-1);
        } catch (BlipServerAccessException e) {
            e.printStackTrace();
            System.out.println("Error registering user - Have You created " +
                  "a username/password for this application in BlipManager?");
            System.exit(-1);
        }
    }

    private BlipServerEventFilter getEventFilter(String discoverBlipNodeGroup,
                                               BluetoothAddress[] terminals) {
        // List of BlipNodeIds used for tracking - is built from input 
        // in-line parameters entered at start up of Tracking application.
        BluetoothAddress[] blipNodeAddressList = null;

        BlipNode[] inquiryOnlyBlipNodes = null;
        try {
            inquiryOnlyBlipNodes = blipServerConnection.getBlipNodes
                    (discoverBlipNodeGroup, "Discover Devices", false, false);
        } catch (BlipServerConnectionException e) {
            System.out.println("Could not get BlipNode handles " + 
                "for the BlipNode Group: "+ discoverBlipNodeGroup + "\n" + e);
            System.exit(-1);
        }

        // Are there any BlipNodes in the specified group ?
        if (inquiryOnlyBlipNodes.length > 1) {
            blipNodeAddressList = 
                   new BluetoothAddress[inquiryOnlyBlipNodes.length];
            for (int i = 0; i < blipNodeAddressList.length; i++) {
                blipNodeAddressList[i] = 
                       inquiryOnlyBlipNodes[i].getBlipNodeID();
            }
        } else {
            System.out.println("Have You inserted at least 2 BlipNodes " +
                         "in the group (" + discoverBlipNodeGroup + ") ?");
            usage();
        }

        System.out.println("BlipNodes used for tracking (from group '" +
                            discoverBlipNodeGroup + "'):");
        for (int i=0; i<blipNodeAddressList.length; i++) {
            System.out.println("* " + 
                      blipNodeAddressList[i].toString().toUpperCase());
        }

        if (null != terminals) {
            System.out.println("\nTerminals being tracked: ");
            for (int i=0; i<terminals.length; i++) {
                System.out.println("* " + 
                      terminals[i].toString().toUpperCase());
            }
        } else {
            System.out.println("* Tracking all discoverable devices.");
        }
        System.out.println("----------------------------------\n");


        return new BlipServerEventFilter(null, 
                                         new int[] {Event.TERMINAL_DETECTED},
                                         blipNodeAddressList, terminals);
    }

    private class TrackingEventListener extends BlipServerEventAdapter {
        public void handleConnectionEvent(ConnectionEvent e) {
            switch (e.getEventID()) {
                case Event.TERMINAL_DETECTED:
                    BluetoothAddress terminalID = e.getTerminalID();
                    BluetoothAddress blipNodeID = e.getBlipNodeID();
                    if (terminalLastSeenOnThisBlipNode.containsKey(terminalID)) {
                        // Terminal has already been discovered before,
                        // so has it moved?
                        if (!terminalLastSeenOnThisBlipNode.
                                            get(terminalID).equals(blipNodeID)) {
                            System.out.println("Terminal: " + terminalID + " (" +
                                            e.getTerminalFriendlyName() +
                                            ") moved from BlipNode: " + 
         ((BluetoothAddress) terminalLastSeenOnThisBlipNode.remove(terminalID)) +
                                            " to BlipNode:" + blipNodeID);
                            terminalLastSeenOnThisBlipNode.put(terminalID,
                                                               blipNodeID);
                        } else {
                            // Terminal stays on the same BlipNode. 
                            // Do not do anything.
                        }
                    } else {
                        // This is the first this terminal is seen on the system
                        System.out.println("Terminal: " + terminalID + " (" + 
                                           e.getTerminalFriendlyName() +
                                           ") discovered for the first time on" +
                                           " BlipNode: " + blipNodeID);
                        terminalLastSeenOnThisBlipNode.put(terminalID,
                                                           blipNodeID);
                    }
                    break;
                default:
                    System.out.println("Error - only TERMINAL_DETECTED " +
                                       "events should be received! \nReceived " +
                                       "event:" + 
                                       Event.FRIENDLY_NAMES[e.getEventID()]);
            }
        }
    }

    private static BluetoothAddress[] parseTerminalList(final String[] args) {
        int numberOfTerminals = args.length - 1;

        // List of BlipNodeIds used for tracking - is built from 
        // input in-line parameters entered at start up of Tracking application.
        BluetoothAddress[] trackTheseTerminals = null;

        if (numberOfTerminals > 0) {
            trackTheseTerminals = new BluetoothAddress[numberOfTerminals];
            for (int inputParameterCount=0; inputParameterCount < 
                                   numberOfTerminals; inputParameterCount++) {
                try {
                    // Make sure it is a valid TerminalID (BluetoothAddress)
                    trackTheseTerminals[inputParameterCount] = 
                               new BluetoothAddress(args[inputParameterCount+1]);
                } catch (IllegalArgumentException iae) {
                    System.out.println("TerminalId: " + 
                                       args[inputParameterCount] + 
                                       " is invalid. A valid id, e.g. " +
                                       "112233445566\n" + iae);
                    usage();
                }
            }
        }
        return trackTheseTerminals;
    }

    private static void usage() {
        System.out.println("The tracking application requires at least 2 " +
                           "BlipNodes, please use BlipManager to specify the " +
                           "BlipNodeIds in the group.");
        System.out.println("Specify the group name as first input parameter:");
        System.out.println("> Tracking MyGroup");
        System.out.println("Thereby the BlipNodes (specified in the " +
                           "BlipManager) in the group 'MyGroup' will be used. " +
                           "These BlipNodes must be");
        System.out.println("configured as 'Inquiry Only' BlipNodes. Use at " +
                           "least two BlipNodes in the group.");
        System.out.println("When no Terminal Ids are specified all " +
                           "discoverable devices will be tracked.");
        System.out.println("-------------------------------------");
        System.out.println("If only specific terminal is to be tracked, the " +
                           "Terminal Ids can be specified after the group " +
                           "name, e.g.:");
        System.out.println("> Tracking MyGroup 001122334455 000102030405");
        System.out.println("Thereby the same BlipNodes as above be used " +
                           "for tracking,");
        System.out.println("and only the terminals with Ids 001122334455 " +
                           "000102030405 will be tracked (terminal list can " + 
                           "be continued).");
        System.out.println("-------------------------------------");
        System.out.println("In BlipManager a username/password pair must be " +
                           "defined for the Tracking-application. Under " +
                           "'BlipServer Properties',");
        System.out.println("'Applications'; Create a new user with " + 
                           "username/password: Tracking/Tracking.");
        System.exit(-1);
    }

    /**
     * @param args - GroupName specifying the BlipNodes to use for tracking (shall be set to InquryModeOnly in BlipManager).
     * after GroupName a list af TerminalIds (BluetoothAddresses) can be specified, so only these terminals will be tracked.
     * If no terminals are specified, all terminals are tracked.
     */
    public static void main(String[] args) {
        // Must specify at least a BlipNode Group
        if (args.length<1) {
            usage();
        }

        BluetoothAddress[] trackTheseTerminals = parseTerminalList(args);

        System.out.println("** Starting Tracking application **");
        System.out.println("-----------------------------------");

        Tracking tracker = new Tracking(args[0], trackTheseTerminals);

        System.out.println("Tracking application started");

    }

}
