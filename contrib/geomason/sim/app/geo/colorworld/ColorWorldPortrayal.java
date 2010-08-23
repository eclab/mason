package sim.app.geo.colorworld;

import java.awt.Graphics2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.*; 
import sim.util.gui.*; 

public class ColorWorldPortrayal extends GeomPortrayal {

	private static final long serialVersionUID = 6026649920581400781L;

	SimpleColorMap colorMap = null; 
	
	public ColorWorldPortrayal(SimpleColorMap map) 
	{
		super(true); 
		colorMap = map; 
	}
	
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
    	CountingGeomWrapper gm = (CountingGeomWrapper)object; 
        paint = colorMap.getColor(gm.numAgentsInGeometry());
        super.draw(object, graphics, info); 
    }
}
