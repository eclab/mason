/*
    PeoplePortrayal.java

    $Id$
*/

package sim.app.geo.haiti;



import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.gui.ColorMap;
import sim.util.gui.SimpleColorMap;


/**
 */
class PeoplePortrayal extends RectanglePortrayal2D
{
    // colormap for people in tile levels, which varies between 0 and 30
    //	public static ColorMap PeopleColor = new sim.util.gui.SimpleColorMap(
    //			0, 30, new Color(0, 0, 0, 0), new Color(255, 0, 0, 255));
    public static ColorMap PeopleColor = new SimpleColorMap(0, HaitiFood.maximumDensity, new Color(0, 0, 255, 200), new Color(255, 0, 0, 200));
    private static final long serialVersionUID = 1L;


    Color personColor = new Color(200, 100, 100, 20);


    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        if (object == null)
        {
            return;
        }
        Rectangle2D.Double draw = info.draw;
        final double width = draw.width * scale;
        final double height = draw.height * scale;
        final int x = (int) (draw.x - width / 2.0);
        final int y = (int) (draw.y - height / 2.0);
        final int w = (int) (width);
        final int h = (int) (height);
        graphics.setColor(personColor);
        graphics.fillRect(x, y, w, h);
    }

}
/*
class DensityPortrayal  extends RectanglePortrayal2D {
// colormap for people in tile levels, which varies between 0 and 30
//	public static ColorMap PeopleColor = new sim.util.gui.SimpleColorMap(
//			0, 30, new Color(0, 0, 0, 0), new Color(255, 0, 0, 255));
public static ColorMap PeopleColor = new sim.util.gui.SimpleColorMap(
0, HaitiFood.maximumDensity, new Color(0, 0, 255, 150), new Color(255, 0, 0, 150));
Color personColor = new Color(200, 100, 100, 10);
public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
{
if(object == null) return;
scale = 100;
Rectangle2D.Double draw = info.draw;
final double width = draw.width*scale;
final double height = draw.height*scale;
final int x = (int)(draw.x - width / 2.0);
final int y = (int)(draw.y - height / 2.0);
final int w = (int)(width);
final int h = (int)(height);
graphics.setColor( personColor );
graphics.fillRect(x,y,w,h);
}
}
 */
