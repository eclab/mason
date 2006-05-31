/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import sim.display.*;

/**
   A common interface for portrayal classes.
*/

public interface Portrayal extends java.io.Serializable
    {
    /** Provide an inspector for an object. */
    public Inspector getInspector(LocationWrapper wrapper, GUIState state);
    
    /** Returns a static, one-line name for the given object that is useful for a human
        to distinguish it from other objects.  A simple default would
        be just to return "" + object. */
    public String getName(LocationWrapper wrapper);

    /** Returns a simple, informative one-line description of the current status of the object,
        which may change at any time to reflect how the object is changing.  A simple default would
        be just to return getName(wrapper). */
    public String getStatus(LocationWrapper wrapper);

    /** Change the portrayal state to reflect the fact that you've 
        been selected or not selected.  Always return true, except
        if you've received a setSelected(true) and in fact do not
        wish to be selectable, in which case return false in that
        sole situation.  */
    public boolean setSelected(LocationWrapper wrapper, boolean selected);
    }
