package raft;

import raft.*;



/**
 * Protocol constants
 *
 */
public class Protocol {
	static enum Operation{GET,PUT,DEL,STORE,EXIT;}
        static  enum TYPE{MBPSTORE,CLIENT};
        public static enum RAFT{CANDIDATE,FOLLOWER,LEADER};
        public static final String SERVER_ROLE = "ts";
	public static final String CLIENT_ROLE = "tc";
}
