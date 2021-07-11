package THREAD;


import java.util.concurrent.Future;

class Counter{

          int count= 0 ;

       //  public synchronized void increment(){
          public  void  increment(){    // this avoids the race condition in java
              synchronized (this){
                  count++;
              }
          }

}

public class MyClass {

    public static void main(String[] args) throws Exception {

        Counter c1 = new Counter();

        Runnable obj1 = () -> {

                for(int i = 0 ; i<2000 ; i++){
                    c1.increment();
                }
        };
        Runnable obj2 = () -> {

            for(int i = 0 ; i<1000 ; i++){
                c1.increment();
            }
        };

        Thread t1 = new Thread(obj1,"first Thread");  // we can set the thread name
        Thread t2 = new Thread(obj2,"second thread");

        t1.start();
        t2.start();

        t1.join();  // wait the thread t1 , to complete first

       // Future<Integer> future = Counter.count;
        System.out.println("Count = "+ c1.count);
    }



}
