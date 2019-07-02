package sim.app.geo.refugee;

import java.awt.Color;
import ec.util.MersenneTwisterFast;
import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;

class Refugee {
	private int age;
	private int sex; // 0 male, 1 female
	private RefugeeFamily family;
	private Int2D location;
	private int healthStatus = 1; // default 1 (alive), dead 0

	public Refugee(int sex, int age, RefugeeFamily family) {
		this.sex = sex;
		this.age = age;
		this.family = family;
	}

	public int getHealthStatus() {
		return healthStatus;
	}

	public void setHealthStatus(int status) {
		this.healthStatus = status;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public RefugeeFamily getFamily() {
		return family;
	}

	public void setFamily(RefugeeFamily family) {
		this.family = family;
	}

}
