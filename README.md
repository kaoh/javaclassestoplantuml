# Introduction

This library generates [PlantUML](http://plantuml.com/class-diagram) class diagrams from Java classes.

# Features

The library in its current state was created for getting a data model presentation of entities or DTOs.

This is the first version and has the following limitations:

* No methods are included
* No generics on classes are considered
* Only the passed package is scanned and no outside package
* It might be nice to add documentation based on supplied sources
* it might be nice to support some stereotypes based on existing annotation

# Usage

```
        String dbModel = new Converter("mypackage.foo.bar").convert();
        FileUtils.writeStringToFile(new File(dbModel.pu"), dbModel, StandardCharsets.UTF_8);

```

# Deployment + Release

See https://central.sonatype.org/pages/apache-maven.html


# For Snapshots

    mvn clean deploy

## For Releases

```
mvn release:clean release:prepare
mvn release:perform
```

Release the deployment using Nexus See https://central.sonatype.org/pages/releasing-the-deployment.html
Or alternatively do it with Maven:

```
cd target/checkout
mvn nexus-staging:release
```
