package myau.util;

public class MSTimer {

    private long time = System.currentTimeMillis();

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public boolean hasTimePassed(int delay) {
        return hasTimePassed((long) delay);
    }

    public boolean hasTimePassed(long delay) {
        return System.currentTimeMillis() >= time + delay;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

