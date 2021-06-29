package sim.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mpi.*;
import sim.field.storage.GridStorage;
import sim.util.*;

// TODO need to use generic for other type of rectangles
/**
 * Contains data type (mpi.Datatype), size, index and rectangles of a partition
 *
 */
public class MPIParam
{
	private static final long serialVersionUID = 1L;

	/**
	 * Nd subarray MPI datatype
	 * 
	 */
	public Datatype type;
	public int idx, size;

	/**
	 * Will be used by ObjectGridStorage to collect all the objects from rects. This
	 * is due to the limitations of openmpi java bindings coordinates of the rects
	 * stored here are local
	 */
	public List<IntRect2D> rects;

	// TODO need to track all previously allocated datatypes and implement free() to
	// free them all
	// TODO should store rects in local coordinates?

	public MPIParam(IntRect2D rect, IntRect2D bound, Datatype baseType)
	{
		int width = bound.getWidth();
		int height = bound.getHeight();
		int[] bsize = new int[] { width, height };
		int[] rsize = new int[] { rect.getWidth(), rect.getHeight() };

//		this.idx = GridStorage.getFlatIndex(rect.ul().subtract(new int[]{bound.ul().x,bound.ul().y}), bsize);

		this.idx = GridStorage.getFlatIndex(rect.ul().subtract(bound.ul), height);
		this.type = getNdArrayDatatype(rsize , baseType, bsize );
		this.size = rect.getArea();
		this.rects = new ArrayList<IntRect2D>()
		{
			{
				//add(rect.rshift(new int[]{bound.ul().x,bound.ul().y}));
				add(rect.subtract(bound.ul()));
			}
		};
	}

	
	/*old version
	public MPIParam(List<IntRect2D> rects, IntRect2D bound, Datatype baseType) {
		this.idx = 0;
		this.size = 0;
		this.rects = new ArrayList<IntRect2D>();

		int count = rects.size();
		int typeSize = getTypePackSize(baseType);

		int[] bl = new int[count], displ = new int[count];
		int width = bound.getWidth();
		int height = bound.getHeight();
		int[] bsize = new int[] { width, height };
	
		Datatype[] types = new Datatype[count];

		// blocklength is always 1
		Arrays.fill(bl, 1);

		for (int i = 0; i < count; i++) {
			IntRect2D rect = rects.get(i);
//			displ[i] = GridStorage.getFlatIndex(rect.ul().subtract(new int[]{bound.ul().x,bound.ul().y}), bsize) * typeSize; // displacement from the start in bytes
			displ[i] = GridStorage.getFlatIndex(rect.ul().subtract(bound.ul()), height) * typeSize; // displacement from the start in bytes
			types[i] = getNdArrayDatatype(new int[] { rect.getWidth(), rect.getHeight() }, baseType, bsize);
			this.size += rect.getArea();
			/// this.rects.add(rect.rshift(new int[]{bound.ul().x,bound.ul().y}));
			this.rects.add(rect.subtract(bound.ul()));
		}

		try {
			this.type = Datatype.createStruct(bl, displ, types);
			this.type.commit();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	*/
	
	//fixed by Raj, this should get local point first, then get flat idx I think
	public MPIParam(List<IntRect2D> rects, IntRect2D bound, Datatype baseType)
	{
		this.idx = 0;
		this.size = 0;
		this.rects = new ArrayList<IntRect2D>();

		int count = rects.size();
		int typeSize = getTypePackSize(baseType);

		int[] bl = new int[count], displ = new int[count];
		int width = bound.getWidth();
		int height = bound.getHeight();
		int[] bsize = new int[] { width, height };
	
		Datatype[] types = new Datatype[count];

		// blocklength is always 1
		Arrays.fill(bl, 1);

		for (int i = 0; i < count; i++)
		{
			IntRect2D rect = rects.get(i);
//			displ[i] = GridStorage.getFlatIndex(rect.ul().subtract(new int[]{bound.ul().x,bound.ul().y}), bsize) * typeSize; // displacement from the start in bytes
			displ[i] = GridStorage.getFlatIndex(rect.ul().subtract(bound.ul()), height) * typeSize; // displacement from the start in bytes
			types[i] = getNdArrayDatatype(new int[] { rect.getWidth(), rect.getHeight() }, baseType, bsize);
			this.size += rect.getArea();
			/// this.rects.add(rect.rshift(new int[]{bound.ul().x,bound.ul().y}));
			this.rects.add(rect.subtract(bound.ul()));
			//this.rects.add(rect);
		}

		try
		{
			this.type = Datatype.createStruct(bl, displ, types);
			this.type.commit();
		}
		catch (MPIException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Create Nd subarray MPI datatype
	 * 
	 * @param size
	 * @param base
	 * @param strideSize
	 * 
	 * @return MPI Datatype
	 */
	Datatype getNdArrayDatatype(int[] size, Datatype base, int[] strideSize)
	{
		Datatype type = null;
		int typeSize = getTypePackSize(base);

		try
		{
			for (int i = size.length - 1; i >= 0; i--)
			{
				type = Datatype.createContiguous(size[i], base);
				type = Datatype.createResized(type, 0, strideSize[i] * typeSize);
				base = type;
			}
			type.commit();
		}
		catch (MPIException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		return type;
	}

	int getTypePackSize(Datatype type)
	{
		int size = 0;

		try
		{
			size = MPI.COMM_WORLD.packSize(1, type);
		}
		catch (MPIException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		return size;
	}
}
