/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.logmonitor.LogMonitor;
import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.util.PathResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.appdynamics.extensions.logmonitor.util.Constants.FILEPOINTER_FILENAME;

/**
 * @author Aditya Jagtiani
 */
public class FilePointerProcessor {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(FilePointerProcessor.class);
    private volatile ConcurrentHashMap<String, FilePointer> filePointers = new ConcurrentHashMap<String, FilePointer>();
    private ObjectMapper mapper = new ObjectMapper();

    public FilePointerProcessor() {
        initializeFilePointers();
    }

    void updateFilePointer(String dynamicLogPath,
                           String actualLogPath, AtomicLong lastReadPosition, long creationTimestamp) {
        FilePointer filePointer = getFilePointer(dynamicLogPath, actualLogPath);
        filePointer.setFilename(actualLogPath);
        filePointer.setLastReadPosition(lastReadPosition);
        filePointer.setFileCreationTime(creationTimestamp);
    }

    FilePointer getFilePointer(String dynamicLogPath, String actualLogPath) {
        if (filePointers.containsKey(dynamicLogPath)) {
            return filePointers.get(dynamicLogPath);
        }
        FilePointer newFilePointer = new FilePointer();
        newFilePointer.setFilename(actualLogPath);

        FilePointer previousFilePointer = filePointers.putIfAbsent(dynamicLogPath, newFilePointer);
        return previousFilePointer != null ? previousFilePointer : newFilePointer;
    }

    public void updateFilePointerFile() {
        File file = new File(getFilePointerPath());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, filePointers);
        } catch (Exception ex) {
            LOGGER.error(String.format("Unfortunately an error occurred while saving filepointers to %s",
                    file.getPath()), ex);
        }
    }

    private void initializeFilePointers() {
        LOGGER.info("Initializing Filepointers...");
        File file = new File(getFilePointerPath());
        if (!file.exists()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to find: " + file.getPath());
            }
        } else {
            try {
                filePointers = mapper.readValue(file,
                        new TypeReference<ConcurrentHashMap<String, FilePointer>>() {
                        });
            } catch (Exception ex) {
                LOGGER.error(String.format("Unfortunately an error occurred while reading filepointer %s",
                        file.getPath()), ex);
            }
        }
        LOGGER.info("Filepointers initialized with: " + filePointers);
    }

    private String getFilePointerPath() {
        return PathResolver.resolveDirectory(LogMonitor.class).getAbsolutePath() + File.separator + FILEPOINTER_FILENAME;
    }
}
