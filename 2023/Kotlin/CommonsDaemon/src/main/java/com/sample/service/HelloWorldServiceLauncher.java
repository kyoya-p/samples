package com.sample.service;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HelloWorldServiceLauncher {
    private static Log LOG = LogFactory.getLog(HelloWorldServiceLauncher.class);
    private static IService service = null;
    private static HelloWorldServiceLauncher instance = new HelloWorldServiceLauncher();
    private ExecutorService executor = null;
    private static Scanner scanner;

    public static void main(String[] args) {
        if (args != null) {
            LOG.debug("Param : " + Arrays.toString(args));
        }

        HelloWorldServiceLauncher.start(null);

        scanner = new Scanner(System.in);
        LOG.debug("Enter 'stop' to halt: ");
        while (!scanner.nextLine().toLowerCase().equals("stop")) {
            ;
        }

        HelloWorldServiceLauncher.stop(null);
    }

    public static void start(String[] args) {
        if (args != null) {
            LOG.debug("Param : " + Arrays.toString(args));
        }
        instance.initialize();
    }

    public static void stop(String[] args) {
        if (args != null) {
            LOG.debug("Param : " + Arrays.toString(args));
        }
        instance.terminate();
    }

    public void initialize() {
        if (HelloWorldServiceLauncher.service == null) {
            HelloWorldServiceLauncher.service = new HelloWorldService();
        }

        this.executor = Executors.newSingleThreadExecutor();
        this.executor.execute(HelloWorldServiceLauncher.service);
    }

    public void terminate() {
        if (HelloWorldServiceLauncher.service != null) {
            HelloWorldServiceLauncher.service.stop();
        }
        if (this.executor != null) {
            this.executor.shutdown();
        }
    }
}
