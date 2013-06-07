package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.Vector;
import java.util.regex.*;
import java.util.Date;

import com.dickimawbooks.datatooltk.io.*;
import com.dickimawbooks.datatooltk.enumeration.*;

public class DatatoolDb
{
   public DatatoolDb()
   {
      headers = new Vector<DatatoolHeader>();
      data = new Vector<DatatoolRow>();
   }

   public DatatoolDb(int rows, int cols)
   {
      headers = new Vector<DatatoolHeader>(cols);
      data = new Vector<DatatoolRow>(rows);
   }

   public DatatoolDb(int cols)
   {
      headers = new Vector<DatatoolHeader>(cols);
      data = new Vector<DatatoolRow>();
   }

   public static DatatoolDb load(String filename)
     throws IOException
   {
      return load(new File(filename));
   }

   public static DatatoolDb load(File dbFile)
     throws IOException
   {
      BufferedReader in = null;
      DatatoolDb db = null;

      try
      {
         in = new BufferedReader(new FileReader(dbFile));

         db = new DatatoolDb();

         int linenum = 0;
         String line;

         // Skip any comment lines at the start of the file

         while ((line = in.readLine()) != null)
         {
            linenum++;
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
                  ""+linenum, "\\DTLifdbexists"));
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
            linenum++;
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

            linenum++;
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
                 ""+linenum, "\\db@plist@elt@w"));
            }

            DatatoolHeader header = new DatatoolHeader();

            linenum = header.parseHeader(in, linenum);

            db.addColumn(header);

            while ((line = in.readLine()) != null)
            {
               // Ignore commented lines

               linenum++;
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
                      ""+linenum, "\\db@plist@elt@end@"));
               }

               break;
            }
         }

         // skip until we reach "\\csname dtldb@<name>\endcsname={"

         p = Pattern.compile("\\s*\\\\csname\\s+dtldb@"
           + db.name+"\\\\endcsname\\s*=\\s*\\{%.*");

         while ((line = in.readLine()) != null)
         {
            linenum++;
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
            linenum++;

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
                     ""+linenum, PATTERN_ROW_ELT.pattern()));
               }

               line = in.readLine();

               if (line == null)
               {
                  break;
               }

               linenum++;

               m = PATTERN_ROW_ID.matcher(line);

               if (!m.matches())
               {
                   throw new InvalidSyntaxException(
                    DatatoolTk.getLabelWithValues(
                    "error.dbload.expected",
                     ""+linenum, PATTERN_ROW_ID.pattern()));
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

               linenum++;

               m = PATTERN_ROW_ID_END.matcher(line);

               if (!m.matches())
               {
                   throw new InvalidSyntaxException(
                    DatatoolTk.getLabelWithValues(
                    "error.dbload.expected",
                     ""+linenum, PATTERN_ROW_ID_END.pattern()));
               }

               // Now read in columns

               while ((line = in.readLine()) != null)
               {
                  linenum++;

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
                                 new String[]{""+linenum, ""+rowIdx, ""+idx}
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

                      linenum++;

                      m = PATTERN_ROW_ID_END.matcher(line);

                      if (!m.matches())
                      {
                         throw new InvalidSyntaxException(
                            DatatoolTk.getLabelWithValues(
                            "error.dbload.missing_end_row_tag_pat",
                            new String[]
                            {
                              ""+linenum,
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

                      linenum++;

                      m = PATTERN_ROW_ELT_END.matcher(line);

                      if (!m.matches())
                      {
                         throw new InvalidSyntaxException(
                            DatatoolTk.getLabelWithValues(
                            "error.dbload.missing_end_row_tag_pat",
                            new String[]
                            {
                              ""+linenum,
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
                          ""+linenum, PATTERN_COL_ID.pattern()));
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

                  linenum++;

                  m = PATTERN_COL_ID_END.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                        DatatoolTk.getLabelWithValues(
                           "error.dbload.missing_col_tag",
                           ""+linenum, PATTERN_COL_ID_END.pattern()));
                  }

                  // Read cell data

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException(
                        DatatoolTk.getLabelWithValue(
                          "error.dbload.col_data_eof", colIdx));
                  }

                  linenum++;

                  m = PATTERN_COL_ELT.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                        DatatoolTk.getLabelWithValues(
                           "error.dbload.missing_col_tag",
                            ""+linenum, PATTERN_COL_ID_END.pattern()));
                  }

                  String value = m.group(1);

                  while ((line = in.readLine()) != null)
                  {
                     linenum++;

                     m = PATTERN_COL_ELT_END.matcher(line);

                     if (m.matches())
                     {
                        break;
                     }

                     value += System.getProperty("line.separator", "\n") + line;
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

                  linenum++;

                  m = PATTERN_COL_ID.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                       DatatoolTk.getLabelWithValues(
                         "error.dbload.missing_end_col", 
                         ""+linenum, ""+colIdx));
                  }

                  try
                  {
                     int idx = Integer.parseInt(m.group(1));

                     if (idx != colIdx)
                     {
                         throw new InvalidSyntaxException(
                           DatatoolTk.getLabelWithValues(
                             "error.dbload.wrong_end_col_tag",
                             new String[]{""+linenum, ""+colIdx, ""+idx}));
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

                  linenum++;

                  m = PATTERN_COL_ID_END.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException(
                        DatatoolTk.getLabelWithValues(
                          "error.dbload.missing_end_col_tag",
                          ""+linenum, ""+colIdx));
                  }

                  db.addCell(rowIdx, colIdx, value);

               }

               if (done) break;

               line = in.readLine();
               linenum++;
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

      return db;
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

         for (HeaderEnumeration en = headerElements(); en.hasMoreElements();)
         {
            DatatoolHeader header = en.nextElement();

            int type = header.getType();

            out.println("\\db@plist@elt@w %");
            out.println("\\db@col@id@w "+header.getColumnIndex()+"%");
            out.println("\\db@col@id@end@ %");
            out.println("\\db@key@id@w "+header.getKey()+"%");
            out.println("\\db@key@id@end@ %");
            out.println("\\db@type@id@w "
               +(type==TYPE_UNKNOWN?"":type)+"%");
            out.println("\\db@type@id@end@ %");
            out.println("\\db@header@id@w "+header.getTitle()+"%");
            out.println("\\db@header@id@end@ %");
            out.println("\\db@col@id@w "+header.getColumnIndex()+"%");
            out.println("\\db@col@id@end@ %");
            out.println("\\db@plist@elt@end@ %");
         }

         out.println("}%"); // end of dtlkeys@<name>

         out.println("\\expandafter\\global\\expandafter");
         out.println("\\newtoks\\csname dtldb@"+name+"\\endcsname");
         out.println("\\expandafter\\global");
         out.println("\\csname dtldb@"+name+"\\endcsname={%");
         out.println("%");

         for (RowEnumeration en=rowElements(); en.hasMoreElements(); )
         {
            DatatoolRow row = en.nextElement();

            out.println("\\db@row@elt@w %");
            out.println("\\db@row@id@w "+row.getRowIndex()+"%");
            out.println("\\db@row@id@end@ %");

            for (CellEnumeration ce=row.cellElements(); ce.hasMoreElements();)
            {
               DatatoolCell cell = ce.nextElement();

               out.println("\\db@col@id@w "+cell.getIndex()+"%");
               out.println("\\db@col@id@end@ %");

               out.println("\\db@col@elt@w "+cell.getValue()+"%");
               out.println("\\db@col@elt@end@ %");

               out.println("\\db@col@id@w "+cell.getIndex()+"%");
               out.println("\\db@col@id@end@ %");
            }

            out.println("\\db@row@id@w "+row.getRowIndex()+"%");
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

         for (DatatoolHeader header : headers)
         {
            out.println("\\expandafter");
            out.println(" \\gdef\\csname dtl@ci@"+name
              +"@"+header.getKey()+"\\endcsname{"
              +header.getColumnIndex()+"}%");
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
      // Do we already have a row with index rowIdx ?

      DatatoolRow row = getRow(rowIdx);

      if (row == null)
      {
         row = new DatatoolRow();
         row.setRowIndex(rowIdx);
         data.add(row);
      }

      row.setCell(colIdx, value);

      if (!value.isEmpty())
      {
         // What's the datatype?

         int type = TYPE_UNKNOWN;

         try
         {
            int num = Integer.parseInt(value);
   
            type = TYPE_INTEGER;
         }
         catch (NumberFormatException ie)
         {
            try
            {
               float num = Float.parseFloat(value);

               type = TYPE_REAL;
            }
            catch (NumberFormatException fe)
            {
               // TODO test for currency

               type = TYPE_STRING;
            }
         }

         // Does this column have a type assigned to it?

         DatatoolHeader header = getHeader(colIdx);

         switch (header.getType())
         {
            case TYPE_STRING:
            break;
            case TYPE_UNKNOWN:
              header.setType(type);
            break;
            case TYPE_CURRENCY:
              if (type == TYPE_STRING)
              {
                 header.setType(type);
              }
            break;
            case TYPE_REAL:
              if (type == TYPE_STRING || type == TYPE_CURRENCY)
              {
                 header.setType(type);
              }
            break;
            case TYPE_INTEGER:
              if (type != TYPE_INTEGER)
              {
                 header.setType(type);
              }
            break;
         }
      }
   }

   public void addColumn(DatatoolHeader header)
   {
      int colIndex = header.getColumnIndex();

      if (colIndex == -1)
      {
         header.setColumnIndex(headers.size()+1);
      }

      headers.add(header);
   }

   // headerIndex is the datatool header index which starts from 1

   public DatatoolHeader getHeader(int headerIndex)
   {
      for (DatatoolHeader header : headers)
      {
         if (header.getColumnIndex() == headerIndex)
         {
            return header;
         }
      }

      return null;
   }

   // rowIndex is the datatool row index which starts from 1

   public DatatoolRow getRow(int rowIndex)
   {
      for (RowEnumeration en=rowElements(rowIndex-1);
           en.hasMoreElements();)
      {
         DatatoolRow row = en.nextElement();

         if (row.getRowIndex() == rowIndex)
         {
            return row;
         }
      }

      return null;
   }

   public HeaderEnumeration headerElements()
   {
      return new HeaderEnumeration(headers);
   }

   public RowEnumeration rowElements()
   {
      return new RowEnumeration(data);
   }

   public RowEnumeration rowElements(int offset)
   {
      return new RowEnumeration(data, offset);
   }

   public String[] getColumnTitles()
   {
      String[] fields = new String[headers.size()];

      int i = 0;

      for (HeaderEnumeration en=headerElements();
           en.hasMoreElements(); )
      {
         i++;

         DatatoolHeader header = en.nextElement();

         if (header == null)
         {
            fields[i] = "";
         }
         else
         {
            fields[i] = header.getTitle();
         }

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

   private Vector<DatatoolHeader> headers;

   private Vector<DatatoolRow> data;

   private File file;

   private String name;

   public static final int TYPE_UNKNOWN=-1, TYPE_STRING = 0, TYPE_INTEGER=1,
     TYPE_REAL=2, TYPE_CURRENCY=3;

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

   private static final Pattern PATTERN_COL_ELT = Pattern.compile("\\s*\\\\db@col@elt@w\\s*(.*)%\\s*");
   private static final Pattern PATTERN_COL_ELT_END = Pattern.compile("\\s*\\\\db@col@elt@end@\\s*(%\\s*)?");
}
