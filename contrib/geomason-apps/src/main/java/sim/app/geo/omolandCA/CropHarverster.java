package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
import sim.engine.SimState;
import sim.engine.Steppable;

public class CropHarverster implements Steppable {

    public static final int ORDERING = 5;

    public void growCrop(final Landscape ls) {

            for (final Object c : ls.crops.getAllObjects()) {
                final Crop crop = (Crop) c;
                crop.grow(ls);
                crop.yield(ls);

            }
    }

    @Override
    public void step(final SimState state) {
        final Landscape ls = (Landscape) state;
        growCrop(ls);

    }
}
