/*
 *  Copyright 2019. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

/**
 * @author Aditya Jagtiani
 */

public class LogEvent {
    private String logDisplayName;

    public String getLogDisplayName() {
        return logDisplayName;
    }

    public void setLogDisplayName(String logDisplayName) {
        this.logDisplayName = logDisplayName;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    public String getSearchPatternDisplayName() {
        return searchPatternDisplayName;
    }

    public void setSearchPatternDisplayName(String searchPatternDisplayName) {
        this.searchPatternDisplayName = searchPatternDisplayName;
    }

    public String getLogMatch() {
        return logMatch;
    }

    public void setLogMatch(String logMatch) {
        this.logMatch = logMatch;
    }

    private String searchPattern;
    private String searchPatternDisplayName;
    private String logMatch;

}
