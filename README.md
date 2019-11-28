# Registry core

This provides an implemention of the Linked Data Registry originally developed under ukl-registry-poc.

The implemention runs as a java web application. The war is self-contained and includes an example starting configuration and set of UI templates and style sheets. However, it will detect and use an external set of configuration files if available.

For information on installing and operating a registry instance see the wiki:
   * [Build](https://github.com/UKGovLD/registry-core/wiki/Build)
   * [Installation](https://github.com/UKGovLD/registry-core/wiki/Installation)
   * [Configuration](https://github.com/UKGovLD/registry-core/wiki/Configuration)
   * [Operation](https://github.com/UKGovLD/registry-core/wiki/Operation)
 
Download most recent full release: [registry-core-2.1.0.war](https://s3-eu-west-1.amazonaws.com/ukgovld/release/com/github/ukgovld/registry-core/2.1.0/registry-core-2.1.0.war) - 
[Release Notes 2.1.0](https://github.com/UKGovLD/registry-core/wiki/Release-2.1.0)

## Project governance

See:
   * [Project governance](https://github.com/der/ukl-registry-poc/wiki/Project-Governance)


## Build & run

```
mvn clean install
mvn tomcat:run
```
Go to http://localhost:8080.