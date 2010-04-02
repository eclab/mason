package sim.io.geo; 

import java.io.*; 
import sim.util.*; 
import sim.util.geo.*; 
import sim.field.geo.*; 
import java.nio.*; 
import java.nio.channels.*; 
import sim.util.geo.GeomWrapper;
import com.vividsolutions.jts.geom.*; 

/** 
 A native MASON importer to read ERSI shapefile data into the GeomField.  Other data formats are not 
 supported.  
 */ 

public class ShapeFileImporter extends GeomImporter {

    public
    ShapeFileImporter(Class wrapper)
    {
        super(wrapper);
    }

    public
    ShapeFileImporter()
    {
        super();
    }


	
	public void ingest(final String input, GeomField field, Bag masked) throws FileNotFoundException
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
			int numRecords = dbBuffer.getInt(4); 
			int headerSize = dbBuffer.getShort(8); 
			int recordSize = dbBuffer.getShort(10); 
						
			int fieldCnt = (short) ((headerSize - 1) / 32 - 1);
			AttributeField fields[] = new AttributeField[fieldCnt]; 
						
			RandomAccessFile inFile = new RandomAccessFile(dbFile, "r"); 
			
			inFile.seek(32); 
			
			byte c[] = new byte[32];
			char type;
			int length;
			for (int i=0; i < fieldCnt; i++) {
				inFile.readFully(c, 0, 11); 
				
				int j=0;
				for (j=0; j < 12 && c[j] != 0; j++); 
				String name = new String(c, 0, j); 
				
				type = (char)inFile.readByte(); 
				inFile.read(c, 0, 4);  // data address 
				
				byte b = inFile.readByte();
				if (b > 0) length = (int)b; 
				else length = 256 + (int)b; 
				
				inFile.skipBytes(15); 
				fields[i] = new AttributeField(name, type, length); 
			}
						
			inFile.seek(0); 
			inFile.skipBytes(headerSize); 
			
			FileChannel channel = new FileInputStream(file).getChannel();
			ByteBuffer byteBuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int)channel.size());
			channel.close(); 
			
			// advance to the first record
			byteBuf.position(100); 
			
			while (byteBuf.hasRemaining()) { 
				byteBuf.order(ByteOrder.BIG_ENDIAN);
				int recordNumber = byteBuf.getInt(); 
				int recordLength = byteBuf.getInt(); 
				
				byteBuf.order(ByteOrder.LITTLE_ENDIAN);
				 
				int recordType = byteBuf.getInt(); 
							
				if (recordType != 1 && recordType != 3 && recordType != 5) continue; 
				
				double minX = byteBuf.getDouble(); 
				double minY = byteBuf.getDouble(); 
				double maxX = byteBuf.getDouble(); 
				double maxY = byteBuf.getDouble(); 
				
				int numParts = byteBuf.getInt(); 
				int numPoints = byteBuf.getInt(); 
				
				// get the array of part indicies 
				int partIndicies[] = new int[numParts]; 
				for (int i=0; i < numParts ; i++) 
					partIndicies[i] = byteBuf.getInt(); 
				
				// get the array of points 
				Coordinate pointsArray[] = new Coordinate[numPoints]; 
				for (int i=0; i < numPoints; i++) 
					pointsArray[i] = new Coordinate(byteBuf.getDouble(), byteBuf.getDouble()); 
				
				GeometryFactory geomFactory = new GeometryFactory(); 
				
				ShapeFileInfo geoInfo = new ShapeFileInfo();
				
				byte r[] = new byte[recordSize]; 
				inFile.read(r); 
				
				int start1 =1; 
				
				for (int k=0; k < fieldCnt; k++) { 
					if (masked == null || masked.contains(fields[k].name)) { 
						String str = new String(r, start1, fields[k].size); 
						str = str.trim(); 
						Object o  = str;  // fields[k].type == 'C'
									
						if (fields[k].type == 'N')  { 
							if (str.length()==0) str = "0"; 
							if (str.indexOf('.') != -1)
								o = new Double(str); 
							else 
								o = new Integer(str); 
						}
						else if (fields[k].type == 'L')
							o = new Boolean(str); 
						
						AttributeField fld = (AttributeField)fields[k].clone(); 
						fld.value = o; 
						geoInfo.fields.put(fld.name, fld);
					}
					start1 += fields[k].size; 
				}
				
				if (recordType == 1) { 
					Point p = geomFactory.createPoint(pointsArray[0]); 
					GeomWrapper mg = makeGeomWrapper(p, geoInfo);
					field.addGeometry(mg);
				}
				else if (recordType == 3 || recordType == 5) { 
					for (int i=0; i < numParts; i++) { 
						int start = partIndicies[i]; 
						int end = numPoints; 
						if (i < numParts - 1 ) end = partIndicies[i+1]; 
						int size = end - start; 
						Coordinate coords[] = new Coordinate[size]; 
						
												
						for (int j=0; j < size; j++)
							coords[j] = new Coordinate(pointsArray[start+j]); 
						
						if (recordType == 3) { 
							LineString ls = geomFactory.createLineString(coords); 
							GeomWrapper mg = makeGeomWrapper(ls, geoInfo);
							field.addGeometry(mg); 
						}
						else {
							LinearRing lr = geomFactory.createLinearRing(coords); 
							Polygon p = geomFactory.createPolygon(lr, null); 
							GeomWrapper mg = makeGeomWrapper(p, geoInfo);
							field.addGeometry(mg); 
						}
					}
				}
				else 
					System.err.println("Unknown shape type in " + input); 
			}
		} catch (IOException e) { System.out.println("Error in MasonImporter: " + e); }
	}
}
