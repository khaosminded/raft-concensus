package RMI;

import static RMI.Follower.log;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import raft.Entry;
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
    ArrayList<Integer> nextIndex;
    ArrayList<Integer> matchIndex;
    //set timer
    static private Timer timer;
    static final private int heartBeatInterval = 200;
    static final private int sendLimit = 20;

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

    private void startHeartTimer() {
        timer = new Timer(heartBeatInterval);
        timer.start();
    }

    private void endHeartTimer() {
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
                /**
                 * will be a heavy function. send initial empty AppendEntries
                 * RPCs (heartbeat) to each server
                 */
                ArrayList<Entry> entries = new ArrayList<>();
                ArrayList result;
                int prevLogIndex = nextIndex.get(hostid) - 1;
                long prevLogTerm = log.get(prevLogIndex).getT();
                /**
                 * If last log index ≥ nextIndex for a follower: send
                 * AppendEntries RPC with log entries starting at nextIndex
                 */
                int maxSend = sendLimit;
                int next = nextIndex.get(hostid);
                for (; next < log.size() && maxSend > 0; next++, maxSend--) {
                    entries.add(log.get(next));
                }
                result = stub.AppendEntries(currentTerm, id, prevLogIndex,
                        prevLogTerm, entries, commitIndex);

                /**
                 * If successful: update nextIndex and matchIndex for follower
                 *
                 * @matchIndex: increases monotonically <???>
                 */
                if ((boolean) result.get(1)) {
                    nextIndex.set(hostid, next);
                    matchIndex.set(hostid, next - 1);
//                        matchIndex.set(hostid, i>matchIndex.get(hostid)?
//                                i:matchIndex.get(hostid));
                }
                /**
                 * If AppendEntries fails because of log inconsistency:
                 * decrement nextIndex and retry (§5.3)
                 */
                if ((boolean) result.get(1)) {
                    nextIndex.set(hostid, nextIndex.get(hostid) - 1);
                }

                /**
                 * If there exists an N such that N > commitIndex, a majority of
                 * matchIndex[i] ≥ N, and log[N].term == currentTerm: set
                 * commitIndex = N (§5.3, §5.4)
                 */
                //take care of leader itself
                //worst O(log.size * (mbpList.size-1))
                for (int N = commitIndex + 1; N < log.size(); N++) {
                    int majority = matchIndex.size() / 2;
                    int count = 0;
                    for (int i = 0; i < matchIndex.size(); i++) {
                        if (matchIndex.get(i) >= N) {
                            count++;
                        }
                    }
                    //don't know why algorithm author said check currentTerm
                    if (count >= majority && log.get(N).getT() == currentTerm) {
                        commitIndex = N;//MAX(such N)
                    }
                }
                checkTerm((Long) result.get(0));

            } catch (RemoteException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotBoundException ex) {
                Logger.getLogger(Candidate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void initIndexes() {
        nextIndex.clear();
        matchIndex.clear();
        for (int i = 0; i < mbpList.size(); i++) {
            nextIndex.add(log.size());
            matchIndex.add(-1);
        }

    }

    public void run() {
        initIndexes();
        while (getState() == RAFT.LEADER) {
            startHeartTimer();

            broadCast();
            applyLog2Store();

            try {
                timer.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Leader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
