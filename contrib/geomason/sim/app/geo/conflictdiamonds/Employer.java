package conflictdiamonds;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The Employer object tracks residents that are employed
 * Used to track the residents that work in the diamond mines and outside of the diamond mines
 * 
 * @author bpint
 *
 */
public class Employer {
	
    private Collection<Person> employees; //People employed by this employer
    ConflictDiamonds conflict;

    //income distribution	
    public Employer( ConflictDiamonds c ) {
        conflict = c;		
        employees = new ArrayList<Person>();
    }

    public Employer() {
        super();
    }


    public void addEmployee(Person e) {
        employees.add(e);

    }

    public Collection getEmployees() {
        return employees;

    }

    public void removeEmployee(Person e) {
        employees.remove(e);
    }

}
