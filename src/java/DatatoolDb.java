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
      BufferedReader in = null;
      DatatoolDb db = null;
      boolean hasVerbatim = false;

      try
      {
         in = new BufferedReader(new FileReader(dbFile));

         db = new DatatoolDb(settings);

         db.linenum = 0;
         String line;

         // Skip any comment lines at the start of the file

         while ((line = in.readLine()) != null)
         {
            db.linenum++;
            Matcher m = PATTERN_COMMENT.matcher(line);

            if (m.matches())
            {
               continue;
            }

            m = PATTERN_DBNAME.matcher(line);

            if (m.matches())
            {
               db.setName(m.group(1));
               break;
            }
            else
            {
               throw new InvalidSyntaxException(
                 DatatoolTk.getLabelWithValues("error.dbload.expected",
                  ""+db.linenum, "\\DTLifdbexists"));
            }
         }

         if (line == null)
         {
            throw new EOFException(
               DatatoolTk.getLabelWithValue("error.dbload.not_found",
                 "\\DTLifdbexists"));
         }

         // skip until we reach "\\csname dtlkeys@<name>\endcsname={"

         Pattern p = Pattern.compile("\\s*\\\\csname\\s+dtlkeys@"
           + db.name+"\\\\endcsname\\s*=\\s*\\{%.*");

         while ((line = in.readLine()) != null)
         {
            db.linenum++;
            Matcher m = PATTERN_COMMENT.matcher(line);

            if (m.matches())
            {
               continue;
            }

            m = p.matcher(line);

            if (m.matches())
            {
               break;
            }
         }

         if (line == null)
         {
            throw new EOFException(
               DatatoolTk.getLabelWithValue("error.dbload.not_found",
                 p.pattern()));
         }

         // Now read the header info

         while ((line = in.readLine()) != null)
         {
            // Ignore commented lines

            db.linenum++;
            Matcher m = PATTERN_COMMENT.matcher(line);

            if (m.matches())
            {
               continue;
            }


            m = PATTERN_CLOSE.matcher(line);

            if (m.matches())
            {
               break;
            }

            m = PATTERN_PLIST_ELT.matcher(line);

            if (!m.matches())
            {
               throw new InvalidSyntaxException(
                 DatatoolTk.getLabelWithValues("error.dbload.expected",
                 ""+db.linenum, "\\db@plist@elt@w"));
            }

            hasVerbatim = db.parseHeader(in, !hasVerbatim) || hasVerbatim;

            while ((line = in.readLine()) != null)
            {
               // Ignore commented lines

               db.linenum++;
               m = PATTERN_COMMENT.matcher(line);

               if (m.matches())
               {
                  continue;
               }

               m = PATTERN_PLIST_ELT_END.matcher(line);

               if (!m.matches())
               {
                  throw new InvalidSyntaxException(
                    DatatoolTk.getLabelWithValues("error.dbload.expected",
                      ""+db.linenum, "\\db@plist@elt@end@"));
               }

               break;
            }
         }

         // skip until we reach "\\csname dtldb@<name>\endcsname={"

         p = Pattern.compile("\\s*\\\\csname\\s+dtldb@"
           + db.name+"\\\\endcsname\\s*=\\s*\\{%.*");

         while ((line = in.readLine()) != null)
         {
            db.linenum++;
            Matcher m = PATTERN_COMMENT.matcher(line);

            if (m.matches())
            {
               continue;
            }

            m = p.matcher(line);

            if (m.matches())
            {
               break;
            }
         }

         if (line == null)
         {
            throw new EOFException(
             DatatoolTk.getLabelWithValue("error.dbload.not_found",
               p.pattern()));
         }

         while ((line = in.readLine()) != null)
         {
            db.linenum++;

            // skip comments outside of values

            Matcher m = PATTERN_COMMENT.matcher(line);

            if (m.matches())
            {
               continue;
            }

            boolean done = false;

            // Read in each row

            while (line != null)
            {
               // Finish if we've reached the closing brace

               m = PATTERN_CLOSE.matcher(line);

               if (m.matches())
               {
                  done = true;
                  break;
               }

               m = PATTERN_ROW_ELT.matcher(line);

               if (!m.matches())
               {
                   throw new InvalidSyntaxException(
                    DatatoolTk.getLabelWithValues(
                    "error.dbload.expected",
                     ""+db.linenum, PATTERN_ROW_ELT.pattern()));
               }

               line = in.readLine();

               if (line == null)
               {
                  break;
               }

               db.linenum++;

               m = PATTERN_ROW_ID.matcher(line);

               if (!m.matches())
               {
                   throw new InvalidSyntaxException(
                    DatatoolTk.getLabelWithValues(
                    "error.dbload.expected",
                     ""+db.linenum, PATTERN_ROW_ID.pattern()));
               }

               int rowIdx = -1;

               try
               {
                  rowIdx = Integer.parseInt(m.group(1));
               }
               catch (NumberFormatException e)
               {
                  // shouldn't happen
               }

               line = in.readLine();

               if (line == null)
               {
                  break;
               }

               db.linenum++;

               m = PATTERN_ROW_ID_END.matcher(line);

               if (!m.matches())
               {
                   throw new InvalidSyntaxException(
                    DatatoolTk.getLabelWithValues(
                    "error.dbload.expected",
                     ""+db.linenum, PATTERN_ROW_ID_END.pattern()));
               }

               // Now read in columns

               while ((line = in.readLine()) != null)
               {
                  db.linenum++;

                  // Finish if we've reached the closing brace

                  m = PATTERN_CLOSE.matcher(line);

                  if (m.matches())
                  {
                     done = true;
                     break;
                  }

                  // Have we reached the end of the current row?

                  m = PATTERN_ROW_ID.matcher(line);

                  if (m.matches())
                  {
                      try
                      {
                         int idx = Integer.parseInt(m.group(1));

                         if (idx != rowIdx)
                         {
                            throw new InvalidSyntaxException(
                               DatatoolTk.getLabelWithValues(
                                 "error.dbload.wrong_end_row_tag",
                                 new String[]{""+db.linenum, ""+rowIdx, ""+idx}
                               ));
                         }
                      }
                      catch (NumberFormatException e)
                      {
                         // shouldn't happen
                      }

                      line = in.readLine();

                      if (line == null)
                      {
                         throw new EOFException(
                            DatatoolTk.getLabelWithValue(
                            "error.dbload.missing_end_row_tag", rowIdx));
                      }

                      db.linenum++;

                      m = PATTERN_ROW_ID_END.matcher(line);

                      if (!m.matches())
                      {
                         throw new InvalidSyntaxException(
                            DatatoolTk.getLabelWithValues(
                            "error.dbload.missing_end_row_tag_pat",
                            new String[]
                            {
                              ""+db.linenum,
                              PATTERN_ROW_ID_END.pattern(),
                              ""+rowIdx
                            }));
                      }

                      line = in.readLine();

                      if (line == null)
                      {
                         throw new EOFException(
                           DatatoolTk.getLabelWithValue(
                             "error.dbload.missing_end_row_tag", rowIdx));
                      }

                      db.linenum++;

                      m = PATTERN_ROW_ELT_END.matcher(line);

                      if (!m.matches())
                      {
                         throw new InvalidSyntaxException(
                            DatatoolTk.getLabelWithValues(
                            "error.dbload.missing_end_row_tag_pat",
                            new String[]
                            {
                              ""+db.linenum,
                              PATTERN_ROW_ELT_END.pattern(),
                              ""+rowIdx
                            }));
                      }

                      break;
                  }

                  // read in column data for current row

                  m = PATTERN_COL_ID.matcher(line);

                  int colIdx = -1;

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                        DatatoolTk.getLabelWithValues(
                          "error.dbload.missing_col_tag",
                          ""+db.linenum, PATTERN_COL_ID.pattern()));
                  }

                  try
                  {
                     colIdx = Integer.parseInt(m.group(1));
                  }
                  catch (NumberFormatException e)
                  {
                     // shouldn't happen
                  }

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException(
                        DatatoolTk.getLabelWithValue(
                          "error.dbload.col_tag_eof", colIdx));
                  }

                  db.linenum++;

                  m = PATTERN_COL_ID_END.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                        DatatoolTk.getLabelWithValues(
                           "error.dbload.missing_col_tag",
                           ""+db.linenum, PATTERN_COL_ID_END.pattern()));
                  }

                  // Read cell data

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException(
                        DatatoolTk.getLabelWithValue(
                          "error.dbload.col_data_eof", colIdx));
                  }

                  db.linenum++;

                  m = PATTERN_COL_ELT.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                        DatatoolTk.getLabelWithValues(
                           "error.dbload.missing_col_tag",
                            ""+db.linenum, PATTERN_COL_ELT.pattern()));
                  }

                  String value = m.group(1);

                  while ((line = in.readLine()) != null)
                  {
                     db.linenum++;

                     m = PATTERN_COL_ELT_END.matcher(line);

                     if (m.matches())
                     {
                        break;
                     }

                     value += System.getProperty("line.separator", "\n") 
                            + line;
                  }

                  if (line == null)
                  {
                     throw new EOFException(
                        DatatoolTk.getLabelWithValue(
                           "error.dbload.col_data_eof", colIdx));
                  }

                  // check for end column tag

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException(
                        DatatoolTk.getLabelWithValue(
                           "error.dbload.col_end_tag_eof", colIdx));
                  }

                  db.linenum++;

                  m = PATTERN_COL_ID.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                       DatatoolTk.getLabelWithValues(
                         "error.dbload.missing_end_col", 
                         ""+db.linenum, ""+colIdx));
                  }

                  try
                  {
                     int idx = Integer.parseInt(m.group(1));

                     if (idx != colIdx)
                     {
                         throw new InvalidSyntaxException(
                           DatatoolTk.getLabelWithValues(
                             "error.dbload.wrong_end_col_tag",
                             new String[]{""+db.linenum, ""+colIdx, ""+idx}));
                     }
                  }
                  catch (NumberFormatException e)
                  {
                     // shouldn't happen
                  }

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException(
                        DatatoolTk.getLabelWithValue(
                           "error.dbload.col_end_tag_eof", colIdx));
                  }

                  db.linenum++;

                  m = PATTERN_COL_ID_END.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                        DatatoolTk.getLabelWithValues(
                          "error.dbload.missing_end_col_tag",
                          ""+db.linenum, ""+colIdx));
                  }

                  if (value.endsWith("%"))
                  {
                     value = value.substring(0, value.length()-1);
                  }

                  if (!hasVerbatim)
                  {
                     hasVerbatim = checkForVerbatim(value);
                  }

                  db.addCell(rowIdx-1, colIdx-1, value);

               }

               if (done) break;

               line = in.readLine();
               db.linenum++;
            }

            if (done) break;
         }

         if (line == null)
         {
            throw new EOFException(
               DatatoolTk.getLabelWithValue(
                 "error.dbload.missing_end_brace", p.pattern()));
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

   public static boolean checkForVerbatim(String value)
   {
      for (int i = 0; i < PATTERN_VERBATIM.length; i++)
      {
         Matcher m = PATTERN_VERBATIM[i].matcher(value);

         if (m.matches()) return true;
      }

      return false;
   }

   public boolean parseHeader(BufferedReader in, boolean checkForVerbatim)
    throws IOException
   {
      Integer colIdx = (Integer)parseGroup(in, GROUP_COL);
      String key     = (String)parseGroup(in, GROUP_KEY);
      Integer type   = (Integer)parseGroup(in, GROUP_TYPE);
      String title   = (String)parseGroup(in, GROUP_TITLE);

      Integer idx    = (Integer)parseGroup(in, GROUP_COL);

      if (!idx.equals(colIdx))
      {
         throw new InvalidSyntaxException(DatatoolTk.getLabelWithValues(
            "error.dbload.wrong_end_col_tag",
            new String[] {""+linenum, colIdx.toString(), idx.toString()}));
      }

      insertColumn(colIdx.intValue()-1, 
         new DatatoolHeader(key, title, type.intValue()));

      return checkForVerbatim ? checkForVerbatim(title): false;
   }

   private Object parseGroup(BufferedReader in, int groupType)
    throws IOException
   {
      Pattern openPat  = null;
      Pattern closePat = null;

      switch (groupType)
      {
         case GROUP_COL:
           openPat = PATTERN_COL_ID;
           closePat = PATTERN_COL_ID_END;
         break;
         case GROUP_KEY:
           openPat = PATTERN_KEY_ID;
           closePat = PATTERN_KEY_ID_END;
         break;
         case GROUP_TITLE:
           openPat = PATTERN_TITLE_ID;
           closePat = PATTERN_TITLE_ID_END;
         break;
         case GROUP_TYPE:
           openPat = PATTERN_TYPE_ID;
           closePat = PATTERN_TYPE_ID_END;
         break;
         default:
            throw new IllegalArgumentException(
              DatatoolTk.getLabelWithValue("error.invalid_group_id",
                groupType));
      }

      String value = null;
      String line = null;

      while ((line = in.readLine()) != null)
      {
         linenum++;
         Matcher m = DatatoolDb.PATTERN_COMMENT.matcher(line);

         if (m.matches())
         {
            continue;
         }

         m = openPat.matcher(line);

         if (m.matches())
         {
            value = m.group(1);

            break;
         }

         throw new InvalidSyntaxException(
           DatatoolTk.getLabelWithValues("error.dbload.expected",
            ""+linenum, openPat.pattern()));
      }

      while ((line = in.readLine()) != null)
      {
         linenum++;

         Matcher m = closePat.matcher(line);

         if (m.matches())
         {
            break;
         }

         value += System.getProperty("line.separator", "\n") + line;
      }

      if (line == null)
      {
         throw new InvalidSyntaxException(
            DatatoolTk.getLabelWithValues("error.dbload.expected",
            ""+linenum, closePat.pattern()));
      }

      switch (groupType)
      {
         case GROUP_COL:
            try
            {
               return new Integer(value);
            }
            catch (NumberFormatException e)
            {
                throw new InvalidSyntaxException(
                   DatatoolTk.getLabelWithValues(
                      "error.invalid_col_id", ""+linenum, value));
            }
         case GROUP_TYPE:
            try
            {
               if (value.isEmpty())
               {
                  return DatatoolDb.TYPE_UNKNOWN;
               }
               else
               {
                  return new Integer(value);
               }
            }
            catch (NumberFormatException e)
            {
                throw new InvalidSyntaxException(
                   DatatoolTk.getLabelWithValues(
                      "error.invalid_type_id", ""+linenum, value));
            }
      }

      return value;
   }


   public void save(String filename)
     throws IOException
   {
      setFile(filename);
      save();
   }

   public void save()
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

            int colIdx = i+1;

            int type = header.getType();

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
            int rowIdx = i+1;

            out.println("\\db@row@elt@w %");
            out.println("\\db@row@id@w "+rowIdx+"%");
            out.println("\\db@row@id@end@ %");

            for (int j = 0, m = row.size(); j < m; j++)
            {
               String cell = row.get(j);
               int colIdx = j+1;

               out.println("\\db@col@id@w "+colIdx+"%");
               out.println("\\db@col@id@end@ %");

               out.println("\\db@col@elt@w "+cell+"%");
               out.println("\\db@col@elt@end@ %");

               out.println("\\db@col@id@w "+colIdx+"%");
               out.println("\\db@col@id@end@ %");
            }

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

            out.println("\\expandafter");
            out.println(" \\gdef\\csname dtl@ci@"+name
              +"@"+header.getKey()+"\\endcsname{"
               +(i+1)+"}%");
         }

         out.println("\\egroup");
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
      if (colIdx >= headers.size())
      {
         return null;
      }
      else
      {
         return headers.get(colIdx);
      }
   }

   public int getColumnType(int colIdx)
   {
      return headers.get(colIdx).getType();
   }

   public int getType(String value)
   {
      if (value == null || value.isEmpty()) return TYPE_UNKNOWN;

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

   public void setValue(int rowIdx, int colIdx, String value)
   {
      data.get(rowIdx).setCell(colIdx, value);

      DatatoolHeader header = headers.get(colIdx);

      // What's the data type of this value?

      int type = getType(value);

      // If it's unknown, do nothing

      if (type == TYPE_UNKNOWN)
      {
         return;
      }

      switch (header.getType())
      {
         case TYPE_UNKNOWN:
         case TYPE_INTEGER:
            // All other types override unknown and int
            header.setType(type);
         break;
         case TYPE_CURRENCY:
            // string overrides currency

            if (type == TYPE_STRING)
            {
               header.setType(type);
            }
         break;
         case TYPE_REAL:
            // string and currency override real
            if (type == TYPE_STRING || type == TYPE_CURRENCY)
            {
               header.setType(type);
            }
         break;
         // nothing overrides string
      }
   }

   public Object getValue(int rowIdx, int colIdx)
   {
      String value = getRow(rowIdx).get(colIdx);

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

      int n = data.size();

      if (rowIdx == n)
      {
         data.add(row);
      }
      else if (rowIdx > n)
      {
         for (int i = n; i < rowIdx; i++)
         {
            data.add(new DatatoolRow(this, headers.size()));
         }

         data.add(row);
      }
      else
      {
         data.add(rowIdx, row);
      }
   }

   public void insertColumn(DatatoolColumn column)
   {
      column.insertIntoData(headers, data);
   }

   public DatatoolHeader insertColumn(int colIdx)
   {
      return insertColumn(colIdx, new DatatoolHeader());
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
            headers.add(new DatatoolHeader());

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
         db.setName(templateFile.toString());

         TemplateHandler handler = new TemplateHandler(db);
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

   private DatatoolSettings settings;

   private Vector<DatatoolHeader> headers;

   private Vector<DatatoolRow> data;

   private File file;

   private String name;

   private int linenum;

   private int sortColumn = 0;

   private boolean sortAscending = true;

   private boolean sortCaseSensitive = false;

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
   private static final Pattern PATTERN_DBNAME = Pattern.compile("\\\\DTLifdbexists\\{(.+)\\}%\\s*");
   public static final Pattern PATTERN_COMMENT = Pattern.compile("\\s*%.*");
   private static final Pattern PATTERN_CLOSE = Pattern.compile("\\s*\\}%\\s*");
   private static final Pattern PATTERN_PLIST_ELT = Pattern.compile("\\s*\\\\db@plist@elt@w\\s*(%\\s*)?");
   private static final Pattern PATTERN_PLIST_ELT_END = Pattern.compile("\\s*\\\\db@plist@elt@end@\\s*(%\\s*)?");

   private static final Pattern PATTERN_ROW_ELT = Pattern.compile("\\s*\\\\db@row@elt@w\\s*(%\\s*)?");
   private static final Pattern PATTERN_ROW_ELT_END = Pattern.compile("\\s*\\\\db@row@elt@end@\\s*(%\\s*)?");
   private static final Pattern PATTERN_ROW_ID = Pattern.compile("\\s*\\\\db@row@id@w\\s*([0-9]+)(%\\s*)?");
   private static final Pattern PATTERN_ROW_ID_END = Pattern.compile("\\s*\\\\db@row@id@end@\\s*(%\\s*)?");
   private static final Pattern PATTERN_COL_ID = Pattern.compile("\\s*\\\\db@col@id@w\\s*([0-9]+)(%\\s*)?");
   private static final Pattern PATTERN_COL_ID_END = Pattern.compile("\\s*\\\\db@col@id@end@\\s*(%\\s*)?");

   private static final Pattern PATTERN_COL_ELT = Pattern.compile("\\s*\\\\db@col@elt@w\\s*(.*?)%?");
   private static final Pattern PATTERN_COL_ELT_END = Pattern.compile("\\s*\\\\db@col@elt@end@\\s*%?");

   private static final int GROUP_COL=0, GROUP_KEY=1, GROUP_TITLE=2, GROUP_TYPE=3;

   private static final Pattern PATTERN_KEY_ID = Pattern.compile("\\s*\\\\db@key@id@w\\s*(.*)%\\s*");
   private static final Pattern PATTERN_KEY_ID_END = Pattern.compile("\\s*\\\\db@key@id@end@\\s*%\\s*");
   private static final Pattern PATTERN_TYPE_ID = Pattern.compile("\\s*\\\\db@type@id@w\\s*([0-9]*)%\\s*");
   private static final Pattern PATTERN_TYPE_ID_END = Pattern.compile("\\s*\\\\db@type@id@end@\\s*%\\s*");
   private static final Pattern PATTERN_TITLE_ID = Pattern.compile("\\s*\\\\db@header@id@w\\s*(.*)%\\s*");
   private static final Pattern PATTERN_TITLE_ID_END = Pattern.compile("\\s*\\\\db@header@id@end@\\s*%\\s*");

   private static final Pattern[] PATTERN_VERBATIM =
    new Pattern[]
    { 
       Pattern.compile(".*\\\\begin\\s*\\{verbatim\\}.*", Pattern.DOTALL),
       Pattern.compile(".*\\\\verb\\b.*", Pattern.DOTALL),
       Pattern.compile(".*\\\\begin\\s*\\{lstlisting\\}.*", Pattern.DOTALL),
       Pattern.compile(".*\\\\lstinline\\b.*", Pattern.DOTALL)
    };
}
