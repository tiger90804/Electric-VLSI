/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayersTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ColorPatternPanel;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class to handle the "Colors and Layers" tab of the Preferences dialog.
 */
public class LayersTab extends PreferencePanel
{
	private static HashMap layerMap;
	private HashMap transAndSpecialMap;
	private HashMap colorMapMap;
	private ColorPatternPanel colorAndPatternPanel;

	/** Creates new form LayerTab */
	public LayersTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make the color/pattern panel
		colorAndPatternPanel = new ColorPatternPanel(true);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 1;
		gbc.weightx = 1;    gbc.weighty = 1;
		gbc.gridwidth = 4;  gbc.gridheight = 1;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		layers.add(colorAndPatternPanel, gbc);

		layerMap = new HashMap();
		transAndSpecialMap = new HashMap();
		colorMapMap = new HashMap();
		layerName.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { layerSelected(); }
		});
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			technology.addItem(tech.getTechName());
		}
		technology.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { setTechnology(); }
		});
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return layers; }

	/** return the name of this preferences tab. */
	public String getName() { return "Layers"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the tab.
	 */
	public void init()
	{
		// make a map of all layers
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				layerName.addItem(layer.getName());
                ColorPatternPanel.Info li = new ColorPatternPanel.Info(layer.getGraphics());
                layerMap.put(layer, li);
			}

			// make an entry for the technology's color map
			Color [] map = new Color[tech.getNumTransparentLayers()];
			Color [] fullMap = tech.getColorMap();
			for(int i=0; i<map.length; i++)
				map[i] = fullMap[1<<i];
			colorMapMap.put(tech, map);
		}

		// add the special layers
		transAndSpecialMap.put("Special: BACKGROUND", new ColorPatternPanel.Info(User.getColorBackground()));
		transAndSpecialMap.put("Special: GRID", new ColorPatternPanel.Info(User.getColorGrid()));
		transAndSpecialMap.put("Special: HIGHLIGHT", new ColorPatternPanel.Info(User.getColorHighlight()));
        transAndSpecialMap.put("Special: MOUSE-OVER HIGHLIGHT", new ColorPatternPanel.Info(User.getColorMouseOverHighlight()));
		transAndSpecialMap.put("Special: PORT HIGHLIGHT", new ColorPatternPanel.Info(User.getColorPortHighlight()));
		transAndSpecialMap.put("Special: TEXT", new ColorPatternPanel.Info(User.getColorText()));
		transAndSpecialMap.put("Special: INSTANCE OUTLINES", new ColorPatternPanel.Info(User.getColorInstanceOutline()));
		transAndSpecialMap.put("Special: WAVEFORM BACKGROUND", new ColorPatternPanel.Info(User.getColorWaveformBackground()));
		transAndSpecialMap.put("Special: WAVEFORM FOREGROUND", new ColorPatternPanel.Info(User.getColorWaveformForeground()));
		transAndSpecialMap.put("Special: WAVEFORM STIMULI", new ColorPatternPanel.Info(User.getColorWaveformStimuli()));
		transAndSpecialMap.put("Special: WAVEFORM OFF STRENGTH", new ColorPatternPanel.Info(User.getColorWaveformStrengthOff()));
		transAndSpecialMap.put("Special: WAVEFORM NODE (WEAK) STRENGTH", new ColorPatternPanel.Info(User.getColorWaveformStrengthNode()));
		transAndSpecialMap.put("Special: WAVEFORM GATE STRENGTH", new ColorPatternPanel.Info(User.getColorWaveformStrengthGate()));
		transAndSpecialMap.put("Special: WAVEFORM POWER STRENGTH", new ColorPatternPanel.Info(User.getColorWaveformStrengthPower()));
		transAndSpecialMap.put("Special: WAVEFORM CROSSPROBE LOW", new ColorPatternPanel.Info(User.getColorWaveformCrossProbeLow()));
		transAndSpecialMap.put("Special: WAVEFORM CROSSPROBE HIGH", new ColorPatternPanel.Info(User.getColorWaveformCrossProbeHigh()));
		transAndSpecialMap.put("Special: WAVEFORM CROSSPROBE UNDEFINED", new ColorPatternPanel.Info(User.getColorWaveformCrossProbeX()));
		transAndSpecialMap.put("Special: WAVEFORM CROSSPROBE FLOATING", new ColorPatternPanel.Info(User.getColorWaveformCrossProbeZ()));

		technology.setSelectedItem(Technology.getCurrent().getTechName());
	}

	/**
	 * Method called when the Technology popup changes.
	 */
	private void setTechnology()
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		layerName.removeAllItems();

		// add all layers in the technology
		for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
		{
			Layer layer = (Layer)lIt.next();
			layerName.addItem(layer.getName());
		}

		// add special layer names
		List specialList = new ArrayList();
		for(Iterator it = transAndSpecialMap.keySet().iterator(); it.hasNext(); )
			specialList.add(it.next());
		Collections.sort(specialList, TextUtils.STRING_NUMBER_ORDER);
		for(Iterator it = specialList.iterator(); it.hasNext(); )
		{
			String name = (String)it.next();
			layerName.addItem(name);
		}

		// report the map for the technology
		Color [] map = (Color [])colorMapMap.get(tech);
		colorAndPatternPanel.setColorMap(map);
		layerSelected();
	}

	/**
	 * Method called when the Layer popup changes.
	 */
	private void layerSelected()
	{
		String techName = (String)technology.getSelectedItem();
		Technology tech = Technology.findTechnology(techName);
		if (tech == null) return;

		String name = (String)layerName.getSelectedItem();
		ColorPatternPanel.Info li = (ColorPatternPanel.Info)transAndSpecialMap.get(name);
		if (li == null)
		{
			Layer layer = tech.findLayer(name);
			li = (ColorPatternPanel.Info)layerMap.get(layer);
		}
		if (li == null) return;
		colorAndPatternPanel.setColorPattern(li);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Layers tab.
	 */
	public void term()
	{
		boolean changed = false;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				ColorPatternPanel.Info li = (ColorPatternPanel.Info)layerMap.get(layer);
				if (li.updateGraphics())
					changed = true;
			}

			// determine the original colors for this technology
			Color [] fullOrigMap = tech.getColorMap();
			Color [] origMap = new Color[tech.getNumTransparentLayers()];
			for(int i=0; i<origMap.length; i++)
				origMap[i] = fullOrigMap[1<<i];

			// see if any colors changed
			boolean mapChanged = false;
			Color [] map = (Color [])colorMapMap.get(tech);
			for(int i=0; i<map.length; i++)
				if (map[i].getRGB() != origMap[i].getRGB()) mapChanged = true;
			if (mapChanged)
				tech.setColorMapFromLayers(map);
		}

		// also get any changes to special layers
		int c = 0;
		if ((c = specialMapColor("Special: BACKGROUND", User.getColorBackground())) >= 0)
			{ User.setColorBackground(c);   changed = true; }
		if ((c = specialMapColor("Special: GRID", User.getColorGrid())) >= 0)
			{ User.setColorGrid(c);   changed = true; }
		if ((c = specialMapColor("Special: HIGHLIGHT", User.getColorHighlight())) >= 0)
			{ User.setColorHighlight(c);   changed = true; }
		if ((c = specialMapColor("Special: MOUSE-OVER HIGHLIGHT", User.getColorMouseOverHighlight())) >= 0)
			{ User.setColorMouseOverHighlight(c);   changed = true; }
		if ((c = specialMapColor("Special: PORT HIGHLIGHT", User.getColorPortHighlight())) >= 0)
			{ User.setColorPortHighlight(c);   changed = true; }
		if ((c = specialMapColor("Special: TEXT", User.getColorText())) >= 0)
			{ User.setColorText(c);   changed = true; }
		if ((c = specialMapColor("Special: INSTANCE OUTLINES", User.getColorInstanceOutline())) >= 0)
			{ User.setColorInstanceOutline(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM BACKGROUND", User.getColorWaveformBackground())) >= 0)
			{ User.setColorWaveformBackground(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM FOREGROUND", User.getColorWaveformForeground())) >= 0)
			{ User.setColorWaveformForeground(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM STIMULI", User.getColorWaveformStimuli())) >= 0)
			{ User.setColorWaveformStimuli(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM OFF STRENGTH", User.getColorWaveformStrengthOff())) >= 0)
			{ User.setColorWaveformStrengthOff(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM NODE (WEAK) STRENGTH", User.getColorWaveformStrengthNode())) >= 0)
			{ User.setColorWaveformStrengthNode(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM GATE STRENGTH", User.getColorWaveformStrengthGate())) >= 0)
			{ User.setColorWaveformStrengthGate(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM POWER STRENGTH", User.getColorWaveformStrengthPower())) >= 0)
			{ User.setColorWaveformStrengthPower(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM CROSSPROBE LOW", User.getColorWaveformCrossProbeLow())) >= 0)
			{ User.setColorWaveformCrossProbeLow(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM CROSSPROBE HIGH", User.getColorWaveformCrossProbeHigh())) >= 0)
			{ User.setColorWaveformCrossProbeHigh(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM CROSSPROBE UNDEFINED", User.getColorWaveformCrossProbeX())) >= 0)
			{ User.setColorWaveformCrossProbeX(c);   changed = true; }
		if ((c = specialMapColor("Special: WAVEFORM CROSSPROBE FLOATING", User.getColorWaveformCrossProbeZ())) >= 0)
			{ User.setColorWaveformCrossProbeZ(c);   changed = true; }

		// redisplay if changes were made
		if (changed)
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();
			EditWindow.repaintAllContents();
		}
	}

	private int specialMapColor(String title, int curColor)
	{
		ColorPatternPanel.Info li = (ColorPatternPanel.Info)transAndSpecialMap.get(title);
		if (li == null) return -1;
		int newColor = (li.red << 16) | (li.green << 8) | li.blue;
		if (newColor != curColor) return newColor;
		return -1;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        layers = new javax.swing.JPanel();
        layerName = new javax.swing.JComboBox();
        layerTechName = new javax.swing.JLabel();
        technology = new javax.swing.JComboBox();
        layerTechName1 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        layers.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(layerName, gridBagConstraints);

        layerTechName.setText("Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(layerTechName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(technology, gridBagConstraints);

        layerTechName1.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(layerTechName1, gridBagConstraints);

        getContentPane().add(layers, new java.awt.GridBagConstraints());

        pack();
    }
    // </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox layerName;
    private javax.swing.JLabel layerTechName;
    private javax.swing.JLabel layerTechName1;
    private javax.swing.JPanel layers;
    private javax.swing.JComboBox technology;
    // End of variables declaration//GEN-END:variables

}
