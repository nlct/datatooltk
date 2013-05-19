package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.Vector;
import java.util.regex.*;

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
               throw new InvalidSyntaxException("l."+linenum
                +" Expected '\\DTLifdbexists'");
            }
         }

         if (line == null)
         {
            throw new EOFException("Premature end of file. Failed to find \\DTLifdbexists");
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
            throw new EOFException("Premature end of file. Failed to find '"+p.pattern()+"'");
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
               throw new InvalidSyntaxException("l."+linenum
                 +" Expected '\\db@plist@elt@w'");
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
                  throw new InvalidSyntaxException("l."+linenum
                    +" Expected '\\db@plist@elt@end@'");
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
            throw new EOFException("Premature end of file. Failed to find '"+p.pattern()+"'");
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
                   throw new InvalidSyntaxException("l."
                     +linenum+" Expected '"
                     +PATTERN_ROW_ELT.pattern()+"'");
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
                   throw new InvalidSyntaxException("l."
                     +linenum+" Expected '"
                     +PATTERN_ROW_ID.pattern()+"'");
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
                   throw new InvalidSyntaxException("l."
                     +linenum+" Expected '"
                     +PATTERN_ROW_ID_END.pattern()+"'");
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
                            throw new InvalidSyntaxException("l."+linenum
                           + " Row "+rowIdx
                           +" ended with closing tag for row "+idx);
                         }
                      }
                      catch (NumberFormatException e)
                      {
                         // shouldn't happen
                      }

                      line = in.readLine();

                      if (line == null)
                      {
                         throw new EOFException("Unexpected end of file while parsing end of row "+rowIdx+" tag");
                      }

                      linenum++;

                      m = PATTERN_ROW_ID_END.matcher(line);

                      if (!m.matches())
                      {
                         throw new InvalidSyntaxException("l."+linenum
                          + " Missing end of row tag '"
                          + PATTERN_ROW_ID_END.pattern()
                          + "' for row "+rowIdx);
                      }

                      line = in.readLine();

                      if (line == null)
                      {
                         throw new EOFException("Unexpected end of file while parsing end of row "+rowIdx+" tag");
                      }

                      linenum++;

                      m = PATTERN_ROW_ELT_END.matcher(line);

                      if (!m.matches())
                      {
                         throw new InvalidSyntaxException("l."+linenum
                          + " Missing end of row tag '"
                          + PATTERN_ROW_ELT_END.pattern()
                          + "' for row "+rowIdx);
                      }

                      break;
                  }

                  // read in column data for current row

                  m = PATTERN_COL_ID.matcher(line);

                  int colIdx = -1;

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException("l."+linenum
                        + " Expected column tag '"
                        + PATTERN_COL_ID.pattern()+"'");
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
                     throw new EOFException("Unexpected end of file while parsing tag for column "+colIdx);
                  }

                  linenum++;

                  m = PATTERN_COL_ID_END.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException("l."+linenum
                        + " Expected column tag '"
                        + PATTERN_COL_ID_END.pattern()+"'");
                  }

                  // Read cell data

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException("Unexpected end of file while parsing data for column "+colIdx);
                  }

                  linenum++;

                  m = PATTERN_COL_ELT.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException("l."+linenum
                        + " Expected column tag '"
                        + PATTERN_COL_ID_END.pattern()+"'");
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
                     throw new EOFException("Unexpected end of file while parsing data for column "+colIdx);
                  }

                  // check for end column tag

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException("Unexpected end of file while parsing end tag for column "+colIdx);
                  }

                  linenum++;

                  m = PATTERN_COL_ID.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException("l."+linenum
                      + " Expected end tag for column "+colIdx);
                  }

                  try
                  {
                     int idx = Integer.parseInt(m.group(1));

                     if (idx != colIdx)
                     {
                         throw new InvalidSyntaxException("l."+linenum
                           + " Column "+colIdx
                           +" ended with closing tag for column "+idx);
                     }
                  }
                  catch (NumberFormatException e)
                  {
                     // shouldn't happen
                  }

                  line = in.readLine();

                  if (line == null)
                  {
                     throw new EOFException("Unexpected end of file while parsing end tag for column "+colIdx);
                  }

                  linenum++;

                  m = PATTERN_COL_ID_END.matcher(line);

                  if (!m.matches())
                  {
                     throw new InvalidSyntaxException("l."+linenum
                      + " Expected end tag for column "+colIdx);
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
            throw new EOFException("Premature end of file. Failed to find end brace for '"+ p.pattern()+"'");
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
      return name == null ? (file == null ? null : file.getName()): name;
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
      for (DatatoolRow row : data)
      {
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

   private Vector<DatatoolHeader> headers;

   private Vector<DatatoolRow> data;

   private File file;

   private String name;

   public static final int TYPE_STRING = 0, TYPE_INTEGER=1,
     TYPE_REAL=2, TYPE_CURRENCY=3;

   private static final Pattern PATTERN_DBNAME = Pattern.compile("\\\\DTLifdbexists\\{(.+)\\}%");
   public static final Pattern PATTERN_COMMENT = Pattern.compile("\\s*%.*");
   private static final Pattern PATTERN_CLOSE = Pattern.compile("\\s*\\}%.*");
   private static final Pattern PATTERN_PLIST_ELT = Pattern.compile("\\s*\\\\db@plist@elt@w\\s*(%.*)?");
   private static final Pattern PATTERN_PLIST_ELT_END = Pattern.compile("\\s*\\\\db@plist@elt@end@\\s*(%.*)?");

   private static final Pattern PATTERN_ROW_ELT = Pattern.compile("\\s*\\\\db@row@elt@w\\s*(%.*)?");
   private static final Pattern PATTERN_ROW_ELT_END = Pattern.compile("\\s*\\\\db@row@elt@end@\\s*(%.*)?");
   private static final Pattern PATTERN_ROW_ID = Pattern.compile("\\s*\\\\db@row@id@w\\s*([0-9]+)(%.*)?");
   private static final Pattern PATTERN_ROW_ID_END = Pattern.compile("\\s*\\\\db@row@id@end@\\s*(%.*)?");
   private static final Pattern PATTERN_COL_ID = Pattern.compile("\\s*\\\\db@col@id@w\\s*([0-9]+)(%.*)?");
   private static final Pattern PATTERN_COL_ID_END = Pattern.compile("\\s*\\\\db@col@id@end@\\s*(%.*)?");

   private static final Pattern PATTERN_COL_ELT = Pattern.compile("\\s*\\\\db@col@elt@w\\s*(.*)");
   private static final Pattern PATTERN_COL_ELT_END = Pattern.compile("\\s*\\\\db@col@elt@end@\\s*(%.*)?");
}
