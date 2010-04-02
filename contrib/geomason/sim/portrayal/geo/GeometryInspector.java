package sim.portrayal.geo; 

import sim.portrayal.*; 
import sim.display.*;
import java.awt.*; 
import sim.util.geo.*; 
import sim.io.geo.*; 

/** 
 GeometryInspector displays two SimpleInspectors: the first SimpleInspector displays information gathered 
 the standard way (i.e., using getXXX and setXXX methods).  The second SimpleInspector displays an objects 
 underlying metdadata (help in a GeomWrapper object).
 */

public class GeometryInspector extends SimpleInspector { 

	/** Inspector for metadata */ 
	SimpleInspector otherInspector; 
	
	/** Default constructor */ 
	public GeometryInspector(Object o, GUIState state, String name)
	{
		super(o, state, name); 
		if (o instanceof GeomWrapper) {
			GeomWrapper mg = (GeomWrapper) o;
			GeometricProperties properties = new GeometricProperties(mg.fetchGeometryInfo()); 
			otherInspector = new SimpleInspector(properties, state, name); 
			add(otherInspector, BorderLayout.SOUTH); 
			revalidate(); 
		}
	}
	
	/** Update both SimpleInspectors */ 
	public void updateInspector()
	{
		super.updateInspector(); 
		if (otherInspector != null)  
			otherInspector.updateInspector(); 
	}

}
