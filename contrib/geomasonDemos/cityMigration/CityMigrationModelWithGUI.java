package cityMigration;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import sim.display.Console;

import riftland.PopulationCenter;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.portrayal.network.stats.SocialNetworkInspector;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.gui.SimpleColorMap;

public class CityMigrationModelWithGUI extends GUIState
{

    static public boolean preciseFills = false;
    static public boolean preciseDraws = false;
    
	private CityMigrationModel model;
	public Display2D display;
	private JFrame displayFrame;
	private ContinuousPortrayal2D citiesPortrayal = new ContinuousPortrayal2D();
	private ContinuousPortrayal2D cityIDPsPortrayal = new ContinuousPortrayal2D();
	private ContinuousPortrayal2D cityCapacityPortrayal = new ContinuousPortrayal2D();
	private NetworkPortrayal2D cityInteractionPortrayal = new NetworkPortrayal2D();
	private SimpleEdgePortrayal2D cityEdgePortrayal = new SimpleEdgePortrayal2D(Color.darkGray, Color.lightGray, null);

	private ContinuousPortrayal2D roadNodePortrayal = new ContinuousPortrayal2D();
	private NetworkPortrayal2D roadNetworkPortrayal = new NetworkPortrayal2D();
	private SimpleEdgePortrayal2D roadEdgePortrayal = new SimpleEdgePortrayal2D(Color.gray, null);

	private NetworkPortrayal2D roadProblemPortrayal = new NetworkPortrayal2D();
	private SimpleEdgePortrayal2D roadProblemEdgePortrayal = new SimpleEdgePortrayal2D(Color.red, null);

	private ContinuousPortrayal2D roadNodeSimpPortrayal = new ContinuousPortrayal2D();
	private NetworkPortrayal2D roadNetworkSimpPortrayal = new NetworkPortrayal2D();
	private SimpleEdgePortrayal2D roadEdgeSimpPortrayal = new SimpleEdgePortrayal2D(Color.darkGray, null);
	
	private ContinuousPortrayal2D roadNodeSimpWeightedPortrayal = new ContinuousPortrayal2D();
	private NetworkPortrayal2D roadNetworkSimpWeightedPortrayal = new NetworkPortrayal2D();
	private SimpleEdgePortrayal2D roadEdgeSimpWeightedPortrayal = new SimpleEdgePortrayal2D(Color.black, null);
	

	private NetworkPortrayal2D shortestPathPortrayal = new NetworkPortrayal2D();
	private SimpleEdgePortrayal2D shortestPathEdgePortrayal = new SimpleEdgePortrayal2D(Color.orange, null);
	
	
//	private NetworkPortrayal2D tinPortrayal = new NetworkPortrayal2D();
//	private SimpleEdgePortrayal2D tinEdgePortrayal = new SimpleEdgePortrayal2D(Color.darkGray, null);
	
    private GeomVectorFieldPortrayal tinNodesPortrayal = new GeomVectorFieldPortrayal(false);
    private GeomVectorFieldPortrayal tinEdgesPortrayal = new GeomVectorFieldPortrayal(false);

//    private FastValueGridPortrayal2D urbanUsePortrayal = new FastValueGridPortrayal2D();
//    private FastValueGridPortrayal2D landUsePortrayal = new FastValueGridPortrayal2D();

    public GeomVectorFieldPortrayal countriesPortrayal = new GeomVectorFieldPortrayal();
    public GeomVectorFieldPortrayal countryBoundariesPortrayal = new GeomVectorFieldPortrayal();
    
    SocialNetworkInspector inspector = new SocialNetworkInspector();

	public CityMigrationModelWithGUI(SimState state) {
		super(state);
		model = (CityMigrationModel) state;
	}

	public CityMigrationModelWithGUI() {
		super(new CityMigrationModel(System.currentTimeMillis()));
		model = (CityMigrationModel) state;
	}

	public static String getName() {
		return "City Migration Model";
	}

	@Override
	public Object getSimulationInspectedObject() {
		return state;
	} // non-volatile

	@SuppressWarnings("serial")
	public void setupPortrayals() {

		cityEdgePortrayal.setShape(0);
		cityEdgePortrayal.setScaling(0);
		cityEdgePortrayal.setBaseWidth(0);
		cityInteractionPortrayal.setField(new SpatialNetwork2D(model.cityInteractionNetwork.nodeField, model.cityInteractionNetwork.edgeField));
		cityInteractionPortrayal.setPortrayalForAll(cityEdgePortrayal);
		setEdgeColors();

		citiesPortrayal.setField(model.cityInteractionNetwork.nodeField);
		cityIDPsPortrayal.setField(model.cityInteractionNetwork.nodeField);
		cityCapacityPortrayal.setField(model.cityInteractionNetwork.nodeField);
		
		roadNodePortrayal.setField(model.roadNetwork.nodeField);
		roadEdgePortrayal.setShape(0);
		roadEdgePortrayal.setScaling(0);
		roadEdgePortrayal.setBaseWidth(0);
		roadNetworkPortrayal.setField(new SpatialNetwork2D(model.roadNetwork.nodeField, model.roadNetwork.edgeField));
		roadNetworkPortrayal.setPortrayalForAll(roadEdgePortrayal);

		roadProblemPortrayal.setField(new SpatialNetwork2D(model.roadNetwork.nodeField, model.roadNetwork.roadProblemField));
		roadProblemPortrayal.setPortrayalForAll(roadProblemEdgePortrayal);

		//roadNodeSimpPortrayal.setField(model.roadNetworkSimp.nodeField); // this is done below
		roadEdgeSimpPortrayal.setShape(0);
		roadEdgeSimpPortrayal.setScaling(0);
		roadEdgeSimpPortrayal.setBaseWidth(0);
		roadNetworkSimpPortrayal.setField(new SpatialNetwork2D(model.roadNetworkSimp.nodeField, model.roadNetworkSimp.edgeField));
		roadNetworkSimpPortrayal.setPortrayalForAll(roadEdgeSimpPortrayal);
		
		roadEdgeSimpWeightedPortrayal.setShape(0);
		roadEdgeSimpWeightedPortrayal.setScaling(5);
		roadEdgeSimpWeightedPortrayal.setBaseWidth(5);
		roadEdgeSimpWeightedPortrayal.setAdjustsThickness(true);
		roadNetworkSimpWeightedPortrayal.setField(new SpatialNetwork2D(model.roadNetworkSimpWeighted.nodeField, model.roadNetworkSimpWeighted.edgeField));
		roadNetworkSimpWeightedPortrayal.setPortrayalForAll(roadEdgeSimpWeightedPortrayal);
		
		shortestPathEdgePortrayal.setShape(SimpleEdgePortrayal2D.SHAPE_LINE_ROUND_ENDS);
		shortestPathEdgePortrayal.setBaseWidth(7);
		shortestPathPortrayal.setField(new SpatialNetwork2D(model.workingNetwork.nodeField, model.shortestPathDebug));
		shortestPathPortrayal.setPortrayalForAll(shortestPathEdgePortrayal);
		
		roadNodePortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.red) {
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				if (((PopulationCenter)object).getUrbanites() > 0)
					this.paint = Color.blue;
				else
					this.paint = Color.gray;
				scale = 100 * model.roadNodeSizeMultiplier;
				super.draw(object, graphics, info);
			}
			
			public boolean hitObject(Object object, DrawInfo2D range) {
				scale = 100 * model.roadNodeSizeMultiplier;
				return super.hitObject(object, range);
			}
		});	
		
		// just use the same one
		roadNodeSimpPortrayal.setPortrayalForAll(roadNodePortrayal.getPortrayalForAll());
		roadNodeSimpPortrayal.setField(model.roadNetworkSimp.nodeField);
		
		roadNodeSimpWeightedPortrayal.setPortrayalForAll(roadNodePortrayal.getPortrayalForAll());
		roadNodeSimpWeightedPortrayal.setField(model.roadNetworkSimpWeighted.nodeField);
		
		citiesPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.red) {
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				scale = Math.sqrt(((PopulationCenter)object).getUrbanites()) * model.citySizeMultiplier;
				if (preciseFills)
					info.precise = true;	// needed
				super.draw(object, graphics, info);
			}
			
			public boolean hitObject(Object object, DrawInfo2D range) {
				scale = Math.sqrt(((PopulationCenter)object).getUrbanites()) * model.citySizeMultiplier;
				return super.hitObject(object, range);
			}
		});

		cityIDPsPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.blue) {
			public double getScale(Object object) {
				return Math.sqrt(((PopulationCenter)object).getNumRefugees()) * model.cityIDPsSizeMultiplier;				
			}
			
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				scale = Math.sqrt(((PopulationCenter)object).getNumRefugees()) * model.cityIDPsSizeMultiplier;
				if (preciseFills)
					info.precise = true;	// needed 
				super.draw(object, graphics, info);
			}
//			
//			public boolean hitObject(Object object, DrawInfo2D range) {
//				scale = Math.sqrt(((PopulationCenter)object).getNumRefugees()) * model.cityIDPsSizeMultiplier;
//				return super.hitObject(object, range);
//			}
		});

		cityCapacityPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.black, false) {
			public double getScale(Object object) {
				return Math.sqrt(((PopulationCenter)object).getRefugeeCapacity()) * model.cityIDPsSizeMultiplier;				
			}
			
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				scale = Math.sqrt(((PopulationCenter)object).getRefugeeCapacity()) * model.cityIDPsSizeMultiplier;
				if (preciseDraws)
					info.precise = true;
				super.draw(object, graphics, info);
			}
//
//			public boolean hitObject(Object object, DrawInfo2D range) {
//				scale = Math.sqrt(((PopulationCenter)object).getRefugeeCapacity()) * model.cityIDPsSizeMultiplier;
//				return super.hitObject(object, range);
//			}
		});

		
//		inspector.setField(model.cityInteractionNetwork.edgeField, this);
		
		// TIN nodes and edges
		tinNodesPortrayal.setField(model.cityTIN.nodes);

		tinNodesPortrayal.setPortrayalForAll(new GeomPortrayal(Color.blue) {
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				scale = model.citySizeMultiplier;
				super.draw(object, graphics, info);
			}
			
			public boolean hitObject(Object object, DrawInfo2D range) {
				scale = model.citySizeMultiplier;
				return super.hitObject(object, range);
			}
		});
		tinEdgesPortrayal.setField(model.cityTIN.edges);
		tinEdgesPortrayal.setPortrayalForAll(new GeomPortrayal(Color.BLACK, false));
		
		countriesPortrayal.setField(model.politicalBoundaries);
		countriesPortrayal.setPortrayalForAll(new GeomPortrayal(new Color(0xFFFFCC), true));

		countryBoundariesPortrayal.setField(model.politicalBoundaries);
		countryBoundariesPortrayal.setPortrayalForAll(new GeomPortrayal(Color.LIGHT_GRAY, 3, false) {
			@Override
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				Stroke oldStroke = graphics.getStroke();
				graphics.setStroke(new BasicStroke((float)scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				super.draw(object, graphics, info);
				graphics.setStroke(oldStroke);
			}
		});

//		urbanUsePortrayal.setField(model.urbanUseGrid.getGrid());
//		urbanUsePortrayal.setMap(new SimpleColorMap(-9999, 400, Color.white, Color.black));
		
//		landUsePortrayal.setField(model.landUseGrid.getGrid());
//		landUsePortrayal.setMap(new SimpleColorMap(0, 0.0001, new Color(0x00FFFF), Color.white));	// note: the real range is 0-1. reducing the range will show only the water

		display.reset();
		display.setBackdrop(Color.white);
		display.repaint();

	}

	@Override
	public void init(Controller c) {
		super.init(c);
		display = new Display2D(600, 600, this);
		display.setClipping(false);
		displayFrame = display.createFrame();
		displayFrame.setTitle("Cities Display");
		c.registerFrame(displayFrame); // so the frame appears in the "Display"
		// list
		displayFrame.setVisible(true);

		display.attach(countriesPortrayal, "Countries", true);
		display.attach(countryBoundariesPortrayal, "Country Boundaries", true);
//		display.attach(urbanUsePortrayal, "Urban Use");
//		display.attach(landUsePortrayal, "Land Use", true);
		display.attach(shortestPathPortrayal, "Shortest Path", true);
		display.attach(cityInteractionPortrayal, "cityInteractions", false);
		display.attach(roadNetworkSimpPortrayal, "Simplified Road Network", false);
		display.attach(roadNetworkSimpWeightedPortrayal, "Simplified Weighted Road Network", true);
		display.attach(tinEdgesPortrayal, "TIN Edges", false);
		display.attach(citiesPortrayal, "Cities", true);
		display.attach(cityIDPsPortrayal, "IDPs", true);
		display.attach(cityCapacityPortrayal, "Capacity", true);
        display.attach(inspector, "Inspector");
		display.attach(roadNodeSimpPortrayal, "Simplified Road Nodes", false);
		display.attach(roadNodePortrayal, "Road Nodes", false);
		display.attach(roadNetworkPortrayal, "Road Network", false);
		display.attach(roadProblemPortrayal, "Road Network Problems", false);


//		display.attach(tinNodesPortrayal, "TIN Nodes");
        
        model.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				if (arg0.getPropertyName().equals("NetworkFormationMethod"))
					setEdgeColors();
				if (inspector.isVisible())
					inspector.updateInspector();
				if (arg0.getPropertyName().equals("WorkingNetwork"))
					shortestPathPortrayal.setField(new SpatialNetwork2D(model.workingNetwork.nodeField, model.shortestPathDebug));
			}			
		});
        

        display.insideDisplay.setupHints(true, false, false);
		((Console)controller).setSize(450, 740);
	}
	
	private void setEdgeColors() {

		if (model.networkFormationMethod == 3) {	// TIN is undirected
			cityEdgePortrayal.fromPaint = Color.lightGray;
			cityEdgePortrayal.toPaint = Color.lightGray;
		}
		else {	
			cityEdgePortrayal.fromPaint = Color.lightGray; // TRG changed from darkGray for paper illustrations
			cityEdgePortrayal.toPaint = Color.lightGray;
		}
	}

    @Override
    public Inspector getInspector()
    {
        super.getInspector();

        TabbedInspector i = new TabbedInspector();
        
//        i.setVolatile(true);
        i.addInspector(new SimpleInspector(new ModelProperties(this), this), "Model");
        i.addInspector(new SimpleInspector(new IDPInputProperties(this), this), "IDP Input");
        i.addInspector(new SimpleInspector(new DebugProperties((CityMigrationModel) state), this), "Debug");

        return i;
    }

	@Override
	public void start() {
		super.start();
		setupPortrayals();
	}

	public void quit() {
		super.quit();

		if (displayFrame != null) displayFrame.dispose();
		displayFrame = null; // let gc
		display = null; // let gc
	}

	public static void main(String[] args) {
		new CityMigrationModelWithGUI().createController();

	}
	




    public static class ModelProperties
    {
    	CityMigrationModelWithGUI modelWithGUI;
    	CityMigrationModel model;

        private ModelProperties(CityMigrationModelWithGUI modelWithGUI) {
        	this.modelWithGUI = modelWithGUI;
        	this.model = modelWithGUI.model;
        }

    	public double getCitySizeMultiplier() { return model.getCitySizeMultiplier(); }
    	public void setCitySizeMultiplier(double val) { model.setCitySizeMultiplier(val); }
        public Object domCitySizeMultiplier() { return model.domCitySizeMultiplier(); }
        public String desCitySizeMultiplier() { return model.desCitySizeMultiplier(); }
        
    	public double getRoadNodeSizeMultiplier() { return model.getRoadNodeSizeMultiplier(); }
    	public void setRoadNodeSizeMultiplier(double val) { model.setRoadNodeSizeMultiplier(val); }
        public Object domRoadNodeSizeMultiplier() { return model.domRoadNodeSizeMultiplier(); }
        
    	public double getCityIDPsSizeMultiplier() { return model.getCityIDPsSizeMultiplier(); }
    	public void setCityIDPsSizeMultiplier(double val) { model.setCityIDPsSizeMultiplier(val); }
        public Object domCityIDPsSizeMultiplier() { return model.domCityIDPsSizeMultiplier(); }

        public int getMinLinksPerCity() { return model.getMinLinksPerCity(); }
    	public void setMinLinksPerCity(int val) { model.setMinLinksPerCity(val); }
        public Object domMinLinksPerCity() { return model.domMinLinksPerCity(); }

    	public int getMaxLinksPerCity() { return model.getMaxLinksPerCity(); }	
    	public void setMaxLinksPerCity(int val) { model.setMaxLinksPerCity(val); }
        public Object domMaxLinksPerCity() { return model.domMaxLinksPerCity(); }

    	public double getThresholdLinks() { return model.getThresholdLinks(); }
    	public void setThresholdLinks(double val) { model.setThresholdLinks(val); }
        public Object domThresholdLinks() { return model.domThresholdLinks(); }

    	public double getSpatialDecayExp() { return model.getSpatialDecayExp(); }
    	public void setSpatialDecayExp(double val) { model.setSpatialDecayExp(val); }
        public Object domSpatialDecayExp() { return model.domSpatialDecayExp(); }

    	public double getSimplifyRoadsDistanceExp() { return model.getSimplifyRoadsDistanceExp(); }
    	public void setSimplifyRoadsDistanceExp(double val) { model.setSimplifyRoadsDistanceExp(val); }
//        public Object domSimplifyRoadsDistanceExp() { return model.domSimplifyRoadsDistanceExp(); }

        public double getIDPLoadThreshold() { return model.getIDPLoadThreshold(); }
        public void setIDPLoadThreshold(double val) { model.setIDPLoadThreshold(val); }
        public Object domIDPLoadThreshold() { return model.domIDPLoadThreshold(); }

        public double getConsumptionRate() { return model.getConsumptionRate(); }
        public void setConsumptionRate(double val) { model.setConsumptionRate(val); }
        public Object domConsumptionRate() { return model.domConsumptionRate(); }

        public double getTravelPerDay() { return model.getTravelPerDay(); }
        public void setTravelPerDay(double val) { model.setTravelPerDay(val); }

        public boolean getMoveOneCityPerStep() { return model.getMoveOneCityPerStep(); }
        public void setMoveOneCityPerStep(boolean val) { model.setMoveOneCityPerStep(val); }

    	public double getIDPGroupSize() { return model.getIDPGroupSize(); }
    	public void setIDPGroupSize(double val) { model.setIDPGroupSize(val); }
        public Object domIDPGroupSize() { return model.domIDPGroupSize(); }
        
        public int getNetworkFormationMethod() { return model.getNetworkFormationMethod(); }
        public void setNetworkFormationMethod(int val) { model.setNetworkFormationMethod(val); }
        public Object domNetworkFormationMethod() { return model.domNetworkFormationMethod(); }

        public int getIDPsDestinationMethod() { return model.getIDPsDestinationMethod(); }
        public void setIDPsDestinationMethod(int val) { model.setIDPsDestinationMethod(val); }
        public Object domIDPsDestinationMethod() { return model.domIDPsDestinationMethod(); }
    }
    
    public static class IDPInputProperties
    {
    	CityMigrationModelWithGUI modelWithGUI;
    	CityMigrationModel model;

        private IDPInputProperties(CityMigrationModelWithGUI modelWithGUI) {
        	this.modelWithGUI = modelWithGUI;
        	this.model = modelWithGUI.model;
        }

        public int getInitialIDPsPerCity() { return model.getInitialIDPsPerCity(); }
        public void setInitialIDPsPerCity(int val) { model.setInitialIDPsPerCity(val); }
        
        public int getIDPsAddedPerStep() { return model.getIDPsAddedPerStep(); }
        public void setIDPsAddedPerStep(int val) { model.setIDPsAddedPerStep(val); }

        public int getInitialIDPsInCity() { return model.getInitialIDPsInCity(); }
        public void setInitialIDPsInCity(int val) { model.setInitialIDPsInCity(val); }
        
        public int getInitialIDPsInCityID() { return model.getInitialIDPsInCityID(); }
        public void setInitialIDPsInCityID(int val) { model.setInitialIDPsInCityID(val); }
                
        public boolean getReadDisplacementEventLog() { return model.getReadDisplacementEventLog(); }
        public void setReadDisplacementEventLog(boolean val) { model.setReadDisplacementEventLog(val); }
        
        public boolean getChooseDisplacementFile() { return false; }
        public void setChooseDisplacementFile(boolean val) { 
        	JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        	
			chooser.setFileFilter(new FileFilter() {
				public boolean accept(File f)
				{
					if (f.isDirectory()) return true;
					
					if (f.getName().endsWith(".csv"))
						return true;

					return false;
				}

				public String getDescription()
				{
					return "Displacement Event Files";
				}
			});

        	int option = chooser.showOpenDialog(((Console)modelWithGUI.controller));
            if (option == JFileChooser.APPROVE_OPTION)
            	model.displacementLogFilename = chooser.getSelectedFile().getAbsolutePath();	//TODO this might need the absolute name
        }
        
        public String getDisplacementLogFilename() { 
        	if (model.displacementLogFilename == null)
        		return null;

        	return new File(model.displacementLogFilename).getName();
        }
        
    }

    public static class DebugProperties
    {
    	CityMigrationModel model;

        private DebugProperties(CityMigrationModel model) {
        	this.model = model;
        }
        
        public int getPathTestCity1() { return model.getPathTestCity1(); }
        public void setPathTestCity1(int val) { model.setPathTestCity1(val); }
        
        public int getPathTestCity2() { return model.getPathTestCity2(); }
        public void setPathTestCity2(int val) { model.setPathTestCity2(val); }

        public int getWorkingNetwork() { return model.getWorkingNetwork(); }
        public void setWorkingNetwork(int val) { model.setWorkingNetwork(val); }
        public Object domWorkingNetwork() {	return model.domWorkingNetwork(); }

        public boolean getAnalyzeRoadNetwork() { return model.getAnalyzeRoadNetwork(); }
        public void setAnalyzeRoadNetwork(boolean val) { model.setAnalyzeRoadNetwork(val); }
        
        public boolean getSaveAllPairsShortestPath() { return model.getSaveAllPairsShortestPath(); }
        public void setSaveAllPairsShortestPath(boolean val) { model.setSaveAllPairsShortestPath(val); }
        
        public boolean getLoadAllPairsShortestPath() { return model.getLoadAllPairsShortestPath(); }
        public void setLoadAllPairsShortestPath(boolean val) { model.setLoadAllPairsShortestPath(val); }
        
        public boolean getCreateSimplifiedRoadNetwork() { return model.getCreateSimplifiedRoadNetwork(); }
        public void setCreateSimplifiedRoadNetwork(boolean val) { model.setCreateSimplifiedRoadNetwork(val); }
        
        public boolean getWriteSimplifiedRoadNetwork() { return model.getWriteSimplifiedRoadNetwork(); }
        public void setWriteSimplifiedRoadNetwork(boolean val) { model.setWriteSimplifiedRoadNetwork(val); }
        
        public boolean getReadSimplifiedRoadNetwork() { return model.getReadSimplifiedRoadNetwork(); }
        public void setReadSimplifiedRoadNetwork(boolean val) { model.setReadSimplifiedRoadNetwork(val); }

    	public double getRoadProblemDistThreshold() { return model.getRoadProblemDistThreshold(); }
    	public void setRoadProblemDistThreshold(double val) { model.setRoadProblemDistThreshold(val); }
        public Object domRoadProblemDistThreshold() { return model.domRoadProblemDistThreshold(); }
        
        public boolean getIgnoreUnconnectedPairs() { return model.getIgnoreUnconnectedPairs(); }
        public void setIgnoreUnconnectedPairs(boolean val) { model.setIgnoreUnconnectedPairs(val); }

        public boolean getTestCitiesOnly() { return model.getTestCitiesOnly(); }
        public void setTestCitiesOnly(boolean val) { model.setTestCitiesOnly(val); }
        
//        public boolean getDrawMinusOne() { return OvalPortrayal2D.drawMinusOne; }
//        public void setDrawMinusOne(boolean val) { OvalPortrayal2D.drawMinusOne = val; }
        
//        public boolean getRoundInts() { return OvalPortrayal2D.roundInts; }
//        public void setRoundInts(boolean val) { OvalPortrayal2D.roundInts = val; }
        
        public boolean getPreciseFills() { return CityMigrationModelWithGUI.preciseFills; }
        public void setPreciseFills(boolean val) { CityMigrationModelWithGUI.preciseFills = val; }
        
        public boolean getPreciseDraws() { return CityMigrationModelWithGUI.preciseDraws; }
        public void setPreciseDraws(boolean val) { CityMigrationModelWithGUI.preciseDraws = val; }
    }


}
