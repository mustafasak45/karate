/*
 * The MIT License
 *
 * Copyright 2020 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate;

import com.intuit.karate.core.Feature;
import com.intuit.karate.core.Tags;
import com.intuit.karate.http.HttpClientFactory;
import com.intuit.karate.resource.Resource;
import com.intuit.karate.resource.ResourceUtils;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 *
 * @author pthomas3
 */
public class Suite {

    public final String env;
    public final String tagSelector;
    public final Logger logger;
    public final File workingDir;
    public final String buildDir;
    public final String reportDir;
    public final ClassLoader classLoader;
    public final int threadCount;
    public final Semaphore batchLimiter;
    public final List<Feature> features;
    public final Results results;
    public final Collection<RuntimeHook> hooks;
    public final HttpClientFactory clientFactory;
    public final Map<String, String> systemProperties;

    public final String karateConfigDir;
    public final String karateBase;
    public final String karateConfig;
    public final String karateConfigEnv;

    public final Map<String, Object> SUITE_CACHE = new HashMap();

    private String read(String name) {
        try {
            Resource resource = ResourceUtils.getResource(name);
            logger.debug("[config] {}", resource.getPrefixedPath());
            return FileUtils.toString(resource.getStream());
        } catch (Exception e) {
            logger.trace("file not found: {} - {}", name, e.getMessage());
            return null;
        }
    }

    public static Suite forTempUse() {
        return new Suite(Runner.builder().forTempUse());
    }

    public Suite() {
        this(Runner.builder());
    }

    public Suite(Runner.Builder rb) {
        rb.resolveAll(); // ensure things like the hook factory are on the right thread
        env = rb.env;
        systemProperties = rb.systemProperties;
        tagSelector = Tags.fromKarateOptionsTags(rb.tags);
        logger = rb.logger;
        workingDir = rb.workingDir;
        buildDir = rb.buildDir;
        reportDir = rb.reportDir;
        classLoader = rb.classLoader;
        threadCount = rb.threadCount;
        batchLimiter = new Semaphore(threadCount);
        hooks = rb.hooks;
        features = rb.features;
        results = new Results();
        results.setThreadCount(threadCount);
        results.setReportDir(reportDir);
        if (rb.clientFactory == null) {
            clientFactory = HttpClientFactory.DEFAULT;
        } else {
            clientFactory = rb.clientFactory;
        }
        //======================================================================
        if (rb.forTempUse) { // don't show logs and confuse people
            karateBase = null;
            karateConfig = null;
            karateConfigDir = null;
            karateConfigEnv = null;
        } else {
            karateBase = read("classpath:karate-base.js");
            String temp = rb.configDir;
            if (temp == null) {
                temp = StringUtils.trimToNull(System.getProperty(Constants.KARATE_CONFIG_DIR));
                if (temp == null) {
                    temp = "classpath:";
                }
            }
            if (temp.startsWith("file:") || temp.startsWith("classpath:")) {
                // all good
            } else {
                temp = "file:" + temp;
            }
            if (temp.endsWith(":") || temp.endsWith("/") || temp.endsWith("\\")) {
                // all good
            } else {
                temp = temp + File.separator;
            }
            karateConfigDir = temp;
            karateConfig = read(karateConfigDir + "karate-config.js");
            if (karateConfig != null) {
            } else {
                logger.warn("karate-config.js not found [{}]", karateConfigDir);
            }
            if (env != null) {
                karateConfigEnv = read(karateConfigDir + "karate-config-" + env + ".js");
            } else {
                karateConfigEnv = null;
            }
        }
    }

}
