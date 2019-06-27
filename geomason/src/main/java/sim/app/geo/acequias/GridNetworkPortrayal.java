package sim.app.geo.acequias;
//social nets
import sim.portrayal.*;
import sim.portrayal.network.EdgeDrawInfo2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.field.network.*;
import sim.util.*;
import java.awt.*;
import java.awt.geom.*;

import sim.app.geo.acequias.objects.Tile;

/** <b>GridNetworkPortrayal</b> displays a network on a grid.
 * 
 * A tweak of a Portrayal from MASON.
 * 
 * @author Sarah Wise and Andrew Crooks
 *
 */
public class GridNetworkPortrayal extends FieldPortrayal2D
    {
	
	int gridWidth = 0;
	int gridHeight = 0;
	
    // a line with a label
    SimpleEdgePortrayal2D defaultPortrayal = new SimpleEdgePortrayal2D();
    public Portrayal getDefaultPortrayal() { return defaultPortrayal; }

    public void setField(Object field, int width, int height)
        {
        //dirtyField = true;
    	setDirtyField(true);
    	gridWidth = width;
        gridHeight = height;
        if (field instanceof Network ) this.field = field;
        else throw new RuntimeException("Invalid field for FieldPortrayal2D: " + field);
        }
        
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final Network field = (Network)this.field;
        if( field == null ) return;

        double xScale = info.draw.width / gridWidth;
        double yScale = info.draw.height / gridHeight;

//        final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());
            //(GUIState state, FieldPortrayal2D fieldPortrayal, RectangularShape draw, RectangularShape clip, Double secondPoint)
        EdgeDrawInfo2D newinfo = new EdgeDrawInfo2D(
                info.gui,
                info.fieldPortrayal,
            new Rectangle2D.Double(0,0, xScale, yScale),  // the first two will get replaced

            info.clip, // we don't do further clipping
            new Point2D.Double(0,0)
            );  // these will also get replaced

        // draw ALL the edges -- one never knows if an edge will cross into our boundary
        
        Bag nodes = field.getAllNodes();
        for(int x=0;x<nodes.numObjs;x++)
            {
        	Tile fromTile = (Tile) nodes.objs[x];
            Bag edges = field.getEdgesOut(nodes.objs[x]);
            Double2D locStart = new Double2D( fromTile.getX(), fromTile.getY() );
            
            if (locStart == null) continue;

            // coordinates of the first endpoint
                newinfo.draw.x = (int)(info.draw.x + (xScale) * locStart.x);
                newinfo.draw.y = (int)(info.draw.y + (yScale) * locStart.y);
                double width = (int)(info.draw.x + (xScale) * (locStart.x+1)) - newinfo.draw.x;
                double height = (int)(info.draw.y + (yScale) * (locStart.y+1)) - newinfo.draw.y;

                // adjust drawX and drawY to center
                newinfo.draw.x += width / 2.0;
                newinfo.draw.y += height / 2.0;
            
            for(int y=0;y<edges.numObjs;y++)
                {
                Edge edge = (Edge)edges.objs[y];
                Tile toTile = (Tile) edge.getTo();
                
                Double2D locStop = new Double2D( toTile.getX(), toTile.getY() );
                if (locStop == null) continue;
                
                // coordinates of second endpoint
                    newinfo.secondPoint.x = (int)(info.draw.x + (xScale) * locStop.x);
                    newinfo.secondPoint.y = (int)(info.draw.y + (yScale) * locStop.y);
                    double wid = (int)(info.draw.x + (xScale) * (locStop.x+1)) - newinfo.secondPoint.x;
                    double hei = (int)(info.draw.y + (yScale) * (locStop.y+1)) - newinfo.secondPoint.y;
    
                    // adjust drawX and drawY to center
                    newinfo.secondPoint.x += wid / 2.0;
                    newinfo.secondPoint.y += hei / 2.0;
                
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
        final Network field = (Network)this.field;
        return new LocationWrapper( edge.info, edge, this )
            {
            public String getLocationName()
                {
                Edge edge = (Edge)getLocation();
                if (field != null)
                    {  
                    // do I still exist in the field?  Check the from() value
                    Bag b = field.getEdgesOut(edge.from());
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
    
    
