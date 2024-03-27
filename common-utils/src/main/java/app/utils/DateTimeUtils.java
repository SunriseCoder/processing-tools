package app.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    public static String getCurrentDateFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        LocalDate localDateNow = LocalDate.now();
        String result = formatter.format(localDateNow);
        return result;
    }
}
