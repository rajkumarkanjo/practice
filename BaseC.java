interface BaseI { void method(); }

class BaseC
{
    public void method()
    {
        System.out.println("Inside BaseC::method");
    }
}

class ImplC extends BaseC implements BaseI
{
    public static void main(String []s)
    {
        (new ImplC()).method();
    }
}

/*  int mask = 0x000F;
        int value = 0x2222;
        System.out.println(value & mask); */

/* class SampleDemo implements Runnable {

   private Thread t;
   private String threadName;

   SampleDemo (String threadName){
       this.threadName = threadName;
   }

   public void run()
   {
       while (true)
            System.out.print(threadName);
   }

   public void start ()
   {
      if (t == null)
      {
         t = new Thread (this, threadName);
         t.start ();
      }
   }
}

public class TestThread {

   public static void main(String args[]) {

      SampleDemo A = new SampleDemo( "A");
      SampleDemo B = new SampleDemo( "B");

      B.start();
      A.start();
   }
}
 */