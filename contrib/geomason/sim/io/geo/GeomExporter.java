package sim.io.geo; 

import java.io.FileNotFoundException;
import sim.field.geo.GeomField;

public interface GeomExporter { 

    /**
       Writes the information stored in the GeomField to disk.  Don't include the file extension in 
       the outout arguement, as the implementing exporter will handle that.  Depending on the GeomExporter
       used, the driver arguement determines the file type.  
    */
        
    public void write(String output, String driver, GeomField field) throws FileNotFoundException; 

} 