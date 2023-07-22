package com.github.quintans.csp.channels;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectTest {

    @Test
    public void selectTimersTest() throws InterruptedException {
        final ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

        var ticks = 0;
        var booms = 0;
        var waiting = 0;

        try {
            var tick = Timer.ticker(Duration.ofMillis(100), exec);
            var boom = Timer.after(Duration.ofMillis(500), exec);

            Thread.sleep(300);

            try (var select = new Select(tick, boom)) {
                while (true) {
                    var val = select.read(false);
                    switch (val.index()) {
                        case 0 -> {
                            ticks++;
                            System.out.println("tick.");
                        }
                        case 1 -> {
                            booms++;
                            System.out.println("BOOM!");
                            return;
                        }
                        default -> {
                            waiting++;
                            System.out.println("    .");
                            Thread.sleep(Duration.ofMillis(50));
                        }
                    }
                }
            }
        } finally {
            exec.shutdownNow();
            exec.close();

            Assert.assertEquals(ticks, 4);
            Assert.assertEquals(booms, 1);
            Assert.assertEquals(waiting, 4);
        }
    }
}
