package org.cytoscape.view.manual.internal.control.actions.align;

/*
 * #%L
 * Cytoscape Manual Layout Impl (manual-layout-impl)
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

import java.util.List;

import javax.swing.Icon;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.manual.internal.control.actions.AbstractControlAction;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
/**
 *
 */
public class HAlignRight extends AbstractControlAction {

	private static final long serialVersionUID = -2582880158463407206L;

	public HAlignRight(Icon i,CyApplicationManager appMgr) {
		super("",i,appMgr);
	}

	protected void control(List<View<CyNode>> nodes) {
		for ( View<CyNode> n : nodes ) {
			final double w = n.getVisualProperty(BasicVisualLexicon.NODE_WIDTH) / 2;
			n.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, X_max - w);
		}
	}

	protected double getX(View<CyNode> n) {
		final double x = n.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
		final double w = n.getVisualProperty(BasicVisualLexicon.NODE_WIDTH) / 2;

		return x + w;
	}
}
