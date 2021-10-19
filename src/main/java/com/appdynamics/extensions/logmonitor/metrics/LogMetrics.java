/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */


package com.appdynamics.extensions.logmonitor.metrics;

import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Aditya Jagtiani
 */

public class LogMetrics {
    private String metricPrefix;
    private CopyOnWriteArrayList<FilePointer> filePointers = new CopyOnWriteArrayList<FilePointer>();
    private CopyOnWriteArrayList<LogEvent> eventsToBePublished = new CopyOnWriteArrayList<LogEvent>();
    private ConcurrentHashMap<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public void add(String metricName, String metricPath) {
        BigInteger value;
        if (metrics.containsKey(metricName)) {
            value = new BigInteger(metrics.get(metricName).getMetricValue()).add(BigInteger.ONE);
        } else {
            value = BigInteger.ONE;
        }
        add(metricName, new Metric(metricName, String.valueOf(value), metricPath));
    }

    public void add(String metricName, Metric metric) {
        metrics.put(metricName, metric);
    }

    public CopyOnWriteArrayList<FilePointer> getFilePointers() {
        return this.filePointers;
    }

    public void updateFilePointer(FilePointer filePointer) {
        filePointers.add(filePointer);
    }

    public ConcurrentHashMap<String, Metric> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public void addLogEvent(LogEvent logEvent) {
        eventsToBePublished.add(logEvent);
    }

    public CopyOnWriteArrayList<LogEvent> getEventsToBePublished() {
        return eventsToBePublished;
    }
}