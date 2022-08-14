/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import java.awt.*;
import sim.portrayal.*;
import sim.display.*;
import java.awt.event.*;

/** 
    A portrayal which builds and uses ANOTHER portrayal to do the work for it.  Why would you need
    such a thing?  Because the other portrayal is transient: it goes to null when serialized and
    is rebuilt as necessary when deserialized.  Ordinarily model objects cannot subclass from
    ShapePortrayal2D (for example) if they want to be checkpointed, because Stroke and Shape 
    are not serializable.  But they can subclass from InternalPortrayal2D, and override the
    buildPortrayal() method to provide the appropriate ShapePortrayal2D object, and thus 
    can be serializable.
*/
        
public abstract class InternalPortrayal2D extends SimplePortrayal2D
    {
    protected transient SimplePortrayal2D portrayal;

    /** Override this method to provide the actual portrayal to use to portray the object. */
    public abstract SimplePortrayal2D buildPortrayal(Object obj);

    /** Builds and stores a portrayal if necessary, then returns the stored portrayal. */
    public SimplePortrayal2D providePortrayal(Object obj)
        {
        if (portrayal == null) portrayal = buildPortrayal(obj);
        return portrayal;
        }

    public void draw(Object object, Graphics2D graphics, DrawInfo2D info) 
        { providePortrayal(object).draw(object, graphics, info); }
    public boolean hitObject(Object object, DrawInfo2D range) 
        { return providePortrayal(object).hitObject(object, range); }
    public boolean setSelected(LocationWrapper wrapper, boolean selected) 
        { return providePortrayal(wrapper.getObject()).setSelected(wrapper, selected); }
    public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper, MouseEvent event, DrawInfo2D fieldPortrayalDrawInfo, int type) 
        { return providePortrayal(wrapper.getObject()).handleMouseEvent(guistate, manipulating, wrapper, event, fieldPortrayalDrawInfo, type); }
    public Inspector getInspector(LocationWrapper wrapper, GUIState state) 
        { return providePortrayal(wrapper.getObject()).getInspector(wrapper, state); }
    public String getName(LocationWrapper wrapper) 
        { return providePortrayal(wrapper.getObject()).getName(wrapper); }
    }       
        
