/*
 * GeomValuedFieldPortrayal.java
 *
 * $Id: GeomValuedFieldPortrayal.java,v 1.1 2010-04-02 17:12:37 mcoletti Exp $
 */

package sim.portrayal.geo;

import java.awt.Color;
import sim.util.geo.GeomWrapper;
import sim.util.gui.ColorMap;
import sim.util.gui.SimpleColorMap;

/** Displays GeomWrappers
 *
 * Uses GeomWrapper.doubleValue() to look up color table value for each
 * instance.
 *
 * @author mcoletti
 */
public class GeomValuedFieldPortrayal  extends GeomFieldPortrayal {
    
    public ColorMap colorMap = new SimpleColorMap();
    
    public ColorMap getMap() { return colorMap; }
    public void setMap(ColorMap m) { colorMap = m; }


    public GeomValuedFieldPortrayal() { super(); }
    public GeomValuedFieldPortrayal(ColorMap cm)
    {
        super();
        colorMap = cm;
    }

    /** compute color based on color LUT computed from gw.getValue()
	 *
     * @param gw 
     * @return Color mapped from gw.doubleValue() as dictated by color table
     */
    protected Color lookupColor(GeomWrapper gw)
    {        
        Color color = null; // Color of current rendered geometry based on
                            // color table lookup of its double value

        double value = gw.doubleValue();
		
        if (this.colorMap.validLevel(value))
            color = this.colorMap.getColor(value);
        else
            color = this.colorMap.getColor(this.colorMap.defaultValue());
        
        return color;
    }

}
