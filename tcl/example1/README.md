Requirements
------------

* Tcl 8.6
* yajl-tcl package
* tclTLS package

You can download Tcl from https://www.tcl.tk/. You may need to also install the Tcl packagees that are referenced.
The yajl-tcl package can be downloaded from https://github.com/flightaware/yajl-tcl/releases
The tcltls package can be downloaded from https://core.tcl.tk/tcltls/wiki/Download

On FreeBSD systems, you can use the command:
    pkg install tcl86 yajl-tcl tcltls


What to change
--------------

Substitute your actual username and API key in the variables at the top of the program.

Change/remove count on the number of messages received.


Run commands
------------

    tclsh example1.tcl
