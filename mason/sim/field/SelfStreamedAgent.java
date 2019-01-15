package sim.field;

import java.io.ObjectInputStream;

import sim.field.DObjectMigrator.AgentOutputStream;

public interface SelfStreamedAgent extends Cloneable
{	
	public void writeStream(AgentOutputStream out);
	public void readStream(ObjectInputStream in);
}
