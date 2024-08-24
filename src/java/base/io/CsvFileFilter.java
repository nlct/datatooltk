/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.datatooltk.base.io;

import java.io.File;
import javax.swing.filechooser.FileFilter;

import com.dickimawbooks.datatooltk.base.DatatoolTk;
import com.dickimawbooks.datatooltk.base.MessageHandler;

/**
 * Class representing a CSV file filter
 */
public class CsvFileFilter extends DatatoolFileFilter
{
   public CsvFileFilter(MessageHandler messageHandler)
   {
      super(messageHandler.getLabelWithValues("filter.csv", "*.csv, *.tsv"),
         "csv", "tsv");
   }

}
