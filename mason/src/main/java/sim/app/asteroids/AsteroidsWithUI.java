/*
  Copyright 2009 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.asteroids;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import javax.swing.*;
import java.awt.*;
import sim.portrayal.simple.*;
import sim.portrayal.SimplePortrayal2D;
import java.awt.event.*;

/** AsteroidsWithUI is the GUIState of the game.  It holds a single display and its frame; and the
    Controller of the game is set to be a SimpleController rather than a SimpleController.  AsteroidsWithUI
    has a RateAdjuster which it uses to maintain an approximate consistent 60 frames per second.
*/

public class AsteroidsWithUI extends GUIState
    {
    /** The desired FPS */
    public double FRAMES_PER_SECOND = 60;
        
    /** The Display for the game. */
    public Display2D display;
        
    /** The window in which the Display resides. */
    public JFrame displayFrame;

    /** The field portrayal for the asteroids field. */
    public ContinuousPortrayal2D fieldPortrayal = new ContinuousPortrayal2D();
        
    /** The text overlay. */
    public Overlay overlay = new Overlay(this);
        
    /** Default main() for MASON */
    public static void main(String[] args)
        {
        new AsteroidsWithUI().createController();  // randomizes by currentTimeMillis
        }

    /** Creates a SimpleController and starts it playing. */
    public Controller createController()
        {
        SimpleController c = new SimpleController(this);
        c.pressPlay();
        return c;
        }

    /** Creates a default Asteroids game. */
    public AsteroidsWithUI()
        {
        super(new Asteroids(System.currentTimeMillis()));
        }
    
    /** Creates an Asteroids game with the given Asteroids object. */
    public AsteroidsWithUI(SimState state) 
        {
        super(state);
        }

    /** Provides the name of the game. */
    public static String getName() { return "Asteroids"; }

    /** Starts the game. */
    public void start()
        {
        super.start();
        setupPortrayals();
        }

    /** Loads a game.  This is unlikely to occur. */
    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();
        }
        
    /** Creates the portrayal and sets it up.  The portrayal is set up to display
        toroidally to show the asteroids properly.  Additionally a RateAdjuster is
        added to the GUIState minischedule to maintain an approximate rate of speed. */
    public void setupPortrayals()
        {
        Asteroids asteroids = (Asteroids)state;

        fieldPortrayal.setField(asteroids.field);
        fieldPortrayal.setDisplayingToroidally(true);
                
        scheduleRepeatingImmediatelyAfter(new RateAdjuster(FRAMES_PER_SECOND));
                
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }


    /** Sets up the initial game.  Creates a display and sets it to be the only thing
        that can be shown, and sans normal decorations.  Adds listeners for keystrokes. */
    public void init(final Controller c)                        // make it final so we can use it in the anonymous class
        {
        super.init(c);

        // make the displayer
        display = new Display2D(750,750,this)
            {
            public void quit()                                          // we close our controller when we die
                {
                super.quit();
                ((SimpleController) c).doClose();
                }
            };
                        
        display.setBackdrop(Color.black);


        displayFrame = display.createFrame();
        displayFrame.setTitle("Asteroids");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
                
        display.attach( fieldPortrayal, "Asteroids" );
        display.attach( overlay, "Overlay" );
                
                
        // some stuff to make this feel less like MASON:
                
        // delete the header
        display.remove(display.header);
        // delete all listeners
        display.removeListeners();
        // delete the scroll bars
        display.display.setVerticalScrollBarPolicy(display.display.VERTICAL_SCROLLBAR_NEVER);
        display.display.setHorizontalScrollBarPolicy(display.display.HORIZONTAL_SCROLLBAR_NEVER);
        // when we close the window, the application quits
        displayFrame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        // can't resize
        displayFrame.setResizable(false);
        // add antialiasing and interpolation
        display.insideDisplay.setupHints(true, false, false);
        // the window won't be the right size now -- modify it.
        displayFrame.pack();            
                
        // Now we add in the listeners we want
        addListeners(display);
        }
        
                
                
    /** Creates key listeners which issue requests to the simulation. */
    public void addListeners(final Display2D display)
        {
        final Asteroids asteroids = (Asteroids)state;
        final SimpleController cont = (SimpleController) controller;
                
        // Make us able to take focus -- this is by default true usually anyway
        display.setFocusable(true);
                
        // Make us request focus whenever our window comes up
        displayFrame.addWindowListener(new WindowAdapter()
            {
            public void windowActivated(WindowEvent e)
                {
                display.requestFocusInWindow();
                }
            });

        // the display frame has just been set visible so we need to request focus once
        display.requestFocusInWindow();

        display.addKeyListener(new KeyAdapter()
            {
            public void keyReleased(KeyEvent e)
                {
                int c = e.getKeyCode();
                switch(c)
                    {
                    case KeyEvent.VK_UP: 
                        asteroids.actions[0] &= ~Ship.FORWARD;
                        break;
                    case KeyEvent.VK_DOWN: 
                        asteroids.actions[0] &= ~Ship.HYPERSPACE;
                        break;
                    case KeyEvent.VK_LEFT: 
                        asteroids.actions[0] &= ~Ship.LEFT;
                        break;
                    case KeyEvent.VK_RIGHT: 
                        asteroids.actions[0] &= ~Ship.RIGHT;
                        break;
                    case KeyEvent.VK_SPACE: 
                        asteroids.actions[0] &= ~Ship.FIRE;
                        break;
                        /*
                          case KeyEvent.VK_W: 
                          asteroids.actions[1] &= ~Ship.FORWARD;
                          break;
                          case KeyEvent.VK_S: 
                          asteroids.actions[1] &= ~Ship.HYPERSPACE;
                          break;
                          case KeyEvent.VK_A: 
                          asteroids.actions[1] &= ~Ship.LEFT;
                          break;
                          case KeyEvent.VK_D: 
                          asteroids.actions[1] &= ~Ship.RIGHT;
                          break;
                          case KeyEvent.VK_C: 
                          asteroids.actions[0] &= ~Ship.FIRE;
                          break;
                        */
                    default:            // do nothing
                        break;
                    }
                }
                                
            public void keyPressed(KeyEvent e)
                {
                int c = e.getKeyCode();
                switch(c)
                    {
                    case KeyEvent.VK_UP: 
                        asteroids.actions[0] |= Ship.FORWARD;
                        break;
                    case KeyEvent.VK_DOWN: 
                        asteroids.actions[0] |= Ship.HYPERSPACE;
                        break;
                    case KeyEvent.VK_LEFT: 
                        asteroids.actions[0] |= Ship.LEFT;
                        break;
                    case KeyEvent.VK_RIGHT: 
                        asteroids.actions[0] |= Ship.RIGHT;
                        break;
                    case KeyEvent.VK_SPACE: 
                        asteroids.actions[0] |= Ship.FIRE;
                        break;
                        /*
                          case KeyEvent.VK_W: 
                          asteroids.actions[1] = Ship.FORWARD;
                          break;
                          case KeyEvent.VK_S: 
                          asteroids.actions[1] = Ship.HYPERSPACE;
                          break;
                          case KeyEvent.VK_A: 
                          asteroids.actions[1] = Ship.LEFT;
                          break;
                          case KeyEvent.VK_D: 
                          asteroids.actions[1] = Ship.RIGHT;
                          break;
                          case KeyEvent.VK_C: 
                          asteroids.actions[0] = Ship.FIRE;
                          break;
                        */
                    case KeyEvent.VK_R:             // Reset the board.  Easiest way: stop and play, which calls start()
                        cont.pressStop();
                        cont.pressPlay();
                        break;
                    case KeyEvent.VK_P:             // Pause or unpause the game
                        cont.pressPause();
                        break;
                    case KeyEvent.VK_M:             // Call forth MASON's new simulation window
                        if (cont.getPlayState() != cont.PS_PAUSED)  // pause it!
                            cont.pressPause();
                        cont.doNew();

                        // the MASON window belongs to our frame, so Java stupidly doesn't send
                        // us a window activated event when the MASON window is closed and our
                        // frame comes to the fore again.  So we have to manually do request
                        // focus again here.
                        display.requestFocusInWindow();
                        break;
                    default: // do nothing
                        break;
                    }
                }
            });
        }

    /** Quits the game. */
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
