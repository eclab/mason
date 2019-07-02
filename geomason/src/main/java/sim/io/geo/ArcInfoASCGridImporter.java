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

import com.vividsolutions.jts.geom.Envelope;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.field.geo.GeomGridField;
import sim.field.grid.AbstractGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;



/** Importer for ESRI Arc/Info ASCII GRID formatted files
 *
 */
public class ArcInfoASCGridImporter //extends GeomImporter
{
    /** Not intended to be instantiated as there is no local state
     */
    private ArcInfoASCGridImporter() {}



    /** Read geospatial grid data from fileName into given field
     *
     * Note that NODATA values are read in as is without substitution.
     *
     * @param source is the data stream for the file
     * @param type denotes the base type as either integer or double-based
     * @param field to be populated
     * 
     * 
     */
    public static void read(InputStream source, final GeomGridField.GridDataType type, GeomGridField field)
    {
        try
        {
            int width = 0;
            int height = 0;

            Scanner scanner = new Scanner(source);
            scanner.useLocale(Locale.US);

            scanner.next(); // skip "ncols"
            width = scanner.nextInt();

            scanner.next(); // skip "nrows"
            height = scanner.nextInt();


            double xllcorner = 0.0; // X lower left corner
            double yllcorner = 0.0; // Y "     "    "
            double cellSize = 0.0;  // dimensions of grid cell in coordinate
                                    // system units

            scanner.next(); // skip "xllcorner"
            xllcorner = scanner.nextDouble();

            scanner.next(); // skip "yllcorner"
            yllcorner = scanner.nextDouble();

            scanner.next(); // skip "cellsize"
            cellSize = scanner.nextDouble();

            // Skip the optional NODATA line if it exists
            if ( scanner.hasNext("NODATA_value") )
            {
                // Have to do this twice to get past the NODATA line
                String nextLine = scanner.nextLine();
                nextLine = scanner.nextLine();

//                System.out.println("nextLine: " + nextLine);
            }

            // We should now be at the first line of data.  Given how the user
            // wants to interpret the data (i.e., as integers or floats) we'll
            // have to obviously read the datat a little differently.

            AbstractGrid2D grid = null;

            switch (type)
            {
                case INTEGER:
                    grid = new IntGrid2D(width, height);
                    readIntegerBased(scanner, width, height, (IntGrid2D) grid);
                    break;
                case DOUBLE:
                    grid = new DoubleGrid2D(width, height);
                    readDoubleBased(scanner, width, height, (DoubleGrid2D) grid);
                    break;
            }

            field.setGrid(grid);

            // Before we go, ensure that we've got the MBR and cell dimensions
            // all sorted.

            field.setPixelHeight(cellSize);
            field.setPixelWidth(cellSize);

            Envelope MBR = new Envelope(xllcorner, xllcorner + cellSize * width,
                                        yllcorner + cellSize * height, yllcorner);

            field.setMBR(MBR);

            scanner.close();
            
        } catch (IOException ex)
        {  // XXX Yes, but is this due to missing file or some other problem?
            Logger.getLogger(ArcInfoASCGridImporter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }

    }

    /** Reads integer-based geospatial data from ARC/INFO ASCII GRID file
     *
     * intGrid2D.field populated with data found in reader.
     *
     * @throws IOException if problem reading data
     * 
     * @see read()
     */
    private static void readIntegerBased(Scanner scanner, int width, int height, IntGrid2D intGrid2D) throws IOException
    {
        int currentInt;

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                currentInt = scanner.nextInt();
                intGrid2D.set(x, y, currentInt);
            }
        }
    }
    

    /** Reads real-based geospatial data from ARC/INFO ASCII GRID file
     *
     * doubleGrid2D.field populated with data found in reader.
     *
     * @throws IOException if problem reading data
     *
     * @see read()
     */
    private static void readDoubleBased(Scanner scanner, int width, int height, DoubleGrid2D doubleGrid2D) throws IOException
    {
        double currentDouble;

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                currentDouble = scanner.nextDouble();
                doubleGrid2D.set(x, y, currentDouble);
            }
        }
    }

}
