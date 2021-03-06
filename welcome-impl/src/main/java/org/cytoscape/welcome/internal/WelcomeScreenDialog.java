package org.cytoscape.welcome.internal;

/*
 * #%L
 * Cytoscape Welcome Screen Impl (welcome-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import static org.cytoscape.welcome.internal.panel.WelcomeScreenChildPanel.PANEL_COLOR;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.cytoscape.application.CyVersion;
import org.cytoscape.property.CyProperty;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.welcome.internal.panel.AbstractWelcomeScreenChildPanel;
import org.cytoscape.welcome.internal.panel.WelcomeScreenChildPanel;

public class WelcomeScreenDialog extends JDialog {
	
	private static final long serialVersionUID = -2783045197802550425L;

	private static final String TITLE = "Welcome to Cytoscape";
	private static final Dimension DEF_SIZE = new Dimension(668, 500);

	private JPanel mainPanel;
	private JCheckBox checkBox;

	private final CyProperty<Properties> cyProps;

	// Child Panels
	private final AbstractWelcomeScreenChildPanel importPanel;
	private final AbstractWelcomeScreenChildPanel openPanel;
	private JPanel linksPanel;
	private final AbstractWelcomeScreenChildPanel helpPanel;
	
	private JLabel about;
	private JLabel manual;
	private JLabel tutorial;
	private JLabel bugReport;

	private final List<JLabel> labelSet;
	private final Map<JLabel, String> urlMap;

	private final OpenBrowser openBrowser;
	private final CyVersion version;
	
	public WelcomeScreenDialog(final AbstractWelcomeScreenChildPanel importPanel,
							   final AbstractWelcomeScreenChildPanel openPanel,
							   final AbstractWelcomeScreenChildPanel helpPanel,
							   final CyProperty<Properties> cyProps,
							   final boolean hide,
							   final OpenBrowser openBrowser,
							   final CyVersion version,
							   final Window owner) {
		super(owner);
		this.importPanel = importPanel;
		this.openPanel = openPanel;
		this.helpPanel = helpPanel;

		this.importPanel.setParentWindow(this);
		this.openPanel.setParentWindow(this);
		this.helpPanel.setParentWindow(this);

		this.cyProps = cyProps;
		this.openBrowser = openBrowser;
		this.version = version;

		labelSet = new ArrayList<JLabel>();
		urlMap = new HashMap<JLabel, String>();
		
		initComponents();

		this.setTitle(TITLE);

		this.setSize(DEF_SIZE);
		this.setPreferredSize(DEF_SIZE);
		this.setMinimumSize(DEF_SIZE);
		this.setMaximumSize(DEF_SIZE);
		this.setResizable(false);
		this.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);

		checkBox.setSelected(hide);
	}

	public boolean getHideStatus() {
		return checkBox.isSelected();
	}

	@SuppressWarnings("serial")
	private void initComponents() {
		linksPanel = new JPanel();
		linksPanel.setBackground(PANEL_COLOR);
		
		about = new JLabel("<html><u>About Cytoscape</u></html>");
		manual = new JLabel("<html><u>Documentation</u></html>");
		tutorial = new JLabel("<html><u>Tutorials</u></html>");
		bugReport = new JLabel("<html><u>Report a bug</u></html>");
		
		labelSet.add(about);
		labelSet.add(manual);
		labelSet.add(tutorial);
		labelSet.add(bugReport);
		
		// get Cytoscape version
		String cyversion = version.getVersion();

		// get OS string
		String os_str = System.getProperty("os.name") + "_" + System.getProperty("os.version");
		os_str = os_str.replace(" ", "_");
		
		urlMap.put(about, "http://www.cytoscape.org/what_is_cytoscape.html");
		urlMap.put(manual, "http://www.cytoscape.org/documentation_users.html");
		urlMap.put(tutorial, "http://opentutorials.cgl.ucsf.edu/index.php/Portal:Cytoscape3");
		urlMap.put(bugReport, "http://chianti.ucsd.edu/cyto_web/bugreport/bugreport.php?cyversion=" + cyversion
				+ "&os=" + os_str);

		for (final JLabel label : labelSet) {
			label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
			label.setFont(WelcomeScreenChildPanel.LINK_FONT);
			label.setForeground(WelcomeScreenChildPanel.LINK_FONT_COLOR);
			label.setHorizontalAlignment(SwingConstants.LEFT);
			label.setHorizontalTextPosition(SwingConstants.LEFT);
			label.setCursor(new Cursor(Cursor.HAND_CURSOR));
			label.addMouseListener(new LabelMouseListener(urlMap.get(label)));
			linksPanel.add(label);
		}
		
		checkBox = new JCheckBox("Don't show again");
		checkBox.setHorizontalAlignment(SwingConstants.LEFT);
		checkBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				cyProps.getProperties().setProperty(WelcomeScreenAction.DO_NOT_DISPLAY_PROP_NAME,
						((Boolean) checkBox.isSelected()).toString());
			}
		});

		final JButton closeButton = new JButton(new AbstractAction("Close") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		final JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
		bottomPanel.add(checkBox);
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(closeButton);

		mainPanel = new JPanel();
		mainPanel.setSize(DEF_SIZE);
		mainPanel.setLayout(new GridLayout(1, 2));
		mainPanel.setBackground(PANEL_COLOR);
		
		final JPanel southPanel = new JPanel();
		southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
		southPanel.add(linksPanel);
		southPanel.add(bottomPanel);
		
		final Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());
		pane.add(mainPanel, BorderLayout.CENTER);
		pane.add(southPanel, BorderLayout.SOUTH);
		
		createChildPanels();
		
		LookAndFeelUtil.setDefaultOkCancelKeyStrokes(getRootPane(), closeButton.getAction(), closeButton.getAction());
		getRootPane().setDefaultButton(closeButton);
		closeButton.requestFocusInWindow();
		
		pack();
	}

	private void createChildPanels() {
		JPanel openSessionPanel = new JPanel();
		JPanel newSessionPanel = new JPanel();
		JPanel newsPanel = new JPanel();

		openSessionPanel.setLayout(new BorderLayout());
		newSessionPanel.setLayout(new BorderLayout());
		newsPanel.setLayout(new BorderLayout());

		final Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		openSessionPanel.setBorder(border);
		newSessionPanel.setBorder(border);
		newsPanel.setBorder(border);

		openSessionPanel.setBackground(PANEL_COLOR);
		newSessionPanel.setBackground(PANEL_COLOR);
		newsPanel.setBackground(PANEL_COLOR);

		setChildPanel(openSessionPanel, openPanel, "Open Recent Session");
		setChildPanel(newSessionPanel, importPanel, "Start New Session");
		setChildPanel(newsPanel, helpPanel, "Latest News");

		final JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		
		final JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

		rightPanel.add(openSessionPanel);
		rightPanel.add(newsPanel);

		leftPanel.add(newSessionPanel);

		mainPanel.add(leftPanel);
		mainPanel.add(rightPanel);
	}

	private void setChildPanel(JPanel panel, JPanel contentPanel, final String label) {
		final JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new GridLayout(1, 2));
		titlePanel.setBackground(WelcomeScreenChildPanel.TITLE_BG_COLOR);

		final JLabel title = new JLabel();
		title.setFont(WelcomeScreenChildPanel.TITLE_FONT);
		title.setText(label);
		title.setForeground(WelcomeScreenChildPanel.TITLE_FONT_COLOR);
		title.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		titlePanel.add(title);
		
		panel.add(titlePanel, BorderLayout.NORTH);
		panel.add(contentPanel, BorderLayout.CENTER);
	}
	
	private final class LabelMouseListener extends MouseAdapter {

		private final String url;

		LabelMouseListener(final String url) {
			this.url = url;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			dispose();
			openBrowser.openURL(url);
		}
	}
}
