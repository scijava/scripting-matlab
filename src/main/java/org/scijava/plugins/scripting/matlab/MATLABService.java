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

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SingletonService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;

/**
 * {@link Service} performing any utility operations required for MATLAB.
 *
 * @author Mark Hiner
 */
public interface MATLABService extends SingletonService<MATLABCommands>,
	SciJavaService
{

	/**
	 * Builds a complete usage message by querying the
	 * {@link MATLABCommands#usage()} method of all available commands.
	 *
	 * @return A formatted string of all available {@link MATLABCommands}.
	 */
	String commandHelp();

	/**
	 * Creates a MATLAB variable for each {@link MATLABCommands} plugin using the
	 * {@link Plugin#name()} property as an identifier.
	 * <p>
	 * For example, if you create an {@code AwesomeMATLABCommands} class with
	 * {@code name=FOO} and method {@code public void bar()}, after calling this
	 * method you will be able to execute {@code FOO.bar} from within MATLAB.
	 * </p>
	 * <p>
	 * NB: execution of this method requires a valid MATLAB installation.
	 * </p>
	 */
	void initializeCommands();

	/**
	 * Attempts to create a variable in MATLAB with the give name and value.
	 * <p>
	 * NB: execution of this method should only be called from within a valid
	 * MATLAB installation.
	 * </p>
	 */
	void makeMATLABVariable(String name, Object value);
}
