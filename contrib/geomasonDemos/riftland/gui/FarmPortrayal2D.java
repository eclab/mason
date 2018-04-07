package riftland.gui;

import java.awt.Color;
import java.awt.Graphics2D;

import riftland.parcel.GrazableArea;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;

public class FarmPortrayal2D extends RectanglePortrayal2D
{
	private static final long serialVersionUID = 1L;
	
	public FarmPortrayal2D() {
		super(new Color(0, 128, 0));	// forest green
	}

	@Override
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (!(object instanceof GrazableArea))
			return;
		
		GrazableArea area = (GrazableArea)object;
		scale = area.getFarmedLandInHectares() / 100.0;
		scale = Math.sqrt(scale); // so the area varies linearly with the percentage of farmed land
		info.precise = true;
		super.draw(object, graphics, info);
	}


}
