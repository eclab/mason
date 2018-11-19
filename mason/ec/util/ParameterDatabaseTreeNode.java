/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


/*
 * Created on Apr 5, 2005 8:24:19 PM
 * 
 * By: spaus
 */
package ec.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * @author spaus
 */
class ParameterDatabaseTreeNode
    extends DefaultMutableTreeNode
    implements Comparable {

    /**
     * 
     */
    public ParameterDatabaseTreeNode() {
        super();
        }

    /**
     * @param userObject
     */
    public ParameterDatabaseTreeNode(Object userObject) {
        super(userObject);
        }

    /**
     * @param userObject
     * @param allowsChildren
     */
    public ParameterDatabaseTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
        }
    
    /**
     * @param index
     * @param visibleLeaves
     * @return
     */
    public Object getChildAt(int index, boolean visibleLeaves) {
        if (children == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
            }

        if (!visibleLeaves) {
            int nonLeafIndex = -1;
            Enumeration e = children.elements();
            while (e.hasMoreElements()) {
                TreeNode n = (TreeNode)e.nextElement();
                if (!n.isLeaf()) {
                    if (++nonLeafIndex == index)
                        return n;
                    }
                }
            
            throw new ArrayIndexOutOfBoundsException("index = "+index+", children = "+getChildCount(visibleLeaves));
            }
        
        return super.getChildAt(index);
        }
    
    /**
     * @param visibleLeaves
     * @return
     */
    public int getChildCount(boolean visibleLeaves) {
        if (!visibleLeaves) {
            int nonLeafCount = 0;
            Enumeration e = children.elements();
            while (e.hasMoreElements()) {
                TreeNode n = (TreeNode)e.nextElement();
                if (!n.isLeaf()) ++nonLeafCount;
                }
            
            return nonLeafCount;
            }
        
        return super.getChildCount();
        }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        ParameterDatabaseTreeNode n = (ParameterDatabaseTreeNode)o;

        return ((Comparable)userObject).compareTo(n.userObject);
        }
    
    /**
     * @param comp
     */
    public void sort(Comparator comp) {
        if (children == null) 
            return;
        //                by Ermo. get rid of asList, if sorting is the purpose, no need to convert this.       
        //        Object[] childArr = children.toArray();
        //        Arrays.sort(childArr, comp);
        //        children = new Vector(Arrays.asList(childArr));
        // Do we have Collections.sort in 1.5?  1.6?  Sean
        Collections.sort(children, comp);
        
        
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            ParameterDatabaseTreeNode n = 
                (ParameterDatabaseTreeNode)e.nextElement();
            n.sort(comp);
            }
        }
    }
