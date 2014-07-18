/*
 * #%L
 * MATLAB scripting language plugin.
 * %%
 * Copyright (C) 2014 Board of Regents of the University of
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

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;

import javax.script.ScriptException;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

import org.scijava.script.AbstractScriptEngine;

/**
 * A MATLAB interpreter.
 *
 * @author Mark Hiner
 */
public class MATLABScriptEngine extends AbstractScriptEngine {

	private static final String MULTI_LINE = "...";

	public MATLABScriptEngine() {
		engineScopeBindings = new MATLABBindings();
	}

	@Override
	public Object eval(final String script) throws ScriptException {
		try {
			return eval(new StringReader(script));
		}
		catch (final Exception e) {
			throw new ScriptException(e);
		}
	}

	@SuppressWarnings("resource")
	@Override
	public Object eval(final Reader reader) throws ScriptException {
		final BufferedReader bufReader = makeBuffered(reader);
		final MatlabProxy proxy = MATLABControlUtils.proxy();
		final Thread thread = Thread.currentThread();
		Object finalResult = null;
		String command = "";
		try {
			while (!thread.isInterrupted()) {
				final String currentLine = bufReader.readLine();
				if (currentLine == null) break;

				command += currentLine;

				// MATLAB can read multi-line commands if they have "..." at the end
				// of each line. MatlabControl however can not evaluate these lines
				// so we need to build the full command.
				if (!command.endsWith(MULTI_LINE)) {
					// Evaluate the command
					try {
						finalResult = proxy.returningEval(command, 1);
					}
					catch (MatlabInvocationException e) {
						// No return value on the script, so just eval the line;
						proxy.eval(command);
					}
					command = "";
				}
				else {
					command += "\n";
				}
			}
		}
		catch (Exception e) { }

		return finalResult;
	}

	/**
	 * @return A {@link BufferedReader} view of the provided {@link Reader}.
	 */
	private BufferedReader makeBuffered(Reader reader) {
		if (BufferedReader.class.isAssignableFrom(reader.getClass())) return (BufferedReader) reader;
		return new BufferedReader(reader);
	}
}
