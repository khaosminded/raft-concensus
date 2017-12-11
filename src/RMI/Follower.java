package RMI;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import raft.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Follower implements RMIinterface {

    //Persistent state on all servers:
    static long currentTerm;
    static int votedFor;
    static Log log = new Log();
    //Volatile state on all servers:
    static int commitIndex = 0;
    static int lastApplied = 0;
    //flags to ensure leader alive
    static Timer timer;
    static int[] interval = {500, 1000};
    static public volatile boolean isLeaderAlive = false;
    static int currentLeader;

    public Follower() {

    }

    @Override
    public ArrayList RequestVote(long term, int candidateId, int lastLogIndex, int lastLogTerm) {
        //init result {term,voteGranted}
        ArrayList result = new ArrayList();
        result.add(this.currentTerm > term ? this.currentTerm : term);
        result.add(true);
        /**
         * 1. Reply false if term < currentTerm (§5.1)
         */
        if (this.currentTerm > term) {
            result.set(1, false);
            return result;
        }
        /**
         * 2. If votedFor is null or candidateId, and candidate’s log is at
         * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
         *
         * MORE up-to-date log is defined as log with:  * Higher term # in last
         * log entry  * --- OR ---  * When term of last log entries match, log
         * with more entries
         */
        if ((votedFor == 0 || votedFor == candidateId)
                && (log.size() > 0 ? log.get(log.size() - 1).getT() < lastLogTerm : true
                || log.size() < lastLogIndex + 1)) {
            result.set(1, true);
            return result;
        } else {
            result.set(1, false);
            return result;
        }

    }

    @Override
    public ArrayList AppendEntries(long term, int leaderId, int prevLogIndex, int prevLogTerm,
            ArrayList<Entry> entries, int leaderCommit) {
        //init result {term,success}
        ArrayList result = new ArrayList();
        result.add(this.currentTerm > term ? this.currentTerm : term);
        result.add(true);
        /**
         * 1. Reply false if term < currentTerm (§5.1)
         */
        if (this.currentTerm > term) {
            result.set(1, false);
            return result;
        }

        /**
         * 2. Reply false if log doesn’t contain an entry at prevLogIndex whose
         * term matches prevLogTerm (§5.3)
         */
        if (log.size() <= prevLogIndex ? true : log.get(prevLogIndex).getT() != prevLogTerm) {
            result.set(1, false);
            return result;
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
        }//end

        heartBeat(leaderId);
        return result;
    }

    private void heartBeat(int leaderId) {
        isLeaderAlive = true;
        this.currentLeader = leaderId;
        endElectionTimer();
        startElectionTimer();
    }

    private void startElectionTimer() {
        int period;
        period = (int) (Math.random() * (interval[1] - interval[0]) + interval[0]);
        timer = new Timer(period);
        timer.run();
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
                Logger.getLogger(Timer.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("electionTimer restart..");
            }
            isLeaderAlive = false;
        }

    }

    void runFollower() {
        try {
            //TODO ensure there is no side effect for not unbinding
            String name = "raftFollower";
            RMIinterface stub = (RMIinterface) UnicastRemoteObject.exportObject(new Follower(), 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(name, stub);
            System.out.println("Follower is ready..");
        } catch (Exception e) {
            System.err.println("RMIServer exception: " + e.toString());
            e.printStackTrace();
        }
        startElectionTimer();

    }
}
