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
package com.dickimawbooks.datatooltk.io;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.PreambleParser;
import com.dickimawbooks.texparserlib.latex.probsoln.*;
import com.dickimawbooks.datatooltk.*;

/**
 * Class to import data from a probsoln.sty database.
 */
public class DatatoolProbSoln implements DatatoolImport
{
   public DatatoolProbSoln(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
       File file = new File(source);

       if (!file.exists())
       {
          throw new DatatoolImportException(
             getMessageHandler().getLabelWithValue("error.io.file_not_found",
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

       String encoding = settings.getTeXEncoding();

       PreambleParser preambleParser = new PreambleParser(texApp);
       TeXParser texParser = new TeXParser(preambleParser);

       ProbSolnSty probSolnSty = new ProbSolnSty(null, preambleParser);
       preambleParser.usepackage(probSolnSty);

       if (encoding == null)
       {
          texParser.parse(file);
       }
       else
       {
          texParser.parse(file, Charset.forName(encoding));
       }

       int numDataSets = probSolnSty.getDatabaseCount();

       String key = messageHandler.getLabel("probsoln.label");
       db.addColumn(new DatatoolHeader(db, key, key, settings.TYPE_STRING));

       if (numDataSets > 1)
       {
          key = messageHandler.getLabel("probsoln.set");
          db.addColumn(new DatatoolHeader(db, key, key, settings.TYPE_STRING));
       }

       key = messageHandler.getLabel("probsoln.question");
       db.addColumn(new DatatoolHeader(db, key, key, settings.TYPE_STRING));

       key = messageHandler.getLabel("probsoln.answer");
       db.addColumn(new DatatoolHeader(db, key, key, settings.TYPE_STRING));

       Set<String> dataSetLabels = probSolnSty.getDatabaseLabels();
       int rowIdx = 0;

       for (Iterator<String> dbIt = dataSetLabels.iterator(); dbIt.hasNext();)
       {
          String dbLabel = dbIt.next();
 
          ProbSolnDatabase probDb = probSolnSty.getDatabase(dbLabel);

          for (Iterator<String> probIt = probDb.keySet().iterator();
               probIt.hasNext(); )
          {
             String probLabel = probIt.next();

             DatatoolRow row = new DatatoolRow(db, numDataSets > 1 ? 4 : 3);
             int colIdx = 0;

             row.addCell(colIdx++, probLabel);

             if (numDataSets > 1)
             {
                row.addCell(colIdx++, dbLabel);
             }

             ProbSolnData data = probDb.get(probLabel);

             TeXObject question = data.getQuestion(texParser);
             TeXObject answer = data.getAnswer(texParser, true);

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

             String questionText = question.toString(texParser).trim();
             String answerText = answer.toString(texParser).trim();

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

             rowIdx++;
          }
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

      if (list.isEmpty())
      {
         return false;
      }

      TeXObject element = list.firstElement();

      if (element instanceof Comment)
      {
         String commentText = ((Comment)element).getText().trim();

         if (commentText.isEmpty())
         {
            list.pop();
         }
      }

      element = list.lastElement();

      if (element instanceof Comment)
      {
         String commentText = ((Comment)element).getText().trim();

         if (commentText.isEmpty())
         {
            list.remove(list.size()-1);
         }
      }

      boolean hasVerbatim = false;

      for (int i = 0; i < list.size(); i++)
      {
         element = list.get(i);

         if (element instanceof ControlSequence)
         {
            String name = ((ControlSequence)element).getName();

            if (name.equals("par"))
            {
               list.set(i, new TeXCsRef("DTLpar"));
            }
            else if (name.equals("verb") || name.equals("lstinline"))
            {
               hasVerbatim = true;
            }
            else if (name.equals("begin"))
            {
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

      return hasVerbatim;
   }

   private DatatoolSettings settings;
}
