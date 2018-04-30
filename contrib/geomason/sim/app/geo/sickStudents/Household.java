/**
 ** HouseHold.java
 **
 ** Copyright 2011 by Joseph Harrison, Mark Coletti, Cristina Metgher, Andrew Crooks
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **
 **/
package sickStudents;

import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import sim.engine.SimState;
import sim.engine.Steppable;

@SuppressWarnings("serial")
public class Household implements Steppable
{
	public SickStudentsModel model;
	public ArrayList<Student> students = new ArrayList<Student>();
	public Point location;
	
	public Household(SickStudentsModel model) {
		this.model = model;
	}
	
	@Override
	public void step(SimState state) {
		for (Student s : students)
        {
            if (s.status == Status.INFECTED) {	// if any children are sick
                for (Student s2 : students)  // expose the rest
                    s2.expose();
                break;
            }
        }
	}
}
