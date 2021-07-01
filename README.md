[![](https://github.com/scijava/scripting-matlab/actions/workflows/build-main.yml/badge.svg)](https://github.com/scijava/scripting-matlab/actions/workflows/build-main.yml)

# MATLAB Scripting

This library provides a
[JSR-223-compliant](https://en.wikipedia.org/wiki/Scripting_for_the_Java_Platform)
scripting plugin for the [MATLAB](http://www.mathworks.com/products/matlab/) language.

It is implemented as a `ScriptLanguage` plugin for the [SciJava
Common](https://github.com/scijava/scijava-common) platform, which means that
in addition to being usable directly as a `javax.script.ScriptEngineFactory`,
it also provides some functionality on top, such as the ability to generate
lines of script code based on SciJava events.

For a complete list of scripting languages available as part of the SciJava
platform, see the
[Scripting](https://github.com/scijava/scijava-common/wiki/Scripting) page on
the SciJava Common wiki.

See also:
* [MATLAB Scripting](http://wiki.imagej.net/MATLAB_Scripting)
  on the ImageJ wiki.

Restrictions:
* Requires a valid MATLAB installation!
* [Preferences may not be persistable](http://www.mathworks.com/matlabcentral/answers/894-java-usernodeforpackage-function-fails-under-matlab-on-os-x)
* Basic MATLAB evaluations can be done remotely (from an applicaiton running externally to MATLAB). However, transfer of more complex objects (e.g. via script parameters) requires running of scripts from within MATLAB. See [MIJI](http://fiji.sc/Miji).
