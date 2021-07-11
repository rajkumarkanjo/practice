/*
Double colon operator (::)
method reference for static  Constructor reference
 */
interface Bike{
    public String gatName();
        }


class Engine  {

    public Engine() {
        System.out.println("new engine is being created");
    }
}


interface  NewBike{

      public Engine getEngine();
}

public  class DoubleColon_8 {

    public static  String printName(){
        return  " Honda is a static method ";
    }

    public String printCity(){
        return  "non static method my city is Bangalore ";
    }

    public static void main(String[] args) {

        Bike b = () -> "Hello Honda";
      //  System.out.println(b.gatName());

//method reference for static method
        Bike b1 = DoubleColon_8::printName; // reference of interface , calling the method inside the class
        System.out.println(b1.gatName());

//method reference for non static method
        DoubleColon_8 obj = new DoubleColon_8();
        Bike b2 = obj::printCity;
        System.out.println(b2.gatName());

// consturctor reference
    NewBike newBike = Engine::new;
    newBike.getEngine();


    }
}

