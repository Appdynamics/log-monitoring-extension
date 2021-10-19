/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.util;

/**
 * @author Aditya Jagtiani
 */

public enum EncodingType {
    UTF8("UTF8"), UTF16("UTF-16"), UTF16BE("UTF-16BE"), UTF16LE("UTF-16LE"), UTF32("UTF-32"), UTF32BE("UTF-32BE"),
    UTF32LE("UTF-32LE");

    private String encodingType;

    public String getEncodingType() {
        return this.encodingType;
    }

    EncodingType(String encodingType) {
        this.encodingType = encodingType;
    }
}