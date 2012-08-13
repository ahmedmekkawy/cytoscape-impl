package org.cytoscape.browser.internal;


import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.LayoutStyle;
import javax.swing.LayoutStyle.ComponentPlacement;


import org.cytoscape.browser.internal.BrowserTableModel;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;


public class DeletionDialog extends JDialog {
	private static final class CaseInsensitiveCompare implements Comparator<String> {
		public int compare(final String s1, final String s2) {
			return s1.compareToIgnoreCase(s2);
		}

		public boolean equals(final Object other) {
			return other instanceof CaseInsensitiveCompare;
		}
	}

	private CyTable attributes;
	private BrowserTable table;

	/** Creates new form DeletionDialog */
	protected DeletionDialog(final Frame parent, final CyTable attributes, final BrowserTable table) {
		super(parent, "Delete Attributes", /* modal = */ true);

		this.attributes = attributes;
		this.table  = table;

		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */

	// <editor-fold defaultstate="collapsed" desc=" Generated Code ">
	private void initComponents() {
		deletionPane = new javax.swing.JScrollPane();
		attributeList = new javax.swing.JList();
		deleteButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		descriptionLabel = new javax.swing.JLabel();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		attributeList.setModel(new AbstractListModel() {
			public int getSize() {
				int mutableCount = 0;
				for (final CyColumn column : attributes.getColumns()) {
					if (!column.isImmutable())
						if (table.isColumnVisible(column.getName()))
							++mutableCount;
				}

				return mutableCount;
			}

			public Object getElementAt(final int i) {
				final String[] columnNames = new String[getSize()];

				int k = 0;
				for (final CyColumn column : attributes.getColumns()) {
					if (!column.isImmutable()){
						if (table.isColumnVisible(column.getName()))
							columnNames[k++] = column.getName();
					}
				}

				Arrays.sort(columnNames, new CaseInsensitiveCompare());
				return columnNames[i];
			}
		});
		deletionPane.setViewportView(attributeList);

		deleteButton.setText("Delete");
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				deleteButtonActionPerformed(evt);
			}
		});

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
				;
			}
		});

		descriptionLabel.setText("Please select attributes to be deleted:");

		final GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(Alignment.LEADING)
								.addGroup(Alignment.TRAILING,
										layout.createSequentialGroup()
										.addComponent(deleteButton)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(cancelButton)
										.addContainerGap())
										.addGroup(Alignment.TRAILING,
												layout.createSequentialGroup()
												.addComponent(deletionPane,
														GroupLayout.DEFAULT_SIZE,
														229, Short.MAX_VALUE)
														.addGap(12, 12, 12))
														.addGroup(layout.createSequentialGroup()
																.addComponent(descriptionLabel)
																.addContainerGap(26,
																		Short.MAX_VALUE)))));
		layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
				.addGroup(Alignment.TRAILING,
						layout.createSequentialGroup().addContainerGap()
						.addComponent(descriptionLabel).addGap(12, 12, 12)
						.addComponent(deletionPane,
								GroupLayout.DEFAULT_SIZE,
								200, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(Alignment.BASELINE)
										.addComponent(deleteButton).addComponent(cancelButton))
										.addContainerGap()));
		pack();
	} // </editor-fold>

	private void deleteButtonActionPerformed(ActionEvent evt) {
		final Object[] selected = attributeList.getSelectedValues();
		for (int i = 0; i < selected.length; i++)
			attributes.deleteColumn(selected[i].toString());
		dispose();
	}

	private void cancelButtonActionPerformed(ActionEvent evt) {
		dispose();
	}

	// Variables declaration - do not modify
	private javax.swing.JList attributeList;
	private javax.swing.JButton cancelButton;
	private javax.swing.JButton deleteButton;
	private javax.swing.JScrollPane deletionPane;
	private javax.swing.JLabel descriptionLabel;

	// End of variables declaration
}
