/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raft;

import RMI.Leader;
import TCP.Client;
import TCP.MbpServer;
import TCP.Server;
import java.io.IOException;
import java.net.UnknownHostException;
import static raft.Protocol.Operation.*;
import static raft.Protocol.TYPE;

/**
 *
 * @author hanxinlei
 */
public class GenericNode {

    static String addr;
    static String port;
    static String mbpAddr;
    static final int mbpPort = 4410;
    static int portNumber;
    static Protocol.Operation opt;
    static String key;
    static String value;
    static boolean wc = false;
    /**
     *
     */
    static MbpServer mbpServer;
    static Server server;

    /**
     * entrance for the followings:
     *
     * @1. mbpServer
     * @2. TcpServer with RaftService
     * @3. Client
     */
    public static void main(String[] args) throws UnknownHostException, IOException {

        System.err.close();

        if (args.length < 1) {
            System.out.println("=Wrong command!=\n"
                    + "");
            help();
        } else {
            if (args[0].equals(Protocol.MBPS_ROLE)) {
                mbpServer = new MbpServer(mbpPort);
                mbpServer.runMbpServer();

            } else if (args[0].equals(Protocol.SERVER_ROLE)) {
                switch (args.length) {
                    case 3:
                        portNumber = Integer.parseInt(args[1]);
                        mbpAddr = args[2];
                        //@2. TcpServer with RaftService
                        //almost finished Dec.13 3:42
                        Server server = new Server(portNumber, mbpAddr, mbpPort);
                        Leader raftHandle = new RaftService(server.initMbpList()).runRaftService();
                        server.setRaftHandle(raftHandle);
                        server.runServer();

                        break;
                    default:
                        System.out.println("Server Wrong command!");
                        help();
                        return;
                }

            } else if (args[0].equals(Protocol.CLIENT_ROLE)) {
                switch (args.length) {
                    case 4:
                        addr = args[1];
                        port = args[2];
                        if (EXIT.name().equalsIgnoreCase(args[3])) {
                            opt = EXIT;
                        } else if (STORE.name().equalsIgnoreCase(args[3])) {
                            opt = STORE;
                        } else if (LOG.name().equalsIgnoreCase(args[3])) {
                            opt = LOG;
                        } else {
                            wc = true;
                        }
                        break;
                    case 5:
                        addr = args[1];
                        port = args[2];
                        key = args[4];
                        if (GET.name().equalsIgnoreCase(args[3])) {
                            opt = GET;
                        } else if (DEL.name().equalsIgnoreCase(args[3])) {
                            opt = DEL;
                        } else {
                            wc = true;
                        }
                        break;
                    case 6:
                        addr = args[1];
                        port = args[2];
                        key = args[4];
                        value = args[5];
                        if (PUT.name().equalsIgnoreCase(args[3])) {
                            opt = PUT;
                        } else if (DEL.name().equalsIgnoreCase(args[3])) {
                            opt = DEL;
                        } else {
                            wc = true;
                        }
                        break;
                    default:
                        wc = true;
                        break;

                }
                if (wc) {
                    System.out.println("Client Wrong command!");
                    help();
                    return;
                }
                Client client = new Client(addr, Integer.parseInt(port), opt, TYPE.CLIENT);
                client.runClient(key, value);
            }
        }

    }

    static private void help() {
        System.out.println("Launch Client:\n"
                + "tc <host> <port> [put <key> <val>|get <key>|del <key>|store|exit]\n\n"
                + "Launch Server with RAFT service:\n"
                + "ts <port2listen> <mbpAddress>\n\n"
                + "Launch Membership Server:\n"
                + "mb\n\n");
    }
}
