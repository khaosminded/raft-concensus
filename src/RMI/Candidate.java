package RMI;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import raft.Protocol.RAFT;

public class Candidate extends Follower {

    static volatile ArrayList<InetSocketAddress> mbpList = new ArrayList();
    static ArrayList<Boolean> votePool = new ArrayList();
    static ArrayList<Long> termPool = new ArrayList();
    static private Timer timer;

    public Candidate(ArrayList<InetSocketAddress> mbpList) {
        super();
        setMbpList(mbpList);

    }

    private void broadCast() {
        for (int i = 0; i < mbpList.size(); i++) {
            if (i == id) {
                continue;
            }
            callRequestVote call = new callRequestVote(mbpList.get(i), i);
            call.start();
        }
    }

    //the only entrance of member list
    public final synchronized void setMbpList(ArrayList<InetSocketAddress> mbpList) {
        votePool.clear();
        this.mbpList.clear();
        termPool.clear();

        this.mbpList.addAll(mbpList);
        for (int i = 0; i < mbpList.size(); i++) {
            try {
                if (mbpList.get(i).getHostString().
                        equals(InetAddress.getLocalHost())) {
                    //id is '0'based;
                    id = i;
                }
            } catch (UnknownHostException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            }
            votePool.add(false);
            termPool.add(currentTerm);
        }
    }

    private void startElectionTimer() {
        int period;
        period = (int) (Math.random() * (interval[1] - interval[0]) + interval[0]);
        timer = new Timer(period);
        timer.start();
    }

    private void endElectionTimer() {
        timer.interrupt();
        try {
            timer.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Follower.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class Timer extends Thread {

        int sleeptime;

        public Timer(int sleeptime) {
            super();
            this.sleeptime = sleeptime;
        }

        @Override
        public void run() {

            try {
                Thread.sleep(sleeptime);
                System.out.println("electionTimer restart..");
                //timeout!!

            } catch (InterruptedException ex) {
                Logger.getLogger(Timer.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("electionTimer interrupt..");
                return;
            }
        }

    }

    private class callRequestVote extends Thread {

        InetSocketAddress host;
        int hostid;

        public callRequestVote(InetSocketAddress host, int hostid) {
            this.host = host;
            this.hostid = hostid;
        }

        @Override
        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(host.getHostString());
                RMIinterface stub = (RMIinterface) registry.lookup("raftFollower");
                int lastLogIndex = log.size() > 0 ? log.size() - 1 : 0;
                long lastLogTerm = log.size() > 0 ? log.get(log.size() - 1).getT() : 0;
                ArrayList result;
                //TODO might exist problems here
                result = stub.RequestVote(currentTerm, id, lastLogIndex, lastLogTerm);
                checkTerm((long) result.get(0));
                termPool.set(hostid, (long) result.get(0));
                votePool.set(hostid, (boolean) result.get(1));

            } catch (RemoteException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotBoundException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean isMajority(ArrayList<Boolean> array) {
        int N = array.size();
        int majorty = N / 2 + 1;
        int count = 0;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i)) {
                count++;
            }
        }
        return majorty <= count;
    }

    public void run() {
        //congestion method
        while (!isLeaderAlive) {
            /**
             * On conversion to candidate, start election: Increment currentTerm
             * Vote for self Reset election timer Send RequestVote RPCs to all
             * other servers
             */
            checkTerm(currentTerm + 1);
            votedFor = id;
            votePool.set(id, true);
            startElectionTimer();
            broadCast();

            /**
             * If AppendEntries RPC received from new leader: convert to
             * follower
             */
            if (isLeaderAlive) {
                state = RAFT.FOLLOWER;
                endElectionTimer();
                return;
            }

            /**
             * If election timeout elapses: start new election
             */
            try {
                timer.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            }

            /**
             * votes received from majority of servers: become leader
             */
            if (isMajority(votePool)) {
                state = RAFT.LEADER;
                currentLeader = id;
                //endElectionTimer();
                return;
            }
        }
    }
}
