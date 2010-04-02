package sim.io.geo; 

import java.io.FileNotFoundException; 
import sim.field.geo.GeomField;

import com.vividsolutions.jts.geom.*;
import sim.util.Bag; 
import sim.util.geo.*; 
import java.nio.*; 
import java.nio.channels.*; 
import java.io.*; 
import java.util.*; 
import java.text.*; 

public class ShapeFileExporter implements GeomExporter {

	public void write(String output, String driver, GeomField field) throws FileNotFoundException
	{
		try { 
		
			String shpFileName = output + ".shp"; 
			RandomAccessFile shpFile = new RandomAccessFile(new File(shpFileName), "rw");
		
			///////////
			// SHAPEFILE HEADER
			// 100 bytes long: 9 4-byte int32 and 8 8-byte doubles 
			//////////////
		
			ByteBuffer headerBig = ByteBuffer.allocate(28); 
			headerBig.order(ByteOrder.BIG_ENDIAN); 
			
			// bytes 0 - 3 are fixed hex value 
			headerBig.putInt(9994);
		
			// bytes 4 - 23 are five unused int32 
			for (int i=0; i < 5; i++) headerBig.putInt(0); 
		
			// bytes 24 - 27 are the file length
 			// don't know this yet, so write a placeholder value, and we'll 
			// update later 
			headerBig.putInt(0); 
			
			// write to disk for now, we'll overwrite this later 
			shpFile.write(headerBig.array()); 
			
			// switch endianess due to the wacked ESRI shapefile spec
			ByteBuffer headerLittle = ByteBuffer.allocate(72); 
			headerLittle.order(ByteOrder.LITTLE_ENDIAN); 

			// bytes 28 - 31 are the shapefile version 
			headerLittle.putInt(1000); 
			
			// bytes 32 - 35 are the shapefile type
			headerLittle.putInt(5); 
			
			// bytes 36 - 67 are the MBR in min x, min y, max x, max y format (double) 
			Envelope e = field.getMBR(); 
			headerLittle.putDouble(e.getMinX());  headerLittle.putDouble(e.getMinY()); 
			headerLittle.putDouble(e.getMaxX());  headerLittle.putDouble(e.getMaxY()); 
			
			// bytes 68 - 83 are range of Z in min z, max z (double) 
			// current not used, so put artitrary values 
			headerLittle.putDouble(0.0); headerLittle.putDouble(0.0); 
			
			// bytes 84 - 99 are range of M in min m, max m (double)
			// current not used, so put artitrary values 
			headerLittle.putDouble(0.0); headerLittle.putDouble(0.0); 

			// write the header 
			shpFile.write(headerLittle.array()); 
			
			
			//////////////////
			// the shapefile now contains an arbitrary number of records (each of arbitrary 
			// length), where each record
			// consists of a header, followed by the actual geometric information 
			///////////////////
			
			
			
			///////////////////////////////
			//  Shape file index file 
			String shxFileName = output + ".shx"; 
			RandomAccessFile shxFile = new RandomAccessFile(new File(shxFileName), "rw"); 
			
			shxFile.write(headerBig.array());
			shxFile.write(headerLittle.array()); 
			
			
			int fileSize = 100; 
			
			Bag geometries = field.getGeometry(); 
			TreeSet uniqueAttributes = new TreeSet(); 
			for (int i=0; i < geometries.size(); i++) { 
				GeomWrapper wrapper = (GeomWrapper) geometries.objs[i]; 
				String geomType = wrapper.getType(); 
				TreeMap attributes = wrapper.geoInfo.fields; 
				Set keys = attributes.keySet(); 
				Iterator iter = keys.iterator();
				while (iter.hasNext()) { 
					String key = (String) iter.next(); 
					uniqueAttributes.add(key); 
				}
				
				
				/////////
				// first store the record header, in big-endian format
				ByteBuffer recordHeader = ByteBuffer.allocate(8); 
				recordHeader.order(ByteOrder.BIG_ENDIAN); 
				
				// record number, 1-based 
				recordHeader.putInt(i+1); 
				
				// content size, 48 is from p8 of the ESRI shapefile spec
				int size = 20; 
				if (geomType == "LineString") { 
					LineString line = (LineString) wrapper.fetchGeometry();
					size = line.getCoordinates().length * 16 + 48;
				}
				else if (geomType == "Polygon") { 
					Polygon poly = (Polygon) wrapper.fetchGeometry();
					size = poly.getCoordinates().length * 16 + 48; 
				}
				else if (geomType == "MultiPolygon") { 
					MultiPolygon poly = (MultiPolygon)wrapper.fetchGeometry(); 
					size = poly.getCoordinates().length * 16 + 48; 
				}
				
				shxFile.writeInt(fileSize/2);
				shxFile.writeInt(size/2); 
				
				recordHeader.putInt(size/2); 
				shpFile.write(recordHeader.array()); 
				fileSize += 8 + size; 
				
				// now store the actual record information, in little-endian format
				if (geomType == "Point") { 
					ByteBuffer pointBufferLittle = ByteBuffer.allocate(20); 
					pointBufferLittle.order(ByteOrder.LITTLE_ENDIAN);
					
					// type of record
					pointBufferLittle.putInt(1);
					
					Point p = (Point)wrapper.fetchGeometry(); 
					pointBufferLittle.putDouble(p.getX()); 
					pointBufferLittle.putDouble(p.getY()); 
					
					shpFile.write(pointBufferLittle.array()); 
				}
				else { 
					Geometry g = wrapper.fetchGeometry(); 
					Coordinate coords[] = g.getCoordinates();  
					Envelope en = g.getEnvelopeInternal(); 
					
					ByteBuffer polyBufferLittle = ByteBuffer.allocate(size); 
					polyBufferLittle.order(ByteOrder.LITTLE_ENDIAN); 
					
					// record type, from spec
					if (geomType == "LineString")
						polyBufferLittle.putInt(3); 
					else 
						polyBufferLittle.putInt(5);
					
					// get the MBR
					polyBufferLittle.putDouble(en.getMinX()); polyBufferLittle.putDouble(en.getMaxX()); 
					polyBufferLittle.putDouble(en.getMinY()); polyBufferLittle.putDouble(en.getMaxY()); 
					
					// GeomImporter converts multi-* into single versions, so we only have one part
					polyBufferLittle.putInt(1); 
					
					// number of points 
					polyBufferLittle.putInt(g.getNumPoints()); 
					
					// start of the one and only part 
					polyBufferLittle.putInt(0); 
					
					for (int k=0; k < coords.length; k++) { 
						polyBufferLittle.putDouble(coords[k].x); 
						polyBufferLittle.putDouble(coords[k].y); 						
					} 
					shpFile.write(polyBufferLittle.array()); 	
				}
			}

			// file size is in number of 16-bit words, not bytes
			headerBig.putInt(24, fileSize/2); 
			shpFile.seek(0); 
			shpFile.write(headerBig.array()); 			
			shpFile.close();
			
			// now file size in the size of the .shx file, in number of 16-bit words
			shxFile.seek(0); 
			headerBig.putInt(24, (100 + 8*geometries.size())/2); 
			shxFile.write(headerBig.array()); 
			shxFile.close(); 
			
			//////////
			// now we need to save the attributed in XBase format, 
			// see http://www.clicketyclick.dk/databases/xbase/format/dbf.html#DBF_STRUCT
			String attrFileName = output + ".dbf"; 
			RandomAccessFile attrFile = new RandomAccessFile(new File(attrFileName), "rw"); 
			
			/////// 
			// xBase header 
			
			ByteBuffer dataBuff = ByteBuffer.allocate(32); 
			dataBuff.order(ByteOrder.LITTLE_ENDIAN);
			
			// version dBASE v. III - 5
			dataBuff.put((byte)0x03); 
			
			// today's date for last date of modification 
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
			String d = sdf.format(cal.getTime());
			dataBuff.put(Integer.valueOf(d.substring(0,2)).byteValue()); 
			dataBuff.put(Integer.valueOf(d.substring(2,4)).byteValue()); 
			dataBuff.put(Integer.valueOf(d.substring(4,6)).byteValue()); 
			
			// add number of records to dataBuff 
			dataBuff.putInt(geometries.size()); 
			
			// length of header structure, minus database container
			dataBuff.putShort((short)(32 + uniqueAttributes.size()*32 + 1)); 
						
			// length of individual records 
			GeomWrapper w = (GeomWrapper)geometries.objs[0]; 
			TreeMap attr = w.geoInfo.fields; 
			Set kys = attr.keySet(); 
			Iterator it = kys.iterator();
			int recordSize=0; 
			while (it.hasNext()) { 
				AttributeField f = (AttributeField)attr.get(it.next());
				recordSize += f.size; 
			}
			
			dataBuff.putShort((short)(1 + recordSize)); 
			
			// resevered 
			dataBuff.putShort((byte)0); 
			
			// incomplete transaction 
			dataBuff.put((byte)0); 
			
			// encyrption flag 
			dataBuff.put((byte)0); 
			
			// free record thread 
			dataBuff.putInt((byte)0); 
			
			// reserved 
			dataBuff.putDouble((byte)0); 
			
			// MDX flag
			dataBuff.put((byte)0); 
			
			// language driver 
			dataBuff.put((byte)0x01); 
			
			// reserved 
			dataBuff.putShort((byte)0); 
					
			attrFile.write(dataBuff.array()); 
			
			Iterator iter = uniqueAttributes.iterator(); 
			while (iter.hasNext()) { 
				ByteBuffer fieldBuff = ByteBuffer.allocate(32); 
				String key = (String)iter.next(); 
								
				for (int i=0; i < 11; i++) { 
					if (i >= key.length()) 
						fieldBuff.put((byte)0); 
					else 
						fieldBuff.put((byte)key.charAt(i)); 
				}
				
				// field type 
				w = (GeomWrapper)geometries.objs[0]; 
				attr = w.geoInfo.fields; 

				AttributeField f = (AttributeField)attr.get(key); 
				fieldBuff.put((byte)f.type); 
				
				// field data address 
				fieldBuff.putInt((byte)0); 
				
				//field length 
				fieldBuff.put((byte)f.size); 
				
				// decimal count 
				fieldBuff.put((byte)0); 
				
				// reserved 
				fieldBuff.putShort((byte)0); 
				
				// work area ID 
				fieldBuff.put((byte)1);
				
				// reserved 
				fieldBuff.putShort((byte)0); 
				
				// flag for SET FIELD 
				fieldBuff.put((byte)0); 
				
				// reserved 
				for (int i=0; i < 7; i++) fieldBuff.put((byte)0); 
				
				// index field flag
				fieldBuff.put((byte)0); 
				
				attrFile.write(fieldBuff.array()); 
			}
			
			// terminator 
			attrFile.write(0x0D); 
			
			/*
			 // 263 byte database container.  this is not written.  
			ByteBuffer database = ByteBuffer.allocate(263); 
			for (int i=0; i < 263; i++) database.put((byte)0); 
			attrFile.write(database.array()); 
			*/
			
			/////////
			// now write the individual records 
	
			for (int j=0; j < geometries.size(); j++) { 
				GeomWrapper wrapper = (GeomWrapper)geometries.objs[j]; 
				TreeMap attributes = wrapper.geoInfo.fields; 
				Set keys = attributes.keySet(); 
				Iterator iter1 = keys.iterator();
				ByteBuffer recordBuff = ByteBuffer.allocate(1+recordSize); 
				recordBuff.put((byte)0x20);
								
				while (iter1.hasNext()) { 
					AttributeField f = (AttributeField)attributes.get(iter1.next()); 
					StringBuffer value = new StringBuffer(String.valueOf(f.value)); 
					int add = f.size - value.length(); 
					for (int i=0; i < add; i++) 
						value.insert(0, ' '); 
					
					for (int i=0; i < f.size; i++ ) 
						recordBuff.put((byte)value.charAt(i));
				}
				attrFile.write(recordBuff.array()); 
			}
			attrFile.close(); 
			
			 
		} catch (Exception ex) { 
			System.out.println("Error in MasonExporter:write: "); 
			ex.printStackTrace(); 
		} 
	}
}
