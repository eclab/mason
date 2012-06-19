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

import com.vividsolutions.jts.geom.Geometry;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.MasonGeometry;



/** 
    Use the GeoTools Java API to read geospatial data into the GeomVectorField.
 */
public class GeoToolsImporter
{
    private GeoToolsImporter()
    {
    }


    public static void read(final URL input, GeomVectorField field, final Bag masked) throws FileNotFoundException
    {
        try
        {
            Map<String, Serializable> connectParameters = new HashMap<String, Serializable>();

            connectParameters.put("url", input);
            DataStore dataStore = DataStoreFinder.getDataStore(connectParameters);

            // we are now connected
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
            FeatureIterator<SimpleFeature> iterator;

            featureSource = dataStore.getFeatureSource(typeName);
            collection = featureSource.getFeatures();
            iterator = collection.features();

            try
            {
                while (iterator.hasNext())
                {
                    SimpleFeature feature = iterator.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    
                    MasonGeometry mg = new MasonGeometry(geometry);
                    mg.addAttributes(readAttributes(feature, masked));

                    field.addGeometry(mg);
                }
            } finally
            {
                if (iterator != null)
                {
                    iterator.close();
                }
            }
        } catch (Exception e)
        {
            System.out.println("Exception in GeoToolsImportor::ingest:");
            e.printStackTrace();
        }
    }



    public static Map<String,AttributeValue> readAttributes(SimpleFeature feature, Bag masked)
    {
        Map<String, AttributeValue> fields = new TreeMap<String, AttributeValue>();

        for (Property property : feature.getProperties())
        {
            String name = property.getName().getLocalPart();

            if (name.equals("the_geom"))
            {
                continue;
            }

            if (masked == null || masked.contains(name))
            {
                Object value = property.getValue();

                fields.put(name, new AttributeValue(value));
            }
        }

        return fields;
    }

}