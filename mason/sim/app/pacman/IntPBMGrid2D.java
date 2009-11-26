/*
  Copyright 2009 by Christopher Vo and Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;

import java.io.*;
import sim.field.grid.*;

/** A quick and dirty variation of IntGrid2D which reads black-and-white PBM files as
    1's and 0's in a 2D grid.  */
        
public class IntPBMGrid2D extends IntGrid2D
    {
    public IntPBMGrid2D(String filename)
        {
        super(0, 0);
        read(filename);
        }
                
    public IntPBMGrid2D(File file)
        {
        super(0, 0);
        read(file);
        }
        
    public void read(String filename) 
        {
        try { read(new BufferedReader(new FileReader(filename))); }
        catch (IOException e) { throw new RuntimeException("Whoops!"); }
        }

    public void read(File file) 
        {
        try { read(new BufferedReader(new FileReader(file))); }
        catch (IOException e) { throw new RuntimeException("Whoops!"); }
        }
                
    public void read(BufferedReader in) {
        try {
            int w = 0, h = 0, tmp = 0;

            // get magic number
            String line = in.readLine().trim();
            if (!line.equals("P1"))
                throw new IOException("The image's magic number is not P1.");

            // skip a line
            in.readLine();

            // get image size
            line = in.readLine().trim();
            String[] t = line.split("\\s+", 2);
            w = Integer.valueOf(t[0]);
            h = Integer.valueOf(t[1]);

            // resize grid
            if (w > 1 && h > 1) {
                width = w;
                height = h;
                field = new int[w][h];
                } else {
                throw new IOException("The w and h of the image are invalid.");
                }

            // read ints
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    while (((tmp = in.read()) != -1) && tmp != '0' && tmp != '1')
                        /* skip things that are no 0's or 1's */;
                    field[x][y] = (tmp == -1) ? 0 : ((tmp == '0') ? 0 : 1);
                    }
                }
            } catch (Exception e) {
            // print the exception
            e.printStackTrace();
            // create a 100x100 default grid
            width = height = 100;
            field = new int[100][100];
            for (int i = 0; i < 100; i++)
                for (int j = 0; j < 100; j++)
                    field[i][j] = 0;
            }
        }
    }
