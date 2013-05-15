package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.regex.*;

public class DatatoolHeader
{
   public DatatoolHeader()
   {
      this(null);
   }

   public DatatoolHeader(String key)
   {
      this(key, key);
   }

   public DatatoolHeader(String key, String title)
   {
      this(key, title, DatatoolDb.TYPE_STRING);
   }

   public DatatoolHeader(String key, String title, int type)
   {
      setKey(key);
      setTitle(title);
      setType(type);
   }

   public int getType()
   {
      return type;
   }

   public String getKey()
   {
      return key;
   }

   public String getTitle()
   {
      return title;
   }

   public void setType(int type)
   {
      if (type < DatatoolDb.TYPE_STRING || type > DatatoolDb.TYPE_CURRENCY)
      {
         throw new IllegalArgumentException("Invalid data type "+type);
      }

      this.type = type;
   }

   public void setKey(String key)
   {
      this.key = key;
   }

   public void setTitle(String title)
   {
      this.title = title;
   }

   public int parseHeader(BufferedReader in, int linenum)
    throws IOException
   {
      linenum = parseGroup(in, linenum, GROUP_COL);
      int idx = colIndex;
      linenum = parseGroup(in, linenum, GROUP_KEY);
      linenum = parseGroup(in, linenum, GROUP_TYPE);
      linenum = parseGroup(in, linenum, GROUP_TITLE);
      linenum = parseGroup(in, linenum, GROUP_COL);

      if (colIndex != idx)
      {
         throw new InvalidSyntaxException("l."+linenum
          +" Header data for column "
          +idx+" ended by index "+colIndex);
      }

      return linenum;
   }

   private int parseGroup(BufferedReader in, int linenum,
    int groupType)
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
            throw new IllegalArgumentException("Invalid group identifier "+groupType);
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

         throw new InvalidSyntaxException("l."+linenum
            + " Expected '"+openPat.pattern()+"'");
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
         throw new InvalidSyntaxException("l."+linenum
            + " Expected '"+closePat.pattern()+"'");
      }

      switch (groupType)
      {
         case GROUP_COL:
            try
            {
               colIndex = Integer.parseInt(value);
            }
            catch (NumberFormatException e)
            {
                throw new InvalidSyntaxException("l."+linenum
                 +" Invalid column index (found '"+value+"')");
            }
         break;
         case GROUP_KEY:
            key = value;
         break;
         case GROUP_TITLE:
            title = value;
         break;
         case GROUP_TYPE:
            try
            {
               if (value.equals(""))
               {
                  type = DatatoolDb.TYPE_STRING;
               }
               else
               {
                  type = Integer.parseInt(value);
               }
            }
            catch (NumberFormatException e)
            {
                throw new InvalidSyntaxException("l."+linenum
                 +" Invalid type index (found '"+value+"')");
            }
         break;
      }

      return linenum;
   }

   public int getColumnIndex()
   {
      return colIndex;
   }

   public void setColumnIndex(int index)
   {
      colIndex = index;
   }

   private String key;
   private String title;
   private int type;
   private int colIndex=-1;

   private static final int GROUP_COL=0, GROUP_KEY=1, GROUP_TITLE=2, GROUP_TYPE=3;

   private static final Pattern PATTERN_COL_ID = Pattern.compile("\\s*\\\\db@col@id@w\\s*([0-9]+)%.*");
   private static final Pattern PATTERN_COL_ID_END = Pattern.compile("\\s*\\\\db@col@id@end@\\s*%.*");
   private static final Pattern PATTERN_KEY_ID = Pattern.compile("\\s*\\\\db@key@id@w\\s*(.*)");
   private static final Pattern PATTERN_KEY_ID_END = Pattern.compile("\\s*\\\\db@key@id@end@\\s*%.*");
   private static final Pattern PATTERN_TYPE_ID = Pattern.compile("\\s*\\\\db@type@id@w\\s*([0-9]*)%.*");
   private static final Pattern PATTERN_TYPE_ID_END = Pattern.compile("\\s*\\\\db@type@id@end@\\s*%.*");
   private static final Pattern PATTERN_TITLE_ID = Pattern.compile("\\s*\\\\db@header@id@w\\s*(.*)");
   private static final Pattern PATTERN_TITLE_ID_END = Pattern.compile("\\s*\\\\db@header@id@end@\\s*%.*");

}
