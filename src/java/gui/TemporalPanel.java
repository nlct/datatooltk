/*
    Copyright (C) 2024-2025 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.dickimawbooks.datatooltk.gui;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.awt.FlowLayout;
import java.awt.CardLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.*;

import com.dickimawbooks.texparserlib.latex.datatool.Julian;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.DataToolBaseSty;
import com.dickimawbooks.datatooltk.*;

public class TemporalPanel extends JPanel implements ActionListener
{
   public TemporalPanel(DatatoolGUI gui, String tagPrefix)
   {
      super(null);

      temporalCardLayout = new CardLayout();
      setLayout(temporalCardLayout);

      this.gui = gui;

      init(tagPrefix);
   }

   private void init(String tagPrefix)
   {
      DatatoolGuiResources resources = getResources();
      locale = resources.getSettings().getDateTimeLocale();
      String pattern;
      DateFormat df;

      datetimeRow = createRow("datetime");
      add(datetimeRow, datetimeRow.getName());
      currentComponent = datetimeRow;

      df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);

      if (df instanceof SimpleDateFormat)
      {
         pattern = ((SimpleDateFormat)df).toPattern();
      }
      else
      {
         pattern = "yyyy-mm-dd hh:mm:ss";
      }

      datetimeSpinnerModel = new SpinnerDateModel();
      datetimeSpinner = new JSpinner(datetimeSpinnerModel);
      datetimeSpinner.setEditor(new JSpinner.DateEditor(datetimeSpinner, pattern));
      datetimeSpinner.setToolTipText(pattern);

      if (tagPrefix != null)
      {
         datetimeRow.add(resources.createJLabel(
           tagPrefix + ".datetime", datetimeSpinner));
      }

      datetimeRow.add(datetimeSpinner);

      zoneToggle = resources.createJCheckBox(tagPrefix, "timezone", this);
      datetimeRow.add(zoneToggle);

      zoneHrSpinnerModel = new SpinnerNumberModel( 0, -12, 12, 1);
      zoneHrSpinner = new JSpinner(zoneHrSpinnerModel);
      zoneHrSpinner.setEditor(new JSpinner.NumberEditor(zoneHrSpinner, "+00;-00"));
      datetimeRow.add(zoneHrSpinner);

      zoneMinSpinnerModel = new SpinnerNumberModel( 0, 0, 59, 1);
      zoneMinSpinner = new JSpinner(zoneMinSpinnerModel);
      zoneMinSpinner.setEditor(new JSpinner.NumberEditor(zoneMinSpinner, "00"));
      datetimeRow.add(zoneMinSpinner);

      dateRow = createRow("date");
      add(dateRow, dateRow.getName());

      df = DateFormat.getDateInstance(DateFormat.SHORT, locale);

      if (df instanceof SimpleDateFormat)
      {
         pattern = ((SimpleDateFormat)df).toPattern();
      }
      else
      {
         pattern = "yyyy-mm-dd";
      }

      dateSpinnerModel = new SpinnerDateModel();
      dateSpinner = new JSpinner(dateSpinnerModel);
      dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, pattern));
      dateSpinner.setToolTipText(pattern);

      if (tagPrefix != null)
      {
         dateRow.add(resources.createJLabel(tagPrefix+".date", dateSpinner));
      }

      dateRow.add(dateSpinner);

      timeRow = createRow("time");
      add(timeRow, timeRow.getName());

      df = DateFormat.getTimeInstance(DateFormat.SHORT, locale);

      if (df instanceof SimpleDateFormat)
      {
         pattern = ((SimpleDateFormat)df).toPattern();
      }
      else
      {
         pattern = "hh:mm:ss";
      }

      timeSpinnerModel = new SpinnerDateModel();
      timeSpinner = new JSpinner(timeSpinnerModel);
      timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, pattern));
      timeSpinner.setToolTipText(pattern);

      if (tagPrefix != null)
      {
         timeRow.add(resources.createJLabel(tagPrefix+".time", timeSpinner));
      }

      timeRow.add(timeSpinner);
   }

   protected JComponent createRow(String name)
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.setName(name);

      return comp;
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("timezone"))
      {
         if (zoneToggle.isSelected())
         {
            zoneHrSpinner.setVisible(true);
            zoneMinSpinner.setVisible(true);

            Locale curLocale = getResources().getSettings().getDateTimeLocale();

            Calendar cal = new Calendar.Builder().setCalendarType("iso8601")
             .setLocale(curLocale).setInstant(datetimeSpinnerModel.getDate()).build();

            TimeZone tz = cal.getTimeZone();

            int offset = tz.getOffset(cal.getTimeInMillis()) / 1000;

            int tzh = offset / 3600;
            int tzm = Math.abs(offset%60);

            zoneHrSpinner.setValue(Integer.valueOf(tzh));
            zoneMinSpinner.setValue(Integer.valueOf(tzm));
         }
         else
         {
            zoneHrSpinner.setVisible(false);
            zoneMinSpinner.setVisible(false);
         }
      }
   }

   protected void setTimeZone(int tzhr, int tzmin)
   {
      zoneHrSpinner.setValue(Integer.valueOf(tzhr));
      zoneMinSpinner.setValue(Integer.valueOf(tzmin));
   }

   protected int getTimeZoneHour()
   {
      return zoneHrSpinnerModel.getNumber().intValue();
   }

   protected int getTimeZoneMinute()
   {
      return zoneMinSpinnerModel.getNumber().intValue();
   }

   protected long getTimeZoneMillis()
   {
      int tzh = getTimeZoneHour();
      int tzm = getTimeZoneMinute();

      long millis = 3600000L * (long)Math.abs(tzh) + 60000L * (long)tzm;

      if (tzh < 0)
      {
         return -millis;
      }
      else
      {
         return millis;
      }
   }

   public void update(DatumType newType)
   {
      update(newType, getJulian());
   }

   public void update(DatumType newType, Julian julian)
   {
      Locale curLocale = getResources().getSettings().getDateTimeLocale();

      if (curLocale != null && !curLocale.equals(locale))
      {
         String pattern;
         DateFormat df;

         locale = curLocale;

         df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);

         if (df instanceof SimpleDateFormat)
         {
            pattern = ((SimpleDateFormat)df).toPattern();
            datetimeSpinner.setEditor(new JSpinner.DateEditor(datetimeSpinner,
              pattern));
         }

         df = DateFormat.getDateInstance(DateFormat.SHORT, locale);

         if (df instanceof SimpleDateFormat)
         {
            pattern = ((SimpleDateFormat)df).toPattern();
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, pattern));
         }

         df = DateFormat.getTimeInstance(DateFormat.SHORT, locale);

         if (df instanceof SimpleDateFormat)
         {
            pattern = ((SimpleDateFormat)df).toPattern();
            timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, pattern));
         }
      }

      Calendar calendar;

      if (julian != null)
      {
         calendar = julian.toCalendar();
      }
      else
      {
         calendar = Calendar.getInstance(locale);
      }

      Date date = calendar.getTime();

      switch (newType)
      {
         case DATE:
           temporalCardLayout.show(this, "date");
           currentComponent = dateRow;

           if (date != null)
           {
              dateSpinnerModel.setValue(date);
           }
         break;
         case TIME:
           temporalCardLayout.show(this, "time");
           currentComponent = timeRow;

           if (date != null)
           {
              timeSpinnerModel.setValue(date);
           }
         break;
         case DATETIME:
           temporalCardLayout.show(this, "datetime");
           currentComponent = datetimeRow;

           if (date != null)
           {
              datetimeSpinnerModel.setValue(date);
           }

           if (julian != null && julian.hasTimeZone())
           {
              zoneHrSpinnerModel.setValue(julian.getTimeZoneHour());
              zoneMinSpinnerModel.setValue(julian.getTimeZoneMinute());

              zoneToggle.setSelected(true);
              zoneHrSpinner.setVisible(true);
              zoneMinSpinner.setVisible(true);
           }
           else
           {
              zoneHrSpinnerModel.setValue(0);
              zoneMinSpinnerModel.setValue(0);
              zoneToggle.setSelected(false);
              zoneHrSpinner.setVisible(false);
              zoneMinSpinner.setVisible(false);
           }
         break;
      }

      currentJulian = julian;
   }

   public DatumType getDatumType()
   {
      String name = currentComponent.getName();

      if ("date".equals(name))
      {
         return DatumType.DATE;
      }
      else if ("time".equals(name))
      {
         return DatumType.TIME;
      }
      else
      {
         return DatumType.DATETIME;
      }
   }

   public Julian getJulian()
   {
      return getJulian(getDatumType());
   }

   public Julian getJulian(DatumType type)
   {
      switch (type)
      {
         case DATE:
           currentJulian = Julian.createDay(dateSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;
         case TIME:
           currentJulian = Julian.createTime(timeSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;
         case DATETIME:
           Date date = datetimeSpinnerModel.getDate();

           if (zoneToggle.isSelected())
           {
              long millis = date.getTime() - getTimeZoneMillis();
              currentJulian = Julian.createDate(
               DataToolBaseSty.unixEpochMillisToJulianDate(millis), 
                getTimeZoneHour(), getTimeZoneMinute());
           }
           else
           {
              currentJulian = Julian.createDate(date,
                 gui.getSettings().getDateTimeLocale());
           }
         break;

      }

      return currentJulian;
   }

   public Number getNumeric(DatumType type)
   {
      Julian julian = getJulian(type);

      switch (type)
      {
         case DATE:
            return Integer.valueOf(julian.getJulianDay());
         case TIME:
            return Double.valueOf(julian.getJulianTime());
         case DATETIME:
            return Double.valueOf(julian.getJulianDate());
      }

      return null;
   }

   public DatatoolGuiResources getResources()
   {
      return gui.getResources();
   }

   public MessageHandler getMessageHandler()
   {
      return gui.getMessageHandler();
   }

   DatatoolGUI gui;

   private JSpinner dateSpinner, datetimeSpinner, timeSpinner, zoneHrSpinner, zoneMinSpinner;
   private CardLayout temporalCardLayout;
   private SpinnerDateModel dateSpinnerModel, timeSpinnerModel, datetimeSpinnerModel;
   private SpinnerNumberModel zoneHrSpinnerModel, zoneMinSpinnerModel;
   private JCheckBox zoneToggle;
   private Locale locale;
   private JComponent dateRow, timeRow, datetimeRow, currentComponent;
   private Julian currentJulian;
}
