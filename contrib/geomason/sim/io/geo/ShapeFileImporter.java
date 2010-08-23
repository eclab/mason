package sim.io.geo; 

import java.io.*; 
import sim.util.*; 
import sim.util.geo.*; 
import sim.field.geo.*; 
import java.nio.*; 
import java.nio.channels.*; 
import com.vividsolutions.jts.geom.*; 
import java.util.ArrayList; 
import java.util.Collections;

/** 
    A native Java importer to read ERSI shapefile data into the GeomVectorField.  We assume the input file follows the
    standard ESRI shapefile format.    
*/ 

public class ShapeFileImporter extends GeomImporter {
 
    public void ingest(final String input, GeomVectorField field, Bag masked) throws FileNotFoundException
    {
        try { 
            // open shp file 
            File file = new File(input); 
            if (!file.exists()) 
                throw new FileNotFoundException(file.getAbsolutePath()); 
                                
            // open dbf file 
            String s = input.substring(0, input.length()-4) + ".dbf";                       
            File dbFile = new File(s);
            if (!dbFile.exists())
                throw new FileNotFoundException(dbFile.getAbsolutePath()); 
                
                        
            FileChannel dbChannel = new FileInputStream(dbFile).getChannel(); 
            ByteBuffer dbBuffer = dbChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)dbChannel.size()); 
            dbChannel.close(); 
                        
            dbBuffer.order(ByteOrder.LITTLE_ENDIAN);
            int headerSize = dbBuffer.getShort(8); 
            int recordSize = dbBuffer.getShort(10); 
                                                
            int fieldCnt = (short) ((headerSize - 1) / 32 - 1);
            AttributeField fields[] = new AttributeField[fieldCnt]; 
                                                
            RandomAccessFile inFile = new RandomAccessFile(dbFile, "r"); 
                        
            inFile.seek(32); 
                        
            byte c[] = new byte[32];
            char type[] = new char[fieldCnt];
            int length;
            for (int i=0; i < fieldCnt; i++) {
                inFile.readFully(c, 0, 11); 
                                
                int j=0;
                for (j=0; j < 12 && c[j] != 0; j++); 
                String name = new String(c, 0, j); 
                type[i] = (char)inFile.readByte();                 
                fields[i] = new AttributeField(name);
                
                inFile.read(c, 0, 4);  // data address 
                                
                byte b = inFile.readByte();
                if (b > 0) length = (int)b; 
                else length = 256 + (int)b; 
                
                fields[i].fieldSize = length; 
                inFile.skipBytes(15); 
            }
                                                
            inFile.seek(0); 
            inFile.skipBytes(headerSize); 
                        
            FileChannel channel = new FileInputStream(file).getChannel();
            ByteBuffer byteBuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int)channel.size());
            channel.close(); 
                        
            GeometryFactory geomFactory = new GeometryFactory(); 

            
            // advance to the first record
            byteBuf.position(100); 
                        
            while (byteBuf.hasRemaining()) { 
                // advance past two int: recordNumber and recordLength 
                byteBuf.position(byteBuf.position() + 8); 
                                
                byteBuf.order(ByteOrder.LITTLE_ENDIAN);
                                 
                int recordType = byteBuf.getInt(); 
                                                        
                if (recordType != 1 && recordType != 3 && recordType != 5) continue; 
                                
                
                // advance past four doubles: minX, minY, maxX, maxY
                byteBuf.position(byteBuf.position() + 32);
                                
                int numParts = byteBuf.getInt(); 
                int numPoints = byteBuf.getInt(); 
                                
                // get the array of part indices 
                int partIndicies[] = new int[numParts]; 
                for (int i=0; i < numParts ; i++) 
                    partIndicies[i] = byteBuf.getInt(); 
                                
                // get the array of points 
                Coordinate pointsArray[] = new Coordinate[numPoints]; 
                for (int i=0; i < numPoints; i++) 
                    pointsArray[i] = new Coordinate(byteBuf.getDouble(), byteBuf.getDouble()); 
                          
                byte r[] = new byte[recordSize]; 
                inFile.read(r); 
                                
                int start1 =1; 
                           
                attributeInfo  = new ArrayList<AttributeField>(); 
                
                for (int k=0; k < fieldCnt; k++) { 
                    
                        String str = new String(r, start1, fields[k].fieldSize); 
                        str = str.trim(); 
                        Object o  = str;  // fields[k].type == 'C'
                                                                        
                        if (type[k] == 'N')  { 
                            if (str.length()==0) str = "0"; 
                            if (str.indexOf('.') != -1)
                                o = new Double(str); 
                            else 
                                o = new Integer(str); 
                        }
                        else if (type[k] == 'L')
                            o = new Boolean(str); 
                                                
                        AttributeField fld = (AttributeField)fields[k].clone(); 
                        fld.value = o; 
                        
                        if (masked != null && !masked.contains(fields[k].name)) 
                        	fld.hidden = true; 
                        else 
                        	fld.hidden = false; 
                        
                        attributeInfo.add(fld); 
                    
                    start1 += fields[k].fieldSize;
                }
                
                Geometry geom = null; 
                
                if (recordType == POINT) 
                    geom = geomFactory.createPoint(pointsArray[0]); 
                else if (recordType == LINE || recordType == POLYGON) { 
                	Geometry[] parts = new Geometry[numParts]; 

                    for (int i=0; i < numParts; i++) { 
                        int start = partIndicies[i]; 
                        int end = numPoints; 
                        if (i < numParts - 1 ) end = partIndicies[i+1]; 
                        int size = end - start; 
                        Coordinate coords[] = new Coordinate[size]; 
                                                                                               
                        for (int j=0; j < size; j++)
                            coords[j] = new Coordinate(pointsArray[start+j]); 
                                                
                        if (recordType == LINE)                        
                            parts[i] = geomFactory.createLineString(coords); 
                        else {
                            LinearRing lr = geomFactory.createLinearRing(coords); 
                            parts[i] = geomFactory.createPolygon(lr, null); 
                        }
                    }
                    if (recordType == LINE) { 
                    	LineString[] ls = new LineString[numParts]; 
                    	for (int i=0; i < numParts; i++) ls[i] = (LineString)parts[i]; 
                    	geom = geomFactory.createMultiLineString(ls); 
                    }
                    else {
                    	Polygon[] poly = new Polygon[numParts]; 
                    	for (int i=0; i < numParts; i++) poly[i] = (Polygon)parts[i];
                    	geom = geomFactory.createMultiPolygon(poly); 
                    }
                }
                else 
                    System.err.println("Unknown shape type in " + input); 
                
                if (geom != null) { 
                	Collections.sort(attributeInfo, GeometryUtilities.attrFieldCompartor); 
                	geom.setUserData(attributeInfo); 
                	try { 
                		MasonGeometry g = (MasonGeometry)masonGeometryClass.newInstance(); 
                		g.geometry = geom; 
                		field.addGeometry(g);
                	} catch (Exception e) { e.printStackTrace(); } 
                }
                
            }
        } catch (IOException e) { System.out.println("Error in MasonImporter: " + e); }
    }
}
