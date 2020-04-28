Requirements
------------

This example was tested against .NET Core 3.1 under Visual Studio 2019 on Windows 10. It has also been tested against the .NET Core 3.1 SDK Docker Linux container: `mcr.microsoft.com/dotnet/core/sdk:3.1`

Parsing JSON in C# can be done using .NET Framework Classes or by downloading a third party library.

There are plenty of libraries to choose from http://www.json.org/

For this example we are going to use System.Text.Json, which is included with .NET Core 3.0 and higher, however it can also be downloaded from NuGet for certain other .NET Framework versions.


What to change
--------------

Substitute your actual username and API key in the initiation_command.

Change/remove limit on the number of messages received.


Running the example
-------------------
Visual Studio or SharpDeveloper offer IDE that make development and testing easier.

Alternatively, you can run commands (Win32):

Compile:

    dotnet build example1.csproj

Run:

    example1


