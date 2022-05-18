/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;

public class DESPortrayalParameters
    {
    private static final long serialVersionUID = 1;
    
    public static final double DEFAULT_PORTRAYAL_SCALE = 10.0;
    public static final double CIRCLE_RING_SCALE = 1.5;
    static double portrayalScale = DEFAULT_PORTRAYAL_SCALE;

    public static double getPortrayalScale() { return portrayalScale; }
    public static void setPortrayalScale(double scale) { portrayalScale = scale; }
    
    static Class baseImageClass = DESPortrayalParameters.class;
    
    public static void setImageClass(Class cls) { baseImageClass = cls; }
    public static Class getImageClass() { return baseImageClass; }
    }       
        
