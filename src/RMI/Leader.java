package RMI;

import java.util.ArrayList;
import raft.*;

public class Leader extends Follower{
    ArrayList nextIndex;
    ArrayList matchIndex;

    public Leader() {
        super();
        nextIndex=new ArrayList();
        matchIndex=new ArrayList();
    }
    ArrayList getMbpList()
    {
        ArrayList list=new ArrayList();
        
        return list;
    }
    void broadCast()
    {
        
    }
    void AppendEntriesToLog(){
        
    
    }
    
}
