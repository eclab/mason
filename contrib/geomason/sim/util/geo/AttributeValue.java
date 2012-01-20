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

import sim.io.geo.ShapeFileImporter;



/** This contains the values associated with MasonGeometry attributes.
 * <p>
 * This consists of a value,
 * whether to display this attribute in the inspector or not, and the number of bytes needed to store the value.
 * The value can be an Integer, Float, or String.
 * <p>
 * We store
 * the number of bytes since the attributes are formatted similar to a relational database table where each column has a 
 * fixed width (not necessarily the same width as the data) defined by the user.  The number of bytes is used to pad the data
 * when writing to disk.
 *
 * @see MasonGeometry
 * @see ShapeFileImporter
 *
 */
public class AttributeValue implements java.io.Serializable
{
    private static final long serialVersionUID = -2342742107342686581L;
    
    /** Attribute name */
//    private String name;

    /** Attribute value */
    private Object value;

    /** Whether the attribute is displayed in the inspector or not */
    private boolean hidden;

    /** Attributes are stored in format similar to a relational database table, so we 
     * need to save the size of the field for exporting.
     *
     * XXX Is this really necessary?  Isn't this a low level implementation detail?
     */
    private int fieldSize;



    public AttributeValue()
    {
        this(null, 0, false);
    }



    public AttributeValue(Object v)
    {
        this(v, 0, false);
    }



    public AttributeValue(Object v, int f, boolean h)
    {
//        name = n;
        value = v;
        hidden = h;
        fieldSize = f;
    }



    /** Human readable form
     */
    @Override
    public String toString()
    {
//        return "Name: " + getName() + " Value: " + getValue() + " Field size: " + getFieldSize() + " Hidden: " + isHidden();
        return "Value: " + getValue() + " Field size: " + getFieldSize() + " Hidden: " + isHidden();
    }



    /** Simple, shallow clone */
    @Override
    public Object clone()
    {
//        AttributeValue a = new AttributeValue(getName(), getValue(), getFieldSize(), isHidden());
        AttributeValue a = new AttributeValue(getValue(), getFieldSize(), isHidden());
        return a;
    }


//
//    /**
//     * @return the name
//     */
//    public String getName()
//    {
//        return name;
//    }
//
//
//
//    /**
//     * @param name the name to set
//     */
//    public void setName(String name)
//    {
//        this.name = name;
//    }



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
        if (this.fieldSize != other.fieldSize)
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
        hash = 79 * hash + this.fieldSize;
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



    /**
     * @return the fieldSize
     */
    public int getFieldSize()
    {
        return fieldSize;
    }



    /**
     * @param fieldSize the fieldSize to set
     */
    public void setFieldSize(int fieldSize)
    {
        this.fieldSize = fieldSize;
    }

}
