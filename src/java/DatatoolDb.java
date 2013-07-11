package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.Vector;
import java.util.Random;
import java.util.Enumeration;
import java.util.Collections;
import java.util.regex.*;
import java.util.Date;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.dickimawbooks.datatooltk.io.*;

public class DatatoolDb
{
   public DatatoolDb(DatatoolSettings settings)
   {
      this.settings = settings;
      headers = new Vector<DatatoolHeader>();
      data = new Vector<DatatoolRow>();
   }

   public DatatoolDb(DatatoolSettings settings, int rows, int cols)
   {
      this.settings = settings;
      headers = new Vector<DatatoolHeader>(cols);
      data = new Vector<DatatoolRow>(rows);
   }

   public DatatoolDb(DatatoolSettings settings, int cols)
   {
      this.settings = settings;
      headers = new Vector<DatatoolHeader>(cols);
      data = new Vector<DatatoolRow>();
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
      LineNumberReader in = null;
      DatatoolDb db = null;
      boolean hasVerbatim = false;

      try
      {
         in = new LineNumberReader(new FileReader(dbFile));

         db = new DatatoolDb(settings);

         // Read until we find \newtoks\csname dtlkeys@<name>\endcsname

         String controlSequence = null;

         while ((controlSequence = readCommand(in)) != null)
         {
            if (controlSequence.equals("\\newtoks"))
            {
               controlSequence = readCommand(in);

               if ("\\csname".equals(controlSequence))
               {
                  break;
               }
            }
         }

         if (controlSequence == null)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", "\\newtoks\\csname"));
         }

         String name = readUntil(in, "\\endcsname");

         if (name == null)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", "\\endcsname"));
         }

         if (!name.startsWith("dtlkeys@"))
         {
            throw new IOException(DatatoolTk.getLabelWithValues
              (
                 "error.dbload.expected",
                 in.getLineNumber(),
                 "\\newtoks\\csname dtlkeys@<name>\\endcsname"
              ));
         }

         name = name.substring(8);

         db.setName(name);

         // Now look for \csname dtlkeys@<name>\endcsname

         controlSequence = null;

         while ((controlSequence = readCommand(in)) != null)
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
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
                "\\csname dtlkeys@"+name+"\\endcsname"));
         }

         int c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
                "\\csname dtlkeys@"+name+"\\endcsname="));
         }
         else if (c != (int)'=')
         {
            throw new IOException(DatatoolTk.getLabelWithValues(
              "error.dbload.expected_found", 
               new String[]
               {
                  ""+in.getLineNumber(),
                  "\\csname dtlkeys@"+name+"\\endcsname=",
                  "\\csname dtlkeys@"+name+"\\endcsname"+((char)c)
               }));
         }

         c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
                "\\csname dtlkeys@"+name+"\\endcsname={"));
         }
         else if (c != (int)'{')
         {
            throw new IOException(DatatoolTk.getLabelWithValues(
              "error.dbload.expected_found", 
               new String[]
               {
                  ""+in.getLineNumber(),
                  "\\csname dtlkeys@"+name+"\\endcsname={",
                  "\\csname dtlkeys@"+name+"\\endcsname"+((char)c)
               }));
         }

         int currentColumn = 0;

         while (true)
         {
            readCommand(in, "\\db@plist@elt@w");

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
               throw new IOException(DatatoolTk.getLabelWithValues
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

         while ((controlSequence = readCommand(in)) != null)
         {
            if (controlSequence.equals("\\newtoks"))
            {
               controlSequence = readCommand(in);

               if ("\\csname".equals(controlSequence))
               {
                  break;
               }
            }
         }

         if (controlSequence == null)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", "\\newtoks\\csname"));
         }

         String contents = readUntil(in, "\\endcsname");

         if (contents == null)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
             "\\newtoks\\csname dtldb@"+name+"\\endcsname"));
         }
         else if (!contents.equals("dtldb@"+name))
         {
            throw new IOException(DatatoolTk.getLabelWithValues(
              "error.dbload.expected_found",
                new String[]
                {
                  ""+in.getLineNumber(),
                  "\\newtoks\\csname dtldb@"+name+"\\endcsname",
                  "\\newtoks\\csname "+contents+"\\endcsname"
                }
              ));
         }

         contents = readUntil(in, "\\csname");

         if (contents == null)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
             "\\csname dtldb@"+name+"\\endcsname="));
         }

         // skip any whitespace

         c = readChar(in, true);

         contents = readUntil(in, "\\endcsname");

         if (contents == null)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
             "\\csname dtldb@"+name+"\\endcsname="));
         }

         contents = (""+(char)c)+contents;

         if (!contents.equals("dtldb@"+name))
         {
            throw new IOException(DatatoolTk.getLabelWithValues(
              "error.dbload.expected_found",
                new String[]
                {
                  ""+in.getLineNumber(),
                  "\\csname dtldb@"+name+"\\endcsname",
                  "\\csname "+contents+"\\endcsname"
                }
              ));
         }

         // Look for ={ assignment

         c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
                "\\csname dtldb@"+name+"\\endcsname="));
         }
         else if (c != (int)'=')
         {
            throw new IOException(DatatoolTk.getLabelWithValues(
              "error.dbload.expected_found", 
               new String[]
               {
                  ""+in.getLineNumber(),
                  "\\csname dtldb@"+name+"\\endcsname=",
                  "\\csname dtldb@"+name+"\\endcsname"+((char)c)
               }));
         }

         c = readChar(in, true);

         if (c == -1)
         {
            throw new IOException(DatatoolTk.getLabelWithValue(
              "error.dbload.not_found", 
                "\\csname dtldb@"+name+"\\endcsname={"));
         }
         else if (c != (int)'{')
         {
            throw new IOException(DatatoolTk.getLabelWithValues(
              "error.dbload.expected_found", 
               new String[]
               {
                  ""+in.getLineNumber(),
                  "\\csname dtldb@"+name+"\\endcsname={",
                  "\\csname dtldb@"+name+"\\endcsname"+((char)c)
               }));
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
               throw new IOException(DatatoolTk.getLabelWithValues
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
         DatatoolTk.warning(DatatoolTk.getLabel("warning.verb_detected"));
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
         throw new IOException(DatatoolTk.getLabelWithValues
           (
              "error.dbload.invalid_row_id",
              in.getLineNumber(),
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
         throw new IOException(DatatoolTk.getLabelWithValues
           (
              "error.dbload.expected",
              in.getLineNumber(),
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
               throw new IOException(DatatoolTk.getLabelWithValues
                 (
                    "error.dbload.wrong_end_row_tag",
                    new String[]
                    {
                       ""+in.getLineNumber(),
                       ""+currentRow,
                       contents
                    }
                 ));
            }
         }
         catch (NumberFormatException e)
         {
            throw new IOException(DatatoolTk.getLabelWithValues
              (
                 "error.dbload.invalid_row_id",
                 in.getLineNumber(),
                 contents
              ), e);
         }

         readCommand(in, "\\db@row@elt@end@");

         return;
      }

      if (!controlSequence.equals("\\db@col@id@w"))
      {
         throw new IOException(DatatoolTk.getLabelWithValues
           (
              "error.dbload.expected_found",
              new String[]
              {
                 ""+in.getLineNumber(),
                 "\\db@col@id@w",
                 controlSequence
              }
           ));
      }

      String contents = readUntil(in, "\\db@col@id@end@");

      if (contents == null)
      {
         throw new IOException(DatatoolTk.getLabelWithValues
           (
              "error.dbload.expected",
                 in.getLineNumber(),
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
         throw new IOException(DatatoolTk.getLabelWithValues
           (
              "error.dbload.invalid_col_id",
              in.getLineNumber(),
              contents
           ), e);
      }

      readCommand(in, "\\db@col@elt@w");

      contents = readUntil(in, "\\db@col@elt@end@", false);

      if (contents == null)
      {
         throw new IOException(DatatoolTk.getLabelWithValues
           (
              "error.dbload.expected",
                 in.getLineNumber(),
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
            throw new IOException(DatatoolTk.getLabelWithValues
              (
                 "error.dbload.wrong_end_col_tag",
                 new String[]
                 {
                    ""+in.getLineNumber(),
                    ""+currentColumn,
                    contents
                 }
              ));
         }
      }
      catch (NumberFormatException e)
      {
         throw new IOException(DatatoolTk.getLabelWithValues
           (
              "error.dbload.invalid_col_id",
              in.getLineNumber(),
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
         throw new IOException(DatatoolTk.getLabelWithValue(
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
            throw new IOException(DatatoolTk.getLabelWithValue(
               "error.dbload.not_found", "\\db@col@id@end@"));
         }

         try
         {
            currentColumn = Integer.parseInt(content);
         }
         catch (NumberFormatException e)
         {
             throw new IOException(DatatoolTk.getLabelWithValues
             (
                "error.dbload.invalid_col_id",
                in.getLineNumber(),
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
            throw new IOException(DatatoolTk.getLabelWithValue(
               "error.dbload.not_found", "\\db@key@id@end@"));
         }

         // Get the header for the current column and set this key

         if (headers.size() < currentColumn)
         {
            throw new IOException(DatatoolTk.getLabelWithValues
             (
                "error.db.load.expected_found",
                new String[]
                {
                   ""+in.getLineNumber(),
                   "\\db@col@id@w",
                   "\\db@key@id@w"
                }
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
            throw new IOException(DatatoolTk.getLabelWithValue(
               "error.dbload.not_found", "\\db@header@id@end@"));
         }

         // Get the header for the current column and set this title

         if (headers.size() < currentColumn)
         {
            throw new IOException(DatatoolTk.getLabelWithValues
             (
                "error.db.load.expected_found",
                new String[]
                {
                   ""+in.getLineNumber(),
                   "\\db@col@id@w",
                   "\\db@header@id@w"
                }
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
            throw new IOException(DatatoolTk.getLabelWithValue(
               "error.dbload.not_found", "\\db@type@id@end@"));
         }

         int type = TYPE_UNKNOWN;

         try
         {
            if (!content.isEmpty())
            {
               type = Integer.parseInt(content);
            }
 
            // Get the header for the current column and set this title

            if (headers.size() < currentColumn)
            {
               throw new IOException(DatatoolTk.getLabelWithValues
                (
                   "error.db.load.expected_found",
                   new String[]
                   {
                      ""+in.getLineNumber(),
                      "\\db@col@id@w",
                   "   \\db@header@id@w"
                   }
                ));
            }

            DatatoolHeader header = headers.get(currentColumn-1);

            header.setType(type);
         }
         catch (NumberFormatException e)
         {
             throw new IOException(DatatoolTk.getLabelWithValues
             (
                "error.dbload_unknown_type",
                in.getLineNumber(),
                content
             ), e);
         }
         catch (IllegalArgumentException e)
         {
             throw new IOException(DatatoolTk.getLabelWithValues
             (
                "error.dbload_unknown_type",
                in.getLineNumber(),
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
   private static void readCommand(LineNumberReader in, String requiredCommand)
     throws IOException
   {
      String controlSequence = readCommand(in);

      if (controlSequence == null)
      {
         throw new IOException(DatatoolTk.getLabelWithValue(
           "error.dbload.not_found", 
             requiredCommand));
      }
      else if (!requiredCommand.equals(controlSequence))
      {
         throw new IOException(DatatoolTk.getLabelWithValues(
           "error.dbload.expected_found", 
            new String[]
            {
               ""+in.getLineNumber(),
               requiredCommand,
               controlSequence
            }));
      }
   }

   private static String readCommand(BufferedReader in)
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

      try
      {
         out = new PrintWriter(file);

         name = getName();

         out.println("% "+DatatoolTk.getLabelWithValues("default.texheader",
           DatatoolTk.appName, (new Date()).toString()));
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
               +(type==TYPE_UNKNOWN?"":type)+"%");
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
      }
   }

   public void setFile(File file)
   {
      this.file = file;
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
        DatatoolTk.getLabel("default.untitled") : file.getName()): name;
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
         return TYPE_UNKNOWN;
      }

      try
      {
         Integer.parseInt(value);

         return TYPE_INTEGER;
      }
      catch (NumberFormatException e)
      {
      }

      try
      {
         Float.parseFloat(value);

         return TYPE_REAL;
      }
      catch (NumberFormatException e)
      {
      }

      try
      {
         settings.parseCurrency(value);

         return TYPE_CURRENCY;
      }
      catch (NumberFormatException e)
      {
      }

      return TYPE_STRING;
   }

   public int getDataType(int colIdx, String value)
   {
      DatatoolHeader header = headers.get(colIdx);

      // What's the data type of this value?

      int type = getType(value);

      // If it's unknown, return

      if (type == TYPE_UNKNOWN)
      {
         return type;
      }

      switch (header.getType())
      {
         case TYPE_UNKNOWN:
         case TYPE_INTEGER:
            // All other types override unknown and int
            return type;
         case TYPE_CURRENCY:
            // string overrides currency

            if (type == TYPE_STRING)
            {
               return type;
            }
         break;
         case TYPE_REAL:
            // string and currency override real
            if (type == TYPE_STRING || type == TYPE_CURRENCY)
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

      if (type != TYPE_UNKNOWN)
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

      if (type == TYPE_INTEGER)
      {
         if (value.isEmpty())
         {
            return new Integer(0);
         }

         try
         {
            return new Integer(value);
         }
         catch (NumberFormatException e)
         {
            // Not an integer
         }

         // Is it a float?

         try
         {
            Float num = new Float(value);

            header.setType(TYPE_REAL);

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

            header.setType(TYPE_CURRENCY);

            return currency;
         }
         catch (NumberFormatException e)
         {
            // Not currency.
         }

         header.setType(TYPE_STRING);
      }
      else if (type == TYPE_REAL)
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

            header.setType(TYPE_CURRENCY);

            return currency;
         }
         catch (NumberFormatException e)
         {
            // Not currency.
         }

         // Set to String.

         header.setType(TYPE_STRING);
      }
      else if (type == TYPE_CURRENCY)
      {
         if (value.isEmpty())
         {
            return new Currency(null, 0.0f);
         }

         try
         {
            Currency currency = settings.parseCurrency(value);

            header.setType(TYPE_CURRENCY);

            return currency;
         }
         catch (NumberFormatException e)
         {
            // Not currency.
         }

         // Set to String.

         header.setType(TYPE_STRING);
      }

      return value;
   }

   public DatatoolRow removeRow(int rowIdx)
   {
      return data.remove(rowIdx);
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

         if (type != TYPE_UNKNOWN)
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

         if (type != TYPE_UNKNOWN)
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
      String defName = DatatoolTk.getLabelWithValue(
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

         db.setName(DatatoolTk.getLabelWithAlt("plugin."+theName+".default_name",
           theName));

         TemplateHandler handler = new TemplateHandler(db, templateFile.toString());
         xr.setContentHandler(handler);
         xr.setErrorHandler(settings.getErrorHandler());

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

   public ErrorHandler getErrorHandler()
   {
      return settings.getErrorHandler();
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

   public static final int TYPE_UNKNOWN=-1, TYPE_STRING = 0, TYPE_INTEGER=1,
     TYPE_REAL=2, TYPE_CURRENCY=3;

   public static final String[] TYPE_LABELS = new String[] 
         {
            DatatoolTk.getLabel("header.type.unset"),
            DatatoolTk.getLabel("header.type.string"),
            DatatoolTk.getLabel("header.type.int"),
            DatatoolTk.getLabel("header.type.real"),
            DatatoolTk.getLabel("header.type.currency")
         };
   public static final int[] TYPE_MNEMONICS = new int[] 
         {
            DatatoolTk.getMnemonicInt("header.type.unset"),
            DatatoolTk.getMnemonicInt("header.type.string"),
            DatatoolTk.getMnemonicInt("header.type.int"),
            DatatoolTk.getMnemonicInt("header.type.real"),
            DatatoolTk.getMnemonicInt("header.type.currency")
         };


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
