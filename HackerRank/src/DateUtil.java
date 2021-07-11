import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

    public static void main(String[] args) {

        String cstDate = dateincstformat();
        System.out.println(cstDate);

    }

    private static String dateincstformat() {


        TimeZone tz = TimeZone.getTimeZone("US/Central");
        //  Calendar cal = Calendar.getInstance(tz);
        //  cal.clear(Calendar.MILLISECOND);
        //  OffsetDateTime timeOfEntryOfItem = OffsetDateTime.ofInstant(cal.toInstant(), tz.toZoneId());
        //  System.out.println(timeOfEntryOfItem.toString());


        // return  timeOfEntryOfItem.toString();


        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // date format for BY
/*        Calendar presentDay = Calendar.getInstance(tz);
        Date present = presentDay.getTime();
        String presentDate = dateFormat.format(present);

        Calendar nextDay = Calendar.getInstance(tz);
        nextDay.add(Calendar.DATE, 1);
        Date next = presentDay.getTime();
        String nextDate = dateFormat.format(next);


        System.out.println(presentDate);
        System.out.println(nextDate);*/


        Calendar today = Calendar.getInstance(tz);
        Date present = today.getTime();
        String presentDate = dateFormat.format(present);

        Calendar nextDay = Calendar.getInstance(tz);
        nextDay.add(Calendar.DATE,1);
        Date next = nextDay.getTime();
        String nextDate = dateFormat.format(next);


        System.out.println(presentDate);
        System.out.println(nextDate);


     /*   SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ");
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatter("yyyy-MM-ddTHH:mm:ssZ");

        timeOfEntryOfItem.format(simpleDateFormat);*/


         return  null;
    }
}
