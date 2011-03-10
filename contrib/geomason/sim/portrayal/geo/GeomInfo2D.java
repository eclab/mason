package sim.portrayal.geo; 


import java.awt.Graphics2D;
import java.awt.geom.*;
import sim.field.geo.GeomVectorField;
import sim.portrayal.*;
import sim.util.*; 
import sim.util.geo.*; 
import java.awt.image.*;


public class GeomInfo2D extends DrawInfo2D 
{
    public AffineTransform transform; 

    public GeomInfo2D(DrawInfo2D info, AffineTransform t) 
    {
	super(info); 
	transform = t; 
    }

}