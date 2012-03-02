/*
 * Copyright 2012 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocpsoft.pretty.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.ocpsoft.pretty.time.units.Century;
import com.ocpsoft.pretty.time.units.Day;
import com.ocpsoft.pretty.time.units.Decade;
import com.ocpsoft.pretty.time.units.Hour;
import com.ocpsoft.pretty.time.units.JustNow;
import com.ocpsoft.pretty.time.units.Millennium;
import com.ocpsoft.pretty.time.units.Millisecond;
import com.ocpsoft.pretty.time.units.Minute;
import com.ocpsoft.pretty.time.units.Month;
import com.ocpsoft.pretty.time.units.Second;
import com.ocpsoft.pretty.time.units.Week;
import com.ocpsoft.pretty.time.units.Year;

/**
 * A utility for creating social-networking style timestamps. (e.g. "just now", "moments ago", "3 days ago",
 * "within 2 months")
 * <p>
 * <b>Usage:</b>
 * <p>
 * <code>
 * PrettyTime t = new PrettyTime();<br/>
 * String timestamp = t.format(new Date());<br/>
 * //result: moments from now
 * <p>
 * </code>
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com>Lincoln Baxter, III</a>
 */
public class PrettyTime
{

   private volatile Date reference;
   private volatile List<TimeUnit> timeUnits;
   private volatile Locale locale = Locale.getDefault();

   /**
    * Default constructor
    */
   public PrettyTime()
   {
      initTimeUnits(locale);
   }

   /**
    * Accept a {@link Date} timestamp to represent the point of reference for comparison. This may be changed by the
    * user, after construction.
    * <p>
    * See {@code PrettyTime.setReference(Date timestamp)}.
    * 
    * @param reference
    */
   public PrettyTime(final Date reference)
   {
      this();
      setReference(reference);
   }

   /**
    * Construct a new instance using the given {@link Locale} instead of the system default.
    */
   public PrettyTime(final Locale locale)
   {
      this.locale = locale;
      initTimeUnits(locale);
   }

   /**
    * Accept a {@link Date} timestamp to represent the point of reference for comparison. This may be changed by the
    * user, after construction. Use the given {@link Locale} instead of the system default.
    * <p>
    * See {@code PrettyTime.setReference(Date timestamp)}.
    */
   public PrettyTime(final Date reference, final Locale locale)
   {
      this(locale);
      setReference(reference);
   }

   /**
    * Calculate the approximate duration between the referenceDate and date
    * 
    * @param date
    * @return
    */
   public Duration approximateDuration(final Date then)
   {
      Date ref = reference;
      if (null == ref)
      {
         ref = new Date();
      }

      long difference = then.getTime() - ref.getTime();
      return calculateDuration(difference);
   }

   private void initTimeUnits(Locale locale)
   {
      timeUnits = new ArrayList<TimeUnit>();
      timeUnits.add(new JustNow(locale));
      timeUnits.add(new Millisecond(locale));
      timeUnits.add(new Second(locale));
      timeUnits.add(new Minute(locale));
      timeUnits.add(new Hour(locale));
      timeUnits.add(new Day(locale));
      timeUnits.add(new Week(locale));
      timeUnits.add(new Month(locale));
      timeUnits.add(new Year(locale));
      timeUnits.add(new Decade(locale));
      timeUnits.add(new Century(locale));
      timeUnits.add(new Millennium(locale));
   }

   private Duration calculateDuration(final long difference)
   {
      long absoluteDifference = Math.abs(difference);

      // Required for thread-safety
      List<TimeUnit> units = new ArrayList<TimeUnit>(timeUnits.size());
      units.addAll(timeUnits);

      Duration result = new Duration();

      for (int i = 0; i < units.size(); i++)
      {
         TimeUnit unit = units.get(i);
         long millisPerUnit = Math.abs(unit.getMillisPerUnit());
         long quantity = Math.abs(unit.getMaxQuantity());

         boolean isLastUnit = (i == units.size() - 1);

         if ((0 == quantity) && !isLastUnit)
         {
            quantity = units.get(i + 1).getMillisPerUnit() / unit.getMillisPerUnit();
         }

         // does our unit encompass the time duration?
         if ((millisPerUnit * quantity > absoluteDifference) || isLastUnit)
         {
            result.setUnit(unit);
            if (millisPerUnit > absoluteDifference)
            {
               // we are rounding up: get 1 or -1 for past or future
               result.setQuantity(getSign(difference, absoluteDifference));
            }
            else
            {
               result.setQuantity(difference / millisPerUnit);
            }
            result.setDelta(difference - result.getQuantity() * millisPerUnit);
            break;
         }

      }
      return result;
   }

   private long getSign(final long difference, final long absoluteDifference)
   {
      if (0 > difference)
      {
         return -1;
      }
      else
      {
         return 1;
      }
   }

   /**
    * Calculate to the precision of the smallest provided {@link TimeUnit}, the exact duration represented by the
    * difference between the reference timestamp, and {@code then}
    * <p>
    * <b>Note</b>: Precision may be lost if no supplied {@link TimeUnit} is granular enough to represent one millisecond
    * 
    * @param then The date to be compared against the reference timestamp, or <i>now</i> if no reference timestamp was
    *           provided
    * @return A sorted {@link List} of {@link Duration} objects, from largest to smallest. Each element in the list
    *         represents the approximate duration (number of times) that {@link TimeUnit} to fit into the previous
    *         element's delta. The first element is the largest {@link TimeUnit} to fit within the total difference
    *         between compared dates.
    */
   public List<Duration> calculatePreciseDuration(final Date then)
   {
      if (null == reference)
      {
         reference = new Date();
      }

      List<Duration> result = new ArrayList<Duration>();
      long difference = then.getTime() - reference.getTime();
      Duration duration = calculateDuration(difference);
      result.add(duration);
      while (0 != duration.getDelta())
      {
         duration = calculateDuration(duration.getDelta());
         result.add(duration);
      }
      return result;
   }

   /**
    * Format the given {@link Date} object. This method applies the {@code PrettyTime.approximateDuration(date)} method
    * to perform its calculation. If {@code then} is null, it will default to {@code new Date()}
    * 
    * @param duration the {@link Date} to be formatted
    * @return A formatted string representing {@code then}
    */
   public String format(Date then)
   {
      if (then == null)
      {
         then = new Date();
      }
      Duration d = approximateDuration(then);
      return format(d);
   }

   /**
    * Format the given {@link Duration} object, using the {@link TimeFormat} specified by the {@link TimeUnit} contained
    * within
    * 
    * @param duration the {@link Duration} to be formatted
    * @return A formatted string representing {@code duration}
    */
   public String format(final Duration duration)
   {
      TimeFormat format = duration.getUnit().getFormat();
      String time = format.format(duration);
      return format.decorate(duration, time);
   }

   /**
    * Format the given {@link Duration} objects, using the {@link TimeFormat} specified by the {@link TimeUnit}
    * contained within. Rounds only the last {@link Duration} object.
    * 
    * @param durations the {@link Duration}s to be formatted
    * @return A list of formatted strings representing {@code durations}
    */
   public String format(final List<Duration> durations)
   {
      String result = null;
      if (durations != null) {
         StringBuilder builder = new StringBuilder();
         Duration duration = null;
         TimeFormat format = null;
         for (int i = 0; i < durations.size(); i++) {
            duration = durations.get(i);
            boolean isLast = (i == durations.size() - 1);
            format = duration.getUnit().getFormat();
            if (!isLast) {
               builder.append(format.formatUnrounded(duration));
               builder.append(" ");
            }
            else {
               builder.append(format.format(duration));
            }
         }
         result = format.decorate(duration, builder.toString());
      }
      return result;
   }

   /**
    * Get the current reference timestamp.
    * <p>
    * See {@code PrettyTime.setReference(Date timestamp)}
    * 
    * @return
    */
   public Date getReference()
   {
      return reference;
   }

   /**
    * Set the reference timestamp.
    * <p>
    * If the Date formatted is before the reference timestamp, the format command will produce a String that is in the
    * past tense. If the Date formatted is after the reference timestamp, the format command will produce a string that
    * is in the future tense.
    */
   public void setReference(final Date timestamp)
   {
      reference = timestamp;
   }

   /**
    * Get a {@link List} of the current configured {@link TimeUnit} instances in calculations.
    * 
    * @return
    */
   public List<TimeUnit> getUnits()
   {
      return Collections.unmodifiableList(timeUnits);
   }

   /**
    * Set the current configured {@link TimeUnit} instances to be used in calculations
    */
   public void setUnits(final List<TimeUnit> units)
   {
      this.timeUnits = units;
   }

   /**
    * Set the available {@link TimeUnit} instances with which this {@link PrettyTime} instance will segment any
    * calculated {@link Duration}.
    */
   public void setUnits(final TimeUnit... units)
   {
      this.timeUnits = Arrays.asList(units);
   }

   /**
    * Get the currently configured {@link Locale} for this {@link PrettyTime} object.
    */
   public Locale getLocale()
   {
      return locale;
   }

   /**
    * Set the the {@link Locale} for this {@link PrettyTime} object. This may be an expensive operation, since this
    * operation calls {@link TimeUnit#setLocale(Locale)} for each {@link TimeUnit} in {@link #getUnits()}.
    */
   public void setLocale(final Locale locale)
   {
      this.locale = locale;
      for (TimeUnit unit : timeUnits) {
         unit.setLocale(locale);
      }
   }

   @Override
   public String toString()
   {
      return "PrettyTime [reference=" + reference + ", locale=" + locale + "]";
   }

}
