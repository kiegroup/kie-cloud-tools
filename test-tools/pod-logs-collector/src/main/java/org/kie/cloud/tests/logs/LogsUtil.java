/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.cloud.tests.logs;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogsUtil.class);

    private static final String INSTANCES_LOGS_OUTPUT_DIRECTORY = "instance.logs";
    private static final String DEFAULT_LOG_OUTPUT_DIRECTORY = "instances";
    private static final String LOG_SUFFIX = ".log";

    public static void writeLogs(String name, String customLogFolderName, String podLogs) {
        File logFile = getOutputFile(name, customLogFolderName);
        try {
            FileUtils.write(logFile, podLogs, "UTF-8");
        } catch (Exception e) {
            LOGGER.error("Error writting instance logs", e);
        }
    }

    public static void appendPodLogLines(String name, String customLogFolderName, Collection<String> lines) {
        File logFile = getOutputFile(name, customLogFolderName);
        try {
            FileUtils.writeLines(logFile, "UTF-8", lines, true);
        } catch (Exception e) {
            LOGGER.error("Error writting instance logs", e);
        }
    }

    private static File getOutputFile(String name, String customLogFolderName) {
        File outputDirectory = new File(System.getProperty(INSTANCES_LOGS_OUTPUT_DIRECTORY, DEFAULT_LOG_OUTPUT_DIRECTORY), customLogFolderName);
        outputDirectory.mkdirs();
        return new File(outputDirectory, name + LOG_SUFFIX);
    }
}
