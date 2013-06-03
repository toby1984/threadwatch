
import java.util.concurrent.*;

public class Test {

  public static void main(String[] args) throws Exception {

     final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);
     final CountDownLatch startLatch = new CountDownLatch(1);
     final CountDownLatch stopLatch = new CountDownLatch(2);

     MyRunnable producer = new MyRunnable("producer") 
     {
       protected void init() throws Exception { startLatch.await(); }
       protected void work() throws Exception{ delayLoop(500000); queue.put( "dummy" ); }
       protected void onTermination() { stopLatch.countDown(); }
     };
     
     MyRunnable consumer = new MyRunnable("consumer") 
     {
       protected void init() throws Exception { startLatch.await(); }
       protected void work() throws Exception { queue.take(); delayLoop(2000000); }
       protected void onTermination() { stopLatch.countDown(); }
     };

     producer.start();
     consumer.start();          

     Thread.sleep(1*1000);

     System.out.println("*** Starting threads..."); 
     startLatch.countDown();

     System.out.println("*** Sleeping some time..."); 
     Thread.sleep(15*1000);

     System.out.println("*** Stopping threads..."); 
     producer.stopThread();
     consumer.stopThread();

     stopLatch.await();
     System.out.println("*** Threads stopped."); 
  }
  
  protected static abstract class MyRunnable extends Thread {

       private volatile boolean terminate = false;

       private double dummyValue;

       public MyRunnable(String name) {
         setName(name);
       }

       protected final void delayLoop(int iterations) 
       {
           dummyValue = -System.currentTimeMillis();
           for ( int i = 0 ; i < iterations ; i++) {
               dummyValue *= 2.0+(Math.sqrt(dummyValue));
           }
       }

       public double getDummyValue() { return dummyValue; }

       public void stopThread() throws Exception {
         terminate = true;
         this.interrupt();
       }

       protected void init() throws Exception { }

       protected void onTermination() {}

       public final void run() 
       {
           try {
              init();
              System.out.println("*** Thread running: "+Thread.currentThread().getName());
           while ( ! terminate ) {
	     work();
           }
           } catch(Exception e) {
             e.printStackTrace(); 
           } finally {
             System.out.println("*** Thread terminating: "+Thread.currentThread().getName());
             onTermination();
           }
       }  

       protected abstract void work() throws Exception;
  }
}
