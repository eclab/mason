/*
 * GDALImporter.java
 *
 * $Id: GDALImporter.java,v 1.2 2010-09-22 01:20:08 mcoletti Exp $
 */
package sim.io.geo;

import com.vividsolutions.jts.geom.Envelope;
import java.io.FileNotFoundException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import sim.field.geo.GeomGridField;
import sim.field.grid.AbstractGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;

/**
 *
 */
public class GDALImporter extends GeomImporter
{


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
    private void readIntegerBased(int xSize, int ySize, Band band, IntGrid2D grid) throws RuntimeException
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
    private void readDoubleBased(int xSize, int ySize, Band band, DoubleGrid2D grid) throws RuntimeException
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
	public void printLastError()
    {
		System.out.println("Last error: " + gdal.GetLastErrorMsg());
		System.out.println("Last error no: " + gdal.GetLastErrorNo());
		System.out.println("Last error type: " + gdal.GetLastErrorType());
	}


    /** Read geospatial grid data from inputFile into field
     *
     * This only reads the first band of data.
     *
     * @param inputFile is file name of data file
     * @param type denotes the base type as either integer or double-based
     * @param field is field to populate
     * @throws FileNotFoundException if 'inputFile' not found
     * @thrown RuntimeException if unable to read data
     */
    @Override
    public void ingest(String fileName, GridDataType type, GeomGridField field) throws FileNotFoundException
    {
        gdal.AllRegister(); // register the plugins for all supported data formats

        Dataset dataset = gdal.Open(fileName);

        if (dataset == null)
        {
            throw new FileNotFoundException(fileName + " not found");
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

//        for (int i = 0; i < transformData.length; i++)
//        {
//            double d = transformData[i];
//
//            System.out.println("Raster transform: " + i + " " + d);
//        }

        // Grab the pixel and image dimensions from the GeoTransform object
        field.setPixelHeight(Math.abs(transformData[5]));
        field.setPixelWidth(Math.abs(transformData[1]));

//        MBR.init(transformData[0], transformData[0] + Math.abs(transformData[1]) * xSize,
//                 transformData[3], transformData[3] + Math.abs(transformData[5]) * ySize);

//        System.out.println("a: " + transformData[0] + transformData[1] * xSize);
//        System.out.println("b: " + transformData[3] + transformData[5] * ySize);

//       // Now to set up the image's MBR
        Envelope MBR = new Envelope(transformData[0], transformData[0] + transformData[1] * xSize,
                transformData[3] + transformData[5] * ySize, transformData[3]);

        field.setMBR(MBR);

//        System.out.println("projection:" + dataset.GetGCPProjection());
//        System.out.println("Raster MBR:" + MBR);

    }

}
