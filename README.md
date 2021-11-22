# AppDynamics Log Monitoring Extension

## Use Case
The AppDynamics Log Monitoring Extension monitors the occurrences of configured text or regular expressions in a set of log files, and the sizes of these files. The extension works seamlessly with logs that are constantly generated and rotating from time to time. 

## Prerequisites
1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.

2. Download and install [Apache Maven](https://maven.apache.org/) which is configured with `Java 8` to build the extension artifact from source. You can check the java version used in maven using command `mvn -v` or `mvn --version`. If your maven is using some other java version then please download java 8 for your platform and set JAVA_HOME parameter before starting maven.

3. The extension must be deployed on the same box as the one with the log files you wish to monitor.

## Installation
1. Clone the "log-monitoring-extension" repo using `git clone <repoUrl>` command.
2. Run 'mvn clean install' from log-monitoring-extension. This will produce a LogMonitor-VERSION.zip in the target directory
3. Unzip the file LogMonitor-[version].zip into `<MACHINE_AGENT_HOME>/monitors/`
4. In the newly created directory "LogMonitor", edit the config.yml to configure the parameters (See Configuration section below)
5. Restart the Machine Agent
6. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance|\<Tier\>|Custom Metrics|Log Monitor. If SIM is enabled, look for the 
metric browser under the Servers tab. 

## Configuration

Configure the Log Monitoring Extension by editing the ```config.yml``` & ```monitor.xml``` files in `<MACHINE_AGENT_HOME>/monitors/LogMonitor/`.

### 1. Tier Configuration

Configure the Tier under which the metrics should be reported. This can be done by adding the Tier ID to the metric prefix. 
```metricPrefix: "Server|Component:<TIER_ID>|Custom Metrics|Log Monitor|"``` 

If SIM is enabled, please use the default metric prefix. 
```metricPrefix: "Custom Metrics|Log Monitor|```

### 2. Log Configuration

This includes specifying the location of the logs and the various search strings/regular expressions to be searched for in the logs. For Windows, use escape characters while configuring log directories. For example: C:\\\Directory\\\LogsToMonitor\\\. Note that '\\' is the escape sequence. 
 
There are multiple scenarios that can be configured in the ```logs``` section:

#### 2.1 Static Logs 

Consider a log file, ```myLog.log``` that does not rollover. To search for case sensitive matches of ```ERROR```: 

```
logs:
     - displayName: "Test Log"
       logDirectory: "/Users/XYZ/MyApplication/logs"
       logName: "myLog.log"
       encoding: ""      #Not mandatory. Supported types: UTF8, UTF16, UTF16-LE, UTF16-BE, UTF32, UTF-32LE, UTF32-BE
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

#### 2.2 Dynamic Logs 

Consider a log, ```myLog.log``` that rolls over after reaching a pre-configured file size threshold to ```myLog.log.1```, ```myLog.log.2``` etc. 
To search for case sensitive matches of ```ERROR``` across the entire rollover sequence: 

```
logs:
     - displayName: "Test Log"
       logDirectory: "/Users/XYZ/MyApplication/logs"
       logName: "myLog.log*" #Use of a regex to include the entire rollover set
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

#### 2.3 Common Log Scenarios

1. The ```pattern``` section under searchStrings accepts regular expressions. 

Consider a scenario where you're monitoring errors like the one below:   
```[Thread-1] 29 Apr 2014 12:31:18,680  ERROR MemoryMetricGenerator - Identified major collection bean :ConcurrentMarkSweep```

To get the complete line as a metric: 

```
searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Memory Metric Generator Errors"
            pattern: "(.*)ERROR(.*)"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

2. The extension supports various Unicode character sets that your logs may be encoded in. 
To monitor a non-UTF8 encoded file, add the encoding type under the ```encoding``` section for the log. Supported encoding types are `UTF8, UTF16, UTF16-LE, UTF16-BE, UTF32, UTF-32LE, UTF32-BE` .

```
logs:
     - displayName: "Test UTF-16 Log"
       logDirectory: "/Users/XYZ/MyApplication/UTF16Logs"
       logName: "myLogUTF16LE.log"
       encoding: "UTF16-LE"
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

The ```encoding``` field is not mandatory and can be disregarded in the config.yml unless you're working with non-UTF8 files. 

3. To get only the occurrences of a configured pattern and not the exact pattern match, simply set the ```printMatchedString``` field to false. 
By default, an Occurrences metric is initialized with 0 for each configured pattern, and can be used to create alerts and health rules. 

### 3. Metric Path Replacements

This section can be used to replace any characters in a match with the specified characters. They come into effect only a match is found for the 
original pattern. We have pre-configured three metric character replacers for characters considered invalid by the AppDynamics Metric Browser. 

```
#Replaces characters in metric name with the specified characters.
# "replace" takes any regular expression
# "replaceWith" takes the string to replace the matched characters
metricPathReplacements:
    - replace: ":"
      replaceWith: ";"
    - replace: "|"
      replaceWith: "#"
    - replace: ","
      replaceWith: "#"
```

### 4. Number of Threads 
The extension uses one thread per configured log, and one thread per log file within. Let's consider our initial example: 

```
logs:
     - displayName: "Test Log"
       logDirectory: "/Users/XYZ/MyApplication/logs"
       logName: "myLog.log*"
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

Assuming that your logger settings make ```myLog.log``` rollover to a max of five files (myLog.log.1 to myLog.log.5), the number of threads needed in this case would be 7. 
(One for the log directory, and six for the files within). 

This can be configured using the ```numberOfThreads``` field in the config.yml. 


### 5. Configuring the monitor.xml

Configure the path to the config.yml by editing the ```<task-arguments>``` in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/LogMonitor/` directory: 

```
<task-arguments>
     <!-- config file-->
     <argument name="config-file" is-required="true" default-value="monitors/LogMonitor/config.yml" />
      ....
</task-arguments>
```

Restart the machine agent once this is done. 

### 6. Events Service

Before proceeding with this step, please ensure that all the prerequisites and steps to install the AppDynamics Events Service have been met. Refer to the sub-pages under 
https://docs.appdynamics.com/display/latest/Events+Service+Deployment for detailed information. 

The extension can now publish log matches to the Events Service. Use the following section in the config.yml for this feature: 

```
sendDataToEventsService: true

logMatchOffset: 5

# This field contains the various parameters required to initiate a connection and send data to the AppDynamics Events Service.
eventsServiceParameters:
  host: 
  port: 
  globalAccountName:
  eventsApiKey: 
  useSSL: 
```

The logMatchOffset section appends the specified number of lines with the line containing the actual log match and all 
of this makes the body of an event. This can be particularly useful while trying to search for exceptions and also retrieving the stack trace that follows. 

Note that enabling this feature will not impact the regular delivery of metrics to the metric browser. 


## Metrics

The extension publishes the following metrics: 

**1. File size in bytes** 

For the next two metrics, let's use the following configuration as an example: 

myLog.log 
(This is a static log. It does not rollover and no further content is added to this file)
```
[Thread-1] 29 Apr 2014 12:31:18,647  INFO DynamicServiceManager - Scheduling DynamicServiceManager at interval of 30 seconds
[Thread-1] 29 Apr 2014 12:31:18,647  INFO LifeCycleManager - Started service [DynamicServiceManager]
```

config.yml 
```
metricPrefix: "Server|Component:<TIER_ID>|Custom Metrics|Log Monitor"

logs:
     - displayName: "Test Log"
       logDirectory: "/Users/XYZ/MyApplication/logs"
       logName: "myLog.log"
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Info Statements"
            pattern: "INFO"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

**2. Occurrences of each configured pattern**
When the extension starts, a base occurrence metric for the pattern ```INFO``` is initialized with a value of 0. This value will represent the unique occurrences of ```INFO``` 
observed every minute. It will reset to 0 if no occurrences of ```INFO``` are found in any given minute. This metric is always reported, regardless of the state of 
the ```printMatchedString``` flag and can be used to set up alerts and health rules. 

In this case, the metric will be reported as: 

```Application Infrastructure Performance|<TIER>|Custom Metrics|Log Monitor|Test Log|Info Statements|Occurrences``` with a value of 2 for when the log is read for the first time. The metric then resets to 
0 in the next minute, until the log is repopulated with more ```INFO``` statements. 


**3. Occurrences of matched strings**
This metric is reported only when the ```printMatchedString``` flag is set to true for a searchString. In this case, the following metrics will be reported when the log is read for the first time: 

```Application Infrastructure Performance|<TIER>|Custom Metrics|Log Monitor|Test Log|Info Statements|Occurrences``` with a value of 2 
```Application Infrastructure Performance|<TIER>|Custom Metrics|Log Monitor|Test Log|Info Statements|Matches|INFO``` with a value of 2

Both metrics reset to a value of 0 in the next minute, until the log is repopulated with more ```INFO``` statements. 

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following [document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130) for how to use the Extensions WorkBench

## Troubleshooting
Please follow the steps listed in the [extensions troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might face during the installation of the extension.

## Contributing
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/log-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |4.0.5       |
|Last Update               |14/06/2021 |
|List of Changes           |[Change log](https://github.com/Appdynamics/log-monitoring-extension/blob/main/CHANGELOG.md) |

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.
