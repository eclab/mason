package sim.app.geo.ddadaab;

import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntBag;

public class DActivity {
    /*
     * activity of each agent
     * including staying at home there are 10 types of activities
     * each activity is related with location. agent go to each activity 
     */
    final public static int STAY_HOME = 0;
    final public static int SCHOOL = 1; 
    final public static int BOREHOLE_OR_RIVER = 2; // 
    final public static int MOSQUE = 3;
    final public static int MARKET = 4;
    final public static int FOOD_CENTER = 5;
    final public static int HEALTH_CENTER = 6;
    final public static int SOCIAL_RELATIVES = 7;
    final public static int VISIT_SOCIAL = 8;
    final public static int VISIT_LATRINE = 9;
    
    // best location is mainly determine by distance
    // near is best
    public DFieldUnit bestActivityLocation(DRefugee ref, DFieldUnit position, int id, DDadaab d) {
       
        
       // FieldUnit potLoc = null;
        
     
        if (id == STAY_HOME) {

            return ref.getHome();
            //System.out.println("home" + newL.getCampID());

        } 
         else if (id == SCHOOL){
            return betstLoc (ref.getHome(), d.schooles,d);
        }
         
         else if (id == BOREHOLE_OR_RIVER) {
           // System.out.println("borehole" + nearestWaterSource(ref, position, d));
            return nearestWaterSource(ref.getHome(), d);

        } 
         else if(id == MOSQUE){
            return betstLoc (ref.getHome(), d.mosques,d);
        }
         
         else if (id == MARKET){
            return betstLoc (ref.getHome(), d.market,d);
        }
          
        else if(id == FOOD_CENTER){
            return betstLoc (ref.getHome(), d.foodCenter,d);
        }
        
         
        else if (id == HEALTH_CENTER){
            return betstLoc (ref.getHome(), d.healthCenters,d);
        }
        
        else if (id == SOCIAL_RELATIVES) {
           // System.out.println("camp");
            
            int l=ref.getFamily().getRelativesLocation().numObjs;
            
            if(l == 0){
                return ref.getHome();
            }
            else return ((DFieldUnit)(ref.getFamily().getRelativesLocation().objs[d.random.nextInt(l)]));

        } 
        
        else if (id == VISIT_SOCIAL) {
          //  System.out.println("camp");
            return socialize(ref, d);

        } 
        else if (id == VISIT_LATRINE) {

//            ref.setLatrineField(this.openLatrine(ref, d));

            return this.openLatrine(ref, d);

        } 
        
        else {
            return ref.getHome();
        }
      // return newL;

    }
     
    
    private DFieldUnit betstLoc (DFieldUnit fLoc, Bag fieldBag, DDadaab d){
            Bag newLoc = new Bag();
        
            double bestScoreSoFar = Double.POSITIVE_INFINITY;
            for (int i = 0; i < fieldBag.numObjs; i++) {
                DFieldUnit potLoc = ((DFieldUnit) fieldBag.objs[i]);

                double fScore = fLoc.distanceTo(potLoc);
                if (fScore > bestScoreSoFar) {
                    continue;
                }

                if (fScore <= bestScoreSoFar) {
                    bestScoreSoFar = fScore;
                    newLoc.clear();
                }
                newLoc.add(potLoc);


            }
            DFieldUnit f = null;
            if (newLoc != null) {
                int winningIndex = 0;
                if (newLoc.numObjs >= 1) {
                    winningIndex = d.random.nextInt(newLoc.numObjs);
                }
                //System.out.println("other" + newLoc.numObjs);
              f= (DFieldUnit) newLoc.objs[winningIndex];

            }
            return f;
    }

    // Haiti project
    public DFieldUnit getNextTile(DDadaab dadaab, DFieldUnit subgoal, DFieldUnit position) {

        // move in which direction?
        int moveX = 0, moveY = 0;
        int dx = subgoal.getX() - position.getX();
        int dy = subgoal.getY() - position.getY();
        if (dx < 0) {
            moveX = -1;
        } else if (dx > 0) {
            moveX = 1;
        }
        if (dy < 0) {
            moveY = -1;
        } else if (dy > 0) {
            moveY = 1;
        }
        //((FieldUnit) o).loc

        
        System.out.println("only query if in bounds");
        System.exit(-1);
        
        Object a = dadaab.roadGrid;
        
        // can either move in Y direction or X direction: see which is better
        DFieldUnit xmove = ((DFieldUnit) dadaab.allCamps.getLocal(new Int2D(position.getX() + moveX,position.getY())));
        DFieldUnit ymove = ((DFieldUnit) dadaab.allCamps.getLocal(new Int2D(position.getX(),position.getY() + moveY)));

        boolean xmoveToRoad = ((Integer) dadaab.roadGrid.getLocal(new Int2D(xmove.getX(), xmove.getY()))) > 0;
        boolean ymoveToRoad = ((Integer) dadaab.roadGrid.getLocal(new Int2D(ymove.getX(), ymove.getX()))) > 0;

        if (moveX == 0 && moveY == 0) { // we are ON the subgoal, so don't move at all!
            // both are the same result, so just return the xmove (which is identical)
            return xmove;
        } else if (moveX == 0) // this means that moving in the x direction is not a valid move: it's +0
        {
            return ymove;
        } else if (moveY == 0) // this means that moving in the y direction is not a valid move: it's +0
        {
            return xmove;
        } else if (xmoveToRoad == ymoveToRoad) { //equally good moves: pick randomly between them
            if (dadaab.random.nextBoolean()) {
                return xmove;
            } else {
                return ymove;
            }
        } else if (xmoveToRoad && moveX != 0) // x is a road: pick it
        {
            return xmove;
        } else if (ymoveToRoad && moveY != 0)// y is a road: pick it
        {
            return ymove;
        } else if (moveX != 0) // move in the better direction
        {
            return xmove;
        } else if (moveY != 0) // yes
        {
            return ymove;
        } else {
            return ymove; // no justification
        }
    }
  
    // three camp sites in the model
    // agent select one camp which is not their camp randomly
    private DFieldUnit socialize(DRefugee ref, DDadaab d) {
      
        Bag potential = new Bag();
        DFieldUnit newLoc= null;
        potential.clear();
        
        // socialize - visit friend or any place in
        // potential = d.campSites;

        int camp = ref.getHome().getCampID(); // get camp id
        
        // select any camp site but not the camp that belong to the agent
        for (Object campsite : d.campSites) {
             DFieldUnit cmp = ((DFieldUnit) campsite);
            if (cmp.getCampID() == camp && cmp.equals(ref.getHome())!=true && cmp.getRefugeeHH().numObjs >0) {
                potential.add(cmp); // potential locations to visit
            }
        }
        
//        if(potential.isEmpty() ==true){
//            System.out.println("empty");
//        }
        if(potential.numObjs==1){
            newLoc = (DFieldUnit)potential.objs[0];
        }
        else{
            newLoc = (DFieldUnit)potential.objs[d.random.nextInt(potential.numObjs)];
        }
       
            return newLoc;
    }
    
    
    // if the camp has laterine use it otherwise
    // open latine - usually is a bit far from home - should open
    // children use around
    // age < 4 use home

    // select best location for open latrine
    private DFieldUnit openLatrine(DRefugee ref, DDadaab d) {
        Bag potential = new Bag();
        
         potential.clear();
        DFieldUnit loc = null;
       
        // if agent has laterine, agent use it// // if you are too young 
        if (ref.getFamily().getHasLaterine() == true ||ref.getAge() < 5) {
            loc = ref.getHome();
        } 
        // 
        else {
               // potential location is within the distance given in parapeter
                //Bag adjacent = d.allCamps.getNeighborsMaxDistance(ref.getHome().getX(), ref.getHome().getY(), d.params.global.getMaxDistanceLaterine(),
                 //       false, null, null, null);
                
                //what do I do here exactly?
        	    IntBag xPos = new IntBag();
        	    IntBag yPos = new IntBag();

                d.allCamps.getMooreLocations( ref.getHome().getX(), ref.getHome().getY(), d.params.global.getMaxDistanceLaterine(), 0, false, xPos, yPos);
                
                Bag adjacent = new Bag();
                for (int i=0; i<xPos.numObjs; i++) {
                	adjacent.add(d.allCamps.getLocal(new Int2D(xPos.get(i), yPos.get(i))));
                }
                
                for (Object l : adjacent) {
                    DFieldUnit lt = (DFieldUnit) l;
                    if (lt.getFieldID() == 0) {
                        potential.add(lt);
                    }


                }
                // pick one of the potential location randomly
                if (potential.isEmpty() == true){
                    //System.out.println("null");
                    loc=  null;
                }
                else loc = (DFieldUnit) potential.objs[d.random.nextInt(potential.numObjs)];
      

        }
        
        
        return loc;
    }
    
    
    // find the nearest water points
     public DFieldUnit nearestWaterPoint(DFieldUnit f, DDadaab d){
         
            return betstLoc(f,d.rainfallWater,d);
    }
    
   // search nearest borehold 
   // this is useful if one of the borehole is empty
   // agent will selct another borehole nearest from the current 
  private DFieldUnit nearestBorehole(DFieldUnit f, DDadaab d){
      
            return betstLoc (f, d.boreHoles,d);
  }
    
  //   select water source either from borehole or rainfall water points
  // agent preference weight and distance affect the choice
  
  public DFieldUnit nearestWaterSource( DFieldUnit f, DDadaab d){
      DFieldUnit fieldP =  null;
      
      double preference_river = 0.0;
      double preference_borehole = 0.0;
      
      //incase no water point field to select, preference is 0
      if(nearestWaterPoint(f,d) == null){
          preference_river = 0.0;
      }
      // preference depend on inverse distance and  preference weight
      else { 
          preference_river = (1.0 / (1.0+Math.log(1+f.distanceTo(nearestWaterPoint(f,d))))) * d.params.global.getWaterSourcePreference_River() + (0.2 *d.random.nextDouble()); 
      }
      
      if(nearestBorehole(f,d) == null){
       preference_borehole  =0.0;
      }
      
      else {
          preference_borehole = (1.0 /(1.0+Math.log(1+f.distanceTo(nearestBorehole(f,d)))))* d.params.global.getWaterSourcePreference_Borehole()+ (0.2 *d.random.nextDouble());
      }
      
      if (preference_river > preference_borehole){
         fieldP =nearestWaterPoint(f,d);
      }
      
      else {
          fieldP = nearestBorehole(f,d);
      }
      return fieldP;
      
  }
}
