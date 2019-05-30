/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display;

import java.util.prefs.*;

/** A simple cover class for Java's preferences system.  MASON has both global and application-specific
    preferences.  Each Preferences object is associated with a unique namespace hung under the global
    Preferences or under an application-specific preferences.  You are responsible for defining 
    the namespace for your particular object.  Typically it's a single unique word like "Display2D"
    or "Bugaboo".  Do not include periods, whitespace, or begin or end with a slash.
        
    <p>Once you have retrieved the Preferences object and modified it, you must save it before MASON is
    terminated.
        
    <p>How this works with Java's Preferences:
    All preferences start with a certain path and then add a namespace onto it.       
    Global preferences have a path which starts with "edu/gmu/mason/global/" -- Display2D's namespace is "Display2D", so for example,
    Display2D's global preferences are "edu/gmu/mason/global/Display2D".  App-specific preferences
    also have unique paths.  For example, HeatBugs's preferences path is
    "edu/gmu/mason/app/sim/app/heatbugs/", so Display2D's app preferences, when used with Heatbugs, is
    "edu/gmu/mason/app/sim/app/heatbugs/Display2D".  You don't need to be concerned with this if you
    just want to retrieve and/or delete preferences objects.
*/

public class Prefs
    {
    public static final String MASON_PREFERENCES = "edu/gmu/mason/global/";
    public static final String APP_PREFERENCES = "edu/gmu/mason/app/";

    /** Returns the global preferences for MASON, with the given additional prefix as a namespace. */
    public static Preferences getGlobalPreferences(String namespace)
        {
        return Preferences.userRoot().node(MASON_PREFERENCES + namespace);
        }

    /** Returns app-specific preferences for MASON, with the given additional prefix as a namespace. */
    public static Preferences getAppPreferences(GUIState simulation, String namespace)
        {
        return Preferences.userRoot().node(
            APP_PREFERENCES + simulation.getClass().getName().replace('.','/') + "/" + namespace); 
        }
        
    /** Deletes certain global preferences for MASON, with the given additional prefix as a namespace. */
    public static boolean removeGlobalPreferences(String namespace)
        {
        try
            {
            getGlobalPreferences(namespace).removeNode();
            return true;
            }
        catch (BackingStoreException e)
            {
            return false;
            }
        }

    /** Deletes certain app-specific preferences for MASON, with the given additional prefix as a namespace. */
    public static boolean removeAppPreferences(GUIState simulation, String namespace)
        {
        try
            {
            getAppPreferences(simulation, namespace).removeNode();
            return true;
            }
        catch (BackingStoreException e)
            {
            return false;
            }
        }

    /** Saves a given Preferences. */
    public static boolean save(Preferences prefs)
        {
        try 
            {
            prefs.flush();
            return true;
            }
        catch (BackingStoreException ex)
            {
            return false;
            }
        }
    }