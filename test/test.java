
import RMI.Follower;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import raft.Entry;
import raft.Log;
import raft.Protocol;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author hanxinlei
 */
public class test {//nothing

    static volatile Integer a = 0;
    static A t1 = new A();
    static B t2 = new B();
    static Lock lock = new ReentrantLock();

    public static void main(String args[]) {
//        Follower testHandle= new Follower();
//        ArrayList<Entry> entries =new ArrayList<>();
//        testHandle.AppendEntries(0, 3, -1, -1, entries, 0);
//        System.out.println("test.main()");
//        
//        if(true?true:1/0==1)
//        {
//            System.out.println("test.main()");
////        }
//        String str="a:::::";
//        System.out.println(str.toCharArray());
//        t1.start();
//        t2.start();
        Entry e=new Entry(Protocol.Operation.GET, "a","123", 0);
        Log log=new Log();
        log.add(e);log.add(e);log.add(e);log.add(e);
        System.out.println(log.displayLog());
    }

    static class A extends Thread {

        @Override
        public void run() {
            while (true) {
                if (lock.tryLock()) {
                    try {
                        a++;
                        System.out.println("test.A.run()=" + a);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // perform alternative actions
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(test.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

    }


static class B extends Thread {

@Override
        public void run() {
            while (true) {
                if (lock.tryLock()) {
                    try {
                        a++;
                        System.out.println("test.B.run()=" + a);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // perform alternative actions
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(test.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
}

}
