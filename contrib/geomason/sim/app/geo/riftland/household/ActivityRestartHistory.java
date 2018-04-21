package riftland.household;

import ec.util.MersenneTwisterFast;

/**
 * A data structure for keeping track of how many failed attempts a Household
 * has made to start various activities, and how long to wait before the next
 * attempt.
 * 
 * @author Eric 'Siggy' Scott
 * @see ActivityManager
 */
final class ActivityRestartHistory
{
    // Default value of 4 is meant to mean that, at initialization, approximately 1 week passes before the first start attempt (see activityRestartDays()).
    private int herdingTries;
    private int lastHerdingTryDate;
    private int farmingTries;
    private int lastFarmingTryDate;
    private int laboringTries;
    private int lastLaboringTryDate;

    ActivityRestartHistory() { reset(); }
    
    // <editor-fold defaultstate="collapsed" desc="Accessors">
    public int getHerdingTries() {
        return herdingTries;
    }

    public int getLastHerdingTryDate() {
        return lastHerdingTryDate;
    }

    public int getFarmingTries() {
        return farmingTries;
    }

    public int getLastFarmingTryDate() {
        return lastFarmingTryDate;
    }

    public int getLaboringTries() {
        return laboringTries;
    }

    public int getLastLaboringTryDate() {
        return lastLaboringTryDate;
    }

    void setFarmingRestartTries(int count, int date)
    {
        farmingTries = count;
        lastFarmingTryDate = date;
        assert(repOK());
    }

    void setHerdingRestartTries(int count, int date)
    {
        herdingTries = Math.max(0, count);
        lastHerdingTryDate = date;
        assert(repOK());
    }

    void setLaboringRestartTries(int count, int date)
    {
        laboringTries = count;
        lastLaboringTryDate = date;
        assert(repOK());
    }
    // </editor-fold>

    /**
     * With each successive count of tries, significantly increment the next
     * time to do a retry for the given activity up to a limit of 18 years. Note
     * that a little Gaussian jitter is added to the next scheduled retry event.
     */
    int activityRestartDays(MersenneTwisterFast random, int count)
    {
        int days = 0;

        switch (count)
        {
            case 5:
                days = 0;
                break;
            case 4:
                days = (int) (7 + random.nextGaussian() * 2);
                break;
            case 3:
                days = (int) (30 + random.nextGaussian() * 5);
                break;
            case 2:
                days = (int) (180 + random.nextGaussian() * 30);
                break;
            case 1:
                days = (int) (365 * 3 + random.nextGaussian() * 90);
                break;
            default:
                if (count <= 0)
                    days = (int) (365 * 18 + random.nextGaussian() * 365 * 2);
        }
        assert(repOK());
        return days;
    }
    
    void reset()
    {
        // Default value of 4 is meant to mean that, at initialization, approximately 1 week passes before the first start attempt (see activityRestartDays()).
        herdingTries = 4;
        lastHerdingTryDate = 0;
        farmingTries = 4;
        lastFarmingTryDate = 0;
        laboringTries = 4;
        lastLaboringTryDate = 0;
    }
    
    void resetHerding()
    {
        herdingTries = 4;
        lastHerdingTryDate = 0;
    }

    /** Have enough days passed since the last restart that we can
     * try farming again? */
    boolean isFarmingRestartDelayOk(int today, MersenneTwisterFast random)
    {
        // determine days required to restart as func of restart number
        int daysRequired = activityRestartDays(random, farmingTries);
        // determine days since last restart
        int daysSince = today - lastFarmingTryDate;
        assert(repOK());
        return (daysSince >= daysRequired);
        //return true;
    }

    /** Have enough days passed since the last restart that we can
     * try herding again? */
    boolean isHerdingRestartDelayOk(int today, MersenneTwisterFast random)
    {
        // determine days required to restart as func of restart number
        int daysRequired = activityRestartDays(random, herdingTries);
        // determine days since last restart
        int daysSince = today - this.lastHerdingTryDate;
        assert(repOK());
        return (daysSince >= daysRequired);
    }
    
    boolean repOK()
    {
        return herdingTries >= 0
                && lastHerdingTryDate >= 0
                && farmingTries >= 0
                && lastFarmingTryDate >= 0
                && laboringTries >= 0
                && lastLaboringTryDate >= 0;
    }
}
