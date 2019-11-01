
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static java.lang.System.exit;

public class PlasticRunner {

    static Logger logger = LoggerFactory.getLogger(PlasticRunner.class);

    private final List<Properties> properties = new ArrayList<>();

    private final String[] propnames = {
            "in-schema-name",
            "in-schema-version",
            "in-schema-type",
            "out-schema-name",
            "out-schema-version",
            "out-schema-type",
            "payload-file",
            "defaults-file",
            "sleep-afterwards"
    };

    private final Set<String> optionals = new HashSet<>();

    private final CartographyService cartographer;

    public PlasticRunner(String[] args) throws IOException {
        validateArgs(args);

        optionals.add("defaults-file");
        optionals.add("sleep-afterwards");

        readProperties(args);

        Factory factory = new Factory();
        cartographer = factory.build();
    }

    private void validateArgs(String[] args) {
        switch(args.length) {
            case 1:
                break;
            case 0:
                error("Must supply a props file name as command line argument");
                exit(1);
        }
    }

    private void error(String msg) {
        logger.error(msg);
    }

    private void info(String msg) {
        logger.info(msg);
    }

    private void readProperties(String[] args) throws IOException {
        for (int i = 0; i< args.length; i++) {
            this.properties.add(readProperties(args[i]));
        }
    }

    private Properties readProperties(String propFileName) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(propFileName));

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            properties.setProperty(key, value.trim());
        }

        List<String> missings = new ArrayList<>();
        List<String> notFounds = new ArrayList<>();
        List<String> blanks = new ArrayList<>();

        for (String optional : optionals) {
            if (!properties.containsKey(optional)) {
                properties.put(optional, "");
            }
        }

        for (String required : propnames) {
            String value = properties.getProperty(required);
            if (!properties.containsKey(required)) {
                missings.add(required);
            }
            else if (value.isEmpty() && !optionals.contains(required)) {
                blanks.add(required);
            }
            else if(required.endsWith("-file")) {
                File f = new File(value);
                if (!f.exists() && !optionals.contains(required)) {
                    notFounds.add(required);
                }
            }
        }

        if (!missings.isEmpty()) {
            error("The following props were missing: "+ join(missings));
            exit(1);
        }

        if (!blanks.isEmpty()) {
            error("The following props had blank values: "+ join(blanks));
            exit(1);
        }

        if (!notFounds.isEmpty()) {
            error("The following support files were missing: "+ join(notFounds));
            exit(1);
        }
        return properties;
    }

    private String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(s);
        }
        return sb.toString();
    }

    private void translate(Properties props) {
        String inName = props.getProperty("in-schema-name").trim();
        String inVers = props.getProperty("in-schema-version").trim();
        String inType = props.getProperty("in-schema-type").trim();

        String outName = props.getProperty("out-schema-name").trim();
        String outVers = props.getProperty("out-schema-version").trim();
        String outType = props.getProperty("out-schema-type").trim();

        String payload = read(props.getProperty("payload-file").trim());

        String defaultsFile = props.getProperty("defaults-file").trim();
        String defaults = defaultsFile.isEmpty() ? "" : read(defaultsFile);

        info("*** Plastic Runner ***");
        info(String.format("\t*** Input: %s %s %s\n", inName, inVers, inType));
        info(String.format("\t*** Output: %s %s %s\n", outName, outVers, outType));
        info(String.format("\t*** Payload:\n%s\n", payload));

        String result;

        if (defaults.isEmpty()) {
            result = cartographer.translate(inName, inVers, inType, outName, outVers, outType, payload);
        } else {
            info(String.format("\t*** Defaults:\n%s\n", defaults));
            result = cartographer.translate(inName, inVers, inType, outName, outVers, outType, payload, defaults);
        }

        info(String.format("\t*** Result:\n%s\n", result));
    }

    private String read(String fileName) {

        try {
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }

                return sb.toString();
            }
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    // A hidden feature to help with debugging directory monitoring

    private boolean sleep(Properties props) {
        try {
            String sleepTime = props.getProperty("sleep-afterwards");
            if (sleepTime != null && !sleepTime.trim().isEmpty()) {
                logger.warn("Sleeping for {} seconds", sleepTime);
                int sleeper = Integer.valueOf(sleepTime);
                Thread.sleep(sleeper * 1000);
                return true;
            }
            return false;
        }
        catch(InterruptedException e) {
            return false;
        }
    }

    private void close() {
        try {
            cartographer.close();
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) throws IOException {

        PlasticRunner runner = new PlasticRunner(args);

        List<Thread> threads = new ArrayList<>();

        for (Properties properties : runner.properties) {
            threads.add(new Thread(() -> {
                while(true) {
                    runner.translate(properties);
                    if (!runner.sleep(properties))
                        break;
                }
            }));
        }

        threads.forEach((t) -> { t.start(); });
        threads.forEach((t) -> {
            try {
                t.join();
                runner.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
