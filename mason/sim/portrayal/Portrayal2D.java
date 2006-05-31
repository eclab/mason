/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.awt.*;

/**
   The basic 2D portrayal interface.  It adds the draw method in the 2D context.
*/

public interface Portrayal2D extends Portrayal
    {
    /** Draw a portrayed object centered at the origin in info, and
        with the given scaling factors.  It is possible that object
        is null. */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info);
    }
