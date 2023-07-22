package com.github.quintans.csp.channels;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ChannelAbstract<T> implements ChannelRead<T> {
    private enum Status {
        OPEN, CLOSING, CLOSED;
    }

    final Lock lock = new ReentrantLock();
    final Condition notFull = lock.newCondition();
    final Condition notEmpty = lock.newCondition();

    final ChannelValue<?>[] items;
    int putPtr, takePtr, count;

    final WeakHashMap<Signal, Object> monitors = new WeakHashMap<>();

    Status status = Status.OPEN;

    public ChannelAbstract(int capacity) {
        items = new ChannelValue[capacity];
    }

    public void addMonitor(Signal s) {
        lock.lock();
        try {
            monitors.put(s, null);
        } finally {
            lock.unlock();
        }
    }

    public void removeMonitor(Signal s) {
        lock.lock();
        try {
            monitors.remove(s);
        } finally {
            lock.unlock();
        }
    }

    private void signalMonitors() {
        monitors.forEach((signal, o) -> signal.set());
    }

    protected void add(T x) throws InterruptedException, ClosedChannelException {
        lock.lockInterruptibly();
        try {
            while (true) {
                if (status != Status.OPEN)
                    throw new ClosedChannelException();

                if (count != items.length)
                    break;

                notFull.await();
            }

            items[putPtr] = new ChannelValue<>(x, false);
            if (++putPtr == items.length)
                putPtr = 0;
            ++count;
            notEmpty.signal();
            signalMonitors();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public ChannelValue<T> take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (true) {
                if (status == Status.CLOSED)
                    return new ChannelValue<>(null, true);

                if (count != 0)
                    break;

                notEmpty.await();
            }

            var x = items[takePtr];
            if (x.closed()) {
                status = Status.CLOSED;
                return new ChannelValue<>(null, true);
            }

            if (++takePtr == items.length)
                takePtr = 0;
            --count;
            notFull.signal();
            return new ChannelValue<>((T) x, false);
        } finally {
            lock.unlock();
        }
    }

    public ChannelValue<Object> pool() {
        lock.lock();
        try {
            if (status == Status.CLOSED)
                return new ChannelValue<>(null, true);

            if (count == 0)
                return new ChannelValue<>(null, false);

            var x = items[takePtr];
            if (x.closed()) {
                status = Status.CLOSED;
                return new ChannelValue<>(null, true);
            }
            if (++takePtr == items.length)
                takePtr = 0;
            --count;

            notFull.signal();
            return new ChannelValue<>(x, false);
        } finally {
            lock.unlock();
        }
    }

    public void close() throws InterruptedException {
        lock.lock();
        try {
            if (status != Status.OPEN)
                return;

            status = Status.CLOSING;
            // poison pill
            try {
                add(null);
            } catch (ClosedChannelException e) {
                return;
            }
            notFull.signal();
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
}
