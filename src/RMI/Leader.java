package RMI;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Leader extends Candidate{
    ArrayList nextIndex;
    ArrayList matchIndex;

    public Leader(ArrayList<InetSocketAddress> mbpList) {
        super(mbpList);
        nextIndex=new ArrayList();
        matchIndex=new ArrayList();
    }
    
    public void runLeader()
    {
        
    }


    
}
