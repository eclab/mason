/** ShapeFileInfo.java
 *
 *  $Id: ShapeFileInfo.java,v 1.1 2010-04-02 17:12:35 mcoletti Exp $
 */

package sim.io.geo; 

/** 
 A simple class used by the native MASON importer.  Popultating the <name, value> pairs 
 is handled by MasonImportor, hence, this class only instatiates the underlying TreeMap.  
 */
public class ShapeFileInfo extends GeometryInfo {
	
	/** Default constructor.  Make sure to call this!*/ 
	public ShapeFileInfo()
	{
		super(); 
	}

}