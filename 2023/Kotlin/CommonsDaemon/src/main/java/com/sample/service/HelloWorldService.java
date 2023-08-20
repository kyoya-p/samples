package com.sample.service;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HelloWorldService implements IService {
    private static Log LOG = LogFactory.getLog(HelloWorldService.class);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private Boolean isStopped = Boolean.FALSE;

    @Override
    public void run() {
        while (!this.isStopped()) {
            LOG.debug("[ENTER]" + dateFormat.format(new Date()));
            try {

                try (FileWriter fileWriter = new FileWriter("C:\\temp\\hello-world.txt", true);) {
                    fileWriter.write(dateFormat.format(new Date()) + " : Hello World!\n");
                } catch (IOException ex) {
                    LOG.error("IO Error", ex);
                }
                Thread.sleep(5_000L);

            } catch (InterruptedException ex) {
                LOG.error("Error", ex);
                this.isStopped = Boolean.TRUE;
            }
            LOG.debug("[EXIT]" + dateFormat.format(new Date()));
        }
    }

    @Override
    public void stop() {
        this.isStopped = Boolean.TRUE;
    }

    @Override
    public Boolean isStopped() {
        return this.isStopped;
    }
}
