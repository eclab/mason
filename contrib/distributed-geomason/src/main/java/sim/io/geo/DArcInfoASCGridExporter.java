package sim.io.geo;

import java.io.IOException;
import java.io.Writer;

import sim.field.geo.DGeomGridField;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DIntGrid2D;
import sim.util.Int2D;



public class DArcInfoASCGridExporter {
	
	  private static final int DEFAULT_NODATA_VALUE = -9999;
	    
	    /**
	     * Not intended to be instantiated.
	     */
	    private DArcInfoASCGridExporter() {}


	    /** Write out the given grid field to output stream
	     *
	     * @param gridField containing the data to be written
	     * @param nodata is the integer value indicating that given cell is null
	     * @param outputStream is an open, valid output stream
	     * @throws IOException
	     */
	    public static void write(final DGeomGridField gridField, int nodata, Writer outputStream) throws IOException
	    {
	        outputStream.write( "ncols         " );
	        outputStream.write( Integer.toString(gridField.getGridWidth()) );
	        outputStream.write( "\n" );

	        outputStream.write( "nrows         " );
	        outputStream.write( Integer.toString(gridField.getGridHeight()) );
	        outputStream.write( "\n" );

	        outputStream.write( "xllcorner     " );
	        outputStream.write( Double.toString(gridField.getMBR().getMinX()) );
	        outputStream.write( "\n" );

	        outputStream.write( "yllcorner     " );
	        outputStream.write( Double.toString(gridField.getMBR().getMinY()) );
	        outputStream.write( "\n" );

	        outputStream.write( "cellsize      " );
	        // Yes, I'm presuming that the pixels are exactly square; so I'm 
	        // arbitrarily picking width.
	        outputStream.write( Double.toString(gridField.getPixelWidth()) );
	        outputStream.write( "\n" );

	        outputStream.write( "NODATA_value  " );

	        outputStream.write( Integer.toString(nodata) );
	        outputStream.write( "\n" );



	                DDoubleGrid2D doubleGrid = (DDoubleGrid2D) gridField.getGrid();

	                for (int y = 0; y < doubleGrid.getHeight(); y++)
	                {
	                    for (int x = 0; x < doubleGrid.getWidth(); x++)
	                    {
	                        outputStream.write( Double.toString(doubleGrid.getLocal(new Int2D(x, y))) );
	                        outputStream.write( " " );
	                    }
	                    outputStream.write( "\n" );
	                }
	        
	    }


	    /** Like write() with default NODATA value of -9999.
	     *
	     * @param gridField
	     * @param outputStream
	     * @throws IOException
	     */
	    public static void write( final DGeomGridField gridField, Writer outputStream ) throws IOException
	    {
	        write(gridField, DEFAULT_NODATA_VALUE, outputStream );
	    }

}
