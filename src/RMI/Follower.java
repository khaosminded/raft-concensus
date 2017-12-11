package RMI;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import raft.*;
import RMI.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Follower implements RMIinterface{
    //Persistent state on all servers:
    static long currentTerm;
    static int votedFor;
    static Log log=new Log();
    //Volatile state on all servers:
    int commitIndex;
    int lastApplied;
    //flags to ensure leader alive
    final static int MAX=1,MIN=0;
    static int[] interval={500,1000};
    public volatile boolean  isLeaderAlive=false;
    int currentLeader;
    
    
    public Follower() {
        commitIndex=0;
        lastApplied=0;
    }


    @Override
    public ArrayList RequestVote(long term, int candidateId, int lastLogIndex, int lastLogTerm) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
//1. Reply false if term < currentTerm (§5.1)
//2. Reply false if log doesn’t contain an entry at prevLogIndex
//whose term matches prevLogTerm (§5.3)
//3. If an existing entry conflicts with a new one (same index
//but different terms), delete the existing entry and all that
//follow it (§5.3)
//4. Append any new entries not already in the log
//5. If leaderCommit > commitIndex, set commitIndex =
//min(leaderCommit, index of last new entry)
    @Override
    public ArrayList AppendEntries(long term, int leaderId, int prevLogIndex, int prevLogTerm, 
            ArrayList<Entry> entries, int leaderCommit) {
        ArrayList result=new ArrayList();
        result.add(0, this.currentTerm>term?this.currentTerm:term);
        if(log.get(prevLogIndex)==null?
                false:log.get(prevLogIndex).getT()!=prevLogTerm)
            result.add(false);
        else if()
                
        heartBeat(leaderId);
        
        return result;
    }
    private void heartBeat(int leaderId)
    {
        isLeaderAlive=true;
        this.currentLeader=leaderId;
    }
    
    
    
    private void startElectionTimer()
    {
        int period;
        period=(int) (Math.random()*(interval[MAX]-interval[MIN])+interval[MIN]);
        Timer t=new Timer(period);
        t.run();
    }
    private class Timer extends Thread{
        int sleeptime;
        public Timer(int sleeptime) {
            super();
            this.sleeptime=sleeptime;
        }
        @Override
        public void run()
        {
                try {
                Thread.sleep(sleeptime);
                //timeout!!
                isLeaderAlive=false;
                } catch (InterruptedException ex) {
                    Logger.getLogger(Timer.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        
    }
    void runFollower()
    {
        try {
            //TODO ensure there is no side effect for not unbinding
            String name="raftFollower";
            RMIinterface stub = (RMIinterface) 
                    UnicastRemoteObject.exportObject(new Follower(), 0);
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
