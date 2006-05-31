/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

// Class LSystemData
package sim.app.lsystem;
import java.util.*;

// This class holds the L-system data.
// It is set in RuleUI's calculation thread, and
// in DrawUI's Set button call, then copied into
// a LSystemDrawer instance (on LSystem.start()).  The LSystemDrawer is
// a steppable which draws the L-system post-calculation.
// So, this class holds the data between the time of 
// calculation and draw setting and the actual drawing.

public /*strictfp*/ class LSystemData implements java.io.Serializable
    {
    // expanded code and rule lists
    public ByteList code;
    public ArrayList rules;
    
    // used only for deserialization
    public String seed;
    public int expansions;
    
    // for drawing--basically just passed to LSystemDrawer
    // start facing upward
    public double theta=-/*Strict*/Math.PI/2;
    // length of segment
    public double segsize = 2;
    // rotation angle
    public double angle = /*Strict*/Math.PI/2;
    // start coordinates
    public double x=50,y=50;
    
    // turns a string of chars into a ByteList
    public static void setVector( ByteList v, final String dat )
        {
        v.clear();
        int p = 0;
        for(p=0; p<dat.length(); p++)
            v.add(((byte)dat.substring(p,p+1).charAt(0)));
        }
    
    // turns a ByteList into a string of chars... The reverse of setVector()
    public static String fromVector( ByteList v )
        {
        int p = 0;
        String ret = new String();
        for(p=0; p<v.length; p++)
            ret += String.valueOf((char)v.b[p]);
            
        return ret;
        }
    
    LSystemData()
        {
        code = new ByteList();
        rules = new ArrayList();
        }
    }
// end class
