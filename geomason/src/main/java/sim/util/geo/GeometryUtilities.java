/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 * 
 * $Id$
 * 
 */
package sim.util.geo;

import com.vividsolutions.jts.geom.Envelope;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import sim.display.Display2D;
import sim.field.geo.GeomField;
import sim.field.geo.GeomGridField;
import sim.portrayal.DrawInfo2D;

public class GeometryUtilities
{

	/**
	 * Determines the affine transform which converts world coordinates into
	 * screen coordinates. Modified from GeoTools RenderUtilities.java.
	 * 
	 * @param mapExtent
	 *            MBR in world coordinates mapped to viewport defined in 'info'
	 * @param viewport
	 * @return AffineTransform suitable for converting from world to screen
	 *         coordinates
	 */
	public static AffineTransform worldToScreenTransform(final Envelope mapExtent,
			final java.awt.geom.Rectangle2D.Double viewport)
	{
		double scaleX = viewport.width / mapExtent.getWidth();
		double scaleY = viewport.height / mapExtent.getHeight();

		double tx = -mapExtent.getMinX() * scaleX;
		double ty = (mapExtent.getMinY() * scaleY) + viewport.height;

		AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
		AffineTransform originTranslation = AffineTransform.getTranslateInstance(viewport.x, viewport.y);
		originTranslation.concatenate(at);

		return originTranslation != null ? originTranslation : at;

	}

	public static com.vividsolutions.jts.geom.util.AffineTransformation getPortrayalTransform(final AffineTransform transform, 
			final GeomField field,
			final Rectangle2D.Double view)
	{
		AffineTransform worldToScreen = transform; 
		//if (worldToScreen.getScaleX() > 1 || worldToScreen.getScaleY() > 1) { 
		//	Envelope e = new Envelope(field.drawX, field.getFieldWidth(), field.drawY, field.getFieldHeight());
		//	worldToScreen = worldToScreenTransform(e, view);
		//}
		double m[] = new double[6];
		worldToScreen.getMatrix(m);
		return new com.vividsolutions.jts.geom.util.AffineTransformation(m[0], m[2], m[4], m[1], m[3], m[5]);
	}

	/**
	 * Determines the affine transform which converts world coordinates into
	 * screen coordinates. Modified from GeoTools RenderUtilities.java.
	 * 
	 * convenience variant of other worldToSceenTransform()
	 * 
	 * @param mapExtent
	 *            MBR in world coordinates mapped to viewport defined in 'info'
	 * @param info
	 *            defines the viewport dimensions
	 * @return AffineTransform suitable for converting from world to screen
	 *         coordinates
	 */
	public static AffineTransform worldToScreenTransform(final Envelope mapExtent, final DrawInfo2D info)
	{
		return worldToScreenTransform(mapExtent, info.draw);
	}

	/**
	 * Uses the worldToScreen transform to transform the point (x,y) in screen
	 * coordinates to world coordinates.
	 */
	public static Point2D screenToWorldPointTransform(final AffineTransform worldToScreen, double x, double y)
	{
		// code taken from GeoTools and hacked on
		AffineTransform screenToWorld = null;
		try
		{
			screenToWorld = worldToScreen.createInverse();
		} catch (Exception e)
		{
			System.out.println(e);
			System.exit(-1);
		}

		Point2D p = new Point2D.Double();
		screenToWorld.transform(new Point2D.Double(x, y), p);
		return p;
	}


    
	/**
	 * compute the MBR for the grid field in display coordinates
	 * 
	 * This is used to determine the display bounds for the grid field for
	 * Display2D.attach().
	 * 
	 * @param outer
	 *            denotes MBR that maps to display window in world coordinates
	 * @param display
	 *            is the display into which the grid will be rendered
	 * @param gridField
	 *            for which we wish to find bounds in display coordinates
	 * 
	 * @return grid field bounds in display coordinates; will return viewport if
	 *         grid does not intersect given outer MBR
	 */
	static public java.awt.geom.Rectangle2D.Double computeBounds(final Envelope outer,
                                                                 final Display2D display,
                                                                 final GeomGridField gridField)
	{
		Display2D.InnerDisplay2D innerDisplay = display.insideDisplay;

		// Initialize bounds to that of display viewport
		java.awt.geom.Rectangle2D.Double bounds = new java.awt.geom.Rectangle2D.Double(innerDisplay.xOffset,
				innerDisplay.yOffset, innerDisplay.width, innerDisplay.height);

		AffineTransform transform = GeometryUtilities.worldToScreenTransform(outer, bounds);

        if (isWithinBounds(outer, gridField))
		{
			// Pretty straightforward; just translate all the corners into
			// display coordinates

			Point2D.Double srcMinPoint = new Point2D.Double(gridField.MBR.getMinX(), gridField.MBR.getMaxY());
			Point2D destMinPoint = transform.transform(srcMinPoint, null);

			Point2D.Double srcMaxPoint = new Point2D.Double(gridField.MBR.getMaxX(), gridField.MBR.getMinY());
			Point2D destMaxPoint = transform.transform(srcMaxPoint, null);

			bounds.setRect(destMinPoint.getX(), destMinPoint.getY(), destMaxPoint.getX() - destMinPoint.getX(),
					destMaxPoint.getY() - destMinPoint.getY());

		}
		else
		// badness happened
		{
			// not good if the grid isn't even within the outer MBR; this likely
			// means that 'outer' and 'gridField' are using different spatial
            // reference systems
			System.err.println("Warning: raster not in display");
		}

		return bounds;
	}

    
	/**
	 * @param outer
	 *            denotes MBR that maps to display window in world coordinates
	 * @param gridField
	 *            for which we wish to find bounds in display coordinates
	 * 
	 * @return true iff 'gridField' is within, intersects, or covers 'outer',
	 *         else returns false
	 * 
	 *          Can be used as check for computeBounds()
	 */
	static public boolean isWithinBounds(final Envelope outer, final GeomGridField gridField)
	{
		if (outer.contains(gridField.MBR) || gridField.MBR.contains(outer) || outer.intersects(gridField.MBR))
		{
			return true;
		}
		return false;
	}

}
