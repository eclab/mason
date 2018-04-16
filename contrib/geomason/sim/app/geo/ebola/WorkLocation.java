package ebola;



import sim.util.Int2D;

/**
 * Created by geoint on 8/5/15.
 */
public class WorkLocation extends Structure
{
    private int sector_id;

    public WorkLocation(Int2D location, int sector_id)
    {
        super(location);
        this.sector_id = sector_id;

        //set up capacity based on sector id
        this.setCapacity(getWorkSize(this.sector_id));
    }

    private static int getWorkSize(int sector_id)
    {
        double rand = EbolaBuilder.ebolaSim.random.nextDouble();
        double sum = 0;
        for(int i = 0; i < Parameters.WORK_SIZE_BY_SECTOR[sector_id].length; i++)
        {
            sum += Parameters.WORK_SIZE_BY_SECTOR[sector_id][i];
            if(rand < sum)
            {
                //we found the work size index, now convert it to number
                int size;
                if(i == 0)
                    size = 1;
                else if(i == 1)
                    size = 2 + EbolaBuilder.ebolaSim.random.nextInt(3);
                else if(i == 2)
                    size = 5 + EbolaBuilder.ebolaSim.random.nextInt(5);
                else if(i == 3)
                    size = 10 + EbolaBuilder.ebolaSim.random.nextInt(10);
                else if(i == 4)
                    size = 20 + EbolaBuilder.ebolaSim.random.nextInt(30);
                else
                    size = 55;
                return size;
            }
        }
        //error
        //System.out.println("rand = " + rand);
        return 55;
    }

    public int getSector_id() {
        return sector_id;
    }

    public void setSector_id(int sector_id) {
        this.sector_id = sector_id;
    }
}
