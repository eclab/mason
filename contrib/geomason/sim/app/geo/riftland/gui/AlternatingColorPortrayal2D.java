package riftland.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.simple.RectanglePortrayal2D;

/**
 * A portrayal that displays each object found in a consistent color, even if
 * there are multiple instances of the same object.
 * 
 * @author Eric 'Siggy' Scott
 */
public class AlternatingColorPortrayal2D extends RectanglePortrayal2D
{
	private static final long serialVersionUID = 1L;
        private final Map<Object, Color> colorMap = new HashMap();
        private final static Color[] colorSet = new Color[] { Color.BLUE, Color.ORANGE, Color.LIGHT_GRAY, Color.WHITE, Color.YELLOW, Color.CYAN, Color.RED, Color.GREEN, Color.BLACK, Color.PINK, Color.DARK_GRAY, Color.MAGENTA, Color.GRAY };
	
	public AlternatingColorPortrayal2D()
        {
	}

	@Override
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            Color c;
            if (colorMap.containsKey(object))
                c = colorMap.get(object);
            else
            {
                c = colorSet[colorMap.size()%colorSet.length];
                colorMap.put(object, c);
            }
            paint = c; // super.draw will call graphics.setPaint(paint);
            super.draw(object, graphics, info);
        }
}
