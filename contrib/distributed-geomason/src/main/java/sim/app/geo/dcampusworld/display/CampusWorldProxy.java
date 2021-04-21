package sim.app.geo.dcampusworld.display;

import sim.display.*;
import sim.field.geo.GeomVectorField;
import sim.portrayal.geo.GeomVectorFieldPortrayal;

public class CampusWorldProxy extends SimStateProxy
  {
  private static final long serialVersionUID = 1;

  public CampusWorldProxy(long seed)
      {
      super(seed);
		setRegistryHost("localhost");
		//setRegistryPort(5000);
      }
      
  //TODO
  double discretization = 6;
//  
//  GeomVectorFieldPortrayal walkwaysPortrayal = new GeomVectorFieldPortrayal();
//  GeomVectorFieldPortrayal buildingPortrayal = new GeomVectorFieldPortrayal();
//  GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal();
  
//  ObjectGrid2DProxy walkways = new ObjectGrid2DProxy(1, 1);
//  ObjectGrid2DProxy buildings = new ObjectGrid2DProxy(1, 1);
//  ObjectGrid2DProxy roads = new ObjectGrid2DProxy(1, 1);
  GeomVectorFieldProxy walkways = new GeomVectorFieldProxy();
  GeomVectorFieldProxy buildings = new GeomVectorFieldProxy();
  GeomVectorFieldProxy roads = new GeomVectorFieldProxy();
  
  Continuous2DProxy agents = new Continuous2DProxy(discretization, 1, 1);

  public void start()
      {
      super.start();
      	//TODO indexing. Needs to match same index ordering as...
//		registerFieldProxy(walkwaysPortrayal, 0);
//		registerFieldProxy(buildingPortrayal, 1);
//		registerFieldProxy(roadsPortrayal, 2);
      
//		registerFieldProxy(walkways, 0);
//		registerFieldProxy(buildings, 1);
//		registerFieldProxy(roads, 2);
		registerFieldProxy(agents, 0);
//      	registerFieldProxy(agents, 0);
      }
  }
  
  
  
  
  
