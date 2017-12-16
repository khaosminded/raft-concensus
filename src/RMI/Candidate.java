package RMI;

import static RMI.Follower.state;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import raft.Protocol.RAFT;

public class Candidate extends Follower {

    static volatile ArrayList<InetSocketAddress> mbpList = new ArrayList();
    static ArrayList<Boolean> votePool = new ArrayList();
    static private Timer timer;
    static private int[] interval = {150, 300};

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
            //multiThreads or single
            call.start();
            //call.run();
        }
    }

    //the only entrance of member list
    private Lock votePoolLock = new ReentrantLock(true);

    public final void setMbpList(ArrayList<InetSocketAddress> mbpList) {
        votePoolLock.lock();
        try {
            System.err.println("RMI.Candidate.setMbpList(): id refresh..list refresh...");
            votePool.clear();
            this.mbpList.clear();

            this.mbpList.addAll(mbpList);
            for (int i = 0; i < mbpList.size(); i++) {
                try {
                    if (mbpList.get(i).getHostString().
                            equals(InetAddress.getLocalHost().getHostAddress())) {
                        //id is '0'based;
                        id = i;
                    }
                } catch (UnknownHostException ex) {
                    Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
                }
                votePool.add(false);
            }
        } finally {
            votePoolLock.unlock();
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
                //Logger.getLogger(Timer.class.getName()).log(Level.SEVERE, null, ex);
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
                stateLock.lock();
                try {
                    checkTerm((long) result.get(0));
                    if (state == RAFT.FOLLOWER) {
                        return;
                    }
                } finally {
                    stateLock.unlock();
                }

                votePool.set(hostid, (boolean) result.get(1));

            } catch (RemoteException | NotBoundException ex) {
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
        System.out.println(">CANDIDATE GET VOTE:" + count + "\n");
        return majorty <= count;
    }

    private void resetVotepool() {
        for (int i = 0; i < votePool.size(); i++) {
            votePool.set(i, false);
        }
    }

    public void runCandidate() {
        System.err.println("RMI.Candidate.run()");
        //congestion method
        while (state == RAFT.CANDIDATE) {

            /**
             * On conversion to candidate, start election: Increment currentTerm
             * Vote for self Reset election timer Send RequestVote RPCs to all
             * other servers
             */
            resetVotepool();
            currentTerm++;
            votedFor = id;
            votePool.set(id, true);
            if (votePoolLock.tryLock()) {
                try {
                    startElectionTimer();
                    broadCast();

                    /**
                     * If election timeout elapses: start new election
                     */
                    try {
                        timer.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (stateLock.tryLock()) {
                        try {
                            /**
                             * votes received from majority of servers: become
                             * leader
                             */
                            if (isMajority(votePool)) {
                                System.out.println("candidate->leader");
                                state = RAFT.LEADER;
                                currentLeader = id;
                                //endElectionTimer();
                                return;
                            }
                            /**
                             * If AppendEntries RPC received from new leader:
                             * convert to follower
                             */
                            if (state == RAFT.FOLLOWER) {
                                System.out.println("candidate->follower");
                                return;
                            }
                            /**
                             * else start next election
                             */
                        } finally {
                            stateLock.unlock();
                        }
                    } else {
                        System.err.println("stateLock.tryLock() FAIL");
                    }
                } finally {
                    votePoolLock.unlock();
                }
            } else {
                System.err.println("votePoolLock.tryLock() FAIL");
            }
        }
    }
}
