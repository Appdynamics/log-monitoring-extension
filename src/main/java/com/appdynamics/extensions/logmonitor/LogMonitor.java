/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.processors.FilePointerProcessor;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.logmonitor.util.Constants.DEFAULT_METRIC_PREFIX;
import static com.appdynamics.extensions.logmonitor.util.Constants.MONITOR_NAME;

/**
 * @author Aditya Jagtiani
 */

public class LogMonitor extends ABaseMonitor {
    private static Logger LOGGER = ExtensionsLoggerFactory.getLogger(LogMonitor.class);
    private MonitorContextConfiguration monitorContextConfiguration;
    private Map<String, ?> configYml = Maps.newHashMap();

    @Override
    public String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        monitorContextConfiguration = getContextConfiguration();
        configYml = monitorContextConfiguration.getConfigYml();
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        return (List<Map<String, ?>>) configYml.get("logs");
    }

    @Override
    public void doRun(TasksExecutionServiceProvider taskExecutor) {
        List<Map<String, ?>> logsFromConfig = (List<Map<String, ?>>) configYml.get("logs");
        List<Log> logsToMonitor = LogMonitorUtil.getValidLogsFromConfig(logsFromConfig, (String) configYml.get("metricPrefix"));
        FilePointerProcessor filePointerProcessor = new FilePointerProcessor();
        for (Log log : logsToMonitor) {
            LOGGER.info("Starting the Log Monitoring Task for log : " + log.getDisplayName());
            LogMonitorTask task = new LogMonitorTask(monitorContextConfiguration, taskExecutor.getMetricWriteHelper(),
                    log, filePointerProcessor);
            taskExecutor.submit(log.getDisplayName(), task);
        }
    }
}