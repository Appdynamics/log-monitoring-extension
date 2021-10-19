/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.metrics.LogMetrics;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.google.common.collect.Lists;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.*;

/**
 * @author Aditya Jagtiani
 */

public class LogFileManager {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(LogFileManager.class);
    private Log log;
    private FilePointerProcessor filePointerProcessor;
    private MonitorContextConfiguration monitorContextConfiguration;
    private MonitorExecutorService executorService;
    private EventsServiceDataManager eventsServiceDataManager;
    private int offset;

    public LogFileManager(FilePointerProcessor filePointerProcessor, Log log,
                          MonitorContextConfiguration monitorContextConfiguration) {
        this.log = log;
        this.filePointerProcessor = filePointerProcessor;
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.executorService = this.monitorContextConfiguration.getContext().getExecutorService();
    }

    public LogMetrics processLogMetrics() throws Exception {
        LOGGER.info("Starting the metric collection process for log : {}", log.getDisplayName());
        String dirPath = resolveDirPath(log.getLogDirectory());
        File file = getLogFile(dirPath);
        LogMetrics logMetrics = new LogMetrics();
        logMetrics.setMetricPrefix(monitorContextConfiguration.getMetricPrefix());

        if (file != null) {
            try {
                CountDownLatch latch;
                String dynamicLogPath = dirPath + log.getLogName();
                long currentTimeStampFromFilePointer = getCurrentTimeStampFromFilePointer(dynamicLogPath, file.getPath());
                long currentFilePointerPosition = getCurrentFilePointerOffset(dynamicLogPath, file.getPath());
                eventsServiceDataManager = evaluateEventsServiceConfig();
                offset = (Integer) this.monitorContextConfiguration.getConfigYml().get("logMatchOffset");
                if (hasLogRolledOver(dynamicLogPath, file.getPath(), file.length())) {
                    List<File> filesToBeProcessed = getFilesToBeProcessedFromDirectory(currentTimeStampFromFilePointer, dirPath);
                    latch = new CountDownLatch(filesToBeProcessed.size());
                    processRolledOverLogs(filesToBeProcessed, currentTimeStampFromFilePointer, currentFilePointerPosition,
                            logMetrics, latch);
                } else {
                    latch = new CountDownLatch(1);
                    processLogsWithoutRollover(file, latch, currentFilePointerPosition, logMetrics);
                }
                latch.await();
                setNewFilePointer(dynamicLogPath, logMetrics.getFilePointers());
            } catch (Exception ex) {
                LOGGER.error("File I/O issue while processing : " + file.getAbsolutePath(), ex);
            }
        }
        return logMetrics;
    }

    private void processRolledOverLogs(List<File> filesToBeProcessed, long currentTimeStampFromFilePointer,
                                       long currentFilePointerPosition, LogMetrics logMetrics, CountDownLatch latch) throws Exception {
        for (File currentFile : filesToBeProcessed) {
            if (!StringUtils.isBlank(log.getEncoding())) {
                handleFileEncoding(currentFile);
            }
            OptimizedRandomAccessFile randomAccessFile = new OptimizedRandomAccessFile(currentFile, "r");
            if (getCurrentFileCreationTimeStamp(currentFile) == currentTimeStampFromFilePointer) {
                randomAccessFile.seek(currentFilePointerPosition); //found the oldest file, process from CFP
            } else {
                randomAccessFile.seek(0);
            }
            executorService.execute("LogMetricsProcessor", new LogMetricsProcessor(randomAccessFile, log, latch,
                    logMetrics, currentFile, eventsServiceDataManager, offset));
        }
    }

    private void processLogsWithoutRollover(File file, CountDownLatch latch, long currentFilePointerPosition,
                                            LogMetrics logMetrics) throws Exception {
        if (!StringUtils.isBlank(log.getEncoding())) {
            handleFileEncoding(file);
        }
        OptimizedRandomAccessFile randomAccessFile = new OptimizedRandomAccessFile(file, "r");
        randomAccessFile.seek(currentFilePointerPosition);
        executorService.execute("LogMetricsProcessor", new LogMetricsProcessor(randomAccessFile, log, latch, logMetrics,
                file, eventsServiceDataManager, offset));
    }

    private void setNewFilePointer(String dynamicLogPath, CopyOnWriteArrayList<FilePointer> filePointers) {
        FilePointer latestFilePointer = LogMonitorUtil.getLatestFilePointer(filePointers);
        LOGGER.debug("Updating File Pointer with the most recently processed log: {}, pointing to file: {} with the " +
                        "last read position: {} and a creation time stamp of: {}", dynamicLogPath,
                latestFilePointer.getFilename(), latestFilePointer.getLastReadPosition(),
                latestFilePointer.getFileCreationTime());
        filePointerProcessor.updateFilePointer(dynamicLogPath, latestFilePointer.getFilename(),
                latestFilePointer.getLastReadPosition(), latestFilePointer.getFileCreationTime());
    }

    private String resolveDirPath(String confDirPath) {
        String resolvedPath = resolvePath(confDirPath);
        if (!resolvedPath.endsWith(File.separator)) {
            resolvedPath = resolvedPath + File.separator;
        }

        return resolvedPath;
    }

    private long getCurrentTimeStampFromFilePointer(String dynamicLogPath, String actualLogPath) {
        FilePointer filePointer = filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);
        return filePointer.getFileCreationTime();
    }

    private List<File> getFilesToBeProcessedFromDirectory(long currentTimeStampFromFilePointer, String path)
            throws IOException {
        List<File> filesToBeProcessed = Lists.newArrayList();
        File directory = new File(path);

        if (directory.isDirectory()) {
            FileFilter fileFilter = new WildcardFileFilter(log.getLogName());
            File[] files = directory.listFiles(fileFilter);

            if (files != null && files.length > 0) {
                for (File file : files) {
                    long currentFileCreationTime = getCurrentFileCreationTimeStamp(file);
                    if (currentFileCreationTime >= currentTimeStampFromFilePointer) {
                        filesToBeProcessed.add(file);
                    }
                }
            }
        } else {
            throw new FileNotFoundException(
                    String.format("Directory [%s] not found. Ensure it is a directory.", path));
        }
        return filesToBeProcessed;
    }

    private File getLogFile(String dirPath) throws Exception {
        File directory = new File(dirPath);
        File logFile;

        if (directory.isDirectory()) {
            FileFilter fileFilter = new WildcardFileFilter(log.getLogName());
            File[] files = directory.listFiles(fileFilter);
            if (files != null && files.length > 0) {
                logFile = getLatestFile(files);
                if (!logFile.canRead()) {
                    throw new IOException(String.format("Unable to read file [%s]", logFile.getPath()));
                }
            } else {
                LOGGER.info("Unable to find any file with name {} in {}. Skipping", log.getLogName(), dirPath);
                logFile = null;
            }
        } else {
            throw new FileNotFoundException(String.format("Directory [%s] not found. Ensure that it's a directory", dirPath));
        }
        return logFile;
    }

    private File getLatestFile(File[] files) {
        File latestFile = null;
        long lastModified = Long.MIN_VALUE;

        for (File file : files) {
            if (file.lastModified() > lastModified) {
                latestFile = file;
                lastModified = file.lastModified();
            }
        }
        return latestFile;
    }

    private boolean isLogRotated(long fileSize, long startPosition) {
        return fileSize < startPosition;
    }

    private boolean isFilenameChanged(String oldFilename, String newFilename) {
        return !oldFilename.equals(newFilename);
    }

    private boolean hasLogRolledOver(String dynamicLogPath, String actualLogPath, long fileSize) {
        FilePointer filePointer =
                filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);
        long currentPosition = filePointer.getLastReadPosition().get();
        if (isFilenameChanged(filePointer.getFilename(), actualLogPath) || isLogRotated(fileSize, currentPosition)) {
            LOGGER.debug("File: {} has either changed or rotated, resetting position to 0", filePointer.getFilename());
            return true;
        }
        return false;
    }

    private long getCurrentFilePointerOffset(String dynamicLogPath, String actualLogPath) {
        return filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath).getLastReadPosition().get();
    }

    private void handleFileEncoding(File file) throws Exception {
        LOGGER.debug("Converting current file: {} to UTF-8 encoding for further processing", file.getName());
        convertToUTF8Encoding(file, log.getEncoding());
    }

    private EventsServiceDataManager evaluateEventsServiceConfig() {
        if (this.monitorContextConfiguration.getConfigYml().get("sendDataToEventsService").equals(true)) {
            return monitorContextConfiguration.getContext().getEventsServiceDataManager();
        }
        return null;
    }
}