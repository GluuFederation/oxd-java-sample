# About oxd-java-sample

A web application that demostrates how to use oxd-java library to perform user authentication against an openID Connect provider (OP).

This demo app uses basic well-known technologies part of the Java EE 7 web profile. We strove for not using additional frameworks in order to facilitate the understanding of project structure as well as its code.

**Note**: At Gluu, we already have an oxd-java sample app that leverages the [spring framework](https://gluu.org/docs/oxd/3.1.1/libraries/framework/spring/) that might be of your interest.

Having this application up and running is very straightforward. You don't have to go through an endless list of steps for installation and configuration. Once the [prereq](#requisites) software is installed, you are almost done.

## Requisites

1. Java 8+

Install [Java Standard Edition](http://www.oracle.com/technetwork/java/javase/downloads/2133151) version 8 or higher.

2. Maven 3+

Download [maven](https://maven.apache.org/download.cgi) and follow the simple installation instructions. Ensure the `bin` directory is added to your PATH.

3. An OpenID Connect Provider (OP), like the Gluu Server

Learn how to download and install Gluu server by visiting this [page](https://gluu.org/docs/ce/installation-guide/).

4. oxd-server

Download and install [oxd-server](https://gluu.org/docs/oxd/4.0/). For the purposes of this demo app, built-in default configuration files will work.

## Run

You can run the app without assembling a war file or installing a servlet container. This is achieved by using the Jetty Maven Plugin, which allows users to easily run web applications.

1. Ensure all required components are up and running

Double check you have your Gluu Server and oxd-server accessible.

2. Clone or download the project to your local disk

If you have `git` installed, just open a console and run:

```
git clone https://github.com/GluuFederation/oxd-java-sample.git
```

if not, just visit [this page](https://github.com/GluuFederation/oxd-java-sample) and click the button labeled "clone or download" to download the project as a zip. Decompress the file to a destination of your choosing, then open a console and `cd` to the project's directory.

3. Start the app

Issue the following command:

```
mvn jetty:run
```

Depending on your connection speed and computer performance, you will have to wait a couple of minutes for the first time you run this command. It will download all required dependencies and do some configurations on your behalf.

Once you see in the console a message like `... INFO:oejs.Server:main: Started @XXXms` you will know all is already set. Then open a browser and point to `https://localhost:8463/`.


## Kill

Press `Ctrl+C` on the console to stop the application.


## What happens upon start

When the app is starting, it will try to automatically interact with an available oxd-server and attempt to register a site. It does so by searching for connection parameters in local disk, Java system properties or just assuming typical default values.

Site Registration is an important API operation since it supplies **"oxd-id"**, a value required to perform any operation of the API.

If no site registration was possible upon start, the UI of the app will show you a warning stating that action needs to be taken and will take you to a form to complete/provide required values.

Every time a Site Registration is successfully performed by means of the UI, the settings are saved to disk (in a temp directory of your OS). This way there is no need to re-enter info after subsequent restarts.

## Supplying parameters to the app

You can also provide specific values upon start to override the default values used when no file exists in the temporary directory. The way to supply values is by passing Java properties a in the following:

```
mvn -Dprop1=value1 -Dprop2=value2 ... jetty:run
```

The table below lists the set of properties you can provide:

|Name|Description|Example value|
|-|-|-|
|**oxd.server.op-host**|The location of the OpenID Provider|https://my.gluu-server.com|
|**oxd.server.host**|The name of host where oxd-server is located|localhost|
|**oxd.server.port**|The port of oxd-server|8080|
|**oxd.server.acr-values**|A comma-separated list of acrs that will be used in Site Registration and Get Authorization URL operations of the API|`auth_ldap_server`|
|**oxd.server.scopes**|A comma-separated list of scopes supported by the OP that will be used in Site Registration operation|openid, profile|
|**oxd.sample.host**|By default this app is accessible at https://localhost:8463/. With this property you can provide a different host name|my.own.box|
|**oxd.sample.port**|By default this app runs on port 8463. With this property you can provide a different port|8081|
|**oxd.sample.skip-conf-file**|If this property is present, the app will ignore the settings file if any|Any value (even empty) will work|

The example above shows how to start the app bound to port 1234, using an oxd-https-extension located at `https://my.oxd-ext.org` and an OP located at `https://my.op-provider.com`.

```
mvn 	-Doxd.sample.skip-conf-file -Doxd.sample.port=1234
	-Doxd.server.is-https -Doxd.server.host=my.oxd-ext.org -Doxd.server.port=443 -Doxd.server.op-host=https://my.op-provider.com
	jetty:run
```

This way your application will be accessible at https://localhost:1234/.

Note that **https** MUST always be used. The project files `jetty-ssl.xml`, `jetty-https.xml` and `keystore` already automate the setup in order to support SSL.

## Deep diving the code

This app is organized as a Maven project, so it adheres to usual maven's structure conventions.

If you are interested in the logic behind project, for instance to understand how `oxd-java` library is actually being used, keep reading.

The following table depicts the overall anatomy of the application:

|Location|Description|
|-|-|
|`webapp` folder|basic UI pages (facelets) and templates|
|`webapp/static`|CSSs and javascript assets|
|`webapp/oidc`|UI pages implementing a sample authentication workflow|
|package `org.gluu.oxd.sample.listener`|Triggers execution of startup logic|
|package `org.gluu.oxd.sample.bean`|Beans that back UI pages, hold configurations, and interact with oxd-server|

The last row (`org.xdi.oxd.sample.bean`) deserves a deeper look. Particularly the class `OxdService` that represents an application-scoped bean employed to issue the API calls to oxd via oxd-java library. See how maven's `pom.xml` file lists `oxd-common` and `oxd-client` as one of the first required dependencies for the project.

The public methods in `OxdService` map directly to OpenID Authorization Code flow steps.

All API calls resemble the way it's already depicted in the oxd-java [doc page]https://gluu.org/docs/oxd/4.0/api/) so we are not getting into those details here. Just note that in this project most of parameters passed are obtained directly from an instance of `OxdConfig` class, which is an application-scoped bean that holds configuration data. These values are set early during [application start](#what-happens-upon-start) or updated when you use the "settings" page of the application.

Also worth noting is that parameters sent are always wrapped in instances of classes belong to package [`org.gluu.oxd.common.params`](https://github.com/GluuFederation/oxd/tree/version_4.0/oxd-common/src/main/java/org/gluu/oxd/common/params).

## Java docs

In terms of size this is a tiny application but if you want to read API docs of the few classes, just issue:

```
mvn javadoc:javadoc
```

and navigate to folder `target/site/apidocs`.

## FAQ

### The console shows errors upon start, what's wrong?

As the app attemts to do Site Registration when starting, this may lead to some stack traces shown if the operation was not successful. This is normal for the very first time you run it if you just issued `mvn jetty:run` (because there is no actual way to infer for instance, the URL of the OP you are trying to use). 

An error trace like 

```
ERROR oxd.sample.bean.OxdService OxdService.java:73- Connection refused: connect
...
WARN  oxd.sample.bean.OxdService OxdService.java:76- Registration failed
```

can be ignored. Once you get to the app's home page, you will be presented with a form to supply missing values and execute Site Registration.


### How do I deploy this app in a servlet container?

The project was designed so configuration and deployment tasks were minimal through the use of Maven build tool and the Jetty Maven Plugin.

If you still want to make a separate deployment in a Jetty container, you may have to create a Jetty base (use [these instructions](http://www.eclipse.org/jetty/documentation/current/quickstart-running-jetty.html) as a guide) and do [SSL](http://www.eclipse.org/jetty/documentation/current/configuring-ssl.html) configurations required.

To generate your war file, you will have to use the Maven WAR plugin and do adjustments to `pom.xml` file of project. For more information see corresponding [plugin's page](https://maven.apache.org/plugins/maven-war-plugin/).

Instructions on how to run the project using a different servlet container or Java application server is out of the scope of this document.


### What to do if "Check your settings or the console output" appears in the UI

Take a look at the error traces to help diagnose the problem. This is often due to connectivity problems, e.g: oxd-server is down or it cannot reach your OP. 