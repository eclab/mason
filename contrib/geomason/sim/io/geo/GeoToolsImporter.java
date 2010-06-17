package sim.io.geo; 

import sim.field.geo.GeomField;
import sim.util.Bag;

import java.io.File;
import java.io.Serializable; 
import java.io.FileNotFoundException; 
import java.util.HashMap; 
import java.util.Map; 
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.vividsolutions.jts.geom.Geometry; 
import sim.util.geo.GeomWrapper;

/** 
    Use the GeoTools Java API to read geospatial data into the GeomField.
*/ 

public class GeoToolsImporter extends GeomImporter {

    public
        GeoToolsImporter(Class wrapper)
    {
        super(wrapper);
    }

    public
        GeoToolsImporter()
    {
    }

    

    public void ingest(final String input, GeomField field, Bag masked) throws FileNotFoundException
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
                    GeoToolsInfo geoInfo = new GeoToolsInfo(feature, masked); 
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    GeomWrapper mg = makeGeomWrapper(geometry, geoInfo);
                    field.addGeometry(mg); 
                }
            }
            finally {
                if( iterator != null )
                    iterator.close();
            }
        } catch (Exception e) { System.out.println ("Exception in GeoToolsImportor::ingest:"); e.printStackTrace(); } 
    }
}