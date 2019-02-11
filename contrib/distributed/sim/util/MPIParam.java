package sim.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mpi.*;

import sim.field.storage.GridStorage;

// TODO need to use generic for other type of rectangles
public class MPIParam {
    public Datatype type;
    public int idx, size;

    // Will be used by ObjectGridStorage to collect all the objects from rects
    // this is due to the limitations of openmpi java bindings
    // coordinates of the rects stored here are local
    public List<IntHyperRect> rects;

    // TODO need to track all previously allocated datatypes and implement free() to free them all
    // TODO should store rects in local coordinates?

    public MPIParam(IntHyperRect rect, IntHyperRect bound, Datatype baseType) {
        int[] bsize = bound.getSize();

        this.idx = GridStorage.getFlatIdx(rect.ul.rshift(bound.ul.c), bsize);
        this.type = getNdArrayDatatype(rect.getSize(), baseType, bsize);
        this.size = rect.getArea();
        this.rects = new ArrayList<IntHyperRect>() {{ add(rect.rshift(bound.ul.c)); }};
    }

    public MPIParam(List<IntHyperRect> rects, IntHyperRect bound, Datatype baseType) {
        this.idx = 0;
        this.size = 0;
        this.rects = new ArrayList<IntHyperRect>();

        int count = rects.size();
        int typeSize = getTypePackSize(baseType);

        int[] bl = new int[count], displ = new int[count];
        int[] bsize = bound.getSize();

        Datatype[] types = new Datatype[count];

        // blocklength is always 1
        Arrays.fill(bl, 1);

        for (int i = 0; i < count; i++) {
            IntHyperRect rect = rects.get(i);
            displ[i] = GridStorage.getFlatIdx(rect.ul.rshift(bound.ul.c), bsize) * typeSize; // displacement from the start in bytes
            types[i] = getNdArrayDatatype(rect.getSize(), baseType, bsize);
            this.size += rect.getArea();
            this.rects.add(rect.rshift(bound.ul.c));
        }

        try {
            this.type = Datatype.createStruct(bl, displ, types);
            this.type.commit();
        } catch (MPIException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    // Create Nd subarray MPI datatype
    private Datatype getNdArrayDatatype(int[] size, Datatype base, int[] strideSize) {
        Datatype type = null;
        int typeSize = getTypePackSize(base);

        try {
            for (int i = size.length - 1; i >= 0; i--) {
                type = Datatype.createContiguous(size[i], base);
                type = Datatype.createResized(type, 0, strideSize[i] * typeSize);
                base = type;
            }
            type.commit();
        } catch (MPIException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return type;
    }

    private int getTypePackSize(Datatype type) {
        int size = 0;

        try {
            size = MPI.COMM_WORLD.packSize(1, type);
        } catch (MPIException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return size;
    }
}
