/**
 * SnailTrailPortrayal.java
 *
 * $Id: SnailTrailPortrayal.java 1843 2013-05-25 01:34:28Z escott8 $
 * 
 */
package sim.app.geo.riftland.gui;

import java.awt.*;
import java.util.Iterator;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.household.Herding;
import sim.app.geo.riftland.parcel.Parcel;
import sim.portrayal.*;
import sim.util.gui.ColorMap;
import sim.util.gui.SimpleColorMap;

/**
 * Used to render a herder's trajectory.
 *
 */
public class SnailTrailPortrayal extends SimplePortrayal2D
{

    /**
     * XXX What are these for?
     */
    private Color minColor, maxColor;

    /**
     * XXX What is this for?
     */
    private ColorMap previousColorMap = null;

    /**
     * XXX What is this for?  And why the magic number?
     */
    private int previousMaxTrail = -1;

    /**
     * XXX I *think* that if this is true that herder trajectory detail is to
     * be rendered.  As in explicitly draw the nodes along the line segment.
     */
    private boolean drawIntermediaryPoints;


    /**
     *
     * @param color to draw the herder trajectory
     * @param drawIntermediaryPoints is true if trajectory detail to be rendered
     */
    public SnailTrailPortrayal(Color color, boolean drawIntermediaryPoints)
    {
        this(color, color, drawIntermediaryPoints);
    }


    /**
     * @param maxColor of herder trajectory
     * @param minColor of herder trajectory
     * @param drawIntermediaryPoints is true if trajectory detail to be rendered
     */
    public SnailTrailPortrayal(Color maxColor, Color minColor, boolean drawIntermediaryPoints)
    {
        this.maxColor = maxColor;
        this.minColor = minColor;
        this.drawIntermediaryPoints = drawIntermediaryPoints;
    }

    
    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        Herding herding = (Herding) object;

        int maxTrailLength = herding.getMaxTrailLength();


        // Re-calibrate the color map if the movement history is less than the
        // maximum to be remembered.
        if (previousMaxTrail != maxTrailLength && maxTrailLength != 0)
        {
            previousMaxTrail = maxTrailLength;
            previousColorMap = new SimpleColorMap(1, maxTrailLength, minColor, maxColor);
        }

        double cellSizeWidth = info.draw.width;
        double cellSizeHeight = info.draw.height;

        int iCellSizeWidth = (int) cellSizeWidth;
        int iCellSizeHeight = (int) cellSizeHeight;
        int iCellSizeHalfWidth = (int) (cellSizeWidth / 2);
        int iCellSizeHalfhHeight = (int) (cellSizeHeight / 2);


        int currentParcelGridX = 0;
        int currentParcelGridY = 0;
        Iterator<GrazableArea> trailIterator = herding.trailIterator();

        Parcel currentParcel = herding.getLocation();
        currentParcelGridX = currentParcel.getX();
        currentParcelGridY = currentParcel.getY();


        double currentParcelGraphicsX = info.draw.x;
        double currentParcelGraphicsY = info.draw.y;

        int segmentStartGraphicsX = (int) currentParcelGraphicsX;
        int segmentStartGraphicsY = (int) currentParcelGraphicsY;

        int segment = 0;
        

        while (trailIterator.hasNext())
        {
            Parcel segmentEndParcel = trailIterator.next();

            int segmentEndGridX = segmentEndParcel.getX();
            int segmentEndGridY = segmentEndParcel.getY();

            int segmentEndGraphicsX = (int) (currentParcelGraphicsX
                    + (segmentEndGridX - currentParcelGridX) * cellSizeWidth);
            int segmentEndGraphicsY = (int) (currentParcelGraphicsY
                    + (segmentEndGridY - currentParcelGridY) * cellSizeHeight);

            if (drawIntermediaryPoints)
            {
                graphics.drawOval(segmentEndGraphicsX - iCellSizeHalfWidth,
                        segmentEndGraphicsY - iCellSizeHalfhHeight,
                        iCellSizeWidth/2,
                        iCellSizeHeight/2);
            }


            segment++;
            graphics.setColor(previousColorMap.getColor(segment));

            //set color of each segment separately?
//      		graphics.setColor(trailColor);
            graphics.drawLine(segmentStartGraphicsX,
                    segmentStartGraphicsY,
                    segmentEndGraphicsX,
                    segmentEndGraphicsY);

            segmentStartGraphicsX = segmentEndGraphicsX;
            segmentStartGraphicsY = segmentEndGraphicsY;
        }

    }
}
