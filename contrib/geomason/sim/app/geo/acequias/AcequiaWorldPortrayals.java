package acequias;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import acequias.objects.Tile;

import sim.portrayal.DrawInfo2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.gui.ColorMap;


/*
Copyright 2006 by Sean Luke and George Mason University
Licensed under the Academic Free License version 3.0
See the file "LICENSE" for more information
 */

import sim.app.tutorial5.Band;
import sim.field.network.*;
import sim.portrayal.network.*;
import sim.portrayal.*;
import java.awt.*;
import java.util.Random;

/** A series of Portrayals associated with AcequiaWorldWithUI
 * 
 * @author Sarah Wise and Andrew Crooks
 */

class LinkPortrayal extends SimpleEdgePortrayal2D
{
	static ColorMap LinkColor = new sim.util.gui.SimpleColorMap(0, 100,
			new Color(200, 200, 200), new Color(0, 0, 0));
	
	public LinkPortrayal() {}

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		// this better be an EdgeDrawInfo2D!  :-)
		EdgeDrawInfo2D ei = (EdgeDrawInfo2D) info;
		// likewise, this better be an Edge!
		Edge e = (Edge) object;

		// our start (x,y), ending (x,y), and midpoint (for drawing the label)
		final int startX = (int)ei.draw.x;
		final int startY = (int)ei.draw.y;
		final int endX = (int)ei.secondPoint.x;
		final int endY = (int)ei.secondPoint.y;

		// draw line.
		graphics.setColor( LinkColor.getColor( (Integer) e.info ));
		graphics.drawLine (startX, startY, endX, endY);
	}

	// use the default hitObject -- don't bother writing that one, it works fine
}



class AcequiaPortrayal extends RectanglePortrayal2D {

	Color acequiaColor = new Color(100, 200, 255);

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		if (t.getAcequia() >= 0) {
			graphics.setColor(acequiaColor);
			graphics.fillRect(x, y, w, h);
		}
	}

}

class AcequiaTractPortrayal extends RectanglePortrayal2D {
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		if (t.getTract() >= 0) {
			graphics.setColor( randomColor(t.getTract()) );
			graphics.fillRect(x, y, w, h);
		}
	}

	Color randomColor( int i ){
		Random rand = new Random(i);
		return new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
	}
}

class CountiesPortrayal extends RectanglePortrayal2D {

	static ColorMap CountyColor = new sim.util.gui.SimpleColorMap(0, 30,
			new Color(255, 0, 0), new Color(0, 0, 255));

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		if (t.getCounty() >= 0) {
			graphics.setColor(CountyColor.getColor(t.getCounty()));
			graphics.fillRect(x, y, w, h);
		}
	}

}

class ElevationPortrayal extends RectanglePortrayal2D {

	static ColorMap ElevationColor = new sim.util.gui.SimpleColorMap(0, 4000,
			new Color(20, 20, 5), new Color(200, 200, 170));

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		graphics.setColor(ElevationColor.getColor(t.getElevation()));
		graphics.fillRect(x, y, w, h);
	}

}

class HydrationPortrayal extends RectanglePortrayal2D {

	static ColorMap HydrationColor = new sim.util.gui.SimpleColorMap(0, 100,
			new Color(0, 0, 0), new Color(200, 200, 255));

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		if( t.getHydration() == -1) return;
		graphics.setColor(HydrationColor.getColor(t.getHydration()));
		graphics.fillRect(x, y, w, h);
	}

}


class HydrologicalNetworkPortrayal extends RectanglePortrayal2D {

	Color hydrologyColor = new Color(50, 100, 150);

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		if (t.getHydrologicalFeature() == 1) {
			graphics.setColor(hydrologyColor);
			graphics.fillRect(x, y, w, h);
		}
	}

}

class RoadsPortrayal extends RectanglePortrayal2D {

	Color roadColor = new Color(150, 150, 80);

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		if (t.getRoad()) {
			graphics.setColor(roadColor);
			graphics.fillRect(x, y, w, h);
		}
	}

}

/** SEE HOMER ET AL */
class LandUsePortrayal extends RectanglePortrayal2D {

	Color openWater = new Color( 30, 30, 200 );
	Color developedOpenSpace = new Color(255, 230, 230);
	Color developedLowIntensity = new Color(255, 220, 200);
	Color developedMediumIntensity = new Color( 255, 150, 150);
	Color developedHighIntensity = new Color( 230, 50, 50);
	Color barren = new Color(150, 255, 200);
	Color deciduousForest = new Color( 200, 255, 210);
	Color evergreenForest = new Color( 100, 220, 100);
	Color mixedForest = new Color( 230, 255, 230);
	Color shrub = new Color( 230, 230, 200);
	Color grassland = new Color( 240, 240, 220);
	Color hay = new Color( 255, 255, 200);
	Color cultivatedCrops = new Color( 230, 200, 150);
	Color woodyWetlands = new Color( 230, 230, 255);
	Color herbaceousWetlands = new Color( 200, 200, 255);

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		if (object == null)
			return;

		Rectangle2D.Double draw = info.draw;
		final double width = draw.width * scale;
		final double height = draw.height * scale;

		final int x = (int) (draw.x - width / 2.0);
		final int y = (int) (draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);

		Tile t = (Tile) object;
		int lu = t.getLanduse();
		if( lu == 11 ) // open water
			graphics.setColor( openWater );
		else if( lu == 21 ) // developed, open space
			graphics.setColor( developedOpenSpace );
		else if( lu == 22 ) // developed, low intensity
			graphics.setColor( developedLowIntensity );
		else if( lu == 23 ) // developed, medium intensity
			graphics.setColor( developedMediumIntensity );
		else if( lu == 24 ) // developed, high intensity
			graphics.setColor( developedHighIntensity );
		else if( lu == 31 ) // barren land
			graphics.setColor( barren );
		else if( lu == 41 ) // deciduous forest
			graphics.setColor( deciduousForest );
		else if( lu == 42 ) // evergreen forest
			graphics.setColor( evergreenForest );
		else if( lu == 43 ) // mixed forest
			graphics.setColor( mixedForest );
		else if( lu == 52 ) // shrub/scrub
			graphics.setColor( shrub );
		else if( lu == 71 ) // grassland/herbaceous
			graphics.setColor( grassland );
		else if( lu == 81 ) // hay/pasture
			graphics.setColor( hay );
		else if( lu == 82 ) // cultivated crops
			graphics.setColor( cultivatedCrops );
		else if( lu == 90 ) // woody wetlands
			graphics.setColor( woodyWetlands );
		else if( lu == 95 ) // emergent herbaceous wetlands
			graphics.setColor( herbaceousWetlands );
		else // error: don't color it!
			return;

		graphics.fillRect(x, y, w, h);
	}

}

