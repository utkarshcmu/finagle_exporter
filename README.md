Finagle Exporter
=====

An exporter for [Twitter Finagle](https://twitter.github.io/finagle/), for Prometheus.

## Building and running

`mvn package` to build.

`java -jar target/finagle_exporter-0.1-SNAPSHOT-jar-with-dependencies.jar localhost 9990` to run.

All metrics are exported as gauges.

## Usage
```
java -jar target/finagle_exporter-0.1-SNAPSHOT-jar-with-dependencies.jar <finagle_admin_host> <finagle_admin_port> [exporter_port]
```

By default, exporter port is 9991
