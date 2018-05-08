/**
 ** SleuthPortrayals.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sleuth;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.simple.RectanglePortrayal2D;


/////////////////////
// PORTRAYALS FOR SLEUTHWORLD VISUALIZATION
/////////////////////

// slope-based portrayal
class SlopePortrayal extends RectanglePortrayal2D
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

        Tile t = (Tile) object;

        // only draw areas for which we have data
        if (t.slope != -9999)
        {
            graphics.setColor(SleuthWorldWithUI.getSlopeColor().getColor(t.slope));
            graphics.fillRect(x, y, w, h);
        }
    }

}


// hillshade-based portrayal

class HillshadePortrayal extends RectanglePortrayal2D
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

        Tile t = (Tile) object;

        // only draw areas for which we have data
        if (t.hillshade != -9999)
        {
            graphics.setColor(SleuthWorldWithUI.getHillshadeColor().getColor(t.hillshade));
            graphics.fillRect(x, y, w, h);
        }
    }

}


// exclusion-based portrayal

class ExcludedPortrayal extends RectanglePortrayal2D
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

        Tile t = (Tile) object;
        if (t.excluded)
        {
            graphics.setColor(Color.black);
        } else
        {
            graphics.setColor(Color.white);
        }
        graphics.fillRect(x, y, w, h);
    }

}

// urban-based portrayal


class OriginalUrbanPortrayal extends RectanglePortrayal2D
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

        Tile t = (Tile) object;
        if (t.urbanOriginally)
        {
            graphics.setColor(Color.blue);
            graphics.fillRect(x, y, w, h);
        }
    }

}

// transport-based portrayal


class TransportPortrayal extends RectanglePortrayal2D
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

        Tile t = (Tile) object;
        if (t.transport != -9999)
        {
            graphics.setColor(Color.black);
            graphics.fillRect(x, y, w, h);
        }
    }

}

// landuse-based portrayal


class LandusePortrayal extends RectanglePortrayal2D
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

        Tile t = (Tile) object;
        if (t.landuse == 1)
        {
            graphics.setColor(Color.orange);
        } else if (t.landuse == 2)
        {
            graphics.setColor(Color.yellow);
        } else if (t.landuse == 3)
        {
            graphics.setColor(Color.blue);
        } else if (t.landuse == 4)
        {
            graphics.setColor(Color.green);
        } else
        {
            return; // landuse is not provided for this Tile
        }
        graphics.fillRect(x, y, w, h);
    }

}

// urban growth-based portrayal


class GrowingUrbanZonesPortrayal extends RectanglePortrayal2D
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

        Tile t = (Tile) object;
        if (t.urbanized)
        {
            graphics.setColor(Color.red);
            graphics.fillRect(x, y, w, h);
        }
    }

}
