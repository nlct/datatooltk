package  com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class DbNameEdit extends AbstractUndoableEdit
{
   public DbNameEdit(DatatoolDbPanel panel, String name)
   {
      super();
      this.panel = panel;
      newName = name;
      oldName = panel.db.getName();

      panel.db.setName(newName);
      panel.setName(newName);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.setName(oldName);
      panel.setName(oldName);
      panel.setModified(true);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.setName(newName);
      panel.setName(newName);
      panel.setModified(true);
   }

   public String getPresentationName()
   {
      return presentationName;
   }

   private DatatoolDbPanel panel;

   private String oldName, newName;

   private static final String presentationName 
     = DatatoolTk.getLabel("undo.edit_name");
}
