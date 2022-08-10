package sim.app.geo.dschellingspace;

import java.util.ArrayList;

import sim.util.geo.MasonGeometry;


public class DSchellingGeometry extends MasonGeometry {

    private int id = -1;

    private String soc;

    public int initRed = 0, initBlue = 0;

    public ArrayList<DPerson> residents;
    public ArrayList<DSchellingGeometry> neighbors;



    public DSchellingGeometry()
    {
        super();
        residents = new ArrayList<DPerson>();
        neighbors = new ArrayList<DSchellingGeometry>();
    }



    public void init()
    {
        initRed = getIntegerAttribute("RED");
        initBlue = getIntegerAttribute("BLUE");
    }



    int getID()
    {
        return getDoubleAttribute("ID_ID").intValue();
    }



    String getSoc()
    {
        return getStringAttribute("SOC");
    }
}
