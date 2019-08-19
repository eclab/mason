package sim.io.geo;
public class DataObject {
    Class dummy;
    String file;
    public DataObject(Class dummy, String filename)
    {
        this.dummy = dummy;
        this.file = filename;
    }
    public getNewStream(){
        return dummy.getResourceAsStream(filename);
    }
    public getNewResource(){
        return dummy.getResource(filename);
    }
}
