package sim.util.opt;

import ec.*;
import ec.eval.*;

public class Worker
    {
    public static void main(String[] args)
        {
        try
        	{
			if (args.length == 0)
				{
				System.err.println("Connecting to localhost at port 15000");
				ec.eval.Slave.main(new String[] { "-from", "mason.worker.params", "-at", "sim.util.opt.Worker" });
				}
			else if (args.length == 2 && args[0].equals("-host"))
				{
				System.err.println("Connecting to IP address " + args[1] + " at port 15000");
				ec.eval.Slave.main(new String[] { "-from", "mason.worker.params", "-at", "sim.util.opt.Worker", "-p", "eval.master.host=" + args[1]});
				}
			else
				{
				System.err.println("Using a custom parameter file and parameters");        	
				ec.eval.Slave.main(args);
				}
			System.err.println("Worker quitting");
			}
		catch (Exception ex)
			{
			System.err.println("Worker dying because of exception:");
			ex.printStackTrace();
 			}
        }
    }
