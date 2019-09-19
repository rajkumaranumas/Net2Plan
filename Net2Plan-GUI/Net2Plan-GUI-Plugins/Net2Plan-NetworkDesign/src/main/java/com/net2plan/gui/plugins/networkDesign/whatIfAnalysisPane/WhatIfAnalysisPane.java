/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/


package com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.ParameterValueDescriptionPanel;
import com.net2plan.gui.utils.RunnableSelector;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.sim.SimKernel;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Triple;

/**
 * Targeted to evaluate network designs from the offline tool simulating the
 * network operation. Different aspects such as network resilience,
 * connection-admission-control and time-varying traffic resource allocation,
 * or even mix of them, can be analyzed using the online simulator.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class WhatIfAnalysisPane extends JPanel implements ActionListener
{
    private final GUINetworkDesign callback;
    private Thread simThread;
    private ParameterValueDescriptionPanel simulationConfigurationPanel;
    private RunnableSelector statelessSimulatorPanel;
    private final JCheckBox checkBox_whatIfActivated;
    private SimKernel simKernel;

    public WhatIfAnalysisPane(GUINetworkDesign callback)
    {
        super();
        this.callback = callback;

        File ALGORITHMS_DIRECTORY = new File(IGUIModule.CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        ALGORITHMS_DIRECTORY = ALGORITHMS_DIRECTORY.isDirectory() ? ALGORITHMS_DIRECTORY : IGUIModule.CURRENT_DIR;

        statelessSimulatorPanel = new RunnableSelector(SimKernel.getEventProcessorLabel(), "File", IAlgorithm.class , ALGORITHMS_DIRECTORY, new ParameterValueDescriptionPanel());

        simulationConfigurationPanel = new ParameterValueDescriptionPanel();
        simulationConfigurationPanel.setParameters(new ArrayList<> ());

        final JPanel upperButtonPlusLabelPanel = new JPanel();
        checkBox_whatIfActivated = new JCheckBox("Activate what-if analysis mode");
        checkBox_whatIfActivated.setToolTipText("Activate/Deactivate What-if analysis tool");
        checkBox_whatIfActivated.addActionListener(this);
        checkBox_whatIfActivated.setSelected(callback.getVisualizationState().isWhatIfAnalysisActive());
        // Negate the last selection and run the listener.
//        btn_whatIfActivated.doClick();
        upperButtonPlusLabelPanel.setLayout(new BorderLayout());
        upperButtonPlusLabelPanel.add(new JLabel ("  "), BorderLayout.NORTH);
        upperButtonPlusLabelPanel.add(checkBox_whatIfActivated, BorderLayout.CENTER);
        upperButtonPlusLabelPanel.add(new JLabel ("  "), BorderLayout.SOUTH);

        final JTextArea upperText = new JTextArea();
        upperText.setFont(new JLabel().getFont());
        upperText.setBackground(new JLabel().getBackground());
        upperText.setLineWrap(true);
        upperText.setEditable(false);
        upperText.setWrapStyleWord(true);
        final String NEWLINE = String.format("%n");
        upperText.setText("The what-if analysis tool permits plugging in a network simulator algorithm "
                + "that is called after any change in the design in the user interface." + NEWLINE
                + "The simulator will receive the current design, and should update it to reflect how the network " + NEWLINE
                + "would react e.g. to the current failed elements and traffic demands." + NEWLINE + NEWLINE
                + "Note: If the simulation algorithm fails when computing the new network design, the design is unchanged."
        );
        this.setLayout(new BorderLayout());
        this.add(upperButtonPlusLabelPanel, BorderLayout.NORTH);

        final JSplitPane aux_Panel = new JSplitPane();
        aux_Panel.setLeftComponent(new JScrollPane(upperText));
        aux_Panel.setRightComponent(statelessSimulatorPanel);

        aux_Panel.setOrientation(JSplitPane.VERTICAL_SPLIT);
        aux_Panel.setResizeWeight(0.3);
        aux_Panel.setEnabled(false);
        aux_Panel.setDividerLocation(0.3);
        aux_Panel.setDividerSize(0      );

        this.add(aux_Panel, BorderLayout.CENTER);
    }

    public void whatIfSomethingModified() 
    {
    	runSimulation();
    }

    /**
     * Runs a short simulation to perform the what-if analysis. At the end, the resulting netplan is set
     *
     * @param eventToRun
     */
    private void runSimulation()
    {
    	final NetPlan originalNpCopy = callback.getDesign().copy();
    	try
    	{
	    	final NetPlan np = callback.getDesign();
	        final Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
            final Triple<File, String, Class> aux = statelessSimulatorPanel.getRunnable();
            System.out.println("To load: " + aux);
            final IAlgorithm algorithmInstance = ClassLoaderUtils.getInstance(aux.getFirst(), aux.getSecond(), IAlgorithm.class , null);
            Map<String, String> eventProcessorParameters = statelessSimulatorPanel.getRunnableParameters();
            System.out.println("eventProcessorParameters: " + eventProcessorParameters);
            algorithmInstance.executeAlgorithm(np, eventProcessorParameters, net2planParameters);
        } catch (Throwable ex)
        {
        	ex.printStackTrace();
        	callback.getDesign().assignFrom(originalNpCopy);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final Object src = e.getSource();
        if (src == checkBox_whatIfActivated)
        {
            final VisualizationState vs = callback.getVisualizationState();
            if (callback.inOnlineSimulationMode()) checkBox_whatIfActivated.setSelected(false);
            vs.setWhatIfAnalysisActive(checkBox_whatIfActivated.isSelected());
            statelessSimulatorPanel.setEnabled(checkBox_whatIfActivated.isSelected());
        }
    }

}
