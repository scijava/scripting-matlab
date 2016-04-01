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

import javax.script.ScriptEngine;

import matlabcontrol.extensions.MatlabNumericArray;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.Service;

/**
 * Default {@link MATLABService} implementation.
 *
 * @author Mark Hiner
 */
@Plugin(type = Service.class)
public class DefaultMATLABService extends
	AbstractSingletonService<MATLABCommands> implements MATLABService
{

	@Parameter
	private ScriptService scriptService;

	private boolean initializedCommands = false;

	@Override
	public String commandHelp() {
		String helpMessage = "--- MATLAB Command Plugins ---\n\n";

		for (final MATLABCommands command : getInstances()) {
			helpMessage += command.help();
		}

		helpMessage += "\n";

		return helpMessage;
	}

	@Override
	public void initializeCommands() {
		if (!initializedCommands) createCommandVariables();
	}

	@Override
	public void makeMATLABVariable(final String name, final Object value) {
		final ScriptEngine engine =
			scriptService.getLanguageByName("MATLAB").getScriptEngine();
		engine.put(name, value);
	}

	// -- Service methods --

	@Override
	public void initialize() {
		// Register known data type aliases for use in script @parameters
		scriptService.addAlias("matrix", MatlabNumericArray.class);
	}

	@Override
	public void dispose() {
		removeCommandVariables();
	}

	// -- Typed methods --

	@Override
	public Class<MATLABCommands> getPluginType() {
		return MATLABCommands.class;
	}

	// -- Helper methods --

	/**
	 * Helper method to create variables for each {@link MATLABCommands} within
	 * MATLAB.
	 */
	private synchronized void createCommandVariables() {
		if (!initializedCommands) {
			for (final MATLABCommands command : getInstances()) {
				final String name = command.getInfo().getName();

				makeMATLABVariable(name, command);
			}
			initializedCommands = true;
		}
	}

	/**
	 * Helper method to remove variables for each {@link MATLABCommands} within
	 * MATLAB.
	 */
	private void removeCommandVariables() {
		if (initializedCommands) {
			for (final MATLABCommands command : getInstances()) {
				final String name = command.getInfo().getName();

				makeMATLABVariable(name, null);
			}
		}
	}
}
