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
package com.dickimawbooks.datatooltk.io;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.PreambleParser;
import com.dickimawbooks.texparserlib.latex.probsoln.*;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.datatooltk.*;

/**
 * Class to import data from a probsoln.sty database.
 */
public class DatatoolProbSoln implements DatatoolImport
{
   public DatatoolProbSoln(DatatoolSettings settings)
   {
      this.settings = settings;
      this.importSettings = settings.getImportSettings();
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   @Override
   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(settings.getImportSettings(), source);
   }

   @Override
   public DatatoolDb importData(ImportSettings importSettings, String source)
      throws DatatoolImportException
   {
       this.importSettings = importSettings;

       File file = new File(source);

       if (!file.exists())
       {
          throw new DatatoolImportException(
             getMessageHandler().getLabelWithValues("error.io.file_not_found",
             file.getAbsolutePath()));
       }

       DatatoolDb db = new DatatoolDb(settings);

       String name = file.getName();

       int index = name.lastIndexOf(".");

       if (index > -1)
       {
          name = name.substring(0, index);
       }

       db.setName(name);

       try
       {
          parseData(file, db);
       }
       catch (IOException e)
       {
          throw new DatatoolImportException(e);
       }

       return db;
    }

   protected void parseData(File file, DatatoolDb db) throws IOException
   {
      MessageHandler messageHandler = getMessageHandler();
      TeXApp texApp = messageHandler.getTeXApp();

      boolean hasVerbatim = false;

      PreambleParser preambleParser = new PreambleParser(texApp);
      TeXParser texParser = new TeXParser(preambleParser);
      messageHandler.setDebugModeForParser(texParser);

      ProbSolnSty probSolnSty = new ProbSolnSty(
        settings.getInitialRowCapacity(), true, 
        null, preambleParser, false);

      preambleParser.usepackage(probSolnSty, texParser);

      texParser.parse(file);

      int numDataSets = probSolnSty.getDatabaseCount();

      String key = messageHandler.getLabel("probsoln.label");
      db.addColumn(new DatatoolHeader(db, key, key, DatumType.STRING));

      if (numDataSets > 1)
      {
         key = messageHandler.getLabel("probsoln.set");
         db.addColumn(new DatatoolHeader(db, key, key, DatumType.STRING));
      }

      key = messageHandler.getLabel("probsoln.question");
      db.addColumn(new DatatoolHeader(db, key, key, DatumType.STRING));

      key = messageHandler.getLabel("probsoln.answer");
      db.addColumn(new DatatoolHeader(db, key, key, DatumType.STRING));

      Iterator<ProbSolnData> allDataIt = probSolnSty.allEntriesIterator();

      texApp.progress(0);
      int maxRows = probSolnSty.getTotalProblemCount();

      for (int rowIdx = 0; allDataIt.hasNext(); rowIdx++)
      {
         ProbSolnData data = allDataIt.next();
       
         String probLabel = data.getName();
         String dbLabel = data.getDataBaseLabel();

         DatatoolRow row = new DatatoolRow(db, numDataSets > 1 ? 4 : 3);
         int colIdx = 0;

         row.addCell(colIdx++, probLabel);

         if (numDataSets > 1)
         {
            row.addCell(colIdx++, dbLabel);
         }

         TeXObject question = data.getQuestion(texParser);
         TeXObject answer = data.getAnswer(texParser,
           importSettings.isStripSolutionEnvOn());

         boolean hasVerb = checkElement(question);

         if (hasVerb)
         {
            hasVerbatim = true;
         }

         hasVerb = checkElement(answer);

         if (hasVerb)
         {
            hasVerbatim = true;
         }

         String questionText = question.toString(texParser);
         String answerText = answer.toString(texParser);

         row.addCell(colIdx++, questionText);

         if (answerText.equals(questionText))
         {
            row.addCell(colIdx++, "");
         }
         else
         {
            row.addCell(colIdx++, answerText);
         }

         db.insertRow(rowIdx, row);

         texApp.progress((100*rowIdx)/maxRows);
      }

      if (hasVerbatim)
      {
         getMessageHandler().warning(
            getMessageHandler().getLabel("warning.verb_detected"));
      }
   }

   private boolean checkElement(TeXObject object) throws IOException
   {
      if (!(object instanceof TeXObjectList))
      {
         return false;
      }

      TeXObjectList list = (TeXObjectList)object;

      TeXObject element = list.peek();

      while (element != null && 
         (element instanceof Ignoreable
          || element instanceof Eol))
      {
         if (element instanceof Comment)
         {
            String commentText = ((Comment)element).getText().trim();

            if (commentText.isEmpty())
            {
               list.pop();
            }
         }
         else if (element instanceof Eol
           || element instanceof SkippedEols)
         {
            list.pop();
         }
         else
         {
            break;
         }

         element = list.peek();
      }

      element = list.peekLast();

      while (element != null && element instanceof Comment)
      {
         String commentText = ((Comment)element).getText().trim();

         if (commentText.isEmpty())
         {
            list.remove(list.size()-1);
         }

         element = list.peekLast();
      }

      if (list.isEmpty())
      {
         return false;
      }

      boolean hasVerbatim = false;
      TeXCsRef prev = null;

      for (int i = 0; i < list.size(); i++)
      {
         element = list.get(i);

         if (element instanceof ControlSequence)
         {
            String name = ((ControlSequence)element).getName();

            if (name.equals("par"))
            {
               if (i == 0 || prev != null)
               {
                  list.remove(i);
                  i--;
               }
               else
               {
                  prev = new TeXCsRef("DTLpar");
                  list.set(i, prev);
               }
            }
            else if (importSettings.isCheckForVerbatimOn())
            {
               if (name.equals("verb") || name.equals("lstinline"))
               {
                  prev = null;
                  hasVerbatim = true;
               }
               else if (name.equals("begin"))
               {
                  prev = null;
                  TeXObject nextObj = list.get(++i);

                  while (nextObj instanceof Ignoreable)
                  {
                     nextObj = list.get(++i);
                  }

                  if (nextObj instanceof Group)
                  {
                     nextObj = ((Group)nextObj).toList();
                  }

                  name = nextObj.format().toLowerCase();

                  if (name.endsWith("*"))
                  {
                     name = name.substring(0, name.length()-1);
                  }

                  if (name.equals("verbatim") || name.equals("lstlisting"))
                  {
                     hasVerbatim = true;
                  }
               }
            }
         }
         else if (element instanceof SkippedEols
               || element instanceof Eol)
         {
            if (i > 0)
            {
               TeXObject obj = list.get(i-1);

               if (obj instanceof Eol || obj instanceof SkippedEols)
               {
                  list.remove(i--);
               }
               else if (i > 1 && obj instanceof SkippedSpaces)
               {
                  obj = list.get(i-2);

                  if (obj instanceof Eol  
                     || obj instanceof SkippedEols)
                  {
                     list.remove(i--);
                     list.remove(i--);
                  }
               }
            }
         }
         else
         {
            prev = null;
         }
      }

      return hasVerbatim;
   }

   private DatatoolSettings settings;
   private ImportSettings importSettings;
}
