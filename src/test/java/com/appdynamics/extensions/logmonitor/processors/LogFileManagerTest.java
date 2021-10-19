/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.logmonitor.config.SearchString;
import com.appdynamics.extensions.logmonitor.metrics.LogMetrics;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.MetricPathUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.util.Constants.SCHEMA_NAME;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Aditya Jagtiani
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(MetricPathUtils.class)
@PowerMockIgnore({ "javax.net.ssl.*" })
public class LogFileManagerTest {
    private LogFileManager classUnderTest;
    private FilePointerProcessor mockFilePointerProcessor = Mockito.mock(FilePointerProcessor.class);
    private MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Log Monitor",
            "Custom Metrics|Log Monitor|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));


    @Test
    public void testProcessorWhenPrintMatchedStringIsFalse() throws Exception {
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-1.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Log Monitor",
                "Custom Metrics|Log Monitor|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = logMetrics.getMetrics();

        assertEquals("13", metrics.get("TestLog|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("24", metrics.get("TestLog|Search String|Info|Occurrences").getMetricValue());
        assertEquals("7", metrics.get("TestLog|Search String|Error|Occurrences").getMetricValue());
        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                metrics.get("TestLog|File size (Bytes)").getMetricValue());
    }

    @Test
    public void testProcessorWhenPrintMatchedStringIsTrue() throws Exception {
        PowerMockito.mockStatic(MetricPathUtils.class);
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-1.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(true);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(true);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(true);

        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");
        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);
        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = logMetrics.getMetrics();

        assertEquals("13", metrics.get("TestLog|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("24", metrics.get("TestLog|Search String|Info|Occurrences").getMetricValue());
        assertEquals("7", metrics.get("TestLog|Search String|Error|Occurrences").getMetricValue());

        assertEquals("13", metrics.get("TestLog|Search String|Debug|Matches|Debug").getMetricValue());
        assertEquals("24", metrics.get("TestLog|Search String|Info|Matches|Info").getMetricValue());
        assertEquals("7", metrics.get("TestLog|Search String|Error|Matches|Error").getMetricValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                metrics.get("TestLog|File size (Bytes)").getMetricValue());
    }

    @Test
    public void testProcessorWhenPrintMatchedStringIsFalseUTF16() throws Exception {
        Log log = new Log();
        log.setDisplayName("TestUTF16Log");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-utf16.log");
        log.setEncoding("UTF-16LE");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Log Monitor",
                "Custom Metrics|Log Monitor|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = logMetrics.getMetrics();
        assertEquals("5", metrics.get("TestUTF16Log|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("5", metrics.get("TestUTF16Log|Search String|Info|Occurrences").getMetricValue());
        assertEquals("0", metrics.get("TestUTF16Log|Search String|Error|Occurrences").getMetricValue());

        revertToUTF16Encoding(new File(log.getLogDirectory() + log.getLogName()));
    }

    @Test
    public void testProcessorForRegexPatternMatch() throws Exception {
        PowerMockito.mockStatic(MetricPathUtils.class);
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-regex.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(false);
        searchString.setPattern("<");
        searchString.setDisplayName("Pattern <");
        searchString.setPrintMatchedString(true);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(false);
        searchString1.setPattern(">");
        searchString1.setDisplayName("Pattern >");
        searchString1.setPrintMatchedString(true);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(false);
        searchString2.setPattern("\\*");
        searchString2.setDisplayName("Pattern *");
        searchString2.setPrintMatchedString(true);

        SearchString searchString3 = new SearchString();
        searchString3.setCaseSensitive(false);
        searchString3.setMatchExactString(false);
        searchString3.setPattern("\\[");
        searchString3.setDisplayName("Pattern [");
        searchString3.setPrintMatchedString(true);

        SearchString searchString4 = new SearchString();
        searchString4.setCaseSensitive(false);
        searchString4.setMatchExactString(false);
        searchString4.setPattern("\\]");
        searchString4.setDisplayName("Pattern ]");
        searchString4.setPrintMatchedString(true);

        SearchString searchString5 = new SearchString();
        searchString5.setCaseSensitive(false);
        searchString5.setMatchExactString(false);
        searchString5.setPattern("\\.");
        searchString5.setDisplayName("Pattern .");
        searchString5.setPrintMatchedString(true);

        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");
        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2, searchString3, searchString4, searchString5));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = logMetrics.getMetrics();

        assertEquals("5", metrics.get("TestLog|Search String|Pattern <|Matches|<").getMetricValue());
        assertEquals("6", metrics.get("TestLog|Search String|Pattern >|Matches|>").getMetricValue());
        assertEquals("16", metrics.get("TestLog|Search String|Pattern *|Matches|*").getMetricValue());
        assertEquals("23", metrics.get("TestLog|Search String|Pattern [|Matches|[").getMetricValue());
        assertEquals("23", metrics.get("TestLog|Search String|Pattern ]|Matches|]").getMetricValue());
        assertEquals("2", metrics.get("TestLog|Search String|Pattern .|Matches|.").getMetricValue());

        assertEquals("5", metrics.get("TestLog|Search String|Pattern <|Occurrences").getMetricValue());
        assertEquals("6", metrics.get("TestLog|Search String|Pattern >|Occurrences").getMetricValue());
        assertEquals("16", metrics.get("TestLog|Search String|Pattern *|Occurrences").getMetricValue());
        assertEquals("23", metrics.get("TestLog|Search String|Pattern [|Occurrences").getMetricValue());
        assertEquals("23", metrics.get("TestLog|Search String|Pattern ]|Occurrences").getMetricValue());
        assertEquals("2", metrics.get("TestLog|Search String|Pattern .|Occurrences").getMetricValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                metrics.get("TestLog|File size (Bytes)").getMetricValue());
    }

    @Test
    public void testProcessorForRegexWordMatch() throws Exception {
        PowerMockito.mockStatic(MetricPathUtils.class);
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-regex.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(false);
        searchString.setPattern("(\\s|^)m\\w+(\\s|$)");
        searchString.setDisplayName("Pattern start with M");
        searchString.setPrintMatchedString(true);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(false);
        searchString1.setPattern("<\\w*>");
        searchString1.setDisplayName("Pattern start with <");
        searchString1.setPrintMatchedString(true);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(false);
        searchString2.setPattern("\\[JMX.*\\]");
        searchString2.setDisplayName("Pattern start with [JMX");
        searchString2.setPrintMatchedString(true);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = logMetrics.getMetrics();

        // matches (\\s|^)m\\w+(\\s|$)
        assertEquals("7", metrics.get("TestLog|Search String|Pattern start with M|Matches|Memorymetricgenerator").getMetricValue());
        assertEquals("2", metrics.get("TestLog|Search String|Pattern start with M|Matches|Memory").getMetricValue());
        assertEquals("2", metrics.get("TestLog|Search String|Pattern start with M|Matches|Major").getMetricValue());
        assertEquals("1", metrics.get("TestLog|Search String|Pattern start with M|Matches|Mx").getMetricValue());
        assertEquals("1", metrics.get("TestLog|Search String|Pattern start with M|Matches|Metric").getMetricValue());
        assertEquals("2", metrics.get("TestLog|Search String|Pattern start with M|Matches|Minor").getMetricValue());
        assertEquals("3", metrics.get("TestLog|Search String|Pattern start with M|Matches|Metrics").getMetricValue());
        assertEquals("1", metrics.get("TestLog|Search String|Pattern start with M|Matches|Mbean").getMetricValue());

        // matches <\\w*>
        assertEquals("2", metrics.get("TestLog|Search String|Pattern start with <|Matches|<this>").getMetricValue());
        assertEquals("3", metrics.get("TestLog|Search String|Pattern start with <|Matches|<again>").getMetricValue());

        // matches \\[JMX.*\\]
        assertEquals("1", metrics.get("TestLog|Search String|Pattern start with [JMX|Matches|[jmxservice]").getMetricValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                metrics.get("TestLog|File size (Bytes)").getMetricValue());
    }
    

    
    @Test
    public void testLogFileAfterAdditionOfMoreLogs() throws Exception {
        PowerMockito.mockStatic(MetricPathUtils.class);
        String originalFilePath = this.getClass().getClassLoader().getResource("test-log-1.log").getPath();

        String testFilename = "active-test-log.log";
        String testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(originalFilePath, testFilepath);

        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory(getTargetDir().getPath());
        log.setLogName(testFilename);

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(true);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(true);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(true);

        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");
        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + File.separator + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics result = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = result.getMetrics();

        assertEquals("13", metrics.get("TestLog|Search String|Debug|Matches|Debug").getMetricValue());
        assertEquals("24", metrics.get("TestLog|Search String|Info|Matches|Info").getMetricValue());
        assertEquals("7", metrics.get("TestLog|Search String|Error|Matches|Error").getMetricValue());

        assertEquals("13", metrics.get("TestLog|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("24", metrics.get("TestLog|Search String|Info|Occurrences").getMetricValue());
        assertEquals("7", metrics.get("TestLog|Search String|Error|Occurrences").getMetricValue());

        String filesize = getFileSize(log.getLogDirectory(), log.getLogName());
        assertEquals(filesize, metrics.get("TestLog|File size (Bytes)").getMetricValue());

        FilePointer filePointerAfterCurrentRun = LogMonitorUtil.getLatestFilePointer(result.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1)).updateFilePointer(filePointerAfterCurrentRun.getFilename(),
                filePointerAfterCurrentRun.getFilename(), filePointerAfterCurrentRun.getLastReadPosition(), filePointerAfterCurrentRun.getFileCreationTime());

        // simulate our filepointer was updated
        filePointer.updateLastReadPosition(new Long(filesize));
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString()))
                .thenReturn(filePointer);

        List<String> logsToAdd = Arrays.asList("",
                new Date() + "	DEBUG	This is the first line",
                new Date() + "	INFO	This is the second line",
                new Date() + "	INFO	This is the third line",
                new Date() + "	DEBUG	This is the fourth line",
                new Date() + "	DEBUG	This is the fifth line");
        updateLogFile(testFilepath, logsToAdd);

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        result = classUnderTest.processLogMetrics();
        metrics = result.getMetrics();

        assertEquals("3", metrics.get("TestLog|Search String|Debug|Matches|Debug").getMetricValue());
        assertEquals("2", metrics.get("TestLog|Search String|Info|Matches|Info").getMetricValue());
        assertEquals("2", metrics.get("TestLog|Search String|Info|Occurrences").getMetricValue());
        assertEquals("3", metrics.get("TestLog|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("0", metrics.get("TestLog|Search String|Error|Occurrences").getMetricValue());

        filePointerAfterCurrentRun = LogMonitorUtil.getLatestFilePointer(result.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1)).updateFilePointer(filePointerAfterCurrentRun.getFilename(),
                filePointerAfterCurrentRun.getFilename(), filePointerAfterCurrentRun.getLastReadPosition(), filePointerAfterCurrentRun.getFileCreationTime());
    }

    @Test
    public void testLogFileProcessingAfterRollover() throws Exception {
        String dynamicLog1 = "src/test/resources/dynamic-log-1.log";
        String testFilename = "active-dynamic-log-1.log";
        String testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog1, testFilepath);

        Log log = new Log();
        log.setLogDirectory(getTargetDir().getPath());
        log.setLogName("active-dynamic-*");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("error");
        searchString1.setDisplayName("Error");
        searchString1.setPrintMatchedString(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + File.separator + testFilename);
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = logMetrics.getMetrics();

        assertEquals("3", metrics.get("active-dynamic-*|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("0", metrics.get("active-dynamic-*|Search String|Error|Occurrences").getMetricValue());

        String filesize = getFileSize(log.getLogDirectory(), testFilename);
        assertEquals(filesize, metrics.get("active-dynamic-*|File size (Bytes)").getMetricValue());

        // simulate a file pointer update
        filePointer.updateLastReadPosition(new Long(filesize));
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        List<String> logsToAdd = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            logsToAdd.add(new Date() + "	DEBUG	Statement " + i + "\n");
        }
        updateLogFile(testFilepath, logsToAdd);

        // simulate new file created with different name
        Thread.sleep(1000);
        String dynamicLog2 = this.getClass().getClassLoader().getResource("dynamic-log-2.log").getPath();

        testFilename = "active-dynamic-log-2.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog2, testFilepath);

        // simulate another file created with different name
        Thread.sleep(1000);
        String dynamicLog3 = this.getClass().getClassLoader().getResource("dynamic-log-3.log").getPath();
        testFilename = "active-dynamic-log-3.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog3, testFilepath);
        logsToAdd.clear();

        logMetrics = classUnderTest.processLogMetrics();
        metrics = logMetrics.getMetrics();

        assertNotSame(3, metrics.get("active-dynamic-*|Search String|Debug|Occurrences"));
    }

    @Test
    public void testFilePointerHasLatestTimeStampAfterRollover() throws Exception {
        String dynamicLog1 = "src/test/resources/dynamic-log-1.log";

        String testFilename = "active-dynamic-log-1.log";
        String testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog1, testFilepath);

        Log log = new Log();
        log.setLogDirectory(getTargetDir().getPath());
        log.setLogName("active-dynamic-*");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("error");
        searchString1.setDisplayName("Error");
        searchString1.setPrintMatchedString(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + File.separator + testFilename);
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();

        String filesize = getFileSize(log.getLogDirectory(), testFilename);
        FilePointer latestFilePointer = LogMonitorUtil.getLatestFilePointer(logMetrics.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1))
                .updateFilePointer("./target/active-dynamic-*",
                        latestFilePointer.getFilename(), latestFilePointer.getLastReadPosition(), latestFilePointer.getFileCreationTime());

        // simulate our filepointer was updated
        filePointer.updateLastReadPosition(new Long(filesize));
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString()))
                .thenReturn(filePointer);

        List<String> logsToAdd = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            logsToAdd.add(new Date() + "	DEBUG	Statement " + i + "\n");
        }
        updateLogFile(testFilepath, logsToAdd);

        // simulate new file created with different name
        Thread.sleep(1000);
        String dynamicLog2 = this.getClass().getClassLoader().getResource("dynamic-log-2.log").getPath();
        testFilename = "active-dynamic-log-2.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog2, testFilepath);

        // simulate another file created with different name
        Thread.sleep(1000);
        String dynamicLog3 = this.getClass().getClassLoader().getResource("dynamic-log-3.log").getPath();
        testFilename = "active-dynamic-log-3.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog3, testFilepath);
        logsToAdd.clear();
        for (int i = 0; i < 100; i++) {
            logsToAdd.add(new Date() + "	ERROR	Statement " + i + "\n");
        }

        updateLogFile(testFilepath, logsToAdd);
        logMetrics = classUnderTest.processLogMetrics();
        latestFilePointer = LogMonitorUtil.getLatestFilePointer(logMetrics.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1)).updateFilePointer("./target/active-dynamic-*",
                latestFilePointer.getFilename(), latestFilePointer.getLastReadPosition(), latestFilePointer.getFileCreationTime());
    }

    @Test
    public void testProcessorWhenEventsServiceIsEnabled() throws Exception {
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-1.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);

        log.setSearchStrings(Lists.newArrayList(searchString));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Log Monitor",
                "Custom Metrics|Log Monitor|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config-eventsService.yaml");

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);
        LogMetrics logMetrics = classUnderTest.processLogMetrics();
        Map<String, Metric> metrics = logMetrics.getMetrics();

        assertEquals("13", metrics.get("TestLog|Search String|Debug|Occurrences").getMetricValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                metrics.get("TestLog|File size (Bytes)").getMetricValue());
    }

    @Test
    public void testLogEventGeneratorWithOffset() throws Exception {
        EventsServiceDataManager eventsServiceDataManager = Mockito.mock(EventsServiceDataManager.class);
        Mockito.when(eventsServiceDataManager.retrieveSchema(SCHEMA_NAME)).thenReturn("Hello world");
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-4-events-service.log");

        SearchPattern searchPattern = new SearchPattern("Test Patterns", Pattern.compile("1"), false, false );

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());

        OptimizedRandomAccessFile randomAccessFile = new OptimizedRandomAccessFile(new File("src/test/resources/test-log-4-events-service.log"), "r");

        int offset = 5;

        LogEventsProcessor classUnderTest = new LogEventsProcessor(eventsServiceDataManager, offset, log);
        LogEvent logEvent = classUnderTest.processLogEvent(searchPattern, randomAccessFile, "");

        Assert.assertEquals("1\n1\n2\n3\n4\n", logEvent.getLogMatch());
    }
    

    
    private String getFileSize(String logDir, String logName) throws Exception {
        String fullPath = String.format("%s%s%s", logDir, File.separator, logName);
        RandomAccessFile file = new RandomAccessFile(fullPath, "r");
        long fileSize = file.length();
        file.close();
        return String.valueOf(fileSize);
    }

    private void copyFile(String sourceFilePath, String destFilePath) throws Exception {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;

        try {
            sourceChannel = new FileInputStream(new File(sourceFilePath)).getChannel();
            destChannel = new FileOutputStream(new File(destFilePath)).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());

        } finally {
            sourceChannel.close();
            destChannel.close();
        }
    }

    private void updateLogFile(String filepath, List<String> stringList) throws Exception {
        File file = new File(filepath);
        FileWriter fileWriter = new FileWriter(file, true);

        try {
            String output = StringUtils.join(stringList, System.getProperty("line.separator"));
            fileWriter.write(output);

        } finally {
            fileWriter.close();
        }
    }

    private File getTargetDir() {
        return new File("./target");
    }

    private void revertToUTF16Encoding(File file) throws Exception {
        String charset = "UTF-8";
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        OutputStream outputStream = new FileOutputStream(file, false);
        Writer outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-16LE");
        outputStreamWriter.write(sb.toString());
        outputStreamWriter.close();
    }
}