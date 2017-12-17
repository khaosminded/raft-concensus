package raft;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public String displayLog() {
        String result = "";
        for (int i = 0; i < entries.size(); i++) {
            result += entries.get(i).toString();
        }
        return result;
    }

    public static void toDisk(Entry e) {
        try {
            FileOutputStream fout = new FileOutputStream("log.data", true);
            ObjectOutputStream out = new ObjectOutputStream(fout);
            out.writeObject(e);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void FromDisk() {

        try {
            FileInputStream fin = new FileInputStream("log.data");
            ObjectInputStream in = new ObjectInputStream(fin);
            while (true) {
                try {
                    this.add((Entry) in.readObject());
                } catch (IOException ex) {
                    System.err.println("LOG Unserialization Finished...");
                    break;
                } 
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
