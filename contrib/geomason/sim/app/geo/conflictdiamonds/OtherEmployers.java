package conflictdiamonds;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The Other Employers extends employer
 * Used to track the residents that do not work in the diamond mines
 * 
 * @author bpint
 *
 */
public class OtherEmployers extends Employer {
	
    private Collection<Person> formalEmployees; //People employed by this employer

    public OtherEmployers(ConflictDiamonds conflict) {
        super(conflict);    
    }
    
    //determine if resident is employed here (not a diamond miner)
    public boolean isEmployedHere(Person emp) {
        Collection employees = new ArrayList<Person>();
        employees = getEmployees();	
        int count = 0;

        for(Object o: employees) {
            Person p = (Person) o;
            if ( p == emp ) count++;
        }

        if ( count > 0 ) { return true; }
        else { return false; }
    }

}
