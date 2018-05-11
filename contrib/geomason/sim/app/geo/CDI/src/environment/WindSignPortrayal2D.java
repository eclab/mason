package CDI.src.environment;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import sim.display.GUIState;
import sim.display.Manipulating2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.LocationWrapper;
import sim.portrayal.Oriented2D;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.simple.OrientedPortrayal2D;

public class WindSignPortrayal2D extends SimplePortrayal2D {
	public static final double DEFAULT_SCALE = 0.5;
	public static final int DEFAULT_OFFSET = 0;

	public int level = 0;
	
	/** The pre-scaling length */
	public double scale;

	/** The post-scaling length offset */
	public int offset;

	/** The Paint or Color of the line */
	public Paint paint;

	public SimplePortrayal2D child;

	/** Overrides all drawing. */
	boolean showOrientation = true;

	public boolean drawFilled = true;

	public void setDrawFilled(boolean val) {
		drawFilled = val;
	}

	public boolean isDrawFilled() {
		return drawFilled;
	}

	public boolean isOrientationShowing() {
		return showOrientation;
	}

	public void setOrientationShowing(boolean val) {
		showOrientation = val;
	}

	/** @deprecated use isOrientationShowing() */
	public boolean isLineShowing() {
		return showOrientation;
	}

	/** @deprecated use setOrientationShowing() */
	public void setLineShowing(boolean val) {
		showOrientation = val;
	}

	Shape path = null;

	Shape buildPolygon(double[] xpoints, double[] ypoints) {
		GeneralPath path = new GeneralPath();
		// general paths are only floats and not doubles in Java 1.4, 1.5
		// in 1.6 it's been changed to doubles finally but we're not there yet.
		if (xpoints.length > 0)
			path.moveTo((float) xpoints[0], (float) ypoints[0]);
		for (int i = xpoints.length - 1; i >= 0; i--)
			path.lineTo((float) xpoints[i], (float) ypoints[i]);
		return path;
	}

	boolean onlyDrawWhenSelected = false;

	public void setOnlyDrawWhenSelected(boolean val) {
		onlyDrawWhenSelected = val;
	}

	public boolean getOnlyDrawWhenSelected() {
		return onlyDrawWhenSelected;
	}

	public WindSignPortrayal2D(SimplePortrayal2D child, int offset,
			double scale, Paint paint) {
		this.offset = offset;
		this.scale = scale;
		this.child = child;
		this.paint = paint;
	}

	/**
	 * Draw a line of length scale = 0.5, offset = 0, in red. If child is null,
	 * then the underlying model object is presumed to be a Portrayal2D and will
	 * be used.
	 */
	public WindSignPortrayal2D(SimplePortrayal2D child) {
		this(child, DEFAULT_OFFSET, DEFAULT_SCALE, Color.red);
	}

	/**
	 * Draw a line of the given length in red. If child is null, then the
	 * underlying model object is presumed to be a Portrayal2D and will be used.
	 */
	public WindSignPortrayal2D(SimplePortrayal2D child, int offset, double scale) {
		this(child, offset, scale, Color.red);
	}

	/**
	 * Draw a line of length scale = 0.5, offset = 0. If child is null, then the
	 * underlying model object is presumed to be a Portrayal2D and will be used.
	 */
	public WindSignPortrayal2D(SimplePortrayal2D child, Paint paint) {
		this(child, DEFAULT_OFFSET, DEFAULT_SCALE, paint);
	}

	public SimplePortrayal2D getChild(Object object) {
		if (child != null)
			return child;
		else {
			if (!(object instanceof SimplePortrayal2D))
				throw new RuntimeException(
						"Object provided to OrientedPortrayal2D is not a SimplePortrayal2D: "
								+ object);
			return (SimplePortrayal2D) object;
		}
	}

	int[] simplePolygonX = new int[4];
	int[] simplePolygonY = new int[4];
	double[] simplePolygonXd = new double[4];
	double[] simplePolygonYd = new double[4];
	double lastLength = Double.NaN;
	AffineTransform transform = new AffineTransform();
	Stroke stroke = new BasicStroke();

	/**
	 * Returns the orientation of the underlying object, or NaN if there is no
	 * such orientation. The default implementation assumes that the object is
	 * non-null and is an instance of Oriented2D, and calls orientation2D() on
	 * it; else it returns NaN.
	 */

	public double getOrientation(Object object, DrawInfo2D info) {
		if (object != null && object instanceof Oriented2D)
			return ((Oriented2D) object).orientation2D();
		else
			return Double.NaN;
	}

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {

		if (showOrientation && (info.selected || !onlyDrawWhenSelected)) {
			double theta = getOrientation(object, info);
			if (theta == theta) // NaN != NaN
			{
				double length = (scale * (info.draw.width < info.draw.height ? info.draw.width
						: info.draw.height))
						+ offset; // fit in smallest dimension
				if (length != lastLength) {
					lastLength = length;
					path = null;
				} // redo shape

				graphics.setPaint(paint);


				transform.setToTranslation(info.draw.x, info.draw.y);
				transform.rotate(theta);

				if (path == null) {
					path = getPath(level, length);
				}
				graphics.setStroke(stroke);
				graphics.draw(transform.createTransformedShape(path));

			}
		}

		// draw the underlying object last?
		if (drawFilled)
			getChild(object).draw(object, graphics, info);
	}

	public GeneralPath getPath(int level, double length) {
		// draw the circle and the vertical line
		GeneralPath generalPath = new GeneralPath(new Ellipse2D.Double(-4*length, -4*length,
				8*length, 8*length));

		if (level == 0) {
			// calm, a dot inside
			generalPath.append(new Ellipse2D.Double(-1*length, -1*length, 2*length, 2*length), false);
			return generalPath;
		}

		// draw the vertical line
		generalPath.moveTo(0*length, -4*length);
		generalPath.lineTo(0*length, -20*length);

		switch (level) {
		case 1:
			// 5 knots, short bar
			generalPath.moveTo(0*length, -20*length);
			generalPath.lineTo(5*length, -20*length);
			break;
		case 2:
			// 10 knots, long bar
			generalPath.moveTo(0*length, -20*length);
			generalPath.lineTo(10*length, -20*length);
			break;
		case 3: // 15 knots, a short bar and a long bar
			generalPath.moveTo(0*length, -18*length);
			generalPath.lineTo(5*length, -18*length);
			generalPath.moveTo(0*length, -20*length);
			generalPath.lineTo(10*length, -20*length);
			break;
		case 4: // 20 knots, two long bars
			for (int i = 0; i < 2; ++i) {
				generalPath.moveTo(0*length, (-20 + i * 2)*length);
				generalPath.lineTo(10*length, (-20 + i * 2)*length);
			}
			break;
		case 5: // 25 knots
			for (int i = 0; i < 2; ++i) {
				generalPath.moveTo(0*length, (-20 + i * 2)*length);
				generalPath.lineTo(10*length, (-20 + i * 2)*length);
			}
			generalPath.moveTo(0*length, -16*length);
			generalPath.lineTo(5*length, -16*length);
			break;
		case 6:
			for (int i = 0; i < 3; ++i) {
				generalPath.moveTo(0*length, (-20 + i * 2)*length);
				generalPath.lineTo(10*length, (-20 + i * 2)*length);
			}
			break;
		case 7:
			for (int i = 0; i < 3; ++i) {
				generalPath.moveTo(0*length, (-20 + i * 2)*length);
				generalPath.lineTo(10*length, (-20 + i * 2)*length);
			}
			generalPath.moveTo(0*length, -14*length);
			generalPath.lineTo(5*length, -14*length);
			break;
		case 8:
			for (int i = 0; i < 4; ++i) {
				generalPath.moveTo(0*length, (-20 + i * 2)*length);
				generalPath.lineTo(10*length, (-20 + i * 2)*length);
			}
			break;
		case 9:
			for (int i = 0; i < 4; ++i) {
				generalPath.moveTo(0*length, (-20 + i * 2)*length);
				generalPath.lineTo(10*length, (-20 + i * 2)*length);
			}
			generalPath.moveTo(0*length, -12*length);
			generalPath.lineTo(5*length, -12*length);
			break;
		case 10:
			for (int i = 0; i < 5; ++i) {
				generalPath.moveTo(0*length, (-20 + i * 2)*length);
				generalPath.lineTo(10*length, (-20 + i * 2)*length);
			}
			break;
		}
		generalPath.closePath();
		return generalPath;
	}

	boolean orientationHittable = true;

	/**
	 * Returns true if the orientation marker can be hit as part of the object.
	 * By default the answer is YES.
	 */
	public boolean isOrientationHittable() {
		return orientationHittable;
	}

	/**
	 * Sets whether or not the orientation marker can be hit as part of the
	 * object.
	 */
	public void setOrientationHittable(boolean val) {
		orientationHittable = val;
	}

	public boolean hitObject(Object object, DrawInfo2D range) {
		if (getChild(object).hitObject(object, range))
			return true;
		if (!orientationHittable)
			return false;

		// now additionally determine if I was hit

		if (showOrientation && (object != null)
				&& (object instanceof Oriented2D)) {
			final double theta = ((Oriented2D) object).orientation2D();
			final double length = ((scale * (range.draw.width < range.draw.height ? range.draw.width
					: range.draw.height)) + offset); // fit in smallest
														// dimension

			// we'll always do precise hitting

			transform.setToTranslation(range.draw.x, range.draw.y);
			transform.rotate(theta);


			if (path == null) {
				path = getPath(level,length);
			}
			return transform.createTransformedShape(path).intersects(
					range.clip.x, range.clip.y, range.clip.width,
					range.clip.height);
			// break;

		}
		return false;
	}

	public boolean setSelected(LocationWrapper wrapper, boolean selected) {
		return getChild(wrapper.getObject()).setSelected(wrapper, selected);
	}

	public Inspector getInspector(LocationWrapper wrapper, GUIState state) {
		return getChild(wrapper.getObject()).getInspector(wrapper, state);
	}

	public String getName(LocationWrapper wrapper) {
		return getChild(wrapper.getObject()).getName(wrapper);
	}

	public boolean handleMouseEvent(GUIState guistate,
			Manipulating2D manipulating, LocationWrapper wrapper,
			MouseEvent event, DrawInfo2D fieldPortrayalDrawInfo, int type) {
		return getChild(wrapper.getObject()).handleMouseEvent(guistate,
				manipulating, wrapper, event, fieldPortrayalDrawInfo, type); // let
																				// someone
																				// else
																				// have
																				// it
	}
}
