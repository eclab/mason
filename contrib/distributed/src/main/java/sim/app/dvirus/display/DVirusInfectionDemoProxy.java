package sim.app.dvirus.display;

import sim.display.Continuous2DProxy;

import sim.display.SimStateProxy;
import sim.field.continuous.DContinuous2D;

public class DVirusInfectionDemoProxy extends SimStateProxy{
	
    private static final long serialVersionUID = 1;

    public DVirusInfectionDemoProxy(long seed)
        {
        super(seed);
		setRegistryHost("localhost");
		//setRegistryPort(5000);
        }
        

    public Continuous2DProxy envgrid = new Continuous2DProxy(1, 1, 1);

    public void start()
        {
        super.start();
		registerFieldProxy(envgrid, 0);
        }

}
