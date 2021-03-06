package org.cytoscape.work.internal.tunables;

/*
 * #%L
 * Cytoscape Work Swing Impl (work-swing-impl)
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


import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static org.cytoscape.work.internal.tunables.utils.GUIDefaults.setTooltip;
import static org.cytoscape.work.internal.tunables.utils.GUIDefaults.updateFieldPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.AbstractGUITunableHandler;
import org.cytoscape.work.util.ListChangeListener;
import org.cytoscape.work.util.ListMultipleSelection;
import org.cytoscape.work.util.ListSelection;


/**
 * Handler for the type <i>ListMultipleSelection</i> of <code>Tunable</code>
 *
 * @author pasteur
 *
 * @param <T> type of items the List contains
 */
public class ListMultipleHandler<T> extends AbstractGUITunableHandler 
                                            implements ListSelectionListener, ListChangeListener<T> {
	
	private JList<T> itemsContainerList;
	private DefaultListModel<T> listModel;
	private ListMultipleSelection<T> listMultipleSelection;

	/**
	 * Constructs the <code>GUIHandler</code> for the <code>ListMultipleSelection</code> type
	 *
	 * creates a list to collect all the <code>T</code> items and displays it in the GUI through a JScrollPane
	 * Informations about the list and its contents are also displayed
	 *
	 * @param f field that has been annotated
	 * @param o object contained in <code>f</code>
	 * @param t tunable associated to <code>f</code>
	 */
	public ListMultipleHandler(Field f, Object o, Tunable t) {
		super(f, o, t);
		init();
	}

	public ListMultipleHandler(final Method getter, final Method setter, final Object instance, final Tunable tunable) {
		super(getter, setter, instance, tunable);
		init();
	}
	
	@SuppressWarnings("unchecked")
	private ListMultipleSelection<T> getMultipleSelection() {
		try {
			return (ListMultipleSelection<T>)getValue();
		} catch(final Exception e) {
			throw new NullPointerException("bad ListMultipleSelection object");	
		}
	}

	private void init() {
		listMultipleSelection = getMultipleSelection();
		listModel = new DefaultListModel<>();
		itemsContainerList = new JList<>(listModel);
		
		if (listMultipleSelection.getPossibleValues().isEmpty()) {
			panel = new JPanel();
			return;
		}
		
		// Select All/None buttons
		final JButton selectAllButton = new JButton("Select All");
		selectAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (listModel.getSize() > 0)
					itemsContainerList.getSelectionModel().setSelectionInterval(0, listModel.getSize() - 1);
			}
		});
		selectAllButton.putClientProperty("JButton.buttonType", "gradient"); // Mac OS X only
		selectAllButton.putClientProperty("JComponent.sizeVariant", "small"); // Mac OS X only
		
		final JButton selectNoneButton = new JButton("Select None");
		selectNoneButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				itemsContainerList.clearSelection();
			}
		});
		selectNoneButton.putClientProperty("JButton.buttonType", "gradient"); // Mac OS X only
		selectNoneButton.putClientProperty("JComponent.sizeVariant", "small"); // Mac OS X only
		selectNoneButton.setEnabled(false);
		
		LookAndFeelUtil.equalizeSize(selectAllButton, selectNoneButton);
		
		// Enable/disable buttons when list selection changes
		itemsContainerList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					final int total = listModel.getSize();
					final int selected = itemsContainerList.getSelectedIndices().length;
					selectAllButton.setEnabled(selected < total);
					selectNoneButton.setEnabled(selected > 0);
				}
			}
		});
		
		// put the items in a list
		for (T value : getMultipleSelection().getPossibleValues()) 
			listModel.addElement(value);
		
		itemsContainerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		itemsContainerList.addListSelectionListener(this);
		
		// selected items
		final List<T> selectedVals = listMultipleSelection.getSelectedValues();
		final List<T> allValues = listMultipleSelection.getPossibleValues();
		
		final int[] selectedIdx = new int[selectedVals.size()];
		int index = 0;
		
		for (T selected: selectedVals) {
			for (int i = 0; i < allValues.size(); i++) {
				if (itemsContainerList.getModel().getElementAt(i).equals(selected)) {
					selectedIdx[index] = i;
					index++;
				}
			}
		}
		
		itemsContainerList.setSelectedIndices(selectedIdx);
		
		// use a JscrollPane to visualize the items
		final JScrollPane scrollpane = new JScrollPane(itemsContainerList);
		scrollpane.setAutoscrolls(true);
		scrollpane.setOpaque(false);
		
		final JPanel controlPanel = new JPanel();
		if (LookAndFeelUtil.isAquaLAF()) controlPanel.setOpaque(false);
		
		final GroupLayout layout = new GroupLayout(controlPanel);
		controlPanel.setLayout(layout);
		layout.setAutoCreateGaps(LookAndFeelUtil.isWinLAF());
		layout.setAutoCreateContainerGaps(false);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(scrollpane, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(selectAllButton)
						.addComponent(selectNoneButton)
				)
		);
		layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING, true)
				.addComponent(scrollpane, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				.addGroup(layout.createSequentialGroup()
						.addComponent(selectAllButton)
						.addComponent(selectNoneButton)
				)
		);
		
		String description = getDescription();
		
		if (description != null && description.length() > 80) {
			// Use JTextArea for long descriptions
			final JTextArea textArea = new JTextArea(description);
			updateFieldPanel(panel, textArea, controlPanel, horizontal);
			setTooltip(getTooltip(), textArea, scrollpane);
		} else if (description != null && description.length() > 0) {
			// Otherwise, use JLabel
			final JLabel label = new JLabel(description);
			updateFieldPanel(panel, label, controlPanel, horizontal);
			setTooltip(getTooltip(), label, scrollpane);
		}
	}
	
	@Override
	public void update(){
		boolean reloadSelection = false;
		
		listMultipleSelection = getMultipleSelection();
		
		//If the list of elements has changed, remove old elements and add new ones
		if (!Arrays.equals(listModel.toArray(),listMultipleSelection.getPossibleValues().toArray())) {
			listModel.removeAllElements();
			reloadSelection = true;
			
			for ( T value : listMultipleSelection.getPossibleValues() ) 
				listModel.addElement(value);
		} else {
			//if the list is the same but the selection has changed, remove all selections and select new ones
			if(!Arrays.equals(itemsContainerList.getSelectedValues(),listMultipleSelection.getSelectedValues().toArray()))
				reloadSelection = true;
		}
		
		if (reloadSelection) {
			// selected items
			final List<T> selectedVals = listMultipleSelection.getSelectedValues();
			final List<T> allValues = listMultipleSelection.getPossibleValues();
			
			final int[] selectedIdx = new int[selectedVals.size()];
			int index = 0;
			
			for (T selected: selectedVals) {
				for (int i = 0; i<allValues.size(); i++) {
					if (itemsContainerList.getModel().getElementAt(i).equals(selected)) {
						selectedIdx[index] = i;
						index++;
					}
				}
			}
			
			if (!allValues.isEmpty()) {
				itemsContainerList.removeSelectionInterval(0, allValues.size()-1);
				itemsContainerList.setSelectedIndices(selectedIdx);
			}
		}
	}
	
	/**
	 * set the items that are currently selected in the <code>itemsContainerList</code> as the selected items in <code>listMultipleSelection</code>
	 */
	@Override
	public void handle() {
		if ( itemsContainerList == null || itemsContainerList.getModel().getSize() == 0)
			return;

		List selectedItems = Arrays.asList(itemsContainerList.getSelectedValues());
		if (!selectedItems.isEmpty()) {
			getMultipleSelection().setSelectedValues(selectedItems);
		}
		
		try {
			setValue(getMultipleSelection());
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * returns a string representation of all the selected items of <code>listMultipleSelection</code>
	 */
	@Override
	public String getState() {
		if ( itemsContainerList == null )
			return "";

		final List<T> selection = getMultipleSelection().getSelectedValues();
		return selection == null ? "" : selection.toString();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		handle();
	}

	@Override
	public void selectionChanged(ListSelection<T> source) {
		update();
	}

	@Override
	public void listChanged(ListSelection<T> source) {
		update();
	}
}
