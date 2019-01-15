/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.geo.cityMigration;

import java.util.*;
import sim.field.network.*;
/**
 *
 * @author Tim
 */

public class EdgeComparator implements Comparator{

        public int compare (Object edge1, Object edge2){
            Edge e1 = (Edge) edge1;
            Edge e2 = (Edge) edge2;

            if (e1.getWeight() > e2.getWeight())
                return -1;
            else if (e1.getWeight() < e2.getWeight())
                return 1;
            else
                return 0;
        }


}
