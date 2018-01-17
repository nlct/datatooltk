/*
    Copyright (C) 2013 Nicola L.C. Talbot
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
package com.dickimawbooks.datatooltk;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.Random;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.*;
import java.util.Date;
import java.util.Locale;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.dickimawbooks.datatooltk.io.*;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.PreambleParser;
import com.dickimawbooks.texparserlib.latex.datatool.*;

/**
 * Class representing a database.
 */
public class DatatoolDb
{
   private DatatoolDb()
   {
      headers = new Vector<DatatoolHeader>();
      data = new Vector<DatatoolRow>();
   }

   public DatatoolDb(DatatoolSettings settings)
   {
      if (settings == null)
      {
         throw new NullPointerException();
      }

      this.settings = settings;

      headers = new Vector<DatatoolHeader>(settings.getInitialColumnCapacity());
      data = new Vector<DatatoolRow>(settings.getInitialRowCapacity());
   }

   public DatatoolDb(DatatoolSettings settings, int rows, int cols)
   {
      if (settings == null)
      {
         throw new NullPointerException();
      }

      this.settings = settings;
      headers = new Vector<DatatoolHeader>(cols);
      data = new Vector<DatatoolRow>(rows);
   }

   public DatatoolDb(DatatoolSettings settings, int cols)
   {
      if (settings == null)
      {
         throw new NullPointerException();
      }

      this.settings = settings;
      headers = new Vector<DatatoolHeader>(cols);
      data = new Vector<DatatoolRow>(settings.getInitialRowCapacity());
   }

   public static DatatoolDb load(DatatoolSettings settings,
      String filename)
     throws IOException
   {
      return load(settings, new File(filename));
   }

   public static DatatoolDb load(DatatoolSettings settings, 
     File dbFile)
     throws IOException
   {
      DatatoolDb db = null;
      boolean hasVerbatim = false;
      String encoding = settings.getTeXEncoding();

      MessageHandler messageHandler = settings.getMessageHandler();
      TeXApp texApp = messageHandler.getTeXApp();

      PreambleParser preambleParser = new PreambleParser(texApp);
      TeXParser texParser = new TeXParser(preambleParser);

      DataToolSty sty = new DataToolSty(null, preambleParser);
      preambleParser.usepackage(sty);
      preambleParser.addVerbEnv("lstlisting");
      preambleParser.addVerbEnv("alltt");

      texParser.addVerbCommand("lstinline");

      try
      {
         messageHandler.startBuffering();

         if (encoding == null || encoding.isEmpty())
         {
            texParser.parse(dbFile);
         }
         else
         {
            texParser.parse(dbFile, Charset.forName(encoding));
         }

         ControlSequence cs = texParser.getControlSequence("dtllastloadeddb");
         String dbName = null;
   
         if (cs != null && cs instanceof Expandable)
         {
            // should be expandable since it's defined in the .dbtex
            // file using \def\dtllastloadeddb{name}
   
            TeXObjectList expanded = ((Expandable)cs).expandfully(texParser);
   
            if (expanded != null)
            {
               dbName = expanded.toString(texParser);
            }
         }
         else
         {
            Iterator<String> it = sty.getDataBaseKeySetIterator();
   
            if (it == null)
            {
               // most likely an old .dbtex file that doesn't
               // contain \dtllastloadeddb assignment. Find the
               // first token register whose name matches
               // \dtlkeys@<label>

               Pattern p = Pattern.compile("dtlkeys@(.*)");

               TokenRegister reg = texParser.getTokenRegister(p);

               if (reg == null)
               {
                  throw new IOException(messageHandler.getLabelWithValues(
                    "error.dbload.missing_data", dbFile));
               }

               Matcher m = p.matcher(reg.getName());

               if (m.matches())
               {
                  dbName = m.group(1);
               }
            }
            else
            { 
               for (; it.hasNext(); )
               {
                  if (dbName != null)
                  {
                     throw new IOException(messageHandler.getLabelWithValues(
                       "error.dbload.multiple_db_found", dbFile));
                  }
   
                  dbName = it.next();
               }
            }
         }
   
         if (dbName == null)
         {// shouldn't happen unless file has code missing
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.missing_data", dbFile));
         }

         DataBase texDb = sty.getDataBase(dbName);
   
         int rowCount = texDb.getRowCount();
         int columnCount = texDb.getColumnCount();

         messageHandler.debug(String.format("%s (%dx%d)", dbName,
          rowCount, columnCount));
   
         db = new DatatoolDb(settings, rowCount, columnCount);
         db.setFile(dbFile);
         db.setName(dbName);
   
         DataToolHeaderRow headerRow = texDb.getHeaders();
   
         for (int i = 0; i < columnCount; i++)
         {
            DataToolHeader header = headerRow.getHeader(i+1);
   
            String key = header.getColumnLabel();
            String title = header.getTitle().toString(texParser);
   
            int type = header.getType();
   
            db.addColumn(new DatatoolHeader(db, key, title, type));
         }
   
         DataToolRows contents = texDb.getData();
   
         for (int i = 0; i < rowCount; i++)
         {
            DataToolEntryRow texRow = contents.get(i);
   
            DatatoolRow row = new DatatoolRow(db, columnCount);
            db.data.add(row);

            for (int j = 0; j < columnCount; j++)
            {
               row.add(NULL_VALUE);
            }

            for (DataToolEntry entry : texRow)
            { 
               int columnIndex = entry.getColumnIndex()-1;

               TeXObject entryContents = null;
   
               if (entry != null)
               {
                  entryContents = entry.getContents();
               }
   
               if (entryContents != null)
               {
                  if (!hasVerbatim && entryContents instanceof TeXObjectList)
                  {
                     hasVerbatim = preambleParser.containsVerbatim(
                        entryContents);
                  }

                  if (entryContents instanceof TeXObjectList
                      && ((TeXObjectList)entryContents).size() > 0)
                  {
                     TeXObjectList list = (TeXObjectList)entryContents;
                     TeXObject obj = list.lastElement();

                     if (obj instanceof Comment 
                         && ((Comment)obj).isEmpty())
                     {
                        list.remove(list.size()-1);
                     }
                  }
   
                  row.setCell(columnIndex, entryContents.toString(texParser));
               }
            }
         }
      }
      finally
      {
         messageHandler.stopBuffering();
      }

      if (hasVerbatim)
      {
         messageHandler.warning(messageHandler.getLabel("warning.verb_detected"));
      }

      return db;
   }
      
   private static DatatoolDb originalLoad(DatatoolSettings settings, 
     File dbFile)
     throws IOException
   {
      DatatoolDb db = null;
      boolean hasVerbatim = false;
      LineNumberReader in = null;
      String encoding = settings.getTeXEncoding();
      MessageHandler messageHandler = settings.getMessageHandler();

      try
      {
         FileInputStream fis = new FileInputStream(dbFile);

         if (encoding == null)
         {
            in = new LineNumberReader(new InputStreamReader(fis));
         }
         else
         {
            in = new LineNumberReader(new InputStreamReader(fis, encoding));
         }

         db = new DatatoolDb(settings);

         // Read until we find \newtoks\csname dtlkeys@<name>\endcsname

         String controlSequence = null;

         while ((controlSequence = db.readCommand(in)) != null)
         {
            if (controlSequence.equals("\\newtoks"))
            {
               controlSequence = db.readCommand(in);

               if ("\\csname".equals(controlSequence))
               {
                  break;
               }
            }
         }

         if (controlSequence == null)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", "\\newtoks\\csname"));
         }

         String name = readUntil(in, "\\endcsname");

         if (name == null)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", "\\endcsname"));
         }

         if (!name.startsWith("dtlkeys@"))
         {
            throw new IOException(messageHandler.getLabelWithValues
              (
                 in.getLineNumber(),
                 "error.expected",
                 "\\newtoks\\csname dtlkeys@<name>\\endcsname"
              ));
         }

         name = name.substring(8);

         db.setName(name);

         // Now look for \csname dtlkeys@<name>\endcsname

         controlSequence = null;

         while ((controlSequence = db.readCommand(in)) != null)
         {
            if (controlSequence.equals("\\csname"))
            {
               if (readUntil(in, "dtlkeys@"+name+"\\endcsname") != null)
               {
                  break;
               }
            }
         }

         if (controlSequence == null)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
                "\\csname dtlkeys@"+name+"\\endcsname"));
         }

         int c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
                "\\csname dtlkeys@"+name+"\\endcsname="));
         }
         else if (c != (int)'=')
         {
            throw new IOException(messageHandler.getLabelWithValues(
               in.getLineNumber(),
              "error.expected_found", 
                  "\\csname dtlkeys@"+name+"\\endcsname=",
                  "\\csname dtlkeys@"+name+"\\endcsname"+((char)c)
               ));
         }

         c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
                "\\csname dtlkeys@"+name+"\\endcsname={"));
         }
         else if (c != (int)'{')
         {
            throw new IOException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected_found", 
                  "\\csname dtlkeys@"+name+"\\endcsname={",
                  "\\csname dtlkeys@"+name+"\\endcsname"+((char)c)
               ));
         }

         int currentColumn = 0;

         while (true)
         {
            db.readCommand(in, "\\db@plist@elt@w");

            currentColumn = db.parseHeader(in, currentColumn);

            in.mark(80);

            c = readChar(in, true);

            if (c == (int)'}')
            {
               // Finished
               break;
            }
            else if (c == -1)
            {
               throw new IOException(messageHandler.getLabelWithValues
                 (
                  "error.dbload.not_found",
                  in.getLineNumber(),
                  "}"
                 ));
            }
            else
            {
               in.reset();
            }
         }

         // Now read in the database contents

         while ((controlSequence = db.readCommand(in)) != null)
         {
            if (controlSequence.equals("\\newtoks"))
            {
               controlSequence = db.readCommand(in);

               if ("\\csname".equals(controlSequence))
               {
                  break;
               }
            }
         }

         if (controlSequence == null)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", "\\newtoks\\csname"));
         }

         String contents = readUntil(in, "\\endcsname");

         if (contents == null)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
             "\\newtoks\\csname dtldb@"+name+"\\endcsname"));
         }
         else if (!contents.equals("dtldb@"+name))
         {
            throw new IOException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected_found",
                  "\\newtoks\\csname dtldb@"+name+"\\endcsname",
                  "\\newtoks\\csname "+contents+"\\endcsname"
              ));
         }

         contents = readUntil(in, "\\csname");

         if (contents == null)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
             "\\csname dtldb@"+name+"\\endcsname="));
         }

         // skip any whitespace

         c = readChar(in, true);

         contents = readUntil(in, "\\endcsname");

         if (contents == null)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
             "\\csname dtldb@"+name+"\\endcsname="));
         }

         contents = (""+(char)c)+contents;

         if (!contents.equals("dtldb@"+name))
         {
            throw new IOException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected_found",
                  "\\csname dtldb@"+name+"\\endcsname",
                  "\\csname "+contents+"\\endcsname"
              ));
         }

         // Look for ={ assignment

         c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
                "\\csname dtldb@"+name+"\\endcsname="));
         }
         else if (c != (int)'=')
         {
            throw new IOException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected_found", 
                  "\\csname dtldb@"+name+"\\endcsname=",
                  "\\csname dtldb@"+name+"\\endcsname"+((char)c)
               ));
         }

         c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(messageHandler.getLabelWithValues(
              "error.dbload.not_found", 
                "\\csname dtldb@"+name+"\\endcsname={"));
         }
         else if (c != (int)'{')
         {
            throw new IOException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected_found", 
                  "\\csname dtldb@"+name+"\\endcsname={",
                  "\\csname dtldb@"+name+"\\endcsname"+((char)c)
               ));
         }

         // Read row data until we reach the closing }

         int currentRow = 0;

         while (true)
         {
            in.mark(80);

            c = readChar(in, true);

            if (c == (int)'}')
            {
               // Finished
               break;
            }
            else if (c == -1)
            {
               throw new IOException(messageHandler.getLabelWithValues
                 (
                  "error.dbload.not_found",
                  in.getLineNumber(),
                  "}"
                 ));
            }
            else
            {
               in.reset();
            }

            currentRow = db.parseRow(in, currentRow);
         }

         db.setFile(dbFile);
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      if (hasVerbatim)
      {
         messageHandler.warning(messageHandler.getLabel("warning.verb_detected"));
      }

      return db;
   }

   private int parseRow(LineNumberReader in, int currentRow)
     throws IOException
   {
      readCommand(in, "\\db@row@elt@w");

      readCommand(in, "\\db@row@id@w");

      String contents = readUntil(in, "\\db@row@id@end@");

      try
      {
         int num = Integer.parseInt(contents);

         if (num == currentRow)
         {
            // We've finished with this row

            return currentRow;
         }

         currentRow = num;

         DatatoolRow row;

         // Have rows been defined out of order?
         // (Row index starts at 1)

         if (currentRow < data.size())
         {
            row = data.get(currentRow-1);
         }
         else
         {
            row = insertRow(currentRow-1);
         }

         // Populate row with null values in case any entries are
         // missing.

         for (int i = 0, n = row.size(); i < n; i++)
         {
            row.set(i, NULL_VALUE);
         }
      }
      catch (NumberFormatException e)
      {
         throw new IOException(getMessageHandler().getLabelWithValues
           (
              in.getLineNumber(),
              "error.dbload.invalid_row_id",
              contents
           ), e);
      }

      parseEntry(in, currentRow);

      return currentRow;
   }

   private void parseEntry(LineNumberReader in, int currentRow)
     throws IOException
   {
      String controlSequence = readCommand(in);

      if (controlSequence == null)
      {
         throw new IOException(getMessageHandler().getLabelWithValues
           (
              in.getLineNumber(),
              "error.expected",
              "\\db@row@elt@w"
           ));
      }

      if (controlSequence.equals("\\db@row@id@w"))
      {
         // Finished. Read in end marker.

         String contents = readUntil(in, "\\db@row@id@end@");

         try
         {
            int num = Integer.parseInt(contents);

            if (num != currentRow)
            {
               throw new IOException(getMessageHandler().getLabelWithValues
                 (
                    in.getLineNumber(),
                    "error.dbload.wrong_end_row_tag",
                       currentRow, num
                 ));
            }
         }
         catch (NumberFormatException e)
         {
            throw new IOException(getMessageHandler().getLabelWithValues
              (
                 in.getLineNumber(),
                 "error.dbload.invalid_row_id",
                 contents
              ), e);
         }

         readCommand(in, "\\db@row@elt@end@");

         return;
      }

      if (!controlSequence.equals("\\db@col@id@w"))
      {
         throw new IOException(getMessageHandler().getLabelWithValues
           (
               in.getLineNumber(),
              "error.expected_found",
                 "\\db@col@id@w",
                 controlSequence
           ));
      }

      String contents = readUntil(in, "\\db@col@id@end@");

      if (contents == null)
      {
         throw new IOException(getMessageHandler().getLabelWithValues
           (
               in.getLineNumber(),
              "error.expected",
              "\\db@col@id@end@"
           ));
      }

      int currentColumn;

      try
      {
         currentColumn = Integer.parseInt(contents);
      }
      catch (NumberFormatException e)
      {
         throw new IOException(getMessageHandler().getLabelWithValues
           (
              in.getLineNumber(),
              "error.dbload.invalid_col_id",
              contents
           ), e);
      }

      readCommand(in, "\\db@col@elt@w");

      contents = readUntil(in, "\\db@col@elt@end@", false);

      if (contents == null)
      {
         throw new IOException(getMessageHandler().getLabelWithValues
           (
               in.getLineNumber(),
              "error.expected",
                 "\\db@col@elt@end@"
           ));
      }

      // Trim any final %\n

      contents = contents.replaceFirst("([^\\\\](?:\\\\\\\\)*)%\\s*\\z", "$1");

      DatatoolRow row = data.get(currentRow-1);

      row.set(currentColumn-1, contents);

      readCommand(in, "\\db@col@id@w");

      contents = readUntil(in, "\\db@col@id@end@");

      try
      {
         int num = Integer.parseInt(contents);

         if (num != currentColumn)
         {
            throw new IOException(getMessageHandler().getLabelWithValues
              (
                 in.getLineNumber(),
                 "error.dbload.wrong_end_col_tag",
                 currentColumn,
                 num
              ));
         }
      }
      catch (NumberFormatException e)
      {
         throw new IOException(getMessageHandler().getLabelWithValues
           (
              in.getLineNumber(),
              "error.dbload.invalid_col_id",
              contents
           ), e);
      }

      parseEntry(in, currentRow);
   }

   private int parseHeader(LineNumberReader in, int currentColumn)
     throws IOException
   {
      String controlSequence = readCommand(in);

      if (controlSequence == null)
      {
         throw new IOException(getMessageHandler().getLabelWithValues(
            "error.dbload.not_found", "\\db@plist@elt@end@"));
      }

      if (controlSequence.equals("\\db@plist@elt@end@"))
      {
         return currentColumn; // finished
      }

      if (controlSequence.equals("\\db@col@id@w"))
      {
         String content = readUntil(in, "\\db@col@id@end@");

         if (content == null)
         {
            throw new IOException(getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@col@id@end@"));
         }

         try
         {
            currentColumn = Integer.parseInt(content);
         }
         catch (NumberFormatException e)
         {
             throw new IOException(getMessageHandler().getLabelWithValues
             (
                in.getLineNumber(),
                "error.dbload.invalid_col_id",
                content
             ), e);
         }

         // Do we have a column with this index?
         // (This may be the terminating tag or columns may be
         // listed without order.)

         if (headers.size() < currentColumn)
         {
            insertColumn(currentColumn-1);
         }
      }
      else if (controlSequence.equals("\\db@key@id@w"))
      {
         String content = readUntil(in, "\\db@key@id@end@");

         if (content == null)
         {
            throw new IOException(getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@key@id@end@"));
         }

         // Get the header for the current column and set this key

         if (headers.size() < currentColumn)
         {
            throw new IOException(getMessageHandler().getLabelWithValues
             (
                "error.db.load.expected_found",
                   in.getLineNumber(),
                   "\\db@col@id@w",
                   "\\db@key@id@w"
             ));
         }

         DatatoolHeader header = headers.get(currentColumn-1);

         header.setKey(content);
      }
      else if (controlSequence.equals("\\db@header@id@w"))
      {
         String content = readUntil(in, "\\db@header@id@end@");

         if (content == null)
         {
            throw new IOException(getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@header@id@end@"));
         }

         // Get the header for the current column and set this title

         if (headers.size() < currentColumn)
         {
            throw new IOException(getMessageHandler().getLabelWithValues
             (
                "error.db.load.expected_found",
                   in.getLineNumber(),
                   "\\db@col@id@w",
                   "\\db@header@id@w"
             ));
         }

         DatatoolHeader header = headers.get(currentColumn-1);

         header.setTitle(content);
      }
      else if (controlSequence.equals("\\db@type@id@w"))
      {
         String content = readUntil(in, "\\db@type@id@end@");

         if (content == null)
         {
            throw new IOException(getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@type@id@end@"));
         }

         int type = DatatoolSettings.TYPE_UNKNOWN;

         try
         {
            if (!content.isEmpty())
            {
               type = Integer.parseInt(content);
            }
 
            // Get the header for the current column and set this title

            if (headers.size() < currentColumn)
            {
               throw new IOException(getMessageHandler().getLabelWithValues
                (
                   "error.db.load.expected_found",
                      ""+in.getLineNumber(),
                      "\\db@col@id@w",
                   "   \\db@header@id@w"
                ));
            }

            DatatoolHeader header = headers.get(currentColumn-1);

            header.setType(type);
         }
         catch (NumberFormatException e)
         {
             throw new IOException(getMessageHandler().getLabelWithValues
             (
                in.getLineNumber(),
                "error.dbload_unknown_type",
                content
             ), e);
         }
         catch (IllegalArgumentException e)
         {
             throw new IOException(getMessageHandler().getLabelWithValues
             (
                in.getLineNumber(),
                "error.dbload_unknown_type",
                content
             ), e);
         }

      }

      return parseHeader(in, currentColumn);
   }

   // Read in next character ignoring comments and optionally
   // whitespace

   private static int readChar(BufferedReader in, boolean ignoreSpaces)
     throws IOException
   {
      int c;

      while ((c = in.read()) != -1)
      {
         if (ignoreSpaces && Character.isWhitespace(c))
         {
            continue;
         }

         if (c == (int)'%')
         {
            in.readLine();
            continue;
         }

         return c;
      }

      return -1;
   }

   private static String readUntil(BufferedReader in, String stopPoint)
     throws IOException
   {
      return readUntil(in, stopPoint, true);
   }

   private static String readUntil(BufferedReader in, String stopPoint,
    boolean skipComments)
     throws IOException
   {
      StringBuffer buffer = new StringBuffer(256);

      int prefixLength = stopPoint.length();

      int c;

      while ((c = in.read()) != -1)
      {
         int n = buffer.length();

         if (skipComments && c == (int)'%')
         {
            // If buffer doesn't end with a backslash or if it ends
            // with an even number of backslashes, discard
            // everything up to (and including) the end of line character.

            if (n == 0 || buffer.charAt(n-1) != '\\')
            {
               in.readLine();
               continue;
            }
            else
            {
               Matcher matcher = PATTERN_END_DBSLASH.matcher(buffer);

               if (matcher.matches())
               {
                  in.readLine();
                  continue;
               }
               else
               {
                  // odd number of backslashes so we have \%

                  buffer.appendCodePoint(c);
               }
            }
         }
         else
         {
            buffer.appendCodePoint(c);
         }

         n = buffer.length();

         if (n >= prefixLength)
         {
            int idx = n-prefixLength;

            if (buffer.lastIndexOf(stopPoint, idx) != -1)
            {
               // found it

               return buffer.substring(0, idx);
            }
         }
      }

      return null;
   }


   // Returns the first command it encounters, skipping anything
   // that comes before it.
   private void readCommand(LineNumberReader in, String requiredCommand)
     throws IOException
   {
      String controlSequence = readCommand(in);

      if (controlSequence == null)
      {
         throw new IOException(getMessageHandler().getLabelWithValues(
           "error.dbload.not_found", 
             requiredCommand));
      }
      else if (!requiredCommand.equals(controlSequence))
      {
         throw new IOException(getMessageHandler().getLabelWithValues(
            in.getLineNumber(),
           "error.expected_found", 
            requiredCommand,
            controlSequence));
      }
   }

   private String readCommand(BufferedReader in)
     throws IOException
   {
      StringBuffer buffer = new StringBuffer(32);

      int c;

      in.mark(2);

      while ((c = in.read()) != -1)
      {
         if (buffer.length() == 0)
         {
            if (c == (int)'\\')
            {
               buffer.appendCodePoint(c);
            }
            else if (c == (int)'%')
            {
               // discard everything up to the end of line
               // character

               if (in.readLine() == null)
               {
                  return null; // reached end of file
               }
            }
         }
         else if (buffer.length() == 1)
         {
            buffer.appendCodePoint(c);

            // If c isn't alphabetical, we have a control symbol
            // (Remember to include @ as a letter)

            if (!(Character.isAlphabetic(c) || c == (int)'@'))
            {
               return buffer.toString();
            }

            // Is alphabetical, so we have the start of a control
            // word.
         }
         else if (Character.isAlphabetic(c) || c == (int)'@')
         {
            // Still part of control word

            buffer.appendCodePoint(c);
         }
         else
         {
            // Reached the end of the control word.
            // Discard any white space.

            while (Character.isWhitespace(c))
            {
               in.mark(2);
               c = in.read();
            }

            // Reset back to mark and return control word.

            in.reset();

            return buffer.toString();
         }

         in.mark(2);
      }

      return null;
   }

   public static boolean checkForVerbatim(String value)
   {
      for (int i = 0; i < PATTERN_VERBATIM.length; i++)
      {
         Matcher m = PATTERN_VERBATIM[i].matcher(value);

         if (m.matches()) return true;
      }

      return false;
   }


   public void save(File file)
     throws IOException
   {
      setFile(file);
      save(null, null);
   }

   public void save(String filename)
     throws IOException
   {
      setFile(filename);
      save(null, null);
   }

   public void save(String filename, int[] columnIndexes, int[] rowIndexes)
     throws IOException
   {
      setFile(filename);
      save(columnIndexes, rowIndexes);
   }

   public void save()
     throws IOException
   {
      save(null, null);
   }

   public void save(int[] columnIndexes, int[] rowIndexes)
     throws IOException
   {
      PrintWriter out = null;

      String encoding = settings.getTeXEncoding();

      try
      {
         if (encoding == null)
         {
            out = new PrintWriter(file);
         }
         else
         {
            out = new PrintWriter(file, encoding);
         }

         name = getName();

         out.print("% ");
         out.println(getMessageHandler().getLabelWithValues("default.texheader",
           DatatoolTk.APP_NAME, new Date()));
         out.println("\\DTLifdbexists{"+name+"}%");
         out.println("{\\PackageError{datatool}{Database `"+name+"'");
         out.println("already exists}{}%");
         out.println("\\aftergroup\\endinput}{}%");
         out.println("\\bgroup\\makeatletter");
         out.println("\\dtl@message{Reconstructing database");
         out.println("`"+name+"'}%");
         out.println("\\expandafter\\global\\expandafter");
         out.println("\\newtoks\\csname dtlkeys@"+name+"\\endcsname");
         out.println("\\expandafter\\global");
         out.println(" \\csname dtlkeys@"+name+"\\endcsname={%");
         out.println("%");

         for (int i = 0, n = headers.size(); i < n; i++)
         {
            DatatoolHeader header = headers.get(i);

            int colIdx = (columnIndexes == null ? i : columnIndexes[i])
                       + 1;

            int type = header.getType();

out.println("% header block for column "+colIdx);
            out.println("\\db@plist@elt@w %");
            out.println("\\db@col@id@w "+colIdx+"%");
            out.println("\\db@col@id@end@ %");
            out.println("\\db@key@id@w "+header.getKey()+"%");
            out.println("\\db@key@id@end@ %");
            out.println("\\db@type@id@w "
               +(type==DatatoolSettings.TYPE_UNKNOWN?"":type)+"%");
            out.println("\\db@type@id@end@ %");
            out.println("\\db@header@id@w "+header.getTitle()+"%");
            out.println("\\db@header@id@end@ %");
            out.println("\\db@col@id@w "+colIdx+"%");
            out.println("\\db@col@id@end@ %");
            out.println("\\db@plist@elt@end@ %");
         }

         out.println("}%"); // end of dtlkeys@<name>

         out.println("\\expandafter\\global\\expandafter");
         out.println("\\newtoks\\csname dtldb@"+name+"\\endcsname");
         out.println("\\expandafter\\global");
         out.println("\\csname dtldb@"+name+"\\endcsname={%");
         out.println("%");

         for (int i = 0, n = data.size(); i < n; i++)
         {
            DatatoolRow row = data.get(i);
            int rowIdx = (rowIndexes == null ? i : rowIndexes[i])
                       + 1;

            out.println("% Start of row "+rowIdx);
            out.println("\\db@row@elt@w %");
            out.println("\\db@row@id@w "+rowIdx+"%");
            out.println("\\db@row@id@end@ %");

            for (int j = 0, m = row.size(); j < m; j++)
            {
               String cell = row.get(j);

               if (!cell.equals(NULL_VALUE))
               {
                  int colIdx = (columnIndexes == null ? j : columnIndexes[j])
                          + 1;

                  out.println("% Column "+colIdx);
                  out.println("\\db@col@id@w "+colIdx+"%");
                  out.println("\\db@col@id@end@ %");

                  out.println("\\db@col@elt@w "+cell+"%");
                  out.println("\\db@col@elt@end@ %");

                  out.println("\\db@col@id@w "+colIdx+"%");
                  out.println("\\db@col@id@end@ %");
               }
            }

            out.println("% End of row "+rowIdx);
            out.println("\\db@row@id@w "+rowIdx+"%");
            out.println("\\db@row@id@end@ %");
            out.println("\\db@row@elt@end@ %");
         }

         out.println("}%"); // end of dtldb@<name>

         out.println("\\expandafter\\global");
         out.println(" \\expandafter\\newcount\\csname dtlrows@"
           +name+"\\endcsname");

         out.println("\\expandafter\\global");
         out.println(" \\csname dtlrows@"+name+"\\endcsname="
           +data.size()+"\\relax");

         out.println("\\expandafter\\global");
         out.println(" \\expandafter\\newcount\\csname dtlcols@"
           +name+"\\endcsname");

         out.println("\\expandafter\\global");
         out.println(" \\csname dtlcols@"+name+"\\endcsname="
           +headers.size()+"\\relax");

         for (int i = 0, n = headers.size(); i < n; i++)
         {
            DatatoolHeader header = headers.get(i);

            int colIdx = (columnIndexes == null ? i : columnIndexes[i])
                       + 1;

            out.println("\\expandafter");
            out.println(" \\gdef\\csname dtl@ci@"+name
              +"@"+header.getKey()+"\\endcsname{" +colIdx+"}%");
         }

         out.println("\\egroup");

         out.println("\\def\\dtllastloadeddb{"+name+"}");
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }

         setPermissions();
      }
   }

   public void setFile(File file)
   {
      this.file = file;
   }

   private void setPermissions()
   {
      if (settings.isOwnerOnly())
      {
         getMessageHandler().debug("Requesting owner only read/write permissions");

         file.setWritable(false, false);

         if (!file.setWritable(true, true))
         {
            getMessageHandler().debug(
             "Can't change owner-only permissions to writeable on '"
             +file+"'");

            file.setWritable(true);
         }

         file.setReadable(false, false);

         if (!file.setReadable(true, true))
         {
            getMessageHandler().debug(
             "Can't change owner-only permissions to readable on '"
             +file+"'");

            file.setReadable(true);
         }
      }
   }

   public void setFile(String filename)
   {
      setFile(new File(filename));
   }

   public File getFile()
   {
      return file;
   }

   public String getFileName()
   {
      return file == null ? null : file.getAbsolutePath();
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getName()
   {
      return name == null ? (file == null ? 
        getMessageHandler().getLabel("default.untitled") : file.getName()): name;
   }

   public void addCell(int rowIdx, int colIdx, String value)
   {
      // Do we have a column with index colIdx?

      DatatoolHeader header = getHeader(colIdx);

      if (header == null)
      {
         header = insertColumn(colIdx);
      }

      // Do we already have a row with index rowIdx ?

      DatatoolRow row = getRow(rowIdx);

      if (row == null)
      {
         row = insertRow(rowIdx);
      }

      setValue(rowIdx, colIdx, value);

   }

   // Get header from its key

   public DatatoolHeader getHeader(String key)
   {
      for (DatatoolHeader header : headers)
      {
         if (header.getKey().equals(key))
         {
            return header;
         }
      }

      return null;
   }

   public int getColumnIndex(String key)
   {
      for (int i = 0, n = headers.size(); i < n; i++)
      {
         if (headers.get(i).getKey().equals(key))
         {
            return i;
         }
      }

      return -1;
   }

   public String[] getColumnTitles()
   {
      int n = headers.size();

      String[] fields = new String[n];

      for (int i = 0; i < n; i++)
      {
         fields[i] = headers.get(i).getTitle();
      }

      return fields;
   }

   public Vector<DatatoolRow> getData()
   {
      return data;
   }

   public int getRowCount()
   {
      return data.size();
   }

   public int getColumnCount()
   {
      return headers.size();
   }

   public DatatoolRow getRow(int rowIdx)
   {
      if (rowIdx >= data.size())
      {
         return null;
      }
      else
      {
         return data.get(rowIdx);
      }
   }

   public void setHeader(int colIdx, DatatoolHeader header)
   {
      headers.set(colIdx, header);
   }

   public DatatoolHeader getHeader(int colIdx)
   {
      return headers.get(colIdx);
   }

   public int getColumnType(int colIdx)
   {
      return headers.get(colIdx).getType();
   }

   public int getType(String value)
   {
      if (value == null || value.isEmpty() || value.equals(NULL_VALUE))
      {
         return DatatoolSettings.TYPE_UNKNOWN;
      }

      try
      {
         Integer.parseInt(value);

         return DatatoolSettings.TYPE_INTEGER;
      }
      catch (NumberFormatException e)
      {
      }

      try
      {
         Float.parseFloat(value);

         return DatatoolSettings.TYPE_REAL;
      }
      catch (NumberFormatException e)
      {
      }

      try
      {
         settings.parseCurrency(value);

         return DatatoolSettings.TYPE_CURRENCY;
      }
      catch (NumberFormatException e)
      {
      }

      return DatatoolSettings.TYPE_STRING;
   }

   public int getDataType(int colIdx, String value)
   {
      DatatoolHeader header = headers.get(colIdx);

      // What's the data type of this value?

      int type = getType(value);

      // If it's unknown, return

      if (type == DatatoolSettings.TYPE_UNKNOWN)
      {
         return type;
      }

      switch (header.getType())
      {
         case DatatoolSettings.TYPE_UNKNOWN:
         case DatatoolSettings.TYPE_INTEGER:
            // All other types override unknown and int
            return type;
         case DatatoolSettings.TYPE_CURRENCY:
            // string overrides currency

            if (type == DatatoolSettings.TYPE_STRING)
            {
               return type;
            }
         break;
         case DatatoolSettings.TYPE_REAL:
            // string and currency override real
            if (type == DatatoolSettings.TYPE_STRING
             || type == DatatoolSettings.TYPE_CURRENCY)
            {
               return type;
            }
         break;
         // nothing overrides string
      }

      return header.getType();
   }

   public void setValue(int rowIdx, int colIdx, String value)
   {
      data.get(rowIdx).setCell(colIdx, value);

      int type = getDataType(colIdx, value);

      if (type != DatatoolSettings.TYPE_UNKNOWN)
      {
         headers.get(colIdx).setType(type);
      }
   }

   public Object getValue(int rowIdx, int colIdx)
   {
      DatatoolRow row = getRow(rowIdx);

      String value = row.get(colIdx);

      // What's the data type of this column?

      DatatoolHeader header = getHeader(colIdx);

      int type = header.getType();

      if (type == DatatoolSettings.TYPE_INTEGER)
      {
         if (value.isEmpty())
         {
            return Integer.valueOf(0);
         }

         try
         {
            return Integer.valueOf(value);
         }
         catch (NumberFormatException e)
         {
            // Not an integer
         }

         // Is it a float?

         try
         {
            Float num = new Float(value);

            header.setType(DatatoolSettings.TYPE_REAL);

            return num;
         }
         catch (NumberFormatException e)
         {
            // Not a float.
         }

         // Is it currency?

         try
         {
            Currency currency = settings.parseCurrency(value);

            header.setType(DatatoolSettings.TYPE_CURRENCY);

            return currency;
         }
         catch (NumberFormatException e)
         {
            // Not currency.
         }

         header.setType(DatatoolSettings.TYPE_STRING);
      }
      else if (type == DatatoolSettings.TYPE_REAL)
      {
         if (value.isEmpty())
         {
            return new Float(0.0f);
         }

         try
         {
            return new Float(value);
         }
         catch (NumberFormatException fe)
         {
            // Not a float.
         }

         // Is it currency?

         try
         {
            Currency currency = settings.parseCurrency(value);

            header.setType(DatatoolSettings.TYPE_CURRENCY);

            return currency;
         }
         catch (NumberFormatException e)
         {
            // Not currency.
         }

         // Set to String.

         header.setType(DatatoolSettings.TYPE_STRING);
      }
      else if (type == DatatoolSettings.TYPE_CURRENCY)
      {
         if (value.isEmpty())
         {
            return new Currency(null, 0.0f);
         }

         try
         {
            Currency currency = settings.parseCurrency(value);

            header.setType(DatatoolSettings.TYPE_CURRENCY);

            return currency;
         }
         catch (NumberFormatException e)
         {
            // Not currency.
         }

         // Set to String.

         header.setType(DatatoolSettings.TYPE_STRING);
      }

      return value;
   }

   public DatatoolRow removeRow(int rowIdx)
   {
      return data.remove(rowIdx);
   }

   public void truncate(int newSize)
   {
      if (newSize < data.size())
      {
         data.setSize(newSize);
      }
   }

   public void removeMatching(DataFilter dataFilter)
   {
      for (int i = data.size()-1; i >= 0; i--)
      {
         if (dataFilter.matches(data.get(i)))
         {
            data.remove(i);
         }
      }
   }

   public void removeNonMatching(DataFilter dataFilter)
   {
      for (int i = data.size()-1; i >= 0; i--)
      {
         if (!dataFilter.matches(data.get(i)))
         {
            data.remove(i);
         }
      }
   }

   public DatatoolColumn removeColumn(int colIdx)
   {
      DatatoolHeader header = headers.remove(colIdx);

      if (header == null)
      {
         return null;
      }

      return new DatatoolColumn(header, colIdx, data, true);
   }

   public void removeColumn(DatatoolColumn column)
   {
      int colIdx = column.getColumnIndex();

      headers.remove(colIdx);

      for (DatatoolRow row : data)
      {
         row.remove(colIdx);
      }
   }

   public DatatoolRow insertRow(int rowIdx)
   {
      DatatoolRow row = new DatatoolRow(this, headers.size());

      for (int i = 0; i < headers.size(); i++)
      {
         row.add(new String());
      }

      insertRow(rowIdx, row);

      return row;
   }

   public void insertRow(int rowIdx, DatatoolRow row)
   {
      row.setDatabase(this);

      int numCols = headers.size();

      if (row.size() < numCols)
      {
         // If new row is shorter than current number of columns,
         // pad out the row

         for (int i = row.size(); i < numCols; i++)
         {
            row.add(new String());
         }
      }
      else if (row.size() > numCols)
      {
         // if new row is longer than current number of columns, add
         // more columns

         for (int i = numCols; i < row.size(); i++)
         {
            insertColumn(i);
         }

         numCols = headers.size();
      }

      int numRows = data.size();

      if (rowIdx == numRows)
      {
         data.add(row);
      }
      else if (rowIdx > numRows)
      {
         for (int i = numRows; i < rowIdx; i++)
         {
            data.add(new DatatoolRow(this, headers.size()));
         }

         data.add(row);
      }
      else
      {
         data.add(rowIdx, row);
      }

      for (int colIdx = 0, n = headers.size(); colIdx < n; colIdx++)
      {
         int type = getDataType(colIdx, row.get(colIdx));

         if (type != DatatoolSettings.TYPE_UNKNOWN)
         {
            headers.get(colIdx).setType(type);
         }
      }
   }

   public void replaceRow(int index, DatatoolRow newRow)
   {
      DatatoolRow oldRow = data.set(index, newRow);

      int n = headers.size();

      if (newRow.size() < n)
      {
         // if new row shorter than old row, pad it with values from old row

         for (int i = newRow.size(); i < n; i++)
         {
            newRow.add(oldRow.get(i));
         }
      }
      else if (newRow.size() > n)
      {
         // if new row is longer than old row, add extra columns

         for (int i = n; i < newRow.size(); i++)
         {
            insertColumn(i);
         }

         n = headers.size();
      }

      for (int colIdx = 0; colIdx < n; colIdx++)
      {
         int type = getDataType(colIdx, newRow.get(colIdx));

         if (type != DatatoolSettings.TYPE_UNKNOWN)
         {
            headers.get(colIdx).setType(type);
         }
      }
   }

   public void insertColumn(DatatoolColumn column)
   {
      column.insertIntoData(headers, data);
   }

   public DatatoolHeader insertColumn(int colIdx)
   {
      String defName = getMessageHandler().getLabelWithValues(
         "default.field", (colIdx+1));
      return insertColumn(colIdx, new DatatoolHeader(this, defName, defName));
   }

   public DatatoolHeader insertColumn(int colIdx, DatatoolHeader header)
   {
      int n = headers.size();

      if (colIdx == n)
      {
         addColumn(header);
      }
      else if (colIdx > n)
      {
         for (int i = n; i < colIdx; i++)
         {
            headers.add(new DatatoolHeader(this));

            for (DatatoolRow row : data)
            {
               row.add(new String());
            }
         }

         addColumn(header);
      }
      else
      {
         headers.add(colIdx, header);

         for (DatatoolRow row : data)
         {
            row.add(colIdx, new String());
         }
      }

      return header;
   }

   public void addColumn(DatatoolHeader header)
   {
      headers.add(header);

      for (DatatoolRow row : data)
      {
         row.add(new String());
      }
   }

   public void moveRow(int fromIndex, int toIndex)
   {
      if (fromIndex == toIndex) return;

      DatatoolRow row = data.remove(fromIndex);

      data.add(toIndex, row);
   }

   public void moveColumn(int fromIndex, int toIndex)
   {
      if (fromIndex == toIndex) return;

      DatatoolHeader header = headers.remove(fromIndex);
      headers.add(toIndex, header);

      for (DatatoolRow row : data)
      {
         String value = row.remove(fromIndex);
         row.add(toIndex, value);
      }
   }

   public ColumnEnumeration getColumnEnumeration(int colIdx)
   {
      return new ColumnEnumeration(data, colIdx);
   }

   public Currency parseCurrency(String text)
     throws NumberFormatException
   {
      return settings.parseCurrency(text);
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public int getSortColumn()
   {
      return sortColumn;
   }

   public void setSortColumn(int columnIndex)
   {
      sortColumn = columnIndex;
   }

   public boolean isSortAscending()
   {
      return sortAscending;
   }

   public void setSortAscending(boolean isAscending)
   {
      sortAscending = isAscending;
   }

   public boolean isSortCaseSensitive()
   {
      return sortCaseSensitive;
   }

   public void setSortCaseSensitive(boolean isSensitive)
   {
      sortCaseSensitive = isSensitive;
   }

   public void setSortLocale(Locale locale)
   {
      settings.setSortLocale(locale);
   }

   public Locale getSortLocale()
   {
      String prop = settings.getSortLocale();

      if (prop == null)
      {
         return null;
      }
      else
      {
         return Locale.forLanguageTag(prop);
      }
   }

   public void sort()
   {
      Collections.sort(data);
   }

   public void shuffle()
   {
      shuffle(settings.getRandom());
   }

   public void shuffle(Random random)
   {
      if (settings.isCompatibilityLevel(settings.COMPAT_1_6))
      {
         shuffle16(random);
      }
      else
      {
         Collections.shuffle(data, random);
      }
   }

   public void shuffle16(Random random)
   {
      int numRows = data.size();

      int n = settings.getShuffleIterations();

      for (int i = 0; i < n; i++)
      {
         int index1 = random.nextInt(numRows);
         int index2 = random.nextInt(numRows);

         if (index1 != index2)
         {
            DatatoolRow row1 = data.get(index1);
            DatatoolRow row2 = data.get(index2);

            data.set(index1, row2);
            data.set(index2, row1);
         }
      }
   }

   public Vector<DatatoolHeader> getHeaders()
   {
      return headers;
   }

   public DatatoolRow[] dataToArray()
   {
      int n = data.size();
      DatatoolRow[] array = new DatatoolRow[n];

      for (int i = 0; i < n; i++)
      {
         array[i] = data.get(i);
      }

      return array;
   }

   public void dataFromArray(DatatoolRow[] array)
   {
      for (int i = 0; i < array.length; i++)
      {
         data.set(i, array[i]);
      }
   }

   public static DatatoolDb createFromTemplate(
    DatatoolSettings settings, Template templateFile)
    throws SAXException,IOException
   {
      XMLReader xr = XMLReaderFactory.createXMLReader();

      FileReader reader = null;
      DatatoolDb db = null;

      try
      {
         reader = new FileReader(templateFile.getFile());

         db = new DatatoolDb(settings);

         String theName = templateFile.toString();

         db.setName(settings.getMessageHandler().getLabelWithAlt(
            String.format("plugin.%s.default_name", theName), theName));

         TemplateHandler handler = new TemplateHandler(db, 
            templateFile.toString());
         xr.setContentHandler(handler);
         xr.setErrorHandler(settings.getMessageHandler());

         xr.parse(new InputSource(reader));

      }
      finally
      {
         if (reader != null)
         {
            reader.close();
         }
      }

      return db;
   }

   public void merge(DatatoolDb db, String key)
    throws InvalidSyntaxException
   {
      int colIdx1 = getColumnIndex(key);

      if (colIdx1 == -1)
      {
         throw new InvalidSyntaxException(
           getMessageHandler().getLabelWithValues("error.db.unknown_key",
             key, getName()));
      }

      int colIdx2 = db.getColumnIndex(key);

      if (colIdx2 == -1)
      {
         throw new InvalidSyntaxException(
           getMessageHandler().getLabelWithValues("error.db.unknown_key", 
             key, db.getName()));
      }

      merge(db, colIdx1, colIdx2);
   }

   public void merge(DatatoolDb db, int colIdx, int dbColIdx)
   {
      for (DatatoolHeader header: db.headers)
      {
         if (getHeader(header.getKey()) == null)
         {
            addColumn(header);
         }
      }

      for (DatatoolRow dbRow : db.data)
      {
         String dbValue = dbRow.get(dbColIdx);

         DatatoolRow thisRow = null;

         for (DatatoolRow row : data)
         {
            String value = row.get(colIdx);

            if (value.equals(dbValue))
            {
               thisRow = row;
               break;
            }
         }

         if (thisRow == null)
         {
            int n = headers.size();

            thisRow = new DatatoolRow(this, n);

            for (int i = 0; i < n; i++)
            {
               thisRow.add(new String());
            }

            data.add(thisRow);
         }

         for (int i = 0, n = db.headers.size(); i < n; i++)
         {
            DatatoolHeader header = db.headers.get(i);

            int idx = getColumnIndex(header.getKey());

            thisRow.setCell(idx, dbRow.get(i));
         }
      }
   }

   private DatatoolSettings settings;

   private Vector<DatatoolHeader> headers;

   private Vector<DatatoolRow> data;

   private File file;

   private String name;

   private int linenum;

   private int sortColumn = 0;

   private boolean sortAscending = true;

   private boolean sortCaseSensitive = false;

   public static final String NULL_VALUE="\\@dtlnovalue";

   private static final Pattern PATTERN_END_DBSLASH 
    = Pattern.compile(".*[^\\\\](\\\\\\\\)+");

   private static final Pattern[] PATTERN_VERBATIM =
    new Pattern[]
    { 
       Pattern.compile(".*\\\\begin\\s*\\{verbatim\\}.*", Pattern.DOTALL),
       Pattern.compile(".*\\\\verb\\b.*", Pattern.DOTALL),
       Pattern.compile(".*\\\\begin\\s*\\{lstlisting\\}.*", Pattern.DOTALL),
       Pattern.compile(".*\\\\lstinline\\b.*", Pattern.DOTALL),
       Pattern.compile(".*\\\\begin\\s*\\{alltt\\}.*", Pattern.DOTALL)
    };
}
