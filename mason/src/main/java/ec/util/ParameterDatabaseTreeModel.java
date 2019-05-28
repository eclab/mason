/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


/*
 * Created on Apr 6, 2005 7:12:32 PM
 * 
 * By: spaus
 */
package ec.util;

import java.util.Comparator;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * @author spaus
 */
public class ParameterDatabaseTreeModel
    extends DefaultTreeModel {

    private boolean visibleLeaves;
    
    /**
     * @param root
     */
    public ParameterDatabaseTreeModel(TreeNode root) {
        super(root);
        visibleLeaves = true;
        }

    /**
     * @param root
     * @param asksAllowsChildren
     */
    public ParameterDatabaseTreeModel(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
        visibleLeaves = true;
        }

    /**
     * @param visibleLeaves
     */
    public void setVisibleLeaves(boolean visibleLeaves) {
        this.visibleLeaves = visibleLeaves;
        }
    
    public boolean getVisibleLeaves() {
        return visibleLeaves;
        }
    
    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    public Object getChild(Object parent, int index) {
        if (!visibleLeaves) {
            if (parent instanceof ParameterDatabaseTreeNode) {
                return ((ParameterDatabaseTreeNode)parent).getChildAt(index,visibleLeaves);
                }
            }
        
        return ((TreeNode)parent).getChildAt(index);
        }
    
    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    public int getChildCount(Object parent) {
        if (!visibleLeaves) {
            if (parent instanceof ParameterDatabaseTreeNode) {
                return ((ParameterDatabaseTreeNode)parent).getChildCount(visibleLeaves);
                }
            }
        
        return ((TreeNode)parent).getChildCount();
        }
    
    /**
     * @param parent
     * @param comp
     */
    public void sort(Object parent, Comparator comp) {
        ((ParameterDatabaseTreeNode)parent).sort(comp);
        }
    }
