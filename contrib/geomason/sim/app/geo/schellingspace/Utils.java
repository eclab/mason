/**
 ** Utils.java
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
package schellingspace;

import java.awt.Color;
import java.awt.Graphics2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.util.geo.MasonGeometry;
import sim.util.gui.ColorMap;
import sim.util.gui.SimpleColorMap;



/**
 * Portrayal for People in the simulation. Colors them based on their
 * society.
 */
class PersonPortrayal extends GeomPortrayal
{
    private static final long serialVersionUID = 1L;
    
    Color darkBlue = new Color(50, 50, 100);
    Color darkRed = new Color(100, 50, 50);


    @Override
    /** Render the agent as a red or blue circle with a gray outline so
     * that it stands out from the background.
     *
     */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        MasonGeometry p = (MasonGeometry) object;
        Person person = (Person) p.getUserData();

//        // First draw the gray outline
//
//        double savedScale = scale;
//
//        scale *= 1.4;
//        paint = Color.GRAY;
//
//        super.draw(object, graphics, info);
//
//        scale = savedScale;

        // set paint based on Person's choice of color
        if (person.getAffiliation().equals(Person.Affiliation.RED))
        {
            paint = Color.MAGENTA;
        } else
        {
            paint = Color.CYAN;
        }

        super.draw(object, graphics, info);

//        graphics.setPaint(paint);
//
//        // set up the geometry for drawing
//        Rectangle2D.Double draw = info.draw;
//        final double width = draw.width * scale;
//        final double height = draw.height * scale;
//
//        // correct for the fact that the oval is drawn from the corner, but we
//        // want the dot centered
//        final int x = (int) (p.geometry.getCoordinate().x - width / 2.);
//        final int y = (int) (p.geometry.getCoordinate().y - height / 2.);
//        int w = (int) (width);
//        int h = (int) (height);
//
//        // without setting this back, People look terrible!
//        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                                  RenderingHints.VALUE_ANTIALIAS_OFF);
//
//        // draw centered on the origin
//        graphics.fillOval(x, y, w, h);
//
//        // draw the margin with a darker version of the same color
//        if (graphics.getPaint() == Color.RED)
//        {
//            graphics.setPaint(darkRed);
//        } else
//        {
//            graphics.setPaint(darkBlue);
//        }
//        graphics.drawOval(x, y, w, h);
    }

}



/** The portrayal used to render the wards with color based on relative
 * proportions of Red and Blue Persons within boundaries
 */
//@SuppressWarnings("restriction")
class WardPortrayal extends GeomPortrayal
{
    private static final long serialVersionUID = 1L;

    ColorMap cmap = new SimpleColorMap(0, 1., Color.blue, Color.red);


    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        SchellingGeometry poly = (SchellingGeometry) object;

        // calculate the percentage of Red Persons versus Blue Persons and choose
        // a color accordingly
        double numRed = 0;
        
        for (Person p : poly.residents)
        {
            if (p.getAffiliation().equals(Person.Affiliation.RED))
            {
                numRed++;
            }
        }

        paint = cmap.getColor(numRed / poly.residents.size());

        graphics.setPaint(paint);

        super.draw(object, graphics, info);
    }


}
