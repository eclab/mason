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
	public double proportionSick = Math.random();
	SickStudentsModel model;
	
	public CatchmentPortrayal(SimpleColorMap map, SickStudentsModel model) 
	{
		super(true); 
		colorMap = map;
		this.model = model;
	}
	
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
    	MasonGeometry mg = (MasonGeometry)object;
    	Integer num = (Integer)SickStudentsModel.getAttribute(mg, "SCHOOL_NUM");
    	School s = model.schoolMap.get(num);
    	proportionSick = s.getProportionOfSickStudents();
    	paint = colorMap.getColor(proportionSick);
        super.draw(object, graphics, info);    
    }
}
