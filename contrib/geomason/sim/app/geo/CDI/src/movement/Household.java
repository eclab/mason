package CDI.src.movement;

import java.util.*;

import ec.util.MersenneTwisterFast;
import CDI.src.environment.Cell;
import CDI.src.environment.MegaCellSign;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.Orientable2D;
import sim.util.DoubleBag;
import CDI.src.movement.parameters.*;

public class Household implements Cloneable, Steppable{
    
    private class Index implements Comparable<Index>{
        int index;
        double disSquare;
        public Index(int i, double l)
        {
            this.index = i;
            this.disSquare = l;
        }
        @Override
        public int compareTo(Index o) {
            if(this.disSquare>o.disSquare)
                return 1;
            else if(this.disSquare<o.disSquare)
                return -1;
            return 0;
        }
    }
    
    

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static int householdCounter = 0;
    
    
    MersenneTwisterFast rand;
    
    protected boolean hasMovedAsUrban;
    protected boolean hasMovedAsRural;
    protected int id;
    protected boolean trapped;

	protected Cell previousCell;
	protected Cell currentCell;
	protected Parameters parameters;
	protected NorthLandsMovement model;
	protected Cell attachedCell;
	protected double satisfaction;
	public double stayLength=0;
	public double timeAwayFromAttached=0;
	public double wealthAdjustment;
	private double distance=0;
	protected Cell previousCensusCell;
	private double birthWealthFactor;
	
	public int typeFlag = -1; // type of the agent: -1 for unknown
							  //                     0 for urban
	         				  // 1 for rural
	public int previousType;
	
	public double wealth;     // Socioeconomic status
	public double SocCon;     // Social connectedness (a measure of one's support structure)

    public Household(Cell cell, Parameters parameters, int type, MersenneTwisterFast rand, double wealth)
    {
        init(cell, null, parameters, type, rand, wealth);
    }
    
    
    public Household(Cell cell, Cell attachedCell, Parameters parameters, int type, MersenneTwisterFast rand, double wealth) //attached cell can be same as cell
    {
        init(cell, attachedCell, parameters, type, rand, wealth);
    }
    

    public void init(Cell cell, Cell attachedCell, Parameters parameters, int type, MersenneTwisterFast rand, double wealth)
    {
        this.hasMovedAsUrban=false;
        this.hasMovedAsRural=false;
        this.currentCell = cell;
        this.previousCell = cell;
        this.previousCensusCell = cell;
        this.parameters = parameters;
        this.typeFlag = type;
        this.attachedCell = attachedCell;
        this.rand = rand;
        if (this.wealth<0) {
            this.wealth=0;
        }
        else {this.wealth=wealth;}    
        this.SocCon = rand.nextDouble();
        this.id = householdCounter++;
    }
    
    @Override
    public void step(SimState state) {
        
        
        if (this.wealth<0) {
            this.wealth=0;
        }
        
        this.model = (NorthLandsMovement)state;
        
        // based on the current threshold of the cell, determine the type of the agents 
        
        this.updateType();
        this.hasMovedAsUrban=false;
        this.hasMovedAsRural=false;
        previousCell = this.currentCell;
        if (model.censusHeld())
        {
            previousCensusCell=this.currentCell;
        }
        
        this.updateSatisfaction();
        if ((model.schedule.getTime()-1911) % 1 == 0) {
        this.updateWealth();
        }

        
        // then we can move
        if (!model.parameters.preventMoves) {
        if(wantToMove())
	        {
	            // move using the roulette wheel
	            Cell newCell = move();
	            impact(previousCell,newCell);
	            if (newCell!=previousCell && this.typeFlag==0 && this.distance > model.parameters.recordDistance) {
	                this.hasMovedAsUrban=true;
	                model.urbanHouseholdsMovedThisStep++;
	                
	            }
	            else if (newCell!=previousCell && this.typeFlag==1 && this.distance > model.parameters.recordDistance) {
	                this.hasMovedAsRural=true;
	                model.ruralHouseholdsMovedThisStep++;
	                
	            }
	            
	            // update the location in denseGrid
	            model.updateHouseholdLoc(this, newCell.x, newCell.y);
	            
	            
	            // track the moving direction
	            if(newCell.megaCellId!=previousCell.megaCellId) {
	                double deltaX = newCell.x - previousCell.x;
	                double deltaY = newCell.y - previousCell.y;
	                
	                MegaCellSign sign = model.map.megaCellTable.get(previousCell.megaCellId);
	                MegaCellSign newSign = model.map.megaCellTable.get(newCell.megaCellId);
	                sign.incrementMovedOutPeople();
	                sign.addDelta(deltaX, deltaY);
	                newSign.incrementMovedInPeople();
	            }
	            
	        }
    	}
        
        calculateAttachment();
        
        newBirth();
        
        
        
        if (this.wealth==Double.POSITIVE_INFINITY) {
            this.wealth=Double.MAX_VALUE; 
        }
        
        
    }
    
    private void calculateAttachment() {
        if (previousCell==currentCell) {stayLength++;}
        else {
            stayLength=0;
        }
        
        if (stayLength>model.parameters.attachmentTime) {
            attachedCell=currentCell;
        }
        
        if (currentCell==attachedCell || attachedCell==null) {
            timeAwayFromAttached=0;
        }
        else {
            timeAwayFromAttached++;
        }
        
        if (timeAwayFromAttached>model.parameters.detachmentTime) {
            attachedCell=null;
        }
        
    }
    
    // for now, the satisfaction are compute only using the factors associate with cell
    // so we just grab it back
    private void updateSatisfaction() {
        double[] desirabilities = this.typeFlag==1?model.worldAgent.ruralDesirability:model.worldAgent.urbanDesirability;
        this.satisfaction = desirabilities[model.map.indexMap.get(currentCell)];
        
    }

    private void updateType() {
        previousType = this.typeFlag;
        
        this.typeFlag = model.worldAgent.isUrban(this.currentCell)?0:1;
        
        // record data
        if(previousType==0 && this.typeFlag==0)
        {
            model.collector.incrementUrbanToUrban(); 
            
            if ((!model.parameters.censusTracking && previousCell!=currentCell && this.distance > model.parameters.recordDistance)
                    || (model.censusHeld() && previousCensusCell!=currentCell &&  this.distance > model.parameters.recordDistance)) {
                model.collector.incrementUrbanToUrbanMove();
            }
            
        }
        else if(previousType==1 && this.typeFlag==0)
        {
            model.collector.incrementRuralToUrban();
            
            if ((!model.parameters.censusTracking && previousCell!=currentCell && this.distance > model.parameters.recordDistance)
                    || (model.censusHeld() && previousCensusCell!=currentCell &&  this.distance > model.parameters.recordDistance)) {
                model.collector.incrementRuralToUrbanMove();
            }
        }
        else if(previousType==0 && this.typeFlag==1)
        {
            model.collector.incrementUrbanToRural();
            
            if ((!model.parameters.censusTracking && previousCell!=currentCell && this.distance > model.parameters.recordDistance)
                    || (model.censusHeld() && previousCensusCell!=currentCell &&  this.distance > model.parameters.recordDistance)) {
                model.collector.incrementUrbanToRuralMove();
            }
        }
        else if(previousType==1 && this.typeFlag==1)
        {
            model.collector.incrementRuralToRural();
            
            if ((!model.parameters.censusTracking && previousCell!=currentCell && this.distance > model.parameters.recordDistance)
                    || (model.censusHeld() && previousCensusCell!=currentCell &&  this.distance > model.parameters.recordDistance)) {
                model.collector.incrementRuralToRuralMove();
            }
        }
        
        if (!model.parameters.censusTracking || (model.parameters.censusTracking && model.censusHeld())) 
        {
            this.distance=0;
        }
    }
    
    public void updateWealth() {
        
        this.wealthAdjustment=this.wealth*(parameters.wealthAdjMu+parameters.wealthAdjSigma*rand.nextGaussian());
        this.wealth=this.wealth+this.wealthAdjustment;
        if (this.wealth<0) {
            this.wealth=0;
        }
        
        if (this.typeFlag==0) { model.collector.incrementUrbanWealth(this.wealth); }
        else { model.collector.incrementRuralWealth(this.wealth); }
        
    }

    
    
    /**
     * this method is responsible for this agent to move the another cell
     * @return Cell the cell this agent is going to move into
     */
    public Cell move()
    {
        Index[] list = new Index[model.selectionProb.length];
        for(int i = 0;i<list.length;++i)
        {
            int index = model.worldAgent.chooseLocation(this.typeFlag);
                
            Cell cell = model.map.canadaCells.get(index);
            double disSquare = (currentCell.x-cell.x)*(currentCell.x-cell.x);
            disSquare += (currentCell.y-cell.y)*(currentCell.y-cell.y);
            list[i] = new Index(index, disSquare);
        }
        
        if (!model.parameters.favorCloserMoves)
            Arrays.sort(list);

        Index indexOfCell = selectFromCandidates(list);
        
        double distance = Math.sqrt(indexOfCell.disSquare);
        
        
        Cell cell = model.map.canadaCells.get(indexOfCell.index);
        assert(cell != null);
        if (moveCost(distance) < this.wealth || !model.parameters.wealthLimitsMoves) {
            if (trapped) {
                trapped=false;
            }
            moveToCell(this.currentCell,cell);
            this.wealth-=moveCost(distance);
            
            // record the distance
            recordDistance(distance);
            
            // record province immigration data in a format compatible with census data
            recordImmigration(cell);
            
            return cell;
        }
        //System.out.println("Move from ("+ currentCell.x+","+currentCell.y + ") to ("+ cell.x + "," + cell.y + ")");
        else {
            if (this.typeFlag==0){    
                model.collector.incrementTrappedUrban();
            }
            if (this.typeFlag==1){
                model.collector.incrementTrappedRural();
            }
            this.trapped = true;
            return this.currentCell;
        }
    }


    
    private void recordDistance(double distance) {
        
        if (!model.parameters.censusTracking || distance > this.distance)
        {    
            this.distance=distance;
        }
        
        if(this.typeFlag==1)
            model.collector.incrementRuralDistance(distance);
        else 
            model.collector.incrementUrbanDistance(distance);        
    }

    
    private void recordImmigration(Cell cell)
    {
        
    }
    
    
    private Index selectFromCandidates(Index[] buffer) {
        double value = Math.random();
        double sum = 0;
        for(int i = 0;i<model.selectionProb.length;++i)
        {
            sum += model.selectionProb[i];
            if(value <= sum)
            {
                return buffer[i];
            }
        }
        // this should never happen
        System.err.println("This should never happen");
        return null;
    }
    
    
    
    private double moveCost(double dist) {
        
        
        double cost = parameters.moveCost*Math.log(dist+1);
        
        
        return cost;
    
    }

    /**
     * what impact will this movement cause
     */
    public void impact(Cell previousCell, Cell newCell)
    {
        int indexNew = -1, indexOld = -1;
        
        if(model.map.indexMap.containsKey(previousCell))
            indexOld = model.map.indexMap.get(previousCell);
        
        if(model.map.indexMap.containsKey(currentCell))
            indexNew = model.map.indexMap.get(newCell);
        
        
    }
    
    public Cell getCurrentCell()
    {
        return this.currentCell;
    }
    
    protected double getSatisfaction(int index)
    {
        return AgentUtil.getDesirability(model, index, this.typeFlag);
        //return this.satisfaction;
    }
    
    protected double getMovementWill()
    {
        return AgentUtil.getMovementWill(model, this.typeFlag);
    }
    protected double getGrowthRate()
    {
        return AgentUtil.getGrowthRate(model, this.typeFlag);
    }
    protected double getSocialWeight()
    {
        return AgentUtil.getSocialWeight(model, this.typeFlag);
    }
    protected double getAdjacentSocialDiscount()
    {
        return AgentUtil.getAdjacentSocialDiscount(model, this.typeFlag);
    }
    
    private boolean wantToMove()
    {
        double attachmentCoeff = 1;
        if (currentCell==attachedCell) {attachmentCoeff=1-model.parameters.attachmentStrength;}
        
        double prob = attachmentCoeff*getMovementWill();
        
        // get satisfaction
        int index = model.map.indexMap.get(currentCell);
        Double sat = new Double(this.getSatisfaction(index));
        assert(sat >= 0.0 && sat <= 1.0);
        assert(!sat.isNaN());
        assert(!sat.isInfinite(sat));
//        assert(!Float.isNaN(1.0/0.0));
//        assert(!Float.isInfinite(1.0/0.0));
//        System.out.println(sat);
        prob = prob * (1 - sat);

        boolean wantMove = model.random.nextDouble() < prob;
//        System.out.println(wantMove);
        return wantMove;
    }

    protected void moveToCell(Cell from, Cell to)
    {
        from.removeHousehold();
        to.addHousehold();
        this.currentCell=to;
    }
    
    
    
    private void newBirth()
    {
		if(model.random.nextDouble()<getGrowthRate())
		{
		    this.birthWealthFactor=this.parameters.wealthLossToBirthMu+this.rand.nextGaussian()*this.parameters.wealthLossToBirthSigma;
			this.wealth=this.wealth*this.birthWealthFactor;
			if (this.wealth<0) {
				this.wealth=0;
			}
			Household newHousehold = newHousehold();
			model.addNewHousehold(newHousehold);
			currentCell.addHousehold();

			model.schedule.scheduleRepeating(newHousehold,2,0.25);
		}
	}

    private Household newHousehold() {

		Household newHousehold = new Household(this.currentCell, parameters, this.typeFlag, this.rand, this.wealth*(this.parameters.wealthLossToBirthMu+this.rand.nextGaussian()*this.parameters.wealthLossToBirthSigma));
		
		return newHousehold;
	
    }


/* Moved this to NorthLandsMovement class.  If you're reading this, delete it.
    public boolean censusHeld() {
        
        double time = model.schedule.getTime();
        
        if (time<1951.25 && time%10==1 || time>1951.25 && time%5==1)
        {
            return true;
        }
        
        else return false;
    }
*/
    

    @Override
    public String toString() {
        return "Household-" + this.id;
    }


    public String getHouseholdType() {
        if(typeFlag==0)
            return "Urban";
        else if(typeFlag==1)
            return "Rural";
        else {
            return "Unknown";
        }
    }

    public double getWealth() {
        return wealth;
    }
    


    public boolean getTrapped() {
        return trapped;
    }

	
    public int previousCensusProvince()
    {
        return this.previousCensusCell.province;
    }
    
    public int currentCensusProvince()
    {
        return this.currentCell.province;
    }
}
