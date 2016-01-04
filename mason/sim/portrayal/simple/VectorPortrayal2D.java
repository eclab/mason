/*
  Copyright 2015 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.display.*;
import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal.grid.*;
import java.awt.*;
import sim.util.*;
import sim.util.gui.*;

/**
	VectorPortrayal2D draws vectors in grids.  It is designed to work with either DoubleGrid2D or ObjectGrid2D, though
	it will work with DenseGrid2D and SparseGrid2D as well.
	
	<p>VectorPortrayal2D works in one of two modes:
	
	<ul>
	<li><b>Number mode</b>.  Here, VectorPortrayal2D takes up to <i>three</i> DoubleGrid2D fields.  One field specifies
		the <i>orientation</i> (in radians) of the vector at a given location, one field specifies its <i>scale</i> (size), and one field
		specifies its <i>color</i>.  Ideally, the three fields would be the same dimension.  You can omit one or two of these
		fields (setting them to null) but not all three.  If you chose to include the color field, you must also include a ColorMap
		which maps the field's value to a color.
		
		<p>You specify these grids when constructing the VectorPortrayal2D.  Additionally of course you will assign the VectorPortrayal
		as a portrayal for some ValueGridPortrayal2D attached to a grid of some sort.  This grid will be ignored as far as drawing goes,
		but will be the grid used for all other purposes (such as hit-testing) and thus will be the grid that will determine the inspector
		that pops up when you double-click on a location.  You could naturally use one of your three grids (perhaps the orientation grid)
		as the attached grid as well.
		
	<li><b>Object mode</b>.  Here, no DoubleGrid2D fields are provided at all, nor a ColorMap.  In this case, VectorPortrayal2D is 
		intended to be registered with an ObjectGridPortrayal2D (or in theory a DenseGridPortrayal2D or a SparseGridPortrayal2D).
		The VectorPortrayal2D will simply portray object from their underlying grids which is given to it.  This is done by querying
		the object to see if it implements the Oriented2D interface (to get the vector orientation), the Scaled2D interface (to get
		the vector scale), and the Valuable interface (to get the color).  Instead of the Valuable interface, an object may
		also subclass from Number.  If the color is to be queried, a ColorMap must also be provided, else it may be null.
		
		<p>Object mode also supports Double2D, Double3D, Int2D, and Int3D objects.  Here, the x and y values define the vector proper
		and are converted into appropriate orientation and scale values (which isn't fast, as it requires atan2!).  For Double3D
		and Int3D, the z value is converted into the color.
	</ul>
	
	<p>You can set the scale of the VectorPortrayal2D manually, in which case this value is multiplied by the value provided by the
	underlying object to get the final scale of the vector.  You can also specify a vector shape, one of the OrientedPortrayal2D shapes
	(since VectorPortrayal2D is its subclass).
	
	<p>VectorPortrayal2D is a subclass of OrientedPortrayal2D mostly because it's convenient for it to use OrientedPortrayal's shapes
	and auto-rotation.  OrentedPortrayal2D has other random features which don't make a whole lot of sense to use in this context, so we
	suggest you not play with them unless you absolutely must.  This inludes mucking with the "offset" instance variable, calling 
	setOrientationShowing(), or changing the "child" instance variable.  
*/

public class VectorPortrayal2D extends OrientedPortrayal2D
    {
    DoubleGrid2D sizeGrid;
    DoubleGrid2D orientationGrid;
    DoubleGrid2D colorGrid;
    ColorMap map;
    double orientation;
    
    public static final int SHAPE_LINE_T = -100;
    
    /** Creates a VectorPortrayal2D which responds to objects which are
    	Oriented2D and/or Scaled2D.  These define the orientation and
    	scaling of the vector respectively. The vector color is white,
    	and its shape is a line arrow.  */
    public VectorPortrayal2D()
    	{
    	this(SHAPE_LINE_ARROW, null, null, null, null);
    	}
    
    /** Creates a VectorPortrayal2D which responds to objects which are
    	Oriented2D and/or Scaled2D.  These define the orientation and
    	scaling of the vector respectively. The vector color is white,
    	and is of the shape specified.  */
    public VectorPortrayal2D(int shape)
    	{
    	this(shape, null, null, null, null);
    	}
    	
    /** Creates a VectorPortrayal2D which responds to objects which are
    	Valuable (or a Number), Oriented2D, and/or Scaled2D.  These define the color, orientation and
    	scaling of the vector respectively. The vector color is defined by the map
    	provided, mapping the Valuable interface (or Number.doubleValue()).  The vector shape is a line arrow. 
    	If the map is null, this constructor operates like new VectorPortrayal2D().
    	*/
    public VectorPortrayal2D(ColorMap map)
    	{
    	this(SHAPE_LINE_ARROW, null, null, null, map);
    	}
    
    /** Creates a VectorPortrayal2D which responds to objects which are
    	Valuable (or a Number), Oriented2D, and/or Scaled2D.  These define the color, orientation and
    	scaling of the vector respectively. The vector color is defined by the map
    	provided, mapping the Valuable interface (or Number.doubleValue()).  The vector shape the shape provided.  
    	If the map is null, this constructor operates like new VectorPortrayal2D(shape).
    	*/
    public VectorPortrayal2D(int shape, ColorMap map)
    	{
    	this(shape, null, null, null, map);
    	}
    
 	/** Creates a VectorPortrayal with an orientation grid.  If this grid is null, this constructor operates the same as
 		new VectorPortrayal2D().
 		The shape will be a white line arrow.  */
    public VectorPortrayal2D(DoubleGrid2D orientationGrid)
    	{
    	this(SHAPE_LINE_ARROW, orientationGrid, null, null, null);
    	}
     
 	/** Creates a VectorPortrayal with an orientation grid and size grid.  Either of these grids can be null,
 		and so won't be used.  If both grids are null, this constructor operates the same as
 		new VectorPortrayal2D().  The shape will be a white line arrow.  */
    public VectorPortrayal2D(DoubleGrid2D orientationGrid, DoubleGrid2D sizeGrid)
    	{
    	this(SHAPE_LINE_ARROW, orientationGrid, sizeGrid, null, null);
    	}
 
 	/** Create a VectorPortrayal with a provided shape (see OrientedPortrayal2D), and with an orientation, size, and color grid,
 		plus a ColorMap for the color grid.  Any of these grids can be null, and so won't be used.  However, if the color grid
 		is null, the ColorMap must be also null (and vice versa).  If all three grids are null, this constructor operates
		the same as new VectorPortrayal(shape).  */
    public VectorPortrayal2D(int shape, DoubleGrid2D orientationGrid, DoubleGrid2D sizeGrid, DoubleGrid2D colorGrid, ColorMap map)
    	{
    	super(new SimplePortrayal2D());
    	this.sizeGrid = sizeGrid;
    	this.orientationGrid = orientationGrid;
    	this.colorGrid = colorGrid;
    	this.map = map;
    	this.paint = Color.white;
    	setShape(shape);
    	if (!(	(map == null && colorGrid == null) ||
    			(map != null && colorGrid != null) ||
    			(colorGrid == null && orientationGrid == null && sizeGrid == null)))
    		throw new IllegalArgumentException("Either the Map or the Color Grid must both be null, or neither null");
    	}
 
     public double getOrientation(Object object, DrawInfo2D info)
        {
        return orientation;
        }

	/** Override this method to change the scale of a value to a new one.
		The default version of this method multiples the value by the 
		predefined object scale (this.scale). */
    public double filterScale(double objectScale)
    	{
    	return this.scale * objectScale;
    	}
    
    public void setShape(int val) { if (val == SHAPE_LINE_T) { shape = val; path = null; } else super.setShape(val); }
    
    boolean usesExactOrientation = false;
    public boolean getUsesExactOrientation() { return usesExactOrientation; }
    public void setUsesExactOrientation(boolean val) { usesExactOrientation = val; }
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
		if (object==null) return;  // bah

		// we will be temporarily modifying the scale and paint prior to calling super.draw(...)
		// so we save the old versions here in order to restore them later.
		double oldscale = this.scale;
		Paint oldPaint = this.paint;

		
		// orientation, scale, and color numerical values, and whether
		// these values were set or defaults should be used instead
		double o = 0;
		double s = 0;
		double c = 0;
		boolean oset = false;
		boolean sset = false;
		boolean cset = false;
			
			
		// compute the numerical values
        if (sizeGrid == null && orientationGrid==null && colorGrid == null)
        	{
        	if (object instanceof Scaled2D || object instanceof Oriented2D || object instanceof Valuable || object instanceof Number)
        		{
				if (object instanceof Number)
					{
					c = ((Number)object).doubleValue();
					cset = true;
					}
				else if (object instanceof Valuable)
					{
					c = ((Valuable)object).doubleValue();
					cset = true;
					}
				else cset = false;
				
				if (object instanceof Scaled2D)
					{
					s = ((Scaled2D)object).getScale2D();
					sset = true;
					}
				else sset = false;
			
				if (object instanceof Oriented2D)
					{
					o = ((Oriented2D)object).orientation2D();
					oset = true;
					}
				else oset = false;
				}
			else if (object instanceof Double2D)
				{
				Double2D num = (Double2D) object;
				o = (usesExactOrientation || info.precise ? Math.atan2(num.y, num.x) : fastAtan2(num.y, num.x));  // ugh atan2
				s = Math.sqrt(num.x * num.x + num.y * num.y);  // ugh sqrt
				oset = true;
				sset = true;
				cset = false;
				}
			else if (object instanceof Double3D)
				{
				Double3D num = (Double3D) object;
				o = (usesExactOrientation || info.precise ? Math.atan2(num.y, num.x) : fastAtan2(num.y, num.x));  // ugh atan2
				s = Math.sqrt(num.x * num.x + num.y * num.y);  // ugh sqrt
				c = num.z;
				oset = true;
				sset = true;
				cset = true;
				}
			else if (object instanceof Int2D)
				{
				Int2D num = (Int2D) object;
				o = (usesExactOrientation || info.precise ? Math.atan2(num.y, num.x) : fastAtan2(num.y, num.x));  // ugh atan2
				s = Math.sqrt(num.x * num.x + num.y * num.y);  // ugh sqrt
				oset = true;
				sset = true;
				cset = false;
				}
			else if (object instanceof Int3D)
				{
				Int3D num = (Int3D) object;
				o = (usesExactOrientation || info.precise ? Math.atan2(num.y, num.x) : fastAtan2(num.y, num.x));  // ugh atan2
				s = Math.sqrt(num.x * num.x + num.y * num.y);  // ugh sqrt
				c = num.z;
				oset = true;
				sset = true;
				cset = true;
				}
			else return; // what the... ?
        	}
        else
        	{
			MutableInt2D d = ((MutableInt2D)(info.location));
			
			if (map != null)
				{
				c = colorGrid.field[d.x][d.y];
				cset = true;
				}
			else cset = false;
			
			if (sizeGrid != null)
				{
				s = sizeGrid.field[d.x][d.y];
				sset = true;
				}
			else sset = false;

			if (orientationGrid != null)
				{
				o = orientationGrid.field[d.x][d.y];
				oset = true;
				}
			else oset = false;
			}
		
		
		// set orientation, scale, and color
		if (oset)
			{
			this.orientation = o;
			}
		else this.orientation = Double.NaN;
		
		if (sset)
			{
			this.scale = 0.5 * filterScale(s);
			if (this.scale < 0 || this.scale != this.scale ) // (NaN)
				this.scale = 0;
			}
		else this.scale = oldscale * 0.5;
		
		if (cset && map != null)
			{
			paint = map.getColor(c);
			}

			
		// draw
		if (this.scale > 0)
			{
			if (shape == SHAPE_LINE_T)
				{
				if (info.precise)
					{
					
					}
				}
			else 
				super.draw(object, graphics, info);
			}
			
		// restore scale and paint
		this.paint = oldPaint;  // this is probably not necessary, since we don't use it in any way
		this.scale = oldscale;
        }

	/** Returns true if any part of the 1x1 square surrounding the VectorPortrayal is hit (as opposed
		to the shape itself. */
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        final double width = range.draw.width;
        final double height = range.draw.height;
        return( range.clip.intersects( range.draw.x-width/2, range.draw.y-height/2, width, height ) );
        }
    
    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        // static inner classes don't need serialVersionUIDs
        FieldPortrayal portrayal = wrapper.getFieldPortrayal();
        if (portrayal instanceof ValueGridPortrayal2D)
        	{
	        if (((ValueGridPortrayal2D) portrayal).getField() instanceof DoubleGrid2D)
	            return sim.portrayal.Inspector.getInspector(new ValuePortrayal2D.DoubleFilter(wrapper), state, "Properties");
	        else // IntGrid2D
	            return sim.portrayal.Inspector.getInspector(new ValuePortrayal2D.IntFilter(wrapper) ,state, "Properties");
            }
        else return super.getInspector(wrapper, state);
        }


    // pretty accurate, 13x the speed of Math.Math.atan2(x)
    static final double M_PI_2 = Math.PI / 2;
    static final double C = (1 + Math.sqrt(17.0)) / 8.0;
    static final double C_P_1 = (C + 1);
    double fastAtan1(double x)
        {
        double a = Math.abs(x);
        double aa = a * a;
        double aaa = aa * a;
        return (x < 0 ? -1 : 1) * M_PI_2 * (C * a + aa + aaa) / (1 + C_P_1 * a + C_P_1 * aa + aaa);
        }

    // See http://en.wikipedia.org/wiki/Atan2 for conversion to atan
    // about 9x the speed of Math.atan2(y,x)
    double fastAtan2(double y, double x)
        {
        if (x > 0)
            {
            return fastAtan1(y / x);
            }
        else if (x < 0)
            {
            if (y >= 0)
                return fastAtan1(y/x) + Math.PI;
            else
                return fastAtan1(y/x) - Math.PI;
            }
        else // x == 0
            {
            if (y > 0) 
                return Math.PI / 2;
            else if (y < 0) 
                return 0 - Math.PI / 2;
            else // (y==0)
                return 0;  // technically this is undefined but Java returns 0
            }
        }
    }
