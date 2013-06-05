
import java.util.concurrent.*;

public class Test {

  public static void main(String[] args) throws Exception {

     final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);
     final CountDownLatch startLatch = new CountDownLatch(1);

     MyRunnable producer = new MyRunnable("producer") 
     {
       protected void init() throws Exception { startLatch.await(); }
       protected void work() throws Exception{ queue.put( "dummy" ); }
     };
     
     MyRunnable consumer = new MyRunnable("consumer") 
     {
       protected void init() throws Exception { startLatch.await(); }
       protected void work() throws Exception { queue.take(); }
     };

     producer.start();
     consumer.start();          

     System.out.println("*** Starting threads..."); 
     startLatch.countDown();

     System.out.println("*** Sleeping some time..."); 
     Thread.sleep(60*1000);

     System.out.println("*** Stopping threads..."); 
     producer.stopThread();
     consumer.stopThread();
  }
  
  protected static abstract class MyRunnable extends Thread {

       private volatile boolean terminate = false;
       private final CountDownLatch latch = new CountDownLatch(1);

       public MyRunnable(String name) {
         setName(name);
       }

       public void stopThread() throws Exception {
         terminate = true;
         this.interrupt();
         latch.await();
       }

       protected void init() throws Exception {

       }
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
             latch.countDown();
           }
       }  

       protected abstract void work() throws Exception;
  }
}
