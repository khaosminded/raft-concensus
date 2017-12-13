/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raft;

import RMI.Leader;
import TCP.Server;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 *
 * @author hanxinlei
 */
public class GenericNode {
    /**
     * entrance for the followings:
     * @1. mbpServer
     * @2. TcpServer with RaftService
     * @3. Client
     */
    void main(String [] args) throws UnknownHostException, IOException
    {
        
        //@2. TcpServer with RaftService
        Server server=new Server(8080, "localhost", 4410); 
        Leader raftHandle=new RaftService(server.initMbpList()).runRaftService();
        server.setRaftHandle(raftHandle);
        server.runServer();
    }
}
