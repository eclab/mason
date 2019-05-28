/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


/*
 * Created on Apr 9, 2005 8:36:42 AM
 * 
 * By: spaus
 */
package ec.util;

import java.util.EventObject;

/**
 * @author spaus
 */
public class ParameterDatabaseEvent
    extends EventObject {

    public static final int SET = 0;
    public static final int ACCESSED = 1;
    
    private final Parameter parameter;
    private final String value;
    private final int type;

    /**
     * For ParameterDatabase events.
     * 
     * @param source the ParameterDatabase
     * @param parameter the Parameter associated with the event
     * @param value the value of the Parameter associated with the event
     * @param type the type of the event
     */
    public ParameterDatabaseEvent(Object source, Parameter parameter, String value, int type) {
        super(source);
        this.parameter = parameter;
        this.value = value;
        this.type = type;
        }
    
    /**
     * @return the Parameter associated with the event
     */
    public Parameter getParameter() {
        return parameter;
        }
    
    /**
     * @return the value of the Parameter associated with the event.
     */
    public String getValue() {
        return value;
        }
    
    /**
     * @return the type of the event.
     */
    public int getType() {
        return type;
        }
    }
