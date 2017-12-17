package raft;

import java.util.ArrayList;
import java.util.Vector;
import raft.Protocol.*;

public class Log {

    private ArrayList<Entry> entries;

    public Log() {
        this.entries = new ArrayList<Entry>();
        System.out.println("RAFT log init..");
    }

    public void Log(Log l) {
        entries.addAll(l.get());
    }

    public ArrayList<Entry> get() {
        return this.entries;
    }

    public Entry get(int index) {
        return this.entries.get(index);
    }

    public int size() {
        return this.entries.size();
    }

    synchronized public void add(Entry e) {
        entries.add(e);
    }

    synchronized public void set(int index, Entry e) {
        entries.set(index, e);
    }
    
    synchronized public void delFrom(int index) {
        for (int i = index; i < entries.size(); i++) {
            entries.remove(index);
        }
    }
    public String displayLog()
    {
        String result="";
        for(int i=0;i<entries.size();i++)
            result+=entries.get(i).toString();
        return result;
    }
    
}
