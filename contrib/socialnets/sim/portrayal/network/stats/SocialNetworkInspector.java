/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network.stats;
import sim.portrayal.*;
import sim.display.*;
import sim.util.gui.*;
import sim.field.network.*;
import sim.field.network.stats.*;
import java.awt.*;
import javax.swing.*;


/** An inspector for Networks which provides basic network statistical information of interest to the user.
    At present this includes:
    <ul>
    <li>Clustering Coefficient
    <li>Density
    <li>Weighted Diameter (if any)
    <li>Unweighted Diameter
    <li>Symmetry Coefficient
    <li>Weighted Node Eccentricity Distribution
    <li>Unweighted Node Eccentricity Distribution
    <li>In-Degree Distribution
    <li>Out-Degree Distribution
    <li>In-Degree LogLogScale Degree CCDF
    <li>Out-Degree LogLogScale Degree CCDF
    </ul>

    <p>Once set to a Network, this inspector can be attached to a Display2D or Display3D.
*/

public class SocialNetworkInspector extends Inspector
    {
    MiniHistogram ecc=new MiniHistogram();
    MiniHistogram eccw = new MiniHistogram();
    MiniHistogram out=new MiniHistogram();
    MiniHistogram in=new MiniHistogram();
    MiniHistogram logout=new MiniHistogram();
    MiniHistogram login=new MiniHistogram();
    SimpleInspector properties;
    Network net;
    DisplayableNetworkStatistics stat;
    boolean created = false;
        
    public SocialNetworkInspector() { setVolatile(false); }
        
    public void setField(Network field, final GUIState state)
        {
        net = field;
                
        // set it up appropriately
        removeAll();
        revalidate();
        createInspector(state);
        }
        
    void createInspector(final GUIState state)
        {
        stat = new DisplayableNetworkStatistics(net);
        properties = new SimpleInspector(stat,state,"Network Properties");
                
        // create histograms
        LabelledList l = new LabelledList("Network Distributions");
        l.addLabelled("Node Eccentricity (by edge)",ecc);
        l.addLabelled("Node Eccentricity (by weight)",eccw);
        l.addLabelled("In-Degree", in);
        l.addLabelled("Out-Degree",out);
        l.addLabelled("Log-Log In-Degree CCDF", login);
        l.addLabelled("Log-Log Out-Degree CCDF", logout);
        reloadHistograms();
                
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        // make update button
        setLayout(new BorderLayout());
        Box b = new Box(BoxLayout.X_AXIS);
        JButton updateButton = (JButton)makeUpdateButton();
        b.add(updateButton);
        b.add(b.createGlue());
        p.add(b,BorderLayout.NORTH);

        // modify update button height -- stupid MacOS X 1.4.2 bug has icon buttons too big
        NumberTextField sacrificial = new NumberTextField(1,true);
        Dimension d = sacrificial.getPreferredSize();
        d.width = updateButton.getPreferredSize().width;                                
        updateButton.setPreferredSize(d);
        d = sacrificial.getMinimumSize();
        d.width = updateButton.getMinimumSize().width;
        updateButton.setMinimumSize(d);
                
        // load other stuff
        p.add(properties,BorderLayout.CENTER);
        add(p,BorderLayout.NORTH);
        add(l,BorderLayout.CENTER);
        setVolatile(false);
        created = true;
        }

    void reloadHistograms()
        {
        ecc.setBucketsAndLabels(stat.nodeEccentricityDistribution(new UnitEdgeMetric()),stat.nodeEccentricityLabels());
        eccw.setBucketsAndLabels(stat.nodeEccentricityDistribution(new WeightedEdgeMetric()),stat.nodeEccentricityLabels());
        out.setBucketsAndLabels(stat.degreeDistribution(true),stat.degreeDistributionLabels());
        in.setBucketsAndLabels(stat.degreeDistribution(false),stat.degreeDistributionLabels());
        logout.setBucketsAndLabels(stat.loglogScaleDegreeCCDF(true),stat.loglogScaleDegreeCCDFLabels());
        login.setBucketsAndLabels(stat.loglogScaleDegreeCCDF(false),stat.loglogScaleDegreeCCDFLabels());
        }

    public void updateInspector()
        {
        // do nothing if we have no model to display
        if (net==null) return;
                
        // now do the updating
        properties.updateInspector();
        reloadHistograms();
        }
    }
