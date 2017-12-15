package raft;

import RMI.Candidate;
import RMI.Follower;
import RMI.Leader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import raft.Protocol.RAFT;

public class RaftService {

    ArrayList<InetSocketAddress> mbpList;

    public RaftService(ArrayList<InetSocketAddress> mbpList) {
        this.mbpList = mbpList;
    }

    Leader runRaftService() {
        Leader raftHandle = new Leader(mbpList);
        new service(raftHandle).start();

        return raftHandle;
    }

    private class service extends Thread {

        private final Leader raftHandle;

        service(Leader raftHandle) {
            this.raftHandle = raftHandle;
        }

        @Override
        public void run() {
            //Implement Follower->Candidate->Leader finite state machine here;
            //all runXxxx() function are design to be congested
            System.out.println("RAFT service is running...");

            raftHandle.initRMI();
            while (true) {
                if (raftHandle.state == RAFT.FOLLOWER) {
                    raftHandle.runFollower();
                }
                if (raftHandle.state == RAFT.CANDIDATE) {

                    raftHandle.runCandidate();
                }
                if (raftHandle.state == RAFT.LEADER) {

                    raftHandle.runLeader();
                }
            }
        }

    }

}
