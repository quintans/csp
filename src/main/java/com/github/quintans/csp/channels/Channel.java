package com.github.quintans.csp.channels;

public class Channel<T> extends ChannelAbstract<T> {

    public Channel(int capacity) {
       super(capacity);
    }

    public void put(T x) throws InterruptedException, ClosedChannelException {
        if (x == null)
            throw new IllegalArgumentException("Cannot put null values");

        add(x);
    }
}
