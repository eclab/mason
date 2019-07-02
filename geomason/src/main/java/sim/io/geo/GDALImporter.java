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
import java.io.FileNotFoundException;
import java.net.URL;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.grid.AbstractGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;

/**
 *
 */
public class GDALImporter 
{
    /** Read geospatial grid data from inputSource into field
     *
     * This only reads the first band of data.
     *
     * @param inputSource of grid data
     * @param type denotes the base type as either integer or double-based
     * @param field is field to populate
     *
     * @throws FileNotFoundException
     * @thrown RuntimeException if unable to read data
     *
     * TODO: add support for reading specific band, or multiband datasets
     */
    public static void read(final URL inputSource, GridDataType type, GeomGridField field) throws FileNotFoundException
    {
        gdal.AllRegister(); // register the plugins for all supported data formats

        Dataset dataset = gdal.Open(inputSource.getFile());

        if (dataset == null)
        {
            throw new FileNotFoundException(inputSource + " not found");
        }

        int xSize = dataset.getRasterXSize();
        int ySize = dataset.getRasterYSize();
        
        AbstractGrid2D grid = null;
        
        // GDAL starts from 1, not zero
        Band band = dataset.GetRasterBand(1);

        if (band == null)
        {
            throw new RuntimeException("Unable to get raster band");
        }
   
        switch (type)
        {
            case INTEGER:
                grid = new IntGrid2D(xSize, ySize);
                readIntegerBased(xSize, ySize, band, (IntGrid2D) grid);
                break;
            case DOUBLE:
                grid = new DoubleGrid2D(xSize, ySize);
                readDoubleBased(xSize, ySize, band, (DoubleGrid2D) grid);
                break;
        }

        field.setGrid(grid);

        // See http://gdal.org/ for the magic meaning of transformData
        double [] transformData = dataset.GetGeoTransform();

        // Grab the pixel and image dimensions from the GeoTransform object
        field.setPixelHeight(Math.abs(transformData[5]));
        field.setPixelWidth(Math.abs(transformData[1]));

       // Now to set up the image's MBR
        Envelope MBR = new Envelope(transformData[0], transformData[0] + transformData[1] * xSize,
                transformData[3] + transformData[5] * ySize, transformData[3]);

        field.setMBR(MBR);

        dataset.FlushCache();

        dataset = null;
    }



    /** Not intended to be instantiated as there is no local state
     */
    private GDALImporter()
    {
    }


    /** Reads integer-based geospatial data
     *
     * Helper function for ingest().
     *
     * @param xSize
     * @param ySize
     * @param band
     * @param grid
     * @throws RuntimeException if GDAL complains about reading the data
     *
     * @see ingest()
     */
    private static void readIntegerBased(int xSize, int ySize, Band band, IntGrid2D grid) throws RuntimeException
    {
        int result = 0;
        int [] line = new int[xSize];

        for (int currRow = 0; currRow < ySize; currRow++)
        {
            //result = band.ReadRaster(0, currRow, xSize, 1, line);
            if (result != gdalconstConstants.CE_None)
            {
                throw new RuntimeException("Problem reading raster");
            }
            for (int currCol = 0; currCol < xSize; currCol++)
            {
                int i = line[currCol];
                grid.set(currCol, currRow, i);
            }
        }
    }


    /** Reads double-based geospatial data
     *
     * Helper function for ingest().
     *
     * @param xSize
     * @param ySize
     * @param band
     * @param grid
     * @throws RuntimeException if GDAL complains about reading the data
     *
     * @see ingest()
     */
    private static void readDoubleBased(int xSize, int ySize, Band band, DoubleGrid2D grid) throws RuntimeException
    {
        int result = 0;
        float [] line = new float[xSize];

        for (int currRow = 0; currRow < ySize; currRow++)
        {
            //result = band.ReadRaster(0, currRow, xSize, 1, line);
            if (result != gdalconstConstants.CE_None)
            {
                throw new RuntimeException("Problem reading raster");
            }
            for (int currCol = 0; currCol < xSize; currCol++)
            {
                float f = line[currCol];
                grid.set(currCol, currRow, f);
            }
        }
    }


    /**
     * Ganked from GDAL/swig/java/apps/GDALtest.java
     */
	public static void printLastError()
    {
		System.out.println("Last error: " + gdal.GetLastErrorMsg());
		System.out.println("Last error no: " + gdal.GetLastErrorNo());
		System.out.println("Last error type: " + gdal.GetLastErrorType());
	}

}
