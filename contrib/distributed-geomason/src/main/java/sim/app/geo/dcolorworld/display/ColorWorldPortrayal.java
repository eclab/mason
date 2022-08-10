package sim.app.geo.dcolorworld.display;

import java.awt.Graphics2D;

import sim.app.geo.dcolorworld.DColorWorld;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.util.gui.SimpleColorMap;

/**
 *  We override GeomPortrayal so we can change the paint color for each voting district based on 
 *  how many agents are currently inside the district.  After setting the paint color, GeomPortrayal
 *  handles drawing in the standard GeoMASON way. 
 *
 */
public class ColorWorldPortrayal extends GeomPortrayal
{
	private static final long serialVersionUID = 6026649920581400781L;

	SimpleColorMap colorMap = null; 
	
	public ColorWorldPortrayal(SimpleColorMap map) 
	{
		super(true); 
		colorMap = map; 
	}
	
    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
    	CountingGeomWrapper gm = (CountingGeomWrapper)object;
    	paint = colorMap.getColor(gm.numAgentsInGeometry(this.world));
        super.draw(object, graphics, info);    
    }
}
