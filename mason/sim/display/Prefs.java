package sim.display;

import java.util.prefs.*;

public class Prefs
	{
	public static final String MASON_PREFERENCES = "edu/gmu/mason/";

	public static Preferences getGlobalPreferences(String postfix)
		{
		System.err.println("Getting " + MASON_PREFERENCES + postfix);
		return Preferences.userRoot().node(MASON_PREFERENCES + postfix);
		}

	public static Preferences getAppPreferences(GUIState simulation, String postfix)
		{
		System.err.println("Getting " + MASON_PREFERENCES + simulation.getClass().getName().replace('.','/') + "/" + postfix);
		return Preferences.userRoot().node(
			MASON_PREFERENCES + simulation.getClass().getName().replace('.','/') + "/" + postfix); 
		}
	
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