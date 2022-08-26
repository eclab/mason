package sim.app.dkeepaway.display;

import sim.display.Continuous2DProxy;
import sim.display.SimStateProxy;

public class DKeepawayProxy extends SimStateProxy{

    public DKeepawayProxy(long seed)
        {
        super(seed);
        setRegistryHost("localhost");
        //setRegistryPort(5000);
        }
    

    public Continuous2DProxy fieldEnvironmentGrid = new Continuous2DProxy(1, 1, 1);

    public void start()
        {
        super.start();
        registerFieldProxy(fieldEnvironmentGrid, 0);
        }
    }
