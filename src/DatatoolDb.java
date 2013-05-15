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

   public static DatatoolDb importCSV(String filename,
      DatatoolSettings settings)
     throws IOException
   {
      return importCSV(new File(filename));
   }

   public static DatatoolDb importCSV(File csvFile)
     throws IOException
   {
      DatatoolDb db = null;
      BufferedReader in = null;

      try
      {
         in = new BufferedReader(new FileReader(csvFile));

         db = new DatatoolDb();
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
           + db.name+"\\\\endcsname\\s*=\\s*\\{%\\s*");

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
            throw new EOFException("Premature end of file. Failed to find \\csname dtlkeys@"+db.name+"\\endcsname{%");
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

   public void addColumn(DatatoolHeader header)
   {
      int colIndex = header.getColumnIndex();

      if (colIndex == -1)
      {
         header.setColumnIndex(headers.size()+1);
         headers.add(header);
      }
      else if (colIndex == headers.size()+1)
      {
         headers.add(header);
      }
      else if (colIndex <= headers.size())
      {
         headers.set(colIndex-1, header);
      }
      else
      {
         for (int i = headers.size()+1; i < colIndex; i++)
         {
            headers.add(new DatatoolHeader());
         }

         headers.add(header);
      }
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
}
