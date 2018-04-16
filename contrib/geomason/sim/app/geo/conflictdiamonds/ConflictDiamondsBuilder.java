package conflictdiamonds;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import cityMigration.cityMigrationData.CityMigrationData;
import conflictdiamonds.conflictdiamondsData.ConflictDiamondsData;
import sim.field.geo.GeomVectorField;
import sim.field.grid.ObjectGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import java.io.BufferedReader;
import java.net.URL;

/**
 * Conflict Diamonds Builder
 * 
 * Creates the modeling world and the agents
 * 
 * 
 * @author bpint
 */
public class ConflictDiamondsBuilder {	
    
    /**
     * Create modeling world based on GIS data
     *
     * @param regionfile - identifies the modeling world and boundaries. Source of data is the Global Administrative Areas (GADM, 2009)
     * @param remotenessfile - each cell is assigned a remoteness value (based on distance from cities and highways). Source of data is OpenStreetMap (OpenStreetMap, 2010)
     * @param diamondfile - the location of diamond mines (represents real world locations). Source of data is the Peace Research Institute Oslo (PRIO) Center for the Study of Civil War (Gilmore et al., 2005)
     * @param diamondproximatefile - the location of diamond mines if assumed to be in Freetown
     * @param populationfile - the population by cell. Source of data is Oak Ridge National Laboratory (2007).
     * @param conflictDiamonds
     * 
     *
     */
    public static void create(String regionfile, String remotenessfile, String diamondfile, String diamondproximatefile, String populationfile, ConflictDiamonds conflictDiamonds) {
        try {
            // Open the file

			   
           // Convert our input stream to a BufferedReader
           BufferedReader regionarea = new BufferedReader(new InputStreamReader(ConflictDiamondsData.class.getResourceAsStream(regionfile)));
           BufferedReader remotesurface = new BufferedReader(new InputStreamReader(ConflictDiamondsData.class.getResourceAsStream(remotenessfile)));
           BufferedReader diamondsurface = new BufferedReader(new InputStreamReader(ConflictDiamondsData.class.getResourceAsStream(diamondfile)));
           BufferedReader diamondproximatesurface = new BufferedReader(new InputStreamReader(ConflictDiamondsData.class.getResourceAsStream(diamondproximatefile)));
           BufferedReader populationdist = new BufferedReader(new InputStreamReader(ConflictDiamondsData.class.getResourceAsStream(populationfile)));

           //Create the regions
           Region newRegion;
           int numRegions = 15;
	    					
           for (int i = 0; i < numRegions; i++) {
               newRegion = new Region(conflictDiamonds);
               conflictDiamonds.allRegions.put(i, newRegion);
               newRegion.setRegionID(i);
               setupRegions(newRegion, conflictDiamonds);    
            }		   
						   			             		   			   			
            // get the parameters from the file
            String line;
            int width = 0, height = 0;
            int nodata = -1;	 
	           
            // skip the irrelevant metadata
            for (int i = 0; i < 6; i++) {
                line = regionarea.readLine();

                // format the line appropriately
                String [] parts = line.split(" ", 2);
                String trimmed = parts[1].trim();

                // save the data in the appropriate place
                if(i == 1) { height = Integer.parseInt( trimmed ); }
                else if(i == 0) { width = Integer.parseInt( trimmed ); }
                else if( i == 5) { nodata = Integer.parseInt( trimmed ); }
                else { continue; }
            }
	            
            conflictDiamonds.allLand = new ObjectGrid2D(width, height);
            
            conflictDiamonds.allBoundaries = new GeomVectorField(width, height);
            conflictDiamonds.allDiamonds = new GeomVectorField(width, height);

            //Create the region boundaries
            Bag regionNames = new Bag();
            regionNames.add("NAME_2");

            //File file=new File("confli/conflictdiamonds/z_boundaries.shp");
            URL boundariesShapeUL = getUrl("/conflictdiamonds/conflictdiamondsData/z_boundaries.shp");//file.toURL();
            //System.exit(0);
            ShapeFileImporter.read(boundariesShapeUL, conflictDiamonds.allBoundaries, regionNames);

            Bag diamondLocation = new Bag();
            diamondLocation.add("Country");


            URL diamondsShapeUL = getUrl("/conflictdiamonds/conflictdiamondsData/z_Diamond_SL.shp");//fileDiamonds.toURL();

            ShapeFileImporter.read(diamondsShapeUL, conflictDiamonds.allDiamonds, diamondLocation);
			
            for ( int i = 0; i < width; i++ ) {
                for ( int j = 0; j < height; j++ ) {
                    conflictDiamonds.allLand.set(i, j, new Parcel(i, j));
                }
            }
			
            // read in the region data from the file and store it in parcels
            int i = 0, j = 0;
            while((line = regionarea.readLine()) != null){
                String [] parts = line.split(" ");

                for(String p: parts){
                    int value = Integer.parseInt(p);					

                    if( value == nodata ) {// no positive match
                        value = 0;
                    }
					
                    //if( value != Integer.MIN_VALUE) {
                    Parcel par = (Parcel) conflictDiamonds.allLand.get( j, i);
                    
                    

                    newRegion = conflictDiamonds.allRegions.get(value);
                    par.setRegion(newRegion);

                    newRegion.addRegionParcels(par);

                    j++; // increase the column count
                }

                j = 0; // reset the column count
                i++; // increase the row count
            }
			
			
            // skip the irrelevant metadata for remoteness file
            for (i = 0; i < 6; i++) {
                line = remotesurface.readLine();
                
                // format the line appropriately
                String [] parts = line.split(" ", 2);
                String trimmed = parts[1].trim();

                // save the data in the appropriate place
                if(i == 1) { height = Integer.parseInt( trimmed ); }
                else if(i == 0) { width = Integer.parseInt( trimmed ); }
                else if( i == 5) { nodata = Integer.parseInt( trimmed ); }
                else { continue; }
            }
			
            //read in remoteness (distance from cities) data and store it in parcels
            i = 0;
            j = 0;

            while((line = remotesurface.readLine()) != null) {
                String [] parts = line.split(" ");
                
	        	
                for(String p: parts) {
                    double value = Double.parseDouble(p);
                    
                    value = value - 1;

                    if( value == nodata ) {// no positive match
                        value = 32;
                    }

                    Parcel par = (Parcel) conflictDiamonds.allLand.get( j, i);

                    //set remoteness between 0 and 1
                    value = value / 32;

                    par.setRemoteness(value);

                    j++; // increase the column count
                }
                j = 0; // reset the column count
                i++; // increase the row count
            }	 	
            
            if ( conflictDiamonds.params.global.isProximateExperiment() == false ) {			
       	     	// skip the irrelevant metadata for diamond file
                for (i = 0; i < 6; i++) {
                    line = diamondsurface.readLine();
                    
                    // format the line appropriately
                    String [] parts = line.split(" ", 2);
                    String trimmed = parts[1].trim();

                    // save the data in the appropriate place
                    if(i == 1) { height = Integer.parseInt( trimmed ); }
                    else if(i == 0) { width = Integer.parseInt( trimmed ); }
                    else if( i == 5) { nodata = Integer.parseInt( trimmed ); }
                    else { continue; }
                }
                
            	//read in diamonds (distance from mines) data and store it in parcels
            	i = 0;
            	j = 0;
	        
            	while((line = diamondsurface.readLine()) != null) {
                    String [] parts = line.split(" ");

                    for(String p: parts){
                            double value = Double.parseDouble(p);
                            
                            value = value - 1;
                            
                            if( value == nodata ) {// no positive match
                                    //value = Integer.MIN_VALUE;
                                    value = 32;
                            }
                            

                            value = value / 32;
                            Parcel par = (Parcel) conflictDiamonds.allLand.get( j, i);
                            par.setDiamondMineDistance(value);					
                            j++; // increase the column count
                    }
                    j = 0; // reset the column count
                    i++; // increase the row count
            	}	
            }
            
            else {   			
                // skip the irrelevant metadata for diamond proximate file
                for (i = 0; i < 6; i++) {
                    line = diamondproximatesurface.readLine();       
                    // format the line appropriately
                    String [] parts = line.split(" ", 2);
                    String trimmed = parts[1].trim();

                    // save the data in the appropriate place
                    if(i == 1) { height = Integer.parseInt( trimmed ); }
                    else if(i == 0) { width = Integer.parseInt( trimmed ); }
                    else if( i == 5) { nodata = Integer.parseInt( trimmed ); }
                    else { continue; }
                }
                
            	//read in diamonds (distance from mines) data and store it in parcels
            	i = 0;
            	j = 0;
	        
            	while((line = diamondproximatesurface.readLine()) != null) {
                    String [] parts = line.split(" ");
	        	
                    for(String p: parts){
                        double value = Double.parseDouble(p);	
                        
                        value = value - 1;

                        if( value == nodata ) {// no positive match
                            //value = Integer.MIN_VALUE;
                            value = 32;
                        }

                        value = value / 32;
                        Parcel par = (Parcel) conflictDiamonds.allLand.get( j, i);
                        par.setDiamondMineDistance(value);					
                        j++; // increase the column count
                    }
                    j = 0; // reset the column count
                    i++; // increase the row count
            	}	
            }
	        
            // skip the irrelevant metadata for population file
            for (i = 0; i < 6; i++) {
                line = populationdist.readLine();                
                // format the line appropriately
                String [] parts = line.split(",", 2);
                String trimmed = parts[1].trim();

                // save the data in the appropriate place
                if(i == 1) { height = Integer.parseInt( trimmed ); }
                else if(i == 0) { width = Integer.parseInt( trimmed ); }
                else if( i == 5) { nodata = Integer.parseInt( trimmed ); }
                else { continue; }
            }
            
            conflictDiamonds.allPopulation = new SparseGrid2D(width, height);

            // read in the population data from the file and store it in parcels
            int r = 0;
            int c = 0;

            while((line = populationdist.readLine()) != null){
               
                String [] parts = line.split(",");

                for(String p: parts){
                    int value = Integer.parseInt(p);					

                    if( value == nodata ) {// no positive match
                        value = 0;
                    }							

                    Parcel par = (Parcel) conflictDiamonds.allLand.get( c, r);
                    Region region = par.getRegion();

                    if ( par.getRegion().getRegionID() == 0 ) {
                            value = 0;
                    }

                    for ( i = 0; i < value; i++ ) {					
                        //if select to place rebels in regions of the country that most closely match where the oppositon group was formed
                        if ( conflictDiamonds.params.global.isInitialRebelPositionActualEvents() && conflictDiamonds.random.nextDouble() < conflictDiamonds.params.global.getOppositionActual() ) {
                            //if running experiment where diamond mines are in Freetown
                            if ( conflictDiamonds.params.global.isProximateExperiment() ) {
                                if ( par.getRegion().getRegionID() == 12 || par.getRegion().getRegionID() == 13 || par.getRegion().getRegionID() == 6 ) {
                                    //create and initialize initial rebels
                                    Rebel newRebel = new Rebel(conflictDiamonds, par, region);
                                    conflictDiamonds.allRebels.add(newRebel);
                                    newRebel.setResidingParcel(par);
                                    par.addPopulation(newRebel);
                                    conflictDiamonds.allPopulation.setObjectLocation(newRebel, par.getX(), par.getY());
                                    conflictDiamonds.schedule.scheduleOnce(newRebel);
                                    newRebel.setOpposition( true );
                                    
                                    newRebel.setResidingRegion(region);
                                    region.addRebels(newRebel);
                                    
                                    newRebel.setInitialRebel(true);
                                    newRebel.getResidingRegion().addInitialRebel(newRebel);
                                    
                         
                                    newRebel.setCurrentGoal(ConflictDiamonds.Goal.Rebel);
                                    newRebel.getResidingRegion().addGoalRebel(newRebel);
                                  
                                }
                            }
                            //if running experiment where diamond mines are distant (in actual locations)
                            else {
                                if ( par.getRegion().getRegionID() == 3 || par.getRegion().getRegionID() == 1 ) { 
                                    //create and initialize initial rebels
                                    Rebel newRebel = new Rebel(conflictDiamonds, par, region);
                                    conflictDiamonds.allRebels.add(newRebel);
                                    newRebel.setResidingParcel(par);
                                    par.addPopulation(newRebel);
                                    conflictDiamonds.allPopulation.setObjectLocation(newRebel, par.getX(), par.getY());
                                    conflictDiamonds.schedule.scheduleOnce(newRebel);
                                    newRebel.setOpposition( true );
                                    
                                    newRebel.setResidingRegion(region);
                                    region.addRebels(newRebel);
                                    
                                    newRebel.setInitialRebel(true);
                                    newRebel.getResidingRegion().addInitialRebel(newRebel);
                                
                                    newRebel.setCurrentGoal(ConflictDiamonds.Goal.Rebel);
                                    newRebel.getResidingRegion().addGoalRebel(newRebel);
                                    
                                }
                            }
                        }
                        //if select to place initial rebels (opposition group) at random locations
                        else if ( conflictDiamonds.params.global.isInitialRebelPositionActualEvents() == false && conflictDiamonds.random.nextDouble() < conflictDiamonds.params.global.getOpposition() ) {
                            //create and initialize initial rebels
                            Rebel newRebel = new Rebel(conflictDiamonds, par, region);
                            conflictDiamonds.allRebels.add(newRebel);
                            newRebel.setResidingParcel(par);
                            par.addPopulation(newRebel);
                            conflictDiamonds.allPopulation.setObjectLocation(newRebel, par.getX(), par.getY());
                            conflictDiamonds.schedule.scheduleOnce(newRebel);
                            newRebel.setOpposition( true );
                            
                            newRebel.setResidingRegion(region);
                            region.addRebels(newRebel);
                            region.addPerson(newRebel);
                            
                            newRebel.setInitialRebel(true);
                            newRebel.getResidingRegion().addInitialRebel(newRebel);
                                                    
                            newRebel.setCurrentGoal(ConflictDiamonds.Goal.Rebel);
                            newRebel.getResidingRegion().addGoalRebel(newRebel);
                            
                        }			
                        //otherwise, agent is not an initial rebel, so create a resident agent
                        else {
                            //create and initialize residents
                            Resident newResident = new Resident(conflictDiamonds, par, region);					
                            conflictDiamonds.allResidents.add(newResident);														
                            newResident.setResidingParcel(par);
                            par.addPopulation(newResident);							
                            conflictDiamonds.allPopulation.setObjectLocation(newResident, par.getX(), par.getY());
                            conflictDiamonds.schedule.scheduleOnce(newResident);
                            newResident.setOpposition( false );
                            
                            //determine the resident's employment status and income level
                            newResident.determineLaborStatistics();
                            newResident.determineIncomeLevel();
                            
                            newResident.setResidingRegion(region);
                            region.addPerson(newResident);
                            
                            if (newResident.getIncomeLevel() == 0) { region.addFoodPoor(newResident); }
                            else if (newResident.getIncomeLevel() == 1) { region.addTotalPoor(newResident); }
                            else { region.addNotPoor(newResident); }
                            
                            //add residents to region objects based on employment and income attributes for tracking and reporting purposes
                            if (newResident.getOtherEmployer() != null) { region.addFormalEmployee(newResident); }
                            if (newResident.getDiamondMiner() != null) { region.addInformalEmployee(newResident); }
                            if (newResident.isMinor()) { region.addMinors(newResident); }
                            if (newResident.isActiveLaborForce()) { region.addActiveLaborMarket(newResident); }
                            if (newResident.isEligible()) { region.addEligibleToMine(newResident); }
                           
                            newResident.setInitialRebel(false);
                            
                            //set initial goal
                            newResident.setCurrentGoal(ConflictDiamonds.Goal.Stay_Home);
                            newResident.getResidingRegion().addGoalStayHome(newResident);
                        }													
                    }					
                    c++; // increase the column count
					
                }

                c = 0; // reset the column count
                r++; // increase the row count
            }		
          
         }
         catch (IOException ex) {
            Logger.getLogger(ConflictDiamondsBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static URL getUrl(String nodesFilename) throws IOException {
        try {
            InputStream nodeStream = ConflictDiamondsData.class.getResourceAsStream(nodesFilename);
            //nodeStream.read(buffer);
            if(!new File("./shapeFiles/").exists()){
                new File("./shapeFiles/").mkdir();
            }
            File targetFile = new File("./shapeFiles/" + nodesFilename.split("/")[nodesFilename.split("/").length - 1]);
            OutputStream outStream = new FileOutputStream(targetFile);
            //outStream.write(buffer);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = nodeStream.read(bytes)) != -1) {
                outStream.write(bytes, 0, read);
            }
            outStream.close();
            nodeStream.close();
            if (nodesFilename.endsWith(".shp")) {
                getUrl(nodesFilename.replace("shp", "dbf"));
                getUrl(nodesFilename.replace("shp", "prj"));
                getUrl(nodesFilename.replace("shp", "sbx"));
                getUrl(nodesFilename.replace("shp", "sbn"));
                getUrl(nodesFilename.replace("shp", "shx"));
            }
            return targetFile.toURI().toURL();
        }catch(Exception e){
            if(nodesFilename.endsWith("shp")){
                e.printStackTrace();
                return null;
            }else {
                //e.printStackTrace();
                return null;
            }
        }

    }
    /**
     * Setup the regions and set agent attributes based on regional data
     * 
     * Source for agent's age is from Sierra Leone's 2004 population and housing census, age and sex structure report (Thomas et al., 2006)
     * Labor attributes are from Sierra Leone's 2004 population and housing census, employment and labor report (Braima et al., 2006)
     * Income levels are from Statistics Sierra Leone’s Annual Statistical Digest (2006)
     *
     * @param r - the region
     * @param conflictDiamonds
     * 
     *
     */
    public static void setupRegions(Region r, ConflictDiamonds conflictDiamonds) {
	
        double percentFoodPoor; //monthly income less than 31,420.42 (Le)
        double percentTotalPoor; //monthly income less than 64,223.17 (Le)
        double percentEmployedFormal; //proportion of total population that is employed aged 15-64
        double percentLaborForce; //proportion of population that is active aged 15-64 (employed plus unemployed)
        double percentMining; //proportion of employed population is in mining
        
        double percent0to4; //percent of population in region that is under 5
        double percent5to6; //percent of population in region that is betwen 5 and 6
        double percent7to14; //percent of population in region that is betwen 7 and 14
        double percent15to17; //percent of population in region that is betwen 15 and 17
        double percent18to64; //percent of population in region that is betwen 18 and 64
        double percent65Over; //percent of population in region that is over 64
		
        //set percent of population that is food poor
        if ( r.getRegionID() == 1 ) percentFoodPoor = .45; 		
        else if ( r.getRegionID() == 2 ) percentFoodPoor = .38;
        else if ( r.getRegionID() == 3 ) percentFoodPoor = .22;
        else if ( r.getRegionID() == 4 ) percentFoodPoor = .63;
        else if ( r.getRegionID() == 5 ) percentFoodPoor = .09;
        else if ( r.getRegionID() == 6 ) percentFoodPoor = .29;
        else if ( r.getRegionID() == 7 ) percentFoodPoor = .2;
        else if ( r.getRegionID() == 8 ) percentFoodPoor = .32;
        else if ( r.getRegionID() == 9 ) percentFoodPoor = .25;
        else if ( r.getRegionID() == 10 ) percentFoodPoor = .35;
        else if ( r.getRegionID() == 11 ) percentFoodPoor = .16;
        else if ( r.getRegionID() == 12 ) percentFoodPoor = .14;
        else if ( r.getRegionID() == 13 ) percentFoodPoor = .15;
        else if ( r.getRegionID() == 14 ) percentFoodPoor = .02;
        else percentFoodPoor = .0;
		
        //set percent of population that is total poor
        if ( r.getRegionID() == 1 ) percentTotalPoor = .92; 		
        else if ( r.getRegionID() == 2 ) percentTotalPoor = .88;
        else if ( r.getRegionID() == 3 ) percentTotalPoor = .66;
        else if ( r.getRegionID() == 4 ) percentTotalPoor = .89;
        else if ( r.getRegionID() == 5 ) percentTotalPoor = .69;
        else if ( r.getRegionID() == 6 ) percentTotalPoor = .77;
        else if ( r.getRegionID() == 7 ) percentTotalPoor = .82;
        else if ( r.getRegionID() == 8 ) percentTotalPoor = .84;
        else if ( r.getRegionID() == 9 ) percentTotalPoor = .64;
        else if ( r.getRegionID() == 10 ) percentTotalPoor = .85;
        else if ( r.getRegionID() == 11 ) percentTotalPoor = .68;
        else if ( r.getRegionID() == 12 ) percentTotalPoor = .59;
        else if ( r.getRegionID() == 13 ) percentTotalPoor = .45;
        else if ( r.getRegionID() == 14 ) percentTotalPoor = .15;
        else percentTotalPoor = .0;
		
        r.setPovertyRate(percentFoodPoor, percentTotalPoor);	

        //set percent of employed population in formal market
        if ( r.getRegionID() == 1 ) percentEmployedFormal = .27; 		
        else if ( r.getRegionID() == 2 ) percentEmployedFormal = .36;
        else if ( r.getRegionID() == 3 ) percentEmployedFormal = .39;
        else if ( r.getRegionID() == 4 ) percentEmployedFormal = .35;
        else if ( r.getRegionID() == 5 ) percentEmployedFormal = .30;
        else if ( r.getRegionID() == 6 ) percentEmployedFormal = .34;
        else if ( r.getRegionID() == 7 ) percentEmployedFormal = .35;
        else if ( r.getRegionID() == 8 ) percentEmployedFormal = .29;
        else if ( r.getRegionID() == 9 ) percentEmployedFormal = .38;
        else if ( r.getRegionID() == 10 ) percentEmployedFormal = .38;
        else if ( r.getRegionID() == 11 ) percentEmployedFormal = .38;
        else if ( r.getRegionID() == 12 ) percentEmployedFormal = .31;
        else if ( r.getRegionID() == 13 ) percentEmployedFormal = .46;
        else if ( r.getRegionID() == 14 ) percentEmployedFormal = .39;
        else percentEmployedFormal = .0;

      
        //minor is anyone under 18 (the youngest child soldiers are about 7)
        //source - Twun-Danso, 2003 and ChildrenSoldiers.pdf
        //distribution of resident's by age group
        if ( r.getRegionID() == 1 )      percent0to4 = .14;
        else if ( r.getRegionID() == 2 ) percent0to4 = .13;
        else if ( r.getRegionID() == 3 ) percent0to4 = .13;
        else if ( r.getRegionID() == 4 ) percent0to4 = .13;
        else if ( r.getRegionID() == 5 ) percent0to4 = .15;
        else if ( r.getRegionID() == 6 ) percent0to4 = .13;
        else if ( r.getRegionID() == 7 ) percent0to4 = .15;
        else if ( r.getRegionID() == 8 ) percent0to4 = .14;
        else if ( r.getRegionID() == 9 ) percent0to4 = .13;
        else if ( r.getRegionID() == 10 )percent0to4 = .15;
        else if ( r.getRegionID() == 11 )percent0to4 = .15; 
        else if ( r.getRegionID() == 12 )percent0to4 = .15;
        else if ( r.getRegionID() == 13 )percent0to4 = .13; 
        else if ( r.getRegionID() == 14 )percent0to4 = .10; 
        else percent0to4 = 0;
        
        if ( r.getRegionID() == 1 )      percent5to6 = .09;
        else if ( r.getRegionID() == 2 ) percent5to6 = .08;
        else if ( r.getRegionID() == 3 ) percent5to6 = .08;
        else if ( r.getRegionID() == 4 ) percent5to6 = .09;
        else if ( r.getRegionID() == 5 ) percent5to6 = .10;
        else if ( r.getRegionID() == 6 ) percent5to6 = .10;
        else if ( r.getRegionID() == 7 ) percent5to6 = .09;
        else if ( r.getRegionID() == 8 ) percent5to6 = .09;
        else if ( r.getRegionID() == 9 ) percent5to6 = .09;
        else if ( r.getRegionID() == 10 )percent5to6 = .09;
        else if ( r.getRegionID() == 11 )percent5to6 = .09;
        else if ( r.getRegionID() == 12 )percent5to6 = .09;
        else if ( r.getRegionID() == 13 )percent5to6 = .09;
        else if ( r.getRegionID() == 14 )percent5to6 = .08;
        else percent5to6 = 0;
        
        if ( r.getRegionID() == 1 )      percent7to14 = .28;
        else if ( r.getRegionID() == 2 ) percent7to14 = .26;
        else if ( r.getRegionID() == 3 ) percent7to14 = .26;
        else if ( r.getRegionID() == 4 ) percent7to14 = .29;
        else if ( r.getRegionID() == 5 ) percent7to14 = .30;
        else if ( r.getRegionID() == 6 ) percent7to14 = .30;
        else if ( r.getRegionID() == 7 ) percent7to14 = .29;
        else if ( r.getRegionID() == 8 ) percent7to14 = .29;
        else if ( r.getRegionID() == 9 ) percent7to14 = .27;
        else if ( r.getRegionID() == 10 )percent7to14 = .27;
        else if ( r.getRegionID() == 11 )percent7to14 = .28;
        else if ( r.getRegionID() == 12 )percent7to14 = .28;
        else if ( r.getRegionID() == 13 )percent7to14 = .26;
        else if ( r.getRegionID() == 14 )percent7to14 = .25;
        else percent7to14 = 0;
        
        if ( r.getRegionID() == 1 )      percent15to17 = .05;
        else if ( r.getRegionID() == 2 ) percent15to17 = .06;
        else if ( r.getRegionID() == 3 ) percent15to17 = .06;
        else if ( r.getRegionID() == 4 ) percent15to17 = .05;
        else if ( r.getRegionID() == 5 ) percent15to17 = .05;
        else if ( r.getRegionID() == 6 ) percent15to17 = .05;
        else if ( r.getRegionID() == 7 ) percent15to17 = .05;
        else if ( r.getRegionID() == 8 ) percent15to17 = .05;
        else if ( r.getRegionID() == 9 ) percent15to17 = .06;
        else if ( r.getRegionID() == 10 )percent15to17 = .05;
        else if ( r.getRegionID() == 11 )percent15to17 = .05;
        else if ( r.getRegionID() == 12 )percent15to17 = .05;
        else if ( r.getRegionID() == 13 )percent15to17 = .06;
        else if ( r.getRegionID() == 14 )percent15to17 = .07;
        else percent15to17 = 0;
        
        if ( r.getRegionID() == 1 )      percent18to64 = .40;
        else if ( r.getRegionID() == 2 ) percent18to64 = .43;
        else if ( r.getRegionID() == 3 ) percent18to64 = .43;
        else if ( r.getRegionID() == 4 ) percent18to64 = .40;
        else if ( r.getRegionID() == 5 ) percent18to64 = .36;
        else if ( r.getRegionID() == 6 ) percent18to64 = .39;
        else if ( r.getRegionID() == 7 ) percent18to64 = .38;
        else if ( r.getRegionID() == 8 ) percent18to64 = .39;
        else if ( r.getRegionID() == 9 ) percent18to64 = .41;
        else if ( r.getRegionID() == 10 )percent18to64 = .38;
        else if ( r.getRegionID() == 11 )percent18to64 = .38;
        else if ( r.getRegionID() == 12 )percent18to64 = .39;
        else if ( r.getRegionID() == 13 )percent18to64 = .43;
        else if ( r.getRegionID() == 14 )percent18to64 = .48;
        else percent18to64 = 0;
        
        if ( r.getRegionID() == 1 )      percent65Over = .04;
        else if ( r.getRegionID() == 2 ) percent65Over = .04;
        else if ( r.getRegionID() == 3 ) percent65Over = .04;
        else if ( r.getRegionID() == 4 ) percent65Over = .04;
        else if ( r.getRegionID() == 5 ) percent65Over = .04;
        else if ( r.getRegionID() == 6 ) percent65Over = .03;
        else if ( r.getRegionID() == 7 ) percent65Over = .04;
        else if ( r.getRegionID() == 8 ) percent65Over = .04;
        else if ( r.getRegionID() == 9 ) percent65Over = .04;
        else if ( r.getRegionID() == 10 )percent65Over = .06;
        else if ( r.getRegionID() == 11 )percent65Over = .05;
        else if ( r.getRegionID() == 12 )percent65Over = .04;
        else if ( r.getRegionID() == 13 )percent65Over = .03;
        else if ( r.getRegionID() == 14 )percent65Over = .02;
        else percent65Over = 0;
       
        //set percent of population that is working age (15-64) and active
        if ( r.getRegionID() == 1 ) percentLaborForce = .33; 		
        else if ( r.getRegionID() == 2 ) percentLaborForce = .38;
        else if ( r.getRegionID() == 3 ) percentLaborForce = .41;
        else if ( r.getRegionID() == 4 ) percentLaborForce = .37;
        else if ( r.getRegionID() == 5 ) percentLaborForce = .36;
        else if ( r.getRegionID() == 6 ) percentLaborForce = .38;
        else if ( r.getRegionID() == 7 ) percentLaborForce = .38;
        else if ( r.getRegionID() == 8 ) percentLaborForce = .35;
        else if ( r.getRegionID() == 9 ) percentLaborForce = .36;
        else if ( r.getRegionID() == 10 ) percentLaborForce = .36;
        else if ( r.getRegionID() == 11 ) percentLaborForce = .39;
        else if ( r.getRegionID() == 12 ) percentLaborForce = .35;
        else if ( r.getRegionID() == 13 ) percentLaborForce = .41;
        else if ( r.getRegionID() == 14 ) percentLaborForce = .35;
        else percentLaborForce = .0;

        //set percent of employed that are in mining
        if ( r.getRegionID() == 1 ) percentMining = .008; 		
        else if ( r.getRegionID() == 2 ) percentMining = .11;
        else if ( r.getRegionID() == 3 ) percentMining = .15;
        else if ( r.getRegionID() == 4 ) percentMining = .023;
        else if ( r.getRegionID() == 5 ) percentMining = .005;
        else if ( r.getRegionID() == 6 ) percentMining = .003;
        else if ( r.getRegionID() == 7 ) percentMining = .002;
        else if ( r.getRegionID() == 8 ) percentMining = .004;
        else if ( r.getRegionID() == 9 ) percentMining = .065;
        else if ( r.getRegionID() == 10 ) percentMining = .003;
        else if ( r.getRegionID() == 11 ) percentMining = .001;
        else if ( r.getRegionID() == 12 ) percentMining = .04;
        else if ( r.getRegionID() == 13 ) percentMining = .03;
        else if ( r.getRegionID() == 14 ) percentMining = .009;
        else percentMining = .0;
        
        //set region specific agent attributes
        r.setLaborStats(percentEmployedFormal, percentLaborForce, percent0to4, percent5to6, percent7to14, percent15to17, percent18to64, percent65Over, percentMining);
    }

}
