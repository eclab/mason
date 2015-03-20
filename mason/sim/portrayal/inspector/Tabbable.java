/*
  Copyright 2014 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;

public interface Tabbable
    {
    /** Returns a list of names for tabs to appear in the TabbedInspector. */
    public String[] provideTabNames();
    
    /** Returns, for each tab, a list of names of Java Bean Properties of this object
        which should appear under that tab. */
    public String[][] provideTabProperties();
    
    /** Returns a name for an additional tab holding the remaining Java Bean Properties
        not given in provideValues(), or null if no such tab should be displayed. */
    public String provideExtraTab();
    }
