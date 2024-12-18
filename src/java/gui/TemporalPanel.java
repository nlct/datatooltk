/*
    Copyright (C) 2024 Nicola L.C. Talbot
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

import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.awt.FlowLayout;
import java.awt.CardLayout;

import javax.swing.*;

import com.dickimawbooks.texparserlib.latex.datatool.Julian;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.datatooltk.*;

public class TemporalPanel extends JPanel
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

      datetimeRow = createRow();
      add(datetimeRow, "datetime");

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

      dateRow = createRow();
      add(dateRow, "date");

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

      timeRow = createRow();
      add(timeRow, "time");

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

   protected JComponent createRow()
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING));

      return comp;
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

      Date date = null;

      if (julian != null)
      {
         date = julian.toCalendar().getTime();
      }

      switch (newType)
      {
         case DATE:
           temporalCardLayout.show(this, "date");
           if (date != null)
           {
              dateSpinnerModel.setValue(date);
           }
         break;
         case TIME:
           temporalCardLayout.show(this, "time");
           if (date != null)
           {
              timeSpinnerModel.setValue(date);
           }
         break;
         case DATETIME:
           temporalCardLayout.show(this, "datetime");
           if (date != null)
           {
              datetimeSpinnerModel.setValue(date);
           }
         break;
      }
   }

   public Julian getJulian()
   {
      if (dateRow.isVisible())
      {
         return Julian.createDay(dateSpinnerModel.getDate(),
           gui.getSettings().getDateTimeLocale());
      }
      else if (timeRow.isVisible())
      {
         return Julian.createTime(timeSpinnerModel.getDate(),
            gui.getSettings().getDateTimeLocale());
      }
      else if (datetimeRow.isVisible())
      {
         return Julian.createDate(datetimeSpinnerModel.getDate(),
            gui.getSettings().getDateTimeLocale());
      }

      // TODO
      return null;
   }

   public Julian getJulian(DatumType type)
   {
      Julian julian = null;

      switch (type)
      {
         case DATE:
           julian = Julian.createDay(dateSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;
         case TIME:
           julian = Julian.createTime(timeSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;
         case DATETIME:
           julian = Julian.createDate(datetimeSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;

      }

      return null;
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

   private JSpinner dateSpinner, datetimeSpinner, timeSpinner;
   private CardLayout temporalCardLayout;
   private SpinnerDateModel dateSpinnerModel, timeSpinnerModel, datetimeSpinnerModel;
   private Locale locale;
   private JComponent dateRow, timeRow, datetimeRow;
}
