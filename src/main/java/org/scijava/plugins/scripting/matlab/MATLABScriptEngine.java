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
 * <p>
 * NB: we use <a
 * href="https://code.google.com/p/matlabcontrol/">MatlabControl</a> to
 * interpret MATLAB scripts. There are some limitations of this API:
 * <ul>
 * <li>Each line of the script needs to be atomically evaluatable. That is, it
 * must be passable to an {@code eval} call in MATLAB. If you are unfamiliar
 * with the notation for converting traditionally multi-line commands, such as
 * an {@code if...else} block, to single-line, each logical statement is
 * replaced by a comma. For example, you can write: {@code if 1<2, a=3, end}</li>
 * <li>If you need to split up multiple lines, use the MATLAB multi-line
 * character at the end of each line: {@code ...}</li>
 * <li>If you are running from within MATLAB, you will see an error message with
 * each evaluation. This is due to the API restrictions on the function calling
 * from MatlabControl, regarding return values. As long as you see the
 * appropriate output for your command, the error messages can be disregarded.</li>
 * </ul>
 * </p>
 *
 * @author Mark Hiner
 */
public class MATLABScriptEngine extends AbstractScriptEngine {

	private static final String COMMENT = "%";

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
		try {
			while (!thread.isInterrupted()) {
				String command = "sprintf('";
				while (bufReader.ready()) {
					final String line = bufReader.readLine();
					if (line == null) break;

					// NB: we have to manually exclude comment lines in MATLAB. Otherwise,
					// the newline characters themselves on the comment lines will be
					// commented out and ignored - resulting in the first true line of
					// code being skipped unintentionally.
					if (line.matches("^[^\\w]*" + COMMENT + ".*")) continue;
					command += line + "\\n";
				}
				command += "')";

				// Evaluate the command
				// NB: the first eval turns a multi-line command into something properly
				// formatted for MATLAB, stored in the MATLAB variable "ans".
				// We then have to evaluate "ans". However, the eval methods of
				// MatlabControl force "eval(' + args + ')" and "eval('ans')" displays
				// the string literal "ans", whereas "eval(ans)" actually evaluates
				// whatever is stored in ans. We wan the latter behavior, thus the
				// nested eval.
				proxy.eval(command);

				try {
					// Attempt to get a return value
					finalResult = proxy.returningEval("eval(ans)", 1);
				}
				catch (final MatlabInvocationException e) {
					// no return value. Just eval and be done.
					// NB: modified MATLAB variables can be accessed via the bindings.
					proxy.eval("eval(ans)");
				}
				break;
			}
		}
		catch (final Exception e) {}

		return finalResult;
	}

	/**
	 * @return A {@link BufferedReader} view of the provided {@link Reader}.
	 */
	private BufferedReader makeBuffered(final Reader reader) {
		if (BufferedReader.class.isAssignableFrom(reader.getClass())) return (BufferedReader) reader;
		return new BufferedReader(reader);
	}
}
