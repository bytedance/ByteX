package com.ss.android.ugc.bytex.common.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * @author Eric Tjitra
 * @since Nov 26, 2018
 */
public class CalendarUtils {

    private static final String TAG = "CalendarUtils";

    // Format Pattern: https://developer.android.com/reference/java/text/SimpleDateFormat.html
    private static final String TIME_FORMAT_24H = "HH:mm:ss";
    private static final String TIME_FORMAT_12H = "K:mm:ss a";
    private static final String DATE_FORMAT_NO_YEAR = "MMM d, HH:mm";

    // Default separator between date and time
    private static final String DEFAULT_SEPARATOR = ", ";

    public static String getDateAndTimeString(long millis, boolean alwaysShowDate) {
        StringBuilder sb = new StringBuilder();
        if (alwaysShowDate) {
            sb.append(getDateString(millis));
            sb.append(DEFAULT_SEPARATOR);
        }
        sb.append(getTimeString(true, millis));
        return sb.toString();
    }

    public static String getDateString(long millis) {
        if (millis < 0)
            return null;

        Calendar currCal = Calendar.getInstance();
        currCal.setTimeInMillis(millis);
        return DateFormat.getDateInstance().format(currCal.getTime());
    }

    public static String getTimeString(boolean is24h, long millis) {
        if (millis < 0)
            return null;

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(millis);

        SimpleDateFormat sdf = new SimpleDateFormat(is24h ? TIME_FORMAT_24H : TIME_FORMAT_12H, Locale.US);
        return sdf.format(targetCal.getTime());
    }
}
