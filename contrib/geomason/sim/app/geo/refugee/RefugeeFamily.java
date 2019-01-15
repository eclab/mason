package sim.app.geo.refugee;

import java.awt.Color;
import ec.util.MersenneTwisterFast;
import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;

class RefugeeFamily implements Steppable {
	private Int2D location;
	private Bag familyMembers;
	private Route route;
	private int routePosition;
	private double finStatus;
	private City home;
	private Edge currentEdge;
	private City currentCity;
	private City goal;
	static MersenneTwisterFast random = new MersenneTwisterFast();
	private boolean isMoving;
	private HashMap<Route, Integer> cachedRoutes;
	private HashMap<City, Integer> cachedGoals;
	private boolean goalChanged;

	public RefugeeFamily(Int2D location, int size, City home, double finStatus) {
		this.location = location;
		this.home = home;
		this.goal = home;
		this.finStatus = finStatus;
		familyMembers = new Bag();
		currentCity = home;
		isMoving = true;
		// routePosition = 0;
		cachedRoutes = new HashMap<Route, Integer>();
		goalChanged = false;
	}

	@Override
	public void step(SimState state) {
		//random = new MersenneTwisterFast();
		// System.out.println("here");
		System.out.println();
		Migration migrationSim = (Migration) state;
		Bag cities = migrationSim.cities;
		City goalCity = calcGoalCity(cities);

		//if (this.goal.getName().compareTo(goalCity.getName()) != 0)
		//System.out.println("Goal Changed");
		//if (goalCity.getName().compareTo("London") != 0 || goalCity.getName().compareTo("Munich") != 0)
		//System.out.println("Different");
         
        /*for (Object c : cities) {
            City city = (City) c;
            if (this.location == city.getLocation()) { //if at a city, set current city to that city (keep until reach new city)
                currentCity = city;
                RoadInfo edgeinfo = (RoadInfo) this.currentEdge.getInfo();
                this.finStatus -= edgeinfo.getCost();//if at the end of an edge, subtract the money
                for (Object o: this.familyMembers){
                    Refugee r = (Refugee)o;
                    city.addMember(r);
                }
            }
            else{
                for (Object o: this.familyMembers){
                    Refugee r = (Refugee)o;
                    city.getRefugees().remove(r);
            }
            }
             
        }*/

		if (this.location == goalCity.location){
			goal = goalCity;
			isMoving = false;
		}
		else if (finStatus <= 0.0) {
			System.out.println("----NO MONEY LEFT-----");
			return;}
		else if (isMoving == false)
			return;
		else {
			System.out.println(finStatus);

			if (goalCity.getName().compareTo(goal.getName()) != 0){
				double r = random.nextDouble();
				if (r < Parameters.GOAL_CHANGE_PROB){
					this.goal = goalCity;
					System.out.println("-----GOAL CHANGE------");
					goalChanged = true;
				}

				if (goal == home){
					this.goal = goalCity;
					goalChanged = true;
				}
			}
			else
				goalChanged = false;



			if (this.getLocation().getX() != goal.getLocation().getX() || this.getLocation().getY() != goal.getLocation().getY()) {


				System.out.println("Home: " + this.getHome().getName() + " | Goal " + goal.getName());
				System.out.println(this + " Current: "+ currentCity.getName());
				if (currentCity.getName() == goal.getName() && this.getLocation() != goal.getLocation()){
					System.out.println("-----HERE------");
					currentCity = (City) currentEdge.to();
				}
				//setGoal(currentCity, goal);
				route = calcRoute(currentCity, goal);// Astar inside here
				//System.out.println(route);
				if (route == null){
					System.out.println("No route found:");
					return;}
				//System.out.println(route);
				int index = route.getLocIndex(this.location);
				int newIndex = 0;
				if (index != -1) {// if already on the route (in between cities)
					newIndex = index + 1;
					System.out.println("ALREADY ON: " + newIndex);
				} else {// new route
					newIndex = 1;
					System.out.println("NEW");
				}
				Edge edge = route.getEdge(newIndex);
				RoadInfo edgeinfo = (RoadInfo)edge.getInfo();
				if (this.finStatus - edgeinfo.getCost() < 0){
					isMoving = false;
				}
				else{
					Int2D nextStep = route.getLocation(newIndex);
					this.setLocation(nextStep);
					updatePositionOnMap(migrationSim);
					//System.out.println(route.getNumSteps() + ", " + route.getNumEdges());
					this.currentEdge = edge;
					determineDeath(edgeinfo, this);
					route.printRoute();
				}

				City city = (City)currentEdge.getTo();
				if (this.location.getX() == city.getLocation().getX() && this.location.getY() == city.getLocation().getY()){
					currentCity = city;
					RoadInfo einfo = (RoadInfo) this.currentEdge.getInfo();
					this.finStatus -= (einfo.getCost()*this.familyMembers.size());//if at the end of an edge, subtract the money
					//city.addMembers(this.familyMembers);
					for (Object or: this.familyMembers){
						Refugee rr = (Refugee)or;
						city.addMember(rr);
					}
				}


				else{
					for (Object c : cities){
						City cremove = (City)c;
						for (Object o: this.familyMembers){
							Refugee r = (Refugee)o;
							cremove.removeMember(r);
						}
					}
				}
			}
		}
		//  }
		System.out.println(this.location.x + ", " + this.location.y);
        /*for (Object c: cities){
            City city = (City)c;
 
            if (this.location == city.getLocation()){
                System.out.println(this.location.x + ", " + this.location.y + "|| " + city.getName());
            }
        }*/


	}

	public City calcGoalCity(Bag citylist) { // returns the best city
		City bestCity = null;
		double max = 0.0;
		for (Object city : citylist) {
			City c = (City) city;
			double cityDesirability = dangerCare() * c.getViolence()
					+ familyAbroadCare() * c.getFamilyPresence()
					+ c.getEconomy() * (Parameters.ECON_CARE + random.nextDouble()/4)
					+ c.getScaledPopulation() * (Parameters.POP_CARE + random.nextDouble()/4);
			if (c.getRefugeePopulation() + familyMembers.size() >= c.getQuota()) // if
				// reached
				// quota,
				// desirability
				// is
				// 0
				cityDesirability = 0;
			if (cityDesirability > max) {
				max = cityDesirability;
				bestCity = c;
			}

		}
		return bestCity;
	}

	private void setGoal(City from, City to) {
		this.goal = to;
		//this.route = from.getRoute(to, this);
		//this.routePosition = 0;
	}

	private Route calcRoute(City from, City to){
		Route newRoute = from.getRoute(to,  this);
		//if there's a route that contains this route
		//access it and see if decided not to use it before
		//use new route if old one changed mind, keep label as good
		//if not continue with it and keep label as good

		//TODO **** can't return back to an old route unless goal has changed
        /*for (Route r: cachedRoutes.keySet()){
            if (r.equals(newRoute)){
                System.out.println("---------FOUND SAME---------");
                if (cachedRoutes.get(r) == 1 || goalChanged)
                    return newRoute;
                else
                    return this.route;
            }
            else
                cachedRoutes.put(r, 0);
    }
        cachedRoutes.put(newRoute, 1);
        return newRoute;*/


		if (goalChanged)
			return newRoute;
		else
			return this.route;

	}

	public void updatePositionOnMap(Migration migrationSim) {
		//migrationSim.world.setObjectLocation(this.getFamily(), new Double2D(location.getX() , location.getY() ));
		for (Object o: this.getFamily()){
			Refugee r = (Refugee)o;
			double randX = 0;//migrationSim.random.nextDouble() * 0.3;
			double randY = 0;//migrationSim.random.nextDouble() * 0.3;
			//System.out.println("Location: " + location.getX() + " " + location.getY());
			migrationSim.world.setObjectLocation(r, new Double2D(location.getX() + randX/10, location.getY() + randY/10 ));
			//migrationSim.worldPopResolution.setObjectLocation(this,
			//(int)location.getX()/10, (int)location.getY()/10);
		}
	}

	public static void determineDeath(RoadInfo edge, RefugeeFamily refugee){
		double deaths = edge.getDeaths() * Parameters.ROAD_DEATH_PROB;
		double rand= random.nextDouble();
		if (rand < deaths){//first family member dies (for now)
			if (refugee.getFamily().size() != 0){
				Refugee r = (Refugee) refugee.getFamily().get(0);
				r.setHealthStatus(0);
				refugee.getFamily().remove(0);
				refugee.currentCity.getRefugees().remove(r);
			}
		}

	}

	// get and set
	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
		for (Object o: this.familyMembers){
			Refugee r = (Refugee)o;
			r.setLocation(location);
		}
	}

	public double getFinStatus() {
		return finStatus;
	}

	public void setFinStatus(int finStatus) {
		this.finStatus = finStatus;
	}

	public void setHome(City home) {
		this.home = home;
	}

	public City getGoal() {
		return goal;
	}

	public void setGoal(City goal) {
		this.goal = goal;
	}

	public City getHome() {
		return home;
	}

	public void setCurrent(City current) {
		this.currentCity = current;
	}

	public Bag getFamily() {
		return familyMembers;
	}

	public void setFamily(Bag family) {
		this.familyMembers = family;
	}

	public double dangerCare() {// 0-1, young, old, or has family weighted more
		double dangerCare = 0.5;
		for (Object o: this.familyMembers){
			Refugee r = (Refugee)o;
			if (r.getAge() < 12 || r.getAge() > 60) {
				dangerCare += Parameters.DANGER_CARE_WEIGHT * random.nextDouble();
			}
		}
		return dangerCare;
	}

	public double familyAbroadCare() { // 0-1, if traveling without family,
		// cares more
		double familyCare = 1.0;
		if (this.familyMembers.size() == 1)
			familyCare += Parameters.FAMILY_ABROAD_CARE_WEIGHT * random.nextDouble();
		return familyCare;
	}

}
