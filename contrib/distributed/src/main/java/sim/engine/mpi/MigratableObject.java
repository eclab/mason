package sim.engine.mpi;

import java.io.Serializable;

/**
 * An object that can be sent on the transport layer.
 * 
 * @author Carmine Spagnuolo
 *
 */
public abstract class MigratableObject implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String export_name = null;

	public void setExportedName(String export_name)
	{
		this.export_name = export_name;

	}

	public String getExportedName()
	{
		return this.export_name;
	}

}