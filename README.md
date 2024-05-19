# RSS

A Clojure application designed to parse articles from RSS feeds, and (optionally) publish new articles through the Oracle Notification Service (ONS). May be configured using a local configuration file, a remote configuration file retrieved by URL, or a remote configuration file retrieved through Oracle Object Storage Service (OSS).

## Building

Build with Leiningen e.g. `lein uberjar`

## Usage

`lein run`

or

`java -jar <this-uberjar.jar> [config]`

The `config` argument may refer to a local filename, URL, or Object Storage notation. In any case, the configuration file will be re-read on each cycle, so it must be durable.

Object Storage notation takes the form:

`oss:<namespace>:<bucket>:<object>`

where `namespace`, `bucket`, and `object` are required parameters, and `region` is optional.

If `config` is omitted, the application will first try reading it from the environment variable `RSS_CONFIG_PATH`, and failing that, will look for a configuration in `~/.rss`.

The config file must be a valid XML file matching the following example format:

```
<?xml version='1.0' encoding='UTF-8'?>
<topic ons_ocid='ocid1.onstopic.phx.your.ocid.here' interval='15'>
    <feed link='https://alerts.weather.gov/cap/wwaatmget.php?x=WAC033'/>
    <feed link='https://www.nps.gov/feeds/getNewsRSS.htm?id=mora'/>
</topic>
```

The `interval` is in minutes, and defaults to 10 if omitted.

To use ONS for notifications, you must define the topic OCID in the `ons_ocid` attribute and have an authorized means of accessing your tenancy.

To access OCI resources (OSS or ONS), the application will attempt to authenticate as follows:

1. using a resource-principal if the environment variable `OCI_RESOURCE_PRINCIPAL_RPST` is set;
1. using an instance-principal if the environment variable `OCI_INSTANCE_PRINCIPAL_IPST` is set *(note that this is not automatically set by OCI instances; you would need to set it manually)*;
1. configuration-file based authentication e.g. a valid configuration in the `.oci/` folder.

If the `ons_ocid` is omitted or the application cannot authentication to ONS, then articles will be printed to standard output only.

Note that in order to leverage use of ONS, you must have a tenancy configured with an ONS topic defined with one or more subscribers.

Resource and instance principal-based authentication requires that the application be running on an OCI instance (or container instance), and that your tenancy be configured with a policy to allow the instance to access your topic. See here for guidance: https://docs.oracle.com/en-us/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm

## Docker

You may build the application into an Docker image using the included example Dockerfile:

`docker build -t rss-clj .`

and run it as follows (example):

`docker run -d -e RSS_CONFIG_PATH=/app/rss.xml -v $HOME/.rss:/app/rss.xml rss-clj`

## License

Copyright © 2024 Bryan Phillippe, <bp@darkforest.org>

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.
