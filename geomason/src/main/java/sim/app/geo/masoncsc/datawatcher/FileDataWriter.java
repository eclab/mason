package sim.app.geo.masoncsc.datawatcher;

import java.io.*;

/**
 * Records the values observed by a DataWatcher to a CSV file.
 *
 * @author Eric 'Siggy' Scott
 * @author hkarbasi
 */
public class FileDataWriter implements DataListener
{
    public DataOutputStream out;
    /**
     * Initialize an output stream to the specified file.  If the file does not
     * exist, create.  If it does, overwrite it.  Prints an exception to stderr
     * if the file could not be created/opened for writing.
     * 
     * @param path Path to the output file.
     */
    public void InitFileDataWriter(String path, DataWatcher source)
    {
        try
        {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
            out.writeBytes(source.getCSVHeader());
        }
        catch(FileNotFoundException e)
        {
            System.err.println("Could not open file: " + e.getMessage());
        }

        catch (IOException e)
        {
            System.err.println("Could not write to file: " + e.getMessage());
        }
    }
    
    /** 
     * Close the file.  Prints an exception to stderr if the file is not open.
     */
    public void close()
    {
        try
        {
            out.flush();
        }
        catch(IOException e)
        {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    @Override
    public void dataUpdated(DataWatcher source)
    {
        try
        {
            if(!source.dataToCSV().equals("null")) {
                this.out.writeBytes(source.dataToCSV() + "\n");
            }

        }
        catch(IOException e)
        {
            System.err.println("IOException: " + e.getMessage());
        }
    }
}
