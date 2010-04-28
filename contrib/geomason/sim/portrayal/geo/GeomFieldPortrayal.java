/*
 * GeomFieldPortrayal.java
 *
 * $Id: GeomFieldPortrayal.java,v 1.5 2010-04-28 19:33:50 kemsulli Exp $
 */

package sim.portrayal.geo;

import com.vividsolutions.jts.geom.*;
import java.awt.Paint; 
import java.awt.Graphics2D;
import java.awt.geom.*;
import sim.field.geo.GeomField;
import sim.portrayal.*;
import sim.util.*; 
import sim.util.geo.*; 
import java.awt.image.*;

/** 
    GeomFieldPortrayal handles the hit testing and drawing for all geometry objects.    
 
    @see GeomPortrayal
*/
public class GeomFieldPortrayal extends FieldPortrayal2D {
        
    /** The underlying portrayal */ 
    GeomPortrayal defaultPortrayal = new GeomPortrayal();
        
        
    public GeomFieldPortrayal()
    {
        super(); 
        setImmutableField(false);
    }

        
    public GeomFieldPortrayal(boolean immutableField)
    {
        super(); 
        setImmutableField(immutableField);
    }
        
    /** Return the underlying portrayal */ 
    public Portrayal getDefaultPortrayal() { return defaultPortrayal; }

    /** Determine the color based on some value
     *
     * The intent is to override this in a subclass such that there will be
     * some mapping between some value calculated from the GeomWrapper to a
     * color. GeomValuedFieldPortrayal will use GeomValuedWrapper.getValue().
     * Novel subclasses could presumably map attribute values to specific
     * Color instances.
     *
     * This default implementation returns null signaling that hitOrDraw()
     * should use its graphics parameter color.
     *
     * XXX Yes, this adds some overhead though it significantly reduces code
     * complexity in GeomValuedFieldPortrayal and any other subclasses.  The
     * overhead should (hopefully) be optimized away.
     *
     * @param gw Contains the value
     * @return color associated with the value
     */
    protected Paint lookupColor(GeomWrapper gw)
    {
        return gw.paint;
    }

    /** used to cache immutable images
     *
     * If the associated field is immutable, then this is used to cache
     * the field image.  It is updated in hitOrDraw().
     *
     */
    private BufferedImage buffer = null;
        
    /** Handles hit-testing and drawing of the underlying geometry objects.  */ 
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
    {
        if (field == null) return; 
                
        // first question: determine the range in which we need to draw.
        final int maxX = (int)info.draw.width;
        final int maxY = (int)info.draw.height;
        if (maxX == 0 || maxY == 0) return; 

        // If we're drawing (and not inspecting), re-fresh the buffer if the
        // associated field is immutable.
        if (graphics != null && immutableField) {
            if (buffer == null || buffer.getWidth() != maxX || buffer.getHeight() != maxY)  { 
                if (buffer != null) buffer.flush(); 
                buffer = new BufferedImage(maxX,maxY,BufferedImage.TYPE_INT_ARGB);                      
                Graphics2D newGraphics = (Graphics2D)buffer.getGraphics(); 
                hitOrDraw2(newGraphics, info, putInHere); 
                newGraphics.dispose(); 
            }
            graphics.drawImage(buffer, (int)info.draw.x, (int)info.draw.y, (int)info.draw.width, (int)info.draw.height,null);
        }
        else
            hitOrDraw2(graphics, info, putInHere);
    }
        
    void hitOrDraw2(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
    {
        GeomField geomField = (GeomField)field;
        AffineTransform savedTransform=null;
                
        // compute the transform between world and screen coordinates, and 
        // also construct a geom.util.AffineTransform for use in hit-testing later
        Envelope MBR = geomField.getMBR();
        AffineTransform worldToScreen = worldToScreenTransform(MBR, info);
        double m[] = new double[6];
        worldToScreen.getMatrix(m); 
        com.vividsolutions.jts.geom.util.AffineTransformation a = new com.vividsolutions.jts.geom.util.AffineTransformation(m[0], m[2], m[4], m[1], m[3], m[5]);
                                
        if (graphics != null) { 
            savedTransform = graphics.getTransform(); // save transform
            graphics.transform(worldToScreen); // using setTransform instead causes problems with SWING!  
        }
                
        Bag geometries = geomField.getGeometry();
                
        for (int i = 0; i < geometries.numObjs; i++)
            {
                GeomWrapper gm = (GeomWrapper)geometries.objs[i];
                Geometry geom = gm.fetchGeometry();
                Portrayal p = getPortrayalForObject(gm);
                if (!(p instanceof SimplePortrayal2D))
                    throw new RuntimeException("Unexpected Portrayal " + p + " for object " + geom + 
                                               " -- expected a SimplePortrayal2D or a GeomPortrayal");
                                        
                SimplePortrayal2D portrayal = (SimplePortrayal2D) p ; 
                        
                if (graphics == null) {
                    Geometry g = (Geometry)(geom.clone()); 
                    g.apply(a); 
                    g.geometryChanged();
                    if (portrayal.hitObject(g, info))
                        putInHere.add(new LocationWrapper(gm, null, this));
                }
                else { 
                    Paint color = lookupColor(gm);
                    if (color != null) gm.paint = color; //graphics.setPaint(color);
                                
                    if (portrayal instanceof GeomPortrayal)
                        portrayal.draw(gm, graphics, info);
                        
                    else  {         // have a SimplePortrayal2D, so move info.draw to the centroid of the geometry
                        Point pt = geom.getCentroid(); 
                        info.draw.x = pt.getX(); 
                        info.draw.y = pt.getY(); 
                        portrayal.draw(geom, graphics, info);
                    }
                }
                        
                         
            }
                
        if (graphics != null) 
            graphics.setTransform(savedTransform); // restore y axis & origin
    }
        
    /** Sets the underlying field, after ensuring its a GeomField. */ 
    public void setField(Object field)
    {
        dirtyField = true;
        if (field instanceof GeomField)this.field = field;
        else 
            throw new RuntimeException("Invalid field for GeomFieldPortrayal: " + field);
    }
        
        
    /** Sets up the affine transform
     *
     * @param mapExtent
     *            the map extent
     * @param paintArea
     *            the size of the rendering output area
     * @return a transform that maps from real world coordinates to the screen
     * @note Ganked from GeoTools RenderUtilities.java and hacked on
     */
    protected AffineTransform worldToScreenTransform(Envelope mapExtent, DrawInfo2D info) {
        double scaleX = info.draw.width / mapExtent.getWidth();
        double scaleY = info.draw.height / mapExtent.getHeight();
                
        double tx = -mapExtent.getMinX() * scaleX;
        double ty = (mapExtent.getMinY() * scaleY) + info.draw.height;
                
        AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
        AffineTransform originTranslation = AffineTransform.getTranslateInstance(info.draw.x, info.draw.y);
        originTranslation.concatenate(at);
                
        return originTranslation != null ? originTranslation : at;
    }       
}
