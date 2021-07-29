package sim.engine.transport;

import java.io.Serializable;

/**
 * An object that can be sent on the transport layer.
 * 
 * @author Carmine Spagnuolo
 *
 */
public interface MigratableObject extends Serializable
{
	
	public void setExportedName(String exportName);
	public String getExportedName();

}
