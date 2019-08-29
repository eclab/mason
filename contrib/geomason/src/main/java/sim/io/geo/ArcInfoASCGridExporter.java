/*
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
 *
 */
package sim.io.geo;

import java.io.IOException;
import java.io.Writer;
import sim.field.geo.GeomGridField;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;

/** Write a GeomGridField to an Arc/Grid formatted output stream.
 *
 */
public class ArcInfoASCGridExporter
{
    private static final int DEFAULT_NODATA_VALUE = -9999;
    
    /**
     * Not intended to be instantiated.
     */
    private ArcInfoASCGridExporter() {}


    /** Write out the given grid field to output stream
     *
     * @param gridField containing the data to be written
     * @param nodata is the integer value indicating that given cell is null
     * @param outputStream is an open, valid output stream
     * @throws IOException
     */
    public static void write(final GeomGridField gridField, int nodata, Writer outputStream) throws IOException
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


        switch( gridField.getGridDataType() )
        {
            case INTEGER :
                IntGrid2D intGrid = (IntGrid2D) gridField.getGrid();

                for (int y = 0; y < intGrid.getHeight(); y++)
                {
                    for (int x = 0; x < intGrid.getWidth(); x++)
                    {
                        outputStream.write( Integer.toString(intGrid.get(x, y)) );
                        outputStream.write( " " );
                    }
                    outputStream.write( "\n" );
                }
                break;

            case DOUBLE :
                DoubleGrid2D doubleGrid = (DoubleGrid2D) gridField.getGrid();

                for (int y = 0; y < doubleGrid.getHeight(); y++)
                {
                    for (int x = 0; x < doubleGrid.getWidth(); x++)
                    {
                        outputStream.write( Double.toString(doubleGrid.get(x, y)) );
                        outputStream.write( " " );
                    }
                    outputStream.write( "\n" );
                }
                break;
        }
    }


    /** Like write() with default NODATA value of -9999.
     *
     * @param gridField
     * @param outputStream
     * @throws IOException
     */
    public static void write( final GeomGridField gridField, Writer outputStream ) throws IOException
    {
        write(gridField, DEFAULT_NODATA_VALUE, outputStream );
    }
}
