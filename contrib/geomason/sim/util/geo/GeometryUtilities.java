package sim.util.geo;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.Comparator;

import sim.portrayal.DrawInfo2D;
import com.vividsolutions.jts.geom.Envelope;
import sim.field.geo.GeomGridField;

public class GeometryUtilities {

	/** Determines the affine transform which converts world coordinates into screen 
     * coordinates.  Modified from GeoTools RenderUtilities.java. 
     */
     public static AffineTransform worldToScreenTransform(Envelope mapExtent, DrawInfo2D info) {
        double scaleX = info.draw.width / mapExtent.getWidth();
        double scaleY = info.draw.height / mapExtent.getHeight();
                
        double tx = -mapExtent.getMinX() * scaleX;
        double ty = (mapExtent.getMinY() * scaleY) + info.draw.height;
                
        AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
        AffineTransform originTranslation = AffineTransform.getTranslateInstance(info.draw.x, info.draw.y);
        originTranslation.concatenate(at);
                
        return originTranslation != null ? originTranslation : at;
    }     
	
     
     /**
      *  Uses the worldToScreen transform to transform the point (x,y) in screen coordinates 
      *  to world coordinates.  
      */
     public static Point2D screenToWorldPointTransform(AffineTransform worldToScreen, double x, double y)
     {
    	// code taken from GeoTools and hacked on
 		AffineTransform screenToWorld = null;
 		try {
 			screenToWorld = worldToScreen.createInverse(); 
 		} catch (Exception e) {
 			System.out.println(e); 
 			System.exit(-1); 
 		}
 		
		Point2D p = new Point2D.Double();
		screenToWorld.transform(new Point2D.Double(x,y), p);
		return p; 
     }
 
     /** Comparator to sort and search for AttributeFields by name in a Collection */
     public static Comparator<AttributeField> attrFieldCompartor = new Comparator<AttributeField>() { 
     		public int compare(AttributeField af1, AttributeField af2)
     		{
     			return af1.name.compareTo(af2.name) ;
     		}
     };


         /** compute the MBR for the grid field in display coordinates
     *
     * This is used to determine the display bounds for the grid field for
     * Display2D.attach().
     *
     * @param outer denotes MBR that maps to display window in world coordinates
     * @param drawInfo describes the view port into which we'll be displaying the grid field
     * @param gridField for which we wish to find bounds in display coordinates
     *
     * @return grid field bounds in display coordinates; will return drawInfo bounds if grid does not intersect given outer MBR
     */
    static public java.awt.geom.Rectangle2D.Double computeBounds(final Envelope outer, final DrawInfo2D drawInfo, final GeomGridField gridField)
    {
        java.awt.geom.Rectangle2D.Double bounds = (Double) drawInfo.draw.clone();

        AffineTransform transform = GeometryUtilities.worldToScreenTransform(outer, drawInfo);

        if (outer.contains(gridField.MBR) ||
            gridField.MBR.contains(outer) ||
            outer.intersects(gridField.MBR))
        {
            // Pretty straightforward; just translate all the corners into display coordinates

            Point2D.Double srcMinPoint = new Point2D.Double(gridField.MBR.getMinX(), gridField.MBR.getMaxY());
            Point2D destMinPoint = transform.transform(srcMinPoint, null);

            Point2D.Double srcMaxPoint = new Point2D.Double(gridField.MBR.getMaxX(), gridField.MBR.getMinY());
            Point2D destMaxPoint = transform.transform(srcMaxPoint, null);

            bounds.setRect(destMinPoint.getX(), destMinPoint.getY(), destMaxPoint.getX() - destMinPoint.getX(), destMaxPoint.getY() - destMinPoint.getY());

        } else // badness happened
        {
            // not good if the grid isn't even within the outer MBR; this likely means that
            // 'outer' and 'gridField' are using different spatial reference systems
            System.err.println("Warning: raster not in display");
        }

        return bounds;
    }


    /**
     * @param outer denotes MBR that maps to display window in world coordinates
     * @param gridField for which we wish to find bounds in display coordinates
     *
     * @returns true iff 'gridField' is within, intersects, or covers 'outer', else returns false
     *
     * Can be used as check for computeBounds()
     */
    static public boolean isWithinBounds(final Envelope outer, final GeomGridField gridField)
    {
        if (outer.contains(gridField.MBR) ||
            gridField.MBR.contains(outer) ||
            outer.intersects(gridField.MBR))
        {
            return true;
        }
        return false;
    }

     
}
