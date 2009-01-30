/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information

  Portions of this software are copyrighted by Sun Microsystems Incorporated
  and fall under the license listed at the end of this file.
*/

package sim.util.media;
import javax.media.*;
import javax.media.util.*;
import javax.media.protocol.*;
import javax.media.datasink.*;
import javax.media.control.*;
import java.io.*;
import java.awt.image.*;
import java.awt.*;

/*
 * MovieEncoder
 * Sean Luke
 * With help from Sun (see license at end)
 *
 * This file contains two subsidiary classes: MovieEncoderDataStream and MovieEncoderDataSource.
 * These are the custom classes which provide the underlying JMF system with images in the
 * form of buffers to convert to Quicktime.  With the license at the end of the file is a
 * URL which points to a (slightly broken) example file Sun provided to give some idea on how
 * to do this.  Basically, JMF is tough stuff to write!
 *
 */


/** <p>Usage of this class depends on the existence of the Java Media Framework (JMF)
 * which can be acquired from javasoft.com.  The class was tested on JMF 2.1.1.
 *
 * <p>This class encodes BufferedImages into quicktime movies.  It has three main
 * functions:
 * 
 * <p>First, you call getEncodingFormats(), which returns an array of available formats.
 * You specify a frame rate and a prototypical image.
 *
 * <p>Then, you call the constructor -- specify the frame rate, file, an encoding format
 * chosen from the array in getEncodingFormats, and again provide a prototypical image.
 * You'll want to ensure that future images encoded into the MovieEncoder will have
 * the same size and format as the prototype image.  If not, they'll be converted
 * and cropped accordingly, which will take more time.  You should expect the constructor
 * to take a few seconds to start up -- JMF is not speedy.
 *
 * <p>Then you drop images into the MovieEncoder to add to the Quicktime movie
 * as frames, using the add(BufferedImage) method.  If there was an error and the movie
 * could not be written, this method (and all subsequent add(...) calls) will return false.
 *
 * <p>When you're all finished, you call the stop() method to clean up and complete
 * writing the file.
 *
 * <p><b>Note:</b> Sun's JMF spawns threads in the background which it never cleans up.
 * Thus if you use this class, you'll need to call System.exit(0) to quit your program
 * rather than just dropping out of main().
 *
 */


public class MovieEncoder implements DataSinkListener, ControllerListener, java.io.Serializable
    {
    boolean started;    // are we running?
    boolean stopped;    // are we finished?
    int width;          // width of first frame -- guides width of all other frames
    int height;         // height of first rame -- guides height of all other frames
    int type;           // type of first image -- guides type of all other images
    float frameRate;    // desired frame rate
    Processor processor; // the JMF processor
    MovieEncoderDataSource source; // Our buffer data source
    DataSink sink;         // Our buffer data sink.  Defined by the file
    File file;             // URL pointing to the file.  Dumb that JMF can't write to a stream.
    
    Format encodeFormat; // format to encode in
   
    
    
    // Presently commented out: in Java 1.6, this code appears to freak JMF out :-(
    // Not sure why 1.6 would be doing this.   Anyway, JMF doesn't appear to be
    // pooping JMF files out any more, at least under 1.5.  -- Sean
    /*
      static
      {
      // We're going to hack com.sun.media.Log so that it doesn't
      // create the jmf.log poops.  These poops are made by Log.static{}
      // when the Log class is loaded.  Here's the trick.  
      // The method first checks for the current security by calling 
      // com.sun.media.JMFSecurityManager.getJMFSecurity(), which
      // just returns a single object created at static initialize time.
      // We do that here by calling the method ourselves.
            
      com.sun.media.JMFSecurityManager.getJMFSecurity();
            
      // That'll do the job.  Now the next thing the Log.static{} 
      // does is create the filename by calling System.getProperty("user.dir"),
      // and tacking on a file separator and the infamous "jmf.log" string
      // after that.  It then makes the file and more or less exits.
      // We'll mess things up by deleting the System's properties
      // temporarily.

      java.util.Properties p = System.getProperties();
      System.setProperties(new java.util.Properties());

      // now we call something which causes Log.static{} to load.
        
      try
      {
      com.sun.media.Log.getIndent();
      }
      catch (Exception e) { }
        
      // restore the properties and tell the Log never to try to write to the file.
      System.setProperties(p);
        
      com.sun.media.Log.isEnabled = false;
      }
    */
    
    /** Returns null and prints an error out to stderr if an error occurred while trying to
        get the formats */
        
    public static Format[] getEncodingFormats(float fps, BufferedImage typicalImage)
        {
        return new MovieEncoder().getEncodingFormatsHelper(fps,typicalImage);
        }
    
    private Format[] getEncodingFormatsHelper(float fps, BufferedImage typicalImage)
        {
        try
            {
            // get possible formats for encoding
            Format format = (Format)(ImageToBuffer.createBuffer(typicalImage,fps).getFormat());
            
            MovieEncoderDataSource source = new MovieEncoderDataSource(format, fps);
        
            Processor processor = Manager.createProcessor(source);
            processor.addControllerListener(this);
            
            processor.configure();
            // while necessary for encoding video,
            // this may not be necessary for simply getting formats
            // however, better safe than sorry.. so here goes:
            if (!waitForState(processor, Processor.Configured))
                throw new RuntimeException("Failed to configure processor");
                
            processor.setContentDescriptor(new ContentDescriptor(FileTypeDescriptor.QUICKTIME));
            
            TrackControl tcs[] = processor.getTrackControls();

            // and finally...
            Format f[] = tcs[0].getSupportedFormats();
            if (f == null || f.length <= 0)
                throw new RuntimeException("The mux does not support the input format: " + tcs[0].getFormat());
            processor.removeControllerListener(this);
            return f;
            }
        catch (Exception e)
            {
            e.printStackTrace();
            processor.removeControllerListener(this);
            return null;
            }
        }
    
    // private empty constructor
    MovieEncoder() {};

    /** Creates an object which will write out a move of the specified
        format, and written to the provided file.  */
    public MovieEncoder(float frameRate, File file, BufferedImage typicalImage, Format encodeFormat)
        {
        this.frameRate = frameRate;
        this.file = file;
        this.encodeFormat = encodeFormat;
        try
            {
            setup(typicalImage);
            started = true;
            }
        catch (Exception e)
            {
            e.printStackTrace();
            stopped = true;
            }
        }


    final Object waitSync = new Object();
    boolean stateTransitionOK = true;

    /**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    boolean waitForState(Processor p, int state) {
        
        synchronized (waitSync) {
            try {
                while (p.getState() < state && stateTransitionOK)
                    waitSync.wait();
                } catch (Exception e) {}
            }
        return stateTransitionOK;
        }
    /**
     * Controller Listener.
     */
    public void controllerUpdate(ControllerEvent evt) {
        if (evt instanceof ConfigureCompleteEvent ||
            evt instanceof RealizeCompleteEvent ||
            evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
                }
            } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
                }
            } else if (evt instanceof EndOfMediaEvent) {
            evt.getSourceController().stop();  // no need to check for null stopper, as null is not an instance of any class
            // sometimes Java gives us an error on this one.  So we need to wrap it
            // and hope for the best -- Sean
            try
                { evt.getSourceController().close(); }
            catch (Exception e) 
                {
                System.err.println("Spurious Sun JMF Error?\n\n"); e.printStackTrace(); 
                    
                // system sometimes gives no further event updates, so the waiter hangs, 
                // and I'm not sure why -- so here we make it fail.  I wonder if this will work -- Sean
                synchronized (waitSync) 
                    {
                    stateTransitionOK = false;
                    waitSync.notifyAll();
                    }
                synchronized (waitFileSync) 
                    {
                    fileSuccess = false;
                    fileDone = true;
                    waitFileSync.notifyAll();
                    }
                // end weird mods
                }
            }
        }


    final Object waitFileSync = new Object();
    boolean fileDone = false;
    boolean fileSuccess = true;

    /**
     * Block until file writing is done. 
     */
    boolean waitForFileDone() {
        synchronized (waitFileSync) {
            try {
                while (!fileDone)
                    waitFileSync.wait();
                } catch (Exception e) {}
            }
        return fileSuccess;
        }


    /**
     * Event handler for the file writer.
     */
    public void dataSinkUpdate(DataSinkEvent evt) {

        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                waitFileSync.notifyAll();
                }
            } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
                }
            }
        }



    void setup(BufferedImage i) 
        throws IOException, NoDataSinkException, NoProcessorException, CannotRealizeException, RuntimeException
        {
        width = i.getWidth();
        height = i.getHeight();
        type = i.getType();
        
        // i think my hacking begins here (dan)
        // get formats with current framerate, which should not matter
        Format format = (Format)(ImageToBuffer.createBuffer(i,frameRate).getFormat());
        source = new MovieEncoderDataSource(format, frameRate);
    
        processor = Manager.createProcessor(source);

        processor.addControllerListener(this);
        processor.configure();
        if (!waitForState(processor, Processor.Configured))
            throw new RuntimeException("Failed to configure processor");
            
        processor.setContentDescriptor(new ContentDescriptor(FileTypeDescriptor.QUICKTIME));
    
        TrackControl tcs[] = processor.getTrackControls();
        
        // old -- just take first one
        /*
          Format f[] = tcs[0].getSupportedFormats();
          if (f == null || f.length <= 0)
          throw new RuntimeException("The mux does not support the input format: " + tcs[0].getFormat());
          tcs[0].setFormat(f[0]);
          System.out.println(tcs[0]);
          System.out.println(f[0]);*/
        
        // new - set by requested format
        tcs[0].setFormat(encodeFormat);

        // realize the processor
        processor.realize();
        if (!waitForState(processor, Processor.Realized))
            throw new RuntimeException("Failed to Realize processor");
        
        // note: the file.toURI().toURL() thing is because Java6 has deprecated
        // use of file.toURL()
        sink = Manager.createDataSink(processor.getDataOutput(), new MediaLocator(file.toURI().toURL()));
        sink.addDataSinkListener(this);
        sink.open();
        processor.start();
        sink.start();

        started = true;
        }

    BufferedImage preprocess(BufferedImage i)
        {
        // guarantee that subsequent images are exactly the same format and size
        // as the first image
        if (i.getWidth() != width || i.getHeight() != height || i.getType() != type)
            {
            BufferedImage temp = new BufferedImage(width,height,type);
            Graphics2D g = temp.createGraphics();
            g.drawImage(i,0,0,null);
            i = temp;
            }
        return i;
        }

    /** Adds an image to the movie.  The very first image added will specify the size
        of the movie's frames and the image type for later images.  You should strive to
        keep the images the same size, else MovieEncoder must will conver and crop them
        to the original image's format and size.  Returns true if the image was successfully
        added.  Returns false if we've been stopped (either intentionally or due to an error).
    */
    public synchronized boolean add(BufferedImage i)
        {
        if (!stopped)
            {
            i = preprocess(i); // give source a chance to be created, then...
            source.add(i);
            }
        return (!stopped);
        }
    
    /** Stops the writer and finishes uprocessor.  After this, this object cannot be used.
        It is possible that the system may take a second to write out and finish up */
    public synchronized boolean stop()
        {
        if (!started) return false;
        if (stopped) return false;
        stopped = true;
        source.finish();
        // Wait for EndOfStream event.
        boolean success = waitForFileDone();
        try { sink.close(); } catch (Exception e) {}
        processor.removeControllerListener(this);
        stopped = true;  // just in case the dataSinkUpdate didn't do it -- interruptedException maybe
        return success;
        }
    }


/*
 * MovieEncoderDataSource
 * provides the MovieEncoderDataStream, and that's all really
 *
 */

// a very simple data source
class MovieEncoderDataSource extends PullBufferDataSource 
    {
    MovieEncoderDataStream[] streams;
    
    public MovieEncoderDataSource(Format format, float frameRate) 
        {
        streams = new MovieEncoderDataStream[1];
        streams[0] = new MovieEncoderDataStream(format, frameRate);
        }
    
    public void setLocator(MediaLocator source) { }
    
    public MediaLocator getLocator() { return null; }
    
    public String getContentType() { return ContentDescriptor.RAW; }
    
    public void connect() {}
    
    public void disconnect() {}
    
    public void start() {}
    
    public void stop() {}
    
    public PullBufferStream[] getStreams() { return streams; }
    
    public Time getDuration() { return DURATION_UNKNOWN; }
    
    public Object[] getControls() { return new Object[0]; }
    
    public Object getControl(String type) { return null; }
    
    public void add(Image i)
        {
        streams[0].write(i);
        }
    
    public void finish()
        {
        streams[0].finish();
        }
    
    }

/*
 *
 * MovieEncoderDataStream
 * Provides the underlying JMF Processor with images (converted to Buffers) for it to
 * encode and write out to disk as it sees fit.
 *
 */

// Our stream from which the processor requests images.  Here's how it works.
// You put an image in the stream with write().  This is blocking -- we have to
// wait until any existing image in there has been flushed out.
// Additionally, the underlying processor is reading images [as buffers] with read(),
// and it's blocking waiting for us to provide stuff.  The blocks are handled with
// spin-waits (25ms sleeps) because I'm being lazy.
class MovieEncoderDataStream implements PullBufferStream
    {
    // we won't have a buffered mechanism here -- instead we'll assume
    // that there is one, and exactly one, image waiting
    Buffer buffer = null;
    Format format;
    boolean ended = false;
    boolean endAcknowledged = false;
    float frameRate;
    
    MovieEncoderDataStream(Format format, float frameRate) { frameRate = this.frameRate ;this.format = format; }
    
    void finish()
        {
        synchronized(this)
            {
            ended = true;
            }
        }
    
    // blocks on write
    void write(Image i)
        {
        Buffer b = ImageToBuffer.createBuffer(i, frameRate);
        while(checkWriteBlock()) 
            try { Thread.sleep(25); } catch (InterruptedException e) { return; }  // spin-wait, ugh
        synchronized(this)
            {
            buffer = b;
            }
        }

    synchronized boolean checkWriteBlock()  { return (buffer!=null); }
            
    synchronized boolean checkReadBlock() { return (buffer==null && !ended); }

    public boolean willReadBlock() { return false; } // lie

    // could block on read
    public void read(Buffer buf) throws IOException
        {
        while(checkReadBlock()) try { Thread.sleep(25); } catch (InterruptedException e) { }  // spin-wait, ugh
        // Check if we need to close up shop
        synchronized(this)
            {
            if (buffer != null)  // may still have data left even if ended, so we need to do that
                {
                // load the data
                buf.setData(buffer.getData());
                buf.setLength(buffer.getLength());
                buf.setOffset(0);
                buf.setFormat(format);
                buf.setFlags(buf.getFlags() | Buffer.FLAG_KEY_FRAME | Buffer.FLAG_NO_DROP);  // must write the frame
                }
            buffer = null;
            if (ended) 
                {
                // We are done.  Set EndOfMedia.
                buf.setEOM(true);
                buf.setOffset(0);
                buf.setLength(0);
                endAcknowledged = true;
                }
            }
        }

    // returns the buffered image format
    public Format getFormat() {  return format; }

    public ContentDescriptor getContentDescriptor()  { return new ContentDescriptor(ContentDescriptor.RAW); }

    public long getContentLength() { return 0; }

    public boolean endOfStream() { return ended; } // or should it be endAcknowledged?

    public Object[] getControls() { return new Object[0]; }

    public Object getControl(String type) { return null; }
    }











///// LICENSES
/// Some of this code was snarfed from 
/// http://java.sun.com/products/java-media/jmf/2.1.1/solutions/JpegImagesToMovie.java
/// Here's the license to that code.

/*
 * @(#)JpegImagesToMovie.java   1.3 01/03/13
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */



