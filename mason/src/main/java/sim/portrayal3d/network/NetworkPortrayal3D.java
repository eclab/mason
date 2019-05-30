/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.network;
import java.util.HashMap;
import java.util.Iterator;

import sim.portrayal3d.*;
import sim.portrayal.*;
import sim.field.network.*;
import sim.util.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.picking.*;

/**
   Portrays network fields.   Only draws the edges.  To draw the nodes, use a 
   ContinuousPortrayal2D or SparseGridPortrayal2D.
*/

public class NetworkPortrayal3D extends FieldPortrayal3D
    {    
    // a line with a label
    SimpleEdgePortrayal3D defaultPortrayal = new SimpleEdgePortrayal3D();
    public Portrayal getDefaultPortrayal() { return defaultPortrayal; }

    public void setField(Object field)
        {
        if (field instanceof SpatialNetwork3D ) super.setField(field);
        else throw new RuntimeException("Invalid field for FieldPortrayal3D: " + field + ".  You probably wanted a SpatialNetwork3D.");
        }

    public Object getField() { return field; } 

            
    public TransformGroup createModel()
        {
        TransformGroup globalTG = new TransformGroup(); 
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        globalTG.setCapability(Group.ALLOW_CHILDREN_WRITE);
        globalTG.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        
        
        SpatialNetwork3D field = (SpatialNetwork3D)this.field;
                
        if( field == null ) return globalTG;
        
        // draw ALL the edges -- one never knows if an edge will cross into our boundary

        Bag nodes = field.network.getAllNodes();
        for(int x=0;x<nodes.numObjs;x++)
            {
            Bag edges = field.network.getEdgesOut(nodes.objs[x]);
                
            for(int y=0;y<edges.numObjs;y++)
                {
                Edge edge = (Edge)edges.objs[y];
                globalTG.addChild(wrapModelForNewEdge(edge)); 
                }
            }
        return globalTG; 
        }
    
    /**
     * This function is called from createModel for each edge in the 
     * field and from the updateModel part of getModel for the
     * new edges.
     * 
     * <p>In order to dynamically add/remove the subtrees associated with
     * edges, this function wraps their TransformGroups into BranchGroups.
     **/
    protected BranchGroup wrapModelForNewEdge(Edge edge)
        {
        LocationWrapper newwrapper = new LocationWrapper(edge.info, edge, this);
                
        Portrayal p = getPortrayalForObject(newwrapper);
        if (!(p instanceof SimpleEdgePortrayal3D)) 
            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                edge + " -- expected a SimpleEdgePortrayal3D");
        SimpleEdgePortrayal3D portrayal = (SimpleEdgePortrayal3D) p; 
        
        portrayal.setCurrentFieldPortrayal(this);
        TransformGroup localTG = portrayal.getModel(newwrapper, null); 
        localTG.setCapability(Group.ALLOW_CHILDREN_READ); 
        localTG.setUserData(newwrapper);                 
        
        BranchGroup localBG = new BranchGroup();
        localBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        localBG.setCapability(BranchGroup.ALLOW_DETACH);
        localBG.addChild(localTG);
        localBG.setUserData(newwrapper);
        //I set the user data in both localBG, and localTG.
        return localBG;
        }
    


    public void updateModel(TransformGroup globalTG) 
        {
        SpatialNetwork3D field = (SpatialNetwork3D)this.field;

        if (field == null) return;      
        HashMap hm = new HashMap();
        Network net = field.network;
        Bag nodes = net.getAllNodes();
        for(int n=0;n<nodes.numObjs;n++)
            {
            Bag edges = net.getEdgesOut(nodes.objs[n]);
            for(int i=0;i<edges.numObjs;i++)
                {
                Object edge = edges.objs[i];
                hm.put(edge,edge);
                }
            }
                        
        // update children (edges) if they're still in the field (network),
        // else remove the children if they appear to have left.
        // We use a hashmap to efficiently mark out the children
        // as we delete them and update them
        for(int t= globalTG.numChildren()-1; t>=0; t--)
            {
            BranchGroup localBG = (BranchGroup)globalTG.getChild(t);
            LocationWrapper wrapper = (LocationWrapper)localBG.getUserData();
            Object edge = wrapper.getLocation();
                        
            if(hm.remove(edge) != null) // hm.containsKey(edgeObj))  // object still in the field
                {  // we can pull this off because valid edges can't be null -- Gabriel
                TransformGroup localTG = (TransformGroup)localBG.getChild(0);
                Portrayal p = getPortrayalForObject(wrapper);
                if(! (p instanceof SimplePortrayal3D))
                    throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                        wrapper + " -- expecting a SimplePortrayal3D");
                SimplePortrayal3D p3d = (SimplePortrayal3D)p;
                
                p3d.setCurrentFieldPortrayal(this);
                TransformGroup localTG2 = p3d.getModel(wrapper, localTG);

                if(localTG != localTG2)
                    {
                    localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                    localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                    localTG2.setUserData(wrapper);                                                
                    BranchGroup newlocalBG = new BranchGroup();
                    newlocalBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                    newlocalBG.setCapability(BranchGroup.ALLOW_DETACH);
                    newlocalBG.setUserData(wrapper);
                    newlocalBG.addChild(localTG2);
                                                
                    globalTG.setChild(newlocalBG, t);
                    }
                }        
            else  // object is no longer in the field -- remove it from the scenegraph
                globalTG.removeChild(t);
            }
        
        
        // The remaining edges in hm must be new.  We add them to the scenegraph.
        // But first, we should check to see if hm is empty.
        if (!hm.isEmpty())
            {
            Iterator newObjs = hm.values().iterator();  // yuck, inefficient
            while(newObjs.hasNext())
                {
                Edge edge = (Edge)newObjs.next();
                globalTG.addChild(wrapModelForNewEdge(edge));
                }

            }
        }
        
    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr) 
        { 
        return w; 
        }
    }
    
    
