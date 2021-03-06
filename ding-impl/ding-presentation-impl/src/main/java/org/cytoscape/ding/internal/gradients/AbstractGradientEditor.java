package org.cytoscape.ding.internal.gradients;

import static org.cytoscape.ding.internal.gradients.AbstractGradient.GRADIENT_COLORS;
import static org.cytoscape.ding.internal.gradients.AbstractGradient.GRADIENT_FRACTIONS;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.cytoscape.ding.customgraphics.AbstractCustomGraphics2;
import org.cytoscape.ding.internal.util.GradientEditor;
import org.cytoscape.util.swing.LookAndFeelUtil;

public abstract class AbstractGradientEditor<T extends AbstractCustomGraphics2<?>> extends JPanel {

	private static final long serialVersionUID = 8197649738217133935L;
	
	private JLabel colorsLbl;
	private GradientEditor grEditor;
	private JPanel otherOptionsPnl;

	protected final T gradient;
	
	// ==[ CONSTRUCTORS ]===============================================================================================
	
	public AbstractGradientEditor(final T gradient) {
		this.gradient = gradient;
		init();
	}
	
	// ==[ PRIVATE METHODS ]============================================================================================
	
	protected void init() {
		createLabels();
		
		setOpaque(!LookAndFeelUtil.isAquaLAF()); // Transparent if Aqua
		
		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		
		final JSeparator sep = new JSeparator();
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
				.addComponent(colorsLbl)
				.addComponent(getGrEditor())
				.addComponent(sep)
				.addComponent(getOtherOptionsPnl())
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(colorsLbl)
				.addComponent(getGrEditor(), 100, 100, GroupLayout.PREFERRED_SIZE)
				.addComponent(sep, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(getOtherOptionsPnl())
		);
	}
	
	protected void createLabels() {
		colorsLbl = new JLabel("Colors:");
	}

	protected GradientEditor getGrEditor() {
		if (grEditor == null) {
			final List<Float> fractions = gradient.getList(GRADIENT_FRACTIONS, Float.class);
			final List<Color> colors = gradient.getList(GRADIENT_COLORS, Color.class);
			grEditor = new GradientEditor(fractions, colors);
			
			// Add listener--update gradient when user interacts with the UI
			grEditor.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateGradient();
				}
			});
			
			if (fractions == null || fractions.size() < 2) {
				gradient.set(GRADIENT_FRACTIONS, getGrEditor().getPositions());
				gradient.set(GRADIENT_COLORS, getGrEditor().getColors());
			}
		}
		
		return grEditor;
	}
	
	/**
	 * Should be overridden by the concrete subclass if it provides extra fields.
	 * @return
	 */
	protected JPanel getOtherOptionsPnl() {
		if (otherOptionsPnl == null) {
			otherOptionsPnl = new JPanel();
			otherOptionsPnl.setOpaque(!LookAndFeelUtil.isAquaLAF()); // Transparent if Aqua
			otherOptionsPnl.setVisible(false);
		}
		
		return otherOptionsPnl;
	}
	
	protected void updateGradient() {
		gradient.set(GRADIENT_FRACTIONS, getGrEditor().getPositions());
		gradient.set(GRADIENT_COLORS, getGrEditor().getColors());
	}
}
