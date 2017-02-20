Requirements
------------

* Java SE 8
* JSON parsing library

Depending on Java platform used, there may or may not be a standard JSON parsing library.
However, there are plenty of libraries to choose from http://www.json.org/

This example uses the google-gson library. Download google-gson library from https://github.com/google/gson


What to change
--------------

Substitute your actual username and API key in the initiation_command.

Change/remove limit on the number of messages received.



Running the example
-------------------
Eclipse or NetBeans IDE could make development and testing easier.

Alternatively you can run commands such as (Win32):

Compile:

    javac -classpath "xxx\gson-2.3.1.jar" SSL_Client_gson.java

Run:

    java -classpath "xxx\gson-2.3.1.jar;." SSL_Client_gson
