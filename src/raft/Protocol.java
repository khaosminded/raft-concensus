package raft;

import raft.*;



/**
 * Protocol constants
 *
 */
public class Protocol {
	static enum Operation{GET,PUT,DEL,STORE,EXIT;}
        static  enum TYPE{CANDIDATE,FOLLOWER,LEADER,MBPSTORE,CLIENT};
        public static final String SERVER_ROLE = "ts";
	public static final String CLIENT_ROLE = "tc";
}
