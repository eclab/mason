package sim.util.opt;

import sim.util.*;

public class VariableSettings {
    public Properties p;
    public int index;
    public boolean amSet = false;
    public double min = 0.0;
    public double max = 1.0;
    public VariableSettings(Properties p, int index)
        {
        this.p = p;
        this.index = index;
        //TODO Set min/max based on p.getDomain(): expected sim.util.Interval, but instead getting null
        }
    
    /**
     * 
     * @return the property name
     */
    public String getName()
        {
        return p.getName(index);
        }

    /**
     * 
     * @return <code>this</code> property's styled values
     */
    public String getSettings()
        {
        if (!amSet) 
            {
            return "";
            }
        else
            {
            return "<html>" + "<font color='green'>&nbsp;&nbsp;&nbsp;min=" + min + " max=" + max + "</font></html>";// + " div= " + divisions + "</font></html>";
            }
        }
    }



