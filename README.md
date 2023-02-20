# Functions Framework for Java

An open source FaaS (Function as a service) framework for writing portable Java functions.

## How to use

A function is typically structured as a Maven project. We recommend using an IDE
that supports Maven to create the Maven project. Add this dependency in the
`pom.xml` file of your project:

```xml
    <dependencies>
        <dependency>
            <groupId>dev.openfunction.functions</groupId>
            <artifactId>functions-framework-api</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
```

If you are using Gradle to build your functions, you can define the Functions
Framework dependency in your `build.gradle` project file as follows:

```groovy
    dependencies {
        implementation 'dev.openfunction.functions:functions-framework-api:1.0.0'
    }

```
