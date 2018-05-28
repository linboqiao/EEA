package edu.cmu.cs.lti.uima.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import edu.cmu.cs.lti.uima.util.TimeConstants.DayOfWeek;
import edu.cmu.cs.lti.uima.util.TimeConstants.Month;

/**
 * A utility class for handling time. This class is currely relying on java.util.Calendar. However,
 * note that the Calendar class and java.util.Date are very badly desgined. As such, we might want
 * to reimplement this class by relying on another class.
 * 
 * @author Jun Araki
 */
public class TimeUtils {

  /**
   * Returns the current date in the format of MMDDYYYY.
   * 
   * @return the current date in the format of MMDDYYYY
   */
  public static String getCurrentMMDDYYYY() {
    StringBuilder buf = new StringBuilder();
    Calendar cal = new GregorianCalendar();

    String year = Integer.toString(cal.get(Calendar.YEAR));
    String month = padZeroToLeft(cal.get(Calendar.MONTH) + 1, 2);
    String day = padZeroToLeft(cal.get(Calendar.DAY_OF_MONTH), 2);

    buf.append(month);
    buf.append(day);
    buf.append(year);

    return buf.toString();
  }

  /**
   * Returns the current date in the format of YYYYMMDD.
   * 
   * @return the current date in the format of YYYYMMDD
   */
  public static String getCurrentYYYYMMDD() {
    StringBuilder buf = new StringBuilder();
    Calendar cal = new GregorianCalendar();

    String year = Integer.toString(cal.get(Calendar.YEAR));
    String month = padZeroToLeft(cal.get(Calendar.MONTH) + 1, 2);
    String day = padZeroToLeft(cal.get(Calendar.DAY_OF_MONTH), 2);

    buf.append(year);
    buf.append(month);
    buf.append(day);

    return buf.toString();
  }

  /**
   * Returns the current timestamp
   * @return
   */
  public static long getCurrentTimestamp() {
    Calendar cal = Calendar.getInstance();
    Date now = cal.getTime();
    Timestamp currentTimestamp = new Timestamp(now.getTime());
    return currentTimestamp.getTime();
  }

  /**
   * Prints out elapsed time in milliseconds.
   * 
   * @param start
   * @param end
   */
  public static void printElapsedTimeInSeconds(long start, long end) {
    System.out.println("Elapsed time: " + (end - start) / 1000 + " [s]");
  }

  private static String padZeroToLeft(int input, int totalLength) {
    String inputStr = Integer.toString(input);
    return StringUtils.leftPad(inputStr, totalLength, '0');
  }

  public static int getYear(Calendar cal) {
    return cal.get(Calendar.YEAR);
  }

  public static int getMonth(Calendar cal) {
    return findMonthByMonthForCalendar(cal.get(Calendar.MONTH)).getMonth();
  }

  public static int getDayOfMonth(Calendar cal) {
    return cal.get(Calendar.DAY_OF_MONTH);
  }

  /**
   * Returns the day of week from the specified calendar object.
   * 
   * @param cal
   * @return the day of week from the specified calendar object
   */
  public static String getDayOfWeek(Calendar cal) {
    return findDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
  }

  /**
   * Returns a gregorian calendar object from the specified year.
   * 
   * @param year
   * @return a gregorian calendar object from the specified year
   */
  public static Calendar getGregorianCalendar(int year) {
    Calendar cal = new GregorianCalendar();
    cal.clear(); // A hack for GregorianCalendar.
    cal.set(Calendar.YEAR, year);

    return cal;
  }

  /**
   * Returns a gregorian calendar object from the specified year and month.
   * 
   * @param year
   * @param month
   * @return a gregorian calendar object from the specified year and month
   */
  public static Calendar getGregorianCalendar(int year, int month) {
    Calendar cal = new GregorianCalendar();
    cal.clear(); // A hack for GregorianCalendar.
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, findMonthForCalendarByMonth(month));

    return cal;
  }

  /**
   * Returns a gregorian calendar object from the specified year, month, and day of month.
   * 
   * @param year
   * @param month
   * @param dayOfMonth
   * @return a gregorian calenader object from the specified year, month, and day of month
   */
  public static Calendar getGregorianCalendar(int year, int month, int dayOfMonth) {
    Calendar cal = new GregorianCalendar();
    cal.clear(); // A hack for GregorianCalendar.
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, findMonthForCalendarByMonth(month));
    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

    return cal;
  }

  /**
   * Returns the month corresponding to the specified month.
   * 
   * @param month
   * @return Returns the month corresponding to the specified month
   */
  public static Month findMonthByMonth(int month) {
    for (Month m : Month.values()) {
      if (m.getMonth() == month) {
        return m;
      }
    }

    return null;
  }

  /**
   * Returns the month corresponding to the specified month for the Calendar class.
   * 
   * @param monthForCalendar
   * @return the month corresponding to the specified month for the Calendar class
   */
  public static Month findMonthByMonthForCalendar(int monthForCalendar) {
    for (Month m : Month.values()) {
      if (m.getMonthForCalendar() == monthForCalendar) {
        return m;
      }
    }

    return null;
  }

  /**
   * Returns the month corresponding to the specified string.
   * 
   * @param input
   * @return the month corresponding to the specified string
   */
  public static Month findMonthByString(String input) {
    for (Month m : Month.values()) {
      if (m.toString().equals(input)) {
        return m;
      }
    }

    return null;
  }

  /**
   * Returns the calendar month corresponding to the specified month.
   * 
   * @param month
   * @return the calendar month corresponding to the specified month
   */
  public static int findMonthForCalendarByMonth(int month) {
    for (Month m : Month.values()) {
      if (m.getMonth() == month) {
        return m.getMonthForCalendar();
      }
    }

    return -1;
  }

  /**
   * Returns the day of week from the specified day of week for the Calendar class.
   * 
   * @param dayOfWeekForCalendar
   * @return the day of week from the specified day of week for the Calendar class
   */
  public static String findDayOfWeek(int dayOfWeekForCalendar) {
    for (DayOfWeek dow : DayOfWeek.values()) {
      if (dow.getDayOfWeekForCalendar() == dayOfWeekForCalendar) {
        return dow.toString();
      }
    }

    return "";
  }

  /**
   * Tests whether the specified integer represents a date of the specified format.
   * 
   * @param input
   * @param dateFormat
   * @return true if the specified integer represents a date of the specified format; false
   *         otherwise
   */
  public static boolean isValidDate(String input, String dateFormat) {
    if (StringUtils.isEmpty(input)) {
      return false;
    }

    try {
      DateFormat df = new SimpleDateFormat(dateFormat);
      df.setLenient(false);
      df.parse(input);
      return true;
    } catch (ParseException e) {
      // e.printStackTrace();
      return false;
    }
  }

  /**
   * Tests whether the specified integer represents a date in the format "MMDDYYYY".
   * 
   * @param input
   * @return true if the specified integer represents a date in the format "MMDDYYYY"; false
   *         otherwise
   */
  public static boolean isValidMMDDYYYY(String input) {
    return isValidDate(input, "MMddyyyy");
  }

  /**
   * Tests whether the specified integer represents a date in the format "YYYYMMDD".
   * 
   * @param input
   * @return true if the specified integer represents a date in the format "YYYYMMDD"; false
   *         otherwise
   */
  public static boolean isValidYYYYMMDD(String input) {
    return isValidDate(input, "yyyyMMdd");
  }

  /**
   * Tests whether the specified integer represents a year.
   * 
   * @param input
   * @return true if the specified integer represents a year; false otherwise
   */
  public static boolean isValidYear(int input) {
    if (input > 0) {
      return true;
    }

    return false;
  }

  /**
   * Tests whether the specified integer represents a dominical year or not. Since the current year
   * is 2012, we consider a year with four digits as contemporary.
   * 
   * @param input
   * @return true if the specified integer represents a dominical year; false otherwise
   */
  public static boolean isContemporaryYear(int input) {
    String inputStr = Integer.toString(input);
    if (inputStr.length() == 4 && input > 0) {
      return true;
    }

    return false;
  }

  /**
   * Tests whether the specified string represents a dominical year or not. Since the current year
   * is 2012, we consider a year with four digits as contemporary.
   * 
   * @param input
   * @return true if the specified string represents a dominical year; false otherwise
   */
  public static boolean isContemporaryYearString(String input) {
    if (StringUtils.isEmpty(input)) {
      return false;
    }

    if (NumberUtils.isNumber(input)) {
      int inputInt = Integer.parseInt(input);
      if (isContemporaryYear(inputInt)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Tests whether the specified integer represents a valid month.
   * 
   * @param month
   * @return true if the specified integer represents a valid month; false otherwise
   */
  public static boolean isValidMonth(int month) {
    if (findMonthByMonth(month) == null) {
      return false;
    }
    return true;
  }

  /**
   * Tests whether the specified string represents a valid month.
   * 
   * @param input
   * @return true if the specified string represents a valid month; false otherwise
   */
  public static boolean isValidMonthString(String input) {
    if (findMonthByString(input) == null) {
      return false;
    }
    return true;
  }

  /**
   * Tests whether the specified integer represents a day of month.
   * 
   * @param input
   * @return true if the specified integer represents a day of month; false otherwise
   */
  public static boolean isValidDayOfMonth(int input) {
    if (input >= 1 && input <= 31) {
      return true;
    }

    return false;
  }

  /**
   * Tests whether the specified string represents a day of month.
   * 
   * @param input
   * @return true if the specified string represents a day of month; false otherwise
   */
  public static boolean isValidDayOfMonthString(String input) {
    if (StringUtils.isEmpty(input)) {
      return false;
    }

    if (NumberUtils.isNumber(input)) {
      return isValidDayOfMonth(Integer.parseInt(input));
    }

    return false;
  }

  /**
   * Tests whether the specified string represents a day of month.
   * 
   * @param input
   * @return true if the specified string represents a day of week; false otherwise
   */
  public static boolean isValidDayOfWeek(String input) {
    if (StringUtils.isEmpty(input)) {
      return false;
    }

    for (DayOfWeek dow : DayOfWeek.values()) {
      if (dow.toString().equals(input)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Tests whether the specified string represents a certain duration within a day.
   * 
   * @param input
   * @return true if the specified string represents a certain duration within a day; false
   *         otherwise
   */
  public static boolean isDurationWithinOneDay(String input) {
    String inputLowerCase = input.toLowerCase();
    if ("day".equals(inputLowerCase) || "morning".equals(inputLowerCase)
            || "afternoon".equals(inputLowerCase) || "evening".equals(inputLowerCase)
            || "night".equals(inputLowerCase) || "daytime".equals(inputLowerCase)
            || "day-time".equals(inputLowerCase) || "nighttime".equals(inputLowerCase)
            || "night-time".equals(inputLowerCase)) {
      return true;
    }

    return false;
  }

  /**
   * Tests whether the specified string is a base time.
   * 
   * @param input
   * @return true if the specified string is a base time; false otherwise
   */
  public static boolean isBaseTime(String input) {
    if (isContemporaryYearString(input)) {
      return true;
    }

    if (isValidMonthString(input)) {
      return true;
    }

    if (isValidDayOfMonthString(input)) {
      return true;
    }

    if (isValidDayOfWeek(input)) {
      return true;
    }

    String inputLowerCase = input.toLowerCase();
    if ("year".equals(inputLowerCase) || "month".equals(inputLowerCase)
            || "week".equals(inputLowerCase)) {
      return true;
    }

    if ("spring".equals(inputLowerCase) || "summer".equals(inputLowerCase)
            || "fall".equals(inputLowerCase) || "autumn".equals(inputLowerCase)
            || "winter".equals(inputLowerCase)) {
      return true;
    }

    if ("tomorrow".equals(inputLowerCase) || "today".equals(inputLowerCase)
            || "yesterday".equals(inputLowerCase)) {
      return true;
    }

    if (isDurationWithinOneDay(input)) {
      return true;
    }

    return false;
  }

  /**
   * Returns the day difference between the specified two day of weeks. For example, if the first
   * one is Tuesday and the second is Thursday, then this method returns 2.
   * 
   * @param dayOfWeekBase
   * @param dayOfWeekCompared
   * @return the day difference between the specified two day of weeks
   */
  public static Integer getDayDifference(String dayOfWeekBase, String dayOfWeekCompared) {
    if (!isValidDayOfWeek(dayOfWeekBase) || !isValidDayOfWeek(dayOfWeekCompared)) {
      return null;
    }

    int indexBase, indexCompared, index;
    indexBase = indexCompared = -1;
    index = 0;
    for (DayOfWeek dow : DayOfWeek.values()) {
      if (dow.toString().equals(dayOfWeekBase)) {
        indexBase = index;
      }
      if (dow.toString().equals(dayOfWeekCompared)) {
        indexCompared = index;
      }

      if (indexBase >= 0 && indexCompared >= 0) {
        // Stops if both days of week get found.
        break;
      }

      index++;
    }

    return new Integer(indexCompared - indexBase);
  }

  /**
   * Returns the calendar object representing the date in the same week as the specified date and
   * with the specified day of week. For instance, if the specified date is 08/10/2012 (which is
   * Friday), and the specified day of week is Wednesday, then this method returns 08/08/2012.
   * 
   * @param year
   * @param month
   * @param dayOfMonth
   * @param dayOfWeekCompared
   * @return the calendar object representing the date in the same week as the specified date and
   *         with the specified day of week
   */
  public static Calendar getDateOfThisWeek(int year, int month, int dayOfMonth,
          String dayOfWeekCompared) {
    Calendar cal = TimeUtils.getGregorianCalendar(year, month, dayOfMonth);
    String dayOfWeek = TimeUtils.getDayOfWeek(cal);

    Integer dayDifference = getDayDifference(dayOfWeek, dayOfWeekCompared);
    if (dayDifference == null) {
      return null;
    }

    cal.add(Calendar.DAY_OF_YEAR, dayDifference);
    return cal;
  }

  /**
   * Returns the calendar object representing the date in the last week of the specified date and
   * with the specified day of week. For instance, if the specified date is 08/10/2012 (which is
   * Friday), and the specified day of week is Wednesday, then this method returns 08/01/2012.
   * 
   * @param year
   * @param month
   * @param dayOfMonth
   * @param dayOfWeekCompared
   * @return
   */
  public static Calendar getDateOfLastWeek(int year, int month, int dayOfMonth,
          String dayOfWeekCompared) {
    Calendar cal = TimeUtils.getGregorianCalendar(year, month, dayOfMonth);
    String dayOfWeek = TimeUtils.getDayOfWeek(cal);

    Integer dayDifference = getDayDifference(dayOfWeek, dayOfWeekCompared);
    if (dayDifference == null) {
      return null;
    }

    cal.add(Calendar.DAY_OF_YEAR, dayDifference - 7);
    return cal;
  }

  /**
   * Tests whether the specified named entity label is related to time.
   * 
   * @param input
   * @param dateFormat
   * @return true if the specified named entity label is related to time; false otherwise
   */
  public static boolean isTimeRelatedNamedEntity(String namedEntityLabel) {
    if (StringUtils.isEmpty(namedEntityLabel)) {
      return false;
    }

    if ("DATE".equals(namedEntityLabel) || "TIME".equals(namedEntityLabel)
            || "DURATION".equals(namedEntityLabel)) {
      return true;
    }

    return false;
  }

}
