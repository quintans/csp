package com.github.quintans.csp.channels;

public class Select implements AutoCloseable {
    public static final int NONE = -1;

    final Signal signal = new Signal();
    final ChannelRead<?>[] channelReads;

    int chanPtr;

    public Select(ChannelRead<?>... channelReads) {
        for (var c : channelReads) {
            c.addMonitor(signal);
        }
        this.channelReads = channelReads;
    }

    public SelectValue read(boolean wait) throws InterruptedException {
        var last = chanPtr;
        while (true) {
            var value = channelReads[chanPtr].pool();

            var prev = chanPtr;
            if (++chanPtr == channelReads.length)
                chanPtr = 0;

            if (value.closed())
                return new SelectValue(prev, null, true);

            if (value.value() != null) {
                return new SelectValue(prev, value.value(), false);
            }

            // a complete loop was done and there is no value to be returned
            if (chanPtr == last) {
                if (!wait)
                    return new SelectValue(NONE, null, false);

                signal.await();
            }
        }
    }

    @Override
    public void close() {
        for (var c : channelReads) {
            c.removeMonitor(signal);
        }
    }
}
