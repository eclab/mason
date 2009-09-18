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


public class ValueGridPortrayal3D extends FieldPortrayal3D
    {       
    String valueName; 
    public double scale;
    public ColorMap map = new SimpleColorMap(); 
    ValuePortrayal3D defaultPortrayal = new ValuePortrayal3D(); 

    int width, height, length; 
    
    final MutableDouble valueToPass = new MutableDouble(0); 

    public ColorMap getMap() { return map;}
    public void setMap(ColorMap m) { map = m; }
    public String getValueName () { return valueName; } 
    
    public Portrayal getDefaultPortrayal() 
        { 
        return defaultPortrayal; 
        }

    public void setField(Object field)
        {
        if (field instanceof IntGrid3D || field instanceof DoubleGrid3D) this.field = (AbstractGrid3D) field;
        else if (field instanceof IntGrid2D || field instanceof DoubleGrid2D) this.field = (AbstractGrid2D) field;
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

    public ValueGridPortrayal3D(double  s) 
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
        return ((Portrayal3D)(getPortrayalForObject(new ValuePortrayal3D.ValueWrapper(0,0,0,0,this)))).polygonAttributes();
        }

    public double newValue(int x, int y, int z)
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
        
        Switch localSwitch = new Switch(Switch.CHILD_MASK); 
        localSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
        localSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
        localSwitch.setCapability(Group.ALLOW_CHILDREN_READ);
                
        globalTG.addChild(localSwitch); 

        extractDimensions(); // set the width, height, and length based on the underlying grid
        
        java.util.BitSet childMask = new java.util.BitSet(width*height*length); 

        Transform3D trans = new Transform3D(); 
        
        Portrayal p = getPortrayalForObject(new ValuePortrayal3D.ValueWrapper(0,0,0,0,this));
        if (!(p instanceof SimplePortrayal3D))
            throw new RuntimeException("Unexpected Portrayal " + p + "for object " +
                valueToPass + " -- expected a SimplePortrayal3D");
        
        SimplePortrayal3D portrayal = (SimplePortrayal3D) p;
        portrayal.setParentPortrayal(this);

        int i = 0;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
        for (int x=0;x<width;x++) 
            for (int y=0;y<height;y++) 
                for (int z=0;z<length;z++) 
                    {
                    double value = newValue(x,y,z); 
                    ValuePortrayal3D.ValueWrapper wrapper = new ValuePortrayal3D.ValueWrapper(value,x,y,z,this);
                    TransformGroup tg = portrayal.getModel(wrapper, null);
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
        portrayal.setParentPortrayal(this);
                                                
        int i = 0;
        final int width = this.width;
        final int height = this.height;
        final int length = this.length;
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

                    ValuePortrayal3D.ValueWrapper wrapper = (ValuePortrayal3D.ValueWrapper)(shape.getUserData());
                    double value = newValue(x,y,z); 
                    double oldValue = wrapper.lastVal;

                    if (value != oldValue) // change to new value
                        if (map.getAlpha(value) > 2)  // nontransparent
                            { 
                            childMask.set(i);
                            wrapper.lastVal = value;
                            portrayal.getModel(wrapper, tg); 
                            }
                        else childMask.clear(i);
                    i++;  // next index
                    }
        localSwitch.setChildMask(childMask); 
        }
            

    private void extractDimensions() 
        { 
        if (field instanceof IntGrid3D || field instanceof DoubleGrid3D)
            {
            AbstractGrid3D v = (AbstractGrid3D) field;
            width = v.getWidth(); height = v.getHeight(); length = v.getLength();
            }
        else if (field instanceof IntGrid2D || field instanceof DoubleGrid2D)
            {
            AbstractGrid2D v = (AbstractGrid2D) field;
            width = v.getWidth(); height = v.getHeight(); length = 1;
            }
        else throw new RuntimeException("Invalid field for ValueGridPortrayal3D: " + field);
        }



    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr)
        {
        return w;
        }
    }
