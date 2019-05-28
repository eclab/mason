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
    /** Draw a the given object with an origin at (info.draw.x, info.draw.y),
        and with the coordinate system scaled by so that 1 unit is in the x and
        y directions are equal to info.draw.width and info.draw.height respectively
        in pixels.  The rectangle given by info.clip specifies the only region in which
        it is necessary to draw.   If info.precise is true, try to draw using real-valued
        high-resolution drawing rather than faster integer drawing.  It is possible that object
        is null.  The location of the object in the field may (and may not) be stored in
        info.location.  The form of that location varies depending on the kind of field used. */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info);
    }
