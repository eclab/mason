/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
 * 
 */
package sim.util.geo;



/** This contains the values associated with MasonGeometry attributes.
 * <p>
 * This consists of a value,
 * whether to display this attribute in the inspector or not, and the number of bytes needed to store the value.
 * The value can be an Integer, Float, or String.
 *
 * @see MasonGeometry
 * @see ShapeFileImporter
 * @see ShapeFileExporter
 *
 */
public class AttributeValue implements java.io.Serializable
{
    private static final long serialVersionUID = -2342742107342686581L;
    
    /** Attribute value */
    private Object value;

    /** Whether the attribute is displayed in the inspector or not */
    private boolean hidden;



    public AttributeValue()
    {
        this(null, false);
    }



    public AttributeValue(Object v)
    {
        this(v, false);
    }



    public AttributeValue(Object v, boolean h)
    {
        value = v;
        hidden = h;
    }



    /** Human readable form
     */
    @Override
    public String toString()
    {
        return "Value: " + getValue() + " Hidden: " + isHidden();
    }



    /** Simple, shallow clone */
    @Override
    public Object clone()
    {
        AttributeValue a = new AttributeValue(getValue(), isHidden());
        return a;
    }



    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        
        if (getClass() != obj.getClass())
        {
            return false;
        }

        final AttributeValue other = (AttributeValue) obj;

        if (this.value != other.value && (this.value == null || !this.value.equals(other.value)))
        {
            return false;
        }

        if (this.hidden != other.hidden)
        {
            return false;
        }

        return true;
    }



    @Override
    public int hashCode()
    {
        int hash = 3;
        
        hash = 79 * hash + (this.value != null ? this.value.hashCode() : 0);
        hash = 79 * hash + (this.hidden ? 1 : 0);

        return hash;
    }



    /**
     * @return the value
     */
    public Object getValue()
    {
        return value;
    }



    /**
     * @param value the value to set
     */
    public void setValue(Object value)
    {
        this.value = value;
    }


    public void setInteger(int value)
    {
        setValue(new Integer(value));
    }

    public Integer getInteger()
    {
        return (Integer) getValue();
    }


    public void setDouble(double value)
    {
        setValue(new Double(value) );
    }

    public Double getDouble()
    {
        return (Double) getValue();
    }


    public void setString(String value)
    {
        setValue(value);
    }


    public String getString()
    {
        return (String) getValue();
    }

    

    /**
     * @return whether this is visible to the inspector
     */
    public boolean isHidden()
    {
        return hidden;
    }



    /**
     * @param hidden dictates visibility to the inspector
     */
    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }


}
