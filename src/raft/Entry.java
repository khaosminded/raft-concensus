
package raft;

import java.io.Serializable;
import raft.Protocol.Operation;

/**
 *
 * @author hanxinlei
 */

//Serializable, so it can go through network
public class Entry implements Serializable{

    private Operation opt;
    private String key;
    private String value;
    private long term;

    public Entry() {
        opt = null;
        key = null;
        value = null;
        term = -1;
    }

    public Entry(Operation opt, String key, String value, long term) {
        this.opt = opt;
        this.key = key;
        this.value = value;
        this.term = term;
    }

    public Operation getO() {
        return opt;
    }

    public long getT() {
        return term;
    }

    public String getK() {
        return key;
    }

    public String getV() {
        return value;
    }

    public void set(Operation opt, String key, String value, long term) {
        this.opt = opt;
        this.key = key;
        this.value = value;
        this.term = term;
    }
    @Override
    public String toString()
    {
        String result;
        result="\n#"+opt.name()+"\t#"+key+"\t#"+value+"\t#"+term;
        return result;
    }
}
