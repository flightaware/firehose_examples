Requirements
------------

* Java SE 8
* JSON parsing library

Depending on Java platform used, there may or may not be a standard JSON parsing library.
However, there are plenty of libraries to choose from http://www.json.org/

This example uses Json-simple "a simple lightweight Java toolkit for
JSON". Download Json-simple library from https://github.com/fangyidong/json-simple


What to change
--------------

Substitute your actual username and API key in the initiation_command.

Change/remove limit on the number of messages received.



Running the example
-------------------

Eclipse or NetBeans IDE could make development and testing easier.

Alternatively you can run commands such as (Win32):

Compile:

    javac -classpath "xxx\json-simple-1.1.1.jar" SSL_Client_json_simple.java

Run:

    java -classpath "xxx\json-simple-1.1.1.jar;." SSL_Client_json_simple

