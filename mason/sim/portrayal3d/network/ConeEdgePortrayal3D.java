/*
 * Created on Nov 18, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package sim.portrayal3d.network;

import java.awt.Color;
import javax.media.j3d.Appearance;
import sim.portrayal3d.*;

import com.sun.j3d.utils.geometry.Cone;

/**
 * @author Gabriel Balan
 *
 */
public class ConeEdgePortrayal3D extends GenericEdgePortrayal3D
    {
    static class PickableCone extends Cone
        {
        public PickableCone()
            {
            super();
            setAllPickableFlags();
            }
                
        public PickableCone(float radius)
            {
            super(radius, 2);
            setAllPickableFlags();
            }
                
        public PickableCone(float radius, Appearance ap)
            {
            super(radius, 2, ap);
            setAllPickableFlags();
            }
        public PickableCone(Appearance ap)
            {
            super(1, 2, ap);
            setAllPickableFlags();
            }
        
        private void setAllPickableFlags()
            {
            SimplePortrayal3D.setPickableFlags(getShape(Cone.CAP));
            SimplePortrayal3D.setPickableFlags(getShape(Cone.BODY));
            }        
        }

    public ConeEdgePortrayal3D()
        {
        super(new PickableCone());
        }
        
    public ConeEdgePortrayal3D(float coneBaseRadius)
        {
        super(new PickableCone(coneBaseRadius));
        }
        
    public ConeEdgePortrayal3D(float coneBaseRadius, Appearance ap)
        {
        super(new PickableCone(coneBaseRadius, ap));
        }       


    public ConeEdgePortrayal3D(Color labelColor)
        {
        super(new PickableCone(), labelColor);
        }

    public ConeEdgePortrayal3D(Appearance edgeAppearance, Color labelColor)
        {
        super(new PickableCone(edgeAppearance), labelColor);
        }
    }
