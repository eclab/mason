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

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.io.WKTReader;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdal.ogr.*;
import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.MasonGeometry;



/** 
 * OGRImportor uses the OGR JNI interface to read geospatial data into the GeomVectorField.
 *
 */
public class OGRImporter 
{

    /**
     * @param inputResource for the data
     * @param field into which to place read in data
     * @param masked specifies attributes we want
     * @throws FileNotFoundException
     */
    public static void read(final URL inputResource, GeomVectorField field, Bag masked) throws FileNotFoundException
    {
        // register all the data format drivers
        ogr.RegisterAll();

        DataSource dataSource = ogr.Open(inputResource.toString(), false);
        if (dataSource == null)
        {
            throw new FileNotFoundException(inputResource + " not found");
        }

        Driver driver = dataSource.GetDriver();
        if (driver == null)
        {
            throw new FileNotFoundException(inputResource + " not found");
        }

//        System.out.println("INFO: Open of `" + input + "'\n"
//            + "      using driver `" + driver.GetName() + "' successful.");

        for (int i = 0; i < dataSource.GetLayerCount(); i++)
        {
            Layer layer = dataSource.GetLayer(i);

            if (layer == null)
            {
                System.out.println("FAILURE: Couldn't fetch advertised layer " + i + "!");
                return;
            } else
            {
                readLayer(layer, field, masked);
            }
        }
    }



    private static void readLayer(Layer layer, GeomVectorField field, Bag masked)
    {
        FeatureDefn poDefn = layer.GetLayerDefn();

        if (poDefn.GetGeomType() != ogr.wkbLineString
            && poDefn.GetGeomType() != ogr.wkbPolygon
            && poDefn.GetGeomType() != ogr.wkbPoint)
        {
            System.out.println("Unsupported type: " + poDefn.GetGeomType());
            return;
        }

        WKTReader rdr = new WKTReader();
        String wktString = null;
        Feature feature = null;
        com.vividsolutions.jts.geom.Geometry geometry = null;
        feature = layer.GetNextFeature();


        while (feature != null)
        {
            Geometry ogrGeometry = feature.GetGeometryRef();

            if (ogrGeometry == null)
            {
                feature = layer.GetNextFeature();
                continue;
            }

            wktString = ogrGeometry.ExportToWkt();
            try
            {
                Map<String, AttributeValue> myAttributeInfo = readAttributes(feature, masked);
                geometry = rdr.read(wktString);

                if (geometry instanceof GeometryCollection)
                {
                    GeometryCollection gc = (GeometryCollection) geometry;
                    for (int i = 0; i < gc.getNumGeometries(); i++)
                    {
                        com.vividsolutions.jts.geom.Geometry geom = gc.getGeometryN(i);
                        MasonGeometry mg = new MasonGeometry(geom);
                        mg.addAttributes(myAttributeInfo);
                        field.addGeometry(mg);
                    }
                } else
                {
                    MasonGeometry mg = new MasonGeometry(geometry);
                    mg.addAttributes(myAttributeInfo);
                    field.addGeometry(mg);
                }
            } catch (Exception ex)
            {
                Logger.getLogger(OGRImporter.class.getName()).log(Level.SEVERE, null, ex);
            }

            feature.delete(); // free up resources
            feature = layer.GetNextFeature();
        }
    }



    public static Map<String, AttributeValue> readAttributes(Feature feature, Bag masked)
    {
        String key = "";
        Object val;
        TreeMap<String, AttributeValue> fields = new TreeMap<String, AttributeValue>();
        for (int i = 0; i < feature.GetFieldCount(); i++)
        {
            FieldDefn fieldDef = feature.GetFieldDefnRef(i);
            key = fieldDef.GetNameRef();

            if (masked == null || masked.contains(key))
            {
                int fieldType = fieldDef.GetFieldType();

                if (fieldType == ogrConstants.OFTString)
                {
                    val = feature.GetFieldAsString(i);
                } else if (fieldType == ogrConstants.OFTInteger)
                {
                    val = new Integer(feature.GetFieldAsInteger(i));
                } else if (fieldType == ogrConstants.OFTReal)
                {
                    val = new Double(feature.GetFieldAsDouble(i));
                }
                else // If it's not a number or a string, just convert to string
                {
                    val = feature.GetFieldAsString(i);
                }

                AttributeValue attr = new AttributeValue(val);
//                attr.setValue(val);

                fields.put(key, attr);
            }
        }
        return fields;
    }

}
