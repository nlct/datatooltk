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
package com.dickimawbooks.datatooltk.io;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import com.dickimawbooks.texparserlib.latex.inputenc.InputEncSty;

import com.dickimawbooks.datatooltk.MessageHandler;

public class DatatoolFileFormat
{
   public DatatoolFileFormat(MessageHandler messageHandler)
   {
      this.messageHandler = messageHandler;
   }

   public static DatatoolFileFormat valueOf(MessageHandler messageHandler,
      File file)
    throws IOException
   {
      return valueOf(messageHandler, file, false);
   }

   public static DatatoolFileFormat valueOf(MessageHandler messageHandler,
      File file, boolean skipProbeforTeX)
    throws IOException
   {
      DatatoolFileFormat dff = new DatatoolFileFormat(messageHandler);

      dff.probeFile(file, skipProbeforTeX);

      return dff;
   }

   protected void probeFile(File file, boolean skipProbeforTeX)
    throws IOException
   {
      String name = file.getName();

      int idx = name.lastIndexOf('.');

      format = 0;

      if (idx > 0)
      {
         extension = name.substring(idx+1).toLowerCase();

         if (extension.equals("dbtex"))
         {
            format = FILE_FORMAT_FLAG_DBTEX;
         }
         else if (extension.equals("dtltex"))
         {
            format = FILE_FORMAT_FLAG_DTLTEX;
         }
         else if (extension.equals("tex") || extension.equals("ltx"))
         {
            format = FILE_FORMAT_FLAG_TEX;
         }
      }

      byte[] buffer = new byte[256];

      FileInputStream in = null;

      try
      {
         in = new FileInputStream(file);

         int n = in.read(buffer);

         if (n > 0)
         {
            if (startsWith(buffer, BOM, 0, n))
            {
               charset = StandardCharsets.UTF_8;

               if (!skipProbeforTeX && buffer[BOM.length] == '%')
               {
                  if (startsWith(buffer, DBTEX_MARKER, BOM.length, n))
                  {
                     if (startsWith(buffer, V2_MARKER,
                          BOM.length + DBTEX_MARKER.length, n))
                     {
                        format = FILE_FORMAT_FLAG_DBTEX2;
                     }
                     else if (startsWith(buffer, V3_MARKER,
                               BOM.length + DBTEX_MARKER.length, n))
                     {
                        format = FILE_FORMAT_FLAG_DBTEX3;
                     }
                     else
                     {
                        format = FILE_FORMAT_FLAG_DBTEX;
                     }
                  }
                  else if (startsWith(buffer, DTLTEX_MARKER, BOM.length, n))
                  {
                     if (startsWith(buffer, V2_MARKER,
                               BOM.length + DTLTEX_MARKER.length, n))
                     {
                        format = FILE_FORMAT_FLAG_DTLTEX2;
                     }
                     else if (startsWith(buffer, V3_MARKER,
                               BOM.length + DTLTEX_MARKER.length, n))
                     {
                        format = FILE_FORMAT_FLAG_DTLTEX3;
                     }
                     else
                     {
                        format = FILE_FORMAT_FLAG_DBTEX;
                     }
                  }
                  else
                  {
                     format = FILE_FORMAT_FLAG_TEX;
                  }
               }
               else if (contains(buffer, (byte)'\t', BOM.length, n))
               {
                  format = FILE_FORMAT_FLAG_TSV;
               }
               else
               {
                  format = FILE_FORMAT_FLAG_CSV;
               }
            }
            else if (startsWith(buffer, ZIP_MARKER, 0, n))
            {
               if (startsWith(buffer, MIMETYPE_MARKER, 30, n))
               {
                  if (startsWith(buffer, ODS_MIMETYPE, 38, n))
                  {
                     format = FILE_FORMAT_FLAG_ODS;
                  }
                  else if (startsWith(buffer, XLSX_MIMETYPE, 38, n))
                  {
                     // xlsx doesn't seem to include mimetype but
                     // include check anyway
                     format = FILE_FORMAT_FLAG_XLSX;
                  }
                  else
                  {
                     throw new UnsupportedFileFormatException( 
                      messageHandler.getLabel("error.unsupported_mimetype"));
                  }
               }
               else
               {
                  // assume xlsx
                  format = FILE_FORMAT_FLAG_XLSX;
               }
            }
            else if (startsWith(buffer, XML_MARKER, 0, n))
            {
               if (contains(buffer, OFFICE_DOCUMENT_MARKER, 6, n))
               {
                  format = FILE_FORMAT_FLAG_FODS;
               }
               else
               {
                  throw new UnsupportedFileFormatException( 
                   messageHandler.getLabel("error.unsupported_xml"));
               }
            }
            else if (startsWith(buffer, XLS_MARKER, 0, n))
            {
               format = FILE_FORMAT_FLAG_XLS;
            }
            else if (!skipProbeforTeX && buffer[0] == '%')
            {
               if (startsWith(buffer, DBTEX_MARKER, 0, n))
               {
                  if (startsWith(buffer, V2_MARKER, DBTEX_MARKER.length, n))
                  {
                     format = FILE_FORMAT_FLAG_DBTEX2;

                     readEncoding(buffer, DBTEX_MARKER.length + V2_MARKER.length, n);
                  }
                  else if (startsWith(buffer, V3_MARKER, DBTEX_MARKER.length, n))
                  {
                     format = FILE_FORMAT_FLAG_DBTEX3;

                     readEncoding(buffer, DBTEX_MARKER.length + V3_MARKER.length, n);
                  }
                  else
                  {
                     format = FILE_FORMAT_FLAG_DBTEX;
                  }
               }
               else if (startsWith(buffer, DTLTEX_MARKER, 0, n))
               {
                  if (startsWith(buffer, V2_MARKER, DTLTEX_MARKER.length, n))
                  {
                     format = FILE_FORMAT_FLAG_DTLTEX2;

                     readEncoding(buffer, DTLTEX_MARKER.length + V2_MARKER.length, n);
                  }
                  else if (startsWith(buffer, V3_MARKER, DTLTEX_MARKER.length, n))
                  {
                     format = FILE_FORMAT_FLAG_DTLTEX3;

                     readEncoding(buffer, DTLTEX_MARKER.length + V3_MARKER.length, n);
                  }
                  else
                  {
                     format = FILE_FORMAT_FLAG_DBTEX;
                  }
               }
               else
               {
                  if (format == 0)
                  {
                     format = FILE_FORMAT_FLAG_TEX;
                  }

                  StringBuilder result = new StringBuilder();
                  int i = readASCIIWord(buffer, 1, n, result);
                  String word = result.toString();

                  if (word.compareToIgnoreCase("encoding:") == 0)
                  {
                     readEncoding(buffer, i, n);
                  }
                  else if (word.compareToIgnoreCase("!tex") == 0)
                  {
                     checkPlingTeXEncodingComment(buffer, i, n);
                  }
               }
            }
            else if (format == 0 && contains(buffer, (byte)'\t', 0, n))
            {
               format = FILE_FORMAT_FLAG_TSV;
            }
            else if (format == 0)
            {
               format = FILE_FORMAT_FLAG_CSV;
            }
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

   }

   protected void checkPlingTeXEncodingComment(
      byte[] buffer, int startIdx, int buffLen)
   {
      if (startIdx < 0 || startIdx >= buffLen) return;

      StringBuilder result = new StringBuilder();
      int i = readASCIIWord(buffer, startIdx, buffLen, result);
      String word = result.toString();

      if (word.compareToIgnoreCase("encoding") == 0)
      {
         result.setLength(0);
         i = readASCIIWord(buffer, i, buffLen, result);
         word = result.toString();

         if (word.equals("="))
         {
            readEncoding(buffer, i, buffLen);
         }
      }
      else
      {
         i = indexOf(buffer, (byte)'%', startIdx, buffLen);

         if (i > -1 && i < buffLen-1)
         {
            result.setLength(0);
            i = readASCIIWord(buffer, i, buffLen, result);
            word = result.toString();

            if (word.compareToIgnoreCase("!tex") == 0)
            {
               checkPlingTeXEncodingComment(buffer, i, buffLen);
            }
         }
      }
   }

   protected boolean startsWith(byte[] buffer, byte[] marker,
      int startIdx, int buffLen)
   {
      if (buffLen - startIdx < marker.length)
      {
         return false;
      }

      for (int i = startIdx, j = 0; i < buffLen && j < marker.length; i++, j++)
      {
         if (buffer[i] != marker[j]) return false;
      }

      return true;
   }

   protected boolean contains(byte[] buffer, byte[] marker,
      int startIdx, int buffLen)
   {
      for (int i = startIdx; i < buffLen; i++)
      {
         if (startsWith(buffer, marker, i, buffLen))
         {
            return true;
         }
      }

      return false;
   }

   protected boolean contains(byte[] buffer, byte marker,
      int startIdx, int buffLen)
   {
      for (int i = startIdx; i < buffLen; i++)
      {
         if (buffer[i] == marker) return true;
      }

      return false;
   }

   protected int indexOf(byte[] buffer, byte marker,
      int startIdx, int buffLen)
   {
      for (int i = startIdx; i < buffLen; i++)
      {
         if (buffer[i] == marker) return i;
      }

      return -1;
   }

   protected int indexOf(byte[] buffer, byte[] marker,
      int startIdx, int buffLen)
   {
      for (int i = startIdx; i < buffLen; i++)
      {
         if (startsWith(buffer, marker, i, buffLen))
         {
            return i;
         }
      }

      return -1;
   }

   protected String readASCIIWord(byte[] buffer, int startIdx, int buffLen)
   {
      StringBuilder builder = new StringBuilder();

      readASCIIWord(buffer, startIdx, buffLen, builder);

      return builder.toString();
   }

   protected int readASCIIWord(byte[] buffer, int startIdx, int buffLen,
      StringBuilder result)
   {
      boolean started = false;

      for (int i = startIdx; i < buffLen; i++)
      {
         byte b = buffer[i];

         if (Character.isWhitespace((char)b))
         {
            if (started)
            {
               return i;
            }
         }
         else if (b > 0x7F || Character.isISOControl((char)b))
         {
            return i;
         }
         else
         {
            result.append((char)b);
            started = true;
         }
      }

      return buffLen;
   }

   protected void readEncoding(byte[] buffer, int startIdx, int buffLen)
   {
      String encoding = readASCIIWord(buffer, startIdx, buffLen);

      if (!encoding.isEmpty())
      {
         try
         {
            encoding = InputEncSty.getCharSetName(encoding);

            charset = Charset.forName(encoding);
         }
         catch (IllegalCharsetNameException
               | UnsupportedCharsetException e)
         {
         }
         catch (IllegalArgumentException e)
         {
         }
      }
   }

   public int getFileFormat()
   {
      return format;
   }

   public Charset getEncoding()
   {
      return charset;
   }

   public String getExtension()
   {
      return extension;
   }

   public static boolean isAnyTeX(int formats)
   {
      return ((formats | FILE_FORMAT_ANY_TEX) & FILE_FORMAT_ANY_TEX)
        == FILE_FORMAT_ANY_TEX;
   }

   public static boolean isAnyNonTeX(int formats)
   {
      return ((formats | FILE_FORMAT_ANY_NON_TEX)
               & FILE_FORMAT_ANY_NON_TEX) == FILE_FORMAT_ANY_NON_TEX;
   }

   public static boolean isTeX(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_TEX) == FILE_FORMAT_FLAG_TEX;
   }

   public static boolean isDTLTEX(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_DTLTEX) == FILE_FORMAT_FLAG_DTLTEX;
   }

   public static boolean isDTLTEX2(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_DTLTEX2) == FILE_FORMAT_FLAG_DTLTEX2;
   }

   public static boolean isDTLTEX3(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_DTLTEX3) == FILE_FORMAT_FLAG_DTLTEX3;
   }

   public static boolean isDBTEX(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_DBTEX) == FILE_FORMAT_FLAG_DBTEX;
   }

   public static boolean isDBTEX2(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_DBTEX2) == FILE_FORMAT_FLAG_DBTEX2;
   }

   public static boolean isDBTEX3(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_DBTEX3) == FILE_FORMAT_FLAG_DBTEX3;
   }

   public static boolean isCSV(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_CSV) == FILE_FORMAT_FLAG_CSV;
   }

   public static boolean isTSV(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_TSV) == FILE_FORMAT_FLAG_TSV;
   }

   public static boolean isODS(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_ODS) == FILE_FORMAT_FLAG_ODS;
   }

   public static boolean isFODS(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_FODS) == FILE_FORMAT_FLAG_FODS;
   }

   public static boolean isXLSX(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_XLSX) == FILE_FORMAT_FLAG_XLSX;
   }

   public static boolean isXLS(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_XLS) == FILE_FORMAT_FLAG_XLS;
   }

   public static boolean isSQL(int formats)
   {
      return (formats & FILE_FORMAT_FLAG_SQL) == FILE_FORMAT_FLAG_SQL;
   }

   public static boolean isSQLOnly(int formats)
   {
      return (formats | FILE_FORMAT_FLAG_SQL) == FILE_FORMAT_FLAG_SQL;
   }

   public static boolean isCsvOrTsv(int formats)
   {
      return ((formats | FILE_FORMAT_CSV_OR_TSV)
               & FILE_FORMAT_CSV_OR_TSV) == FILE_FORMAT_CSV_OR_TSV;
   }

   public static boolean isCsvOrTsvOnly(int formats)
   {
      return (formats | FILE_FORMAT_CSV_OR_TSV)
       == FILE_FORMAT_CSV_OR_TSV;
   }

   public static boolean isTeXOnly(int formats)
   {
      return (formats | FILE_FORMAT_ANY_TEX)
        == FILE_FORMAT_ANY_TEX;
   }

   public static boolean isSpreadSheet(int formats)
   {
      return ((formats | FILE_FORMAT_ANY_SPREADSHEET)
               & FILE_FORMAT_ANY_SPREADSHEET) == FILE_FORMAT_ANY_SPREADSHEET;
   }

   public static boolean isSpreadSheetOnly(int formats)
   {
      return (formats | FILE_FORMAT_ANY_SPREADSHEET)
       == FILE_FORMAT_ANY_SPREADSHEET;
   }


   public boolean isAnyTeX()
   {
      return isAnyTeX(format);
   }

   public boolean isCsvOrTsv()
   {
      return isCsvOrTsv(format);
   }

   int format;
   Charset charset;
   String extension;
   MessageHandler messageHandler;

   public static final byte[] ZIP_MARKER = new byte[]
     { (byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04 };

   public static final byte[] MIMETYPE_MARKER = 
     "mimetype".getBytes();

   public static final byte[] ODS_MIMETYPE = 
     "application/vnd.oasis.opendocument.spreadsheet".getBytes();

   public static final byte[] XML_MARKER = "<?xml".getBytes();

   public static final byte[] OFFICE_DOCUMENT_MARKER
    = "<office:document".getBytes();

   public static final byte[] XLSX_MIMETYPE =
     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".getBytes();

   public static final byte[] XLS_MARKER = new byte[]
     { (byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0,
       (byte)0xA1, (byte)0xB1, (byte)0x1A, (byte)0xE1};

   public static final byte[] BOM = new byte[]
     { (byte)0xEF, (byte)0xBB, (byte)0xBF };

   public static final byte[] DBTEX_MARKER = "% DBTEX".getBytes();
   public static final byte[] DTLTEX_MARKER = "% DTLTEX".getBytes();

   public static final byte[] V2_MARKER = " 2.0".getBytes();
   public static final byte[] V3_MARKER = " 3.0".getBytes();

   /**
    * Input file contains LaTeX code. Not applicable for output
    * files.
    */
   public static final int FILE_FORMAT_FLAG_TEX = 1;

   /**
    * Input file contains LaTeX code in special DTLTEX format. Parse
    * header to determine version and encoding. Output file 
    * latest DTLTEX version.
    */
   public static final int FILE_FORMAT_FLAG_DTLTEX = 1 << 1;

   /**
    * Input file contains LaTeX code in special DTLTEX format. Parse
    * header to determine encoding. Output file format
    * DTLTEX version 2.0.
    */
   public static final int FILE_FORMAT_FLAG_DTLTEX2 = 1 << 2;

   /**
    * Input file contains LaTeX code in special DTLTEX format. Parse
    * header to determine encoding. Output file format
    * DTLTEX version 3.0.
    */
   public static final int FILE_FORMAT_FLAG_DTLTEX3 = 1 << 3;

   /**
    * Input file contains LaTeX code in DBTEX format. Parse
    * header to determine version and encoding. Output file 
    * latest DBTEX version.
    */
   public static final int FILE_FORMAT_FLAG_DBTEX = 1 << 4;

   /**
    * Input file contains LaTeX code in special DBTEX format. Parse
    * header to determine encoding. Output file format
    * DBTEX version 2.0.
    */
   public static final int FILE_FORMAT_FLAG_DBTEX2 = 1 << 5;

   /**
    * Input file contains LaTeX code in special DBTEX format. Parse
    * header to determine encoding. Output file format
    * DBTEX version 3.0.
    */
   public static final int FILE_FORMAT_FLAG_DBTEX3 = 1 << 6;

   /**
    * CSV file format.
    */
   public static final int FILE_FORMAT_FLAG_CSV = 1 << 7;

   /**
    * TSV file format (tab separator).
    */
   public static final int FILE_FORMAT_FLAG_TSV = 1 << 8;

   /**
    * ODS (zip) file format.
    */
   public static final int FILE_FORMAT_FLAG_ODS = 1 << 9;

   /**
    * FODS (flat xml) file format.
    */
   public static final int FILE_FORMAT_FLAG_FODS = 1 << 10;

   /**
    * XLSX (Excel xml) file format.
    */
   public static final int FILE_FORMAT_FLAG_XLSX = 1 << 11;

   /**
    * XLS (Excel) file format.
    */
   public static final int FILE_FORMAT_FLAG_XLS = 1 << 12;

   /**
    * SQL format.
    */
   public static final int FILE_FORMAT_FLAG_SQL = 1 << 13;

   /**
    * Either CSV or TSV.
    */
   public static final int FILE_FORMAT_CSV_OR_TSV =
     FILE_FORMAT_FLAG_CSV | FILE_FORMAT_FLAG_TSV;

   /**
    * Any DBTEX format.
    */
   public static final int FILE_FORMAT_ANY_DBTEX =
        FILE_FORMAT_FLAG_DBTEX
      | FILE_FORMAT_FLAG_DBTEX2
      | FILE_FORMAT_FLAG_DBTEX3;

   /**
    * Any DTLTEX format.
    */
   public static final int FILE_FORMAT_ANY_DTLTEX =
        FILE_FORMAT_FLAG_DTLTEX
      | FILE_FORMAT_FLAG_DTLTEX2
      | FILE_FORMAT_FLAG_DTLTEX3;

   /**
    * Any TeX format.
    */
   public static final int FILE_FORMAT_ANY_TEX =
     FILE_FORMAT_FLAG_TEX
   | FILE_FORMAT_ANY_DTLTEX
   | FILE_FORMAT_ANY_DBTEX;

   /**
    * Any spreadsheet format.
    */
   public static final int FILE_FORMAT_ANY_SPREADSHEET =
     FILE_FORMAT_FLAG_ODS
   | FILE_FORMAT_FLAG_FODS
   | FILE_FORMAT_FLAG_XLSX
   | FILE_FORMAT_FLAG_XLS;

   /**
    * Any non-TeX format.
    */
   public static final int FILE_FORMAT_ANY_NON_TEX =
     FILE_FORMAT_CSV_OR_TSV
   | FILE_FORMAT_ANY_SPREADSHEET
   | FILE_FORMAT_FLAG_SQL;

}
