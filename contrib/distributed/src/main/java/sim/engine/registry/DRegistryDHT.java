package sim.engine.registry;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mpi.MPI;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FutureRemove;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;
import sim.util.MPIUtil;

public class DRegistryDHT {

	private static DRegistryDHT instance;
	private static Peer peer = null;
	private static PeerDHT dht_node = null;
	private static int port;
	private static final String id = UUID.randomUUID().toString();
	private static int rank;
	
	public static Logger logger;
	

	private static void initLocalLogger(final String loggerName) {
		DRegistryDHT.logger = Logger.getLogger(loggerName);
		DRegistryDHT.logger.setLevel(Level.ALL);
		DRegistryDHT.logger.setUseParentHandlers(false);

		final ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new java.util.logging.Formatter() {
			public synchronized String format(final LogRecord rec) {
				return String.format("[%s][%-7s] %s%n",
						new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
						rec.getLevel().getLocalizedName(), rec.getMessage());
			}
		});
		DRegistryDHT.logger.addHandler(handler);
	}
	

	private DRegistryDHT() throws NumberFormatException, Exception{

		if (instance != null){
			throw new RuntimeException("Use getInstance() method to get the single instance of the Distributed Registry.");
		}
		rank = MPI.COMM_WORLD.getRank();
		initLocalLogger(String.format("MPI-Job-%d", rank));
		
		port = getAvailablePort();
		String myip = InetAddress.getLocalHost().getHostAddress();

		//Find ip and port of the master node
		final String master_data[] = MPIUtil.<String>bcast(myip+":"+port, 0).split(":");

		if(rank == 0) {

			startLocalRegistry(master_data[0], Integer.parseInt(master_data[1]));
		}

		MPI.COMM_WORLD.barrier();

		if(rank != 0) {

			startLocalRegistry(master_data[0], Integer.parseInt(master_data[1]));
		}

	} 

	private void startLocalRegistry(String master_ip, int master_port) throws Exception {
		peer = new PeerBuilder(Number160.createHash(id)).
				ports(port).start();
		dht_node = new PeerBuilderDHT(peer).start();	

		FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(master_ip)).ports(master_port).start();
		fb.awaitUninterruptibly();
		if(fb.isSuccess()) {
			peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
			logger.log(Level.FINE, "Distributed Registry started for MPI node: "+rank+" with id: "+id);
		}else {
			throw new Exception("Error in peer master["+ master_ip+":"+ master_port+"] bootstraping.");
		}
	}
	
	public static DRegistryDHT getInstance(){
		try {
			return instance = instance == null? new DRegistryDHT(): instance;
		}catch(Exception e) {
			logger.log(Level.SEVERE, "Error Distributed Registry started for MPI node: "+rank+" with id: "+id);
			
			return null;
		}
	}
	private static Integer getAvailablePort() throws IOException {

		try ( ServerSocket socket = new ServerSocket(0); ) {
			return socket.getLocalPort();
		}
	}
	
	public boolean registerObject(String name, Serializable obj) {
		try {
			FutureGet futureGet = dht_node.get(Number160.createHash(name)).start();
			futureGet.awaitUninterruptibly();
			if (futureGet.isSuccess() && futureGet.isEmpty()) 
				dht_node.put(Number160.createHash(name)).data(new Data(obj)).start().awaitUninterruptibly();
			
			return futureGet.isSuccess();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public boolean unRegisterObject(String name, Serializable obj) {
		try {
			FutureRemove futureR = dht_node.remove(Number160.createHash(name)).start();
			futureR.awaitUninterruptibly();
			return futureR.isSuccess();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
