package com.github.quintans.csp.channels;

public record ChannelValue<T>(T value, boolean closed) {
}
