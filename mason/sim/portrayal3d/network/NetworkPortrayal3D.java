/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.network;
import sim.portrayal3d.*;
import sim.portrayal.*;
import sim.portrayal.Portrayal; 
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
    public SpatialNetwork3D field;
    
    // a line with a label
    SimpleEdgePortrayal3D defaultPortrayal = new SimpleEdgePortrayal3D();
    public Portrayal getDefaultPortrayal() { return defaultPortrayal; }

    public void setField(Object field)
        {
        if (field instanceof SpatialNetwork3D ) this.field = (SpatialNetwork3D) field;
        else throw new RuntimeException("Invalid field for FieldPortrayal3D: " + field);
        }

    public Object getField() { return field; } 

            
    public TransformGroup createModel()
        {
        TransformGroup globalTG = new TransformGroup(); 
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        if( field == null ) return globalTG;
        
        Edge edge = new Edge(null,null,null); 

        // draw ALL the edges -- one never knows if an edge will cross into our boundary
        Portrayal p = getPortrayalForObject(new SimpleEdgePortrayal3D.EdgeWrapper(0,0,0,this));
        if (!(p instanceof SimpleEdgePortrayal3D)) 
            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                                       edge + " -- expected a SimpleEdgePortrayal3D");

        SimpleEdgePortrayal3D portrayal = (SimpleEdgePortrayal3D) p; 
                        
        Bag nodes = field.network.getAllNodes();
        for(int x=0;x<nodes.numObjs;x++)
            {
            Bag edges = field.network.getEdgesOut(nodes.objs[x]);
                
            for(int y=0;y<edges.numObjs;y++)
                {
                edge = (Edge)edges.objs[y];
                SimpleEdgePortrayal3D.EdgeWrapper newinfo = new SimpleEdgePortrayal3D.EdgeWrapper(0,0,0,this); 
                newinfo.edge = edge; 
                TransformGroup localTG = portrayal.getModel(newinfo, null); 
                localTG.setCapability(Group.ALLOW_CHILDREN_READ); 
                localTG.setUserData(newinfo); 
                globalTG.addChild(localTG); 
                }
            }
        return globalTG; 
        }

    final Edge edge = new Edge(null,null,null); 
    static SimpleEdgePortrayal3D.EdgeWrapper newinfo = new  SimpleEdgePortrayal3D.EdgeWrapper(); 

    public void updateModel(TransformGroup modelTG) 
        {
        if (field == null) return; 

        // draw ALL the edges -- one never knows if an edge will cross into our boundary
        Portrayal p = getPortrayalForObject(edge);
        if (!(p instanceof SimpleEdgePortrayal3D))
            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                                       edge + " -- expected a SimpleEdgePortrayal3D");
        SimpleEdgePortrayal3D portrayal = (SimpleEdgePortrayal3D) p;
        
        for (int i=0; i < modelTG.numChildren(); i++) { 
            TransformGroup tg = (TransformGroup) modelTG.getChild(i); 
            newinfo = (SimpleEdgePortrayal3D.EdgeWrapper)(tg.getUserData()); 
            portrayal.getModel(newinfo, tg); 
            }
        }
        
    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr) 
        { 
        return w; 
        }
    }
    
    
