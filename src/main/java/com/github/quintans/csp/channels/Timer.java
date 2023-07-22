package com.github.quintans.csp.channels;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;

public class Timer {
    public static ChannelRead<Instant> ticker(Duration d, ExecutorService exec) {
        var ch = new Channel<Instant>(1);

        exec.submit(() -> {
            var delta = d.toMillis();
            while (true) {
                if (delta > 0) {
                    Thread.sleep(delta);
                }
                var last = System.currentTimeMillis();
                ch.put(Instant.now()); // this can block because there is no consumer
                delta = d.toMillis() - (System.currentTimeMillis() - last);
            }
        });

        return ch;
    }

    public static ChannelRead<Instant> after(Duration d, ExecutorService exec) {
        var ch = new Channel<Instant>(1);

        exec.submit(() -> {
            Thread.sleep(d.toMillis());
            ch.put(Instant.now());
            return null;
        });

        return ch;
    }
}
