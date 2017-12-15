package RMI;

import TCP.Server;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import raft.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static raft.Protocol.Operation.DEL;
import static raft.Protocol.Operation.PUT;
import raft.Protocol.RAFT;

public class Follower implements RMIinterface {

    //Persistent state on all servers:
    static long currentTerm = 0;
    static int votedFor = -1;
    static public Log log = new Log();
    //Volatile state on all servers: 
    /**
     * @note::assumed to be '0' based index::match with ArrayList index changed
     * <initialized to 0> to <initialized to -1> :: match the meaning of 'last'
     */
    static int commitIndex = -1;
    static int lastApplied = -1;
    //flags to ensure leader is alive
    static private Timer timer;
    static private int[] interval = {800, 1500};
    static volatile boolean isLeaderAlive = false;
    static int currentLeader = -1;
    //ID  '0'based
    static int id = -1;
    //critical flags
    static public volatile RAFT state = RAFT.FOLLOWER;
    static private boolean isRunning = false;

    public Follower() {

    }

    @Override
    public ArrayList RequestVote(long term, int candidateId, int lastLogIndex, long lastLogTerm) {
        //init result {term,voteGranted}
        ArrayList result = new ArrayList();
        result.add(this.currentTerm > term ? this.currentTerm : term);
        result.add(true);
        /**
         * 1. Reply false if term less than currentTerm (§5.1)
         */
        if (this.currentTerm > term) {
            result.set(1, false);
            return result;
        } else {
            checkTerm(term);
        }
        /**
         * 2. If votedFor is null or candidateId, and candidate’s log is at
         * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
         *
         * MORE up-to-date log is defined as log with:  * Higher term # in last
         * log entry  * --- OR ---  * When term of last log entries match, log
         * with more entries
         */
        if ((votedFor == -1 || votedFor == candidateId)
                && ((log.size() > 0 ? log.get(log.size() - 1).getT() < lastLogTerm : true)
                || (log.size() <= lastLogIndex + 1 && (log.get(log.size() - 1).getT() == lastLogTerm)))) {
            votedFor = candidateId;
            result.set(1, true);
            return result;
        } else {
            result.set(1, false);
            return result;
        }

    }

    @Override
    public ArrayList AppendEntries(long term, int leaderId, int prevLogIndex, long prevLogTerm,
            ArrayList<Entry> entries, int leaderCommit) {
        //init result {term,success}
        ArrayList result = new ArrayList();
        result.add(this.currentTerm > term ? this.currentTerm : term);
        result.add(true);
        /**
         * Server rules.
         */
        //>for all server received RPC call
        checkTerm(term);
        //>for folower
        if (state == RAFT.FOLLOWER) {
            heartBeat(leaderId);
        }
        /**
         * 1. Reply false if term less than currentTerm (§5.1)
         */
        if (this.currentTerm > term) {
            result.set(1, false);
            return result;
        }

        /**
         * 2. Reply false if log doesn’t contain an entry at prevLogIndex whose
         * term matches prevLogTerm (§5.3)
         */
        if (prevLogIndex != -1) {
            if (log.size() <= prevLogIndex ? true : log.get(prevLogIndex).getT() != prevLogTerm) {
                result.set(1, false);
                return result;
            }
        }
        /**
         * 3. If an existing entry conflicts with a new one (same index but
         * different terms), delete the existing entry and all that follow it
         * (§5.3)
         *
         * @prevLogIndex+1= entries[]'s beginning
         */
        int i = 0;//critical iterator
        if (entries.size() > 0) {
            for (; i < entries.size(); i++) {
                if (log.size() > i + prevLogIndex + 1
                        && log.get(i + prevLogIndex + 1).getT() == entries.get(i).getT()) {
                    //pass  
                } else {
                    break;
                }
            }
            if (i != entries.size()) {
                log.delFrom(i + prevLogIndex + 1);
            }
        }
        /**
         * 4. Append any new entries not already in the log
         */
        for (; i < entries.size(); i++) {
            log.add(entries.get(i));
        }
        /**
         * 5. If leaderCommit > commitIndex, set commitIndex = min(leaderCommit,
         * index of last new entry)
         */
        if (leaderCommit > commitIndex) {
            int lastNewEntry = prevLogIndex + entries.size();
            commitIndex = leaderCommit < lastNewEntry ? leaderCommit : lastNewEntry;
        }//end Append logic

        return result;
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return currentLeader;
    }

    public long getTerm() {
        return currentTerm;
    }

    private void heartBeat(int leaderId) {
        isLeaderAlive = true;
        this.currentLeader = leaderId;
        endElectionTimer();
        applyLog2Store();
        startElectionTimer();
    }

    void applyLog2Store() {
        /**
         * If commitIndex > lastApplied: increment lastApplied, apply
         * log[lastApplied] to state machine (§5.3)
         */
        System.err.println("RMI.Follower.applyLog2Store()");
        while (commitIndex > lastApplied) {
            lastApplied++;
            Entry e = log.get(lastApplied);
            if (e.getO() == PUT) {
                Server.store.put(e.getK(), e.getV());
            }
            if (e.getO() == DEL) {
                Server.store.del(e.getK());
            }
        }
    }

    public void checkTerm(long term) {
        /**
         * FOR All Servers If RPC request or response contains term T >
         * currentTerm: set currentTerm = T, convert to follower (§5.1)
         */
        if (currentTerm < term) {
            currentTerm = term;
            //CRITICAL reset votedFor, everytime term changes;
            votedFor = -1;
            System.err.println("TERM:"+currentTerm+":::::LEADER:"+currentLeader);
            System.out.println("----->follower");
            state = RAFT.FOLLOWER;
        }
    }

    private void startElectionTimer() {
        System.err.println("RMI.Follower.startElectionTimer()");
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
                //timeout!!

            } catch (InterruptedException ex) {
                //Logger.getLogger(Timer.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("followerTimer restart..");
                return;
            }
            isLeaderAlive = false;
            //TODO
            System.out.println("follower->candidate");
            state = RAFT.CANDIDATE;
            isRunning = false;
        }

    }

    RAFT getState() {
        return state;
    }

    public void initRMI() {
        //init, uncongested
        try {
            String name = "raftFollower";
            //possibly related to Garbage collection
            //Follower obj=new Follower();
            RMIinterface stub = (RMIinterface) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(name, stub);
            System.out.println("RMI service is ready..");
        } catch (Exception e) {
            System.err.println("RMIServer exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public void runFollower() {
        System.err.println("RMI.Follower.run()");
        //elaborate congested;
        isRunning = true;
        startElectionTimer();
        while (isRunning) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(Follower.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
