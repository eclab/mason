package sim.util;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import sun.awt.image.*;
import javax.swing.*;

/****

     TABLELOADER.java

     <p>This class provides utility methods for loading tables from files into int[][] or double[][] arrays.  In MASON
     it's particularly useful for loading files of numbers or graphics into an IntGrid2D or DoubleGrid2D to display.

     <p>TableLoader at present can load the following kinds of files:

     <P><ul>
     <li>"Plain" PBM files.  These store binary (1 and 0) bitmaps in a text-readable format.
     <li>"Raw" PBM files.  These store binary (1 and 0) bitmaps in a somewhat more compressed binary format.
     <li>"Plain" PGM files.  These store grayscale images (from 0 to under some MAXVAL you can define) in a text-readable format.
     <li>"Raw" PGM files.  These store grayscale images (from 0 to under some MAXVAL you can define) in a somewhat more compressed binary format.
     <li>PNG and GIF files which store binary, 8-bit grayscale, or 8-bit indexed data.
     <li>Whitespace-delimited text files.  These store each array row as a single line of numbers.  The numbers are set off with spaces or tabs.
     </ul>

     <p><b>PBM And PGM ("PNM" files)</b>&nbsp;&nbsp;&nbsp;&nbsp;The first four formats are defined by the <a href="http://netpbm.sourceforge.net/doc/">Netpbm</a>
     file format specification.  Various graphics programs can emit PBM or PGM files.  These files are collectively known as <b>PNM files</b>.
     MASON reads these files into int[][] arrays.   Note that
     graphics programs typically emit PBM (black and white) files in the opposite numerical format than you'd expect: 1 is black, and 0 is white.
     On the other hand, PGM (monochrome) files are emitted however your graphics program sees fit, typically with 0 being black and MAXVAL-1, whatever
     you've set it to, being white.  

     <p>If you're constructing these files by hand, note that MASON is a bit more generous about plain formats than
     the specification allows: MASON permits lines of any length, and you can have a MAXVAL of any size you like, as long as its within the integer
     data type range (normally, PGM only allows lines of about 70 chars and a MAXVAL of no more than 2^16).

     <p><b>PNG and GIF files</b>&nbsp;&nbsp;&nbsp;&nbsp;These files must have colors stored as binary (black and white), 8-bit grayscale,
     or 8-bit indexed color.

     <p><b>Whitespace-delimited text files</b>nbsp;&nbsp;&nbsp;&nbsp;These files consist of rows of numbers, each row delimited with newlines.  
     The numbers in each row are delimited with spaces or tabs.  Unlike the PBM/PGM format, you cannot at present have comments in the file.  The
     files are loaded into double[][] arrays, though TableLoader provides a simple utility conversion function to int[][] if you're sure that all
     the values are actually integers and would like an int array.

     <P>MASON determines the row width of the first row by parsing through the first line.  Thereafter it checks to make sure that all subsequent
     rows are the same width (in terms of number of elements) and thus that the int[][] array is rectangular.
*/


public class TableLoader
    {
    // tokenizes a string for PBM and PGM headers and plain PBM and PGM data
    static String tokenizeString(InputStream stream) throws IOException { return tokenizeString(stream, false); }

    // tokenizes a string for PBM and PGM headers and plain PBM and PGM data.  If oneChar is true, then
    // rather than parsing a whole string, a single character is read (not including whitespace or comments)
    static String tokenizeString(InputStream stream, boolean oneChar) throws IOException
        {
        final int EOF = -1;
    
        StringBuilder b = new StringBuilder();
        int c;
        boolean inComment = false;
        
        while(true)
            {
            c = stream.read();
            if (c == EOF) throw new IOException("Stream ended prematurely, before table reading was completed.");  // no more tokens
            else if (inComment)
                {
                if (c=='\r' || c == '\n')  // escape the comment
                    inComment = false;
                else {} // do nothing
                }
            else if (Character.isWhitespace((char)c))
                { } // do nothing
            else if (c == '#')  // start of a comment
                { inComment = true; }
            else // start of a string
                { b.append((char)c); break; }
            }
        
        if (oneChar) return b.toString();
        
        // at this point we have a valid string.  We read until whitespace or a #
        while(true)
            {
            c = stream.read();
            if (c == EOF) break;
            else if (c == '#')  // start of comment, read until a '\n'
                {
                while(true)
                    {
                    c = stream.read();  // could hit EOF, which is fine
                    if (c == EOF) break;
                    else if (c == '\r' || c == '\n') break;
                    }
                //break;   // comments are not delimiters
                }
            else if (Character.isWhitespace((char)c))
                break;
            else b.append((char)c);
            }
        return b.toString();
        }
    

    // tokenizes an integer for PBM and PGM headers and plain PBM and PGM data.
    static int tokenizeInt(InputStream stream) throws IOException
        {
        return Integer.parseInt(tokenizeString(stream));
        }
    
    /** Loads plain or raw PGM files or plain or raw PBM files and return the result as an int[][].  
        If flipY is true, then the Y dimension is flipped. */
    public static int[][] loadPNMFile(InputStream str, boolean flipY) throws IOException
        {
        int[][] vals = loadPNMFile(str);
        if (flipY)
            {
            for(int i = 0 ; i < vals.length; i++)
                {
                int height = vals[i].length;
                for(int j = 0; j < height/2; j++)
                    {
                    int temp = vals[i][j];
                    vals[i][j] = vals[i][height-j+1];
                    vals[i][height-j+1] = temp;
                    }
                }
            }
        return vals;
        }

    /** Loads plain or raw PGM files or plain or raw PBM files and return the result as an int[][].
        The Y dimension is not flipped. */
    public static int[][] loadPNMFile(InputStream str) throws IOException
        {
        BufferedInputStream stream = new BufferedInputStream(str);
        String type = tokenizeString(stream);
        if (type.equals("P1"))
            return loadPlainPBM(stream);
        else if (type.equals("P2"))
            return loadPlainPGM(stream);
        else if (type.equals("P4"))
            return loadRawPBM(stream);
        else if (type.equals("P5"))
            return loadRawPGM(stream);
        else throw new IOException("Not a viable PBM or PGM stream");
        }


    // Loads plain PGM files after the first-line header is stripped
    static int[][] loadPlainPGM(InputStream stream) throws IOException
        {
        int width = tokenizeInt(stream);
        int height = tokenizeInt(stream);
        int maxGray = tokenizeInt(stream);
        if (width < 0) throw new IOException("Invalid width in PGM: " + width);
        if (height < 0) throw new IOException("Invalid height in PGM: " + height);
        if (maxGray <= 0) throw new IOException("Invalid maximum value in PGM: " + maxGray);
        
        int[][] field = new int[width][height];
        for(int i = 0; i < height; i++)
            for(int j = 0; j < width; j++)
                field[j][i] = tokenizeInt(stream);
                
        return field;
        }
    

    // Loads raw PGM files after the first-line header is stripped
    static int[][] loadRawPGM(InputStream stream) throws IOException
        {
        int width = tokenizeInt(stream);
        int height = tokenizeInt(stream);
        int maxVal = tokenizeInt(stream);
        if (width < 0) throw new IOException("Invalid width: " + width);
        if (height < 0) throw new IOException("Invalid height: " + height);
        if (maxVal <= 0) throw new IOException("Invalid maximum value: " + maxVal);
        
        // this single whitespace character will have likely already been consumed by reading maxVal
        //stream.read();  // must be a whitespace
        
        int[][] field = new int[width][height];
        for(int i = 0; i < height; i++)
            for(int j = 0; j < width; j++)
                {
                if (maxVal < 256) // one byte
                    field[j][i] = stream.read();
                else if (maxVal < 65536) // two bytes
                    field[j][i] = (stream.read() << 8) & stream.read();  // two bytes, most significant byte first
                else if (maxVal < 16777216) // three bytes -- this is nonstandard
                    field[j][i] = (stream.read() << 16) & (stream.read() << 8) & stream.read();  // three bytes, most significant byte first
                else // four bytes -- this is nonstandard
                    field[j][i] = (stream.read() << 24) & (stream.read() << 16) & (stream.read() << 8) & stream.read();  // three bytes, most significant byte first
                }
        
        return field;
        }

    // Loads plain PBM files after the first-line header is stripped
    static int[][] loadPlainPBM(InputStream stream) throws IOException
        {
        int width = tokenizeInt(stream);
        int height = tokenizeInt(stream);
        if (width < 0) throw new IOException("Invalid width in PBM: " + width);
        if (height < 0) throw new IOException("Invalid height in PBM: " + height);
        
        int[][] field = new int[width][height];
        for(int i = 0; i < height; i++)
            for(int j = 0; j < width; j++)
                {
                String s = tokenizeString(stream, true);
                if (s.equals("0")) field[j][i] = 0;
                else if (s.equals("1")) field[j][i] = 1;
                else throw new IOException("Invalid byte data in PBM");
                }

        return field;
        }

    // Loads raw PBM files after the first-line header is stripped
    static int[][] loadRawPBM(InputStream stream) throws IOException
        {
        int width = tokenizeInt(stream);
        int height = tokenizeInt(stream);
        if (width < 0) throw new IOException("Invalid width in PBM: " + width);
        if (height < 0) throw new IOException("Invalid height in PBM: " + height);
        
        // this single whitespace character will have likely already been consumed by reading height
        //stream.read();  // must be a whitespace

        int[][] field = new int[width][height];
        for(int i = 0; i < height; i++)
            {
            int data = 0;
            int count = 0;
            for(int j = 0; j < width; j++)
                {
                if (count == 0) { data = stream.read(); count = 8; }
                count--;
                field[j][i] = (data >> count) & 0x1;
                }
            }
                
        return field;
        }


    /** Loads into a double[][] a plain text file of numbers, with newlines dividing the numbers into rows and tabs or spaces delimiting columns. 
        If flipY is true, then the Y dimension is flipped. */
    public static double[][] loadTextFile(InputStream str, boolean flipY) throws RuntimeException, IOException
        {
        double[][] vals = loadTextFile(str);
        if (flipY)  // do the flip
            {
            for(int i = 0 ; i < vals.length; i++)
                {
                int height = vals[i].length;
                for(int j = 0; j < height/2; j++)
                    {
                    double temp = vals[i][j];
                    vals[i][j] = vals[i][height-j+1];
                    vals[i][height-j+1] = temp;
                    }
                }
            }
        return vals;
        }

    /** Loads into a double[][] a plain text file of numbers, with newlines dividing the numbers into rows and tabs or spaces delimiting columns. 
        The Y dimension is not flipped. */
    public static double[][] loadTextFile(InputStream stream) throws IOException
        {
        Scanner scan = new Scanner(stream);
        
        ArrayList rows = new ArrayList();
        int width = -1;
        
        while(scan.hasNextLine())
            {
            String srow = scan.nextLine().trim();
            if (srow.length() > 0)
                {
                int w = 0;
                if (width == -1)  // first time compute width
                    {
                    ArrayList firstRow = new ArrayList();
                    Scanner rowScan = new Scanner(new StringReader(srow));
                    while(rowScan.hasNextDouble())
                        {
                        firstRow.add(new Double(rowScan.nextDouble()));  // ugh, boxed
                        w++;
                        }
                    width = w;
                    double[] row = new double[width];
                    for(int i = 0; i < width; i++)
                        row[i] = ((Double)(firstRow.get(i))).doubleValue();
                    rows.add(row);
                    }
                else
                    {
                    double[] row = new double[width];
                    Scanner rowScan = new Scanner(new StringReader(srow));
                    while(rowScan.hasNextDouble())
                        {
                        if (w == width)  // uh oh
                            throw new IOException("Row lengths do not match in text file");
                        row[w] = rowScan.nextDouble();
                        w++;
                        }
                    if (w < width)  // uh oh
                        throw new IOException("Row lengths do not match in text file");
                    rows.add(row);
                    }
                }
            }
            
        if (width == -1)  // got nothing
            return new double[0][0];
        
        double[][] fieldTransposed = new double[rows.size()][];
        for(int i = 0; i < rows.size(); i++)
            fieldTransposed[i] = ((double[])(rows.get(i)));
            
        // now transpose because we have width first
        double[][] field = new double[width][fieldTransposed.length];
        for(int i = 0; i < field.length; i++)
            for(int j = 0 ; j < field[i].length; j++)
                field[i][j] = fieldTransposed[j][i];
        
        return field;
        }



    /** Loads GIF files and returns the result as an int[][], where each integer value represents
        the color table index of the pixel.  If flipY is true, then the Y dimension is flipped. */
    public static int[][] loadGIFFile(InputStream str, boolean flipY) throws IOException
        {
        return loadPNGFile(str, flipY);
        }

    /** Loads GIF files and returns the result as an int[][], where each integer value represents
        the color table index of the pixel.  The Y dimension is not flipped. */
    public static int[][] loadGIFFile(InputStream str) throws IOException
        {
        return loadPNGFile(str);
        }

    /** Loads PNG files and returns the result as an int[][].  The only PNG formats permitted are those
        with up to 256 grays (including simple black and white) or indexed colors from an up to 
        256-sized color table.  Each integer value represents the gray level or the color table index
        value of the pixel.  flipY is true, then the Y dimension is flipped. */
    public static int[][] loadPNGFile(InputStream str, boolean flipY) throws IOException
        {
        int[][] vals = loadPNGFile(str);
        if (flipY)
            {
            for(int i = 0 ; i < vals.length; i++)
                {
                int height = vals[i].length;
                for(int j = 0; j < height/2; j++)
                    {
                    int temp = vals[i][j];
                    vals[i][j] = vals[i][height-j+1];
                    vals[i][height-j+1] = temp;
                    }
                }
            }
        return vals;
        }
        
    /** Loads PNG files and returns the result as an int[][].  The only PNG formats permitted are those
        with up to 256 grays (including simple black and white) or indexed colors from an up to 
        256-sized color table.  Each integer value represents the gray level or the color table index
        value of the pixel.  The Y dimension is not flipped.  */
    public static int[][] loadPNGFile(InputStream str) throws IOException
        {
        // read the bytes into a byte array
        BufferedInputStream stream = new BufferedInputStream(str);
        ArrayList list = new ArrayList();
        int count = 0;
        while(true)
            {
            byte[] buffer = new byte[16384 * 16];
            int len = stream.read(buffer);
            if (len <= 0) // all done
                break;
            else if (len < buffer.length)
                {
                byte[] buf2 = new byte[len];
                System.arraycopy(buffer, 0, buf2, 0, len);
                buffer = buf2;
                }
            count += len;
            list.add(buffer);
            }
        byte[] data = new byte[count];
        int cur = 0;
        for(int i = 0; i < list.size(); i++)
            {
            byte[] b = (byte[])(list.get(i));
            System.arraycopy(b, 0, data, cur, b.length);
            cur += b.length;
            }
            
        // This creates a deprecation warning in 1.7, but it's important so we're keeping it...

        // Next convert the byte array to a buffered image
        BufferedImage image = ((ToolkitImage)(new ImageIcon(data).getImage())).getBufferedImage();
        
        // Is the color model something we can use?
        int type = image.getType();
        if (type == BufferedImage.TYPE_BYTE_BINARY || type == BufferedImage.TYPE_BYTE_GRAY)
            {
            int w = image.getWidth();
            int h = image.getHeight();
            int[][] result = new int[w][h];
            // obviously this could be done more efficiently
            for(int i = 0; i < w; i++)
                for(int j = 0; j < h; j ++)
                    result[i][j] = (image.getRGB(i,j) & 0xFF);
            return result;
            }
        else if (type == BufferedImage.TYPE_BYTE_INDEXED)
            {
            Raster raster = image.getRaster();
            if (raster.getTransferType() != DataBuffer.TYPE_BYTE)  // uh oh
                throw new IOException ("Input Stream must contain an image with byte data if indexed.");
            byte[] pixel = new byte[1];
            int w = image.getWidth();
            int h = image.getHeight();
            int[][] result = new int[w][h];
            // obviously this could be done more efficiently
            for(int i = 0; i < w; i++)
                for(int j = 0; j < h; j ++)
                    {
                    result[i][j] = ((byte[])(raster.getDataElements(i,j,pixel)))[0];
                    if (result[i][j] < 0) result[i][j] += 256;
                    }
            return result;
            }
        //else if (type == TYPE_USHORT_GRAY)   // at present we don't handle shorts
        //    {
        //    }
        else throw new IOException("Input Stream must contain a binary, byte-sized grayscale, or byte-sized indexed color scheme: " + image);
        }


    /** Converts a double[][] array to an int[][] array only if all values are within the int range.
        If not, returns null. */
    public static int[][] convertToIntArray(double[][] vals)
        {
        int[][] ret = new int[vals.length][];
        for(int i = 0; i < vals.length; i++)
            {
            double[] valsi = vals[i];
            int[] reti = ret[i] = new int[valsi.length];
            for(int j = 0; j < valsi.length; j++)
                {
                int a = (int)valsi[j];
                if (a == valsi[j])
                    reti[j] = a;
                else return null;
                }
            }
        return ret;
        }

    /** Converts an int[][] array to a double[][] array. */
    public static double[][] convertToDoubleArray(int[][] vals)
        {
        double[][] ret = new double[vals.length][];
        for(int i = 0; i < vals.length; i++)
            {
            int[] valsi = vals[i];
            double[] reti = ret[i] = new double[valsi.length];
            for(int j = 0; j < valsi.length; j++)
                reti[j] = valsi[j];
            }
        return ret;
        }

    /** Converts a double[][] array to a long[][] array only if all values are within the long range.
        If not, returns null. */
    public static long[][] convertToLongArray(double[][] vals)
        {
        long[][] ret = new long[vals.length][];
        for(int i = 0; i < vals.length; i++)
            {
            double[] valsi = vals[i];
            long[] reti = ret[i] = new long[valsi.length];
            for(int j = 0; j < valsi.length; j++)
                {
                long a = (long)valsi[j];
                if (a == valsi[j])
                    reti[j] = a;
                else return null;
                }
            }
        return ret;
        }

    /** Converts an int[][] array to a long[][] array. */
    public static long[][] convertToLongArray(int[][] vals)
        {
        long[][] ret = new long[vals.length][];
        for(int i = 0; i < vals.length; i++)
            {
            int[] valsi = vals[i];
            long[] reti = ret[i] = new long[valsi.length];
            for(int j = 0; j < valsi.length; j++)
                reti[j] = valsi[j];
            }
        return ret;
        }
    }