/*
 * #%L
 * MATLAB scripting language plugin.
 * %%
 * Copyright (C) 2014 - 2017 Board of Regents of the University of
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
import matlabcontrol.MatlabProxyFactoryOptions.Builder;

/**
 * Utility class for maintaining a single entry point to the MATLAB executable.
 *
 * @author Mark Hiner
 */
public final class MATLABControlUtils {

	// -- Cached proxy --

	private static MatlabProxy proxy = null;

	private MATLABControlUtils() {
		// Private constructor to prevent utility class instantiation
	}

	// -- Public API --

	/**
	 * @return True if there is an active MATLAB connection.
	 */
	public static boolean hasProxy() {
		return proxy != null;
	}

	/**
	 * A running MATLAB instance can only have one proxy active at a time.
	 * Attempting to create multiple proxies will cause additional instances of
	 * MATLAB to be spawned - which we would like to avoid. Thus this method will
	 * cache a proxy and return it if it is still connected. If not, a new proxy
	 * will be generated with the default configuration:
	 * <ul>
	 * <li>hidden = true</li>
	 * <li>multithreaded = true</li>
	 * <li>license = null</li>
	 * </ul>
	 *
	 * @return An active {@link MatlabProxy}.
	 */
	public static MatlabProxy proxy() {
		return proxy(true);
	}

	/**
	 * As {@link #proxy()}, with a specification flag for whether or not the
	 * MATLAB instance should be hidden and automatically exit when the calling
	 * JVM is shut down.
	 *
	 * @param hidden If MATLAB should run hidden (no command prompt)
	 * @return An active {@link MatlabProxy}.
	 */
	public static MatlabProxy proxy(final boolean hidden) {
		return proxy(hidden, true);
	}

	/**
	 * As {@link #proxy(boolean)} with a specification flag for whether or not
	 * MATLAB should be allowed to run multithreaded.
	 *
	 * @param hidden If MATLAB should run hidden (no command prompt)
	 * @param multithreaded If MATLAB is allowed to be multithreaded
	 * @return An active {@link MatlabProxy}.
	 */
	public static MatlabProxy proxy(final boolean hidden,
		final boolean multithreaded)
	{
		return proxy(hidden, multithreaded, null);
	}

	/**
	 * As {@link #proxy(boolean, boolean)} with a specification string for the
	 * MATLAB license file.
	 *
	 * @param hidden If MATLAB should run hidden (no command prompt)
	 * @param multithreaded If MATLAB is allowed to be multithreaded
	 * @param license Path to MATLAB license
	 * @return An active {@link MatlabProxy}.
	 */
	public static MatlabProxy proxy(final boolean hidden,
		final boolean multithreaded, final String license)
	{
		if (proxy == null || !proxy.isConnected()) {
			try {
				proxy = factory(hidden, multithreaded, license).getProxy();
			}
			catch (final MatlabConnectionException e) {
				throw new IllegalStateException(e);
			}
		}
		return proxy;
	}

	/**
	 * As {@link #proxy(boolean, boolean, String)} using the settings in the given
	 * {@link MATLABOptions}
	 *
	 * @param options - Cached options for proxy configuration
	 * @return An active {@link MatlabProxy}.
	 */
	public static MatlabProxy proxy(final MATLABOptions options) {
		final boolean hidden = options.isHidden();
		final boolean multithreaded = options.isMultithreaded();
		final String license = options.licensePath();

		return proxy(hidden, multithreaded, license);
	}

	// -- Helper methods --

	/**
	 * Returns a factory for creating {@link MatlabProxy} instances with the
	 * specified configuration
	 *
	 * @return A configured {@link MatlabProxyFactory}.
	 */
	private static MatlabProxyFactory factory(final boolean hidden,
		final boolean multithreaded, final String license)
	{
		Builder builder = new MatlabProxyFactoryOptions.Builder();
		builder = builder.setUsePreviouslyControlledSession(true);
		builder = builder.setUseSingleComputationalThread(multithreaded);
		builder = builder.setHidden(hidden);
		if (license != null) builder = builder.setLicenseFile(license);
		return new MatlabProxyFactory(builder.build());
	}
}
