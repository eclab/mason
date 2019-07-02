package sim.engine;

import java.io.Serializable;

import sim.util.NdPoint;

//This class is not supposed to be used by the modelers
/**
 * Wrapper for transporting objects to remote processors<br>
 * Used Internally
 *
 */
class PayloadWrapper implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The is the Object to be transported<br>
	 * Required to be set by the caller
	 */
	final Serializable payload;

	/**
	 * The is the pId of the destination<br>
	 * Required to be set by the caller
	 */
	final int destination;

	/**
	 * Location of the Object in the field <br>
	 * Optional field, only needed it the payload is to be added to a field as well
	 * <br>
	 * <br>
	 * Default: null
	 */
	final NdPoint loc;

	/**
	 * Internal field, do not set it explicitly <br>
	 * It is set by the field, if a field is used to migrate an Object <br>
	 * <br>
	 * Default: -1
	 */
	final int fieldIndex;

	PayloadWrapper(final int dst, final Serializable payload, final NdPoint loc, final int fieldIndex) {
		destination = dst;
		this.payload = payload;
		this.loc = loc;
		this.fieldIndex = fieldIndex;
	}

	PayloadWrapper(final int dst, final Serializable payload) {
		destination = dst;
		this.payload = payload;
		loc = null;
		fieldIndex = -1;
	}

	public String toString() {
		return "PayloadWrapper [payload=" + payload + ", destination=" + destination + ", loc=" + loc + ", fieldIndex="
				+ fieldIndex + "]";
	}

}
