package com.fogoa.photoapplication.misc;

import android.util.Patterns;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class Utilities {
    public static int safeParseInt(String s) {
        if(s == null || s.length() == 0) {
            return(0);
        }
        s = removeCommas(s);
        try {
            return(Integer.parseInt(s));
        } catch (Exception e) {
            return(0);
        }
    }

    public static long safeParseLong(String s) {
        if(s == null || s.length() == 0) {
            return(0);
        }
        s = removeCommas(s);
        try {
            return(Long.parseLong(s));
        } catch (Exception e) {
            return(0);
        }
    }

    public static double safeParseDouble(String s) {
        if(s == null || s.length() == 0) {
            return(0);
        }
        s = removeCommas(s);
        try {
            return(Double.parseDouble(s));
        } catch (Exception e) {
            return(0);
        }
    }

    public static float safeParseFloat(String s) {
        if(s == null || s.length() == 0) {
            return(0);
        }
        s = removeCommas(s);
        try {
            return(Float.parseFloat(s));
        } catch (Exception e) {
            return(0);
        }
    }

    public static char safeParseChar(String s) {
        if(s == null || s.length() == 0) {
            return(0);
        }
        return(s.charAt(0));
    }

    public static boolean safeParseBoolean(String s) {
        if(s == null || s.length() == 0) {
            return(false);
        }
        if(s.contentEquals("1") || s.equalsIgnoreCase("true")) {
            return(true);
        }
        else {
            return (false);
        }
    }

    public static Date safeParseDate(String s) {
        Date rDate = null;
        if (s!=null) {
            try {
                DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
                rDate = iso8601.parse(s);
            } catch (Exception e) {
                //preventing parse error
            }
        }
        return rDate;
    }
    public static String safeDateToString(Date date) {
        String rDate = "";
        if (date!=null) {
            try {
                DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
                iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
                rDate = iso8601.format(date);
            } catch (Exception e) {
                //preventing parse error
            }
        }
        return rDate;
    }
    public static String safeDateToString(Date date, String pattern) {
        String rDate = "";
        if (date!=null) {
            try {
                DateFormat iso8601 = new SimpleDateFormat(pattern, Locale.US);
                iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
                rDate = iso8601.format(date);
            } catch (Exception e) {
                //preventing parse error
            }
        }
        return rDate;
    }



    public static String removeCommas(String s) {
        return(s.replaceAll(",", ""));
    }

    public static boolean isEmailValid(CharSequence seq) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(seq).matches();
    }

    public static boolean isPhoneValid(CharSequence seq) {
        return Patterns.PHONE.matcher(seq).matches();
    }

    public static boolean isUsernameValid(CharSequence seq) {
        int len = seq.length();
        for(int i=0;i<len;i++) {
            char c = seq.charAt(i);
            // Test for all positive cases
            if('0'<=c && c<='9') continue;
            if('a'<=c && c<='z') continue;
            if('A'<=c && c<='Z') continue;
            if(c=='_') continue;
            if(c=='-') continue;
            if(c=='~') continue;

            // If we get here, we had an invalid char, fail right away
            return false;
        }
        // All seen chars were valid, succeed
        return true;
    }

    public static boolean isPasswordValid(CharSequence seq) {
        int len = seq.length();
        for(int i=0;i<len;i++) {
            char c = seq.charAt(i);
            // Test for all positive cases
            if('0'<=c && c<='9') continue;
            if('a'<=c && c<='z') continue;
            if('A'<=c && c<='Z') continue;
            if(c=='_') continue;
            if(c=='-') continue;
            if(c=='~') continue;

            //APL - 11/28/16 - allow new characters in password
            if(c>='!' && c<='~') continue;

            // If we get here, we had an invalid char, fail right away
            return false;
        }
        // All seen chars were valid, succeed
        return true;
    }

    public static String getDisplayDate(Date currentDate) {
        GregorianCalendar calendar = new GregorianCalendar();
        if(currentDate == null) {
            currentDate = new Date();
        }

        //String strDate = (String)android.text.format.DateFormat.format("EEEE, MMMM d", currentDate);
        String strDate = (String)android.text.format.DateFormat.format("EEEE, MMM d", currentDate);

        return strDate;
    }

}
