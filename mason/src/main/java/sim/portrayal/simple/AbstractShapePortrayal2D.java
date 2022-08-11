/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import java.awt.*;
import sim.portrayal.*;

public class AbstractShapePortrayal2D extends SimplePortrayal2D
    {
    public Paint paint;
    public double scale;
    public boolean filled;

    // New
    public Stroke stroke;
    public Paint strokePaint;
    public Paint fillPaint;
    protected static final Stroke defaultStroke = new BasicStroke();
    }
