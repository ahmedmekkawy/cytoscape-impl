package org.cytoscape.tableimport.internal.util;

/*
 * #%L
 * Cytoscape Table Import Impl (table-import-impl)
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

//import cytoscape.Cytoscape;
import static org.cytoscape.tableimport.internal.util.ImportType.NETWORK_IMPORT;
import static org.cytoscape.tableimport.internal.util.ImportType.ONTOLOGY_IMPORT;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.ALIAS;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.ATTR;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.EDGE_ATTR;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.INTERACTION;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.KEY;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.NONE;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.ONTOLOGY;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.SOURCE;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.SOURCE_ATTR;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.TARGET;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.TARGET_ATTR;
import static org.cytoscape.tableimport.internal.util.SourceColumnSemantic.TAXON;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.table.TableModel;

/**
 *
 */
public class TypeUtil {

	private static final List<SourceColumnSemantic> TABLE_IMPORT_TYPES = Arrays.asList(
			NONE, KEY, ATTR
	);
	private static final List<SourceColumnSemantic> NETWORK_IMPORT_TYPES = Arrays.asList(
			NONE, SOURCE, INTERACTION, TARGET, EDGE_ATTR, SOURCE_ATTR, TARGET_ATTR
	);
	private static final List<SourceColumnSemantic> ONTOLOGY_IMPORT_TYPES = Arrays.asList(
			NONE, KEY, ALIAS, ONTOLOGY, TAXON, ATTR
	);
	
	private static final String[] PREF_KEY_NAMES = new String[] {
		"sharedname", "name", "identifier", "id", "node", "edge", "gene", "genename", "protein", "symbol"
	};
	private static final String[] PREF_SOURCE_NAMES = new String[] {
		"source", "sourcenode", "sourcename", "sourceid", "sourceidentifier",
		"node1", "nodea", "identifier1", "identifiera", "id1", "ida", "name1", "namea",
		"sourcegene", "gene1", "genename1", "geneid1",
		"name", "sharedname", "node", "gene", "genename"
	};
	private static final String[] PREF_TARGET_NAMES = new String[] {
		"target", "targetnode", "targetname", "targetid", "targetidentifier",
		"node2", "nodeb", "identifier2", "identifierb", "id2", "idb", "name2", "nameb",
		"targetgene", "gene2", "genename2", "geneid2"
	};
	private static final String[] PREF_INTERACTION_NAMES = new String[] {
		"interaction", "interactiontype", "edgetype"
	};
	private static final String[] PREF_ONTOLOGY_NAMES = new String[] {
		"gene ontology", "ontology", "go"
	};
	private static final String[] PREF_TAXON_NAMES = new String[] {
		"taxon", "taxid", "taxonomy", "organism"
	};
	
	private static Pattern truePattern = Pattern.compile("^true$", Pattern.CASE_INSENSITIVE);
	private static Pattern falsePattern = Pattern.compile("^false$", Pattern.CASE_INSENSITIVE);
	
	private TypeUtil() {}
	
	public static List<SourceColumnSemantic> getAvailableTypes(final ImportType importType) {
		if (importType == NETWORK_IMPORT) return NETWORK_IMPORT_TYPES;
		if (importType == ONTOLOGY_IMPORT) return ONTOLOGY_IMPORT_TYPES;
		
		return TABLE_IMPORT_TYPES;
	}
	
	public static SourceColumnSemantic getDefaultType(final ImportType importType) {
		return importType == NETWORK_IMPORT ? EDGE_ATTR : ATTR;
	}
	
	public static SourceColumnSemantic[] guessTypes(final ImportType importType, final TableModel model) {
		final int size = model.getColumnCount();
		
		final SourceColumnSemantic[] types = new SourceColumnSemantic[size];
		
		if (importType == NETWORK_IMPORT)
			Arrays.fill(types, EDGE_ATTR);
		else
			Arrays.fill(types, ATTR);
		
		boolean srcFound = false;
		boolean tgtFound = false;
		boolean interactFound = false;
		boolean keyFound = false;
		boolean goFound = false;
		boolean taxFound = false;

		// First pass: Look for exact column name
		// Second pass: Select column whose name contains one of the tokens
		MAIN_LOOP:
		for (int count = 0; count < 2; count++) {
			boolean exact = count == 0;
			
			for (int i = 0; i < size; i++) {
				final String name = model.getColumnName(i);
				
				if (importType == NETWORK_IMPORT) {
					if (!srcFound && matches(name, PREF_SOURCE_NAMES, exact)) {
						srcFound = true;
						types[i] = SOURCE;
					} else if (!tgtFound && matches(name, PREF_TARGET_NAMES, exact)) {
						tgtFound = true;
						types[i] = TARGET;
					} else if (!interactFound && matches(name, PREF_INTERACTION_NAMES, exact)) {
						interactFound = true;
						types[i] = INTERACTION;
					}
					
					if (srcFound && tgtFound && interactFound)
						break MAIN_LOOP;
				} else if (importType == ONTOLOGY_IMPORT) {
					if (!keyFound && matches(name, PREF_KEY_NAMES, exact)) {
						keyFound = true;
						types[i] = KEY;
					} else if (!goFound && matches(name, PREF_ONTOLOGY_NAMES, exact)) {
						goFound = true;
						types[i] = ONTOLOGY;
					} else if (!taxFound && matches(name, PREF_TAXON_NAMES, exact)) {
						taxFound = true;
						types[i] = TAXON;
					}
					
					if (keyFound && goFound && taxFound)
						break MAIN_LOOP;
				} else {
					if (matches(name, PREF_KEY_NAMES, exact)) {
						types[i] = KEY;
						break MAIN_LOOP;
					}
				}
			}
		}

		return types;
	}
	
	public static AttributeDataType[] guessDataTypes(final TableModel model) {
		// 0 = Boolean,  1 = Integer,  2 = Double,  3 = String
		final Integer[][] typeChecker = new Integer[4][model.getColumnCount()];

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < model.getColumnCount(); j++) {
				typeChecker[i][j] = 0;
			}
		}

		String cell = null;

		for (int i = 0; i < model.getRowCount(); i++) {
			for (int j = 0; j < model.getColumnCount(); j++) {
				cell = (String) model.getValueAt(i, j);
				boolean found = false;

				if (cell != null) { 
					// boolean
					if (truePattern.matcher(cell).matches() || falsePattern.matcher(cell).matches()) {
						typeChecker[0][j]++;
						found = true;
					} else {
						// integers
						try {
							Integer.valueOf(cell);
							typeChecker[1][j]++;
							found = true;
						} catch (NumberFormatException e) {
						}
			
						// floats
						try {
							Double.valueOf(cell);
							typeChecker[2][j]++;
							found = true;
						} catch (NumberFormatException e) {
						}
					}
				}
				
				// default to string
				if (found == false)
					typeChecker[3][j]++;
			}
		}

		final AttributeDataType[] dataTypes = new AttributeDataType[model.getColumnCount()];

		for (int i = 0; i < dataTypes.length; i++) {
			int maxVal = 0;
			int maxIndex = 0;

			for (int j = 0; j < 4; j++) {
				if (maxVal < typeChecker[j][i]) {
					maxVal = typeChecker[j][i];
					maxIndex = j;
				}
			}
	
			if (maxIndex == 0)
				dataTypes[i] = AttributeDataType.TYPE_BOOLEAN;
			else if (maxIndex == 1)
				dataTypes[i] = AttributeDataType.TYPE_INTEGER;
			else if (maxIndex == 2)
				dataTypes[i] = AttributeDataType.TYPE_FLOATING;
			else
				dataTypes[i] = AttributeDataType.TYPE_STRING;
		}

		return dataTypes;
	}
	
	/**
	 * Returns true if columns of the passed column type can have duplicate names in the source file or table.
	 * @param types 
	 */
	public static boolean allowsDuplicateName(final ImportType importType, final SourceColumnSemantic type1,
			SourceColumnSemantic type2) {
		boolean b = type1 == NONE || type2 == NONE;
		
		if (importType == NETWORK_IMPORT) {
			b = b || (type1 == SOURCE_ATTR && type2 != SOURCE_ATTR && type2 != SOURCE && type2 != TARGET);
			b = b || (type2 == SOURCE_ATTR && type1 != SOURCE_ATTR && type1 != SOURCE && type1 != TARGET);
			b = b || (type1 == TARGET_ATTR && type2 != TARGET_ATTR && type2 != SOURCE && type2 != TARGET);
			b = b || (type2 == TARGET_ATTR && type1 != TARGET_ATTR && type1 != SOURCE && type1 != TARGET);
			b = b || (type1 == EDGE_ATTR && type2 != EDGE_ATTR && type2 != INTERACTION);
			b = b || (type2 == EDGE_ATTR && type1 != EDGE_ATTR && type1 != INTERACTION);
		}
		
		return b;
	}
	
	private static boolean matches(String name, final String[] preferredNames, final boolean exact) {
		// Remove all special chars and spaces from column name
		name = name.replaceAll("[^a-zA-Z0-1]", "").toLowerCase();
		
		for (final String s : preferredNames) {
			if ( (exact && name.equals(s)) || (!exact && name.contains(s)) )
				return true;
		}
		
		return false;
	}
}
