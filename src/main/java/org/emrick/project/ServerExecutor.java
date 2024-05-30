package org.emrick.project;

import java.util.concurrent.Executor;

public class ServerExecutor implements Executor {
    @Override
    public void execute(Runnable command) {
        new Thread(command).start();
    }
}
