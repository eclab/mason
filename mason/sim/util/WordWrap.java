/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.util.*;
import java.awt.*;

interface WordWrapScanner
    {
    // returns the index of the LAST character to fit within
    // the boundaries starting at startIndex and moving to a string
    // length defined by nextLoc
    public int scan(StringBuffer buf, int startIndex, double nextLoc);
    }

class CharColumnScanner implements WordWrapScanner
    {
    /** nextLoc is expected to be the number of columns */
    // presently does a simple linear search
    public int scan(StringBuffer buf, int start, double nextLoc)
        {
        int nextIndex = start + ((int)nextLoc) - 1;
        if (buf.length() <= nextIndex)
            nextIndex = buf.length() - 1;
        // determine if there's a \n first, and if so, use it
        // as the breaking point
        for(int x=start; x < nextIndex; x++)
            if (buf.charAt(x) == '\n')
                nextIndex = x - 1;
        return nextIndex;
        }
    }

class FontMetricsScanner implements WordWrapScanner
    {
    FontMetrics metrics;
    public FontMetricsScanner(FontMetrics metrics)
        {
        this.metrics = metrics;
        }
        
    // nextLoc is expected to be the number of columns
    // presently does a simple linear search, very expensive.
    public int scan(StringBuffer buf, int start, double nextLoc)
        {
        // gather the array
        char[] chars = new char[buf.length() - start];
        buf.getChars(start,buf.length(), chars, 0);

        // start computing the lengths.  Don't bother if we get to a \n
        for(int x = 0; x < chars.length; x++)
            {
            if (chars[x] == '\n')
                return start + x - 1;
            int len = metrics.charsWidth(chars,0,x+1);
            if (len > nextLoc)  // including x was bad
                return start + x - 1;
            }
        // everything fit
        return buf.length() - 1;
        }
    }

/** WordWrap is a simple word-wrapping class which provides word-wrap either to columns of raw text; or to some number
    of pixels (given a font).  It's not terribly efficient but works reasonably well.  Tabs are considered to be the same length as spaces.  The input is the string to wrap, and the output is the same string with returns inserted in the appropriate location. */

// in the future we might provide arbitrary scanners to determine maximal length, but right now it's just these two.
    
public class WordWrap implements java.io.Serializable
    {
        
    /** Wraps a string to a given number of columns. */
    public static String wrap(String string, int numColumns)
        {
        return wrap(string, numColumns, new CharColumnScanner());
        }
    
    /** Wraps a string to a given number of pixels in width, given a font whose metrics are provided as well. */
    public static String wrap(String string, int numPixels, FontMetrics metrics)
        {
        return wrap(string, numPixels, new FontMetricsScanner(metrics));
        }
    
    static String wrap(String string, double desiredLength, WordWrapScanner scanner)
        {
        StringBuffer buf = new StringBuffer(string);
        
        int s = 0;
        int e;
        
        while(true)
            {
            if (s==buf.length())  // gone too far
                return buf.toString();
                
            e = scanner.scan(buf,s,desiredLength) + 1;
            
            if (e>=buf.length())  // up to last character
                return buf.toString();
            
            char ce = buf.charAt(e);
            
            if (ce=='\n')
                {
                s = e + 1;
                }
            else if (Character.isWhitespace(ce))
                {
                int top = e;
                while(top < buf.length() - 1 &&  // not last character
                    Character.isWhitespace(buf.charAt(top)) &&  // it's whitespace
                    buf.charAt(top) != '\n')  // but it's not an '\n'
                    top++;
                buf.delete(e,top);  // yank out all whitespace to the next value
                if (buf.charAt(e)!='\n') // not already a \n, need to add one
                    buf.insert(e,'\n');
                s = e + 1;
                }
            else // not whitespace
                {
                int l = e;
                while(l > s && // don't back beyond s
                    !Character.isWhitespace(buf.charAt(l)))  // not a whitespace char
                    l--;
                if (l==s && !Character.isWhitespace(buf.charAt(l))) // oops, all non-whitespace, must split
                    {
                    buf.insert(e, '\n');
                    s = e + 1;
                    }
                else
                    {
                    buf.insert(l+1, '\n');
                    s = l + 2;
                    }
                }
            }
        }
    
    /** A useful auxillary method: once you're word-wrapped your text, you can use this to break it into
        multiple strings at the \n position. */
    public static String[] split(String str)
        {
        StringTokenizer tok = new StringTokenizer(str, "\n");
        String[] s = new String[tok.countTokens()];
        int x=0;
        while(tok.hasMoreTokens())
            s[x++] = tok.nextToken();
        return s;
        }
    
    /** A useful auxillary method: once you've word-wrapped your text, you can use this to convert it into
        'HTML' style, where <b>&lt;</b> is converted into <b>&amp;lt;</b>, <b>&amp;</b> is converted into
        <b>&amp;amp;</b>, and <b>\n</b> or <b>\r</b> are converted into <b>&lt;br></b>.
        
        <p>You can use this to make multi-line buttons and multi-line menus like this:
        
        <pre><tt>
        String myText = "Here is the big text that we want to have word-wrapped";
        int myNumberOfPixels = 50; // our word-wrap pixel length
        JButton button = new JButton();
        button.setText("&lt;html>" + sim.util.WordWrap.toHTML(sim.util.WordWrap.wrap(
        myText, myNumberOfPixels, button.getFontMetrics(button.getFont()))) +
        "&lt;/html>");
        </tt></pre>
        
        <p>On MacOS X Java 1.3 (which will go away soon), this isn't sufficient -- the default
        font is incorrectly Times Roman, and the default point size is slightly off.  You can get the
        font fixed, but not the size, with:

        <pre><tt>
        String myText = "Here is the big text that we want to have word-wrapped";
        int myNumberOfPixels = 50; // our word-wrap pixel length
        JButton button = new JButton();
        button.setText("&lt;html>&lt;font face=\"" + 
        sim.util.WordWrap.toHTML(button.getFont().getFamily()) + "\">" + 
        sim.util.WordWrap.toHTML(sim.util.WordWrap.wrap(
        myText, myNumberOfPixels, button.getFontMetrics(button.getFont()))) +
        "&lt;/font>&lt;/html>");
        </tt></pre>
    */
    
    public static String toHTML(final String text)
        {
        StringBuffer buf = new StringBuffer();
        char[] c = text.toCharArray();
        for(int x=0;x<c.length;x++)
            {
            switch(c[x])
                {
                case (char)10:
                case (char)13:
                    buf.append("<br>");
                    break;    
                case '&':
                    buf.append("&amp;");
                    break;
                case '<':
                    buf.append("&lt;");
                    break;
                default:
                    buf.append(c[x]);
                    break;
                }
            }
        return buf.toString();
        }
    
    }

