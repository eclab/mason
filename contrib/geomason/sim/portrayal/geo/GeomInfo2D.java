package sim.portrayal.geo; 


import java.awt.geom.*;
import sim.portrayal.*;

public class GeomInfo2D extends DrawInfo2D 
{
    public AffineTransform transform; 

    public GeomInfo2D(DrawInfo2D info, AffineTransform t) 
    {
	super(info); 
	transform = t; 
    }

}