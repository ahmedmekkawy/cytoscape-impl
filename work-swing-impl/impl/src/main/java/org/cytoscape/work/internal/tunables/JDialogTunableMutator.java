package org.cytoscape.work.internal.tunables;


import java.awt.Color;
import java.awt.Dialog;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.cytoscape.work.AbstractTunableInterceptor;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TunableValidator;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.internal.tunables.utils.CollapsablePanel;
import org.cytoscape.work.internal.tunables.utils.XorPanel;
import org.cytoscape.work.swing.GUITunableHandler;
import org.cytoscape.work.swing.TunableDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Interceptor of <code>Tunable</code> that will be applied on <code>GUITunableHandlers</code>.
 *
 * <p><pre>
 * To set the new value to the original objects contained in the <code>GUITunableHandlers</code>:
 * <ul>
 *   <li>Creates the parent container for the GUI, or use the one that is specified </li>
 *   <li>Creates a GUI with swing components for each intercepted <code>Tunable</code> </li>
 *   <li>
 *     Displays the GUI to the user, following the layout construction rule specified in the <code>Tunable</code>
 *     annotations, and the dependencies to enable or not the graphic components
 *   </li>
 *   <li>
 *     Applies the new <i>value,item,string,state...</i> to the object contained in the <code>GUITunableHandler</code>,
 *     if the modifications have been validated by the user.
 *   </li>
 * </ul>
 * </pre></p>
 *
 * @author pasteur
 */
public class JDialogTunableMutator extends JPanelTunableMutator {

	/** Provides an initialised logger. */
	private Logger logger = LoggerFactory.getLogger(JDialogTunableMutator.class);

	private Window parent = null;
	
	/**
	 * Constructor.
	 */
	public JDialogTunableMutator() {
		super();
	}
	
	/** {@inheritDoc} */
	public void setConfigurationContext(Object win) {
		if ( win == null )
			return;

		if ( win instanceof Window )
			parent = (Window)win;
		else
			throw new IllegalArgumentException("Dialog configuration context must be a Window, but it's a: " + win.getClass() );
	}
	
	/** {@inheritDoc} */
	public boolean validateAndWriteBack(Object objectWithTunables) {
		final JPanel panel = buildConfiguration(objectWithTunables);
		if (panel == null)
			return true;

		return displayGUI(panel, objectWithTunables);
	}

	/** 
	 * This implements the final action in execUI() and executes the UI.
	 * @param optionPanel  the panel containing the various UI elements corresponding to individual tunables
	 * @param objectWithTunables    represents the objects annotated with tunables
	 */
	private boolean displayGUI(final JPanel optionPanel, Object objectWithTunables) {
		TunableDialog tunableDialog = new TunableDialog(parent);
		tunableDialog.setLocationRelativeTo(parent);
		tunableDialog.setTitle("Set Parameters");
		tunableDialog.setModal(true);
		tunableDialog.setAlwaysOnTop(true);

		tunableDialog.addComponent(optionPanel);
		tunableDialog.setVisible(true);
		
		String userInput = tunableDialog.getUserInput();
		
		if (userInput.equalsIgnoreCase("OK")){
			return super.validateAndWriteBack(objectWithTunables);			
		} else { 
			return false;			
		}
	}
}
