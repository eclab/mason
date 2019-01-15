package sim.app.geo.conflictdiamonds;

import java.util.ArrayList;
import java.util.TreeMap;
import org.jfree.data.category.DefaultCategoryDataset;
import sim.engine.*;
import sim.field.geo.GeomVectorField;
import sim.field.grid.*;

/**
 * Conflict Diamonds
 * 
 * Basic class used to run model
 * 
 * 
 * @author bpint
 */
public class ConflictDiamonds extends SimState {
	
    public ObjectGrid2D allLand;
    public SparseGrid2D allPopulation;
    public GeomVectorField allBoundaries;
    public GeomVectorField allDiamonds;

    //store objects related to the modeling world
    ArrayList <Parcel> allParcels = new ArrayList <Parcel>();
    TreeMap <Integer, Region> allRegions = new TreeMap <Integer, Region>();
    ArrayList <Resident> allResidents = new ArrayList <Resident>();	
    ArrayList <Rebel> allRebels = new ArrayList <Rebel>();
    
    //store agents based on their employment
    OtherEmployers otherEmployer = new OtherEmployers(this);
    DiamondMiner diamondMinerEmployer = new DiamondMiner(this);	

    //PECS parameters
    public enum Motive { Basic_Needs, Maintain_Current_Lifestyle, Better_Life };
    public enum Goal { Stay_Home, Find_Employment_As_Miner, Remain_Employed, Rebel };
    public enum Action { Move_Closer_to_Diamond_Mines_Mine, Move_Close_to_Diamond_Mines_Rebel, Do_Nothing };
	
    public final Parameters params;

    public boolean opportunityNoise = false; //remove
    public double noiseLevel = .0; //remove

    private static final long serialVersionUID = -5966446373681187141L;
    
    /** This is used to create text files with model run results */
    public ConflictDiamondsObserver cObserver;
    
    private int[] totalbyRegion;
    public void setTotalbyRegion(int[] val) { totalbyRegion = val; }
    public int[] getTotalbyRegion() { return totalbyRegion; }
    
    private int[] totalAction;
    public void setTotalAction(int[] val) { totalAction = val; }
    public int[] getTotalAction() { return totalAction; }

    DefaultCategoryDataset dataset = new DefaultCategoryDataset(); 
   
    public ConflictDiamonds(long seed, String [] args) {
            super(seed);
            
            params  = new Parameters(args);
    }
	
    public void start(){
        super.start();
        
        cObserver = new ConflictDiamondsObserver(this);
        schedule.scheduleRepeating(cObserver, ConflictDiamondsObserver.ORDERING, 1.0);
    
        // create the grids
        ConflictDiamondsBuilder.create("/conflictdiamonds/conflictdiamondsData/z_landscape.txt", "/conflictdiamonds/conflictdiamondsData/z_cities.txt", "/conflictdiamonds/conflictdiamondsData/z_diamonds.txt", "/conflictdiamonds/conflictdiamondsData/z_diamondcities.txt", "/conflictdiamonds/conflictdiamondsData/z_population.txt", this);
        Steppable chartUpdater = new Steppable() {
            public void step(SimState state) {
                //update data to build charts and write to output files
                int[] sumTotalbyRegion = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                int[] sumActions = {0, 0, 0, 0};
                
                for (int i = 0; i < allResidents.size(); i++) {
                    Resident r = allResidents.get(i);
                    //sum agent attributes by region
                    if (r.getResidingParcel().getRegion().getRegionID() == 1) { sumTotalbyRegion[0] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 2) { sumTotalbyRegion[1] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 3) { sumTotalbyRegion[2] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 4) { sumTotalbyRegion[3] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 5) { sumTotalbyRegion[4] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 6) { sumTotalbyRegion[5] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 7) { sumTotalbyRegion[6] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 8) { sumTotalbyRegion[7] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 9) { sumTotalbyRegion[8] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 10) { sumTotalbyRegion[9] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 11) { sumTotalbyRegion[10] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 12) { sumTotalbyRegion[11] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 13) { sumTotalbyRegion[12] += 1; }
                    if (r.getResidingParcel().getRegion().getRegionID() == 14) { sumTotalbyRegion[13] += 1; }
                    
                    if (r.getCurrentGoal() == Goal.Stay_Home) { sumActions[0] += 1; }
                }
                
                sumActions[1] = sumActions[1] + otherEmployer.getEmployees().size();
                sumActions[2] = sumActions[2] + diamondMinerEmployer.getEmployees().size();
                sumActions[3] = sumActions[3] + allRebels.size();

                setTotalbyRegion(sumTotalbyRegion);
                setTotalAction(sumActions);
                
                String actTitle = "Activity"; // row key - activity
                String [] activities = new String[]{"Home", "Work", "Mining", "Rebel"}; 

                int sum = sumActions.length;

                // percentage - agent activity by type
                for ( int i=0; i< sumActions.length; i++){
                    dataset.setValue(sumActions[i] * 100/(allResidents.size() + allRebels.size()), actTitle, activities[i]); 
                }       	
            }
        };
        schedule.scheduleRepeating(chartUpdater);	
    }
    
    public static void main(String[] args) {    	
         doLoop(new MakesSimState()
        {
            @Override
            public SimState newInstance(long seed, String[] args)
            {

                return new ConflictDiamonds(seed, args);
            }

            @Override
            public Class simulationClass()
            {
                return ConflictDiamonds.class;
            }
        }, args);
      
        System.exit(0);
    }
    

}
