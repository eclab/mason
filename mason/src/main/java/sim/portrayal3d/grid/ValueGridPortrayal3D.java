/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid;

import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.portrayal3d.simple.*; 
import sim.util.*;
import sim.util.gui.*;
import javax.vecmath.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.picking.*;
import java.awt.*;


public class ValueGridPortrayal3D extends FieldPortrayal3D
    {       
    String valueName; 
    double scale;
    ColorMap map = new SimpleColorMap(); 

    int width = 0;
    int height = 0;
    int length = 0; 
    
    final MutableDouble valueToPass = new MutableDouble(0); 

    public ColorMap getMap() { return map;}
    public void setMap(ColorMap m) { map = m; }
    public String getValueName () { return valueName; } 
    public void setValueName(String name) { valueName = name; }
        
    boolean dirtyScale = false;
    public double getScale () { return scale; } 
    public void setScale(double val) { scale = val; dirtyScale = true; }
        
    ValuePortrayal3D defaultPortrayal = new ValuePortrayal3D(); 
    public Portrayal getDefaultPortrayal() 
        { 
        return defaultPortrayal; 
        }

    public void setField(Object field)
        {
        if (field instanceof IntGrid3D || field instanceof DoubleGrid3D ||
            field instanceof IntGrid2D || field instanceof DoubleGrid2D) super.setField(field);
        else throw new RuntimeException("Invalid field for ValueGridPortrayal3D: " + field);
        }

    public Object getField() { return field; } 

    public ValueGridPortrayal3D() 
        { 
        this("Value", 1); 
        }

    public ValueGridPortrayal3D(String valueName) 
        { 
        this(valueName, 1); 
        }

    public ValueGridPortrayal3D(double s) 
        { 
        this("Value", s); 
        }

    public ValueGridPortrayal3D(String valueName, double scale) 
        { 
        this.valueName = valueName; 
        this.scale = scale; 
        }

    public PolygonAttributes polygonAttributes()
        {
        return ((Portrayal3D)(getPortrayalForObject(new ValueWrapper(0.0, new Int3D(), this)))).polygonAttributes();
        }

    /** This method is called by the default inspector to filter new values set by the user.
        You should return the "corrected" value if the given value is invalid. The default version
        of this method bases values on the values passed into the setLevels() and setColorTable() methods. */
    public double newValue(int x, int y, int z, double value)
        {
        if (field instanceof IntGrid2D || field instanceof IntGrid3D) value = (int) value;
                
        if (map.validLevel(value)) return value;

        if (field != null)
            {
            if (field instanceof DoubleGrid3D)
                return ((DoubleGrid3D)field).field[x][y][z];
            else if (field instanceof IntGrid3D)
                return ((IntGrid3D)field).field[x][y][z];
            else if (field instanceof DoubleGrid2D)
                return ((DoubleGrid2D)field).field[x][y];
            else //if (field instanceof IntGrid2D)
                return ((IntGrid2D)field).field[x][y];
            }
        else return map.defaultValue();
        }

    // returns the value at the given grid position
    double gridValue(int x, int y, int z)
        {                        
        if (field instanceof DoubleGrid3D)
            return ((DoubleGrid3D)field).field[x][y][z];
        else if (field instanceof IntGrid3D)
            return ((IntGrid3D)field).field[x][y][z];
        else if (field instanceof DoubleGrid2D)
            return ((DoubleGrid2D)field).field[x][y];
        else //if (field instanceof IntGrid2D)
            return ((IntGrid2D)field).field[x][y];
        }
    
    public TransformGroup createModel()
        {
        TransformGroup globalTG = new TransformGroup(); 
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        
        if (field == null) return globalTG;
                
        dirtyScale = false;  // we'll be revising the scale entirely
        
        Switch localSwitch = new Switch(Switch.CHILD_MASK); 
        localSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
        localSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
        localSwitch.setCapability(Group.ALLOW_CHILDREN_READ);
                
        globalTG.addChild(localSwitch); 

        extractDimensions(); // set the width, height, and length based on the underlying grid
        
        java.util.BitSet childMask = new java.util.BitSet(width*height*length); 

        Transform3D trans = new Transform3D(); 
        
        Portrayal p = getPortrayalForObject(new ValueWrapper(0.0, new Int3D(), this));
        if (!(p instanceof SimplePortrayal3D))
            throw new RuntimeException("Unexpected Portrayal " + p + "for object " +
                valueToPass + " -- expected a SimplePortrayal3D");
        
        SimplePortrayal3D portrayal = (SimplePortrayal3D) p;
        portrayal.setCurrentFieldPortrayal(this);

        int i = 0;
        int width = this.width;
        int height = this.height;
        int length = this.length;
        for (int x=0;x<width;x++) 
            for (int y=0;y<height;y++) 
                for (int z=0;z<length;z++) 
                    {
                    double value = gridValue(x,y,z); 
                    TransformGroup tg = portrayal.getModel(new ValueWrapper(0.0, new Int3D(x,y,z), this), null);
                    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                    tg.setCapability(Group.ALLOW_CHILDREN_READ);
                    trans.setTranslation(new Vector3f(x,y,z)); 
                    trans.setScale(scale); 
                    tg.setTransform(trans); 
                    //tg.setUserData(wrapper);  // already done when the object was created
                    localSwitch.addChild(tg);

                    if (map.getAlpha(value) > 2) // nontransparent
                        childMask.set(i);
                    else 
                        childMask.clear(i);

                    i++;
                    }

        localSwitch.setChildMask(childMask); 
        return globalTG;
        }
        
        

    public void updateModel(TransformGroup modelTG)
        {
        if (field == null) return; 

        extractDimensions(); 
        Switch localSwitch = (Switch) modelTG.getChild(0); 
        java.util.BitSet childMask = localSwitch.getChildMask(); 
        
        Portrayal p = getPortrayalForObject(valueToPass);
        if (!(p instanceof SimplePortrayal3D))
            throw new RuntimeException("Unexpected Portrayal " + p + "for object " +
                valueToPass + " -- expected a SimplePortrayal3D");
        
        SimplePortrayal3D portrayal = (SimplePortrayal3D) p;
        portrayal.setCurrentFieldPortrayal(this);
                
        if (dirtyScale || isDirtyField())
            reviseScale(localSwitch);               // sizes may have changed
                                                
        int i = 0;
        int width = this.width;
        int height = this.height;
        int length = this.length;
        for (int x=0;x<width;x++) 
            for (int y=0;y<height;y++) 
                for (int z=0;z<length;z++) 
                    { 
                    TransformGroup tg = (TransformGroup)localSwitch.getChild(i);
                                        
                    // ValuePortrayal3D dispenses with its TransformGroup in order to achieve some
                    // additional speed.  We recognize that fact here.
                    // TransformGroup g = (TransformGroup)(g.getChild(0));
                    // Shape3D shape = (Shape3D)(g.getChild(0));
                                        
                    Shape3D shape = (Shape3D)(tg.getChild(0));

                    ValueWrapper wrapper = (ValueWrapper)(shape.getUserData());
                    double value = gridValue(x,y,z); 
                    double oldValue = wrapper.lastValue;
                                        
                    if (value != oldValue) // change to new value
                        if (map.getAlpha(value) > 2)  // nontransparent
                            { 
                            childMask.set(i);
                            wrapper.lastValue = value;
                            portrayal.getModel(wrapper, tg); 
                            }
                        else childMask.clear(i);
                    i++;  // next index
                    }
        localSwitch.setChildMask(childMask); 
        }
            

    void reviseScale(Switch localSwitch)
        {
        Transform3D trans = new Transform3D();
        int i = 0;
        int width = this.width;
        int height = this.height;
        int length = this.length;
        for (int x=0;x<width;x++) 
            for (int y=0;y<height;y++) 
                for (int z=0;z<length;z++) 
                    {
                    TransformGroup tg = (TransformGroup)localSwitch.getChild(i);
                    tg.getTransform(trans);
                    trans.setScale(scale);
                    tg.setTransform(trans);
                    i++;  // next index
                    }
        dirtyScale = false;
        }


    void extractDimensions() 
        { 
        if (field instanceof IntGrid3D || field instanceof DoubleGrid3D)
            {
            AbstractGrid3D v = (AbstractGrid3D) field;
            int _width = v.getWidth(); 
            int _height = v.getHeight(); 
            int _length = v.getLength();
            if (width != 0  && (_width != width || _height != height || _length != length))
                throw new RuntimeException("Cannot presently change the dimensions of a field once it's set in ValueGridPortrayal3D.  Sorry.");
            width = _width;
            height = _height;
            length = _length;
            }
        else if (field instanceof IntGrid2D || field instanceof DoubleGrid2D)
            {
            AbstractGrid2D v = (AbstractGrid2D) field;
            int _width = v.getWidth(); 
            int _height = v.getHeight(); 
            int _length = 1;
            if (width != 0 && (_width != width || _height != height || _length != length))
                throw new RuntimeException("Cannot presently change the dimensions of a field once it's set in ValueGridPortrayal3D.  Sorry.");
            width = _width;
            height = _height;
            length = _length;
            }
        else throw new RuntimeException("Invalid field for ValueGridPortrayal3D: " + field);
        }

    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr)
        {
        return w;
        }
        
    // used to store the 'lastValue' so in updateModel we can determine whether to bother updating
    // the cube or square's color.
    static class ValueWrapper extends LocationWrapper
        {
        public double lastValue;  // we need the old value to determine if the color of the cube must be updated
                
        public ValueWrapper(double value, Object location, ValueGridPortrayal3D portrayal)
            {
            super(new MutableDouble(value), location, portrayal);
            lastValue = value;
            }
                        
        public Object getObject()
            {
            Int3D loc = (Int3D)location;
            Object field = fieldPortrayal.getField();
            MutableDouble val = (MutableDouble) this.object;
            if (field instanceof DoubleGrid3D)
                val.val = ((DoubleGrid3D)field).field[loc.x][loc.y][loc.z];
            else if (field instanceof IntGrid3D)
                val.val = ((IntGrid3D)field).field[loc.x][loc.y][loc.z];
            else if (field instanceof DoubleGrid2D)
                val.val = ((DoubleGrid2D)field).field[loc.x][loc.y];
            else // if (field instanceof IntGrid2D)
                val.val = ((IntGrid2D)field).field[loc.x][loc.y];
            return val;
            }
                
        public String getLocationName()
            {
            Int3D loc = (Int3D) location;
            Object field = fieldPortrayal.getField();
            if (field instanceof DoubleGrid3D || field instanceof IntGrid3D)
                return loc.toCoordinates();
            else return (new Int2D(loc.x,loc.y)).toCoordinates();
            }
        }
                
    /** Returns the color presently mapped to the value stored within the given wrapper.  The wrapper
        must have been generated by ValueGridPortrayal3D, else a cast error will be raised.   Used
        solely by ValuePortrayal3D to determine the color of the object passed it.  It's an Object
        rather than a LocationWrapper to save an unneccessary cast. */
    public Color getColorFor(Object wrapper)
        {
        return getMap().getColor(((ValueWrapper)wrapper).lastValue);
        }
    }
