/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import sim.util.gui.*;
import sim.display.*;
import sim.util.*;

/**
   A FieldPortrayal is an object which knows how to portray some kind of Field.

   <p>This abstract version of FieldPortrayal provides some basic functionality that
   many FieldPortrayals may find handy.  This functionality allows a FieldPortrayal
   to store Portrayal objects responsible for drawing various objects within the
   Field.  For example, a SparseGrid2D holds a bunch of objects: the SparseGridPortrayal2D,
   which is a FieldPortrayal, lets you store Portrayal objects which know how to draw
   the various objects in the SparseGrid2D.

   <p>The default version of the setField(...) method sets the field without checking
   to see if it's a valid field or not; you'll want to override this to check.

   <p>You can associate a Portrayal object with an object stored in the Field in several
   ways.  First, you can specify one Portrayal object to be used for ALL objects stored
   in the field, using <b>setPortrayalForAll</b>.  Second, you can specify a Portrayal
   object to be used for objects in a Field all belonging to the same class, using
   <b>setPortrayalForClassOf</b>.  Third, you can specify a Portrayal for a specific
   object, using <b>setPortrayalForObject</b>.

   <p>You can get the desired Portrayal for an object by calling <b>getPortrayalForObject</b>.
   This method looks up the Portrayal for an object by going down the following checklist
   until a Portrayal is found (earlier checklist items take precedence over later ones):

   <ol>
   <li>
   If there is a portrayalForAll, return it.
   <li>If the object is null:
   <ol>
   <li>Return the portrayalForNull if there is one
   <li>
   If a portrayal is explicitly registered for null, return that portrayal.
   <li>Return the defaultNullPortrayal.
   </ol>
   <li>If the object is non-null:
   <ol>
   <li>If the object implements the appropriate Portrayal interface, return the object itself as its own Portrayal.
   <li>Return the portrayalForNonNull if there is one
   <li>
   If a portrayal is explicitly registered for the object, return that portrayal.  Portrayals may be registered for <tt>null</tt> as well.
   <li>
   <li> 
   If a Portrayal is registered for the object's exact class (superclasses are ignored), return that portrayal.
   <li>
   Return the portrayalForRemainder if there is one
   <li>
   Return the default Portrayal object.
   </ol>
   </ol>

   <p>FieldPortrayals store Portrayal objects in WeakHashMaps.  This means that if you register a Portrayal explicitly for an object, and then later the object is eliminated from your model, the FieldPortrayal will not hold onto the object or onto its Portrayal, but will allow them to garbage collect as well.  Thus you don't have to worry about de-registering an object.
   
   <p> Some FieldPortrayals benefit (draw faster) if they know that their underlying field is immutable,
   that is, it never changes. Notably, most FieldPortrayal3Ds benefit, as well as various ValueGrid2DPortrayals.
*/


public abstract class FieldPortrayal
    {
    public Portrayal portrayalForAll;
    public Portrayal portrayalForNull;
    public Portrayal portrayalForNonNull;
    public Portrayal portrayalForRemainder;
    public WeakHashMap portrayals; // = new WeakHashMap();
    public WeakHashMap classPortrayals; // = new WeakHashMap();

    /** Set the portrayal to null to remove it. */
    public void setPortrayalForAll(Portrayal portrayal)
        {
        portrayalForAll = portrayal;
        }
    
    public Portrayal getPortrayalForAll()
        { 
        return portrayalForAll;
        }

    /** Set the portrayal to null to remove it. */
    public void setPortrayalForNull(Portrayal portrayal)
        {
        portrayalForNull = portrayal;
        }
        
    public Portrayal getPortrayalForNull()
        {
        return portrayalForNull;
        }

    /** Set the portrayal to null to remove it. */
    public void setPortrayalForNonNull(Portrayal portrayal)
        {
        portrayalForNonNull = portrayal;
        }
        
    public Portrayal getPortrayalForNonNull()
        {
        return portrayalForNonNull;
        }

    /** Set the portrayal to null to remove it. */
    public void setPortrayalForRemainder(Portrayal portrayal)
        {
        portrayalForRemainder = portrayal;
        }
        
    public Portrayal getPortrayalForRemainder()
        {
        return portrayalForRemainder;
        }
        
    /** Sets a portrayal for a class -- objects must be of EXACTLY this class (not subclasses)
        to respond to this. Set the portrayal to null to remove it for a given class. */  
    public void setPortrayalForClass(Class cls, Portrayal portrayal)
        {
        if (classPortrayals == null) classPortrayals = new WeakHashMap();
        if (portrayal==null)
            classPortrayals.remove(cls);
        else classPortrayals.put(cls,portrayal);
        }
        
    /** Sets a portrayal for a class -- objects must be equal(...) to the provided object here
        to respond to this. Set the portrayal to null to remove it for a given object. */  
    public void setPortrayalForObject(Object obj, Portrayal portrayal)
        {
        if (portrayals == null) portrayals = new WeakHashMap();
        if (portrayal==null)
            portrayals.remove(obj);
        else portrayals.put(obj,portrayal);
        }
    
    /** Returns a default portrayal for null.  By default this is set to
        the same as getDefaultPortrayal().  Override this to provide a 
        more interesting default portrayals for null. */
    public Portrayal getDefaultNullPortrayal()
        {
        return getDefaultPortrayal();
        }
    
    /** Should return a portrayal which can portray any object regardless of
        whether it's valid or not */
    public abstract Portrayal getDefaultPortrayal();
        
    /** Returns the appropriate Portrayal. */
    public Portrayal getPortrayalForObject(Object obj)
        {
        Portrayal tmp;
        
        // return the portrayal-for-all if any
        if (portrayalForAll != null) return portrayalForAll;
        
        if (obj == null)
            {
            if (portrayalForNull != null) return portrayalForNull;
            if ( (portrayals != null /* && !portrayals.isEmpty() */) && // a little efficiency -- avoid making weak keys etc. 
                ((tmp = ((Portrayal)(portrayals.get(obj))) ) !=null)) return tmp;
            return getDefaultNullPortrayal();
            }
        else
            {
            if (obj instanceof Portrayal) return (Portrayal) obj;
            if (portrayalForNonNull != null) return portrayalForNonNull;
            if ( (portrayals != null /* && !portrayals.isEmpty() */) &&  // a little efficiency -- avoid making weak keys etc. 
                ((tmp = ((Portrayal)(portrayals.get(obj))) ) !=null)) return tmp;
            if ( (classPortrayals != null /* && !classPortrayals.isEmpty() */) &&  // a little efficiency -- avoid making weak keys etc. 
                ((tmp = ((Portrayal)(classPortrayals.get(obj.getClass()))) ) !=null)) return tmp;
            if (portrayalForRemainder!=null) return portrayalForRemainder;
            return getDefaultPortrayal();
            }
        }
    
    protected Object field = null;
    protected boolean immutableField = false;

    /** This flag is available for field portrayals to set and clear as they like: but its
        intended function is to be set during setField(field) to warn drawing that even
        though the field is immutable, it may have changed to another field and needs to be
        redrawn.  Similarly, typically this flag is cleared after drawing.  Initially true.
    */
    boolean dirtyField=true;
        
    public synchronized void setDirtyField(boolean val) { dirtyField = val; }
    public synchronized boolean isDirtyField() { return dirtyField; }
        
    /**
       @deprecated Use setDirtyField(false);
    */
    public synchronized void reset() { dirtyField = true; }
    
    /** Returns true if the underlying field is assumed to be unchanging -- thus
        there's no reason to update once we're created.  Not all FieldPortrayals
        will care about whether or not a field is immutable. */
    public boolean isImmutableField() { return immutableField; }
    
    /** Specifies that the underlying field is (or is not) to be assumed unchanging --
        thus there's no reason to update once we're created.  Not all FieldPortrayals
        will care about whether or not a field is immutable.  Also sets dirtyField to true regardless. */
    public void setImmutableField(boolean val) { immutableField = val;  setDirtyField(true);}

    /** Returns the field. */
    public Object getField()
        {
        return field;
        }

    /** Sets the field, and sets the dirtyField flag to true.  May throw an exception if the field is inappropriate. 
        The default version just sets the field and sets the dirtyField flag. */
    public void setField(Object field)
        {
        this.field = field;
        setDirtyField(true);
        }

    class CustomInspector extends Inspector
        {
        public JLabel positions = new JLabel();
        public DisclosurePanel disclosurePanel;
        public LabelledList fieldComponent = new LabelledList("Location");
        public Inspector locationInspector;
        public Inspector objectInspector;
        public LocationWrapper wrapper;
        public Object lastObject;   // check to see if it's changed
        public Object lastLocation;
        public GUIState state;

        public CustomInspector( final LocationWrapper wrapper,
            final Inspector objectInspector,
            final GUIState state )
            {
            this.state = state;
            this.wrapper = wrapper;
            this.objectInspector = objectInspector;
            this.state = state;
            lastObject = wrapper.getObject();
            setLayout(new BorderLayout());
            lastLocation = wrapper.getLocation();
            positions.setText(wrapper.getLocationName());
            locationInspector = sim.portrayal.Inspector.getInspector(lastLocation, state, null);
            disclosurePanel = new DisclosurePanel(positions, locationInspector, "Position");
            add(disclosurePanel, BorderLayout.NORTH);
            add( objectInspector, BorderLayout.CENTER);
            updateInspector();
            }


        // setTitle is not overridden but is just ignored.
    
        public String getTitle()
            {
            return objectInspector.getTitle();
            }
                
        public void updateInspector()
            {
            Object newObject = wrapper.getObject();
            if (newObject != lastObject)  // a new object!  Get new inspector just in case
                {
                // Maybe we should revisit this -- if we dynamically remove and then
                // add a new object like we're doing, then stuff gets really flashy -- and
                // in some cases things don't update right (especially if you load into a
                // separate window, hmmmm).  We'll have to deal with the issue anyway because
                // due to the same bug in Java, dynamically changing the values of an array
                // inspector weirds out.
                remove(objectInspector);
                objectInspector = getPortrayalForObject(wrapper.getObject()).getInspector(wrapper, state);
                add(objectInspector,BorderLayout.CENTER);
                revalidate();
                }
            Object location = wrapper.getLocation();
            if (location != lastLocation)  // sadly this will happen an awful lot
                {
                disclosurePanel.setDisclosedComponent(sim.portrayal.Inspector.getInspector(location, state, null));
                lastLocation = location;
                }
            positions.setText(wrapper.getLocationName());
            objectInspector.updateInspector();
            locationInspector.updateInspector();
            }
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        if (wrapper == null) return null;
        Inspector objectInspectorComponent = 
            getPortrayalForObject(wrapper.getObject()).getInspector(wrapper, state);
        if( objectInspectorComponent == null ) return null;
        return new CustomInspector( wrapper, objectInspectorComponent, state);
        }
    
    public String getName(LocationWrapper wrapper)
        {
        if (wrapper == null) return "";
        return getPortrayalForObject(wrapper.getObject()).getName(wrapper);
        }

    public String getStatus(LocationWrapper wrapper)
        {
        if (wrapper == null) return "";
        return getPortrayalForObject(wrapper.getObject()).getStatus(wrapper);
        }

    /**
       Selects or deselects all of the provided objects. 
    */
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return getPortrayalForObject(wrapper.getObject()).setSelected(wrapper, selected);
        }

    public void setSelected(Bag locationWrappers, boolean selected)
        {
        for(int x=0;x<locationWrappers.numObjs;x++)
            {
            LocationWrapper wrapper = (LocationWrapper)(locationWrappers.objs[x]);
            setSelected(wrapper, selected);
            }
        }
    }
