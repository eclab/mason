/*
 * HerderGridPortrayal2D.java
 * 
 * $Id: HerderGridPortrayal2D.java 1639 2013-02-20 01:01:33Z escott8 $
 * 
 */

package sim.app.geo.riftland.gui;

import java.awt.Graphics2D;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.grid.ObjectGridPortrayal2D;

/** Renders all the herders on the grid world
 *
 * @author mcoletti
 */
public class HerderGridPortrayal2D extends ObjectGridPortrayal2D {

    @Override
    public
    void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        //super.draw(object, graphics, info);
        
        if ( object == null)
        {
            return;
        }
        
        GrazableArea parcel = (GrazableArea) object;
        
        if (! parcel.getHerds().isEmpty())
        {
            System.out.println("Parcel has herders");
        }
    }
    
    

}
