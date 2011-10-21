/* 
Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
George Mason University Mason University Licensed under the Academic
Free License version 3.0

See the file "LICENSE" for more information
*/
/*
 * ArcInfoASCGridImporter.java
 *
 */
package sim.io.geo;

import com.vividsolutions.jts.geom.Envelope;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.field.geo.GeomGridField;
import sim.field.grid.AbstractGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import java.io.FileNotFoundException;
import java.io.FileReader;



/** Importer for ESRI Arc/Info ASCII GRID formatted files
 *
 */
public class ArcInfoASCGridImporter extends GeomImporter
{
    /** Read Arc Info grid data from fileName into given field
     * 
     * @param fileName containing the grid data
     * @param referenceClass used to find the data as a resource
     * @param type to denote integer or double data
     * @param field in which the data is to be loaded
     * 
     * @throws FileNotFoundException 
     */
    public void ingest(String fileName, Class<?> referenceClass, GridDataType type, GeomGridField field) throws FileNotFoundException
    {
        String filePath = null;

        try
        {
            filePath = referenceClass.getResource(fileName).getPath();
        } catch (NullPointerException np1)
        {
            throw new FileNotFoundException(fileName);
        }

        ingest( filePath, type, field );
    }


    /** Read geospatial grid data from fileName into given field
     *
     * Note that NODATA values are read in as is without substitution.
     *
     * @param fileName is file name of data file
     * @param type denotes the base type as either integer or double-based
     * @param field is field to populate
     * 
     * @throws FileNotFoundException if 'inputFile' not found
     * @thrown RuntimeException if unable to read data
     */
    @Override
    public void ingest(String fileName, GridDataType type, GeomGridField field) throws FileNotFoundException
    {
        try
        {
            int width = 0;
            int height = 0;

            BufferedReader reader = new BufferedReader(new FileReader(fileName));

            String currentLine; // current line of input


            currentLine = reader.readLine();

            // the tokenized contents of 'line'
            String[] currentTokens = currentLine.split("\\s+");

            width = Integer.parseInt(currentTokens[1]);

            currentLine = reader.readLine();
            currentTokens = currentLine.split("\\s+");
            height = Integer.parseInt(currentTokens[1]);


            double xllcorner = 0.0; // X lower left corner
            double yllcorner = 0.0; // Y "     "    "
            double cellSize = 0.0;  // dimensions of grid cell in coordinate
                                    // system units

            currentLine = reader.readLine();
            currentTokens = currentLine.split("\\s+");
            xllcorner = Double.parseDouble(currentTokens[1]);

            currentLine = reader.readLine();
            currentTokens = currentLine.split("\\s+");
            yllcorner = Double.parseDouble(currentTokens[1]);

            currentLine = reader.readLine();
            currentTokens = currentLine.split("\\s+");
            cellSize = Double.parseDouble(currentTokens[1]);

            // This is the NODATA line, which we don't need.
            // FIXME: NODATA is an optional line, but almost all
            // ASCII GRID files contain it; however it might be nice to check
            // just in case.
            currentLine = reader.readLine();

            // We should now be at the first line of data.  Given how the user
            // wants to interpret the data (i.e., as integers or floats) we'll
            // have to obviously read the datat a little differently.

            AbstractGrid2D grid = null;

            switch (type)
            {
                case INTEGER:
                    grid = new IntGrid2D(width, height);
                    readIntegerBased(reader, width, height, (IntGrid2D) grid);
                    break;
                case DOUBLE:
                    grid = new DoubleGrid2D(width, height);
                    readDoubleBased(reader, width, height, (DoubleGrid2D) grid);
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
     * @see ingest()
     */
    private void readIntegerBased(BufferedReader reader, int width, int height, IntGrid2D intGrid2D) throws IOException
    {
        String currentLine;
        String[] currentTokens;

        for (int y = 0; y < height; y++)
        {
            currentLine = reader.readLine();
            currentTokens = currentLine.split("\\s+");

            for (int x = 0; x < width; x++)
            {
                intGrid2D.set(x, y, Integer.parseInt(currentTokens[x]));
            }
        }
    }

    /** Reads real-based geospatial data from ARC/INFO ASCII GRID file
     *
     * doubleGrid2D.field populated with data found in reader.
     *
     * @throws IOException if problem reading data
     *
     * @see ingest()
     */
    private void readDoubleBased(BufferedReader reader, int width, int height, DoubleGrid2D doubleGrid2D) throws IOException
    {
        String currentLine;
        String[] currentTokens;

        for (int y = 0; y < height; y++)
        {
            currentLine = reader.readLine();
            currentTokens = currentLine.split("\\s+");

            for (int x = 0; x < width; x++)
            {
                doubleGrid2D.set(x, y, Double.parseDouble(currentTokens[x]));
            }
        }
    }

}
