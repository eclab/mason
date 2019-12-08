package sim.field.network;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import mpi.MPI;
import mpi.MPIException;
import sim.engine.transport.PayloadWrapper;
import sim.field.Synchronizable;
import sim.util.MPIUtil;

/**
 * A distributed network structure.
 * @author Carmine Spagnuolo
 * 
 * @param <T> a generic nodes type.
 * @param <O> a generic edge value type.
 */
public class DNetwork<K extends Serializable, T extends Serializable, O extends Serializable> implements Synchronizable{
	
	public static Object DYNAMIC_NETWORK = new Object();
	
	public static enum NetworkType{
		DYNAMIC, STATIC
	}
	
	private NetworkType TYPE = NetworkType.DYNAMIC;
	
	private HashMap<K,T> nodes;
	private ArrayList<DEdge<K,T,O>> edges;
	private ArrayList<DEdge<K,T,O>> haloEdges;
	private HashMap<K,ArrayList<DEdge<K,T,O>>> haloToEdges;
	private HashMap<K,ArrayList<DEdge<K,T,O>>> adjList;
	
	public  DNetwork(NetworkType type) {
		
		this.TYPE = type;
		
		edges = new ArrayList<DEdge<K,T,O>>();
		haloEdges = new ArrayList<DEdge<K,T,O>>();
		nodes = new HashMap<K,T>();
		adjList = new HashMap<K, ArrayList<DEdge<K,T,O>>>();
		haloToEdges = new  HashMap<K,ArrayList<DEdge<K,T,O>>>();
		
	}
	
	public void addNode(T node, K id) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		
		nodes.put(id,node);
		adjList.put(id, new ArrayList<DEdge<K,T,O>>());
		
		for(DEdge<K,T,O> edge : haloToEdges.get(id)) {
			edge.setHaloPart(node);
			haloEdges.remove(edge);
		}
	}
	public boolean addEdge(K node1_id, K node2_id, O value) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		if(!nodes.containsKey(node1_id) && !nodes.containsKey(node2_id))
		{
			return false;
		}
		DEdge<K,T,O> edge = value!=null? new DEdge<K,T,O>(node1_id, node2_id, nodes.get(node1_id),nodes.get(node2_id), value):
			new DEdge<K,T,O>(node1_id, node2_id, nodes.get(node1_id), nodes.get(node2_id));
		
		if(!nodes.containsKey(node1_id)) {
			ArrayList<DEdge<K,T,O>> list = haloToEdges.get(node1_id);
			list = list==null? new ArrayList<DEdge<K,T,O>>(): list;
			list.add(edge);
			haloToEdges.put(node1_id, list);
			adjList.get(node2_id).add(edge);
			haloEdges.add(edge);
		}else if(!nodes.containsKey(node2_id)) {
			ArrayList<DEdge<K,T,O>> list = haloToEdges.get(node2_id);
			list = list==null? new ArrayList<DEdge<K,T,O>>(): list;
			list.add(edge);
			haloToEdges.put(node2_id, list);
			adjList.get(node1_id).add(edge);
			haloEdges.add(edge);
		}else {
			adjList.get(node1_id).add(edge);
			adjList.get(node2_id).add(edge);
		}
		
		edges.add(edge);
		
		return true;
	}
	
	@Override
	public void initRemote() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void syncHalo() throws MPIException {
		
		if(TYPE.equals(NetworkType.DYNAMIC)) {
			//ALL to ALL Halo Edges
			ArrayList<DEdge<K,T,O>> halolists = MPIUtil.neighborAllToAll(MPI.COMM_WORLD, 
													(DEdge<K,T,O>[]) haloEdges.toArray());
			
			for(DEdge<K,T,O> e : haloEdges) {
				for(DEdge<K,T,O> e_rcv : halolists)
				{
					if(e.equals(e_rcv)) e.mergeHalo(e_rcv);
				}
			}
		}else if(TYPE.equals(NetworkType.STATIC)) {
			
		}
		
	}

	@Override
	public void syncObject(PayloadWrapper payloadWrapper) {
		
		//addNode when agent arriv
	}

}
