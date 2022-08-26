package sim.app.dflockers.stats;

import java.rmi.RemoteException;


//import sim.app.dflockers.display.FlockersProxyWithUI;
import sim.app.flockers.Flockers;
import sim.display.SimStateProxy;
import sim.engine.rmi.RemoteProcessorRMI;

public class FlockersStatsProxy  extends SimStateProxy{

    public FlockersStatsProxy(long seed) {
        super(seed);
        setRegistryHost("localhost");
        }
        
        
    public static void main(String[] args)
        {
        doLoop(FlockersStatsProxy.class, args);
        System.exit(0);
        }  
    
        
    public void start()
        {
        super.start();
    
        try {

            //RemoteProcessor vp = visualizationProcessor(0);
            RemoteProcessorRMI vp = this.RemoteProcessorRMI(0);
            vp.startStats(0);
            vp.startStats(1);
        
            } catch (Exception e) {
            System.out.println("birds!!!!");
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
            }
    

        }
    
    //may not trigger
    public void finish() {
 
        try {

            //VisualizationProcessor vp = visualizationProcessor(0);
            RemoteProcessorRMI vp = this.RemoteProcessorRMI(0);


            vp.stopStats(0);
            vp.stopStats(1);
            }
        catch (Exception e) {
                
            }
        super.finish();
        }
    }
