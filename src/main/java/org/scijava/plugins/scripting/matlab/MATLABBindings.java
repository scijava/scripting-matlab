/*
 * #%L
 * MATLAB scripting language plugin.
 * %%
 * Copyright (C) 2014 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.plugins.scripting.matlab;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptModule;

/**
 * A {@link Bindings} wrapper around MATLAB's local variables.
 *
 * @author Mark Hiner
 */
public class MATLABBindings implements Bindings {

	// -- Parameters --

	@Parameter
	private OptionsService optionsService;

	@Parameter
	private ConvertService convertService;

	@Parameter
	private LogService logService;

	// -- Fields --

	private final Set<String> keys = new HashSet<String>();
	private final Set<Object> values = new HashSet<Object>();
	private final Map<String, Object> entries = new HashMap<String, Object>();
	private String scriptModuleKey = ScriptModule.class.getName();
	private Object scriptModule = null;

	// -- Map API --

	@Override
	public int size() {
		return keySet().size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsValue(final Object value) {
		return values().contains(value);
	}

	@Override
	public void clear() {
		try {
			MATLABControlUtils.proxy(opts()).eval("clear");
			values.clear();
			keys.clear();
			entries.clear();
		}
		catch (final MatlabInvocationException e) {
			logService.error(e);
		}
	}

	@Override
	public Set<String> keySet() {
		keys.clear();
		keys.addAll(Arrays.asList(getVars()));
		if (scriptModule != null) keys.add(scriptModuleKey);

		return keys;
	}

	@Override
	public Collection<Object> values() {
		values.clear();
		for (final String key : keySet()) {
			final Object v = get(key);
			if (v != null) values.add(get(key));
		}
		return values;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		entries.clear();
		for (final String key : keySet()) {
			final Object v = get(key);
			if (v != null) entries.put(key, v);
		}
		return entries.entrySet();
	}

	@Override
	public Object put(final String name, final Object value) {
		final MatlabProxy proxy = MATLABControlUtils.proxy(opts());

		// If we aren't inside MATLAB we cache the ScriptModule in the local JVM
		// Because MATLAB is running in a separate JVM we can not pass it a
		// ScriptModule instance.
		if (!proxy.isRunningInsideMatlab() &&
			name.equals(scriptModuleKey)) return (scriptModule = value);

		// Try special MATLAB data types
		if (value != null) {
			MatlabNumericArray arrayVal = null;

			// Cast if able
			if (MatlabNumericArray.class.isAssignableFrom(value.getClass())) {
				arrayVal = (MatlabNumericArray) value;
			}
			// Convert if able
			else if (convertService.supports(value, MatlabNumericArray.class)) {
				arrayVal = convertService.convert(value, MatlabNumericArray.class);
			}

			// Convert the dataset to a MATLAB array and set it as a local variable
			// within MATLAB.
			if (arrayVal != null) {
				final MatlabTypeConverter converter = new MatlabTypeConverter(proxy);
				try {
					converter.setNumericArray(sanitize(name), arrayVal);
					return value;
				}
				catch (final MatlabInvocationException e) {
					logService.warn(e);
				}
			}
		}

		try {
			proxy.setVariable(sanitize(name), value);
			return value;
		}
		catch (final MatlabInvocationException e) {
			logService.warn("Could not set variable: " + name +
				".\n\tIf MATLAB is running remotely, value of:\n\t" + value +
				"\n\tmust be converted to a MatlabNumericArray.");
		}

		return null;
	}

	@Override
	public void putAll(final Map<? extends String, ? extends Object> toMerge) {
		for (final String k : toMerge.keySet()) {
			put(k, toMerge.get(k));
		}
	}

	@Override
	public boolean containsKey(final Object key) {
		return keySet().contains(key);
	}

	@Override
	public Object get(final Object key) {
		return retrieveValue(key, false);
	}

	@Override
	public Object remove(final Object key) {
		return retrieveValue(key, true);
	}

	// -- Helper methods --

	/**
	 * Helper method for retrieving a value for a given key. Can optionally remove
	 * the entry from MATLAB's variable list as well.
	 *
	 * @param key - Key to look up.
	 * @param remove - If true, the entry will be removed from MATLAB's variable
	 *          list.
	 * @return The retrieved value, or null if no value found.
	 */
	private Object retrieveValue(final Object key, final boolean remove) {
		if (!containsKey(key) || !(key instanceof String)) return null;

		final String k = (String) key;
		final MatlabProxy proxy = MATLABControlUtils.proxy(opts());

		if (!proxy.isRunningInsideMatlab() && k.equals(scriptModuleKey)) return scriptModule;

		Object v = null;
		try {
			v = proxy.getVariable(k);
		}
		catch (final MatlabInvocationException e) {
			logService.warn(e);
		}

		// Array types will lose dimensionality if simply called via getVariable.
		// We need to convert the object to a double array in MATLAB so we can use
		// the MatlabNumericArray class.
		// NB: we can NOT perform this double conversion in decode because it
		// requires the variable to still exist in MATLAB (which is not guaranteed
		// by the time control passes to decode).
		if (v != null && v.getClass().isArray())
		{
			try {
				final String command = k + " = double(" + k + ");";
				proxy.eval(command);

				// try recovering key as a MatlabNumericArray
				final MatlabTypeConverter converter = new MatlabTypeConverter(proxy);
				final MatlabNumericArray array = converter.getNumericArray(k);

				// Unwrap single element arrays to primitive array
				if (array.getLength() == 1) {
					v = new double[] { array.getRealValue(0) };
				}
				else v = array;
			}
			catch (final MatlabInvocationException e) {
				logService
					.warn("Could not convert: " + k +
						" to a MatlabNumericArray.\n\tDimensionality information may be lost.");
			}
		}

		if (remove) {
			try {
				proxy.eval("clear " + k);
			}
			catch (MatlabInvocationException e) {
				logService.warn(e);
			}
		}

		return v;
	}

	/**
	 * @return All declared variables from MATLAB, as a String array.
	 */
	private String[] getVars() {
		try {
			final String[] vars =
				(String[]) MATLABControlUtils.proxy(opts()).returningEval("who", 1)[0];
			return vars;
		}
		catch (final MatlabInvocationException e) {
			logService.error(e);
		}
		return new String[0];
	}

	/**
	 * Helper method for converting variables to MATLAB-valid names.
	 *
	 * @param name - String to sanitize
	 * @return A variable name permissible in MATLAB
	 */
	private String sanitize(final String name) {
		return name.replaceAll("[^\\w]", "_");
	}

	/**
	 * Convenience method for access to the {@link MATLABOptions}
	 *
	 * @return Active {@code MATLABOptions}
	 */
	private MATLABOptions opts() {
		return optionsService.getOptions(MATLABOptions.class);
	}
}
