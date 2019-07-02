/**
 ** School.java
 **
 ** Copyright 2011 by Joseph Harrison, Mark Coletti, Cristina Metgher, Andrew Crooks
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.sickStudents;

import java.util.ArrayList;
import sim.engine.SimState;
import sim.engine.Steppable;

@SuppressWarnings("serial")
public class School implements Steppable
{
	public enum SchoolType {
		ElementarySchool,
		MiddleSchool,
		HighSchool,
		Other
	};
	
	public SickStudentsModel model;
	public ArrayList<Student> students = new ArrayList<Student>();
	public String name;
	public SchoolType type;
	public boolean closed = false;
	
	public int catchmentCount = 0;
	
	public School(SickStudentsModel model, String name, String schoolType) {
		this.model = model;
		this.name = name;
		
		if (schoolType.equals("ES"))
        {
            type = SchoolType.ElementarySchool;
        }
		else if (schoolType.equals("MS"))
        {
            type = SchoolType.MiddleSchool;
        }
		else if (schoolType.equals("HS"))
        {
            type = SchoolType.HighSchool;
        }
		else
        {
            type = SchoolType.Other;
        }
	}
	
	private Student getRandomStudent(Student butNotThisStudent) {
		Student s;
		do
        {
            s = students.get(model.random.nextInt(students.size()));
        } while (!(!s.homebound && (s != butNotThisStudent)));
		
		return s;
	}
	
	public double getProportionOfSickStudents() {
		if (students.isEmpty())
        {
            return 0;
        }
		
		int sick = 0;
		for (Student s : students)
        {
            if (s.status == Status.INFECTED)
            {
                sick++;
            }
        }
		
		return sick / (double)students.size();
	}
	
	public double getProportionOfHomeboundStudents() {
		if (students.isEmpty())
        {
            return 0;
        }
		
		int homebound = 0;
		for (Student s : students)
        {
            if (s.homebound)
            {
                homebound++;
            }
        }
		
		return homebound / (double)students.size();
	}
	
	@Override
	public void step(SimState state) {
		if (closed)
        {
            return;
        }
		
		int inAttendence = 0;
		for (Student s : students)
        {
            if (!s.homebound)
            {
                inAttendence++;
            }
        }
		
		if (inAttendence < 2)
        {
            return;
        }

		// TODO the number of interactions should have some justification
		for (int i = 0; i < students.size(); i++) {
			// pick two random students and have them interact
			Student s1 = getRandomStudent(null);
			Student s2 = getRandomStudent(s1);
	
			if (s1.status == Status.INFECTED)
            {
                s2.expose();
            }
	
			if (s2.status == Status.INFECTED)
            {
                s1.expose();
            }
		}
	}
}
