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
                    
        //dpso.updateBest(currVal, currX, currY);
        }
        //System.out.println("currVal : "+currVal);
        //System.out.println("bestVal : "+bestVal);
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
          
    //handled in DPSO, passed here as dpso_nBestPos
    //MutableDouble2D nBestPos = new MutableDouble2D(); 
    //dpso.getNeighborhoodBest(index, nBestPos);       // updates the location of nBestPos
            
    // calc new velocity
    // calc x component
    double inertia = velocity.x;
    double pDelta = bestPosition.x - x;   
    //double nDelta = nBestPos.x - x;
    double nDelta = dpso_nBestPos.x - x;

    
    //double gDelta = dpso.bestPosition.x - x;
    //double gDelta = dpso.best_x - x;
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

    
    //gDelta = dpso.bestPosition.y - y;
    //gDelta = dpso.best_y - y;
    gDelta = dpso_best_y - y;
    
    pWeight = Math.random() + 0.4;
    nWeight = Math.random() + 0.4;
    gWeight = Math.random() + 0.4;
    double vy = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);

    //vx *= dpso.velocityScalar;
    //vy *= dpso.velocityScalar;
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
    
    
    //if new location out of bounds, move as far as we can
    /*
    if (!dpso_bounds.contains(storagePos)){
    	
    	double minXBound = 0.0 - (dpso.width * 0.5);
    	double maxXBound = dpso.width - (dpso.width * 0.5);
    	double minYBound = 0.0 - (dpso.height * 0.5);
    	double maxYBound = dpso.height - (dpso.height * 0.5);
    	
    	
    	double xBound;
    	double yBound;
    	
    	double dirX = this.position.getX() - old_pos_x;
    	double dirY = this.position.getY() - old_pos_y;
    	
    	if (storagePos.getX() > dpso_bounds.br().getX())
    	{
    		xBound = maxXBound - 1; //because max values not included in boundary
    	}
    	
    	else {
    		xBound = minXBound + 1 ;
    	}
    	
    	if (storagePos.getY() > dpso_bounds.br().getY())
    	{
    		yBound = maxYBound - 1; ////because max values not included in boundary
    	}
    	
    	else {
    		yBound = minYBound + 1;
    	}
    	
    	Double2D pointCand1;
    	if (dirX != 0.0)	{	
    		double a = (xBound - old_pos_x)/dirX;
    		pointCand1 = new Double2D(dirX * a + old_pos_x, dirY * a + old_pos_y);
    		}
    	
    	else { //don't do anything with this point!
    		pointCand1 = new Double2D(this.position.getX(), this.position.getY());
    	}
    	
    	Double2D pointCand2;
    	if (dirY != 0.0) {

    		double b = (yBound - old_pos_y)/dirY;
    		pointCand2 = new Double2D(dirX * b + old_pos_x, dirY * b + old_pos_y);
    	}
    	
    	else { //don't do anything with this point!
    		pointCand2 = new Double2D(this.position.getX(), this.position.getY());
    	}
    	
    	Double2D storagePointCand1 = new Double2D(pointCand1.getX()+ (dpso.width * 0.5), pointCand1.getY()+ (dpso.height * 0.5));
    	Double2D storagePointCand2 = new Double2D(pointCand2.getX()+ (dpso.width * 0.5), pointCand2.getY()+ (dpso.height * 0.5));

    	
    	if (dpso_bounds.contains(storagePointCand1)) {
    		
    		this.position = new MutableDouble2D(pointCand1.getX(), pointCand1.getY());
    	}
    	else if (dpso_bounds.contains(storagePointCand2)){
    		this.position = new MutableDouble2D(pointCand2.getX(), pointCand2.getY());

    	}
    	
    	else {
    		System.out.println(old_pos_x+ " " + old_pos_y+" to "+this.position+"both "+storagePointCand1+ " and "+ storagePointCand2+" is still out of bounds somehow");
    		
    		System.exit(-1);
    	}

    	System.out.println(this.position);
    	//this.position = new MutableDouble2D(old_pos_x, old_pos_y); //don't move to invalid location
    	 }
    	 * 
    	 */
    	

    
    
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