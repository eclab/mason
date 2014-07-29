/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network;
import sim.portrayal.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.field.*;
import sim.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
   Portrays network fields.   Only draws the edges.  To draw the nodes, use a 
   ContinuousPortrayal2D or SparseGridPortrayal2D.  The 'location' passed
   into the DrawInfo2D handed to the SimplePortryal2D is the Edge itself,
   while the 'object' passed to the SimplePortryal2D is the Edge's info object. 
*/

public class NetworkPortrayal2D extends FieldPortrayal2D
    {
    // a line with a label
    SimpleEdgePortrayal2D defaultPortrayal = new SimpleEdgePortrayal2D();
    public Portrayal getDefaultPortrayal() { return defaultPortrayal; }

    public void setField(Object field)
        {
        if (field instanceof SpatialNetwork2D ) super.setField(field);
        else throw new RuntimeException("Invalid field for FieldPortrayal2D: " + field);
        }
        
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final SpatialNetwork2D field = (SpatialNetwork2D)this.field;
        if( field == null ) return;

        // compute the field for the second endpoint
        SparseField2D otherField = field.field2;  // do we have an auxiliary field?
        if (otherField == null) otherField = field.field;  // I guess not, use the main field

        Double2D dimensions = field.field.getDimensions();  // dimensions of the main field
        double xScale = info.draw.width / dimensions.x;
        double yScale = info.draw.height / dimensions.y;

        EdgeDrawInfo2D newinfo = new EdgeDrawInfo2D(
            info.gui, 
            info.fieldPortrayal,
            new Rectangle2D.Double(0,0, xScale, yScale),  // the first two will get replaced
            info.clip, // we don't do further clipping
            new Point2D.Double(0,0));  // these will also get replaced  
        newinfo.fieldPortrayal = this;
        newinfo.precise = info.precise;

        // draw ALL the edges -- one never knows if an edge will cross into our boundary
        
        Bag nodes = field.network.getAllNodes();
        HashMap edgemap = new HashMap();        
                
        for(int x=0;x<nodes.numObjs;x++)
            {
            Object node = nodes.objs[x];
            Bag edges = field.network.getEdgesOut(node);
            Double2D locStart = field.field.getObjectLocationAsDouble2D(node);
            if (locStart == null) continue;
                                    
            // coordinates of first endpoint
            if (field.field instanceof Continuous2D) // it's continuous
                {
                newinfo.draw.x = (info.draw.x + (xScale) * locStart.x);
                newinfo.draw.y = (info.draw.y + (yScale) * locStart.y);
                }
            else        // it's a grid
                {
                newinfo.draw.x = (int)Math.floor(info.draw.x + (xScale) * locStart.x);
                newinfo.draw.y = (int)Math.floor(info.draw.y + (yScale) * locStart.y);
                double width = (int)Math.floor(info.draw.x + (xScale) * (locStart.x+1)) - newinfo.draw.x;
                double height = (int)Math.floor(info.draw.y + (yScale) * (locStart.y+1)) - newinfo.draw.y;

                // adjust drawX and drawY to center
                newinfo.draw.x += width / 2.0;
                newinfo.draw.y += height / 2.0;
                }
            
            for(int y=0;y<edges.numObjs;y++)
                {
                Edge edge = (Edge)edges.objs[y];
                                
                Double2D locStop = otherField.getObjectLocationAsDouble2D(edge.getOtherNode(node));
                if (locStop == null) continue;

                // only include the edge if we've not included it already.
                if (!field.network.isDirected())
                    {
                    if (edgemap.containsKey(edge)) continue;
                    edgemap.put(edge, edge);
                    }
                                
                // coordinates of second endpoint
                if (otherField instanceof Continuous2D) // it's continuous
                    {
                    newinfo.secondPoint.x = (info.draw.x + (xScale) * locStop.x);
                    newinfo.secondPoint.y = (info.draw.y + (yScale) * locStop.y);
                    }
                else    // it's a grid
                    {
                    newinfo.secondPoint.x = (int)Math.floor(info.draw.x + (xScale) * locStop.x);
                    newinfo.secondPoint.y = (int)Math.floor(info.draw.y + (yScale) * locStop.y);
                    double width = (int)Math.floor(info.draw.x + (xScale) * (locStop.x+1)) - newinfo.secondPoint.x;
                    double height = (int)Math.floor(info.draw.y + (yScale) * (locStop.y+1)) - newinfo.secondPoint.y;
    
                    // adjust drawX and drawY to center
                    newinfo.secondPoint.x += width / 2.0;
                    newinfo.secondPoint.y += height / 2.0;
                    }
                
                // here's how we could reduce it if we knew that it intersected with the clip.... [cool job, Liviu -- Sean]
                // Line2D.Double line = new Line2D.Double(newinfo.draw.x, newinfo.draw.y, newinfo.secondPoint.x, newinfo.secondPoint.y);
                // if (line.intersects (info.clip))
                        {
                        Portrayal p = getPortrayalForObject(edge);
                        if (!(p instanceof SimpleEdgePortrayal2D))
                            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                                edge + " -- expected a SimpleEdgePortrayal2D");
                        SimpleEdgePortrayal2D portrayal = (SimpleEdgePortrayal2D) p;
                    
                        newinfo.location = edge;

                        if (graphics == null)
                            {
                            if (portrayal.hitObject(edge, newinfo))
                                {
                                putInHere.add(getWrapper(edge));
                                }
                            }
                        else
                            {
                            // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                            //                        graphics.setClip(clip);
                            portrayal.draw(edge, graphics, newinfo);
                            }
                        }
                }
            }
        }
        
    String edgeLocation(Edge edge)
        {
        // don't use toString, too much info
                
        if (edge == null)
            return "(Null)";
        else if (edge.owner() == null) 
            return "(Unowned)" + edge.from() + " --> " + edge.to();
        else if (edge.owner().isDirected())
            return edge.from() + " --> " +edge.to();
        else 
            return edge.from() + " <-> " + edge.to();
        }
                
                
    // The easiest way to make an inspector which gives the location of my objects
    public LocationWrapper getWrapper(Edge edge)
        {
        final SpatialNetwork2D field = (SpatialNetwork2D)this.field;
        return new LocationWrapper( edge.info, edge, this )
            {
            public String getLocationName()
                {
                Edge edge = (Edge)getLocation();
                if (field != null && field.network != null)
                    {  
                    // do I still exist in the field?  Check the from() value
                    Bag b = field.network.getEdgesOut(edge.from());
                    // if (b != null)  // no longer necessary
                    for(int x=0;x<b.numObjs;x++)
                        if (b.objs[x] == edge)
                                                
                            return edgeLocation(edge);
                    }
                return "Gone.  Was: " + edgeLocation(edge);
                }
            };
        }
    }
    
    
