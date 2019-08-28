/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.sweep;

import ec.util.*;
import sim.util.*;
import sim.engine.*;

import javax.swing.*;
import java.util.List;

/**
 * Created by dfreelan on 12/19/17.
 */
public class ParameterSettings 
    {
    public static final String MODEL_P = "model";
    public static final String MIN_P = "min";
    public static final String MAX_P = "max";
    public static final String INDEPENDENT_P = "independent";
    public static final String DEPENDENT_P = "dependent";
    public static final String DIVISIONS_P = "divisions";
    public static final String STEPS_P = "steps";
    public static final String MOD_P = "mod";
    public static final String COMPRESS_P = "compress";
    public static final String TRIALS_P = "trials";
    public static final String THREADS_P = "threads";
    public static final String SEED_P = "seed";
    public static final String OUT_P = "out";
    
    public Properties p;
    public int index;
    public boolean amSet = false;
    public boolean amDependent = false;
    public double min = 0.0;
    public double max = 1.0;
    public int divisions = 1;
    
    public ParameterSettings(Properties p, int index)
        {
        this.p = p;
        this.index = index;
        }
    
    public static ParameterDatabase convertToDatabase(
        ListModel propertySettings, 
        SimState model,
        int steps,
        int mod,
        int trials,
        int threads,
        long seed,
        boolean compress,
        String path
        )
        {
        StringBuilder _min = new StringBuilder("");
        StringBuilder _max= new StringBuilder("");
        StringBuilder _divisions= new StringBuilder("");
        StringBuilder _independent= new StringBuilder("");
        StringBuilder _dependent= new StringBuilder("");
        ParameterDatabase pd = new ParameterDatabase();
        
        for(int i = 0; i<propertySettings.getSize(); i++)
            {
            ParameterSettings param = (ParameterSettings) propertySettings.getElementAt(i);
            if(param.amSet) 
                {
                if (param.amDependent)
                    {
                    _dependent.append(param.getName() + " ");
                    }
                else    // independent
                    {
                    _independent.append(param.getName() + " ");
                    _min.append("" + param.min + " ");
                    _max.append("" + param.max + " ");
                    _divisions.append("" + param.divisions + " ");
                    }
                }
            }
        pd.set(new Parameter(MODEL_P), model.getClass().getName() );
        pd.set(new Parameter(MIN_P), _min.toString());
        pd.set(new Parameter(MAX_P), _max.toString());
        pd.set(new Parameter(INDEPENDENT_P), _independent.toString());
        pd.set(new Parameter(DEPENDENT_P), _dependent.toString());
        pd.set(new Parameter(DIVISIONS_P),   "" + _divisions.toString());
        pd.set(new Parameter(STEPS_P),  "" + steps  );
        pd.set(new Parameter(MOD_P),   "" + mod  );
        pd.set(new Parameter(COMPRESS_P),   "" + compress    );
        pd.set(new Parameter(TRIALS_P),   "" + trials);
        pd.set(new Parameter(THREADS_P), "" + threads);
        pd.set(new Parameter(SEED_P), "" + seed);
        pd.set(new Parameter(OUT_P), path);
        return pd;
        }

    public String getName()
        {
        return p.getName(index);
        }
    
    public String toString()
        {
        if (!amSet) 
            {
            return "<html>" + p.getName(index) + "</html>";
            }
        else if (amDependent) 
            {
            return "<html>" + p.getName(index) + "<font color='blue'>&nbsp;&nbsp;&nbsp;(DEPENDENT) </font></html>";
            }
        else // independent
            {
            return "<html>" + p.getName(index) + "<font color='green'>&nbsp;&nbsp;&nbsp;min=" + min + " max=" + max + " div= " + divisions + "</font></html>";
            }
        }
    }
