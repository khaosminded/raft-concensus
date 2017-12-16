package raft;

import raft.*;

/**
 * Protocol constants
 *
 */
final public class Protocol {

    public static enum Operation {
        GET, PUT, DEL, STORE, EXIT;
    }

    public static enum TYPE {
        RKVSTORE, MBPSTORE, CLIENT
    };

    public static enum RAFT {
        CANDIDATE, FOLLOWER, LEADER
    };
    public static final String SERVER_ROLE = "ts";
    public static final String CLIENT_ROLE = "tc";
    public static final String MBPS_ROLE = "mb";
}
