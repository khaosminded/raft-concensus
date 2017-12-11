/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RMI;

import java.util.ArrayList;
import raft.*;


/**
 *
 * @author hanxinlei
 */
public class RMImethods  implements RMIinterface{

    @Override
    public ArrayList RequestVote(long term, int candidateId, int lastLogIndex, int lastLogTerm) {
        ArrayList result = new ArrayList();
        
        return result;
    }

    @Override
    public ArrayList AppendEntries(long term, int leaderId, int prevLogIndex, int prevLogTerm, int[] entries, int leaderCommit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
