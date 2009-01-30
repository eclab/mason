/*
  Copyright 2000 by J. David Eisenberg 
  Distributed under the Artistic License.
  This license appears at the end of the file.
*/

package sim.util.media;
import java.awt.*;
import java.awt.image.*;
import java.util.zip.*;
import java.io.*;

/** PngEncoder takes a Java Image object and creates a byte string which can be saved as a PNG file.
 * The Image is presumed to use the DirectColorModel.
 * 
 * <p>This code is taken, with permission, from J. David Eisenberg (david@catcode.com), and is 
 * distributed under the Artistic License.
 */
 
/* 
 * Thanks to Jay Denny at KeyPoint Software
 *    http://www.keypoint.com/
 * who let me develop this code on company time.
 *
 * You may contact me with (probably very-much-needed) improvements,
 * comments, and bug fixes at:
 *
 *   david@catcode.com
 *
 * This library is distributed under the Artistic License, found at
 * the end of this file.
 *
 * @author J. David Eisenberg
 * @version 1.4, 31 March 2000
 */

// NOTE -- change in the package declaration above (per Artistic License) -- Sean

public class PngEncoder extends Object
    {
    /** Constant specifying that alpha channel should be encoded. */
    public static final boolean ENCODE_ALPHA=true;
    /** Constant specifying that alpha channel should not be encoded. */
    public static final boolean NO_ALPHA=false;
    /** Constants for filters */
    public static final int FILTER_NONE = 0;
    public static final int FILTER_SUB = 1;
    public static final int FILTER_UP = 2;
    public static final int FILTER_LAST = 2;

    protected byte[] pngBytes;
    protected byte[] priorRow;
    protected byte[] leftBytes;
    protected Image image;
    protected int width, height;
    protected int bytePos, maxPos;
    protected int hdrPos, dataPos, endPos;
    protected CRC32 crc = new CRC32();
    protected long crcValue;
    protected boolean encodeAlpha;
    protected int filter;
    protected int bytesPerPixel;
    protected int compressionLevel;

    /**
     * Class constructor
     *
     */
    public PngEncoder()
        {
        this( null, false, FILTER_NONE, 0 );
        }

    /**
     * Class constructor specifying Image to encode, with no alpha channel encoding.
     *
     * @param image A Java Image object which uses the DirectColorModel
     * @see java.awt.Image
     */
    public PngEncoder( Image image )
        {
        this(image, false, FILTER_NONE, 0);
        }

    /**
     * Class constructor specifying Image to encode, and whether to encode alpha.
     *
     * @param image A Java Image object which uses the DirectColorModel
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     * @see java.awt.Image
     */
    public PngEncoder( Image image, boolean encodeAlpha )
        {
        this(image, encodeAlpha, FILTER_NONE, 0);
        }

    /**
     * Class constructor specifying Image to encode, whether to encode alpha, and filter to use.
     *
     * @param image A Java Image object which uses the DirectColorModel
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     * @param whichFilter 0=none, 1=sub, 2=up
     * @see java.awt.Image
     */
    public PngEncoder( Image image, boolean encodeAlpha, int whichFilter )
        {
        this( image, encodeAlpha, whichFilter, 0 );
        }


    /**
     * Class constructor specifying Image source to encode, whether to encode alpha, filter to use, and compression level.
     *
     * @param image A Java Image object
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     * @param whichFilter 0=none, 1=sub, 2=up
     * @param compLevel 0..9
     * @see java.awt.Image
     */
    public PngEncoder( Image image, boolean encodeAlpha, int whichFilter,
        int compLevel)
        {
        this.image = image;
        this.encodeAlpha = encodeAlpha;
        setFilter( whichFilter );
        if (compLevel >=0 && compLevel <=9)
            {
            this.compressionLevel = compLevel;
            }
        }

    /**
     * Set the image to be encoded
     *
     * @param image A Java Image object which uses the DirectColorModel
     * @see java.awt.Image
     * @see java.awt.image.DirectColorModel
     */
    public void setImage( Image image )
        {
        this.image = image;
        pngBytes = null;
        }

    /**
     * Creates an array of bytes that is the PNG equivalent of the current image, specifying whether to encode alpha or not.
     *
     * @param encodeAlpha boolean false=no alpha, true=encode alpha
     * @return an array of bytes, or null if there was a problem
     */
    public byte[] pngEncode( boolean encodeAlpha )
        {
        byte[]  pngIdBytes = { -119, 80, 78, 71, 13, 10, 26, 10 };

        if (image == null)
            {
            return null;
            }
        width = image.getWidth( null );
        height = image.getHeight( null );

        /*
         * start with an array that is big enough to hold all the pixels
         * (plus filter bytes), and an extra 200 bytes for header info
         */
        pngBytes = new byte[((width+1) * height * 3) + 200];

        /*
         * keep track of largest byte written to the array
         */
        maxPos = 0;

        bytePos = writeBytes( pngIdBytes, 0 );
        hdrPos = bytePos;
        writeHeader();
        dataPos = bytePos;
        if (writeImageData())
            {
            writeEnd();
            pngBytes = resizeByteArray( pngBytes, maxPos );
            }
        else
            {
            pngBytes = null;
            }
        return pngBytes;
        }

    /**
     * Creates an array of bytes that is the PNG equivalent of the current image.
     * Alpha encoding is determined by its setting in the constructor.
     *
     * @return an array of bytes, or null if there was a problem
     */
    public byte[] pngEncode()
        {
        return pngEncode( encodeAlpha );
        }

    /**
     * Set the alpha encoding on or off.
     *
     * @param encodeAlpha  false=no, true=yes
     */
    public void setEncodeAlpha( boolean encodeAlpha )
        {
        this.encodeAlpha = encodeAlpha;
        }

    /**
     * Retrieve alpha encoding status.
     *
     * @return boolean false=no, true=yes
     */
    public boolean getEncodeAlpha()
        {
        return encodeAlpha;
        }

    /**
     * Set the filter to use
     *
     * @param whichFilter from constant list
     */
    public void setFilter( int whichFilter )
        {
        this.filter = FILTER_NONE;
        if ( whichFilter <= FILTER_LAST )
            {
            this.filter = whichFilter;
            }
        }

    /**
     * Retrieve filtering scheme
     *
     * @return int (see constant list)
     */
    public int getFilter()
        {
        return filter;
        }
    
    /**
     * Set the compression level to use
     *
     * @param level 0 through 9
     */
    public void setCompressionLevel( int level )
        {
        if ( level >= 0 && level <= 9)
            {
            this.compressionLevel = level;
            }
        }

    /**
     * Retrieve compression level
     *
     * @return int in range 0-9
     */
    public int getCompressionLevel()
        {
        return compressionLevel;
        }

    /**
     * Increase or decrease the length of a byte array.
     *
     * @param array The original array.
     * @param newLength The length you wish the new array to have.
     * @return Array of newly desired length. If shorter than the
     *         original, the trailing elements are truncated.
     */
    protected byte[] resizeByteArray( byte[] array, int newLength )
        {
        byte[]  newArray = new byte[newLength];
        int     oldLength = array.length;

        System.arraycopy( array, 0, newArray, 0,
            Math.min( oldLength, newLength ) );
        return newArray;
        }

    /**
     * Write an array of bytes into the pngBytes array.
     * Note: This routine has the side effect of updating
     * maxPos, the largest element written in the array.
     * The array is resized by 1000 bytes or the length
     * of the data to be written, whichever is larger.
     *
     * @param data The data to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeBytes( byte[] data, int offset )
        {
        maxPos = Math.max( maxPos, offset + data.length );
        if (data.length + offset > pngBytes.length)
            {
            pngBytes = resizeByteArray( pngBytes, pngBytes.length +
                Math.max( 1000, data.length ) );
            }
        System.arraycopy( data, 0, pngBytes, offset, data.length );
        return offset + data.length;
        }

    /**
     * Write an array of bytes into the pngBytes array, specifying number of bytes to write.
     * Note: This routine has the side effect of updating
     * maxPos, the largest element written in the array.
     * The array is resized by 1000 bytes or the length
     * of the data to be written, whichever is larger.
     *
     * @param data The data to be written into pngBytes.
     * @param nBytes The number of bytes to be written.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeBytes( byte[] data, int nBytes, int offset )
        {
        maxPos = Math.max( maxPos, offset + nBytes );
        if (nBytes + offset > pngBytes.length)
            {
            pngBytes = resizeByteArray( pngBytes, pngBytes.length +
                Math.max( 1000, nBytes ) );
            }
        System.arraycopy( data, 0, pngBytes, offset, nBytes );
        return offset + nBytes;
        }

    /**
     * Write a two-byte integer into the pngBytes array at a given position.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeInt2( int n, int offset )
        {
        byte[] temp = { (byte)((n >> 8) & 0xff),
            (byte) (n & 0xff) };
        return writeBytes( temp, offset );
        }

    /**
     * Write a four-byte integer into the pngBytes array at a given position.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeInt4( int n, int offset )
        {
        byte[] temp = { (byte)((n >> 24) & 0xff),
            (byte) ((n >> 16) & 0xff ),
            (byte) ((n >> 8) & 0xff ),
            (byte) ( n & 0xff ) };
        return writeBytes( temp, offset );
        }

    /**
     * Write a single byte into the pngBytes array at a given position.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeByte( int b, int offset )
        {
        byte[] temp = { (byte) b };
        return writeBytes( temp, offset );
        }

    /**
     * Write a string into the pngBytes array at a given position.
     * This uses the getBytes method, so the encoding used will
     * be its default.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     * @see java.lang.String#getBytes()
     */
    protected int writeString( String s, int offset )
        {
        return writeBytes( s.getBytes(), offset );
        }

    /**
     * Write a PNG "IHDR" chunk into the pngBytes array.
     */
    protected void writeHeader()
        {
        int startPos;

        startPos = bytePos = writeInt4( 13, bytePos );
        bytePos = writeString( "IHDR", bytePos );
        width = image.getWidth( null );
        height = image.getHeight( null );
        bytePos = writeInt4( width, bytePos );
        bytePos = writeInt4( height, bytePos );
        bytePos = writeByte( 8, bytePos ); // bit depth
        bytePos = writeByte( (encodeAlpha) ? 6 : 2, bytePos ); // direct model
        bytePos = writeByte( 0, bytePos ); // compression method
        bytePos = writeByte( 0, bytePos ); // filter method
        bytePos = writeByte( 0, bytePos ); // no interlace
        crc.reset();
        crc.update( pngBytes, startPos, bytePos-startPos );
        crcValue = crc.getValue();
        bytePos = writeInt4( (int) crcValue, bytePos );
        }

    /**
     * Perform "sub" filtering on the given row.
     * Uses temporary array leftBytes to store the original values
     * of the previous pixels.  The array is 16 bytes long, which
     * will easily hold two-byte samples plus two-byte alpha.
     *
     * @param pixels The array holding the scan lines being built
     * @param startPos Starting position within pixels of bytes to be filtered.
     * @param width Width of a scanline in pixels.
     */
    protected void filterSub( byte[] pixels, int startPos, int width )
        {
        int i;
        int offset = bytesPerPixel;
        int actualStart = startPos + offset;
        int nBytes = width * bytesPerPixel;
        int leftInsert = offset;
        int leftExtract = 0;

        for (i=actualStart; i < startPos + nBytes; i++)
            {
            leftBytes[leftInsert] =  pixels[i];
            pixels[i] = (byte) ((pixels[i] - leftBytes[leftExtract]) % 256);
            leftInsert = (leftInsert+1) % 0x0f;
            leftExtract = (leftExtract + 1) % 0x0f;
            }
        }

    /**
     * Perform "up" filtering on the given row.
     * Side effect: refills the prior row with current row
     *
     * @param pixels The array holding the scan lines being built
     * @param startPos Starting position within pixels of bytes to be filtered.
     * @param width Width of a scanline in pixels.
     */
    protected void filterUp( byte[] pixels, int startPos, int width )
        {
        int     i, nBytes;
        byte    current_byte;

        nBytes = width * bytesPerPixel;

        for (i=0; i < nBytes; i++)
            {
            current_byte = pixels[startPos + i];
            pixels[startPos + i] = (byte) ((pixels[startPos  + i] - priorRow[i]) % 256);
            priorRow[i] = current_byte;
            }
        }

    /**
     * Write the image data into the pngBytes array.
     * This will write one or more PNG "IDAT" chunks. In order
     * to conserve memory, this method grabs as many rows as will
     * fit into 32K bytes, or the whole image; whichever is less.
     *
     *
     * @return true if no errors; false if error grabbing pixels
     */
    protected boolean writeImageData()
        {
        int rowsLeft = height;  // number of rows remaining to write
        int startRow = 0;       // starting row to process this time through
        int nRows;              // how many rows to grab at a time

        byte[] scanLines;       // the scan lines to be compressed
        int scanPos;            // where we are in the scan lines
        int startPos;           // where this line's actual pixels start (used for filtering)

        byte[] compressedLines; // the resultant compressed lines
        int nCompressed;        // how big is the compressed area?


        PixelGrabber pg;

        bytesPerPixel = (encodeAlpha) ? 4 : 3;

        Deflater scrunch = new Deflater( compressionLevel );
        ByteArrayOutputStream outBytes = 
            new ByteArrayOutputStream(1024);
            
        DeflaterOutputStream compBytes =
            new DeflaterOutputStream( outBytes, scrunch );
        try
            {
            while (rowsLeft > 0)
                {
                nRows = Math.min( 32767 / (width*(bytesPerPixel+1)), rowsLeft );
                if (nRows <= 0) nRows = 1;   // patch, thanks to David  -- Sean
                // nRows = rowsLeft;

                int[] pixels = new int[width * nRows];

                pg = new PixelGrabber(image, 0, startRow,
                    width, nRows, pixels, 0, width);
                try {
                    pg.grabPixels();
                    }
                catch (Exception e) {
                    System.err.println("interrupted waiting for pixels!");
                    return false;
                    }
                if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
                    System.err.println("image fetch aborted or errored");
                    return false;
                    }

                /*
                 * Create a data chunk. scanLines adds "nRows" for
                 * the filter bytes. 
                 */
                scanLines = new byte[width * nRows * bytesPerPixel +  nRows];

                if (filter == FILTER_SUB)
                    {
                    leftBytes = new byte[16];
                    }
                if (filter == FILTER_UP)
                    {
                    priorRow = new byte[width*bytesPerPixel];
                    }

                scanPos = 0;
                startPos = 1;
                for (int i=0; i<width*nRows; i++)
                    {
                    if (i % width == 0)
                        {
                        scanLines[scanPos++] = (byte) filter; 
                        startPos = scanPos;
                        }
                    scanLines[scanPos++] = (byte) ((pixels[i] >> 16) & 0xff);
                    scanLines[scanPos++] = (byte) ((pixels[i] >>  8) & 0xff);
                    scanLines[scanPos++] = (byte) ((pixels[i]      ) & 0xff);
                    if (encodeAlpha)
                        {
                        scanLines[scanPos++] = (byte) ((pixels[i] >> 24) & 0xff );
                        }
                    if ((i % width == width-1) && (filter != FILTER_NONE))
                        {
                        if (filter == FILTER_SUB)
                            {
                            filterSub( scanLines, startPos, width );
                            }
                        if (filter == FILTER_UP)
                            {
                            filterUp( scanLines, startPos, width );
                            }
                        }
                    }

                /*
                 * Write these lines to the output area
                 */
                compBytes.write( scanLines, 0, scanPos );


                startRow += nRows;
                rowsLeft -= nRows;
                }
            compBytes.close();

            /*
             * Write the compressed bytes
             */
            compressedLines = outBytes.toByteArray();
            nCompressed = compressedLines.length;

            crc.reset();
            bytePos = writeInt4( nCompressed, bytePos );
            bytePos = writeString("IDAT", bytePos );
            crc.update("IDAT".getBytes());
            bytePos = writeBytes( compressedLines, nCompressed, bytePos );
            crc.update( compressedLines, 0, nCompressed );

            crcValue = crc.getValue();
            bytePos = writeInt4( (int) crcValue, bytePos );
            scrunch.finish();
            return true;
            }
        catch (IOException e)
            {
            System.err.println( e.toString());
            return false;
            }
        }

    /**
     * Write a PNG "IEND" chunk into the pngBytes array.
     */
    protected void writeEnd()
        {
        bytePos = writeInt4( 0, bytePos );
        bytePos = writeString( "IEND", bytePos );
        crc.reset();
        crc.update("IEND".getBytes());
        crcValue = crc.getValue();
        bytePos = writeInt4( (int) crcValue, bytePos );
        }
    }


/*
  ARTISTIC LICENSE

  Preamble

  The intent of this document is to state the conditions under which a Package
  may be copied, such that the Copyright Holder maintains some semblance of
  artistic control over the development of the package, while giving the users of
  the package the right to use and distribute the Package in a more-or-less
  customary fashion, plus the right to make reasonable modifications. 

  Definitions:

  "Package" refers to the collection of files distributed by the Copyright
  Holder, and derivatives of that collection of files created through textual
  modification. 

  "Standard Version" refers to such a Package if it has not been modified, or has
  been modified in accordance with the wishes of the Copyright Holder. 

  "Copyright Holder" is whoever is named in the copyright or copyrights for the
  package. 

  "You" is you, if you're thinking about copying or distributing this Package. 

  "Reasonable copying fee" is whatever you can justify on the basis of media
  cost, duplication charges, time of people involved, and so on.  (You will not
  be required to justify it to the Copyright Holder, but only to the computing
  community at large as a m arket that must bear the fee.)

  "Freely Available" means that no fee is charged for the item itself, though
  there may be fees involved in handling the item. It also means that recipients
  of the item may redistribute it under the same conditions they received it. 
    

  1. You may make and give away verbatim copies of the source form of the
  Standard Version of this Package without restriction, provided that you
  duplicate all of the original copyright notices and associated disclaimers. 

  2. You may apply bug fixes, portability fixes and other modifications derived
  from the Public Domain or from the Copyright Holder.  A Package modified in
  such a way shall still be considered the Standard Version. 

  3. You may otherwise modify your copy of this Package in any way, provided that
  you insert a prominent notice in each changed file stating how and when you
  changed that file, and provided that you do at least ONE of the following: 
    
  a) place your modifications in the Public Domain or otherwise make them Freely
  Available, such as by posting said modifications to Usenet or an equivalent
  medium, or placing the modifications on a major archive site such as
  ftp.uu.net, or by allowing the Copyright Holder to include your modifications
  in the Standard Version of the Package. 
    
  b) use the modified Package only within your corporation or organization. 
    
  c) rename any non-standard executables so the names do not conflict with
  standard executables, which must also be provided, and provide a separate
  manual page for each non-standard executable that clearly documents how it
  differs from the Standard Version.
    
  d) make other distribution arrangements with the Copyright Holder.
    
    
  4. You may distribute the programs of this Package in object code or executable
  form, provided that you do at least ONE of the following: 
    
  a) distribute a Standard Version of the executables and library files, together
  with instructions (in the manual page or equivalent) on where to get the
  Standard Version. 
    
  b) accompany the distribution with the machine-readable source of the Package
  with your modifications. 
    
  c) accompany any non-standard executables with their corresponding Standard
  Version executables, giving the non-standard executables non-standard names,
  and clearly documenting the differences in manual pages (or equivalent),
  together with instructions on where to get the Standard Version. 
    
  d) make other distribution arrangements with the Copyright Holder. 


  5. You may charge a reasonable copying fee for any distribution of this
  Package.  You may charge any fee you choose for support of this Package. You
  may not charge a fee for this Package itself.  However, you may distribute this
  Package in aggregate with other (possibly commercial) programs as part of a
  larger (possibly commercial) software distribution provided that you do not
  advertise this Package as a product of your own. 


  6. The scripts and library files supplied as input to or produced as output
  from the programs of this Package do not automatically fall under the copyright
  of this Package, but belong to whomever generated them, and may be sold
  commercially, and may be ag gregated with this Package. 


  7. Subroutines supplied by you and linked into this Package shall not
  be considered part of this Package. 


  8. The name of the Copyright Holder may not be used to endorse or promote
  products derived from this software without specific prior written permission. 


  9. THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED
  WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
  MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE. 


  The End
*/
