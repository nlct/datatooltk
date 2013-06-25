package  com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;
import java.util.Random;

import com.dickimawbooks.datatooltk.*;

public class ShuffleEdit extends AbstractUndoableEdit
{
   public ShuffleEdit(DatatoolDbPanel panel, Random random)
   {
      super();
      this.panel = panel;

      oldData = panel.db.dataToArray();

      panel.db.shuffle(random);
      panel.setModified(true);
      panel.repaint();

      newData = panel.db.dataToArray();
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.dataFromArray(oldData);
      panel.setModified(false);
      panel.repaint();
   }

   public void redo() throws CannotRedoException
   {
      panel.db.dataFromArray(newData);
      panel.setModified(false);
      panel.repaint();
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;

   private DatatoolRow[] oldData, newData;

   private static final String name = DatatoolTk.getLabel("undo.shuffle");
}
