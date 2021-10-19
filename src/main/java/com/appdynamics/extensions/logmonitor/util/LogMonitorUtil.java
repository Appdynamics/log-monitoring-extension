/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.util;

import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.logmonitor.config.SearchString;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.apache.commons.lang3.StringUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * @author Aditya Jagtiani
 */
public class LogMonitorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMonitorUtil.class);
    private static final String CASE_SENSITIVE_PATTERN = "(?-i)";
    private static final String CASE_INSENSITIVE_PATTERN = "(?i)";

    public static String resolvePath(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }

        //for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }

        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        return String.format("%s%s%s", jarPath, File.separator, filename);
    }

    public static List<SearchPattern> createPattern(List<SearchString> searchStrings) {
        List<SearchPattern> searchPatterns = new ArrayList<SearchPattern>();
        if (searchStrings != null && !searchStrings.isEmpty()) {
            for (SearchString searchString : searchStrings) {
                Pattern pattern;
                StringBuilder rawPatternsStringBuilder = new StringBuilder();
                if (searchString.getCaseSensitive()) {
                    rawPatternsStringBuilder.append(CASE_SENSITIVE_PATTERN);
                } else {
                    rawPatternsStringBuilder.append(CASE_INSENSITIVE_PATTERN);
                }
                if (searchString.getMatchExactString()) {
                    rawPatternsStringBuilder.append("(?<=\\s|^)");
                    rawPatternsStringBuilder.append(Pattern.quote(searchString.getPattern().trim()));
                    rawPatternsStringBuilder.append("(?=\\s|$)");
                } else {
                    rawPatternsStringBuilder.append(searchString.getPattern().trim());
                }
                pattern = Pattern.compile(rawPatternsStringBuilder.toString());
                SearchPattern searchPattern = new SearchPattern(searchString.getDisplayName(), pattern,
                        searchString.getCaseSensitive(), searchString.getPrintMatchedString());
                searchPatterns.add(searchPattern);
            }
        }
        return searchPatterns;
    }

    public static void closeRandomAccessFile(OptimizedRandomAccessFile randomAccessFile) {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException ex) {
                LOGGER.debug("An error occurred while closing the random access file : " + ex);
            }
        }
    }

    public static long getCurrentFileCreationTimeStamp(File file) throws IOException {
        Path p = Paths.get(file.getAbsolutePath());
        BasicFileAttributes view
                = Files.getFileAttributeView(p, BasicFileAttributeView.class)
                .readAttributes();
        return view.creationTime().toMillis();
    }

    public static FilePointer getLatestFilePointer(CopyOnWriteArrayList<FilePointer> filePointers) {
        return Collections.max(filePointers, new Comparator<FilePointer>() {
            public int compare(FilePointer file1, FilePointer file2) {
                if (file1.getFileCreationTime() > file2.getFileCreationTime())
                    return 1;
                else if (file1.getFileCreationTime() < file2.getFileCreationTime())
                    return -1;
                return 0;
            }
        });
    }

    public static List<Log> getValidLogsFromConfig(List<Map<String, ?>> logsFromConfig, String metricPrefix) {
        List<Log> validLogs = new ArrayList<Log>();
        for (Map<String, ?> logFromConfig : logsFromConfig) {
            try {
                Log log = initializeLog(logFromConfig, metricPrefix);
                validateLog(log);
                validLogs.add(log);
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Invalid Log Configuration : " + logFromConfig.get("displayName"), ex);
            }
        }
        return validLogs;
    }

    private static void validateLog(Log log) {
        if (StringUtils.isBlank(log.getLogDirectory())) {
            throw new IllegalArgumentException("Log directory must not be blank.");
        }
        if (StringUtils.isBlank(log.getLogName())) {
            throw new IllegalArgumentException("Log name must not be blank.");
        }
        if (log.getSearchStrings() == null || log.getSearchStrings().isEmpty()) {
            throw new IllegalArgumentException("You must provide at least one search string.");
        }
    }

    private static Log initializeLog(Map<String, ?> currentLogFromConfig, String metricPrefix) {
        Log log = new Log();
        log.setDisplayName((String) currentLogFromConfig.get("displayName"));
        log.setLogName((String) currentLogFromConfig.get("logName"));
        log.setLogDirectory((String) currentLogFromConfig.get("logDirectory"));
        log.setSearchStrings(initializeSearchStrings(currentLogFromConfig));

        if (currentLogFromConfig.containsKey("encoding")) {
            String encodingFromConfig = (String) currentLogFromConfig.get("encoding");
            if (!StringUtils.isBlank(encodingFromConfig) && isValidEncodingType(encodingFromConfig, log.getDisplayName())) {
                log.setEncoding(encodingFromConfig);
            }
        }
        return log;
    }

    private static List<SearchString> initializeSearchStrings(Map<String, ?> currentLogFromConfig) {
        List<SearchString> searchStrings = Lists.newArrayList();
        List<Map<String, ?>> searchStringsForCurrentLog = (List) currentLogFromConfig.get("searchStrings");
        for (Map<String, ?> searchStringFromLog : searchStringsForCurrentLog) {
            SearchString searchString = new SearchString();
            searchString.setDisplayName((String) searchStringFromLog.get("displayName"));
            searchString.setPattern((String) searchStringFromLog.get("pattern"));
            searchString.setMatchExactString((Boolean) searchStringFromLog.get("matchExactString"));
            searchString.setCaseSensitive((Boolean) searchStringFromLog.get("caseSensitive"));
            searchString.setPrintMatchedString((Boolean) searchStringFromLog.get("printMatchedString"));
            searchStrings.add(searchString);
        }
        return searchStrings;
    }

    private static boolean isValidEncodingType(String encodingFromConfig, String logDisplayName) {
        for (EncodingType encodingType : EncodingType.values()) {
            if (encodingType.getEncodingType().equals(encodingFromConfig)) {
                return true;
            }
        }
        LOGGER.error("Found Unsupported/Invalid Encoding type log : {}", logDisplayName);
        return false;
    }


    public static void convertToUTF8Encoding(File file, String charset) throws Exception {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        OutputStream outputStream = new FileOutputStream(file, false);
        Writer outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
        outputStreamWriter.write(sb.toString());
        outputStreamWriter.close();
    }

    public static List<Metric> getFinalMetricList(Map<String, Metric> metricMap) {
        List<Metric> metrics = Lists.newArrayList();
        for (Map.Entry<String, Metric> metric : metricMap.entrySet()) {
            metrics.add(metric.getValue());
        }
        return metrics;
    }

    public static CopyOnWriteArrayList<String> prepareEventsForPublishing(List<LogEvent> eventsToBePublished) {
        CopyOnWriteArrayList<String> events = new CopyOnWriteArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
        for (LogEvent logEvent : eventsToBePublished) {
            try {
                events.add(mapper.writeValueAsString(logEvent));
            } catch (Exception ex) {
                LOGGER.error("Error encountered while publishing LogEvent {} for log {}", logEvent, logEvent.getLogDisplayName(), ex);
            }
        }
        return events;
    }
}