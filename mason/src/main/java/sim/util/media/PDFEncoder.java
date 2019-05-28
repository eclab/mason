/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media;

import java.awt.*;
import java.io.*;
import java.awt.geom.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*; 
import org.jfree.chart.*;

public class PDFEncoder
    {
    public static void generatePDF(Component component, File file )
        {
        int width = component.getWidth();
        int height = component.getHeight();
        try
            {
            Document document = new Document(new com.lowagie.text.Rectangle(width,height));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            document.addAuthor("MASON");
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(width, height); 
            Graphics g2 = tp.createGraphics(width, height, new DefaultFontMapper());
            component.paint(g2);
            g2.dispose();
            cb.addTemplate(tp, 0, 0);
            document.close();
            }
        catch( Exception e )
            {
            e.printStackTrace();
            }
        }
                
    /* Generates PDF from the chart, saving out to the given file.  width and height are the
       desired width and height of the chart in points. */
    public static void generatePDF( JFreeChart chart, int width, int height, File file )
        {
        try
            {
            Document document = new Document(new com.lowagie.text.Rectangle(width,height));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            document.addAuthor("MASON");
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(width, height); 
            Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
            Rectangle2D rectangle2D = new Rectangle2D.Double(0, 0, width, height); 
            chart.draw(g2, rectangle2D);
            g2.dispose();
            cb.addTemplate(tp, 0, 0);
            document.close();
            }
        catch( Exception e )
            {
            e.printStackTrace();
            }
        }

    }
