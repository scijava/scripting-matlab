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

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Random;

import javax.script.ScriptException;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

import org.scijava.Context;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
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

	@Parameter
	private OptionsService optionsService;

	public MATLABScriptEngine(final Context context) {
		engineScopeBindings = new MATLABBindings();
		context.inject(this);
		context.inject(engineScopeBindings);
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
		final MATLABOptions options =
			optionsService.getOptions(MATLABOptions.class);
		final MatlabProxy proxy = MATLABControlUtils.proxy(options);
		Object finalResult = null;
		try {
			final String scriptVar = "scijava_script" + new Random().nextInt(999999);
			final StringBuilder command =
				new StringBuilder(scriptVar + " = sprintf('");
			String line = "";
			while ((line = bufReader.readLine()) != null) {
				// NB: we have to manually exclude comment lines in MATLAB. Otherwise,
				// the newline characters themselves on the comment lines will be
				// commented out and ignored - resulting in the first true line of
				// code being skipped unintentionally.
				if (line.matches("^[^\\w]*" + COMMENT + ".*")) continue;
				else if (line.matches(".*[\\w].*" + COMMENT + ".*")) {
					// We need to strip out any comments, as they consume the newline
					// character leading to incorrect script parsing.
					line = line.substring(0, line.indexOf(COMMENT));

					// Add escaped single quotes where needed
					line = line.replaceAll("'", "\''");

					command.append(line);
				}
				else command.append(line);

				command.append("\\n");
			}
			command.append("')");

			// NB: this first eval turns a multi-line command into something properly
			// formatted for MATLAB, stored in a temporary MATLAB variable
			// We then have to evaluate this variable. However, the eval methods of
			// MatlabControl force "eval(' + args + ')" and "eval('var')" displays
			// the string literal "var", whereas "eval(var)" actually evaluates
			// whatever is stored in var. We want the latter behavior, thus the
			// need for a nested eval.
			proxy.eval(command.toString());

			// Perform nested eval.. with or without a return value
			try {
				// Attempt to get a return value
				finalResult = proxy.returningEval("eval(" + scriptVar + ")", 1);
			}
			catch (final MatlabInvocationException e) {
				// no return value. Just eval and be done.
				// NB: modified MATLAB variables can be accessed via the bindings.
				proxy.eval("eval(" + scriptVar + ")");
			}

			proxy.eval("clearvars " + scriptVar);
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
