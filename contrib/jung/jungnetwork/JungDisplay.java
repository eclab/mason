package jungnetwork;

/* This is a simple display capable of working with MASON and 
 * displaying JUNG graphs. It is composed of merged and 
 * lobotomized verions of JUNG's ZoomDemo and MASON's Display2D 
 * classes. Please refer to original code for details about uncommented code.   */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import sim.display.Display2D;
import sim.display.GUIState;
import sim.display.SimApplet;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.Utilities;
import sim.util.media.PngEncoder;
import edu.uci.ics.jung.visualization.BirdsEyeVisualizationViewer;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.contrib.KKLayout;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.control.ViewScalingControl;

public class JungDisplay extends JComponent implements Steppable {

    public static final ImageIcon CAMERA_ICON = iconFor("Camera.png");

    public static final ImageIcon CAMERA_ICON_P = iconFor("CameraPressed.png");

    private static final long serialVersionUID = 1L;

    public Layout layout;

    public VisualizationViewer viewer;

    public JFrame frame;

    /* Variable which will keep track of time of next update */
    public double nextUpdate = 0;

    GUIState simulation;

    JDialog dialog;

    Stoppable stopper;

    Box header;

    /* The button which snaps a screenshot */
    JButton snapshotButton;

    /* Set to true if we're running on a Mac */
    public static final boolean isMacOSX = isMacOSX();

    /* Set to true if we're running on Windows */
    public static final boolean isWindows = isWindows();

    static ImageIcon iconFor(String name) {
        return new ImageIcon(Display2D.class.getResource(name));
    }

    public void step(SimState state) {

        double currentTime = simulation.state.schedule.time();

        /*
         * Check if it is apropriate time to update, draw only if the display
         * can be seen.
         */

        if ((currentTime > nextUpdate)) {

            if (frame.isShowing()) {

                layout.restart();
                while (!layout.incrementsAreDone()) {
                    layout.advancePositions();
                    }
                viewer.repaint();
                }

            // Compute nextUpdate time.

            nextUpdate = currentTime + ((PreferentialAttachment) simulation.state).updateInterval;
            }
    }

    public JungDisplay(GUIState simulation) {

        this.simulation = simulation;
        this.frame = new JFrame();

        /*
         * Layout is the JUNG's procedure which plans how the graph will be
         * drawn. Check out other layouts, by default we will use Kamada-Kawai.
         */
        layout = new KKLayout(((GraphSimState) simulation.state).graph);

        /*
         * Viewer is AWT Component subclass, doing the rendering, which can be
         * directly added to a JPanel.
         */
        viewer = new VisualizationViewer(layout, new PluggableRenderer());

        Container content = frame.getContentPane();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(viewer);
        content.add(panel);

        /*
         * Additional BirdsEyeView for zooming / panning the graph will be
         * created. See JUNG's ZoomDemo for explanations.
         */

        dialog = new JDialog(frame);
        content = dialog.getContentPane();

        final BirdsEyeVisualizationViewer bird = new BirdsEyeVisualizationViewer(viewer, 0.25f, 0.25f);

        JButton reset = new JButton("No Zoom");
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bird.resetLens();
            }
            });
        final ScalingControl scaler = new ViewScalingControl();
        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(viewer, 1.1f, viewer.getCenter());
            }
            });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(viewer, 0.9f, viewer.getCenter());
            }
            });
        JButton help = new JButton("Help");
        help.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String zoomHelp = "<html><center>Drag the rectangle to pan<p>" + "Drag one side of the rectangle to zoom</center></html>";
                JOptionPane.showMessageDialog(dialog, zoomHelp);
            }
            });
        JPanel controls = new JPanel(new GridLayout(2, 2));
        controls.add(plus);
        controls.add(minus);
        controls.add(reset);
        controls.add(help);
        content.add(bird);
        content.add(controls, BorderLayout.SOUTH);

        JButton zoomer = new JButton("Show Zoom Window");
        zoomer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.pack();
                dialog.setLocation((int) (frame.getLocationOnScreen().getX() + frame.getWidth()), (int) frame.getLocationOnScreen().getY());
                dialog.show();
                bird.initLens();
            }
            });

        header = new Box(BoxLayout.X_AXIS);
        JPanel p = new JPanel();
        p.add(zoomer);
        header.add(p);

        // Add the snapshot button for making pictures of graph.
        snapshotButton = new JButton(CAMERA_ICON);
        snapshotButton.setPressedIcon(CAMERA_ICON_P);
        snapshotButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        snapshotButton.setToolTipText("Create a snapshot (as a PNG file)");
        snapshotButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                takeSnapshot();
            }
            });
        header.add(snapshotButton);

        frame.getContentPane().add(header, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.show();

    }

    public void reset() {

        if (stopper != null) {
            stopper.stop();
            }

        stopper = simulation.scheduleImmediateRepeat(true, this);
        this.nextUpdate = 0;

    }

    public void quit() {
        if (stopper != null)
            stopper.stop();
        stopper = null;
    }

    static boolean isMacOSX() {
        try // we'll try to get certain properties if the security permits it
            {
            return (System.getProperty("mrj.version") != null); // Apple's
            // official approach
            } catch (Throwable e) {
                return false;
                } // Non-Mac Web browsers will fail here
    }

    static boolean isWindows() {
        try // we'll try to get certain properties if the security permits it
            {
            return !isMacOSX() && (System.getProperty("os.name").startsWith("Win"));
            } catch (Throwable e) {
                return false;
                }
    }

    static String getVersion() {
        try {
            return System.getProperty("java.version");
            } catch (Throwable e) {
                return "unknown";
                }
    }

    public void takeSnapshot() {
        if (SimApplet.isApplet) {
            Object[] options = { "Oops" };
            JOptionPane.showOptionDialog(this, "You cannot save snapshots from an applet.", "MASON Applet Restriction", JOptionPane.OK_OPTION,
                                         JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            return;
            }

        int width = viewer.getSize().width;

        System.out.println(width);

        int height = viewer.getSize().height;
        Color bg = getBackground();

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D graphics = bi.createGraphics();
        graphics.setColor(bg);
        graphics.fillRect(0, 0, width, height);
        viewer.paint(graphics);

        // NOW pop up the save window
        FileDialog fd = new FileDialog(this.frame, "Save Snapshot as 24-bit PNG...", FileDialog.SAVE);
        fd.setFile("Untitled.png");
        fd.setVisible(true);

        if (fd.getFile() != null)
            try {
                OutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),
                                                                                                                                             ".png"))));
                PngEncoder tmpEncoder = new PngEncoder(bi, false, PngEncoder.FILTER_NONE, 9);
                stream.write(tmpEncoder.pngEncode());
                stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    }
    }

    }
