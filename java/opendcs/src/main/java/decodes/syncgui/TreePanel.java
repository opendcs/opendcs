/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.syncgui;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;
import javax.swing.event.*;
import java.io.IOException;

/**
The TreePanel shows on the left side of the GUI. It shows a 3-level tree:
<ul>
  <li>1 District Names</li>
  <li>2 Archived Databases from each district</li>
  <li>3 Directories within the database</li>
</ul>
*/
public class TreePanel extends JPanel implements TreeSelectionListener
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	BorderLayout borderLayout1 = new BorderLayout();
	DefaultMutableTreeNode top = 
		new DefaultMutableTreeNode(SyncConfig.instance().getHubName(), true);
	DefaultTreeModel treeModel = new DefaultTreeModel(top);
	JTree tree = new JTree(treeModel);


	public TreePanel() 
	{
		jbInit();
		fillDistricts();
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
	}

	public void expandFirstRow()
	{
		tree.expandRow(0);
	}

	/**
	 * Test method to fill tree with demo data.
	 */
	public void fillDistricts()
	{
		int i = 0;
		for(Iterator it = SyncConfig.instance().iterator(); it.hasNext(); )
		{
			District dist = (District)it.next();
			DefaultMutableTreeNode districtNode = 
				new DefaultMutableTreeNode(dist, true);
			treeModel.insertNodeInto(districtNode, top, i++);

			// Put a dummy node under each one so user will be able to expand.
			DefaultMutableTreeNode dummy = new DefaultMutableTreeNode("",true);
			treeModel.insertNodeInto(dummy, districtNode, 0);
		}
	}

	void jbInit() {
		this.setLayout(borderLayout1);
		tree.addTreeWillExpandListener(new TreePanel_tree_treeWillExpandAdapter(this));
    	this.add(tree, BorderLayout.CENTER);
	}

	public void valueChanged(TreeSelectionEvent e)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
				tree.getLastSelectedPathComponent();

		if (node == null)
			return;

		Object nodeObj = node.getUserObject();
		SyncGuiFrame sgf = SyncGuiFrame.instance();
		if (nodeObj instanceof String)
		{
			// this is the top node
			sgf.showTopPanel();
		}
		else if (nodeObj instanceof District)
		{
			// show district info in the view panel.
			sgf.showDistrictPanel((District)nodeObj);
		}
		else if (nodeObj instanceof DistrictDBSnap)
		{
			// show this particular archive db info in the view panel.
			sgf.showSnapshotPanel((DistrictDBSnap)nodeObj);
		}
		else if (nodeObj instanceof PlatList)
		{
			// show the platform list in the view panel.
			sgf.showPlatListPanel((PlatList)nodeObj);
		}
		else if (nodeObj instanceof FileList)
		{
			// Show the list of files in the view panel.
			sgf.showFileListPanel((FileList)nodeObj);
		}
	}

    /**
    * Called when a tree is about to expand. May need to read additional
    * info in order to do the expansion.
    * @param e TreeExpansionEvent
    * @throws ExpandVetoException
    */
	void tree_treeWillExpand(TreeExpansionEvent e) 
		throws ExpandVetoException 
	{
		SyncGuiFrame sgf = SyncGuiFrame.instance();
		TreePath tp = e.getPath();
		Object []path = tp.getPath();

		DefaultMutableTreeNode thisNode = (DefaultMutableTreeNode)
			path[path.length-1];
		Object nodeObj = thisNode.getUserObject();
		if (nodeObj instanceof District)
		{
			District dist = (District)nodeObj;
			String hh = SyncConfig.instance().getHubHome();
			String distDirUrl = hh + "/" + dist.getName();
			if (!dist.isLoaded())
			{
				try
				{
					dist.readSnapList(distDirUrl);

					if (thisNode.getChildCount() == 1)
						thisNode.remove(0);
					for(Iterator it = dist.iterator(); it.hasNext(); )
					{
						DistrictDBSnap snap = (DistrictDBSnap)it.next();
						DefaultMutableTreeNode snapNode = 
							new DefaultMutableTreeNode(snap, true);
						treeModel.insertNodeInto(snapNode, thisNode, 0);

						// Inside the snap, put subdirs for file types
						DefaultMutableTreeNode node = 
							new DefaultMutableTreeNode(
								snap.getPlatList(), true);
						treeModel.insertNodeInto(node, snapNode, 0);

						node = new DefaultMutableTreeNode(
							snap.getFileList("site"), true);
						treeModel.insertNodeInto(node, snapNode, 1);

						node = new DefaultMutableTreeNode(
							snap.getFileList("config"), true);
						treeModel.insertNodeInto(node, snapNode, 2);

						node = new DefaultMutableTreeNode(
							snap.getFileList("equipment"), true);
						treeModel.insertNodeInto(node, snapNode, 3);

						node = new DefaultMutableTreeNode(
							snap.getFileList("presentation"), true);
						treeModel.insertNodeInto(node, snapNode, 4);

						node = new DefaultMutableTreeNode(
							snap.getFileList("routing"), true);
						treeModel.insertNodeInto(node, snapNode, 5);

						node = new DefaultMutableTreeNode(
							snap.getFileList("datasource"), true);
						treeModel.insertNodeInto(node, snapNode, 6);

						node = new DefaultMutableTreeNode(
							snap.getFileList("netlist"), true);
						treeModel.insertNodeInto(node, snapNode, 7);
					}
				}
				catch(IOException ex)
				{
					String msg = "Error '" + distDirUrl + "'";
					log.atError().setCause(ex).log(msg);
					SyncGuiFrame.instance().showLeftStatus(msg + ": " + ex.getMessage());
				}
			}
		}
		else if (nodeObj instanceof DistrictDBSnap)
		{
			DistrictDBSnap dbsnap = (DistrictDBSnap)nodeObj;
			if (!dbsnap.isExpanded())
			{
				District dist = dbsnap.getDistrict();

				// Enqueue the file list and platform list file for read.
				String relurl = dist.getName() + "/" + dbsnap.getDirName()
					+ "/platform/PlatformList.xml";
				sgf.downloadBackground(relurl, dbsnap); 
				relurl = dist.getName() + "/" + dbsnap.getDirName()
					+ "/dblist.txt";
				sgf.downloadBackground(relurl, dbsnap); 
			}
		}
	}
}

class TreePanel_tree_treeWillExpandAdapter implements javax.swing.event.TreeWillExpandListener {
  TreePanel adaptee;

  TreePanel_tree_treeWillExpandAdapter(TreePanel adaptee) {
    this.adaptee = adaptee;
  }
  public void treeWillCollapse(TreeExpansionEvent e) {
  }
  public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
    adaptee.tree_treeWillExpand(e);
  }
}
