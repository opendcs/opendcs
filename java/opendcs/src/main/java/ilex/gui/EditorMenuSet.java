/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2008/01/21 02:37:21  mmaloney
*  modified files for internationalization
*
*  Revision 1.3  2004/08/31 16:35:47  mjmaloney
*  javadoc
*
*  Revision 1.2  2004/08/30 14:50:19  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/03/29 21:37:52  mike
*  Created Editor and EditorMenuSet to support custom file editors like
*  the LRGS searchcrit and config-file editors.
*
*/
package ilex.gui;

import java.util.Hashtable;
import java.util.ResourceBundle;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
* This class provides a set of basic AbstractActions that can
* be used in the File and Edit menus of customized editor GUIs
*/
public class EditorMenuSet
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * The constants defined here are combined in the constructor
    * to enable specific actions. They are also used in getAction
    * to retrieve a specific action object.
    */
    public static final int NEW = 0x0001;
    public static final int OPEN = 0x0002;
    public static final int SAVE = 0x0004;
    public static final int SAVEAS = 0x0008;
    public static final int UNDO = 0x0010;
    public static final int CUT = 0x0020;
    public static final int COPY = 0x0040;
    public static final int PASTE = 0x0080;
    public static final int DELETE = 0x0100;
    public static final int ALL = 0xffff;

    private static final int[] FILE_ACTIONS = {NEW,OPEN,SAVE,SAVEAS};
    private static final int[] EDIT_ACTIONS = {UNDO,CUT,COPY,PASTE,DELETE};

    private Hashtable<Integer,AbstractAction> fileActions, editActions;
    private Editor editor;

    /**
    * Constructs an EditorMenuSet with all actions enabled.
    * @param editor the editor
    */
    public EditorMenuSet( Editor editor )
    {
        this(editor, ALL);
    }

    /**
    * Constructs an EditorMenuSet with specific actions.
    * The mask should be acombination of the constants defined
    * in this class.
    * @param editor the editor
    * @param mask the mask
    */
    public EditorMenuSet( Editor editor, int mask )
    {
        this.editor = editor;
        fileActions = new Hashtable<>();
        editActions = new Hashtable<>();
        if ((mask & NEW) != 0)
        {
            fileActions.put(NEW, new NewAction(editor));
        }
        if ((mask & OPEN) != 0)
        {
            fileActions.put(OPEN, new OpenAction(editor));
        }
        if ((mask & SAVE) != 0)
        {
            fileActions.put(SAVE, new SaveAction(editor));
        }
        if ((mask & SAVEAS) != 0)
        {
            fileActions.put(SAVEAS, new SaveAsAction(editor));
        }
        if ((mask & UNDO) != 0)
        {
            editActions.put(UNDO, new UndoAction(editor));
        }
        if ((mask & CUT) != 0)
        {
            editActions.put(CUT, new CutAction(editor));
        }
        if ((mask & COPY) != 0)
        {
            editActions.put(COPY, new CopyAction(editor));
        }
        if ((mask & PASTE) != 0)
        {
            editActions.put(PASTE, new PasteAction(editor));
        }
        if ((mask & DELETE) != 0)
        {
            editActions.put(DELETE, new DeleteAction(editor));
        }
    }

    /**
    * Returns the acttion associated with the specified operation code.
    * Returns null if that code was not specified in the constructor.
    * @param code one of constants defined in this class
    * @return an action
    */
    public AbstractAction getAction( int code )
    {
        AbstractAction ret = fileActions.get(code);
        if (ret == null)
        {
            ret = editActions.get(code);
        }
        return ret;
    }

    /**
    * @return the array of AbstractAction objects used in the File menu.
    */
    public AbstractAction[] getFileActions( )
    {
        return getActions(fileActions, FILE_ACTIONS);
    }

    /**
    * @return the array of AbstractAction objects used in the Edit menu.
    */
    public AbstractAction[] getEditActions( )
    {
        return getActions(editActions, EDIT_ACTIONS);
    }

    /**
    * Returns actions given a set of codes.
    * @param tbl the hash table
    * @param codes the codes
    * @return the actions
    */
    private AbstractAction[] getActions( Hashtable<Integer,AbstractAction> tbl, int[] codes )
    {
        int sz = tbl.size();
        AbstractAction[] ret = new AbstractAction[sz];
        for(int i=0; i<sz; i++)
        {
            ret[i] = tbl.get(codes[i]);
        }
        return ret;
    }
}

abstract class EditorMenuSetAction extends AbstractAction
{
    protected Editor editor;

    /**
    * @param editor
    * @param text
    * @param iconfile
    */
    EditorMenuSetAction( Editor editor, String text, String iconfile )
    {
        super(text, new ImageIcon(iconfile));
        this.editor = editor;
    }

    /**
    * @param editor
    * @param text
    */
    EditorMenuSetAction( Editor editor, String text )
    {
        super(text);
        this.editor = editor;
    }
}

class NewAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    NewAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.exit"), "new.gif");
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.newPress();
    }
}

class OpenAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    OpenAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.open"), "open.gif");
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.openPress();
    }
}

class SaveAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    SaveAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.save"), "save.gif");
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.savePress();
    }
}

class SaveAsAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    SaveAsAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.saveAs"));
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.saveAsPress();
    }
}

class UndoAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    UndoAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.undo"));
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.undoPress();
    }
}

class CutAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    CutAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.cut"), "cut.gif");
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.cutPress();
    }
}

class CopyAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    CopyAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.copy"), "copy.gif");
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.copyPress();
    }
}

class PasteAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    PasteAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.paste"), "paste.gif");
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.pastePress();
    }
}

class DeleteAction extends EditorMenuSetAction
{
    private static ResourceBundle labels = MenuFrame.getLabels();
    /**
    * @param editor
    */
    DeleteAction( Editor editor )
    {
        super(editor, labels.getString("EditorMenuSet.delete"));
    }

    /**
    * @param ae
    */
    public void actionPerformed( ActionEvent ae )
    {
        editor.deletePress();
    }
}
