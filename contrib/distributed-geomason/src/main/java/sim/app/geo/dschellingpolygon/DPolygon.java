package sim.app.geo.dschellingpolygon;

import java.util.ArrayList;

import sim.app.geo.schellingpolygon.Person;
import sim.app.geo.schellingpolygon.Polygon;
import sim.engine.DSimState;
import sim.engine.SimState;
import sim.util.geo.DGeomObject;
import sim.util.geo.DGeomSteppable;

public class DPolygon extends DGeomSteppable{
	
    int id = -1;
    String soc;
    
    

    ArrayList<DPerson> residents;
    ArrayList<DPolygon> neighbors;



    public DPolygon()
    {
        super();
        residents = new ArrayList<DPerson>();
        neighbors = new ArrayList<DPolygon>();
    }



    public void init()
    {
        id = this.getMasonGeometry().getDoubleAttribute("ID_ID").intValue();
        soc = this.getMasonGeometry().getStringAttribute("SOC");
    }



    int getID()
    {
        if (id == -1)
        {
            init();
        }
        return id;
    }



    String getSoc()
    {
        if (soc == null)
        {
            init();
        }
        return soc;
    }



	@Override
	public void step(SimState state) {
		// TODO Auto-generated method stub
		
		for (DPerson p : residents) {
			p.moveToBetterLocation(state);
		}
		
	}

}
