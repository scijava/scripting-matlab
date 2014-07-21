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

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

/**
 * Utility class for maintaining a single entry point to the MATLAB executable.
 *
 * @author Mark Hiner
 */
public final class MATLABControlUtils {

	private static final MatlabProxyFactoryOptions options =
		new MatlabProxyFactoryOptions.Builder().setHidden(true)
			.setUsePreviouslyControlledSession(true).build();

	private static MatlabProxy proxy = null;

	private MATLABControlUtils() {
		// Private constructor to prevent utility class instantiation
	}

	/**
	 * A running MATLAB instance can only have one proxy active at a time.
	 * Attempting to create multiple proxies will cause additional instances of
	 * MATLAB to be spawned - which we would like to avoid. Thus this method will
	 * cache a proxy and return it if it is still connected. If not, a new proxy
	 * will be generated.
	 *
	 * @return An active {@link MatlabProxy}.
	 */
	public static MatlabProxy proxy() {
		if (proxy == null || !proxy.isConnected()) {
			try {
				proxy = factory().getProxy();
			}
			catch (final MatlabConnectionException e) {
				return null;
			}
		}
		return proxy;
	}

	/**
	 * Returns a factory for creating {@link MatlabProxy} instances with the
	 * following configuration:
	 * <ul>
	 * <li>The MATLAB application window will be hidden</li<
	 * <li>New proxies will attempt to connect to existing MATLAB instances</li>
	 * </ul>
	 *
	 * @return A configured {@link MatlabProxyFactory}.
	 */
	public static MatlabProxyFactory factory() {
		return new MatlabProxyFactory(options);
	}
}
