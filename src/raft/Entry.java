/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raft;

import raft.Protocol.Operation;

/**
 *
 * @author hanxinlei
 */
public class Entry {
    private Operation opt;
    private String key;
    private String value;
    private long term;

    public Entry() {
        opt=null;
        key=null;
        value=null;
        term=-1;
    }
    public Entry(Operation opt,String key,String value,long term) {
        this.opt=opt;
        this.key=key;
        this.value=value;
        this.term=term;
    }
    public Operation getO()
    {
        return opt;
    }
    public long getT()
    {
        return term;
    }
    public String getK()
    {
        return key;
    }
    public String getV()
    {
        return value;
    }
    public void set(Operation opt,String key,String value,long term)
    {
        this.opt=opt;
        this.key=key;
        this.value=value;
        this.term=term;
    }
}
