/*
 * #%L
 * MATLAB scripting language plugin.
 * %%
 * Copyright (C) 2014 - 2021 Board of Regents of the University of
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * MATLAB scripting integration tests (requires a MATLAB installation).
 *
 * @author Mark Hiner
 */
public class MATLABIT {

	private Context context;
	private ScriptService scriptService;

	@Before
	public void setUp() {
		context = new Context();
		scriptService = context.getService(ScriptService.class);

	}

	@After
	public void tearDown() {
		context.dispose();
		context = null;
		scriptService = null;
	}

	/**
	 * Simple script test for executing a basic MATLAB command and checking the
	 * return value.
	 */
	@Test
	public void testBasic() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final String script = "(1+2)";
		final ScriptModule m = scriptService.run("add.m", script, true).get();
		assertTrue(equalDoubleArrays(new double[] { 3.0 }, (double[]) m
			.getReturnValue()));
	}

	/**
	 * MATLAB supports multi-line notation via {@code ...} at the end of each
	 * line. Test that this functionality works in scripting as well.
	 */
	@Test
	public void testBasicMultiline() throws InterruptedException,
		ExecutionException, IOException, ScriptException
	{
		final String script = "(1+...\n2)";
		final ScriptModule m = scriptService.run("add.m", script, true).get();
		assertTrue(equalDoubleArrays(new double[] { 3.0 }, (double[]) m
			.getReturnValue()));
	}

	/**
	 * Test the setting and retrieval of local variables within MATLAB.
	 */
	@Test
	public void testLocals() throws ScriptException {
		final ScriptLanguage language = scriptService.getLanguageByExtension("m");
		final ScriptEngine engine = language.getScriptEngine();
		assertEquals(MATLABScriptEngine.class, engine.getClass());
		engine.put("hello", 17);
		final double[] expected = { 17.0 };
		assertTrue(equalDoubleArrays(expected, (double[]) ((Object[]) engine
			.eval("hello"))[0]));
		assertTrue(equalDoubleArrays(expected, (double[]) engine.get("hello")));

		final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.clear();
		assertEquals(null, bindings.get("hello"));
	}

	/**
	 * Test an {@code if-else} block to ensure multiline expressions work as
	 * intended.
	 */
	@Test
	public void testIfElse() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final String script =
			"if (1 == 2)\n" + "   testVar = 75\n" + "else\n" + "   testVar = 42\n"
				+ "end\n";
		scriptService.run("ifelse.m", script, true).get();
		// There is no return value from this script, but it should create a new
		// "testVar" local variable and initialze its value, so we can attempt
		// reading.
		final Object result =
			scriptService.getLanguageByName("MATLAB").getScriptEngine()
				.get("testVar");
		assertTrue(result != null);
		assertTrue(result instanceof double[]);
		assertTrue(equalDoubleArrays(new double[] { 42.0 }, (double[]) result));
	}

	/**
	 * Ensures that comments do not interfere with script execution.
	 */
	@Test
	public void testComments() throws IOException, ScriptException,
		InterruptedException, ExecutionException
	{
		final String script =
			"% comment line one\n" + "% comment line two\n" + "if (1 == 2)\n"
				+ "   testVar = 75\n" + "else\n" + "% comment line three\n"
				+ "   testVar = 42\n" + "end\n";
		scriptService.run("ifelse.m", script, true).get();
		final Object result =
			scriptService.getLanguageByName("MATLAB").getScriptEngine()
				.get("testVar");
		assertTrue(result != null);
		assertTrue(result instanceof double[]);
		assertTrue(equalDoubleArrays(new double[] { 42.0 }, (double[]) result));
	}

	// -- Helper methods --

	/**
	 * Helper method to compare two double arrays
	 *
	 * @return True iff the two arrays contain equivalent values
	 */
	private boolean equalDoubleArrays(final double[] arr1, final double[] arr2) {
		if (arr1.length != arr2.length) return false;

		for (int i = 0; i < arr1.length; i++) {
			if (Double.compare(arr1[i], arr2[i]) != 0) return false;
		}

		return true;
	}
}
