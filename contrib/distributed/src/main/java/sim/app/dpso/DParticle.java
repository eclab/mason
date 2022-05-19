package sim.app.dpso;



import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.IntRect2D;
import sim.util.MutableDouble2D;

public class DParticle extends DSteppable{
	
    private static final long serialVersionUID = 1;

    double bestVal = 0;     
    MutableDouble2D bestPosition = new MutableDouble2D();

    public MutableDouble2D position = new MutableDouble2D();
    MutableDouble2D velocity = new MutableDouble2D();       
                
    //DPSO dpso;
    Evaluatable fitnessFunction;
    int index;  // this kludge is necessary because the particles are individually scheduled

    public DParticle() 
    {
    super();
    }
    
    public DParticle(double x, double y, double vx, double vy, Evaluatable f, int index)
    {
    super();

    this.position.setTo(x, y);
    this.velocity.setTo(vx, vy);
            
    //this.dpso = dpso;
    this.fitnessFunction = f;
    
    //dpso.space.moveAgent(new Double2D(position), this); //Do this in DPSO
    
    this.index = index;
    }   
    
    public void updateBest(double currVal, double currX, double currY)
    {
    if (currVal > bestVal)
        {
        bestVal = currVal;
        bestPosition.setTo(currX, currY);
                    
        }

    }
    
    public double getFitness()
    {
    return fitnessFunction.calcFitness(position.x,position.y);
    }
    
    public void stepUpdateFitness()
    {
    updateBest(getFitness(), position.x, position.y);
    //dpso.updateBest(getFitness(), position.x, position.y);
    }

    public void stepUpdateVelocity(MutableDouble2D dpso_nBestPos, double dpso_best_x, double dpso_best_y, double dpso_velocityScalar)
    {
    double x = position.x;
    double y = position.y;
          
 
    // calc new velocity
    // calc x component
    double inertia = velocity.x;
    double pDelta = bestPosition.x - x;   
    double nDelta = dpso_nBestPos.x - x;

    

    double gDelta = dpso_best_x - x;
    
    double pWeight = Math.random() + 0.4;
    double nWeight = Math.random() + 0.4;
    double gWeight = Math.random() + 0.4;
    double vx = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);
             
    // calc y component
    inertia = velocity.y;
    pDelta = bestPosition.y - y;   
    //nDelta = nBestPos.y - y;
    nDelta = dpso_nBestPos.y - y;

    

    gDelta = dpso_best_y - y;
    
    pWeight = Math.random() + 0.4;
    nWeight = Math.random() + 0.4;
    gWeight = Math.random() + 0.4;
    double vy = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);


    vx *= dpso_velocityScalar;
    vy *= dpso_velocityScalar;
    
    // update velocity
    velocity.setTo(vx, vy);         
    }
    
public void stepUpdatePosition(IntRect2D dpso_bounds, DPSO dpso)
    {
    //System.out.println(
    //              "Best: " + n.bestVal + " (" + n.bestPosition.x + ", " + n.bestPosition.y + ")");
	double old_pos_x = position.x;
	double old_pos_y = position.y;

	
    this.position.addIn(velocity);
    
    
    Double2D storagePos = dpso.problemSpaceToMasonStorageBounds(this.position);
    
    if (!dpso_bounds.contains(storagePos)){
    	
    	this.position = new MutableDouble2D(old_pos_x, old_pos_y); 
  
    	
    }
    
    
  
    	

    
    
    }

public void step(final SimState state) {
	
	final DPSO dpso = (DPSO) state; //do I need this?
	
	//1
	this.stepUpdateFitness();
    dpso.updateBest(this.getFitness(), this.position.x, this.position.y);

    //2
    MutableDouble2D nBestPos = new MutableDouble2D(); 
    dpso.getNeighborhoodBest(this.index, nBestPos); 
    //System.out.println(nBestPos);
	this.stepUpdateVelocity(nBestPos, dpso.best_x, dpso.best_y, dpso.velocityScalar);
	
	//3
	//System.out.println(this.position+" vel: "+this.velocity);
	IntRect2D dpso_bounds = dpso.getPartition().getWorldBounds(); //bounds in mason world
	//System.out.println(dpso_bounds);
	//System.exit(-1);
	this.stepUpdatePosition(dpso_bounds, dpso);
	
    Double2D storagePos = dpso.problemSpaceToMasonStorageBounds(this.position);

	//if (dpso_bounds.contains(storagePos))
    {
		
	    dpso.space.moveAgent(storagePos, this);
	}
	
	
}




    
}