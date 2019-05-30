package sim.app.geo.masoncsc.submodel.flockersAndHeatBugs;

import sim.app.geo.masoncsc.submodel.MetaSimState;
import sim.app.geo.masoncsc.submodel.flockersAndHeatBugs.flockers.Flocker;
import sim.app.geo.masoncsc.submodel.flockersAndHeatBugs.flockers.Flockers;
import sim.app.geo.masoncsc.submodel.flockersAndHeatBugs.heatBugs.HeatBugs;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;

public class HotFlockersAndHeatBugs extends MetaSimState
{
	private static final long serialVersionUID = 1L;
	
	public Flockers flockers;
	public HeatBugs heatBugs;
	
	public boolean sharedSchedule = false;	// this allows for a comparison between the two scheduling approaches

	double heatAvoidance = 0.1;
	public double getHeatAvoidance() { return heatAvoidance; }
	public void setHeatAvoidance(double val) { heatAvoidance = val; }
	
	public boolean flockersGenerateHeat = false;
	public boolean getFlockersGenerateHeat() { return flockersGenerateHeat; }
	public void setFlockersGenerateHeat(boolean val) { flockersGenerateHeat = val; }
	
	public boolean flockersAvoidHeat = false;
	public boolean getFlockersAvoidHeat() { return flockersAvoidHeat; }
	public void setFlockersAvoidHeat(boolean val) { flockersAvoidHeat = val; }
	
	public boolean syncScheduleTimes = false;
	public boolean getSyncScheduleTimes() { return syncScheduleTimes; }
	public void setSyncScheduleTimes(boolean val) { syncScheduleTimes = val; }
	
	
	double flockerHeat = 1.0e5;
	public double getFlockerHeat() { return flockerHeat; }
	public void setFlockerHeat(double val) { flockerHeat = val; }

	@SuppressWarnings("serial")
	public HotFlockersAndHeatBugs(long seed) {
		super(seed);
		heatBugs = new HeatBugs(seed);

		flockers = new Flockers(seed) {
						
			@Override
			public void start() {
				schedule.reset();			
				// Note: I don't want to call super.start because it will initialize 
				// the flockers as regular old flockers.
						       
		        // set up the flockers field.  It looks like a discretization
		        // of about neighborhood / 1.5 is close to optimal for us.  Hmph,
		        // that's 16 hash lookups! I would have guessed that 
		        // neighborhood * 2 (which is about 4 lookups on average)
		        // would be optimal.  Go figure.
		        flockers = new Continuous2D(neighborhood/1.5,width,height);
		        
		        // make a bunch of flockers and schedule 'em.  A few will be dead
		        for(int x=0;x<numFlockers;x++) {
		            Double2D location = new Double2D(random.nextDouble()*width, random.nextDouble() * height);
		         
					Flocker flocker = new Flocker(location) {

						@Override
						public void step(SimState state) {
					        final Flockers flock = (Flockers)state;

					        loc = flock.flockers.getObjectLocation(this);

					        if (dead) return;
					        
					        Bag b = getNeighbors();
					            
					        Double2D avoid = avoidance(b,flock.flockers);
					        Double2D cohe = cohesion(b,flock.flockers);
					        Double2D rand = randomness(flock.random);
					        Double2D cons = consistency(b,flock.flockers);
					        Double2D mome = momentum();

					        double dx = flock.cohesion * cohe.x + flock.avoidance * avoid.x + flock.consistency* cons.x + flock.randomness * rand.x + flock.momentum * mome.x;
					        double dy = flock.cohesion * cohe.y + flock.avoidance * avoid.y + flock.consistency* cons.y + flock.randomness * rand.y + flock.momentum * mome.y;
					        
					        if (flockersAvoidHeat) {
						        Double2D awayFromHeat = calcVectorAwayFromHeat(loc, 10);
						        dx += heatAvoidance * awayFromHeat.x;
						        dy += heatAvoidance * awayFromHeat.y;
					        }
					                
					        // renormalize to the given step size
					        double dis = Math.sqrt(dx*dx+dy*dy);
					        if (dis>0)
					            {
					            dx = dx / dis * flock.jump;
					            dy = dy / dis * flock.jump;
					            }
					        
					        lastd = new Double2D(dx,dy);
					        loc = new Double2D(flock.flockers.stx(loc.x + dx), flock.flockers.sty(loc.y + dy));
					        flock.flockers.setObjectLocation(this, loc);
					        
					        if (flockersGenerateHeat)
					        	generateFlockerHeat(loc);
						}	// end step
						};	// end new Flocker ...
		            
		            if (random.nextBoolean(deadFlockerProbability)) flocker.dead = true;
		            flockers.setObjectLocation(flocker, location);
		            flocker.flockers = flockers;
		            flocker.theFlock = this;
		            schedule.scheduleRepeating(flocker);	// this was the original
//		            schedule.scheduleRepeating(0, flocker, 0.1);
	            } // end for
			} // end start()
			
		}; // end new Flockers ... 

		setSimStates(new SimState[] { flockers, heatBugs });
	}
	
	private void generateFlockerHeat(Double2D loc) {
		Int2D gridLoc = continuousToGrid(loc);
		heatBugs.valgrid.field[gridLoc.x][gridLoc.y] += flockerHeat;		
	}
	
	Double2D calcVectorAwayFromHeat(Double2D loc, double radius) {
		Int2D gridLoc = continuousToGrid(loc);
		
        HeatBugs hb = heatBugs;
        
        // This is lifted straight from HeatBug.java
        int myx = gridLoc.x;
        int myy = gridLoc.y;
        
        final int START=-1;
        int bestx = START;
        int besty = 0;
		
		for(int x=-1;x<2;x++)
            for (int y=-1;y<2;y++)
                if (!(x==0 && y==0))
                    {
                    int xx = hb.buggrid.stx(x + myx);    // toroidal
                    int yy = hb.buggrid.sty(y + myy);       // toroidal
                    if (bestx==START ||
                        (hb.valgrid.field[xx][yy] < hb.valgrid.field[bestx][besty]) ||
                        (hb.valgrid.field[xx][yy] == hb.valgrid.field[bestx][besty] && random.nextBoolean()))  // not uniform, but enough to break up the go-up-and-to-the-left syndrome
                        { bestx = xx; besty = yy; }
                    }
		
		// end HeatBug code

		Double2D best = gridToContinuous(bestx, besty);

        double dx = flockers.flockers.tdx(best.x, loc.x);
        double dy = flockers.flockers.tdy(best.y, loc.y);
		
		return new Double2D(dx, dy);
	}
	
	public Double2D gridToContinuous(int x, int y) {		
		double cellWidth = flockers.width / heatBugs.gridWidth;
		double cellHeight = flockers.height / heatBugs.gridHeight;

		double cx = x*cellWidth + cellWidth*0.5;		// center of the cell
		double cy = y*cellHeight + cellHeight*0.5;		// center of in the cell
		
		return new Double2D(cx,cy);
	}	
	
	public Double2D gridToContinuous(Int2D gridLoc) {		
		return gridToContinuous(gridLoc.x, gridLoc.y);
	}
	
	public Int2D continuousToGrid(Double2D point) {
		double cellWidth = flockers.width / heatBugs.gridWidth;
		double cellHeight = flockers.height / heatBugs.gridHeight;

		int x = (int)Math.round((point.x - cellWidth*0.5) / cellWidth);
		int y = (int)Math.round((point.y - cellHeight*0.5) / cellHeight);
		
		if ((x < 0) || (y < 0))
			System.out.format("continuousToGrid out of bounds: %f, %f\n", point.x, point.y);
		
		return new Int2D(x,y);
	}

	@Override
	public void start() {
		super.start();
	}

	public static void main(String[] args) {
		doLoop(HotFlockersAndHeatBugs.class, args);
		System.exit(0);
	}
}
