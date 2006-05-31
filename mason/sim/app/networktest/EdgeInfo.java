/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.networktest;

public class EdgeInfo implements java.io.Serializable
    {
    String label;
    public String getLabel() { return label; }
    public void setLabel( final String id ) { label = id; }
    public EdgeInfo(String val) { label = val; }
    public String toString() { return label; }
    }
