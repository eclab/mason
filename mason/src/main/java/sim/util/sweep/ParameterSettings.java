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
                else	// independent
                	{
		            _independent.append(param.getName() + " ");
					_min.append("" + param.min + " ");
					_max.append("" + param.max + " ");
					_divisions.append("" + param.divisions + " ");
                	}
                }
            }
        pd.set(new Parameter("model"), model.getClass().getName() );
        pd.set(new Parameter("min"), _min.toString());
        pd.set(new Parameter("max"), _max.toString());
        pd.set(new Parameter("independent"), _independent.toString());
        pd.set(new Parameter("dependent"), _dependent.toString());
        pd.set(new Parameter("divisions"),   "" + _divisions.toString());
        pd.set(new Parameter("steps"),  "" + steps  );
        pd.set(new Parameter("mod"),   "" + mod  );
        pd.set(new Parameter("compress"),   "" + compress    );
        pd.set(new Parameter("trials"),   "" + trials);
        pd.set(new Parameter("threads"), "" + threads);
        pd.set(new Parameter("seed"), "" + seed);
        pd.set(new Parameter("out"), path);
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
