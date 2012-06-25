/**
 ** CatchmentPortrayal.java
 **
 ** Copyright 2011 by Andrew Crooks, Joseph Harrison, Mark Coletti, Cristina Metgher
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.sickStudents;

import java.awt.Graphics2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.util.geo.MasonGeometry;
import sim.util.gui.SimpleColorMap;

public class CatchmentPortrayal extends GeomPortrayal 
{
	private static final long serialVersionUID = 6026649920581400781L;

	SimpleColorMap colorMap = null;

    // FIXME: this should be using Mason's RNG
	public double proportionSick = Math.random();
	SickStudentsModel model;
	
	public CatchmentPortrayal(SimpleColorMap map, SickStudentsModel model) 
	{
		super(true); 
		colorMap = map;
		this.model = model;
	}
	
    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
    	MasonGeometry mg = (MasonGeometry)object;
    	Integer num = mg.getIntegerAttribute("SCHOOL_NUM");
    	School s = model.schoolMap.get(num);

        // FIXME: why is this occasionally null?
        if (s != null)
        {
            proportionSick = s.getProportionOfSickStudents();
            paint = colorMap.getColor(proportionSick);
        }

        super.draw(object, graphics, info);    
    }
}
