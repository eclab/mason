package sim.util.media;

import java.awt.*;
import java.io.*;
import java.awt.geom.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*; 

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
            Rectangle2D rectangle2D = new Rectangle2D.Double(0, 0, width, height); 
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
    }
