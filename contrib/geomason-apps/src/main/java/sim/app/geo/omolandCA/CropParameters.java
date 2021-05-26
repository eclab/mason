package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
public class CropParameters {

	public static final int NOCROP_ID = 0;
	public static final int MAIZE_ID = 1;
	public static final int MAIZE_LGP = 4;// month 125-180 days FAO 4mo
	public static final int MAIZE_LABOR = 2;// person
	public static final int MAIZE_WATERREQ = 500;// 500-800mm FAO medium to high to drought
	public static final double MAIZE_MAXYIELD = 3000;// kg //Yield Maize QT/HA =27.74 ~ 30QT/HA
														// http://www.csa.gov.et/images/general/2014_2015_crop_report
	public static final double MAIZE_MAXYIELDHYV = 4500; // assume 1.5 more by high yield variety
	public static final double MAIZE_PRICE = 3;// 3;//per kg // CSA 2013
	public static final double MAIZE_SOILFERPENALITY = 1;
	public static final double MAIZE_INPUTCOST = 0;

}
