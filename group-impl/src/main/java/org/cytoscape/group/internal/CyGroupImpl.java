package org.cytoscape.group.internal;

/*
 * #%L
 * Cytoscape Groups Impl (group-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2008 - 2013 The Cytoscape Consortium
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.group.events.GroupAboutToBeRemovedEvent;
import org.cytoscape.group.events.GroupAddedToNetworkEvent;
import org.cytoscape.group.events.GroupNodesAddedEvent;
import org.cytoscape.group.events.GroupNodesRemovedEvent;
import org.cytoscape.group.events.GroupEdgesAddedEvent;
import org.cytoscape.group.events.GroupEdgesRemovedEvent;
import org.cytoscape.group.events.GroupCollapsedEvent;
import org.cytoscape.group.events.GroupAboutToCollapseEvent;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;


class CyGroupImpl implements CyGroup {
	final private static String CHILDREN_ATTR = "NumChildren";
	final private static String DESCENDENTS_ATTR = "NumDescendents";
	final private static String ISMETA_EDGE_ATTR = "__isMetaEdge";

	final private CyEventHelper cyEventHelper;
	final private CyGroupManager mgr;

	private CyNode groupNode;
	private Set<CyEdge> externalEdges;
	private Map<CyEdge, CyEdge> metaEdges;
	private Set<CyEdge> memberEdges;
	private CyRootNetwork rootNetwork = null;
	private Set<CyNetwork> networkSet = null;
	private Set<Long> collapseSet = null;
	private Map<Long, List<CyNode>> collapsedNodes = null;
	private Set<CyEdge> externalEdgeProcessed = null;
	private boolean nodeProvided = false;  // We'll need this when we destroy ourselves
	private static final Logger logger = LoggerFactory.getLogger(CyGroupImpl.class);

	CyGroupImpl(final CyEventHelper eventHelper, 
				final CyGroupManager mgr, CyNetwork network, CyNode node,
				List<CyNode>nodes, List<CyEdge>edges) {
		this.cyEventHelper = eventHelper;
		this.mgr = mgr;

		long timeStamp = System.currentTimeMillis();

		this.rootNetwork = ((CySubNetwork)network).getRootNetwork();
		if (node == null)
			this.groupNode = this.rootNetwork.addNode();
		else {
			nodeProvided = true;
			this.groupNode = node;
		}

		// logger.debug("Creating new group: "+this.groupNode);

		this.externalEdges = new HashSet<CyEdge>();
		this.metaEdges = new HashMap<CyEdge, CyEdge>();
		this.memberEdges = new HashSet<CyEdge>();
		this.networkSet = new HashSet<CyNetwork>();
		this.collapseSet = new HashSet<Long>();
		this.collapsedNodes = new HashMap<Long, List<CyNode>>();
		this.externalEdgeProcessed = new HashSet<CyEdge>();

		networkSet.add(rootNetwork);
		networkSet.add(network);

		if (nodes == null)
			nodes = new ArrayList<CyNode>();

		// This is merely a copy of the "nodes" list but as a set,
		// so it's fast to call the contains() method.
		Set<CyNode> nodeMap = new HashSet<CyNode>(nodes);

		// This block of code makes the distinction between internal and external edges
		// based on the edges we were given. If "edges" is null, it's our responsibility
		// to build the edge list from the parent network's edges in our group.
		if (edges != null) {
			List<CyEdge> intEdges = new ArrayList<CyEdge>();
			// Remove those edges in the list that aren't attached to nodes in
			// the list.  Otherwise, we'll wind up adding nodes to the group
			// that the user didn't request.
			for (CyEdge e: edges) {
				if (nodeMap.contains(e.getSource()) && nodeMap.contains(e.getTarget())) {
					intEdges.add(e);
				} else if(e.getSource().equals(node) || e.getTarget().equals(node)) {
					memberEdges.add(e);
				} else {
					externalEdges.add(e);
				}
			}
			edges = intEdges;
		} else if (edges == null) {
			// Create the edge lists
			edges = new ArrayList<CyEdge>();

			// Get all of the edges and put them in the right lists
			for (CyNode n: nodes) {
				List<CyEdge> aEdges = network.getAdjacentEdgeList(n, CyEdge.Type.ANY);
				for (CyEdge e: aEdges) {
					if (nodeMap.contains(e.getSource()) && nodeMap.contains(e.getTarget())) {
						edges.add(e);
					} else if(e.getSource().equals(node) || e.getTarget().equals(node)) {
						memberEdges.add(e);
					} else {
						// This is an external edge, which means that we need to create
						// a corresponding meta-edge
						externalEdges.add(e);
					}
				}
			}
		}


		// The group node must have a network pointer for expanding and collapsing.
		// If we were given a network pointer with the group node,
		// reflect our own internal data structures with what's in the network pointer.
		// If we were not given a network pointer, make a new subnetwork with our
		// group's contents.
		CySubNetwork np = (CySubNetwork)groupNode.getNetworkPointer();
		// If we already have a network pointer and we didn't get
		// nodes or edges, and the network pointer points to the same
		// root network, then it may have been provided by the session loader
		if (np != null && nodeProvided && 
			edges.size() == 0 && nodes.size() == 0 &&
			np.getRootNetwork().equals(this.rootNetwork)) {
			CySubNetwork groupNet = np;

			// See if we're already collapsed
			if (network.containsNode(groupNode)) {
				// Yes, note it
				collapseSet.add(network.getSUID());
			}
		} else {
			// Create the subnetwork
			CySubNetwork groupNet = rootNetwork.addSubNetwork(nodes, edges);
			groupNode.setNetworkPointer(groupNet);
		}

		// System.out.println("Group initalized in "+(System.currentTimeMillis()-timeStamp)+"ms");

		// Update our meta-edges
		timeStamp = System.currentTimeMillis();
		updateMetaEdges(true);
		// System.out.println("Group meta edge update took "+(System.currentTimeMillis()-timeStamp)+"ms");

		// Initialize our attributes
		timeStamp = System.currentTimeMillis();
		updateCountAttributes(rootNetwork);
		// System.out.println("Group attribute update took "+(System.currentTimeMillis()-timeStamp)+"ms");

		// printGroup();
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getGroupNode()
	 */
	@Override
	public CyNode getGroupNode() {
		return groupNode;
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getNodeList()
	 */
	@Override
	public List<CyNode> getNodeList() {
		return getGroupNetwork().getNodeList();
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getInternalEdgeList()
	 */
	@Override
	public List<CyEdge> getInternalEdgeList() {
		return getGroupNetwork().getEdgeList();
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getExternalEdgeList()
	 */
	@Override
	public Set<CyEdge> getExternalEdgeList() {
		return externalEdges;
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getGroupNetwork()
	 */
	@Override
	public CySubNetwork getGroupNetwork() {
		return (CySubNetwork)groupNode.getNetworkPointer();
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addNode()
	 */
	private synchronized void addNode(CyNode node) {
		// logger.debug("node "+node+" added to "+this.toString());
		if (!rootNetwork.containsNode(node))
			throwIllegalArgumentException("Can only add a node in the same network tree");
		getGroupNetwork().addNode(node);
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addInternalEdge()
	 */
	private synchronized void addInternalEdge(CyEdge edge) {
		// logger.debug("edge "+edge+" added as internal edge to "+this.toString());
		if (!rootNetwork.containsEdge(edge))
			throwIllegalArgumentException("Can only add an edge in the same network tree");
		getGroupNetwork().addEdge(edge);
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addExternalEdge()
	 */
	protected synchronized void addExternalEdge(CyEdge edge) {
		// logger.debug("edge "+edge+" added as external edge to "+this.toString());
		if (!rootNetwork.containsEdge(edge))
			throwIllegalArgumentException("Can only add an edge in the same network tree");
		if (!externalEdges.contains(edge)) {
			// System.out.println("Adding external edge "+edge+" to group "+this);
			externalEdges.add(edge);
		}
	}

	private synchronized void addMemberEdge(CyEdge edge) {
		// logger.debug("edge "+edge+" added as member edge to "+this.toString());
		if (!rootNetwork.containsEdge(edge))
			throwIllegalArgumentException("Can only add an edge in the same network tree");
		if (groupNode == null)
			throwIllegalArgumentException("Cannot add member edge without a group node");
		if (!edge.getSource().equals(groupNode) && !edge.getTarget().equals(groupNode))
			throwIllegalArgumentException("Cannot member edge whose source or target is not the group node");
		if (!memberEdges.contains(edge))
			memberEdges.add(edge);
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addNodes()
	 */
	@Override
	public synchronized void addNodes(List<CyNode> nodes) {
		Set<CyEdge> edgeSet = new HashSet<CyEdge>();
		for (CyNode n: nodes) {
			if (!rootNetwork.containsNode(n))
				throwIllegalArgumentException("Can only add a node in the same network tree");

			addNode(n);
			edgeSet.addAll(rootNetwork.getAdjacentEdgeList(n, CyEdge.Type.ANY));
		}

		for (CyEdge e: edgeSet) {
			final CyNode source = e.getSource();
			final CyNode target = e.getTarget();
			if (getGroupNetwork().containsNode(source) && getGroupNetwork().containsNode(target)) {
				addInternalEdge(e);
			} else if (groupNode != null && (source.equals(groupNode) || target.equals(groupNode))) {
				addMemberEdge(e);
			} else {
				addExternalEdge(e);
			}
		}
		updateMetaEdges(false);
		for (CyNetwork net: networkSet) {
			updateCountAttributes(net);
		}
		cyEventHelper.fireEvent(new GroupNodesAddedEvent(CyGroupImpl.this, nodes));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addEdges()
	 */
	@Override
	public synchronized void addEdges(List<CyEdge> edges) {
		boolean updateMeta = false;
		for (CyEdge edge: edges) {
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			if(getGroupNetwork().containsNode(source) && getGroupNetwork().containsNode(target)) {
				getGroupNetwork().addEdge(edge);
			} else if (groupNode != null && (source.equals(groupNode) || target.equals(groupNode))) {
				memberEdges.add(edge);
			} else if (getGroupNetwork().containsNode(source) || getGroupNetwork().containsNode(target)) {
				if (!metaEdges.values().contains(edge)) {
					externalEdges.add(edge);
					updateMeta = true;
				}
			} else
				throwIllegalArgumentException("Attempted to add an edge that has no node in the group");
		}
		if (updateMeta) {
			updateMetaEdges(true);
		}
		// printGroup();
		cyEventHelper.fireEvent(new GroupEdgesAddedEvent(CyGroupImpl.this, edges));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#removeNodes()
	 */
	@Override
	public synchronized void removeNodes(List<CyNode> nodes) {
		List<CyEdge> netEdges = new ArrayList<CyEdge>();
		for (CyNode node: nodes) {
			List<CyEdge> edges = rootNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			for (CyEdge edge: edges) {
				if (externalEdges.contains(edge))
					externalEdges.remove(edge);
				else if (memberEdges.contains(edge))
					memberEdges.remove(edge);
				else {
					netEdges.add(edge);
				}
			}
		}
		if (netEdges.size() > 0)
			getGroupNetwork().removeEdges(netEdges);
		getGroupNetwork().removeNodes(nodes);
		updateMetaEdges(false);
		for (CyNetwork net: networkSet) {
			updateCountAttributes(net);
		}
		cyEventHelper.fireEvent(new GroupNodesRemovedEvent(CyGroupImpl.this, nodes));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#removeEdges()
	 */
	@Override
	public synchronized void removeEdges(List<CyEdge> edges) {
		List<CyEdge> netEdges = new ArrayList<CyEdge>();
		for (CyEdge edge: edges) {
			if (getGroupNetwork().containsEdge(edge))
				netEdges.add(edge);
			else if (externalEdges.contains(edge))
				externalEdges.remove(edge);
			else if (metaEdges.values().contains(edge)) {
				removeMetaEdge(edge);
			} else if (memberEdges.contains(edge))
				memberEdges.remove(edge);
		}
		cyEventHelper.fireEvent(new GroupEdgesRemovedEvent(CyGroupImpl.this, edges));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getRootNetwork()
	 */
	@Override
	public CyRootNetwork getRootNetwork() {
		return this.rootNetwork;
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#getNetworkSet()
 	 */
	@Override
	public Set<CyNetwork> getNetworkSet() {
		return networkSet;
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#isInNetwork()
 	 */
	@Override
	public boolean isInNetwork(CyNetwork network) {
		return networkSet.contains(network);
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#addGroupToNetwork()
 	 */
	@Override
	public synchronized void addGroupToNetwork(CyNetwork network) {
		// First, we need to make sure this network is in the same
		// root network as the group node
		if (!inSameRoot(network))
			throwIllegalArgumentException("Network not in same root network as group");

		if(!networkSet.contains(network))
			networkSet.add(network);

		// Notify
		cyEventHelper.fireEvent(new GroupAddedToNetworkEvent(CyGroupImpl.this, network));
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#removeGroupFromNetwork()
 	 */
	@Override
	public synchronized void removeGroupFromNetwork(CyNetwork network) {
		// Notify
		cyEventHelper.fireEvent(new GroupAboutToBeRemovedEvent(CyGroupImpl.this, network));

		if(networkSet.contains(network))
			networkSet.remove(network);
	}

	/**
	 * @see org.cytoscape.group.CyGroup#collapse()
	 */
	@Override
	public void collapse(CyNetwork net) {
		// logger.debug("collapse "+this.toString()+" in net "+net.toString()+": isCollapsed = "+isCollapsed(net));

		// printGroup();
		if (isCollapsed(net))
			return; // Already collapsed

		if (!networkSet.contains(net))
			return; // We're not in that network

		CySubNetwork subnet = (CySubNetwork) net;
		CyNetwork groupNet =	groupNode.getNetworkPointer();

		// First collapse any children that are groups
		for (CyNode node: getNodeList()) {
			// Check to see if this is a group, and if it is in our network.  If it's
			// not in our network, it might have been collapsed by another group
			if (mgr.isGroup(node, net) && net.containsNode(node)) {
				// Yes, collapse it
				mgr.getGroup(node,net).collapse(net);
			}
		}

		// Now collapse ourselves
		cyEventHelper.fireEvent(new GroupAboutToCollapseEvent(CyGroupImpl.this, net, true));

		// Deselect all of the nodes
		for (CyNode node: getNodeList()) {
			if (net.containsNode(node))
				net.getRow(node).set(CyNetwork.SELECTED, Boolean.FALSE);
		}

		// Deselect all of our internal edges
		for (CyEdge edge: getInternalEdgeList()) {
			if (net.containsEdge(edge))
				net.getRow(edge).set(CyNetwork.SELECTED, Boolean.FALSE);
		}

		// Deselect all of our member edges
		for (CyEdge edge: memberEdges) {
			if (net.containsEdge(edge))
				net.getRow(edge).set(CyNetwork.SELECTED, Boolean.FALSE);
		}

		Set<CyGroup> partnersSeen = new HashSet<CyGroup>();

		// Deselect all of our external edges, and check for any possible
		// collapsed partner groups
		ListIterator<CyEdge> iterator = (new ArrayList<CyEdge>(externalEdges)).listIterator();
		while (iterator.hasNext()) {
			CyEdge edge = iterator.next();
			if (net.containsEdge(edge)) {
				net.getRow(edge).set(CyNetwork.SELECTED, Boolean.FALSE);
			} else if (!externalEdgeProcessed.contains(edge)) {
				// See if a partner node got collapsed
				CyNode node = getPartner(edge);
				if (!net.containsNode(node) && mgr.getGroupsForNode(node) != null) {
					for (CyGroup group: mgr.getGroupsForNode(node)) {
						if (!group.equals(this)) {
							// Create our meta-edge
							CyEdge metaEdge = null;
							if (isIncoming(edge)) {
 								metaEdge = createMetaEdge(edge, groupNode, group.getGroupNode());
							} else {
 								metaEdge = createMetaEdge(edge, group.getGroupNode(), groupNode);
							}
							addMetaEdge(edge, metaEdge);

							// Get the reciprocal edges
							addPartnerMetaEdges(net, edge, group, metaEdge);

							// Get the external edges and make them part of our external
							ListIterator<CyEdge> edgeIterator = (new ArrayList<CyEdge>(group.getExternalEdgeList())).listIterator();
							while (edgeIterator.hasNext()) {
								CyEdge partnerEdge = edgeIterator.next();
								CyEdge partnerMetaEdge = null;
								if (groupNet.containsNode(partnerEdge.getSource())) {
									partnerMetaEdge = createMetaEdge(partnerEdge, partnerEdge.getTarget(), groupNode);
								} else if (groupNet.containsNode(partnerEdge.getTarget())) {
									partnerMetaEdge = createMetaEdge(partnerEdge, partnerEdge.getSource(), groupNode);
								}
								if (partnerMetaEdge != null) {
									((CyGroupImpl)group).addExternalEdge(partnerMetaEdge);
								}
							}
						}
					}
				}
				externalEdgeProcessed.add(edge);
			}
		}

		// Only collapse nodes that are actually in our
		// network.  This checks for nodes that are in
		// multiple groups.
		List<CyNode> nodes = new ArrayList<CyNode>();
		for (CyNode node: getNodeList()) {
			if (net.containsNode(node)) {
				nodes.add(node);
			} else {
				// This node may be in multiple groups, ours and at least one
				// other, or it has been removed.  We need to add a meta-edge 
				// to that node in case we expand it, and potentially remove 
				// a meta-edge that might not apply
				List<CyGroup> otherGroups = mgr.getGroupsForNode(node);

				// Make sure someone didn't remove it
				if (otherGroups == null || otherGroups.size() == 0) continue;
				for (CyGroup group: otherGroups) {
					if (!((CyGroupImpl)group).equals(this)) {
						// Find any internal edges that point to this node
						for (CyEdge edge: groupNet.getAdjacentEdgeList(node, CyEdge.Type.ANY)) {
							// If this node has an internal edge, add a meta-edge to it
							CyEdge metaEdge = createMetaEdge(edge, node, groupNode);
							((CyGroupImpl)group).addExternalEdge(metaEdge);
							// TODO: Need to deal with unnecessary meta-edge
						}
					}
				}
			}
		}
		// System.out.println(nodes.size()+" nodes to collapse: "+nodes);
		collapsedNodes.put(net.getSUID(), nodes);

		// Remove all of the nodes from the target network
		subnet.removeNodes(nodes);

		// Add the group node and its edges to the target network.
		// If we have member edges, the group node didn't
		// go away so we don't have to add it back in here
		if (memberEdges.size() == 0) {
			subnet.addNode(groupNode);
			// Now add the edges for the group node
			List<CyEdge> groupNodeEdges = rootNetwork.getAdjacentEdgeList(groupNode, CyEdge.Type.ANY);
			for (CyEdge e: groupNodeEdges) {
				// I have no idea why this would be necessary, but it is....
				if (subnet.containsEdge(e))
						subnet.removeEdges(Collections.singletonList(e));
				if (subnet.containsNode(e.getSource()) && subnet.containsNode(e.getTarget()))
					subnet.addEdge(e);
			}
		}

		// Add the meta-edges
		for (CyEdge e: getMetaEdgeList()) {
			if (subnet.containsNode(e.getSource()) && subnet.containsNode(e.getTarget()))
				subnet.addEdge(e);
		}

		collapseSet.add(net.getSUID());

		// Update attributes
		updateCountAttributes(net);

		cyEventHelper.flushPayloadEvents();  // Make sure the view "hears" about all of the changes...

		// OK, all done
		cyEventHelper.fireEvent(new GroupCollapsedEvent(CyGroupImpl.this, net, true));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#expand()
	 */
	@Override
	public void expand(CyNetwork net) {
		// logger.debug("expand "+this.toString()+" in net "+net.toString()+": isCollapsed = "+isCollapsed(net));

		if (!isCollapsed(net))
			return; // Already expanded

		if (!networkSet.contains(net))
			return; // We're not in that network

		cyEventHelper.fireEvent(new GroupAboutToCollapseEvent(CyGroupImpl.this, net, false));

		CySubNetwork subnet = (CySubNetwork) net;

		// Get the list of nodes we collapsed in this net
		List<CyNode> nodes = collapsedNodes.get(net.getSUID());

		// If we were just restored as part of a session, we will be "collapsed", but
		// not really collapsed cleanly, so our collapsedNodes map will not (yet) be
		// initialized.  Handle this special case here
		if (nodes == null) {
			nodes = getNodeList();
		}

		// Expand it.

		// Remove the group node from the target network only if
		// there are no member edges. If there were member edges,
		// the group node did not go away.
		if (memberEdges.size() == 0)
			subnet.removeNodes(Collections.singletonList(groupNode));

		// Add all of the member nodes and edges in
		for (CyNode n: nodes)
			subnet.addNode(n);

		// Add all of the interior edges in
		for (CyEdge e: getInternalEdgeList()) {
			if (subnet.containsNode(e.getSource()) && subnet.containsNode(e.getTarget())) {
				subnet.addEdge(e);
			}
		}

		// Add all of the exterior edges in
		for (CyEdge e: getExternalEdgeList()) {
			// We need to be careful to only add the edge in
			// if both the edge and the target are available
			// since the target node might have been part of a
			// collapsed group
			if (subnet.containsNode(e.getSource()) && subnet.containsNode(e.getTarget())) {
				subnet.addEdge(e);
			}
		}

		// Finally, some of our meta-edges represent connections from
		// our nodes to collapsed groups.  Add those in
		for (CyEdge e: getMetaEdgeList()) {
			if (subnet.containsNode(e.getSource()) && subnet.containsNode(e.getTarget())) {
				subnet.addEdge(e);
			}
		}

		// Add all of the member edges in
		for (CyEdge e: memberEdges)
			subnet.addEdge(e);

		collapseSet.remove(net.getSUID());
		collapsedNodes.remove(net.getSUID());

		// Finish up
		cyEventHelper.fireEvent(new GroupCollapsedEvent(CyGroupImpl.this, net, false));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#isCollapsed()
	 */
	@Override
	public boolean isCollapsed(CyNetwork net) {
		return collapseSet.contains(net.getSUID());
	}

	/**
 	 * Destroy this group. This will destroy the subnetwork, all metaEdges, and 
 	 * the group node (if we created it).  This is meant to be called from the
 	 * CyGroupManager, only.
 	 */
	public void destroyGroup() {
		final CySubNetwork groupNet = getGroupNetwork();
		
		if (groupNet != null) {
			for (CyNetwork net: networkSet)
				expand(net);
	
			// Destroy the subNetwork
			rootNetwork.removeSubNetwork(groupNet);
			groupNode.setNetworkPointer(null);
		}

		// Release all of our external edges
		externalEdges.clear();

		if (groupNet != null) {
			// Remove all of our metaEdges from the root network
			rootNetwork.removeEdges(metaEdges.values());
			metaEdges.clear();

			// If our group node was not provided, destroy it if it doesn't have any member edges
			if (!nodeProvided && rootNetwork.containsNode(groupNode) && memberEdges.size() == 0) {
				rootNetwork.removeNodes(Collections.singletonList(groupNode));
			}
		}

		networkSet.clear();
		collapseSet.clear();
		cyEventHelper.flushPayloadEvents();
	}

	protected synchronized void addMetaEdge(CyEdge edge, CyEdge metaEdge) {
		// logger.debug("Adding edge "+edge.toString()+" as meta edge for group: "+this.toString());
		if (!metaEdges.containsKey(edge))
			metaEdges.put(edge, metaEdge);
	}

	protected synchronized void removeMetaEdge(CyEdge edge) {
		if (!metaEdges.values().contains(edge))
			for (CyEdge metaKey: metaEdges.keySet()) {
				if (metaEdges.get(metaKey).equals(edge))
					metaEdges.remove(metaKey);
			}
	}

	protected Collection<CyEdge> getMetaEdgeList() {
		return metaEdges.values();
	}

	protected CyEdge getMetaEdge(CyEdge edge) {
		if (metaEdges.containsKey(edge))
			return metaEdges.get(edge);
		return null;
	}

	@Override
	public String toString() {
		return "Group: groupNode: " + groupNode;
	}

	private boolean inSameRoot(CyNetwork network) {
		CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		if (!root.equals(rootNetwork))
			return false;
		return true;
	}

	private	void throwIllegalArgumentException(String message) {
		throw new IllegalArgumentException(message);
	}

  /**
	 * This method is the central method for the creation and maintenance of a
	 * meta-node.  Essentially, it is responsible for creating all of the meta-edges
	 * that connect this meta-node to external nodes.
	 *
	 * Basic approach:
	 *  for each external edge:
	 *	add a meta-edge to the parter
	 *	if the partner is a group and the group is in our network:
	 *	  add ourselves to the group's outer edges list (recursively)
	 *	  add ourselves to the partner's meta edge list
	 *	if the partner is in a group:
	 *	  add ourselves to the group's meta edge list
	 */
	private void updateMetaEdges(boolean ignoreMetaEdges) {
		metaEdges = new HashMap<CyEdge, CyEdge>();
		Set<CyGroup> partnersSeen = new HashSet<CyGroup>();

		long simpleMeta = 0L;
		long recursiveMeta1 = 0L;
		long recursiveMeta2 = 0L;

		// logger.debug(this.toString()+" updating meta edges");

		// We need to use a list iterator because we might need to add new
		// edges to our outer edge list and we want to add them to the
		// iterator to re-examine them
		ListIterator<CyEdge> iterator = (new ArrayList<CyEdge>(externalEdges)).listIterator();
		while (iterator.hasNext()) {
			long timeStamp = System.currentTimeMillis();
			CyEdge edge = iterator.next();
			CyNode node = getPartner(edge);

			// logger.debug(this.toString()+" outer edge = "+edge.toString());

			if (ignoreMetaEdges && isMeta(edge)) {
				// logger.debug("...ignoring");
				this.addMetaEdge(edge, edge);
				continue;
			}

			// If the edge is already on our group node, don't create a metaedge for it
			if (edge.getSource() == groupNode || edge.getTarget() == groupNode)
				continue;

			// If our group node already points to this node, and we're collapsed, we may
			// be adding a new edge from the XGMML reader.  Not a clean special case,
			// but there you have it...
			if (rootNetwork.containsEdge(edge.getSource(), groupNode)
				  && metaAlreadyExists(edge.getSource(), groupNode))
					continue;

			if (rootNetwork.containsEdge(groupNode, edge.getTarget())
				  && metaAlreadyExists(groupNode, edge.getTarget()))
					continue;

			// Create the meta-edge to the external node, but maintain the directionality
			// of the original edge
			CyEdge metaEdge = createMetaEdge(edge, node, groupNode);
			if (metaEdge == null)
				continue;
			this.addMetaEdge(edge, metaEdge);

			simpleMeta += System.currentTimeMillis()-timeStamp;

			for (CyNetwork net: networkSet) {
				timeStamp = System.currentTimeMillis();
				if (net.equals(rootNetwork))
					continue;

				// Is the partner a group?
				CyGroup metaPartner = mgr.getGroup(node, net);
				if (metaPartner != null && !partnersSeen.contains(metaPartner)) {
					// Recursively add links to the appropriate children
					addPartnerEdges(metaPartner, net, partnersSeen);
					((CyGroupImpl)metaPartner).addMetaEdge(edge, metaEdge);
				}
				recursiveMeta1 += System.currentTimeMillis()-timeStamp;
				timeStamp = System.currentTimeMillis();

				/*
				// Now, handle the case where the partner is a member of one or more groups
				List<CyGroup> nodeGroups = mgr.getGroupsForNode(node);
				if (nodeGroups != null && nodeGroups.size() > 0) {
					// TODO: Should we skip this and handle this at collapse/expand time?
					addPartnerMetaEdges(net, edge, nodeGroups, metaEdge);
				}
				*/
				recursiveMeta2 += System.currentTimeMillis()-timeStamp;
			}
		}
		// System.out.println("Simple Meta processing took: "+simpleMeta+"ms");
		// System.out.println("Recursive (partner edge) processing took: "+recursiveMeta1+"ms");
		// System.out.println("Recursive (partner meta edge) processing took: "+recursiveMeta2+"ms");
	}

	protected int getDescendents(CyNetwork net) {
		int nDescendents = getGroupNetwork().getNodeCount();
		for (CyNode node: getGroupNetwork().getNodeList()) {
			CyGroup group = mgr.getGroup(node, net);
			if (group != null)
				nDescendents += ((CyGroupImpl)group).getDescendents(net);
		}
		return nDescendents;
	}

	// Find the edge in our partner that links to us
	protected void addPartnerEdges(CyGroup metaPartner, CyNetwork net, Set<CyGroup> partnersSeen) {
		Set<CyEdge> partnerEdges = metaPartner.getExternalEdgeList();
		Set<CyEdge> newEdges = new HashSet<CyEdge>();
		// logger.debug("Group "+this.toString()+" adding partner edges for "+metaPartner.toString());

		// XXX Performance hog XXX
		for (CyEdge edge: partnerEdges) {
			// logger.debug("Looking at partner edge: "+edge.toString());
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			CyNode partner = null;
			boolean directed = edge.isDirected();
			if (getGroupNetwork().containsNode(target)) {
				source = groupNode;
				partner = target;
			} else if (getGroupNetwork().containsNode(source)) {
				target = groupNode;
				partner = source;
			} else {
				continue;
			}

			if (source == target)
				continue;

			// Create a new edge
			CyEdge newEdge = null;
			if (!rootNetwork.containsEdge(source, target)) {
				newEdge = rootNetwork.addEdge(source, target, directed);
				newEdges.add(newEdge);
			}

			// logger.debug("   ... it points to us -- created new edge: "+newEdge.toString());
			externalEdges.add(edge);

			CyGroup partnerMeta = mgr.getGroup(partner, net);
			if (partnerMeta != null && !partnersSeen.contains(partnerMeta)) {
				// logger.debug("Adding partner edges for "+partnerMeta.toString());
				partnersSeen.add(partnerMeta);
				addPartnerEdges(partnerMeta, net, partnersSeen);
				// logger.debug("Done adding partner edges for "+partnerMeta.toString());
			}

			if (newEdge != null)
				addMetaEdge(edge, newEdge);
		}

		for (CyEdge edge: newEdges) { ((CyGroupImpl)metaPartner).addExternalEdge(edge); }
		// logger.debug("Group "+this.toString()+" done adding partner edges for "+metaPartner.toString());
	}

	private CyEdge createMetaEdge(CyEdge edge, CyNode node, CyNode groupNode) {
		CyEdge metaEdge = null;
		if (isIncoming(edge)) {
			if (rootNetwork.containsEdge(node, groupNode))
				return rootNetwork.getConnectingEdgeList(node, groupNode, CyEdge.Type.ANY).get(0);
			metaEdge = rootNetwork.addEdge(node, groupNode, edge.isDirected());
		} else {
			if (rootNetwork.containsEdge(groupNode, node))
				return rootNetwork.getConnectingEdgeList(groupNode, node, CyEdge.Type.ANY).get(0);
			metaEdge = rootNetwork.addEdge(groupNode, node, edge.isDirected());
		}

		// Add the name and mark this as a meta-edge
		String edgeName = rootNetwork.getRow(edge).get(CyNetwork.NAME, String.class);
		rootNetwork.getRow(metaEdge).set(CyNetwork.NAME, "meta-"+edgeName);
		createIfNecessary(metaEdge, CyNetwork.HIDDEN_ATTRS, ISMETA_EDGE_ATTR, Boolean.class);
		rootNetwork.getRow(metaEdge, CyNetwork.HIDDEN_ATTRS).set(ISMETA_EDGE_ATTR, Boolean.TRUE);

		return metaEdge;
	}

	private void addPartnerMetaEdges(CyNetwork net, CyEdge connectingEdge, 
									                 CyGroup partnerGroup, CyEdge metaEdge) {
		// logger.debug("Group "+this.toString()+" adding meta edges for parter "+partnerNode.toString());

		// Are we partners in this network?
		if (!partnerGroup.getNetworkSet().contains(net))
			return;

		CyEdge metaMetaEdge = null;
		CyGroupImpl partner = (CyGroupImpl)partnerGroup;
		if (isIncoming(connectingEdge)) {
			metaMetaEdge = createMetaEdge(connectingEdge, partnerGroup.getGroupNode(), this.groupNode);
		} else {
			metaMetaEdge = createMetaEdge(connectingEdge, this.groupNode, partnerGroup.getGroupNode());
		}

		if (metaMetaEdge != null)
			partner.addMetaEdge(connectingEdge, metaMetaEdge);
		partner.addMetaEdge(connectingEdge, metaEdge);

		// Now, get our partner's metaEdges and add any that point to our children
		// XXX Performance hog XXX
		for (CyEdge outerEdge: partner.getMetaEdgeList()) {
			if (isConnectingEdge(outerEdge))
				addExternalEdge(outerEdge);
		}
		// partner.printGroup();
	}

	private boolean isMeta(CyEdge edge) {
		Boolean meta = rootNetwork.getRow(edge, CyNetwork.HIDDEN_ATTRS).
								get(ISMETA_EDGE_ATTR, Boolean.class, Boolean.FALSE);

		return meta.booleanValue();
	}

	private boolean isConnectingEdge(CyEdge edge) {
		CyNode source = edge.getSource();
		CyNode target = edge.getTarget();
		if (getGroupNetwork().containsNode(source) || getGroupNetwork().containsNode(target))
			return true;
		return false;
	}

	private boolean isIncoming(CyEdge edge) {
		CyNode source = edge.getSource();
		if (source.equals(groupNode) || getGroupNetwork().containsNode(source))
			return false;
		return true;
	}

	private CyNode getPartner(CyEdge edge) {
		CyNode source = edge.getSource();
		CyNode target = edge.getTarget();
		if (source.equals(groupNode) || getGroupNetwork().containsNode(source))
			return target;
		return source;
	}

	private void createIfNecessary(CyIdentifiable entry, String tableName, 
								   String attribute, Class type) {
		CyTable table = rootNetwork.getRow(entry, tableName).getTable();
		if (table.getColumn(attribute) == null)
			table.createColumn(attribute, type, false);

		return;
	}

	public void updateCountAttributes(CyNetwork net) {
		CyTable nodeTable = net.getDefaultNodeTable();
		CyColumn childrenColumn = nodeTable.getColumn(CHILDREN_ATTR);
		if (childrenColumn == null) {
			nodeTable.createColumn(CHILDREN_ATTR, Integer.class, true);
		}

		if (!nodeTable.rowExists(groupNode.getSUID())) {
			// Shouldn't happen!
			return;
		}
		CyRow groupRow = nodeTable.getRow(groupNode.getSUID());
		groupRow.set(CHILDREN_ATTR, getGroupNetwork().getNodeCount());

		CyColumn descendentsColumn = nodeTable.getColumn(DESCENDENTS_ATTR);
		if (descendentsColumn == null) {
			nodeTable.createColumn(DESCENDENTS_ATTR, Integer.class, true);
		}

		int nDescendents = getGroupNetwork().getNodeCount();
		for (CyNode node: getGroupNetwork().getNodeList()) {
			if (mgr.isGroup(node, rootNetwork)) {
				Integer d = nodeTable.getRow(node.getSUID()).get(DESCENDENTS_ATTR, Integer.class);
				if (d != null)
					nDescendents += d.intValue();
			}
		}
		groupRow.set(DESCENDENTS_ATTR, nDescendents);
	}

	public boolean metaAlreadyExists(CyNode source, CyNode target) {
		List<CyEdge> edges = rootNetwork.getConnectingEdgeList(source, target, CyEdge.Type.ANY);
		for (CyEdge groupEdge: edges) {
			if (isMeta(groupEdge)) {
				return true;
			}
		}
		return false;
	}

	protected void printGroup() {
		logger.debug("Group "+this);
		logger.debug("Nodes:");
		for (CyNode n: getNodeList()) {
			logger.debug("	"+n);
		}
		logger.debug("Networks:");
		for (CyNetwork n: networkSet) {
			logger.debug("	"+n);
		}
		logger.debug("Internal edges:");
		for (CyEdge edge: getInternalEdgeList()) {
			logger.debug("	"+edge);
		}
		logger.debug("External edges:");
		for (CyEdge edge: getExternalEdgeList()) {
			logger.debug("	"+edge);
		}
		logger.debug("Meta edges:");
		for (CyEdge edge: getMetaEdgeList()) {
			logger.debug("	"+edge);
		}
	}
}
