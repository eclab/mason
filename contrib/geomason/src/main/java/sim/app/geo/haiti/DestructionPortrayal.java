/*
    DestructionPortrayal.java

    $Id$
*/

package sim.app.geo.haiti;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.MutableDouble;

/* Color based on extent of damage, as defined by data sources */
class DestructionPortrayal extends RectanglePortrayal2D
{
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
        // color based on extent of damage
        MutableDouble i = (MutableDouble) object;
        int value = i.intValue();
        if (value == 51)
        {
            graphics.setColor(Color.gray);
        } else if (value == 102)
        {
            graphics.setColor(Color.green);
        } else if (value == 153)
        {
            graphics.setColor(Color.yellow);
        } else if (value == 204)
        {
            graphics.setColor(Color.orange);
        } else if (value == 255)
        {
            graphics.setColor(Color.red);
        }
        graphics.fillRect(x, y, w, h);
    }

}
