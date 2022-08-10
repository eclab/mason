package sim.app.geo.dschellingpolygon;

import java.util.ArrayList;

import sim.app.geo.schellingpolygon.Person;
import sim.app.geo.schellingpolygon.Polygon;
import sim.engine.DSimState;
import sim.engine.SimState;
import sim.util.geo.DGeomObject;
import sim.util.geo.DGeomSteppable;
import sim.util.geo.MasonGeometry;

public class DPolygon extends DGeomSteppable{
	
    int id = -1;
    String soc;
    
    

    public ArrayList<DPerson> residents;
    public ArrayList<DPolygon> neighbors;



    public DPolygon()
    {
        super();
        residents = new ArrayList<DPerson>();
        neighbors = new ArrayList<DPolygon>();
    }

    public DPolygon(MasonGeometry mg)
    {
        super();
        this.mg = mg;
        residents = new ArrayList<DPerson>();
        neighbors = new ArrayList<DPolygon>();
    }

    public void init()
    {
        id = this.getMasonGeometry().getDoubleAttribute("ID_ID").intValue();
        soc = this.getMasonGeometry().getStringAttribute("SOC");
    }



    public int getID()
    {
        if (id == -1)
        {
            init();
        }
        return id;
    }



    public String getSoc()
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
