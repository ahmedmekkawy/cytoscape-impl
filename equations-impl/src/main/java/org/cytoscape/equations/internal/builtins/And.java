package org.cytoscape.equations.internal.builtins;

/*
 * #%L
 * Cytoscape Equations Impl (equations-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2013 The Cytoscape Consortium
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


import org.cytoscape.equations.AbstractFunction;
import org.cytoscape.equations.ArgDescriptor;
import org.cytoscape.equations.ArgType;
import org.cytoscape.equations.FunctionUtil;


public class And extends AbstractFunction {
	public And() {
		super(new ArgDescriptor[] {
				new ArgDescriptor(ArgType.OPT_BOOLS, "truth_values", "Zero or more truth values or lists of truth values."),
			});
	}

	/**
	 *  Used to parse the function string.
	 *  @return the name by which you must call the function when used in an attribute equation.
	 */
	public String getName() { return "AND"; }

	/**
	 *  Used to provide help for users.
	 *  @return a description of what this function does
	 */
	public String getFunctionSummary() { return "Returns the logical conjunction of any number of boolean values."; }

	public Class<?> getReturnType() { return Boolean.class; }

	/**
	 *  @param args the function arguments which must all be of type Boolean
	 *  @return the result of the function evaluation which is either true or false
	 *  @throws ArithmeticException this can never happen
	 *  @throws IllegalArgumentException thrown if any of the arguments is not of type Boolean
	 */
	public Object evaluateFunction(final Object[] args) throws IllegalArgumentException, ArithmeticException {
		// Now evaluate the function.
		final boolean[] booleans;
		try {
			booleans = FunctionUtil.getBooleans(args);
		} catch (final Exception e) {
			throw new IllegalArgumentException("can't convert an argument or a list element to a boolean in a call to OR().");
		}

		for (final boolean b : booleans) {
			if (!b)
				return false;
		}

		return true;
	}
}
