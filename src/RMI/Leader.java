package RMI;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import raft.Protocol.RAFT;

public class Leader extends Candidate {

    /**
     * Volatile state on leaders:(Reinitialized after election)
     *
     * @nextIndex for each server, index of the next log entry to send to that
     * server (initialized to leader last log index + 1)
     * @matchIndex for each server, index of highest log entry known to be
     * replicated on server (initialized to 0, increases monotonically)
     */
    ArrayList nextIndex;
    ArrayList matchIndex;
    //set timer
    static private Timer timer;
    static final private int heartBeatDelay = 200;

    public Leader(ArrayList<InetSocketAddress> mbpList) {
        super(mbpList);
        nextIndex = new ArrayList();
        matchIndex = new ArrayList();
    }

    private void broadCast() {
        for (int i = 0; i < mbpList.size(); i++) {
            if (i == id) {
                continue;
            }
            callAppendEntries call = new callAppendEntries(mbpList.get(i), i);
            call.start();
        }
    }
    private void startHeartTimer()
    {
        timer=new Timer(heartBeatDelay);
        timer.start();
    }
    private void endHeartTimer()
    {
        timer.interrupt();
        try {
            timer.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Leader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private class Timer extends Thread {

        int sleeptime;

        public Timer(int sleeptime) {
            this.sleeptime = sleeptime;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleeptime);
                System.out.println("I'm leader. <heartbeat>...");
            } catch (InterruptedException ex) {
                Logger.getLogger(Leader.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            broadCast();
        }
    }

    private class callAppendEntries extends Thread {

        InetSocketAddress host;
        int hostid;

        public callAppendEntries(InetSocketAddress host, int hostid) {
            this.host = host;
            this.hostid = hostid;
        }

        @Override
        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(host.getHostString());
                RMIinterface stub = (RMIinterface) registry.lookup("raftFollower");
                //TODO deal with AppendEntries
                //stub.AppendEntries(currentTerm, hostid, commitIndex, currentTerm, nextIndex, MIN_PRIORITY)
                 
            } catch (RemoteException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotBoundException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private void initIndexes()
    {
        nextIndex[];
        matchIndex[];
    }
    public void run() {
        while (getState() == RAFT.LEADER) {
            startHeartTimer();
            
            //recieve Operation opt
            log.add(opt);
            respond(commit(){/**respond**/});
            
            
            try {
                timer.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Leader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
