/*
 Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 George Mason University Mason University Licensed under the Academic
 Free License version 3.0

 See the file "LICENSE" for more information
 */
package sim.io.geo;

import com.vividsolutions.jts.geom.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.MasonGeometry;



/** Writes a GeomVectorField to a Shape file.
 *
 */
public class ShapeFileExporter //extends GeomExporter
{

    /** Write the given vector field to a shape file.
     * <p>
     * The three mandatory shape files (".shp", ".shx", and ".dbf") will be
     * created using the given base file name prefix.
     * 
     * @param baseFileName is the prefix for the ".shp", ".shx", and ".dbf" files
     * @param field to be exported
     * @throws FileNotFoundException
     */
    public static void write(String baseFileName, GeomVectorField field) //throws FileNotFoundException
    {
        try
        {
            // First write the geometry
            String shpFileName = baseFileName + ".shp";
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
            for (int i = 0; i < 5; i++)
            {
                headerBig.putInt(0);
            }

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
            headerLittle.putDouble(e.getMinX());
            headerLittle.putDouble(e.getMinY());
            headerLittle.putDouble(e.getMaxX());
            headerLittle.putDouble(e.getMaxY());

            // bytes 68 - 83 are range of Z in min z, max z (double) 
            // current not used, so put artitrary values 
            headerLittle.putDouble(0.0);
            headerLittle.putDouble(0.0);

            // bytes 84 - 99 are range of M in min m, max m (double)
            // current not used, so put artitrary values 
            headerLittle.putDouble(0.0);
            headerLittle.putDouble(0.0);

            // write the header 
            shpFile.write(headerLittle.array());


            //////////////////
            // the shapefile now contains an arbitrary number of records (each of arbitrary 
            // length), where each record
            // consists of a header, followed by the actual geometric information 
            ///////////////////



            ///////////////////////////////
            //  Shape file index file 
            String shxFileName = baseFileName + ".shx";
            RandomAccessFile shxFile = new RandomAccessFile(new File(shxFileName), "rw");

            // The index file contain the same 100-byte header as found in the
            // corresponding .shp file.
            shxFile.write(headerBig.array());
            shxFile.write(headerLittle.array());


            // Initialized to size of header; this will be incremented by
            // record size for each record.
            int fileSize = 100; 

            Bag geometries = field.getGeometries();

            //TreeSet<String> uniqueAttributes = new TreeSet<String>();

            for (int i = 0; i < geometries.size(); i++)
            {
                MasonGeometry wrapper = (MasonGeometry) geometries.objs[i];
                String geomType = wrapper.toString();

//                ArrayList<AttributeValue> attributes = (ArrayList<AttributeValue>) wrapper.geometry.getUserData();
//
//                for (int j = 0; j < attributes.size(); j++)
//                {
//                    uniqueAttributes.add(attributes.get(j).getName());
//                }
//
                // Add any new attribute names that are associated with this
                // geometry instance.
                //uniqueAttributes.addAll(wrapper.getAttributes().keySet());

                /////////
                // first store the record header, in big-endian format
                ByteBuffer recordHeader = ByteBuffer.allocate(8);
                recordHeader.order(ByteOrder.BIG_ENDIAN);

                // record number, 1-based 
                recordHeader.putInt(i + 1);

                // content size, 48 is from p8 of the ESRI shapefile spec
                int size = 20;
                if ("LineString".equals(geomType))
                {
                    LineString line = (LineString) wrapper.getGeometry();
                    size = line.getCoordinates().length * 16 + 48;
                } else if ("Polygon".equals(geomType))
                {
                    Polygon poly = (Polygon) wrapper.getGeometry();
                    size = poly.getCoordinates().length * 16 + 48;
                } else if ("MultiPolygon".equals(geomType))
                {
                    MultiPolygon poly = (MultiPolygon) wrapper.getGeometry();
                    size = poly.getCoordinates().length * 16 + 48;
                }

                shxFile.writeInt(fileSize / 2);
                shxFile.writeInt(size / 2);

                recordHeader.putInt(size / 2);
                shpFile.write(recordHeader.array());
                fileSize += 8 + size;

                // now store the actual record information, in little-endian format
                if ("Point".equals(geomType))
                {
                    ByteBuffer pointBufferLittle = ByteBuffer.allocate(20);
                    pointBufferLittle.order(ByteOrder.LITTLE_ENDIAN);

                    // type of record
                    pointBufferLittle.putInt(1);

                    Point p = (Point) wrapper.getGeometry();
                    pointBufferLittle.putDouble(p.getX());
                    pointBufferLittle.putDouble(p.getY());

                    shpFile.write(pointBufferLittle.array());
                } else
                {
                    Geometry g = wrapper.getGeometry();
                    Coordinate coords[] = g.getCoordinates();
                    Envelope en = g.getEnvelopeInternal();

                    ByteBuffer polyBufferLittle = ByteBuffer.allocate(size);
                    polyBufferLittle.order(ByteOrder.LITTLE_ENDIAN);

                    // record type, from spec
                    if ("LineString".equals(geomType))
                    {
                        polyBufferLittle.putInt(3);
                    } else
                    {
                        polyBufferLittle.putInt(5);
                    }

                    // get the MBR
                    polyBufferLittle.putDouble(en.getMinX());
                    polyBufferLittle.putDouble(en.getMaxX());
                    polyBufferLittle.putDouble(en.getMinY());
                    polyBufferLittle.putDouble(en.getMaxY());

                    // GeomImporter converts multi-* into single versions, so we only have one part
                    polyBufferLittle.putInt(1);

                    // number of points 
                    polyBufferLittle.putInt(g.getNumPoints());

                    // start of the one and only part 
                    polyBufferLittle.putInt(0);

                    for (int k = 0; k < coords.length; k++)
                    {
                        polyBufferLittle.putDouble(coords[k].x);
                        polyBufferLittle.putDouble(coords[k].y);
                    }
                    shpFile.write(polyBufferLittle.array());
                }
            }

            // file size is in number of 16-bit words, not bytes
            headerBig.putInt(24, fileSize / 2);
            shpFile.seek(0);
            shpFile.write(headerBig.array());
            shpFile.close();

            // now file size in the size of the .shx file, in number of 16-bit words
            shxFile.seek(0);
            headerBig.putInt(24, (100 + 8 * geometries.size()) / 2);
            shxFile.write(headerBig.array());
            shxFile.close();

            //////////
            // now we need to save the attributes in XBase format,
            // see http://www.clicketyclick.dk/databases/xbase/format/dbf.html#DBF_STRUCT
            String attrFileName = baseFileName + ".dbf";
            RandomAccessFile attrFile = new RandomAccessFile(new File(attrFileName), "rw");

            /////// 
            // xBase header 

            ByteBuffer headerBuffer = ByteBuffer.allocate(32);
            headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // version dBASE v. III - 5
            headerBuffer.put((byte) 0x03);

            // today's date for last date of modification 
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
            String d = sdf.format(cal.getTime());
            headerBuffer.put(Integer.valueOf(d.substring(0, 2)).byteValue());
            headerBuffer.put(Integer.valueOf(d.substring(2, 4)).byteValue());
            headerBuffer.put(Integer.valueOf(d.substring(4, 6)).byteValue());

            // add number of records to dataBuff 
            headerBuffer.putInt(geometries.size());

            // length of header structure, minus database container
            // (Presuming all the geometries have the same attribute sets, we
            // can arbitrarily pick the first geometry and ask the number of
            // attributes it has.)
            int numAttributes = ((MasonGeometry)geometries.objs[0]).getAttributes().size();

            headerBuffer.putShort((short) (32 + numAttributes * 32 + 1));

            // This associates the storage needed for each attribute.  We need
            // this to calculate the total space taken up for each record.
            Map<String,Integer> attributeSizes = determineAttributeSizes(geometries);

            // Bytes 10 and 11 is the record length.  Since the attribute record
            // structure is the same for all attribute records, we calculate this by arbitrarily
            // taking the first attribute record and summing all its constitutent
            // attributes.
            // FIXME this doesn't work any more; need to change to new attribute model!!
//            MasonGeometry w = (MasonGeometry) geometries.objs[0];
//            ArrayList<AttributeValue> attrs = (ArrayList<AttributeValue>) w.geometry.getUserData();

            int recordSize = 0;
//            for (int i = 0; i < attrs.size(); i++)
//            {
//                recordSize += getBytes(attrs.get(i)).length;
//            }

            for (String attributeName : attributeSizes.keySet())
            {
                recordSize += attributeSizes.get(attributeName);
            }

            headerBuffer.putShort((short) (1 + recordSize));

            // reserved
            headerBuffer.putShort((byte) 0);

            // incomplete transaction 
            headerBuffer.put((byte) 0);

            // encryption flag
            headerBuffer.put((byte) 0);

            // free record thread 
            headerBuffer.putInt((byte) 0);

            // reserved 
            headerBuffer.putDouble((byte) 0);

            // MDX flag
            headerBuffer.put((byte) 0);

            // language driver 
            headerBuffer.put((byte) 0x01);

            // reserved 
            headerBuffer.putShort((byte) 0);

            attrFile.write(headerBuffer.array());

            // Now write out the field descriptor array, which describes each
            // attribute type.

//            Iterator<String> iter = uniqueAttributes.iterator();
//            while (iter.hasNext())
            for( String key : attributeSizes.keySet() )
            {
                ByteBuffer fieldDescriptorArrayBuffer = ByteBuffer.allocate(32);

//                String key = iter.next();

                // Write out the field name, and pad it out with zeroes up
                // to byte 10
                for (int i = 0; i < 11; i++)
                {
                    if (i >= key.length())
                    {
                        fieldDescriptorArrayBuffer.put((byte) 0);
                    } else
                    {
                        fieldDescriptorArrayBuffer.put((byte) key.charAt(i));
                    }
                }

                // write out the field type; we do this by arbitrarily grabbing
                // the first record, finding the current attribute for which
                // we want the type, identifying the type, and then writing
                // that out
                MasonGeometry w = (MasonGeometry) geometries.objs[0];
// DEPRECATED means of doing this
//                ArrayList<AttributeValue> attr = (ArrayList<AttributeValue>) w.geometry.getUserData();
//
//                AttributeValue f = null;
//                for (int i = 0; i < attr.size(); i++)
//                {
//                    f = attr.get(i);
//                    if (f.getName().equals(key))
//                    {
//                        break;
//                    }
//                }

                // Directly get the attribute value
                AttributeValue value = (AttributeValue) w.getAttribute(key);

                // And then ask what type it is
                if (value.getValue() instanceof String)
                {
                    fieldDescriptorArrayBuffer.put((byte) 'C');
                } else if (value.getValue() instanceof Integer)
                {
                    fieldDescriptorArrayBuffer.put((byte) 'N');
                }
                else if (value.getValue() instanceof Double)
                {
                    // Note that we don't write 'F' as that's a Dbase IV
                    // artifact.  The 'N'umeric type is adequate for
                    // floating point values.
                    fieldDescriptorArrayBuffer.put((byte) 'N');
                } else if (value.getValue() instanceof Boolean)
                {
                    fieldDescriptorArrayBuffer.put((byte) 'L');
                }


                // field data address 
                fieldDescriptorArrayBuffer.putInt((byte) 0);

                //field length
                // TODO Keith read in length; how do I compute this?
//                fieldDescriptorArrayBuffer.put((byte) getBytes(f.getValue()).length);
                fieldDescriptorArrayBuffer.put(attributeSizes.get(key).byteValue());

                // decimal count 
                fieldDescriptorArrayBuffer.put((byte) 0);

                // reserved 
                fieldDescriptorArrayBuffer.putShort((byte) 0);

                // work area ID 
                fieldDescriptorArrayBuffer.put((byte) 1);

                // reserved 
                fieldDescriptorArrayBuffer.putShort((byte) 0);

                // flag for SET FIELD 
                fieldDescriptorArrayBuffer.put((byte) 0);

                // reserved 
                for (int i = 0; i < 7; i++)
                {
                    fieldDescriptorArrayBuffer.put((byte) 0);
                }

                // index field flag
                fieldDescriptorArrayBuffer.put((byte) 0);

                attrFile.write(fieldDescriptorArrayBuffer.array());
            }

            // terminator 
            attrFile.write(0x0D);

            /*
             * // 263 byte database container. this is not written. ByteBuffer
             * database = ByteBuffer.allocate(263); for (int i=0; i < 263; i++)
             * database.put((byte)0); attrFile.write(database.array());
             */

            /////////
            // now write the individual records 

            for (int j = 0; j < geometries.size(); j++)
            {
                MasonGeometry wrapper = (MasonGeometry) geometries.objs[j];

                ByteBuffer recordBuff = ByteBuffer.allocate(1 + recordSize);

                // 0x20 in the first byte indicates that this record is valid.
                // (I.e., it hasn't been deleted.)
                recordBuff.put((byte) 0x20);

                // DEPRECATED, old way
//                ArrayList<AttributeValue> attributes = (ArrayList<AttributeValue>) wrapper.geometry.getUserData();

                for (String attributeName : attributeSizes.keySet())
                {
                    AttributeValue f = (AttributeValue) wrapper.getAttribute(attributeName);

                    Object value = f.getValue();

                    // DEPRECATED, old way
//                    StringBuilder value = new StringBuilder(String.valueOf(f.getValue()));

                    if (value instanceof Boolean)
                    {
                        Boolean truthiness = (Boolean) value;

                        if (truthiness)
                        {
                            recordBuff.putChar('T');
                        }
                        else
                        {
                            recordBuff.putChar('F');
                        }
                    }
                    else
                    {
                        byte [] rawValue = value.toString().getBytes("US-ASCII");

                        byte [] outValue = new byte [attributeSizes.get(attributeName)];

                        // pad with blanks
                        Arrays.fill(outValue,(byte)0x20);

                        System.arraycopy(rawValue, 0, outValue, 0, rawValue.length);

                        // Pad the field with blanks so that it meets its field
                        // size.
//                        int padding = attributeSizes.get(attributeName) - rawValue.length;
//
//                        assert padding >= 0 : "padding: " + padding;
//
//                        for (int k = 0; k < padding; k++)
//                        {
////                        value.insert(0, ' '); DEPRECATED
//                            recordBuff.putChar(' ');
//                        }
                        
                        recordBuff.put(outValue);
                    }

// DEPRECATED, old way
//                    for (int k = 0; k < f.getFieldSize(); k++)
//                    {
//                        recordBuff.put((byte) value.charAt(k));
//                    }
                }

                attrFile.write(recordBuff.array());
            }
            attrFile.close();

        } catch (Exception ex)
        {
            System.out.println("Error in ShapeFileExporter:write: ");
            ex.printStackTrace();
        }
    }



    /** Convert the given object into a byte stream.
     *
     * @param obj
     * @return byte array containing object
     */
    private static byte[] getBytes(Object obj)
    {
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            bos.close();
            byte[] data = bos.toByteArray();
            return data;
        } catch (IOException e)
        {
            System.out.println(e);
        }
        return null;
    }



    /** Calculate the space needed for each attribute type
     * 
     * @param geometries through which we'll be scanning
     *
     * @return map of attribute name to its respective size requirements
     */
    private static Map<String, Integer> determineAttributeSizes(Bag geometries)
    {
        Map<String,Integer> attributeSizes = new HashMap<String,Integer>();

        for (int i = 0; i < geometries.size(); i++)
        {
            MasonGeometry mg = (MasonGeometry) geometries.objs[i];

            // Update the attribute sizes by iterating through all the attributes
            // and taking their string conversion lengths as their sizes; if the
            // stored size for that attribute is smaller, then update that size
            // with the larger.
            // TODO enforce 256 and 18 upper bound for lengths for strings and
            // numeric values.
            for ( String attributeName : mg.getAttributes().keySet() )
            {
                Integer attributeSize = null;
                
                try
                {
                    Object av = mg.getAttribute(attributeName);

                    if (av instanceof Boolean)
                    {
                        attributeSize = 1;
                    }
                    else
                    {
                        attributeSize = mg.getAttribute(attributeName).toString().getBytes("US-ASCII").length;
                    }
                } catch (UnsupportedEncodingException ex)
                {
                    Logger.getLogger(ShapeFileExporter.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (attributeSizes.containsKey(attributeName))
                {
                    Integer storedSize = attributeSizes.get(attributeName);

                    if (storedSize < attributeSize)
                    {
                        attributeSizes.put(attributeName, attributeSize);
                    }
                }
                else
                {
                    attributeSizes.put(attributeName, attributeSize);
                }
            }

        }

        return attributeSizes;
    }


}
