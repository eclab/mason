/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lsystem;

public class ByteList implements java.io.Serializable
    {
    public byte[] b;
    public int length = 0;
    
    ByteList()
        {
        b = new byte[16];
        }
    
    ByteList(int size)
        {
        b = new byte[size];
        }
    
    ByteList(ByteList a)
        {
        b = new byte[a.b.length];
        System.arraycopy(a.b,0,b,0,a.length);
        length = a.length;
        }
    
    public void resize(int toAtLeast)
        {
        if (b.length >= toAtLeast)  // already at least as big as requested
            return;

        if (b.length * 2 > toAtLeast)  // worth doubling
            toAtLeast = b.length * 2;

        // now resize
        byte[] newb = new byte[toAtLeast];
        System.arraycopy(b,0,newb,0,length);
        b=newb;
        }
        
    // assume only one addition at a time
    public void add(byte n)
        {
        if (length + 1 > b.length)
            resize(length + 1);
        b[length] = n;
        length++;
        }
                        
    public void addAll(ByteList a)
        {
        if (length + a.length > b.length)
            resize(length + a.length);
        System.arraycopy(a.b, 0, b,length,a.length);
        length += a.length;
        }
    
    public void clear()
        {
        b = new byte[16];
        length = 0;
        }
                
    static final long serialVersionUID = -7841332939713409966L; // hard-coded for backwards-compatability
    }
