package org.cytoscape.io.webservice.biomart.task;

/*
 * #%L
 * Cytoscape Biomart Webservice Impl (webservice-biomart-client-impl)
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


import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.io.webservice.biomart.BiomartQuery;
import org.cytoscape.io.webservice.biomart.rest.BiomartRestClient;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.task.edit.ImportDataTableTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;


/**
 * Task to import actual data tables from BioMart service.
 * 
 */
public class ImportTableTask extends AbstractTask {
	private final BiomartRestClient client;
	private final BiomartQuery query;

	private final CyTableFactory tableFactory;

	private Set<CyTable> tables;


	private final CyTableManager tableManager;
	private final ImportDataTableTaskFactory mapNetworkAttrTF;

	public ImportTableTask(final BiomartRestClient client, final BiomartQuery query,
			       final CyTableFactory tableFactory,
			       final CyTableManager tableManager,
				   final ImportDataTableTaskFactory mapNetworkAttrTF)
	{
		this.client               = client;
		this.query                = query;
		this.tableFactory         = tableFactory;
		this.tableManager         = tableManager;
		this.mapNetworkAttrTF     = mapNetworkAttrTF;

		this.tables = new HashSet<CyTable>();
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		if (query == null)
			throw new NullPointerException("Query is null");

		taskMonitor.setProgress(0.0);
		taskMonitor.setTitle("Loading data table from BioMart...");
		taskMonitor.setStatusMessage("Loading data...");

		final BufferedReader result = client.sendQuery(query.getQueryString());

		if (result.ready() == false) {
			result.close();
			throw new IOException("Could not get result.");
		}
		
		taskMonitor.setStatusMessage("Creating global table...");
		final CyTable newTable = createGlobalTable(result,
				query.getKeyColumnName());
		result.close();

		tables.add(newTable);
		
		final TaskIterator ti = mapNetworkAttrTF.createTaskIterator(newTable);
		this.insertTasksAfterCurrentTask(ti);
	}

	private CyTable createGlobalTable(BufferedReader reader, String key) throws IOException {
		// Read result from reader
		String line = reader.readLine();
		//System.out.println("Table Header: " + line);
		final String[] columnNames = line.split("\\t");

		if (columnNames[0].contains("Query ERROR"))
			throw new IOException("BioMart service returns error: \n" + line);

		final CyTable globalTable = tableFactory.createTable(query.getTableName(), key, String.class, false, true);

		// For status report
		int recordCount = 0;
		List<String> report = new ArrayList<String>();

		// final int rowCount = result.size();
		final int colSize = columnNames.length;
		int keyIdx = 0;

		// Search column index of the key
		for (int i = 0; i < colSize; i++) {
			if (columnNames[i].equals(key))
				keyIdx = i;
			else
				globalTable.createColumn(columnNames[i], String.class, false);
		}

		String[] row;
		String val;

		List<List<Object>> listOfValList;
		List<String> ids = null;

		List<Object> testList;
		String keyVal = null;
		int rowLength = 0;

		int hitCount = 0;

		long start = System.currentTimeMillis();
		while ((line = reader.readLine()) != null) {

			row = line.split("\\t");

			// Ignore invalid length entry.
			if ((row.length <= keyIdx) || (row.length == 0))
				continue;

			recordCount++;
			keyVal = row[keyIdx];

			rowLength = row.length;

			final CyRow cyRow = globalTable.getRow(keyVal);
			for (int j = 0; j < rowLength; j++) {
				val = row[j];

				if ((val != null) && (val.length() != 0)) {

					cyRow.set(columnNames[j], val);
					// if (keyAttrName.equals("ID")) {
					// testList = attr
					// .getListAttribute(keyVal, columnNames[j]);
					//
					// if (testList != null)
					// listOfValList.add(testList);
					// } else {
					// ids = getIdFromAttrValue(attrDataType, keyAttrName,
					// keyVal, nodeIdList, attr);
					//
					// if (ids.size() == 0)
					// continue;
					//
					// for (String id : ids)
					// listOfValList.add(attr.getListAttribute(id,
					// columnNames[j]));
					// }
					//
					// if (listOfValList.size() == 0) {
					// List<Object> valList = new ArrayList<Object>();
					// listOfValList.add(valList);
					// }
					//
					// int index = 0;
					// for (List<Object> valList : listOfValList) {
					// if (valList == null)
					// valList = new ArrayList<Object>();
					//
					// if (valList.contains(row[j]) == false)
					// valList.add(row[j]);
					//
					// if (keyAttrName.equals("ID")) {
					// attr.setListAttribute(keyVal, columnNames[j],
					// valList);
					// attr.setAttribute(keyVal, columnNames[j] + "-TOP",
					// valList.get(0).toString());
					// } else {
					// attr.setListAttribute(ids.get(index),
					// columnNames[j], valList);
					// attr.setAttribute(ids.get(index), columnNames[j]
					// + "-TOP", valList.get(0).toString());
					//
					// }
					// hitCount++;
					// index++;
					// }
				}
			}
		}

		reader.close();
		reader = null;

		// Dump table
		// final List<CyRow> rows = globalTable.getAllRows();
		// for(CyRow r: rows) {
		// Map<String, Object> rowVals = r.getAllValues();
		// for(String k :rowVals.keySet()) {
		// System.out.print(k + ":" + rowVals.get(k) + " ");
		// }
		// System.out.println();
		// }

		tableManager.addTable(globalTable);

		return globalTable;
	}

	public Set<CyTable> getCyTables() {
		return tables;
	}

}
