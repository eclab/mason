/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.text.*;

/**
   HTMLBrowser is a simple web browser which lets the user click on links and which provides
   a Back button when appropriate.  That's it!
*/

public class HTMLBrowser extends JPanel
    {
    java.util.Stack stack = new java.util.Stack();
    JEditorPane infoPane;
    JScrollPane scroll;
        
    public void setText(Object HTMLTextOrURL)
        {
        if (HTMLTextOrURL == null) HTMLTextOrURL = "<html><body bgcolor='white'></body></html>";
        stack = new java.util.Stack();
        // delete any notion of a URL.  What a pain -- this is so backwards!
        // This is because JEditorPane's setText doesn't eliminate the
        // stream description property of the text (a bug), so the system
        // still thinks the URL is valid.
        infoPane.setContentType("text/html");
        Document d = infoPane.getEditorKit().createDefaultDocument();
        //if (d instanceof AbstractDocument)
        //      {
        //      ((AbstractDocument)d).setAsynchronousLoadPriority(1);
        //      }
        infoPane.setDocument(d);
        if (HTMLTextOrURL instanceof String)
            infoPane.setText((String)HTMLTextOrURL);
        else if (HTMLTextOrURL instanceof URL)
            try
                {
                infoPane.setPage((URL)HTMLTextOrURL);
                }
            catch (IOException e) 
                { 
                e.printStackTrace(); 
                infoPane = new JEditorPane(); 
                }
        else
            {
            new RuntimeException("Info object was neither a string nor a URL").printStackTrace();
            infoPane = new JEditorPane();
            }

        // override a bug in JEditorPane which scrolls to the bottom on all subsequent Consoles
        infoPane.getCaret().setDot(0);
        }
                
                
                
    /** Constructs an HTMLBrowser using either an HTML string or a URL */
    public HTMLBrowser(final Object HTMLTextOrURL)
        {
        infoPane = new JEditorPane();
        setText(HTMLTextOrURL);
                
        infoPane.setEditable(false);
        scroll = new JScrollPane(infoPane);        
        setLayout(new BorderLayout());
        add(scroll,BorderLayout.CENTER);
        // override a bug in JEditorPane which scrolls to the bottom on all subsequent Consoles
        infoPane.getCaret().setDot(0);

        // add a back button and 
        JButton backButton = new JButton("Back");
        final Box backButtonBox = new Box(BoxLayout.X_AXIS);
        backButtonBox.add(backButton);
        backButtonBox.add(Box.createGlue());

        // make the hyperlinks active
        infoPane.addHyperlinkListener(new HyperlinkListener()
            {
            public void hyperlinkUpdate( HyperlinkEvent he ) 
                {
                HyperlinkEvent.EventType type = he.getEventType();
                if (type == HyperlinkEvent.EventType.ENTERED) 
                    {
                    infoPane.setCursor(Cursor.getPredefinedCursor( Cursor.HAND_CURSOR) );
                    } 
                else if (type == HyperlinkEvent.EventType.EXITED) 
                    {
                    infoPane.setCursor( Cursor.getDefaultCursor() );
                    } 
                else // clicked on it!
                    {
                    java.net.URL url = he.getURL();
                    try
                        {
                        infoPane.getEditorKit().createDefaultDocument();
                        infoPane.setPage(url);
                        if (stack.isEmpty())
                            {
                            // show back button
                            add(backButtonBox,BorderLayout.SOUTH);
                            revalidate();
                            }
                        stack.push(url);
                        }
                    catch (Exception e)
                        {
                        e.printStackTrace();
                        java.awt.Toolkit.getDefaultToolkit().beep();
                        }
                    }
                }
            });

        // code for when the user presses the "Back" button
        backButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent ae)
                {
                try
                    {
                    stack.pop();
                    if (stack.isEmpty())
                        {
                        // hide back button
                        remove(backButtonBox);
                        revalidate();
                        setText(HTMLTextOrURL);
                        }
                    else infoPane.setPage((java.net.URL)(stack.peek()));
                    }
                catch (Exception e)
                    {
                    System.err.println("WARNING: This should never happen." + e);
                    }
                }
            });
        }
    }
