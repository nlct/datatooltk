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
package com.dickimawbooks.datatooltk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Vector;
import java.util.List;
import java.util.Random;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.*;
import java.util.Date;
import java.util.Locale;
import java.util.Arrays;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.dickimawbooks.texjavahelplib.InvalidSyntaxException;

import com.dickimawbooks.datatooltk.io.*;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.primitives.Undefined;
import com.dickimawbooks.texparserlib.latex.PreambleParser;
import com.dickimawbooks.texparserlib.latex.datatool.*;
import com.dickimawbooks.texparserlib.latex.inputenc.InputEncSty;

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

      headers = new Vector<DatatoolHeader>(
        cols > 0 ? cols : settings.getInitialColumnCapacity());
      data = new Vector<DatatoolRow>(
        rows > 0 ? rows : settings.getInitialRowCapacity());
   }

   public DatatoolDb(DatatoolSettings settings, int cols)
   {
      if (settings == null)
      {
         throw new NullPointerException();
      }

      this.settings = settings;

      headers = new Vector<DatatoolHeader>(
        cols > 0 ? cols : settings.getInitialColumnCapacity());
      data = new Vector<DatatoolRow>(settings.getInitialRowCapacity());
   }

   public void updateDefaultFormat()
   {
      String defFmt = settings.getDefaultOutputFormat();

      if (defFmt != null)
      {
         Matcher m = FORMAT_PATTERN.matcher(defFmt);

         if (m.matches())
         {
            currentFileFormat = FileFormatType.valueOf(m.group(1));

            currentFileVersion = m.group(2)+".0";
         }
      }
   }

   public FileFormatType getDefaultFormat()
   {
      return currentFileFormat;
   }

   public String getDefaultFileVersion()
   {
      return currentFileVersion;
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
      // using the TeXParser is slower, but it can handle files
      // containing \DTLnewdb etc, so first try the faster method
      // and if that fails try the slower method.

      boolean unsetFile = false;

      IOSettings ioSettings = new IOSettings(null);
      ioSettings.setFileFormat(FileFormatType.DBTEX);
      ioSettings.setFileVersion("2.0");

      try
      {
         return loadNoTeXParser(settings, dbFile, ioSettings);
      }
      catch (InvalidSyntaxException e)
      {
         MessageHandler messageHandler = settings.getMessageHandler();

         messageHandler.progress(messageHandler.getLabelWithValues(
           "progress.quick_load_failed", e.getMessage()));
         messageHandler.debug(e);

         /*
          * It's possible that the user may have accidentally loaded
          * a document file containing \input or \DTLloaddbtex.
          * For simple preambles, the TeX Parser Library may be able
          * to correctly find the database without error. This could
          * mean that if the user then tries to save the database,
          * their document will be overwritten. To guard against
          * this, null the file to force the user to "Save As" so
          * they can select a new file name, unless the file
          * extension is dbtex or dtltex.
          */  

         int idx = dbFile.getName().lastIndexOf(".");
         String ext = "";

         if (idx > -1)
         {
            ext = dbFile.getName().substring(idx+1).toLowerCase();
         }

         if (!ext.equals("dbtex") && !ext.equals("dtltex"))
         {
            unsetFile = true;

            if (!messageHandler.isBatchMode())
            {
               messageHandler.warning(messageHandler.getLabel(
                 "warning.texparser.used"));
            }
         }
      }

      DatatoolDb db = loadTeXParser(settings, dbFile, ioSettings);

      if (unsetFile)
      {
         db.file = null;
      }

      return db;
   }

   public static DatatoolDb loadTeXParser(DatatoolSettings settings, 
     File dbFile, IOSettings ioSettings)
     throws IOException
   {
      DatatoolDb db = null;
      boolean hasVerbatim = false;

      MessageHandler messageHandler = settings.getMessageHandler();
      TeXApp texApp = messageHandler.getTeXApp();

      PreambleParser preambleParser = new PreambleParser(texApp,
        UndefAction.WARN);
      TeXParser texParser = new TeXParser(preambleParser);
      messageHandler.setDebugModeForParser(texParser);

      DataToolSty sty = new DataToolSty(null, preambleParser, false);
      preambleParser.usepackage(sty, texParser);

      ioSettings.setSty(sty);
      FileFormatType format = ioSettings.getFormat();
      String version = ioSettings.getFileVersion();

      texParser.putControlSequence(true,
        new TextualContentCommand(DataToolSty.DEFAULT_EXT,
         format == FileFormatType.DBTEX ? "dbtex" : "dtltex"));

      ioSettings.fetchSettings(true, texParser, texParser);
      ioSettings.setFileFormat(format);
      ioSettings.setFileVersion(version);

      preambleParser.addVerbEnv("lstlisting");
      preambleParser.addVerbEnv("alltt");

      texParser.addVerbCommand("lstinline");

      try
      {
         messageHandler.startBuffering();
         TeXObjectList stack = preambleParser.createStack();

         texParser.startGroup();

         TeXPath texPath = new TeXPath(texParser, dbFile);
         texParser.push(new EndRead());

         DataBase.read(sty, texPath, ioSettings, texParser, texParser);
         texParser.processBuffered();

         String dbName = null;
         DataBase texDb = sty.getLatestDataBase();

         if (texDb != null)
         {
            dbName = texDb.getName();
         }
         else
         {
            ControlSequence cs = texParser.getControlSequence("dtllastloadeddb");

            if (cs != null && cs instanceof Expandable)
            {
               // should be expandable since it's defined in the .dbtex
      
               dbName = texParser.expandToString(cs, null);
               ioSettings.setFileFormat(FileFormatType.DBTEX);
               ioSettings.setFileVersion("2.0");
            }
            else
            {
               ioSettings.setFileFormat(FileFormatType.DTLTEX);

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

            texDb = sty.getDataBase(dbName);
         }
   
         int rowCount = texDb.getRowCount();
         int columnCount = texDb.getColumnCount();

         messageHandler.debug(String.format("%s (%dx%d)", dbName,
          rowCount, columnCount));
   
         db = new DatatoolDb(settings, rowCount, columnCount);
         db.setFile(dbFile);
         db.setName(dbName);

         db.currentFileFormat = ioSettings.getFormat();
         db.currentFileVersion = ioSettings.getFileVersion();
   
         DataToolHeaderRow headerRow = texDb.getHeaders();
   
         for (int i = 0; i < columnCount; i++)
         {
            DataToolHeader header = headerRow.getHeader(i+1);
   
            String key = header.getColumnLabel();
            String title = key;

            TeXObject headerTitle = header.getTitle();

            if (headerTitle != null)
            {
               key = headerTitle.toString(texParser);
            }
   
            DatumType type = header.getDataType();
   
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
               row.add(Datum.createNull(settings));
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
                  if (!hasVerbatim && texParser.isStack(entryContents))
                  {
                     hasVerbatim = preambleParser.containsVerbatim(
                        entryContents);
                  }

                  row.setCell(columnIndex,
                     Datum.valueOf(entryContents, texParser, settings));
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
      
   public static DatatoolDb loadNoTeXParser(DatatoolSettings settings, 
     File dbFile, IOSettings ioSettings)
     throws IOException,InvalidSyntaxException
   {
      MessageHandler messageHandler = settings.getMessageHandler();
      LineNumberReader in = null;
      boolean hasVerbatim = false;
      String encoding = settings.getTeXEncoding();
      Charset charset;
      DatatoolDb db;

      if (encoding == null)
      {
         charset = Charset.defaultCharset();
      }
      else
      {
         try
         {
            charset = Charset.forName(encoding);
         }
         catch (Exception e)
         {
            settings.getMessageHandler().warning(e);
            charset = Charset.defaultCharset();
         }
      }

      try
      {
         in = new LineNumberReader(Files.newBufferedReader(dbFile.toPath(), charset));

         in.mark(256);
         String line = in.readLine();

         Matcher m = FILE_TYPE_MARKER.matcher(line);
         boolean isV3 = false;
         boolean dtltex = false;

         if (m.matches())
         {
            if (m.group(1).equals("DTLTEX"))
            {
               dtltex = true;
            }

            isV3 = (m.group(2).equals("3"));

            Charset fileCharset = null;

            try
            {
               String enc = m.group(3);

               if (InputEncSty.isKnownEncoding(enc))
               {
                  fileCharset = InputEncSty.getCharSet(enc);
               }
               else
               {
                  fileCharset = Charset.forName(enc);
               }

               if (!fileCharset.equals(charset))
               {
                  in.close();
                  in = new LineNumberReader(
                     Files.newBufferedReader(dbFile.toPath(), fileCharset));
                  line = in.readLine();
               }
            }
            catch (Exception e)
            {
               settings.getMessageHandler().warning(e);
            }
         }
         else if (line.startsWith("% Created by datatool"))
         {// assume DBTEX v1.0 or v2.0
            ioSettings.setFileFormat(FileFormatType.DBTEX);
            ioSettings.setFileVersion("2.0");
         }
         else
         {// assume DTLTEX v2.0
            ioSettings.setFileFormat(FileFormatType.DTLTEX);
            ioSettings.setFileVersion("2.0");

            if (!line.startsWith("%"))
            {
               in.reset();
            }
         }

         ioSettings.setFileVersion(isV3 ? "3.0" : "2.0");

         if (dtltex)
         {
            ioSettings.setFileFormat(FileFormatType.DTLTEX);

            if (isV3)
            {
               db = loadNoTeXParserDTLTEX3(settings, in);
            }
            else
            {
               db = loadNoTeXParserDTLTEX2(settings, in);
            }
         }
         else
         {
            ioSettings.setFileFormat(FileFormatType.DBTEX);

            if (isV3)
            {
               db = loadNoTeXParserDBTEX3(settings, in);
            }
            else
            {
               db = loadNoTeXParserDBTEX2(settings, in);
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

      db.setFile(dbFile);
      db.currentFileFormat = ioSettings.getFormat();
      db.currentFileVersion = ioSettings.getFileVersion();

      if (hasVerbatim)
      {
         messageHandler.warning(messageHandler.getLabel("warning.verb_detected"));
      }

      return db;
   }

   /**
    * Loads dtltex v1.3 file.
    */
   protected static DatatoolDb loadNoTeXParserDTLTEX3(DatatoolSettings settings, 
     LineNumberReader in)
     throws IOException,InvalidSyntaxException
   {
      DatatoolDb db = null;
      MessageHandler messageHandler = settings.getMessageHandler();

      String line;
      int lineNum;

      while ((line = in.readLine()) != null && line.startsWith("%"))
      {// discard comment lines
      }

      if (line == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(), "error.expected", "\\DTLdbProvideData"
           ));
      }

      Matcher m = PROVIDE_DATA_PATTERN.matcher(line);

      if (!m.matches())
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(), "error.expected", "\\DTLdbProvideData"
           ));
      }

      db = new DatatoolDb(settings);
      db.setName(m.group(1));

      DatatoolRow row = null;

      while ((line = in.readLine()) != null)
      {
         if (line.startsWith("%")) continue;

         m = NEW_ROW3_PATTERN.matcher(line);

         if (m.matches())
         {
            row = new DatatoolRow(db);
            db.data.add(row);
         }
         else if (line.startsWith("\\DTLdbNewEntry"))
         {
            if (row == null)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                 (
                    in.getLineNumber(), "error.expected", "\\DTLdbNewRow"
                 ));
            }

            m = NEW_ENTRY3_PATTERN.matcher(line);
            lineNum = in.getLineNumber();

            while (!m.matches())
            {
               String l = in.readLine();

               if (l == null)
               {
                  throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
                    lineNum,
                    "error.expected", 
                    "\\DTLdbNewEntry{<key>}{<value>}"
                  ));
               }

               line = String.format("%s%n%s", line, l);
               m = NEW_ENTRY3_PATTERN.matcher(line);
            }

            String colKey = m.group(1);
            String value = m.group(2);

            int colIdx = db.getColumnIndex(colKey);
            DatatoolHeader header;

            if (colIdx == -1)
            {
               header = new DatatoolHeader(db, colKey);
               colIdx = db.headers.size();
               db.headers.add(header);
            }
            else
            {
               header = db.headers.get(colIdx);
            }

            Datum datum = Datum.valueOf(value, settings);
            row.addCell(colIdx, datum);

            if (datum.overrides(header.getDatumType()))
            {
               header.setType(datum.getDatumType());
            }
         }
         else if (line.startsWith("\\DTLdbSetHeader"))
         {
            m = SET_HEADER3_PATTERN.matcher(line);
            lineNum = in.getLineNumber();

            while (!m.matches())
            {
               String l = in.readLine();

               if (l == null)
               {
                  throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
                    lineNum,
                    "error.expected", 
                    "\\DTLdbSetHeader{<key>}{<value>}"
                  ));
               }

               line = String.format("%s%n%s", line, l);
               m = SET_HEADER3_PATTERN.matcher(line);
            }

            String colKey = m.group(1);
            String value = m.group(2);

            DatatoolHeader header = db.getHeader(colKey);

            if (header == null)
            {
               header = new DatatoolHeader(db, colKey, value);
               db.headers.add(header);
            }
            else
            {
               header.setTitle(value);
            }

         }
         else if (row == null)
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected", 
              "\\DTLdbNewRow"
            ));
         }
         else
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected", 
              "\\DTLdbNewEntry{<key>}{<value>}"
            ));
         }
      }

      return db;
   }

   /**
    * Loads dtltex v1.2 file.
    */
   protected static DatatoolDb loadNoTeXParserDTLTEX2(DatatoolSettings settings, 
     LineNumberReader in)
     throws IOException,InvalidSyntaxException
   {
      DatatoolDb db = null;
      MessageHandler messageHandler = settings.getMessageHandler();

      String line;
      int lineNum;

      while ((line = in.readLine()) != null && line.startsWith("%"))
      {// discard comment lines
      }

      if (line == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(), "error.expected", "\\DTLnewdb"
           ));
      }

      Matcher m = NEW_DB_PATTERN.matcher(line);

      if (!m.matches())
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(), "error.expected", "\\DTLnewdb"
           ));
      }

      db = new DatatoolDb(settings);
      db.setName(m.group(1));

      DatatoolRow row = null;

      while ((line = in.readLine()) != null)
      {
         if (line.startsWith("%")) continue;

         m = NEW_ROW2_PATTERN.matcher(line);

         if (m.matches())
         {
            if (!m.group(1).equals(db.getName()))
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                 (
                    in.getLineNumber(), "error.expected_found",
                     String.format("\\DTLnewrow{%s}", db.getName()),
                     line
                 ));
            }

            row = new DatatoolRow(db);
            db.data.add(row);
         }
         else if (line.startsWith("\\DTLnewdbentry"))
         {
            if (row == null)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                 (
                    in.getLineNumber(), "error.expected", "\\DTLnewdbentry"
                 ));
            }

            m = NEW_ENTRY2_PATTERN.matcher(line);
            lineNum = in.getLineNumber();

            while (!m.matches())
            {
               String l = in.readLine();

               if (l == null)
               {
                  throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
                    lineNum,
                    "error.expected", 
                    "\\DTLnewdbentry{<label>}{<key>}{<value>}"
                  ));
               }

               line = String.format("%s%n%s", line, l);
               m = NEW_ENTRY2_PATTERN.matcher(line);
            }

            if (!m.group(1).equals(db.getName()))
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                 (
                    in.getLineNumber(), "error.expected_found",
                     String.format("\\DTLnewdbentry{%s}{<type>}{<value>}", db.getName()),
                     line
                 ));
            }

            String colKey = m.group(2);
            String value = m.group(3);

            int colIdx = db.getColumnIndex(colKey);
            DatatoolHeader header;

            if (colIdx == -1)
            {
               header = new DatatoolHeader(db, colKey);
               colIdx = db.headers.size();
               db.headers.add(header);
            }
            else
            {
               header = db.headers.get(colIdx);
            }

            Datum datum = Datum.valueOf(value, settings);
            row.addCell(colIdx, datum);

            if (datum.overrides(header.getDatumType()))
            {
               header.setType(datum.getDatumType());
            }
         }
         else if (line.startsWith("\\DTLsetheader"))
         {
            m = SET_HEADER2_PATTERN.matcher(line);
            lineNum = in.getLineNumber();

            while (!m.matches())
            {
               String l = in.readLine();

               if (l == null)
               {
                  throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
                    lineNum,
                    "error.expected", 
                    "\\DTLsetheader{<label>}{<key>}{<value>}"
                  ));
               }

               line = String.format("%s%n%s", line, l);
               m = SET_HEADER2_PATTERN.matcher(line);
            }

            if (!m.group(1).equals(db.getName()))
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                 (
                    in.getLineNumber(), "error.expected_found",
                     String.format("\\DTLsetheader{%s}{<type>}{<value>}", db.getName()),
                     line
                 ));
            }

            String colKey = m.group(2);
            String value = m.group(3);

            DatatoolHeader header = db.getHeader(colKey);

            if (header == null)
            {
               header = new DatatoolHeader(db, colKey, value);
               db.headers.add(header);
            }
            else
            {
               header.setTitle(value);
            }

         }
         else if ((m = DEF_LAST_LOADED.matcher(line)).matches())
         {
            if (!m.group(1).equals(db.getName()))
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                 (
                    in.getLineNumber(), "error.expected_found",
                     String.format("\\def\\dtllastloadeddb{%s}", db.getName()),
                     line
                 ));
            }
         }
         else if (row == null)
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected", 
              "\\DTLnewrow"
            ));
         }
         else
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
              in.getLineNumber(),
              "error.expected", 
              "\\DTLnewdbentry{<label>}{<key>}{<value>}"
            ));
         }
      }

      return db;
   }

   /**
    * Loads dbtex v1.3 file.
    */
   protected static DatatoolDb loadNoTeXParserDBTEX3(DatatoolSettings settings, 
     LineNumberReader in)
     throws IOException,InvalidSyntaxException
   {
      DatatoolDb db = null;
      MessageHandler messageHandler = settings.getMessageHandler();

      String line;
      int lineNum;

      while ((line = in.readLine()) != null && line.startsWith("%"))
      {// discard comment lines
      }

      if (line == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(), "error.expected", "\\DTLdbProvideData"
           ));
      }

      Matcher m = PROVIDE_DATA_PATTERN.matcher(line);

      if (!m.matches())
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(), "error.expected", "\\DTLdbProvideData"
           ));
      }

      String dbName = m.group(1);

      while ((line = in.readLine()) != null && line.startsWith("%"))
      {// discard comment lines
      }

      if (!line.startsWith("\\DTLreconstructdatabase"))
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(), "error.expected", "\\DTLreconstructdatabase"
           ));
      }

      m = RECONSTRUCT_PATTERN.matcher(line);
      lineNum = in.getLineNumber();

      while (!m.matches())
      {
         String l = in.readLine();

         if (l == null)
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues
             (
               lineNum, "error.expected",
                "\\DTLreconstructdatabase{<n>}{<m>}"
            ));
         }

         if (l.startsWith("%")) continue;

         int i = line.indexOf("%");

         if (i > -1)
         {
            line = line.substring(0, i);
         }

         line += l;
         m = RECONSTRUCT_PATTERN.matcher(line);
      }

      int numRows = 0;
      int numCols = 0;

      try
      {
         numRows = Integer.parseInt(m.group(1));
         numCols = Integer.parseInt(m.group(2));
      }
      catch (NumberFormatException e)
      {// shouldn't happen
         e.printStackTrace();
      }

      db = new DatatoolDb(settings, numRows, numCols);
      db.setName(dbName);

      for (int i = 0; i < numCols; i++)
      {
         db.headers.add(new DatatoolHeader(db));
      }

      for (int i = 0; i < numRows; i++)
      {
         DatatoolRow row = new DatatoolRow(db,
          numCols > 0 ? numCols : settings.getInitialColumnCapacity());

         db.data.add(row);

         for (int j = 0; j < numCols; j++)
         {
            row.add(Datum.createNull(settings));
         }
      }

      lineNum = in.getLineNumber();
      line = readUntil(in, "{", true);

      if (line == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
          (
            lineNum, "error.expected_after",
             "{",
             String.format("\\DTLreconstructdatabase{%d}{%d}", numRows, numCols)
         ));
      }

      for (int i = 0; i < numCols; i++)
      {
         line = in.readLine();
         m = HEADER_RECONSTRUCT_PATTERN.matcher(line);
         lineNum = in.getLineNumber();

         while (!m.matches())
         {
            String l = in.readLine();

            if (l == null)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                (
                  lineNum, "error.expected",
                   String.format("\\dtldbheaderreconstruct{%d}{<key>}{<type>}{<title>}", i+1)
               ));
            }

            if (l.startsWith("%")) continue;

            int idx = line.indexOf("%");

            if (idx > -1)
            {
               line = line.substring(0, idx);
            }

            line += l;
            m = HEADER_RECONSTRUCT_PATTERN.matcher(line);
         }

         int colIdx = i;
         int typeId = -1;
         String key = m.group(2);
         String title = m.group(4);

         try
         {
            colIdx = Integer.parseInt(m.group(1));

            if (colIdx > numCols || colIdx < 1)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                (
                  lineNum, "error.dbload.invalid_col_id", colIdx
               ));
            }

            colIdx--;
            String typeStr = m.group(3);

            if (!typeStr.isEmpty())
            {
               typeId = Integer.parseInt(typeStr);
            }
         }
         catch (NumberFormatException e)
         {// shouldn't happen
            e.printStackTrace();
         }

         DatatoolHeader header = db.headers.get(colIdx);
         header.setType(DatumType.toDatumType(typeId));
         header.setKey(key);
         header.setTitle(title);
      }

      lineNum = in.getLineNumber();
      line = readUntil(in, "{", true);

      if (line == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
          (
            lineNum, "error.expected_after",
             "{",
             String.format("\\DTLreconstructdatabase{%d}{%d}{<headers>}",
               numRows, numCols)
         ));
      }

      for (int i = 0; i < numRows; i++)
      {
         line = in.readLine();
         m = ROW_RECONSTRUCT_PATTERN.matcher(line);
         lineNum = in.getLineNumber();

         while (!m.matches())
         {
            String l = in.readLine();

            if (l == null)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                (
                  lineNum, "error.expected",
                   String.format("\\dtldbrowreconstruct{%d}", i+1)
               ));
            }

            if (l.startsWith("%")) continue;

            int idx = line.indexOf("%");

            if (idx > -1)
            {
               line = line.substring(0, idx);
            }

            line += l;
            m = ROW_RECONSTRUCT_PATTERN.matcher(line);
         }

         int rowIdx = i;

         try
         {
            rowIdx = Integer.parseInt(m.group(1));

            if (rowIdx > numRows || rowIdx < 1)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                (
                  lineNum, "error.dbload.invalid_row_id", rowIdx
               ));
            }

            rowIdx--;
         }
         catch (NumberFormatException e)
         {// shouldn't happen
            e.printStackTrace();
         }

         lineNum = in.getLineNumber();
         line = readUntil(in, "{", true);

         if (line == null)
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues
             (
               lineNum, "error.expected_after",
                "{",
                String.format("\\dtldbrowreconstruct{%d}", rowIdx+1)
            ));
         }

         DatatoolRow row = db.data.get(rowIdx);

         for (int j = 0; j < numCols; j++)
         {
            line = in.readLine();
            m = COL_RECONSTRUCT_PATTERN.matcher(line);
            lineNum = in.getLineNumber();

            while (!m.matches())
            {
               String l = in.readLine();

               if (l == null)
               {
                  throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                   (
                     lineNum, "error.expected",
                      String.format("\\dtldbcolreconstruct{%d}", j+1)
                  ));
               }

               if (l.startsWith("%")) continue;

               int idx = line.indexOf("%");

               if (idx > -1)
               {
                  line = line.substring(0, idx);
               }

               line += l;
               m = COL_RECONSTRUCT_PATTERN.matcher(line);
            }

            int colIdx = i;

            try
            {
               colIdx = Integer.parseInt(m.group(1));

               if (colIdx > numCols || colIdx < 1)
               {
                  throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                   (
                     lineNum, "error.dbload.invalid_col_id", colIdx
                  ));
               }

               colIdx--;
            }
            catch (NumberFormatException e)
            {// shouldn't happen
               e.printStackTrace();
            }

            lineNum = in.getLineNumber();
            line = readUntil(in, "{", true);

            if (line == null)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                (
                  lineNum, "error.expected_after",
                   "{",
                   String.format("\\dtldbcolreconstruct{%d}", colIdx+1)
               ));
            }

            String cs = db.readCommand(in);

            if (cs == null)
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                (
                  lineNum, "error.expected_or",
                   "\\dtldbvaluereconstruct",
                   "\\dtldbdatumreconstruct"
               ));
            }
            else if (cs.equals("\\dtldbvaluereconstruct"))
            {
               // \\dtldbvaluereconstruct{text}

               String value = db.readValueReconstruct(in);
               row.set(colIdx, Datum.valueOf(value, settings));
            }
            else if (cs.equals("\\dtldbdatumreconstruct"))
            {
               // \\dtldbdatumreconstruct{text}{num}{sym}{type}
               Datum datum = db.readDatumReconstruct(in);
               row.set(colIdx, datum);
            }
            else
            {
               throw new InvalidSyntaxException(messageHandler.getLabelWithValues
                (
                  lineNum, "error.expected_or_found",
                   "\\dtldbvaluereconstruct",
                   "\\dtldbdatumreconstruct",
                   cs
               ));
            }
         }

         lineNum = in.getLineNumber();
         line = readUntil(in, "}", true);

         if (line == null)
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues
             (
               lineNum, "error.expected_after",
                "}",
                String.format("\\dtldbrowreconstruct{%d}{<content>}",
                  rowIdx+1)
            ));
         }
      }

      // Key to index argument can be ignored

      return db;
   }

   private String readValueReconstruct(LineNumberReader in)
    throws IOException
   {
      StringBuilder builder = new StringBuilder();
      Matcher m = readValueReconstruct(in, builder);
      return m.group(1);
   }

   private Matcher readValueReconstruct(LineNumberReader in, StringBuilder builder)
    throws IOException
   {
      MessageHandler messageHandler = settings.getMessageHandler();
      String line = in.readLine();

      if (builder.length() > 0)
      {
         builder.append(String.format("%n"));
      }

      builder.append(line);

      Matcher m = SINGLE_GROUP_PATTERN.matcher(builder.toString());
      int lineNum = in.getLineNumber();

      while (!m.matches())
      {
         String l = in.readLine();

         if (l == null)
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues
             (
               lineNum, "error.expected",
               "\\dtldbvaluereconstruct{<content>}"
            ));
         }

         builder.append(String.format("%n"));
         builder.append(l);
         m = SINGLE_GROUP_PATTERN.matcher(builder.toString());
      }

      lineNum = in.getLineNumber();
      in.mark(255);
      String content = readUntil(in, "}", true);

      if (content == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
          (
            lineNum, "error.expected_after",
             "}",
             String.format("\\dtldbvaluereconstruct{%s}",
               line)
         ));
      }

      if (!content.trim().isEmpty())
      {
         in.reset();
         m = readValueReconstruct(in, builder);
      }

      return m;
   }

   private Datum readDatumReconstruct(LineNumberReader in)
    throws IOException
   {
      StringBuilder builder = new StringBuilder();
      Matcher m = readDatumReconstruct(in, builder);
      String text = m.group(1);
      Number num = null;
      String sym = null;
      int typeId = -1;

      try
      {
         typeId = Integer.parseInt(m.group(4));
      }
      catch (NumberFormatException e)
      {// shouldn't happen
         e.printStackTrace();
      }

      DatumType type = DatumType.toDatumType(typeId);

      if (type.isNumeric())
      {
         String numStr = m.group(2);

         if (type == DatumType.INTEGER)
         {
            num = Integer.valueOf(numStr);
         }
         else
         {
            num = Double.valueOf(numStr);
         }

         if (type == DatumType.CURRENCY)
         {
            sym = m.group(3);
         }
      }

      return new Datum(type, text, sym, num, settings);
   }

   private Matcher readDatumReconstruct(LineNumberReader in, StringBuilder builder)
    throws IOException
   {
      MessageHandler messageHandler = settings.getMessageHandler();
      String line = in.readLine();

      if (builder.length() > 0)
      {
         builder.append(String.format("%n"));
      }

      builder.append(line);

      Matcher m = DATUM_ARGS_PATTERN.matcher(builder.toString());
      int lineNum = in.getLineNumber();

      while (!m.matches())
      {
         String l = in.readLine();

         if (l == null)
         {
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues
             (
               lineNum, "error.expected",
               "\\dtldbdatumreconstruct{<content>}"
            ));
         }

         builder.append(String.format("%n"));
         builder.append(l);
         m = DATUM_ARGS_PATTERN.matcher(builder.toString());
      }

      lineNum = in.getLineNumber();
      in.mark(255);
      String content = readUntil(in, "}", true);

      if (content == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
          (
            lineNum, "error.expected_after",
             "}",
             String.format("\\dtldbdatumreconstruct{%s}",
               line)
         ));
      }

      if (!content.trim().isEmpty())
      {
         in.reset();
         m = readDatumReconstruct(in, builder);
      }

      return m;
   }

   /**
    * Loads dbtex v1.2 file.
    */
   protected static DatatoolDb loadNoTeXParserDBTEX2(DatatoolSettings settings, 
     LineNumberReader in)
     throws IOException,InvalidSyntaxException
   {
      DatatoolDb db = null;
      MessageHandler messageHandler = settings.getMessageHandler();

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
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", "\\newtoks\\csname"));
      }

      String name = readUntil(in, "\\endcsname");

      if (name == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", "\\endcsname"));
      }

      if (!name.startsWith("dtlkeys@"))
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues
           (
              in.getLineNumber(),
              "error.expected",
              "\\newtoks\\csname dtlkeys@<name>\\endcsname"
           ));
      }

      name = name.substring(8);

      db.setName(name);

      // db.name may have been trimmed, but local name
      // shouldn't been to ensure regex match

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
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
             "\\csname dtlkeys@"+name+"\\endcsname"));
      }

      int c = readChar(in, true);

      if (c == -1)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
             "\\csname dtlkeys@"+name+"\\endcsname="));
      }
      else if (c != (int)'=')
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
            in.getLineNumber(),
           "error.expected_found", 
               "\\csname dtlkeys@"+name+"\\endcsname=",
               "\\csname dtlkeys@"+name+"\\endcsname"+((char)c)
            ));
      }

      c = readChar(in, true);

      if (c == -1)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
             "\\csname dtlkeys@"+name+"\\endcsname={"));
      }
      else if (c != (int)'{')
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
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
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues
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
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", "\\newtoks\\csname"));
      }

      String contents = readUntil(in, "\\endcsname");

      if (contents == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
          "\\newtoks\\csname dtldb@"+name+"\\endcsname"));
      }
      else if (!contents.equals("dtldb@"+name))
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           in.getLineNumber(),
           "error.expected_found",
               "\\newtoks\\csname dtldb@"+name+"\\endcsname",
               "\\newtoks\\csname "+contents+"\\endcsname"
           ));
      }

      contents = readUntil(in, "\\csname");

      if (contents == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
          "\\csname dtldb@"+name+"\\endcsname="));
      }

      // skip any whitespace

      c = readChar(in, true);

      contents = readUntil(in, "\\endcsname");

      if (contents == null)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
          "\\csname dtldb@"+name+"\\endcsname="));
      }

      contents = (""+(char)c)+contents;

      if (!contents.equals("dtldb@"+name))
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
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
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
             "\\csname dtldb@"+name+"\\endcsname="));
      }
      else if (c != (int)'=')
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           in.getLineNumber(),
           "error.expected_found", 
               "\\csname dtldb@"+name+"\\endcsname=",
               "\\csname dtldb@"+name+"\\endcsname"+((char)c)
            ));
      }

      c = readChar(in, true);

      if (c == -1)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
           "error.dbload.not_found", 
             "\\csname dtldb@"+name+"\\endcsname={"));
      }
      else if (c != (int)'{')
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
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
            throw new InvalidSyntaxException(messageHandler.getLabelWithValues
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

      return db;
   }

   private int parseRow(LineNumberReader in, int currentRow)
     throws IOException,InvalidSyntaxException
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
            row.set(i, Datum.createNull(settings));
         }
      }
      catch (NumberFormatException e)
      {
         throw new InvalidSyntaxException(getMessageHandler().getLabelWithValues
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
     throws IOException,InvalidSyntaxException
   {
      String controlSequence = readCommand(in);

      if (controlSequence == null)
      {
         throw new InvalidSyntaxException(getMessageHandler().getLabelWithValues
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
               throw new InvalidSyntaxException(
                 getMessageHandler().getLabelWithValues
                 (
                    in.getLineNumber(),
                    "error.dbload.wrong_end_row_tag",
                       currentRow, num
                 ));
            }
         }
         catch (NumberFormatException e)
         {
            throw new InvalidSyntaxException(
              getMessageHandler().getLabelWithValues
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
         throw new InvalidSyntaxException(getMessageHandler().getLabelWithValues
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
         throw new InvalidSyntaxException(getMessageHandler().getLabelWithValues
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
         throw new InvalidSyntaxException(getMessageHandler().getLabelWithValues
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
         throw new InvalidSyntaxException(getMessageHandler().getLabelWithValues
           (
               in.getLineNumber(),
              "error.expected",
                 "\\db@col@elt@end@"
           ));
      }

      // Trim any final %\n

      contents = contents.replaceFirst("([^\\\\](?:\\\\\\\\)*)%\\s*\\z", "$1");

      DatatoolRow row = data.get(currentRow-1);

      row.set(currentColumn-1, Datum.valueOf(contents, getSettings()));

      readCommand(in, "\\db@col@id@w");

      contents = readUntil(in, "\\db@col@id@end@");

      try
      {
         int num = Integer.parseInt(contents);

         if (num != currentColumn)
         {
            throw new InvalidSyntaxException(
              getMessageHandler().getLabelWithValues
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
         throw new InvalidSyntaxException(getMessageHandler().getLabelWithValues
           (
              in.getLineNumber(),
              "error.dbload.invalid_col_id",
              contents
           ), e);
      }

      parseEntry(in, currentRow);
   }

   private int parseHeader(LineNumberReader in, int currentColumn)
     throws IOException,InvalidSyntaxException
   {
      String controlSequence = readCommand(in);

      if (controlSequence == null)
      {
         throw new InvalidSyntaxException(
            getMessageHandler().getLabelWithValues(
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
            throw new InvalidSyntaxException(
               getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@col@id@end@"));
         }

         try
         {
            currentColumn = Integer.parseInt(content);
         }
         catch (NumberFormatException e)
         {
             throw new InvalidSyntaxException(
             getMessageHandler().getLabelWithValues
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
            throw new InvalidSyntaxException(
               getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@key@id@end@"));
         }

         // Get the header for the current column and set this key

         if (headers.size() < currentColumn)
         {
            throw new InvalidSyntaxException(
             getMessageHandler().getLabelWithValues
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
            throw new InvalidSyntaxException(
               getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@header@id@end@"));
         }

         // Get the header for the current column and set this title

         if (headers.size() < currentColumn)
         {
            throw new InvalidSyntaxException(
             getMessageHandler().getLabelWithValues
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
            throw new InvalidSyntaxException(
               getMessageHandler().getLabelWithValues(
               "error.dbload.not_found", "\\db@type@id@end@"));
         }

         int typeId = DatatoolSettings.TYPE_UNKNOWN;

         try
         {
            if (!content.isEmpty())
            {
               typeId = Integer.parseInt(content);
            }
 
            // Get the header for the current column and set this title

            if (headers.size() < currentColumn)
            {
               throw new InvalidSyntaxException(
                getMessageHandler().getLabelWithValues
                (
                   "error.db.load.expected_found",
                      ""+in.getLineNumber(),
                      "\\db@col@id@w",
                   "   \\db@header@id@w"
                ));
            }

            DatatoolHeader header = headers.get(currentColumn-1);

            DatumType type = DatumType.toDatumType(typeId);

            if (type == null)
            {
               throw new InvalidSyntaxException(
                  getMessageHandler().getLabelWithValues(
                    in.getLineNumber(),
                    "error.dbload_unknown_type", typeId));
            }

            header.setType(type);
         }
         catch (NumberFormatException e)
         {
             throw new InvalidSyntaxException(
             getMessageHandler().getLabelWithValues
             (
                in.getLineNumber(),
                "error.dbload_unknown_type",
                content
             ), e);
         }
         catch (IllegalArgumentException e)
         {
             throw new InvalidSyntaxException(
             getMessageHandler().getLabelWithValues
             (
                in.getLineNumber(),
                "error.dbload_unknown_type",
                content
             ), e);
         }

      }

      return parseHeader(in, currentColumn);
   }

   /** Read in next character ignoring comments and optionally
       whitespace.
    */
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


   /** Search for next control word.
      Returns the first command it encounters, skipping anything
      that comes before it.
    */
   private void readCommand(LineNumberReader in, String requiredCommand)
     throws IOException,InvalidSyntaxException
   {
      String controlSequence = readCommand(in);

      if (controlSequence == null)
      {
         throw new InvalidSyntaxException(
           getMessageHandler().getLabelWithValues(
           "error.dbload.not_found", 
             requiredCommand));
      }
      else if (!requiredCommand.equals(controlSequence))
      {
         throw new InvalidSyntaxException(
            getMessageHandler().getLabelWithValues(
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

   /**
     Check if the given value contains known verbatim commands or environments.
    */
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
      save(null, null, currentFileFormat, currentFileVersion);
   }

   public void save(File file,
     FileFormatType fileFormat, String fileVersion)
     throws IOException
   {
      setFile(file);
      save(null, null, fileFormat, fileVersion);
   }

   public void save(String filename)
     throws IOException
   {
      setFile(filename);
      save(null, null, currentFileFormat, currentFileVersion);
   }

   public void save(String filename,
     FileFormatType fileFormat, String fileVersion)
     throws IOException
   {
      setFile(filename);
      save(null, null, fileFormat, fileVersion);
   }

   public void save(String filename, int[] columnIndexes, int[] rowIndexes)
     throws IOException
   {
      setFile(filename);
      save(columnIndexes, rowIndexes);
   }

   public void save(String filename, int[] columnIndexes, int[] rowIndexes,
     FileFormatType fileFormat, String fileVersion)
     throws IOException
   {
      setFile(filename);
      save(columnIndexes, rowIndexes, fileFormat, fileVersion);
   }

   public void save()
     throws IOException
   {
      save(null, null, currentFileFormat, currentFileVersion);
   }

   public void save(FileFormatType fileFormat, String fileVersion)
     throws IOException
   {
      save(null, null, fileFormat, fileVersion);
   }

   public void save(int[] columnIndexes, int[] rowIndexes)
     throws IOException
   {
      save(columnIndexes, rowIndexes, currentFileFormat, currentFileVersion);
   }

   public void save(int[] columnIndexes, int[] rowIndexes,
        FileFormatType fileFormat, String fileVersion)
     throws IOException
   {
      // in case name hasn't been set:
      name = getName();

      switch (fileFormat)
      {
         case DBTEX:
           if ("3.0".equals(fileVersion))
           {
              saveDBTEX3(columnIndexes, rowIndexes);
           }
           else
           {
              saveDBTEX2(columnIndexes, rowIndexes);
           }
         break;
         case DTLTEX:
           saveDTLTEX(fileVersion, columnIndexes, rowIndexes);
         break;
         default:
            throw new IllegalArgumentException("Use export for format "+fileFormat);
      }
   }

   public void saveDBTEX2(int[] columnIndexes, int[] rowIndexes)
     throws IOException
   {
      // DBTEX v2.0

      PrintWriter out = null;

      String encoding = settings.getTeXEncoding();

      MessageHandler messageHandler = getMessageHandler();

      messageHandler.checkOutputFileName(file, false);

      try
      {
         Charset charset;

         if (encoding == null)
         {
            charset = Charset.defaultCharset();
         }
         else
         {
            charset = Charset.forName(encoding);
         }

         out = new PrintWriter(Files.newBufferedWriter(file.toPath(), charset,
           StandardOpenOption.CREATE));

         out.format("%% DBTEX 2.0 %s%n", charset.name());
         out.print("% ");
         out.println(messageHandler.getLabelWithValues("default.texheader",
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

         messageHandler.progress(0);
         int maxProgress = 2*headers.size()+data.size();
         int progress=0;

         for (int i = 0, n = headers.size(); i < n; i++)
         {
            DatatoolHeader header = headers.get(i);

            int colIdx = (columnIndexes == null ? i : columnIndexes[i])
                       + 1;

            DatumType type = header.getDatumType();

            out.println("% header block for column "+colIdx);
            out.println("\\db@plist@elt@w %");
            out.println("\\db@col@id@w "+colIdx+"%");
            out.println("\\db@col@id@end@ %");
            out.println("\\db@key@id@w "+header.getKey()+"%");
            out.println("\\db@key@id@end@ %");
            out.println("\\db@type@id@w "
               +(type==DatumType.UNKNOWN?"":type.getValue())+"%");
            out.println("\\db@type@id@end@ %");
            out.println("\\db@header@id@w "+header.getTitle().trim()+"%");
            out.println("\\db@header@id@end@ %");
            out.println("\\db@col@id@w "+colIdx+"%");
            out.println("\\db@col@id@end@ %");
            out.println("\\db@plist@elt@end@ %");

            messageHandler.progress((100*(++progress))/maxProgress);
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
               Datum cell = row.get(j);

               if (!cell.isNull())
               {
                  int colIdx = (columnIndexes == null ? j : columnIndexes[j])
                          + 1;

                  out.println("% Column "+colIdx);
                  out.println("\\db@col@id@w "+colIdx+"%");
                  out.println("\\db@col@id@end@ %");

                  // Leading spaces will always be ignored
                  // since they immediately follow a control word
                  // (\db@col@id@end@). For consistency, also
                  // trim trailing spaces. Any intentional 
                  // leading/trailing spaces must be identified
                  // using a command (e.g. \space)

                  String text = cell.getText();
                  out.println("\\db@col@elt@w "+text.trim()+"%");
                  out.println("\\db@col@elt@end@ %");

                  out.println("\\db@col@id@w "+colIdx+"%");
                  out.println("\\db@col@id@end@ %");
               }
            }

            out.println("% End of row "+rowIdx);
            out.println("\\db@row@id@w "+rowIdx+"%");
            out.println("\\db@row@id@end@ %");
            out.println("\\db@row@elt@end@ %");

            messageHandler.progress((100*(++progress))/maxProgress);
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

            messageHandler.progress((100*(++progress))/maxProgress);
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

   public void saveDBTEX3(int[] columnIndexes, int[] rowIndexes)
     throws IOException
   {
      // DBTEX v3.0

      int numCols, numRows;

      if (rowIndexes == null)
      {
         numRows = getRowCount();
      }
      else
      {
         numRows = rowIndexes.length;
      }

      if (columnIndexes == null)
      {
         numCols = getColumnCount();
      }
      else
      {
         numCols = columnIndexes.length;
      }

      PrintWriter out = null;

      String encoding = settings.getTeXEncoding();

      MessageHandler messageHandler = getMessageHandler();

      messageHandler.checkOutputFileName(file, false);

      messageHandler.progress(0);
      int maxProgress = 2 * numCols + numRows;
      int progress=0;

      try
      {
         Charset charset;

         if (encoding == null)
         {
            charset = Charset.defaultCharset();
         }
         else
         {
            charset = Charset.forName(encoding);
         }

         out = new PrintWriter(Files.newBufferedWriter(file.toPath(), charset,
           StandardOpenOption.CREATE));

         out.print("% DBTEX 3.0 ");
         out.println(charset.name());
         out.print("% ");
         out.println(messageHandler.getLabelWithValues("default.texheader",
           DatatoolTk.APP_NAME, new Date()));

         out.format("\\DTLdbProvideData{%s}%%%n", name);
         out.println("\\DTLreconstructdatabase");
         out.format((Locale)null, "{%d}{%d}%n", numRows, numCols);
         out.println("{% Header");

         for (int i = 0; i < numCols; i++)
         {
            int colNum = i+1;
            DatatoolHeader header;

            if (columnIndexes == null)
            {
               header = headers.get(i);
            }
            else
            {
               header = headers.get(columnIndexes[i]);
            }

            out.format((Locale)null,
              "\\dtldbheaderreconstruct{%d}{%s}{%d}{%s}%%%n",
              colNum, header.getKey(), header.getDatumType().getValue(),
              header.getTitle());

            messageHandler.progress((100*(++progress))/maxProgress);
         }

         out.println("}% End of Header");

         out.println("{% Content");

         for (int i = 0; i < numRows; i++)
         {
            int rowNum = i+1;
            DatatoolRow row;

            if (rowIndexes == null)
            {
               row = data.get(i);
            }
            else
            {
               row = data.get(rowIndexes[i]);
            }

            out.format("%% Row %d%n", rowNum);

            out.format((Locale)null, "\\dtldbrowreconstruct{%d}%%%n", rowNum);

            out.format("{%% Row %d Content%n", rowNum);

            for (int j = 0; j < numCols; j++)
            {
               int colNum = j+1;
               DatatoolHeader header;
               Datum datum;

               if (columnIndexes == null)
               {
                  header = headers.get(j);
                  datum = row.get(j);
               }
               else
               {
                  header = headers.get(columnIndexes[j]);
                  datum = row.get(columnIndexes[j]);
               }

               if (!datum.isNull())
               {
                  out.format((Locale)null,
                   "  \\dtldbcolreconstruct{%d}%% Column %d%n", colNum, colNum);
                  out.format(" {%% Column %d Content%n", colNum);

                  out.print("   ");
                  out.println(settings.getDbTeX3Cell(datum, header));

                  out.format(" }%% End of column %d Content%n", colNum);
               }
            }

            out.format("}%% End of row %d content%n", rowNum);
            messageHandler.progress((100*(++progress))/maxProgress);
         }

         out.println("}% End of Content");

         out.println("{% Key to index");

         for (int i = 0; i < numCols; i++)
         {
            DatatoolHeader header;

            if (columnIndexes == null)
            {
               header = headers.get(i);
            }
            else
            {
               header = headers.get(columnIndexes[i]);
            }

            out.format((Locale)null, "\\dtldbreconstructkeyindex{%s}{%d}%%%n",
              header.getKey(), header.getDatumType().getValue());

            messageHandler.progress((100*(++progress))/maxProgress);
         }

         out.println("}% End of key to index");
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

   public void saveDTLTEX(String version, int[] columnIndexes, int[] rowIndexes)
     throws IOException
   {
      boolean isV3 = "3.0".equals(version);

      int numCols, numRows;

      if (rowIndexes == null)
      {
         numRows = getRowCount();
      }
      else
      {
         numRows = rowIndexes.length;
      }

      if (columnIndexes == null)
      {
         numCols = getColumnCount();
      }
      else
      {
         numCols = columnIndexes.length;
      }

      PrintWriter out = null;

      String encoding = settings.getTeXEncoding();

      MessageHandler messageHandler = getMessageHandler();

      messageHandler.checkOutputFileName(file, false);

      messageHandler.progress(0);
      int maxProgress = numCols + numRows;
      int progress=0;

      try
      {
         Charset charset;

         if (encoding == null)
         {
            charset = Charset.defaultCharset();
         }
         else
         {
            charset = Charset.forName(encoding);
         }

         out = new PrintWriter(Files.newBufferedWriter(file.toPath(), charset,
           StandardOpenOption.CREATE));

         out.format("%% DTLTEX %s %s%n", version, charset.name());
         out.print("% ");
         out.println(messageHandler.getLabelWithValues("default.texheader",
           DatatoolTk.APP_NAME, new Date()));

         name = getName();

         if (isV3)
         {
            out.print("\\DTLdbProvideData");
         }
         else
         {
            out.print("\\DTLnewdb");
         }

         out.print(name);
         out.println("%");

         for (int i = 0; i < numRows; i++)
         {
            DatatoolRow row;

            if (rowIndexes == null)
            {
               row = data.get(i);
            }
            else
            {
               row = data.get(rowIndexes[i]);
            }

            if (isV3)
            {
               out.println("\\DTLdbNewRow");
            }
            else
            {
               out.format("\\DTLnewrow{%s}%%%n", name);
            }

            for (int j = 0; j < numCols; j++)
            {
               DatatoolHeader header;
               Datum datum;

               if (columnIndexes == null)
               {
                  header = headers.get(j);
                  datum = row.get(j);
               }
               else
               {
                  header = headers.get(columnIndexes[j]);
                  datum = row.get(columnIndexes[j]);
               }

               if (!datum.isNull())
               {
                  if (isV3)
                  {
                     out.print("\\DTLdbNewEntry");
                  }
                  else
                  {
                     out.format("\\DTLnewrow{%s}", name);
                  }

                  out.format("{%s}{", header.getKey());
                  out.print(datum.getText());
                  out.println("}%");
               }
            }

            messageHandler.progress((100*(++progress))/maxProgress);
         }

         for (int i = 0; i < numCols; i++)
         {
            DatatoolHeader header;

            if (columnIndexes == null)
            {
               header = headers.get(i);
            }
            else
            {
               header = headers.get(columnIndexes[i]);
            }

            if (isV3)
            {
               out.print("\\DTLdbSetHeader");
            }
            else
            {
               out.format("\\DTLsetheader{%s}", name);
            }

            out.format("{%s}{", header.getKey());
            out.print(header.getTitle());
            out.println("}%");

            messageHandler.progress((100*(++progress))/maxProgress);
         }

         if (!isV3)
         {
            out.format("\\def\\dtllastloadeddb{%s}%%%n", name);
         }
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
      if (settings.isAutoTrimLabelsOn())
      {
         this.name = name.trim();
      }
      else
      {
         this.name = name;
      }
   }

   public String getName()
   {
      // name may not have been set, in which case return default
      // (no trimming applied to default value)

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

   public DatumType getColumnDatumType(int colIdx)
   {
      return headers.get(colIdx).getDatumType();
   }

   @Deprecated
   public int getColumnType(int colIdx)
   {
      return headers.get(colIdx).getType();
   }

   public DatumType getDataType(int colIdx, Datum value)
   {
      DatatoolHeader header = headers.get(colIdx);

      DatumType type = value.getDatumType();

      if (type == DatumType.UNKNOWN)
      {
         return type;
      }

      switch (header.getDatumType())
      {
         case UNKNOWN:
         case INTEGER:
            // All other types override unknown and int
            return type;
         case CURRENCY:
            // string overrides currency

            if (type == DatumType.STRING)
            {
               return type;
            }
         break;
         case DECIMAL:
            // string and currency override real
            if (type == DatumType.STRING
             || type == DatumType.CURRENCY)
            {
               return type;
            }
         break;
         // nothing overrides string
      }

      return header.getDatumType();
   }

   @Deprecated
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

   @Deprecated
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
      setValue(rowIdx, colIdx, Datum.valueOf(value, getSettings()));
   }

   public void setValue(int rowIdx, int colIdx, Datum datum)
   {
      data.get(rowIdx).setCell(colIdx, datum);
      DatatoolHeader header = headers.get(colIdx);

      DatumType type = getDataType(colIdx, datum);

      if (type != DatumType.UNKNOWN)
      { 
         headers.get(colIdx).setType(type);
      }
   }

   public Datum getDatum(int rowIdx, int colIdx)
   {
      DatatoolRow row = getRow(rowIdx);
      return row.get(colIdx);
   }

   @Deprecated
   public Object getValue(int rowIdx, int colIdx)
   {
      DatatoolRow row = getRow(rowIdx);

      String value = row.get(colIdx).toString();

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

   public void removeColumns(int[] indexes)
   {
      Arrays.parallelSort(indexes);

      for (int i = indexes.length-1; i >= 0; i--)
      {
         headers.remove(indexes[i]);
      }

      for (DatatoolRow row : data)
      {
         for (int i = indexes.length-1; i >= 0; i--)
         {
            row.remove(indexes[i]);
         }
      }
   }

   public void removeColumns(List<Integer> indexes)
   {
      Collections.sort(indexes);

      for (int i = indexes.size()-1; i >= 0; i--)
      {
         headers.remove(indexes.get(i));
      }

      for (DatatoolRow row : data)
      {
         for (int i = indexes.size()-1; i >= 0; i--)
         {
            row.remove(indexes.get(i));
         }
      }
   }

   public void removeColumns(String removeColumnList)
     throws InvalidSyntaxException
   {
      removeColumns(toColumnIndexes(removeColumnList));
   }

   public void removeExceptColumns(String keepColumnList)
     throws InvalidSyntaxException
   {
      Vector<Integer> keepIndexes = toColumnIndexes(keepColumnList);

      int n = getColumnCount();

      Vector<Integer> indexes = new Vector<Integer>(n);

      for (int i = 0; i < n; i++)
      {
         Integer val = Integer.valueOf(i);

         if (!keepIndexes.contains(val))
         {
            indexes.add(val);
         }
      }

      removeColumns(indexes);
   }

   public Vector<Integer> toColumnIndexes(String columnList)
     throws InvalidSyntaxException
   {
      String[] split = columnList.split(",");

      Vector<Integer> indexList = new Vector<Integer>(getColumnCount());

      // has a list of integers or integer ranges been
      // supplied?

      try
      {
         for (String elem : split)
         {
            Matcher m = INT_RANGE_PATTERN.matcher(elem);

            if (m.matches())
            {
               int startRange = 1;
               int endRange = getColumnCount();

               String startGroup = m.group("start");
               String endGroup = m.group("end");

               if (startGroup != null && !startGroup.isEmpty())
               {
                  startRange = Integer.parseInt(startGroup);
               }

               if (endGroup != null && !endGroup.isEmpty())
               {
                  endRange = Integer.parseInt(endGroup);
               }

               if (endRange < startRange)
               {
                  throw new InvalidSyntaxException(
                    getMessageHandler().getLabelWithValues(
                       "error.syntax.invalid_end_range",
                       endRange, startRange));
               }

               for (int i = startRange; i <= endRange; i++)
               {
                  Integer val = Integer.valueOf(i)-1;

                  if (val < 0 || val > getColumnCount())
                  {
                     throw new InvalidSyntaxException(
                       getMessageHandler().getLabelWithValues(
                          "error.dbload.invalid_col_id", val));
                  }

                  if (!indexList.contains(val))
                  {
                     indexList.add(val);
                  }
               }
            }
            else
            {
               Integer val = Integer.parseInt(elem)-1;

               if (val < 0 || val > getColumnCount())
               {
                  throw new InvalidSyntaxException(
                    getMessageHandler().getLabelWithValues(
                       "error.dbload.invalid_col_id", val));
               }

               if (!indexList.contains(val))
               {
                  indexList.add(val);
               }
            }
         }
      }
      catch (NumberFormatException e)
      {
         // list of labels

         for (String elem : split)
         {
            int index = -1;

            for (int i = 0, n = getColumnCount(); i < n; i++)
            {
               DatatoolHeader header = headers.get(i);

               if (elem.equals(header.getKey()))
               {
                  index = i;
               }
            }

            if (index == -1)
            {
               throw new InvalidSyntaxException(
                  getMessageHandler().getLabelWithValues(
                     "error.unknown_key", elem));
            }

            Integer val = Integer.valueOf(index);

            if (!indexList.contains(val))
            {
               indexList.add(val);
            }
         }
      }

      return indexList;
   }

   public DatatoolRow insertRow(int rowIdx)
   {
      DatatoolRow row = new DatatoolRow(this, headers.size());

      for (int i = 0; i < headers.size(); i++)
      {
         row.add(new Datum(getSettings()));
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
            row.add(new Datum(getSettings()));
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
         DatumType type = getDataType(colIdx, row.get(colIdx));

         if (type != DatumType.UNKNOWN)
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
         DatumType type = getDataType(colIdx, newRow.get(colIdx));

         if (type != DatumType.UNKNOWN)
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
               row.add(new Datum(getSettings()));
            }
         }

         addColumn(header);
      }
      else
      {
         headers.add(colIdx, header);

         for (DatatoolRow row : data)
         {
            row.add(colIdx, new Datum(getSettings()));
         }
      }

      return header;
   }

   public void addColumn(DatatoolHeader header)
   {
      headers.add(header);

      for (DatatoolRow row : data)
      {
         row.add(new Datum(getSettings()));
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
         Datum value = row.remove(fromIndex);
         row.add(toIndex, value);
      }
   }

   public ColumnEnumeration getColumnEnumeration(int colIdx)
   {
      return new ColumnEnumeration(data, colIdx);
   }

   @Deprecated
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
         Datum dbValue = dbRow.get(dbColIdx);

         DatatoolRow thisRow = null;

         for (DatatoolRow row : data)
         {
            Datum value = row.get(colIdx);

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
               thisRow.add(new Datum(getSettings()));
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
   private FileFormatType currentFileFormat = FileFormatType.DBTEX;
   private String currentFileVersion = "3.0";

   public static final Pattern FORMAT_PATTERN =
      Pattern.compile("(DBTEX|DTLTEX)[\\s\\-]*([23])(?:\\.0)?");

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

   public static final Pattern INT_RANGE_PATTERN =
     Pattern.compile("(?<start>\\d*)-(?<end>\\d*)");

   public static final Pattern PATTERN_PARAGRAPH =
     Pattern.compile("(^[ \t]*[\n\r])+", Pattern.MULTILINE);

   public static final Pattern INVALID_LABEL_CONTENT =
     Pattern.compile("([#^_\\&%{}~]|\\\\(?:[a-zA-Z]+\\s*)|(?:\\\\[^a-zA-Z]))");

   public static final Pattern FILE_TYPE_MARKER =
     Pattern.compile("% (DBTEX|DTLTEX) ([2,3])\\.0 (.*)");

   public static final Pattern PROVIDE_DATA_PATTERN =
     Pattern.compile("\\\\DTLdbProvideData\\s*\\{(.*?)\\}(\\s*%.*)?");

   public static final Pattern NEW_ROW3_PATTERN =
     Pattern.compile("\\\\DTLdbNewRow\\s*(\\s%.*)?");
   public static final Pattern NEW_ENTRY3_PATTERN =
     Pattern.compile("\\\\DTLdbNewEntry\\s*\\{(.*?)\\}\\s*\\{(.*)\\}(\\s*%.*)?");
   public static final Pattern SET_HEADER3_PATTERN =
     Pattern.compile("\\\\DTLdbSetHeader\\s*\\{(.*?)\\}\\s*\\{(.*)\\}(\\s*%.*)?");

   public static final Pattern NEW_DB_PATTERN =
     Pattern.compile("\\\\DTLnewdb\\s*\\{(.*?)\\}(\\s*%.*)?");

   public static final Pattern NEW_ROW2_PATTERN =
     Pattern.compile("\\\\DTLnewrow\\s*\\{(.*?)\\}(\\s*%.*)?");
   public static final Pattern NEW_ENTRY2_PATTERN =
     Pattern.compile("\\\\DTLnewdbentry\\s*\\{(.*?)\\}\\s*\\{(.*?)\\}\\s*\\{(.*)\\}(\\s*%.*)?");
   public static final Pattern SET_HEADER2_PATTERN =
     Pattern.compile("\\\\DTLsetheader\\s*\\{(.*?)\\}\\s*\\{(.*?)\\}\\s*\\{(.*)\\}(\\s*%.*)?");

   public static final Pattern DEF_LAST_LOADED =
     Pattern.compile("\\\\def\\s*\\\\dtllastloadeddb\\s*\\{(.*)\\}(\\s*%.*)?");

   public static final Pattern RECONSTRUCT_PATTERN =
     Pattern.compile("\\\\DTLreconstructdatabase\\s*\\{(\\d+)\\}\\s*\\{(\\d+)\\}(\\s*%.*)?");

   public static final Pattern HEADER_RECONSTRUCT_PATTERN =
     Pattern.compile("\\s*\\\\dtldbheaderreconstruct\\s*\\{(\\d+)\\}\\s*\\{(.*?)\\}\\s*\\{(-1|[0-3]?)\\}\\{(.*)\\}(\\s*%.*)?");
   public static final Pattern ROW_RECONSTRUCT_PATTERN =
     Pattern.compile("\\s*\\\\dtldbrowreconstruct\\s*\\{(\\d+)\\}(\\s*%.*)?");
   public static final Pattern COL_RECONSTRUCT_PATTERN =
     Pattern.compile("\\s*\\\\dtldbcolreconstruct\\s*\\{(\\d+)\\}(\\s*%.*)?");

   public static final Pattern SINGLE_GROUP_PATTERN =
     Pattern.compile("\\s*\\{(.*)\\}(\\s*%.*)?");
   public static final Pattern DATUM_ARGS_PATTERN =
     Pattern.compile("\\s*\\{(.*)\\}\\s*\\{([+\\-]?\\d*(?:\\.\\d+)?(?:Ee[+\\-]?\\d+)?)\\}\\s*\\{(.*?)\\}\\s*\\{(-1|[0-3]?)\\}(\\s*%.*)?");
}
