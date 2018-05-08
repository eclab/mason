/**
 ** CatchmentPortrayal.java
 **
 ** Copyright 2011 by Joseph Harrison, Mark Coletti, Cristina Metgher, Andrew Crooks
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 **/
package sickStudents;

import java.awt.Graphics2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.util.geo.MasonGeometry;
import sim.util.gui.SimpleColorMap;

public class CatchmentPortrayal extends GeomPortrayal 
{
	private static final long serialVersionUID = 6026649920581400781L;

	SimpleColorMap colorMap = null;

	public double proportionSick;
	SickStudentsModel model;
	
	public CatchmentPortrayal(SimpleColorMap map, SickStudentsModel model) 
	{
		super(true); 
		colorMap = map;
		this.model = model;
		proportionSick = model.random.nextDouble();
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
