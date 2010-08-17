
package sim.io.geo;
import com.vividsolutions.jts.io.WKTReader;
import java.io.FileNotFoundException;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdal.ogr.*; 

import sim.field.geo.GeomField;
import sim.util.Bag;
import sim.util.geo.AttributeField;
import sim.util.geo.MasonGeometry;
import com.vividsolutions.jts.geom.GeometryCollection; 
import org.opengis.feature.simple.*; 


/** 
    OGRImportor uses the OGR JNI interface to read geospatial data into the GeomField.  
*/
public class OGRImporter implements GeomImporter {
   
    /**  */
    public void ingest(final String input, GeomField field, Bag masked) throws FileNotFoundException
    {
        // register all the data format drivers
        ogr.RegisterAll();

        DataSource dataSource = ogr.Open(input, false);
        if ( dataSource == null )
            throw new FileNotFoundException(input + " not found");

        Driver driver = dataSource.GetDriver();
        if ( driver == null )
            throw new FileNotFoundException(input + " not found");

        System.out.println("INFO: Open of `" + input + "'\n" +
                           "      using driver `" + driver.GetName() + "' successful.");
                
        for (int i = 0; i < dataSource.GetLayerCount(); i++)
            {
                Layer layer = dataSource.GetLayer(i);

                if (layer == null)
                    {
                        System.out.println("FAILURE: Couldn't fetch advertised layer " + i + "!");
                        return;
                    }
                else 
                    ingestLayer(layer, field, masked);
            }
    }

  
    private void ingestLayer(Layer layer, GeomField field, Bag masked)
    {
        FeatureDefn poDefn = layer.GetLayerDefn();

        if (poDefn.GetGeomType() != ogr.wkbLineString && 
            poDefn.GetGeomType() != ogr.wkbPolygon) { 
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
                   
                if (ogrGeometry == null) { 
                    feature = layer.GetNextFeature(); 
                    continue; 
                }
                   
                wktString = ogrGeometry.ExportToWkt();
                try
                    {
                		TreeMap attributeInfo = readAttributes(feature, masked);
                        geometry = rdr.read(wktString);
                                                        
                        if (geometry instanceof GeometryCollection) {
                            GeometryCollection gc = (GeometryCollection) geometry; 
                            for (int i=0; i < gc.getNumGeometries(); i++) { 
                            	com.vividsolutions.jts.geom.Geometry geom = gc.getGeometryN(i); 
                            	geom.setUserData(attributeInfo); 
                                field.addGeometry(new MasonGeometry(geom)); ;
                            }
                        }
                        else { 
                        	geometry.setUserData(attributeInfo);
                            field.addGeometry(new MasonGeometry(geometry));
                        }
                    }
                catch (Exception ex)
                    {
                        Logger.getLogger(OGRImporter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        
                feature.delete(); // free up resources
                feature = layer.GetNextFeature();
            }
    }
    
    public TreeMap readAttributes(SimpleFeature feature, Bag masked)
    {
	    String  key=""; 
	    Object val; 
	    TreeMap fields = new TreeMap();
	    for (int i=0 ; i < feature.GetFieldCount(); i++) { 
	        FieldDefn fieldDef = feature.GetFieldDefnRef(i); 
	        key = fieldDef.GetNameRef(); 
	        if (masked == null || masked.contains(key)) { 
	                            
	            val = null; 
	            char type; 
	            int fieldType = fieldDef.GetFieldType(); 
	            if (fieldType == ogrConstants.OFTString) { 
	                type = 'C'; 
	                val = new String(feature.GetFieldAsString(i)); 
	            }
	            else if (fieldType == ogrConstants.OFTInteger) { 
	                type = 'N'; 
	                val = new Integer(feature.GetFieldAsInteger(i)); 
	            }
	            else { //fieldType == ogrConstants.OFTReal 
	                type = 'N'; 
	                val = new Double(feature.GetFieldAsDouble(i)); 
	            }
	                                                                            
	            AttributeField attr = new AttributeField(key, type, fieldDef.GetWidth()); 
	            attr.value = val; 
	            fields.put(key, attr); 
	        }
	    }
	    return fields; 
    }
    
}
