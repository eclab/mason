package migration.parameters;

import ec.util.Parameter;
import ec.util.ParameterDatabase;
/**
 * 
 * @author Ermo Wei
 *
 */
public class Utility {
	 /**
     * Convenience function for getting an integer value from the parameter
     * database
     *
     * @param parameterName name of parameter
     * @param defaultValue value to return if database doesn't know about that
     * parameter
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     *
     */
    public static int returnIntParameter(ParameterDatabase paramDB, String parameterName, int defaultValue)
    {
        return paramDB.getIntWithDefault(new Parameter(parameterName), null, defaultValue);
    }

    /**
     * Convenience function for getting a boolean value from the parameter
     * database
     *
     * @param parameterName
     * @param defaultValue
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     * XXX consider moving this to a separate utility class; or changing MASON
     * to accept mere strings
     *
     */
    public static boolean returnBooleanParameter(ParameterDatabase paramDB, String parameterName, boolean defaultValue)
    {
        return paramDB.getBoolean(new Parameter(parameterName), null, defaultValue);
    }

    /**
     * Convenience function for getting a double value from the parameter
     * database
     *
     * @param parameterName name of parameter
     * @param defaultValue value to return if database doesn't know about that
     * parameter
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     * XXX consider moving this to a separate utility class; or changing MASON
     * to accept mere strings
     *
     */
    public static double returnDoubleParameter(ParameterDatabase paramDB, String parameterName, double defaultValue)
    {
        return paramDB.getDoubleWithDefault(new Parameter(parameterName), null, defaultValue);
    }

    /**
     * Convenience function for getting a String value from the parameter
     * database
     *
     * @param parameterName name of parameter
     * @param defaultValue value to return if database doesn't know about that
     * parameter
     *
     * @return the value stored for that parameter, or 'default' if database
     * doesn't have that value
     *
     * XXX consider moving this to a separate utility class; or changing MASON
     * to accept mere strings
     *
     */
    public static String returnStringParameter(ParameterDatabase paramDB, String parameterName, String defaultValue)
    {
        return paramDB.getStringWithDefault(new Parameter(parameterName), null, defaultValue);
    }
}
