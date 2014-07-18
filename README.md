Scripting-MATLAB
================

Sci-Java scripting plugin allowing MATLAB-language evaluations.

NB: use of this plugin requires a valid MATLAB installation.

Known limitations:
* Preferences may not persistable (see http://www.mathworks.com/matlabcentral/answers/894-java-usernodeforpackage-function-fails-under-matlab-on-os-x)
* If run within ImageJ, setting ImageJ#exitWhenQuitting(true) may cause MATLAB to hang indefinitely when closing.
* Basic MATLAB evaluations can be done remotely (from an applicaiton running externally to MATLAB). However, transfer of objects (e.g. via script parameters) requires running of scripts from within MATLAB. See http://fiji.sc/Miji.
* Simultaneous MATLAB connections (e.g. running scripts on multiple threads) will cause multiple MATLAB instances to be spawned, preventing sharing of any state between them.

Tested MATLAB version(s): MATLAB_R2011b
