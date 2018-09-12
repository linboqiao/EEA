package edu.cmu.cs.lti.uima.util;

import java.util.Calendar;

/**
 * Time-related constants.
 * 
 * @author Jun Araki
 */
public class TimeConstants {

  public enum Month {
    January   (1, Calendar.JANUARY),
    February  (2, Calendar.FEBRUARY),
    March     (3, Calendar.MARCH),
    April     (4, Calendar.APRIL),
    May       (5, Calendar.MAY),
    June      (6, Calendar.JUNE),
    July      (7, Calendar.JULY),
    August    (8, Calendar.AUGUST),
    September (9, Calendar.SEPTEMBER),
    October   (10, Calendar.OCTOBER),
    November  (11, Calendar.NOVEMBER),
    December  (12, Calendar.DECEMBER);

    private final int month;
    private final int monthForCalendar;

    // Constructor.
    Month(int month, int monthForCalendar) {
      this.month = month;
      this.monthForCalendar = monthForCalendar;
    }

    public int getMonth() {
      return month;
    }

    public int getMonthForCalendar() {
      return monthForCalendar;
    }
  }

  public enum DayOfWeek {
    Monday    (Calendar.MONDAY),
    Tuesday   (Calendar.TUESDAY),
    Wednesday (Calendar.WEDNESDAY),
    Thursday  (Calendar.THURSDAY),
    Friday    (Calendar.FRIDAY),
    Saturday  (Calendar.SATURDAY),
    Sunday    (Calendar.SUNDAY);

    private final int dayOfWeekForCalendar;

    // Constructor.
    DayOfWeek(int dayOfWeekForCalendar) {
      this.dayOfWeekForCalendar = dayOfWeekForCalendar;
    }

    public int getDayOfWeekForCalendar() {
      return dayOfWeekForCalendar;
    }
  }

}
