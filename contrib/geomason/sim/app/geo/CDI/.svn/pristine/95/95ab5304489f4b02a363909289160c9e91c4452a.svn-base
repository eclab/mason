#! /usr/bin/env python
"""
Script for converting raw ArcGIS datafiles to zscore datafiles.  It also
creates a CSV file for finding coefficients using regression in R.
This started out as a class for reading and writing ArcGISgrid data files, but
has evolved into more over time.  The class is still useful if others want to
use it elsewhere.
"""

import os
import sys
import commands
import copy
import math
import numpy

def myMin(a, b):
    if a == None:
        return b
    if b == None:
        return a
    return min(a, b)


def myMax(a, b):
    return max(a, b)


def zscore(npArray):
    return (npArray - numpy.mean(npArray)) / numpy.std(npArray)


Sep=","  # Separator for output.  R uses spaces by default, but can read CSV


#############################################################################
#
# class ArcGISgrid
#
#############################################################################
class ArcGISgrid:
    def __init__(self, filename, toFloat = False):
        # Open the file
        fo = open(filename, "r")

        # Read the header
        self.header_dict = self.readHeader(fo)
        self.ncols = int(self.header_dict['ncols'])
        self.nrows = int(self.header_dict['nrows'])
        self.missing = int(self.header_dict['NODATA_value'])
        self.xllcorner = float(self.header_dict['xllcorner'])
        self.yllcorner = float(self.header_dict['xllcorner'])
        self.cellsize = float(self.header_dict['cellsize'])

        # Read the grid
        self.grid = self.readGrid(fo, self.ncols, self.nrows, toFloat)

        fo.close()


    def readHeader(self, fileObject):
        # I'm assuming that comments are not possible in these files.  True?
        header_dict = {}
        for i in range(6):
            line = fileObject.readline();
            fields = line.split()
            if len(fields) != 2:
                raise Exception("Header line has bad format.")
            [key, value] = fields
            header_dict[key] = float(value)
        return header_dict


    def readRow(self, fileObject, ncols, toFloat = False):
        line = fileObject.readline();
        valList = line.split()
        if len(valList) != ncols:
                raise Exception("Line is the wrong length.")
            
        if toFloat:
            valList = [float(i) for i in valList]
        else:
            valList = [int(i) for i in valList]

        return valList


    def readGrid(self, fileObject, ncols, nrows, toFloat = False):
        grid = range(nrows)   # temporary dummy values
        for row in range(nrows):
            grid[row] = self.readRow(fileObject, ncols, toFloat)
        return numpy.array(grid)


    def write(self, filename):
        outFile = open(filename, "w")

        outFile.write("%-13s %s\n" % ("ncols", self.header_dict['ncols']))
        outFile.write("%-13s %s\n" % ("nrows", self.header_dict['nrows']))
        outFile.write("%-13s %s\n" % ("xllcorner", self.header_dict['xllcorner']))
        outFile.write("%-13s %s\n" % ("yllcorner", self.header_dict['yllcorner']))
        outFile.write("%-13s %s\n" % ("cellsize", self.header_dict['cellsize']))
        outFile.write("%-13s %s\n" % ("NODATA_value", self.header_dict['NODATA_value']))

        for row in self.grid:
            outFile.write(" ".join([str(v) for v in row]) + "\n")

        outFile.close()
        return


#############################################################################
#
# main
#
#############################################################################
def main():
    #if len(sys.argv) < 3:
    #    print "Usage: ArcGISgrid.py <filename1> <filename2>"
    #    sys.exit(1)

    #grid1 = ArcGISgrid(sys.argv[1])
    #grid2 = ArcGISgrid(sys.argv[2])

    #grid1 = ArcGISgrid("cdination10knew.txt")
    #grid2 = ArcGISgrid("cdipop10k.txt")

    # Read all the grids (TODO: Get the smoothed population data)
    #gridNat = ArcGISgrid("data/Canada_ascii_final/cdi_country_f.txt")
    #gridPop = ArcGISgrid("data/Canada_ascii_final/cdi_pop_f.txt")
    #gridElev = ArcGISgrid("data/desireFinal3/canada_dem_f3.txt", True)
    #gridRiver = ArcGISgrid("data/desireFinal3/canada_lake_f3.txt", True)
    #gridPort = ArcGISgrid("data/desireFinal3/canada_port_f3.txt", True)
    #gridTemp = ArcGISgrid("data/desireFinal3/canada_temp_f3.txt", True)

    gridNat = ArcGISgrid("data/cdi_dataNew/cdi_country_e2.txt")
    gridPop = ArcGISgrid("data/cdi_dataNew/cdi_pop_e2.txt")
    gridSmooth = ArcGISgrid("data/pop-smooth-9-9-3.0-2.txt", True)
    gridElev = ArcGISgrid("data/cdi_dataNew/canada_dem_raw.txt", True)
    gridRiver = ArcGISgrid("data/cdi_dataNew/canada_lake_raw.txt", True)
    gridPort = ArcGISgrid("data/cdi_dataNew/canada_port_raw.txt", True)
    gridTemp = ArcGISgrid("data/cdi_dataNew/canada_temp_raw.txt", True)
    #gridElev = ArcGISgrid("data/cdi_dataNew/canada_dem_zscore.txt", True)
    #gridRiver = ArcGISgrid("data/cdi_dataNew/canada_lake_zscore.txt", True)
    #gridPort = ArcGISgrid("data/cdi_dataNew/canada_port_zscore.txt", True)
    #gridTemp = ArcGISgrid("data/cdi_dataNew/canada_temp_zscore.txt", True)

    # input grids
    grids = [gridNat, gridPop, gridSmooth, gridElev, gridRiver, gridPort, gridTemp]

    # output columns
    (ROW, COLUMN, POP, SMOOTH, ELEV, RIVER, PORT, TEMP) = range(len(grids)+1)

    assert( all([gridNat.nrows == g.nrows for g in grids]) )
    assert( all([gridNat.ncols == g.ncols for g in grids]) )


    # Construct the csvTable
    missing = gridNat.missing  # Assuming that all grids use the same value
    canadaCode = 124
    numCanadaCells = 0
    canadaRowBounds = [None, None]
    canadaColBounds = [None, None]
    csvTable = []
    for row in range(gridNat.nrows):
        for col in range(gridNat.ncols):
            country = gridNat.grid[row][col]
            vals = [ g.grid[row][col] for g in grids ]

            # Count total Canada cells and update Canada bounds
            if country == canadaCode:
                numCanadaCells += 1
                canadaRowBounds = [myMin(row, canadaRowBounds[0]),
                                   myMax(row, canadaRowBounds[1])]
                canadaColBounds = [myMin(col, canadaColBounds[0]),
                                   myMax(col, canadaColBounds[1])]

                missingVals = [v == missing for v in vals]

                # Write a line of the CSV file
                if not any(missingVals):
                    #outvals = [csvRowCount, row, col] + vals[1:]
                    thisRow = [row, col] + vals[1:] # skip country code
                    csvTable.append(thisRow)

            # Check for mismatches
            missingVals = [v == missing for v in vals]
            if (any(missingVals) and not all(missingVals[3:])):  # Ignore pop
                outvals = ["Error:", row, col] + vals + ["\n"]
                sys.stderr.write(" ".join([str(v) for v in outvals]))

    #print "Total Canada cells =", numCanadaCells
    #print "Col bounds =", canadaColBounds
    #print "Row bounds =", canadaRowBounds

    csvArray = numpy.array(csvTable)   # Raw data
    zscores = copy.copy(csvArray)
    normZscores = copy.copy(csvArray)

    #print "Elevation bounds =", (min(csvArray[:,ELEV]), max(csvArray[:,ELEV]))
    #print "River bounds =", (min(csvArray[:,RIVER]), max(csvArray[:,RIVER]))
    #print "Port bounds =", (min(csvArray[:,PORT]), max(csvArray[:,PORT]))
    #print "Temperature bounds =", (min(csvArray[:,TEMP]), max(csvArray[:,TEMP]))

    # "Normalize" the data.  i.e. make the distributions normally distributed
    # Note that river and port "directions" are changed.  Higher means farther.

    normZscores[:,SMOOTH] = [math.log(v+1) for v in normZscores[:,SMOOTH]]

    # There is one location with an elevtion of -42, so add before taking log
    normZscores[:,ELEV] = [math.log(v+43) for v in normZscores[:,ELEV]]
    normZscores[:,RIVER] = [math.log(v+1) for v in normZscores[:,RIVER]]
    normZscores[:,PORT] = [(v+1)**0.5 for v in normZscores[:,PORT]]

    # Compute Z-scores
    normZscores[:,SMOOTH] = zscore(normZscores[:,SMOOTH]) # Is this a bad idea?
    normZscores[:,ELEV] = zscore(normZscores[:,ELEV])
    normZscores[:,RIVER] = zscore(normZscores[:,RIVER])
    normZscores[:,PORT] = zscore(normZscores[:,PORT])
    normZscores[:,TEMP] = zscore(normZscores[:,TEMP])

    zscores[:,SMOOTH] = zscore(zscores[:,SMOOTH]) # Is this a bad idea?
    zscores[:,ELEV] = zscore(zscores[:,ELEV])
    zscores[:,RIVER] = zscore(zscores[:,RIVER])
    zscores[:,PORT] = zscore(zscores[:,PORT])
    zscores[:,TEMP] = zscore(zscores[:,TEMP])

    # Output the CSV file to stdout
    head=["Row", "Column", "Population", "SmoothPop", "Elevation", "River", \
          "Port","Temperature"]
    print Sep.join(head)
    for row in zscores:  #normZscores:
        print Sep.join([str(v) for v in row])

    # ### Update grids with the zscore data and write them out ###
    
    # Update the grids with the new data
#    for csvRow in normZscores:
    for csvRow in zscores:
        gridSmooth.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[SMOOTH]
        gridElev.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[ELEV]
        gridRiver.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[RIVER]
        gridPort.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[PORT]
        gridTemp.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[TEMP]

    # Write new grid files.
    gridSmooth.write("canada_smooth_zscore.txt")
    gridElev.write("canada_dem_zscore.txt")
    gridRiver.write("canada_lake_zscore.txt")
    gridPort.write("canada_port_zscore.txt")
    gridTemp.write("canada_temp_zscore.txt")


    # ### Update grids with the normalized zscore data and write them out ###
    
    # Update the grids with the new data
    for csvRow in normZscores:
        gridSmooth.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[SMOOTH]
        gridElev.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[ELEV]
        gridRiver.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[RIVER]
        gridPort.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[PORT]
        gridTemp.grid[csvRow[ROW]][csvRow[COLUMN]] = csvRow[TEMP]

    # Write new grid files.
    gridSmooth.write("canada_smooth_norm_zscore.txt")
    gridElev.write("canada_dem_norm_zscore.txt")
    gridRiver.write("canada_lake_norm_zscore.txt")
    gridPort.write("canada_port_norm_zscore.txt")
    gridTemp.write("canada_temp_norm_zscore.txt")

    return



if __name__ == "__main__":
    main()


