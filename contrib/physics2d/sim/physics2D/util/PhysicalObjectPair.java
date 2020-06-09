
package sim.physics2D.util;

import sim.physics2D.physicalObject.PhysicalObject2D;

/** PhysicalObjectPair holds two physical objects that can
 * be put into a hashtable.
 */
public class PhysicalObjectPair 
    {
    public PhysicalObject2D c1;
    public PhysicalObject2D c2;
    public int hashcode;
        
    public PhysicalObjectPair(PhysicalObject2D c1, PhysicalObject2D c2)
        {
        this.c1 = c1;
        this.c2 = c2;
                
        hashcode = calcHashCode();
        }
        
    private int calcHashCode()
        {
        int index1 = c1.getIndex();
        int index2 = c2.getIndex();
                
        if (index2 > index1)
            {
            int temp = index2;
            index2 = index1;
            index1 = temp;
            }
                
        index1 += ~(index1 << 9);
        index1 ^=  (index1 >>> 14);
        index1 +=  (index1 << 4);
        index1 ^=  (index1 >>> 10);
        return index2 ^ index1;
                
        //return index1 + index2;
        }
        
    // Overload hashCode and equals so we can put PhysicalObjectPairs 
    // hashtables
    public int hashCode()
        {       
        return hashcode;
        }
        
    public boolean equals(Object obj)
        {
        PhysicalObjectPair ap = (PhysicalObjectPair)obj;
        if ((ap.c1.getIndex() == this.c1.getIndex() && ap.c2.getIndex() == this.c2.getIndex())
            || (ap.c1.getIndex() == this.c2.getIndex() && ap.c2.getIndex() == this.c1.getIndex()))
            return true;
        else
            return false;
        }
    }
