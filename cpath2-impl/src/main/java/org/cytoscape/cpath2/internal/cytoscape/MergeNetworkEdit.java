// $Id: MergeNetworkEdit.java,v 1.1 2007/06/22 16:02:34 grossb Exp $
//------------------------------------------------------------------------------
/** Copyright (c) 2007 Memorial Sloan-Kettering Cancer Center.
 **
 ** Code written by: Ethan Cerami, Benjamin Gross
 ** Authors: Ethan Cerami, Gary Bader, Chris Sander, Benjamin Gross
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package org.cytoscape.cpath2.internal.cytoscape;

// imports

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.cpath2.internal.CPath2Factory;
import org.cytoscape.cpath2.internal.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.swing.CyAbstractEdit;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.MinimalVisualLexicon;

/**
 * An undoable edit used by MergeNetworkTask
 * to provide undo/redo support.  Code based on cytoscape.editor.AddNodeEdit
 */
public class MergeNetworkEdit extends CyAbstractEdit {

    /**
     * ref to CyNetwork that we are modifying
     */
    private CyNetwork cyNetwork;

    /**
     * ref to map: node is key, value is node position
     */
    private Map<CyNode, Point2D.Double> cyNodes;

    /**
     * ref to edge set
     */
    private Collection<CyEdge> cyEdges;

	private final CPath2Factory factory;

    /**
     * Constructor.
     *
     * @param cyNetwork CyNetwork
     * @param cyNodes   Set<CyNode>
     * @param cyEdges   Set<CyEdge>
     */
    public MergeNetworkEdit(CyNetwork cyNetwork, Collection<CyNode> cyNodes, Collection<CyEdge> cyEdges, CPath2Factory factory) {
        super("Merge Network");
        this.factory = factory;

        // check args
        if (cyNetwork == null || cyNodes == null || cyEdges == null)
            throw new IllegalArgumentException("network, nodes, or edges is null");

        // init args
        this.cyNetwork = cyNetwork;
        this.cyEdges = cyEdges;

        this.cyNodes = new HashMap<CyNode, Point2D.Double>();
        CyNetworkView view = factory.getCyNetworkViewManager().getNetworkView(cyNetwork.getSUID());
        if (view != null) {
            for (CyNode cyNode : cyNodes) {
                View<CyNode> nv = view.getNodeView(cyNode);
                double x = nv.getVisualProperty(MinimalVisualLexicon.NODE_X_LOCATION).doubleValue();
                double y = nv.getVisualProperty(MinimalVisualLexicon.NODE_Y_LOCATION).doubleValue();
                Point2D.Double point = new Point2D.Double(x, y);
                this.cyNodes.put(cyNode, point);
            }
        }
    }

    /**
     * Method to undo this network merge
     */
    public void undo() {
        super.undo();

        // iterate through nodes and hide each one
        for (CyNode cyNode : cyNodes.keySet()) {
        	cyNetwork.removeNode(cyNode);
        }

        // iteracte through edges and hide each one
        for (CyEdge cyEdge : cyEdges) {
            cyNetwork.removeEdge(cyEdge);
        }

        // fire Cytoscape.NETWORK_MODIFIED
//        Cytoscape.firePropertyChange(Cytoscape.NETWORK_MODIFIED, null, cyNetwork);
    }

    /**
     * Method to redo this network merge
     */
    public void redo() {
        super.redo();

        // get ref to view
        CyNetworkView view = factory.getCyNetworkViewManager().getNetworkView(cyNetwork.getSUID());

        if (view != null) {

            // iterate through nodes and restore each one (also set proper position)
            for (CyNode cyNode : cyNodes.keySet()) {
            	CyNode node = cyNetwork.addNode();
            	AttributeUtil.copyAttributes(cyNode, node);
            	
                Point2D.Double point = cyNodes.get(cyNode);
                View<CyNode> nv = view.getNodeView(node);
                nv.setVisualProperty(MinimalVisualLexicon.NODE_X_LOCATION, point.getX());
                nv.setVisualProperty(MinimalVisualLexicon.NODE_Y_LOCATION, point.getY());
            }

            // interate through edges and restore each one...
            for (CyEdge cyEdge : cyEdges) {
            	// TODO: need to restore edges...
            }

            // do we perform layout here ?
        }

        // fire Cytoscape.NETWORK_MODIFIED
//        Cytoscape.firePropertyChange(Cytoscape.NETWORK_MODIFIED, null, cyNetwork);
	}
}
