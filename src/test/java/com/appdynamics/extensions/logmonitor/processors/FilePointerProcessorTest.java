/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

/**
 * @author Aditya Jagtiani
 */

import static com.appdynamics.extensions.logmonitor.util.Constants.FILEPOINTER_FILENAME;
import static org.junit.Assert.assertEquals;

import java.io.File;

import com.appdynamics.extensions.logmonitor.config.FilePointer;
import org.junit.After;
import org.junit.Test;

public class FilePointerProcessorTest {

    @Test
    public void testUpdateFilePointerFileIsPersisted() {
        FilePointerProcessor filePointerProcessor = new FilePointerProcessor();
        String logPath = "src/test/resources/test-log-3.log";

        FilePointer origFilePointer = filePointerProcessor.getFilePointer(logPath, logPath);
        assertEquals(0, origFilePointer.getLastReadPosition().get());

        long newFilePointer = 1234;
        String newFilename = "src/test/resources/test-log-4.log";
        origFilePointer.setFilename(newFilename);
        origFilePointer.updateLastReadPosition(newFilePointer);
        filePointerProcessor.updateFilePointerFile();

        // re-initialise the filepointer
        // it should pick up from the file
        filePointerProcessor = new FilePointerProcessor();
        FilePointer result = filePointerProcessor.getFilePointer(logPath, logPath);
        assertEquals(newFilePointer, result.getLastReadPosition().get());
        assertEquals(newFilename, result.getFilename());
    }

    @After
    public void deleteFilePointerFile() {
        File filePointerFile = new File("./target/classes/com/appdynamics/extensions/logmonitor/" +
                FILEPOINTER_FILENAME);
        if (filePointerFile.exists()) {
            filePointerFile.delete();
        }
    }
}
