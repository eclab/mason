/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
package sim.display;

import java.io.Serializable;

public class Stat implements Serializable
    {
    private static final long serialVersionUID = 1L;

    public final Serializable data;
    public final long steps;
    public final double time;

    public Stat(Serializable _data, long _steps, double _time)
        {
        data = _data;
        steps = _steps;
        time = _time;
        }
    }
