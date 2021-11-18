package sim.util.geo;

import sim.engine.DObject;
import sim.util.Double2D;

public abstract class DGeomObject extends DObject{

	protected MasonGeometry mg;
	Double2D jtsCoordinate; //can we align this with DGeomVectorField continuous half?

	
	public MasonGeometry getMasonGeometry() {
		//need to adapt this correctly?  location changes may need to change this (see DAgent in DCampusWorld)
		return mg;
	}

	
}
