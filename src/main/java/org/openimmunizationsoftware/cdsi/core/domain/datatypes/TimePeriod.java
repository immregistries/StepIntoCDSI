package org.openimmunizationsoftware.cdsi.core.domain.datatypes;

import java.util.Calendar;
import java.util.Date;

public class TimePeriod {
  private String originalValue = "";
  private int amount = 0;
  private TimePeriodType type = null;
  private TimePeriod child = null;
  private boolean valued = false;

  public boolean isValued() {
    return valued;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public TimePeriodType getType() {
    return type;
  }

  public void setType(TimePeriodType type) {
    this.type = type;
  }

  public TimePeriod getChild() {
    return child;
  }

  public void setChild(TimePeriod child) {
    this.child = child;
  }

  public TimePeriod(String value) {
    setValue(value);
  }

  public String getOriginalValue() {
    return originalValue;
  }

  public void setValue(String value) {
    valued = false;
    value = value.trim();
    this.originalValue = value;
    this.type = TimePeriodType.DAY;
    this.child = null;
    if (value.length() == 0 || value.equalsIgnoreCase("n/a")) {
      return;
    }
    valued = true;

    boolean negative = false;
    if (value.startsWith("-")) {
      negative = true;
      value = value.substring(1).trim();
    } else if (value.startsWith("+")) {
      negative = false;
      value = value.substring(1).trim();
    }
    int spacePos = -1;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') {
        spacePos = i;
        break;
      }
    }
    if (spacePos == -1) {
      throw new IllegalArgumentException("Unrecognized interval '" + value + "'");
    }
    String numberValue = value.substring(0, spacePos);
    try {
      amount = Integer.parseInt(numberValue);
    } catch (NumberFormatException nfe) {
      throw new IllegalArgumentException("Unable read number '" + numberValue + "'");
    }
    value = value.substring(spacePos).trim();
    if (negative) {
      amount = -amount;
    }
    spacePos = value.indexOf(' ');
    int negatePos = value.indexOf('-');
    int positivePos = value.indexOf('+');
    if (spacePos == -1) {
      spacePos = value.length();
    }
    if (negatePos != -1 && negatePos < spacePos) {
      spacePos = negatePos;
    }
    if (positivePos != -1 && positivePos < spacePos) {
      spacePos = positivePos;
    }
    String typeString = value.substring(0, spacePos);
    if (typeString.equalsIgnoreCase("day") || typeString.equalsIgnoreCase("days")
        || typeString.equalsIgnoreCase("d") || typeString.equalsIgnoreCase("da")) {
      type = TimePeriodType.DAY;
    } else if (typeString.equalsIgnoreCase("week") || typeString.equalsIgnoreCase("weeks")
        || typeString.equalsIgnoreCase("w") || typeString.equalsIgnoreCase("we")) {
      type = TimePeriodType.WEEK;
    } else if (typeString.equalsIgnoreCase("month") || typeString.equalsIgnoreCase("months")
        || typeString.equalsIgnoreCase("m") || typeString.equalsIgnoreCase("mo")) {
      type = TimePeriodType.MONTH;
    } else if (typeString.equalsIgnoreCase("year") || typeString.equalsIgnoreCase("years")
        || typeString.equalsIgnoreCase("y") || typeString.equalsIgnoreCase("ye")
        || typeString.equalsIgnoreCase("yr") || typeString.equalsIgnoreCase("yrs")) {
      type = TimePeriodType.YEAR;
    } else {
      throw new IllegalArgumentException("Unrecognized interval type '" + typeString + "'");
    }
    value = value.substring(spacePos).trim();
    if (value.length() > 0) {
      child = new TimePeriod(value);
    }
  }

  @Override
  public String toString() {
    if (!valued) {
      return "n/a";
    }
    StringBuilder sb = new StringBuilder();
    if (amount < 0) {
      sb.append("- ");
    }
    sb.append(Math.abs(amount));
    sb.append(" ");
    if (type == TimePeriodType.DAY) {
      sb.append("day");
    } else if (type == TimePeriodType.WEEK) {
      sb.append("week");
    } else if (type == TimePeriodType.MONTH) {
      sb.append("month");
    } else if (type == TimePeriodType.YEAR) {
      sb.append("year");
    }
    if (amount != 1) {
      sb.append("s");
    }
    if (child != null) {
      sb.append(" ");
      sb.append(child.toString());
    }
    return sb.toString();
  }

  public Date getDateFrom(Date date) {
    if (!valued) {
      return null;
    }
    Calendar startingDate = Calendar.getInstance();
    startingDate.setTime(date);
    int year = startingDate.get(Calendar.YEAR);
    int month = startingDate.get(Calendar.MONTH) + 1;
    Calendar endingDate = Calendar.getInstance();
    endingDate.setTime(date);
    endingDate.set(Calendar.HOUR, 0);
    endingDate.set(Calendar.MINUTE, 0);
    endingDate.set(Calendar.SECOND, 0);
    endingDate.set(Calendar.MILLISECOND, 0);

    if (type == TimePeriodType.DAY) {
      endingDate.add(Calendar.DAY_OF_MONTH, amount);
    } else if (type == TimePeriodType.MONTH) {
      month = month + amount;
      if (month < 1) {
        while (month < 1) {
          year = year - 1;
          month = month + 12;
        }
      } else if (month > 12) {
        while (month > 12) {
          year = year + 1;
          month = month - 12;
        }
      }
      endingDate.set(Calendar.MONTH, month - 1);
      endingDate.set(Calendar.YEAR, year);
    } else if (type == TimePeriodType.WEEK) {
      endingDate.add(Calendar.DAY_OF_MONTH, amount * 7);
    } else if (type == TimePeriodType.YEAR) {
      year = year + amount;
      endingDate.set(Calendar.YEAR, year);
    }
    // CALCDT-5
    if (endingDate.get(Calendar.DAY_OF_MONTH) > endingDate
        .getActualMaximum(Calendar.DAY_OF_MONTH)) {
      month = month + 1;
      if (month > 12) {
        year = year + 1;
        month = 1;
      }
      endingDate.set(Calendar.MONTH, month - 1);
      endingDate.set(Calendar.YEAR, year);
      endingDate.set(Calendar.DAY_OF_MONTH, 1);
    }

    if (child != null) {
      return child.getDateFrom(endingDate.getTime());
    }
    return endingDate.getTime();
  }
}
