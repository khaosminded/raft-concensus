

import java.util.logging.Level;
import java.util.logging.Logger;

public class Term { 
   private class Timer extends Thread{
        int sleeptime;
        public Timer(int sleeptime) {
            super();
            this.sleeptime=sleeptime;
        }
        @Override
        public void run()
        {
            while(true)
            {
                try {
                Thread.sleep(sleeptime);
                termId++;
                } catch (InterruptedException ex) {
                    Logger.getLogger(Timer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
    }
   private long termId;
   private Timer timer;
   private int termPeriod; 
    public Term(int termPeriod) {
        this.termPeriod=termPeriod;
        this.termId=0;
        timer = new Timer(termPeriod);
        
    }
   void Start()
   {
       timer.run();
   }
   void end()
   {
       timer.interrupt();
       termId++;
   }
   void  setId(long termId)
   {
       this.termId=termId;
   }
   long getId()
   {
       return this.termId;
   }
    
}
