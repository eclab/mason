package sim.app.geo.refugee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import sim.util.Bag;
import sim.util.Int2D;

class City {
	Int2D location;
	String name;
	private int quota; // 1
	private int ID;
	private int origin;
	private double scaledPop;
	private int pop;
	private double violence; // 2
	private double economy; // 3
	private double familyPresence; // 2
	private HashSet<Refugee> refugees;
	private int departures;
	private int arrivals;

	// need name, get name, set name
	//private MigrationBuilder.Node nearestNode;
	protected HashMap<City, Route> cachedPaths;

	public City(Int2D location, int ID, String name, int origin, double scaledPop, int pop, int quota, double violence,
			double economy, double familyPresence) {
		this.name = name;
		this.location = location;
		this.ID = ID;
		this.scaledPop = scaledPop;
		this.pop = pop;
		this.quota = quota;
		this.violence = violence;
		this.economy = economy;
		this.familyPresence = familyPresence;
		this.origin = origin;
		this.refugees = new HashSet<Refugee>();
		this.departures = 0;
	}

	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getScaledPopulation() {
		return scaledPop;
	}

	public double getPopulation() {
		return pop;
	}

	public int getRefugeePopulation() {
		return refugees.size();
	}

	public HashSet<Refugee> getRefugees() {
		return refugees;
	}

	public int getQuota() {
		return quota;
	}

	public void setQuota(int quota) {
		this.quota = quota;
	}

	public int getID() {
		return ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public double getViolence() {
		return violence;
	}

	public void setViolence(double violence) {
		this.violence = violence;
	}

	public double getEconomy() {
		return economy;
	}

	public void setEconomy(double economy) {
		this.economy = economy;
	}
	
	public int getDepartures(){
		return departures;
	}
	
	public int getArrivals(){
		return arrivals;
	}
	
	public double getFamilyPresence() {
		return familyPresence;
	}

	public void setFamilyPresence(double familyPresence) {
		this.familyPresence = familyPresence;
	}

	/*public void addMembers(Bag people) {
		refugees.addAll(people);
	}*/

	public void addMember(Refugee r) {
		refugees.add(r);
		arrivals++;
	}
	
	/*public void removeMembers(Bag people){
		refugees.remove(people);
		passerbyCount += people.size();
	}*/
	
	public void removeMember(Refugee r){
		if (refugees.remove(r))				
			departures ++;
	}

	/*public void setNearestNode(MigrationBuilder.Node node) {
		nearestNode = node;
	}

	public MigrationBuilder.Node getNearestNode() {
		return nearestNode;
	}

	public void cacheRoute(Route route, City destination) {
		cachedPaths.put(destination, route);
	}*/

	public Map<City, Route> getCachedRoutes() {
		return cachedPaths;
	}

	public Route getRoute(City destination, RefugeeFamily refugeeFamily) {
		Route route;

		route = AStar.astarPath(this, destination, refugeeFamily);
		//System.out.println(route.getNumSteps());

		return route;
	}
	
	public double getScale(){
		return refugees.size() * 1.0 / (Parameters.TOTAL_POP);
	}
	

}
