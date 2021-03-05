# Vaccine Sentinel for Massachusetts

A program to watch for vaccine availability in MA. When availability changes,
it will invoke the notificationScript as many as 3 times
(once every 5 minutes) until the next change occurs. The notificationScript
must be readable and executable. The number of sites available and the full
list of available sites are sent in the environment variables NUM_SITES
and LOCATIONS respectively. It's up to use to choose how you want to be
notified.

VaccineSentinelMA piggybacks on the hard work done by the fine folks who run 
[MACovidVaccines.com](https://macovidvaccines.com). They deserve all
the credit for collecting the data. Sentinel just watches it.

To build, just run:

```shell script
./gradlew shadowJar
```

To run the project as is, simply invoke

```shell script
java -jar build/libs/VaccineSentinelMA-all.jar
```

By default, VaccineSentinelMA will look for a script in the current directory
called `sendNotice`. It must be set up as a "shebang" script. Look
in the examples directory for some sending scripts. To test one out,
just set NUM_SITES to some number in your shell and run the script.
Most of them need at least a little modification to enter your
own credentials.

If you want to change the name of the script, you can override
the default value with

```shell script
java -jar build/libs/VaccineSentinelMA-all.jar --@sentinel.notificationScript /path/to/another/script
```

If you're running the sentinel behind a proxy, specify the proxy's URL like this:

```shell script
java -jar build/libs/VaccineSentinelMA-all.jar --@sentinel.proxyURL http://my-proxy-host:80/
```

