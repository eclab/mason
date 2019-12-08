package sim.field.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DEdge<K extends Serializable, T extends Serializable, O extends Serializable> implements Serializable {

	public K node1key;
	public K node2key;
	public T node1;
	public T node2;
	public ArrayList<O> values;

	public DEdge(K nodekey1, K nodekey2, T node1, T node2, O value) {
		super();
		this.node1key = nodekey1;
		this.node2key = nodekey2;
		this.node1 = node1;
		this.node2 = node2;
		this.values = new ArrayList<O>();
		if(value != null) values.add(value);
	}

	public DEdge(K nodekey1, K nodekey2, T node1, T node2) {
		super();
		this.node1key = nodekey1;
		this.node2key = nodekey2;
		this.node1 = node1;
		this.node2 = node2;
		this.values = new ArrayList<O>();
	}

	public boolean isHalo() {
		return node1 == null || node2 == null;
	}
	public void setHaloPart(T node) {
		if( node1 == null) node1 = node;
		else node2 = node;
	}
	public T getRealPart() {
		return node1 == null?node2:node1;
	}

	public T getNode1() {
		return node1;
	}

	public void setNode1(T node1) {
		this.node1 = node1;
	}

	public T getNode2() {
		return node2;
	}

	public void setNode2(T node2) {
		this.node2 = node2;
	}

	public void addValue(O value) {
		values.add(value);
	}

	public List<O> getValues(){
		return values;
	}
	public void mergeHalo(DEdge<K,T,O> e2){
		for(O value : e2.getValues()) {
			this.addValue(value);
		}
		this.setHaloPart(e2.getRealPart());
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DEdge e2 = (DEdge) obj;
		// field comparison
		return this.node1key.equals(e2.node1key) && this.node2key.equals(e2.node2key)
			||
			 this.node1key.equals(e2.node2key) && this.node1key.equals(e2.node2key);
	}	

}
