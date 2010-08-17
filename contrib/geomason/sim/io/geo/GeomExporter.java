package sim.io.geo; 

import java.io.FileNotFoundException;
import sim.field.geo.GeomField;
import java.io.*; 

public abstract class GeomExporter { 

    /**
       Writes the information stored in the GeomField to disk.  Don't include the file extension in 
       the output argument, as the implementing exporter will handle that.  Depending on the GeomExporter
       used, the driver argument determines the file type.  
    */
        
    public void write(String output, String driver, GeomField field) throws FileNotFoundException {}
    
    public byte[] getBytes(Object obj) { 
    	try { 
    
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        ObjectOutputStream oos = new ObjectOutputStream(bos); 
        oos.writeObject(obj);
        oos.flush(); 
        oos.close(); 
        bos.close();
        byte [] data = bos.toByteArray();
        return data;
    	} catch (IOException e) { System.out.println(e); } 
    	return null; 
    }

} 