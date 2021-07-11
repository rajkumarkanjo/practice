package DSA;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateDemo {

    public static void main(String[] args) {

        String str = "20210622";

       final char ch ='-';
        String datee = str.substring(0,4)+ch+str.substring(4,6)+ch+str.substring(6,8);

       // String datee = new StringBuffer(str).append( "-",4,5).append( "-",6,7).toString();


      //  System.out.println("888888888> "+datee);

       String year = str.substring(0,4);
       String mm = str.substring(4,6);
       String dd = str.substring(6,8);


       String finalDate = year+"-"+mm+"-"+dd;



        System.out.println("<<<<<<<<>>>>>>>>>>  "+str.replace("[-]",""));

       // DateFormat dateFormat = new SimpleDateFormat(finalDate);


        TimeZone tz = TimeZone.getTimeZone("US/Central");


     //   System.out.println(simpleDateFormat.toString());


        /// D , D+1 format

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // declare as DateFormat
        Calendar today = Calendar.getInstance(tz);
        Calendar yesterday = Calendar.getInstance(tz);
        yesterday.add(Calendar.DATE, -1);
        Date d = yesterday.getTime(); // get a Date object
        String yesDate = sdf.format(d); // toString for Calendars is mostly not really useful
        System.out.println(yesDate);

        Date d1 = today.getTime();
        String today1 = sdf.format(d1);
        System.out.println(today1);

        /// D and D-1 format

        Calendar today2 = Calendar.getInstance(tz);
        Date present = today2.getTime();
        String presentDate = sdf.format(present);

        Calendar previousDay = Calendar.getInstance(tz);
        previousDay.add(Calendar.DATE,-1);
        Date next = previousDay.getTime();
        String previousDate = sdf.format(next);

        System.out.println("  "+previousDate);
        System.out.println("  "+presentDate);


       String[] parts;
       String defaultShutdownCutoffTime = "10:00:00";
        parts = defaultShutdownCutoffTime.split(":");


        int shutdownCutOffHour;
        int shutdownCutOffMins;

        shutdownCutOffHour = Integer.parseInt(parts[0]);
        shutdownCutOffMins = Integer.parseInt(parts[1]);

        System.out.println(shutdownCutOffHour +" "+ shutdownCutOffMins);

    }

}
