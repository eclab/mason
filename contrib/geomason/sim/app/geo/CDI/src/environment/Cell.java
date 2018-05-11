package CDI.src.environment;




import CDI.src.movement.data.BarChartFactor.Factor;
import sim.field.grid.Grid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.IntBag;

public class Cell implements Comparable<Object>{
	

	final public int x, y, nation, province;
	public int numHouseholds;
	public int empPop;
	public int cellType; // -1 for unknown, 0 for urban, 1 for rural
	public int megaCellId;
	public Factor urbanMajorFactor,ruralMajorFactor;
	final public double tempDes, portDes, elevDes, riverDes;
	public double infrastructure;
	public double initDes;
	public Bag nearestCities;
	public double bearingCapacity;
	//government:
	public double taxRevenue=0;
	public double netAssets=0;
	//community cohesion
	public double cohesion;
	public double infrastructureTax=0;
	
	
	public Cell(int x, int y, int nation, int province, double tempDes, double portDes, double elevDes, double riverDes, double initDes, int empPop){
		
        this.x=x;
        this.y=y;
        this.nation = nation;
        this.province = province;
        this.tempDes =tempDes;
        this.portDes = portDes;
        this.elevDes = elevDes;
        this.riverDes = riverDes;
        this.initDes = initDes;  // Desirability used for population initialization
        this.empPop = empPop; // this is from the numHouseholds grid
        numHouseholds = 0;
        infrastructure = 0;
        nearestCities= new Bag();
        urbanMajorFactor = Factor.UNKNOWN;
        ruralMajorFactor = Factor.UNKNOWN;
	}
	
	public void findNearestCities(SparseGrid2D cities) {
		cities.getRadialNeighbors(x, y, 50, Grid2D.BOUNDED, false, nearestCities, new IntBag(), new IntBag());
	}
	
	public void addHouseholds(int number) {
		numHouseholds+=number;
	}
	
	
	
	public void addHousehold () {
		numHouseholds++;
	}
	
	public void removeHousehold () {
		numHouseholds--;
	}
	
	public void setHouseholds(int number)
	{
		numHouseholds = number;
	}
    
	@Override
	public int compareTo(Object o) {
		Cell c = (Cell)o;
		if(initDes<c.initDes)
			return -1;
		else if(initDes>c.initDes)
			return 1;
		else
		return 0;
	}  
	
	
	// to allow Cell as a key in hashmap
	@Override
	public int hashCode(){    
        return this.x * 100 + this.y;
	}
	
	@Override
	public boolean equals(Object o){    
		Cell rhs = (Cell)o;
		if((this.x==rhs.x)&&(this.y==rhs.y))
			return true;
		return false;
	}   
	
	@Override
	public String toString() {
		return "x = "+this.x+", y = "+this.y;
	}
}
