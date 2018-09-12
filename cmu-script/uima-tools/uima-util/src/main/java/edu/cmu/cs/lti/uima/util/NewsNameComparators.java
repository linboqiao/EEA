package edu.cmu.cs.lti.uima.util;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/1/14
 * Time: 1:04 PM
 */
public class NewsNameComparators {
    public static Comparator<File> getGigawordDateComparator(final String inputFileSuffix, final String dateFormatStr) {
        return new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {

                Date d1 = extractDate(o1.getName());
                Date d2 = extractDate(o2.getName());

                if (d1 != null && d2 != null) {
                    if (d1.compareTo(d2) != 0) {
                        return d1.compareTo(d2);
                    }
                }

                //if date are different
                int n1 = extractOffset(o1.getName());
                int n2 = extractOffset(o2.getName());
                return n1 - n2;
            }

            private Date extractDate(String name) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatStr);
                Date d = null;
                try {
                    d = dateFormat.parse(name.substring(8, 14));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return d;
            }

            private int extractOffset(String name) {
                int i = 0;
                try {
                    int s = name.lastIndexOf('_') + 1;
                    int e = name.lastIndexOf(inputFileSuffix);
                    String number = name.substring(s, e);
                    i = Integer.parseInt(number);
                } catch (Exception e) {
                    i = 0; // if filename does not match the format
                    // then default to 0
                }
                return i;
            }
        };
    }

    public static Comparator<File> getFileOffsetComparator(final String inputFileSuffix) {
        return new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                int n1 = extractOffset(o1.getName());
                int n2 = extractOffset(o2.getName());
                return n1 - n2;
            }

            private int extractOffset(String name) {
                int i = 0;
                try {
                    int s = name.lastIndexOf('_') + 1;
                    int e = name.lastIndexOf(inputFileSuffix);
                    String number = name.substring(s, e);
                    i = Integer.parseInt(number);
                } catch (Exception e) {
                    i = 0; // if filename does not match the format
                    // then default to 0
                }
                return i;
            }
        };
    }
}
