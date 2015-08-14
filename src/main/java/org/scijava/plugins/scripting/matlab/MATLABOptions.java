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

import java.io.File;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxyFactoryOptions;

import org.scijava.app.StatusService;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

/**
 * Allows the setting and persisting of {@link MatlabProxyFactoryOptions}
 * configuration.
 *
 * @author Mark Hiner
 */
@Plugin(type = OptionsPlugin.class, label = "MATLAB Options", menu = {
	@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
		mnemonic = MenuConstants.EDIT_MNEMONIC), @Menu(label = "Options"),
	@Menu(label = "MATLAB...") })
public class MATLABOptions extends OptionsPlugin {

	@Parameter
	private StatusService statusService;

	// Fields

	@Parameter(label = "Hide MATLAB UI")
	private boolean hidden = false;

	@Parameter(label = "Allow multithreaded MATLAB")
	private boolean multithreaded = true;

	@Parameter(label = "License file path", required = false)
	private File licenseFile = null;

	@Parameter(label = "Exit MATLAB", persist = false, callback = "endSession")
	private Button endSession;

	// -- Option accessors --

	public boolean isHidden() {
		return hidden;
	}

	public boolean isMultithreaded() {
		return multithreaded;
	}

	public String licensePath() {
		return licenseFile == null ? null : licenseFile.getAbsolutePath();
	}

	// -- Callback methods --

	@SuppressWarnings("unused")
	private void endSession() {
		if (MATLABControlUtils.hasProxy()) {
			try {
				MATLABControlUtils.proxy(this).exit();
				statusService.showStatus("MATLAB shutdown successful");
			}
			catch (final MatlabInvocationException e) {
				statusService.showStatus("MATLAB shutdown failed");
			}
		}
		else {
			statusService.showStatus("No MATLAB connection detected.");
		}
	}
}
