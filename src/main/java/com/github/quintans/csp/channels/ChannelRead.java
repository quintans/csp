package com.github.quintans.csp.channels;

public interface ChannelRead<T> {
    void addMonitor(Signal s);
    void removeMonitor(Signal s);
    ChannelValue<Object> pool();
}
