/*
    CentersPortrayal.java

    $Id$
*/

package sim.app.geo.haiti;



import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.simple.RectanglePortrayal2D;


/**
 */
class CentersPortrayal extends RectanglePortrayal2D
{
    private static final long serialVersionUID = 1L;

    
    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        if (object == null)
        {
            return;
        }
        scale = 10;
        Rectangle2D.Double draw = info.draw;
        final double width = scale;
        final double height = scale;
        final int x = (int) (draw.x - width / 2.0);
        final int y = (int) (draw.y - height / 2.0);
        final int w = (int) (width);
        final int h = (int) (height);
        Center c = (Center) object;
        if (c != null)
        {
            graphics.setColor(Color.blue);
            graphics.fillRect(x, y, w, h);
        }
    }

}
