/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal3d.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import sim.util.*;

/** A simple Portrayal3D which provides ambient, directional, or point light to the scene.  While this could be used
    in a FieldPortrayal to represent its objects, it's more likely to be dropped directly into the Display3D itself.  Your
    light can be any color, and it will have infinite bounds -- it irradiates everything regardless of where it is -- meaning there's no such thing as a shadow.  LightPortrayals aren't selectable: how can you catch a moonbeam in your hand?

    <ul>
    <li><b>Ambient Light</b> is light which seems to come from all around and has no direct source.
    <li><b>Directional Light</b> comes from a point light source located infinitely far away.  Thus there is a <i>direction</i> in which the light is shining.  The classic example: sunlight.
    <li><b>Point Light</b> comes from a point light source located at a finite position (like a light bulb).  If you place a point light source in a field portrayal, the position you'd specified here will get scaled and translated just like any other object.  Point light also has an <i>attenuation</i> -- a degree to which its strength drops off.  The attenuation is calculated as an equation on the distance <i>d</i> that the object is from the light source: q^2&nbsp;*&nbsp;d&nbsp;+&nbsp;l&nbsp;*&nbsp;d&nbsp;+&nbsp;c, where  q is the <i>quadratic attenuation</i>, l is the <i>linear attenuation</i> and c is the <i>constant attenuation</i>.  A good default is c=1,l=0,q=0.
    </ul>

    <p>In fact, the default objects provided in the simulator (such as SpherePortrayal3D) don't respond to light at all -- they
    just display themselves with their given color as if there were a magical light source.  To get them to respond
    in a different fashion, you'll need to provide them with a different Appearance object, and set that Appearance's
    ColoringAttributes and Material.  Here's an example which makes a green sphere that's harshly lit only on the side where it receives light, else it's jet black.

    <tt><pre>
    <i>import javax.media.j3d.*;
    import javax.vecmath.*;</i>

    Color3f color = new Color3f(java.awt.Color.green);
    Appearance appearance = new Appearance();
    appearance.setColoringAttributes(
    new ColoringAttributes(color, ColoringAttributes.SHADE_GOURAUD));           
    Material m= new Material();
    m.setAmbientColor(color);
    m.setEmissiveColor(0f,0f,0f);
    m.setDiffuseColor(color);
    m.setSpecularColor(1f,1f,1f);
    m.setShininess(128f);
    appearance.setMaterial(m);
            
    SpherePortrayal3D sphere = new SpherePortrayal3D(appearance, 1.0f);
    </pre></tt>
*/

public class LightPortrayal3D extends SimplePortrayal3D
    {
    public Light light;
    
    Vector3f double3DToVector3f(Double3D d)
        {
        Vector3f v = new Vector3f();
        v.x = (float)d.x; v.y = (float)d.y; v.z = (float)d.z;
        return v;
        }

    /** Directional Light */
    public LightPortrayal3D(java.awt.Color color, Double3D direction)
        {
        light = new DirectionalLight(new Color3f(color),double3DToVector3f(direction));
        light.setInfluencingBounds(new BoundingSphere(new Point3d(0,0,0), Double.POSITIVE_INFINITY));
        }
        
    /** Ambient Light */
    public LightPortrayal3D(java.awt.Color color)
        {
        light = new AmbientLight(new Color3f(color));
        light.setInfluencingBounds(new BoundingSphere(new Point3d(0,0,0), Double.POSITIVE_INFINITY));
        }

    /** Point Light.  If you don't know what to provide for attenutation, you can't go wrong with 1,0,0. */
    public LightPortrayal3D(java.awt.Color color, Double3D position, 
        float constantAttenuation, float linearAttenuation, float quadraticAttenuation)
        {
        PointLight p = new PointLight();
        p.setAttenuation(constantAttenuation, linearAttenuation, quadraticAttenuation);
        p.setPosition((float)position.x,(float)position.y,(float)position.z);
        light = p;
        light.setColor(new Color3f(color));
        light.setInfluencingBounds(new BoundingSphere(new Point3d(0,0,0), Double.POSITIVE_INFINITY));
        }
        
    /** Provide your own Light! */
    public LightPortrayal3D(Light light)
        {
        this.light = light;
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            Light l = (Light)(light.cloneTree(false));
            clearPickableFlags(l);  // make un-pickable.  How do you catch a moonbeam in your hand?
            j3dModel.addChild(l);
            }
        return j3dModel;
        }
    }
