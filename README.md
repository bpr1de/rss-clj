# RSS

A Clojure application designed to parse articles from RSS feeds, and (optionally) publish new articles through the Oracle Notification Service (ONS).

## Building

Build with Leiningen e.g. `lein uberjar`

## Usage

`lein run`

or

`java -jar <this-uberjar.jar> [/path/to/config]`

The path to the configuration file may be local or a URL. In any case, the configuration file will be re-read on each cycle, so it must be durable.

`/path/to/config` may be omitted, in which case the application will first try reading the path from the environment variable `$RSS_CONFIG_PATH`, and failing that, will look for a configuration in `~/.rss`.

The config file must be a valid XML file matching the following example format:

```
<?xml version='1.0' encoding='UTF-8'?>
<topic ocid='ocid1.onstopic.phx.your.ocid.here' client='file' interval='15'>
    <feed link='https://alerts.weather.gov/cap/wwaatmget.php?x=WAC033'/>
    <feed link='https://www.nps.gov/feeds/getNewsRSS.htm?id=mora'/>
</topic>
```

The `interval` is in minutes, and defaults to 10 if omitted.

To use ONS for notifications, define both a topic OCID in the `ocid` attribute and a client type in the `client` attribute of the `topic` tag.  The `ocid` refers to the OCID of your notification topic, and the `client` is a value of `file`, `instance`, or `resource`, for OCI file-based client configuration, instance principal-based client configuration, or resource-principal-based client configuration, respectively.

If the `ocid` or `client` attributes are omitted, articles will be printed to standard output only.

Note that in order to leverage use of ONS, you must have a tenancy configured with an ONS topic defined with one or more subscribers. To use the file-based OCI client, you must have a valid configuration in the `.oci` folder. To use the instance and resource principal-based OCI client, this application must be running on an OCI instance (or container instance) and your tenancy must be configured with a policy to allow the instance to access your topic. See here for guidance: https://docs.oracle.com/en-us/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm

## Docker

You may build the application into an Docker image using the included example Dockerfile:

`docker build -t rss-clj .`

and run it as follows (example):

`docker run -d -e RSS_CONFIG_PATH=/app/rss.xml -v $HOME/.rss:/app/rss.xml rss-clj`

## License

Copyright © 2024 Bryan Phillippe, <bp@darkforest.org>

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.
