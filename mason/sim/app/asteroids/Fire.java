/*
  Copyright 2009 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.asteroids;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;
import java.awt.geom.*;
import java.awt.*;
import sim.portrayal.*;

/** Fire is a simple Element which draws thrusting fire.  Used by Ship only.  */
public class Fire extends Element
    {
    private static final long serialVersionUID = 1;

    /** Creates a Fire. */
    public Fire()
        {
        GeneralPath gp = new GeneralPath();
        gp.moveTo(-1,0);
        gp.lineTo(-3,1);
        gp.lineTo(-5,0);
        gp.lineTo(-3,-1);
        gp.closePath();
        shape = gp;
        }
        
    /** Fire is red. */
    public Color getColor() { return Color.red; }
    }
