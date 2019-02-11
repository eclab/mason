package sim.field.storage;

import java.io.Serializable;
import java.util.Arrays;

import mpi.*;
import static mpi.MPI.slice;

import sim.util.IntHyperRect;
import sim.util.MPIParam;

public class IntGridStorage extends GridStorage {

    public IntGridStorage(IntHyperRect shape, int initVal) {
        super(shape);
        baseType = MPI.INT;
        storage = allocate(shape.getArea());
        Arrays.fill((int[])storage, initVal);
    }
        
    public GridStorage getNewStorage(IntHyperRect shape) {
        return new IntGridStorage(shape, 0);
    }

    public byte[] pack(MPIParam mp) throws MPIException {
        byte[] buf = new byte[MPI.COMM_WORLD.packSize(mp.size, baseType)];
        MPI.COMM_WORLD.pack(slice((int[])storage, mp.idx), 1, mp.type, buf, 0);
        return buf;
    }

    public int unpack(MPIParam mp, Serializable buf) throws MPIException {
        return MPI.COMM_WORLD.unpack((byte[])buf, 0, slice((int[])storage, mp.idx), 1, mp.type);
    }

    public String toString() {
        int[] size = shape.getSize();
        int[] array = (int[])storage;
        StringBuffer buf = new StringBuffer(String.format("IntGridStorage-%s\n", shape));

        if (shape.getNd() == 2)
            for (int i = 0; i < size[0]; i++) {
                for (int j = 0; j < size[1]; j++)
                    buf.append(String.format(" %4d ", array[i * size[1] + j]));
                buf.append("\n");
            }

        return buf.toString();
    }

    protected Object allocate(int size) {
        return new int[size];
    }
}
