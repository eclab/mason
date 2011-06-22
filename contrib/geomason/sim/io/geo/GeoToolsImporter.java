/* 
Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
George Mason University Mason University Licensed under the Academic
Free License version 3.0

See the file "LICENSE" for more information
*/
package sim.io.geo; 

import sim.field.geo.GeomVectorField;
import sim.util.Bag;

import java.io.File;
import java.io.Serializable; 
import java.io.FileNotFoundException; 
import java.util.*; 
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry; 

import sim.util.geo.AttributeField;
import sim.util.geo.MasonGeometry;

/** 
    Use the GeoTools Java API to read geospatial data into the GeomVectorField.
*/ 

public class GeoToolsImporter extends GeomImporter {

    public void ingest(final String input, GeomVectorField field, Bag masked) throws FileNotFoundException
    {
        try { 
            File file = new File(input); 
                        
            if (!file.exists()) 
                throw new FileNotFoundException(file.getAbsolutePath()); 
                        
            Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
                                        
            connectParameters.put("url", file.toURI().toURL());
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
                        
            try {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    TreeMap<?,?> attrs = readAttributes(feature, masked); 
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    geometry.setUserData(attrs); 
                    field.addGeometry(new MasonGeometry(geometry)); 
                }
            }
            finally {
                if( iterator != null )
                    iterator.close();
            }
        } catch (Exception e) { System.out.println ("Exception in GeoToolsImportor::ingest:"); e.printStackTrace(); } 
    }
    
    
    public TreeMap<String, AttributeField> readAttributes(SimpleFeature feature, Bag masked)
    {
    	TreeMap<String, AttributeField> fields = new TreeMap<String, AttributeField>(); 
    	 String key=""; 
         SimpleFeatureType type = feature.getFeatureType(); 
                 
         for (int i=0; i < feature.getAttributeCount(); i++) { 
             AttributeDescriptor desc = type.getDescriptor(i); 
             key = desc.getLocalName(); 
             if (key.equals("the_geom")) continue;
                         
             if (masked == null || masked.contains(key)) {
                 AttributeType attrType = desc.getType(); 
                 String className = attrType.getBinding().getName(); 
                                 
                 char t='C'; 
                 if (className.indexOf("String") != -1)
                     t = 'C'; 
                 else if (className.indexOf("Double") != -1)
                     t = 'N'; 
                 else if (className.indexOf("Integer") != -1) 
                     t = 'N'; 
                                 
                 List<Filter> l = attrType.getRestrictions(); 
                 Iterator<Filter> iter = l.iterator(); 
                 int len = 20;
                 while (iter.hasNext()) { 
                     Filter f = iter.next(); 
                     if (f instanceof BinaryComparisonOperator) { 
                         BinaryComparisonOperator b = (BinaryComparisonOperator) f; 
                         String ex1 = b.getExpression1().toString(); 
                         if (ex1.indexOf("length") != -1) { 
                             String ex2 = b.getExpression2().toString(); 
                             len = Integer.valueOf(ex2).intValue(); 
                             break; 
                         }
                     }
                 }
                                 
                 AttributeField attrField = new AttributeField(key, t, len, false);
                 attrField.value = feature.getAttribute(i); 
                 fields.put(key, attrField); 
             }
         }
         return fields; 
    }
    
}