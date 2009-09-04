/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid;
import sim.portrayal3d.*;
import sim.portrayal.*;
import sim.portrayal.grid.*;
import sim.field.grid.*;
import sim.util.*;
import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.picking.*;

/** Portrays ObjectGrid2D and ObjectGrid3D in 3D space. A (0,0) or (0,0,0) object is centered
 * on the origin.  2D fields are spread through the XY plane and are presumed to have Z=0. 
 *
 * <p>You should be aware that ObjectGrid3DPortrayal3D is slow, especially if objects change
 * a lot at given locations.  This is because it has to update
 * all the objects on a <i>per location</i> basis rather than on a <i>per object</i> basis.
 * This is a worst-case scenario for Java3D.
 *
 * <p>Note for Java3D users: We experimented with a number of approaches to dealing with this.
 * One approach was to use shared groups and only portray an object once, then use links to draw
 * it in many locations.  This has two subapproaches: you can wrap the link in a BranchGroup and
 * replace it and the BranchGroup when the object changes at that location; or you can just change
 * the link directly.  The first approach is very slow (building BranchGroups isn't efficient).
 * The second approach has promise, but there are grievous bugs in Java3D's handling of links
 * which generate all sorts of race conditions with array bounds exceptions and null pointer exceptions
 * etc. internal to Java3D when you change the SharedGroup that a link is pointing to.  Ultimately
 * we just gave up and used BranchGroups wrapping whole new submodels, entire copies for each
 * location.  Memory inefficient, but it's the fastest method we have figured out which doesn't
 * break with stupid Sun bugs.  It's also fairly simple to grok.  Sorry.
 */
 
public class ObjectGridPortrayal3D extends FieldPortrayal3D
    {
    protected TransformGroup createModel()
        {
        TransformGroup globalTG = new TransformGroup(); 
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
                
        // we need a group to stick stuff into, so we create a Group here
        Group global = new Group();
        global.setCapability(Group.ALLOW_CHILDREN_READ);
        global.setCapability(Group.ALLOW_CHILDREN_WRITE);
        global.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        global.setUserData(this);  // a sufficient tag -- a Group containing me.  See LocationWrapper
        // this is set so he'll be in the scenegraph path
        global.setCapability(Group.ENABLE_PICK_REPORTING);
                
        globalTG.addChild(global);

        if (field==null) return globalTG;
        Transform3D tmpLocalT = new Transform3D();
        
        if (field instanceof ObjectGrid2D)
            {
            Object[][] grid = ((ObjectGrid2D)field).field;
            for(int x=0;x<grid.length;x++)
                {
                Object[] gridx = grid[x];
                for(int y=0;y<gridx.length;y++)
                    {
                    Object o = gridx[y];
                    // get the child model -- it doesn't exist yet
                    Portrayal p = getPortrayalForObject(o);
                    if(! (p instanceof SimplePortrayal3D))
                        throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                            o + " -- expecting a SimplePortrayal3D");
                    SimplePortrayal3D p3d = (SimplePortrayal3D)p;
                    p3d.setParentPortrayal(this);
                    TransformGroup newTransformGroup = p3d.getModel(o, null);
                    newTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
                    newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                    newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                    // this is set so he'll be in the scenegraph path
                    newTransformGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
                    tmpLocalT.setTranslation(new Vector3d(x,y,0));
                    newTransformGroup.setTransform(tmpLocalT);
                    newTransformGroup.setUserData(new Int2D(x,y));
                                        
                    BranchGroup bg = new BranchGroup();
                    bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                    bg.setCapability(BranchGroup.ALLOW_DETACH);
                    // this is set so he'll be in the scenegraph path
                    bg.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
                    bg.addChild(newTransformGroup);
                    bg.setUserData(o);
                    global.addChild(bg);
                    }
                }
            }
        else // field instanceof ObjectGrid3D
            {
            Object[][][] grid = ((ObjectGrid3D)field).field;
            for(int x=0;x<grid.length;x++)
                {
                Object[][] gridx = grid[x];
                for(int y=0;y<gridx.length;y++)
                    {
                    Object[] gridy = gridx[y];
                    for(int z=0;z<gridy.length;z++)
                        {
                        Object o = gridy[z];
                        // get the child model -- it doesn't exist yet
                        Portrayal p = getPortrayalForObject(o);
                        if(! (p instanceof SimplePortrayal3D))
                            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                                o + " -- expecting a SimplePortrayal3D");
                        SimplePortrayal3D p3d = (SimplePortrayal3D)p;
                        p3d.setParentPortrayal(this);
                        TransformGroup newTransformGroup = p3d.getModel(o, null);
                        newTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
                        newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                        newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                        // this is set so he'll be in the scenegraph path
                        newTransformGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
                        tmpLocalT.setTranslation(new Vector3d(x,y,z));
                        newTransformGroup.setUserData(new Int3D(x,y,z));
                        newTransformGroup.setTransform(tmpLocalT);
                                                
                        BranchGroup bg = new BranchGroup();
                        bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                        bg.setCapability(BranchGroup.ALLOW_DETACH);
                        // this is set so he'll be in the scenegraph path
                        bg.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
                        bg.addChild(newTransformGroup);
                        bg.setUserData(o);
                        global.addChild(bg);
                        }
                    }
                }
            }
        
        return globalTG;
        }
    
        
        
        
        
        
        
    protected void updateModel(TransformGroup globalTG)
        {
        Group global = (Group)(globalTG.getChild(0));
                
        if (field==null) return;
                
        int count = 0;

        // first, let's pass through the field to see who is DEFINITELY going to have their
        // models removed.  If so, we hash those models by the object, making them available
        // for others.  This is more efficient than lots of recreation.  We hope!
        HashMap models = new HashMap();

        Transform3D tmpLocalT = new Transform3D();
        count = 0;
        if (field instanceof ObjectGrid2D)
            {
            Object[][] grid = ((ObjectGrid2D)field).field;
            for(int x=0;x<grid.length;x++)
                {
                Object[] gridx = grid[x];
                for(int y=0;y<gridx.length;y++)
                    {
                    Object o = gridx[y];
                    // get the child model -- it doesn't exist yet
                    Portrayal p = getPortrayalForObject(o);
                    if(! (p instanceof SimplePortrayal3D))
                        throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                            o + " -- expecting a SimplePortrayal3D");
                    SimplePortrayal3D p3d = (SimplePortrayal3D)p;
                    p3d.setParentPortrayal(this);
                    BranchGroup bg = (BranchGroup)(global.getChild(count++));
                    TransformGroup originalTransformGroup = null;
                    if (bg.numChildren() > 0) originalTransformGroup = (TransformGroup)(bg.getChild(0));  // could be null if we've stubbed
                    TransformGroup newTransformGroup = null;
                    Object originalData = bg.getUserData();
                    if (originalData == o)
                        newTransformGroup = p3d.getModel(o, originalTransformGroup);
                    else 
                        {
                        Bag b = (Bag)(models.get(o));
                        if (b!=null && b.numObjs > 0)
                            {
                            // yay, we can reuse an existing model
                            BranchGroup replacementBranchGroup = (BranchGroup)(b.remove(0));
                            originalTransformGroup = (TransformGroup)(replacementBranchGroup.getChild(0));
                            newTransformGroup = p3d.getModel(o,originalTransformGroup);
                            if (newTransformGroup == originalTransformGroup)  // we can stick the BranchGroup in
                                global.setChild(replacementBranchGroup,count-1);
                            }
                        else 
                            // shoot, we have to create a new model.  Rebuild.
                            newTransformGroup = p3d.getModel(o, null);
                        }
                                                
                    // is the new transformGroup different?
                    if (newTransformGroup != originalTransformGroup)
                        {
                        // dang!
                        newTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
                        newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                        newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                        // this is set so he'll be in the scenegraph path
                        newTransformGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
                                                
                        BranchGroup bg2 = new BranchGroup();
                        bg2.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                        bg2.setCapability(BranchGroup.ALLOW_DETACH);
                        // this is set so he'll be in the scenegraph path
                        bg2.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
                        tmpLocalT.setTranslation(new Vector3d(x,y,0));
                        newTransformGroup.setTransform(tmpLocalT);
                        newTransformGroup.setUserData(new Int2D(x,y));
                        bg2.addChild(newTransformGroup);
                        bg2.setUserData(o);
                        global.setChild(bg2,count-1);

                        // add old BranchGroup to the hashmap
                        Bag b = (Bag)(models.get(originalData));
                        if (b==null) { b = new Bag(); models.put(originalData,b); }
                        b.add(bg);
                        }
                    }
                }
            }
        else // field instanceof ObjectGrid3D
            {
            Object[][][] grid = ((ObjectGrid3D)field).field;
            for(int x=0;x<grid.length;x++)
                {
                Object[][] gridx = grid[x];
                for(int y=0;y<gridx.length;y++)
                    {
                    Object[] gridy = gridx[y];
                    for(int z=0;z<gridy.length;z++)
                        {
                        Object o = gridy[z];
                        // get the child model -- it doesn't exist yet
                        Portrayal p = getPortrayalForObject(o);
                        if(! (p instanceof SimplePortrayal3D))
                            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                                o + " -- expecting a SimplePortrayal3D");
                        SimplePortrayal3D p3d = (SimplePortrayal3D)p;
                        p3d.setParentPortrayal(this);
                        BranchGroup bg = (BranchGroup)(global.getChild(count++));
                        TransformGroup originalTransformGroup = null;
                        if (bg.numChildren() > 0) originalTransformGroup = (TransformGroup)(bg.getChild(0));  // could be null if we've stubbed
                        TransformGroup newTransformGroup = null;
                        Object originalData = bg.getUserData();
                        if (originalData == o)
                            newTransformGroup = p3d.getModel(o, originalTransformGroup);
                        else
                            {
                            Bag b = (Bag)(models.get(o));
                            if (b!=null && b.numObjs > 0)
                                {
                                // yay, we can reuse an existing model
                                BranchGroup replacementBranchGroup = (BranchGroup)(b.remove(0));
                                originalTransformGroup = (TransformGroup)(replacementBranchGroup.getChild(0));
                                newTransformGroup = p3d.getModel(o,originalTransformGroup);
                                if (newTransformGroup == originalTransformGroup)  // we can stick the BranchGroup in
                                    global.setChild(replacementBranchGroup,count-1);
                                }
                            else
                                // shoot, we have to create a new model.  Rebuild.
                                newTransformGroup = p3d.getModel(o, null);
                            }
                                                
                        // is the new transformGroup different?
                        if (newTransformGroup != originalTransformGroup)
                            {
                            // dang!
                            newTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
                            newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                            newTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                            // this is set so he'll be in the scenegraph path
                            newTransformGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
                                                        
                            BranchGroup bg2 = new BranchGroup();
                            bg2.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                            bg2.setCapability(BranchGroup.ALLOW_DETACH);
                            // this is set so he'll be in the scenegraph path
                            bg2.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
                            tmpLocalT.setTranslation(new Vector3d(x,y,z));
                            newTransformGroup.setTransform(tmpLocalT);
                            newTransformGroup.setUserData(new Int3D(x,y,z));
                            bg2.addChild(newTransformGroup);
                            bg2.setUserData(o);
                            global.setChild(bg2,count-1);

                            // add old BranchGroup to the hashmap
                            Bag b = (Bag)(models.get(originalData));
                            if (b==null) { b = new Bag(); models.put(originalData,b); }
                            b.add(bg);
                            }
                        }
                    }
                }
            }
        }




    public void setField(Object field)
        {
        dirtyField = true;
        if (field instanceof ObjectGrid3D || field instanceof ObjectGrid2D) this.field = field;
        else throw new RuntimeException("Invalid field for ObjectGridPortrayal3D: " + field);
        }
        
                




    // searches for an object within a short distance of a location
    final int SEARCH_DISTANCE = 2;
    IntBag xPos = new IntBag(49);
    IntBag yPos = new IntBag(49);
    IntBag zPos = new IntBag(49);
        
    Int3D searchForObject(Object object, Int3D loc)
        {
        ObjectGrid3D field = (ObjectGrid3D)(this.field);
        Object[][][] grid = field.field;
        if (grid[loc.x][loc.y][loc.z] == object)
            return new Int3D(loc.x, loc.y, loc.z);
        field.getNeighborsMaxDistance(loc.x, loc.y, loc.z, SEARCH_DISTANCE, true, xPos, yPos, zPos);
        for(int i=0;i<xPos.numObjs;i++)
            if (grid[xPos.get(i)][yPos.get(i)][zPos.get(i)] == object) return new Int3D(xPos.get(i), yPos.get(i), zPos.get(i));
        return null;
        }


    final ObjectGridPortrayal2D.Message unknown = new ObjectGridPortrayal2D.Message("It's too costly to figure out where the object went.");
    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr)     
        {
        // find the global transform group.
        SceneGraphPath path = pr.getSceneGraphPath();
        int len = path.nodeCount();
        boolean found = false;

        int i=0;
        for(i=0;i<len;i++)
            {
            Node node = path.getNode(i);
            Object userdata = path.getNode(i).getUserData();
            if (userdata == this && node instanceof Group)
                {
                found = true; break;  
                }
            }
        if (!found) throw new RuntimeException(
            "Internal error: ObjectGridPortrayal3D.completedWrapper() couldn't find the root transform group.  This shouldn't happen.");
        
        Object location = path.getNode(i+2).getUserData();  // back off to the TransformGroup
        
        final ObjectGrid3D field = (ObjectGrid3D)(this.field);
        return new LocationWrapper(w.getObject(), location, this)
            {
            public Object getLocation()
                { 
                Int3D loc = (Int3D) super.getLocation();
                if (field.field[loc.x][loc.y][loc.z] == getObject())  // it's still there!
                    {
                    return loc;
                    }
                else
                    {
                    Int3D result = searchForObject(object, loc);
                    if (result != null)  // found it nearby
                        {
                        location = result;
                        return result;
                        }
                    else    // it's moved on!
                        {
                        return unknown;
                        }
                    }
                }
            
            public String getLocationName()
                {
                Object loc = getLocation();
                if (loc instanceof Int3D)
                    return ((Int3D)this.location).toCoordinates();
                else return "Location Unknown";
                }


            /*
              public Object getObject()
              { 
              if (this.location instanceof Int3D)
              {
              Int3D loc = (Int3D)this.location;
              return ((ObjectGrid3D)field).field[loc.x][loc.y][loc.z];
              }
              else
              {
              Int2D loc = (Int2D)this.location;
              return ((ObjectGrid2D)field).field[loc.x][loc.y];
              }
              }
            
              public String getLocationName()
              {
              if (this.location == null) return null;
              if (this.location instanceof Int3D)
              return ((Int3D)this.location).toCoordinates();
              else return ((Int2D)this.location).toCoordinates();
              }
            */
            };
        }
    }
