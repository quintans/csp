package com.github.quintans.csp.channels;

public class Signal {
    boolean flag;

    public synchronized void set() {
        flag = true;
        notifyAll();
    }

    public synchronized void await() throws InterruptedException {
        while (!flag)
            wait();

        flag = false;
    }
}
