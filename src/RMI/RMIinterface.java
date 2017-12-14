package RMI;

import raft.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 *
 * @author hanxinlei
 */
public interface RMIinterface extends Remote {

    /**
     * return ArraryList:
     *
     * @term::long currentTerm for candidate to update itself
     * @voteGranted::boolean true means candidate grant vote
     */
    ArrayList RequestVote(long term, int candidateId, int lastLogIndex, long lastLogTerm)throws RemoteException;

    /**
     * return ArraryList:
     *
     * @term::long currentTerm for leader to update itself
     * @success:boolean true if follower contained entry matching prevLogIndex
     * and prevLogterm
     */
    ArrayList AppendEntries(long term, int leaderId, int prevLogIndex, long prevLogTerm,
            ArrayList<Entry> entries, int leaderCommit)throws RemoteException;

}
