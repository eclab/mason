/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network;
import sim.portrayal.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.*;
import java.awt.*;
import java.awt.geom.*;

/**
   Portrays network fields.   Only draws the edges.  To draw the nodes, use a ContinuousPortrayal2D or SparseGridPortrayal2D.
*/

public class NetworkPortrayal2D extends FieldPortrayal2D
    {
    // a line with a label
    SimpleEdgePortrayal2D defaultPortrayal = new SimpleEdgePortrayal2D();
    public Portrayal getDefaultPortrayal() { return defaultPortrayal; }

    public void setField(Object field)
        {
        dirtyField = true;
        if (field instanceof SpatialNetwork2D ) this.field = field;
        else throw new RuntimeException("Invalid field for FieldPortrayal2D: " + field);
        }
        
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final SpatialNetwork2D field = (SpatialNetwork2D)this.field;
        if( field == null ) return;

        double xScale = info.draw.width / field.getWidth();
        double yScale = info.draw.height / field.getHeight();

//        final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());

        EdgeDrawInfo2D newinfo = new EdgeDrawInfo2D(
            new Rectangle2D.Double(0,0, xScale, yScale),  // the first two will get replaced
            info.clip, // we don't do further clipping
            new Point2D.Double(0,0));  // these will also get replaced  

        // draw ALL the edges -- one never knows if an edge will cross into our boundary
        
        Bag nodes = field.network.getAllNodes();
        for(int x=0;x<nodes.numObjs;x++)
            {
            Bag edges = field.network.getEdgesOut(nodes.objs[x]);
            Double2D locStart = field.getObjectLocation(nodes.objs[x]);
            if (locStart == null) continue;
            // if (edges == null) continue;  // no longer necessary
            
            // coordinates of first endpoint
            if (field.field instanceof Continuous2D) // it's continuous
                {
                newinfo.draw.x = (info.draw.x + (xScale) * locStart.x);
                newinfo.draw.y = (info.draw.y + (yScale) * locStart.y);
                }
            else        // it's a grid
                {
                newinfo.draw.x = (int)(info.draw.x + (xScale) * locStart.x);
                newinfo.draw.y = (int)(info.draw.y + (yScale) * locStart.y);
                double width = (int)(info.draw.x + (xScale) * (locStart.x+1)) - newinfo.draw.x;
                double height = (int)(info.draw.y + (yScale) * (locStart.y+1)) - newinfo.draw.y;

                // adjust drawX and drawY to center
                newinfo.draw.x += width / 2.0;
                newinfo.draw.y += height / 2.0;
                }
            
            for(int y=0;y<edges.numObjs;y++)
                {
                Edge edge = (Edge)edges.objs[y];
                Double2D locStop = field.getObjectLocation(edge.to());
                if (locStop == null) continue;
                
                // coordinates of second endpoint
                if (field.field instanceof Continuous2D) // it's continuous
                    {
                    newinfo.secondPoint.x = (info.draw.x + (xScale) * locStop.x);
                    newinfo.secondPoint.y = (info.draw.y + (yScale) * locStop.y);
                    }
                else    // it's a grid
                    {
                    newinfo.secondPoint.x = (int)(info.draw.x + (xScale) * locStop.x);
                    newinfo.secondPoint.y = (int)(info.draw.y + (yScale) * locStop.y);
                    double width = (int)(info.draw.x + (xScale) * (locStop.x+1)) - newinfo.secondPoint.x;
                    double height = (int)(info.draw.y + (yScale) * (locStop.y+1)) - newinfo.secondPoint.y;
    
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
                    
                        if (graphics == null)
                            {
                            if (portrayal.hitObject(edge, newinfo))
                                putInHere.add(getWrapper(edge));
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
                            return "" + edge.from() + " --> " + edge.to();
                    }
                return "Gone.  Was: " + edge.from() + " --> " + edge.to();
                }
            };
        }
    }
    
    
