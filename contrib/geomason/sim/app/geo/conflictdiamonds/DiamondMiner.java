package conflictdiamonds;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The Diamond Miner extends employer
 * Used to track the residents that work in the diamond mines
 * 
 * @author bpint
 *
 */
public class DiamondMiner extends Employer {
	
    public DiamondMiner(ConflictDiamonds conflict) {
        super(conflict);   
    }
    
    //determine if resident is employed as a diamond miner
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
