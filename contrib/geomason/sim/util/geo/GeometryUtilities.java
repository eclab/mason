package sim.util.geo;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import sim.portrayal.DrawInfo2D;
import com.vividsolutions.jts.geom.Envelope;

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
     
}
