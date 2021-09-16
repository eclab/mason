package sim.util.geo;

//this holds MasonGeometry while extending DObject for DContinuousStorage usage

import sim.engine.DObject;

public class MasonGeometryWrapper extends DObject{
	
	MasonGeometry masonGeom;
	
	public MasonGeometryWrapper(MasonGeometry masonGeom)
	{
		super();
		this.masonGeom = masonGeom;
	}
	
	public MasonGeometry getMasonGeometry() {
		return masonGeom;
	}
	
	public void setMasonGeometry(MasonGeometry masonGeom) {
		this.masonGeom = masonGeom;
	}
	

}
