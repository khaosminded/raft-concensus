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
    private long term;

    public Entry() {
        opt=null;
        term=-1;
    }
    public Operation getO()
    {
        return opt;
    }
    public long getT()
    {
        return term;
    }
    public void set(Operation opt,long term)
    {
        this.opt=opt;
        this.term=term;
    }
}
