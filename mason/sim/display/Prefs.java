/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

import java.util.prefs.*;

/** A simple cover class for Java's preferences system.  MASON has both global and application-specific
	preferences.  Each Preferences object is associated with a unique subpath hung under the global
	Preferences or under an application-specific preferences.  You are responsible for defining 
	the subpath for your particular object.  Typically it's a single unique word like "Display2D"
	or "Bugaboo".  Do not include periods, whitespace, or begin or end with a slash.
	
	<p>Once you have retrieved the Preferences object and modified it, you must save it before MASON is
	terminated.
	
	<p>How this works with Java's Preferences:
	All preferences start with a certain path and then add a subpath onto it.	
	Global preferences have a path which starts with "edu/gmu/mason/global/" -- Display2D's subpath is "Display2D", so for example,
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

	/** Returns the global preferences for MASON, with the given additional prefix as a subpath. */
	public static Preferences getGlobalPreferences(String postfix)
		{
		System.err.println("Getting " + MASON_PREFERENCES + postfix);
		return Preferences.userRoot().node(MASON_PREFERENCES + postfix);
		}

	/** Returns app-specific preferences for MASON, with the given additional prefix as a subpath. */
	public static Preferences getAppPreferences(GUIState simulation, String postfix)
		{
		System.err.println("Getting " + APP_PREFERENCES + simulation.getClass().getName().replace('.','/') + "/" + postfix);
		return Preferences.userRoot().node(
			APP_PREFERENCES + simulation.getClass().getName().replace('.','/') + "/" + postfix); 
		}
	
	/** Deletes certain global preferences for MASON, with the given additional prefix as a subpath. */
	public static boolean removeGlobalPreferences(String postfix)
		{
		try
			{
			getGlobalPreferences(postfix).removeNode();
			return true;
			}
		catch (BackingStoreException e)
			{
			return false;
			}
		}

	/** Deletes certain app-specific preferences for MASON, with the given additional prefix as a subpath. */
	public static boolean removeAppPreferences(GUIState simulation, String postfix)
		{
		try
			{
			getAppPreferences(simulation, postfix).removeNode();
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
			System.err.println("Saving " + prefs);
			prefs.flush();
			return true;
			}
		catch (BackingStoreException ex)
			{
			return false;
			}
		}
	}