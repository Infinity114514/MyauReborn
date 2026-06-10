package myau.util;

public class TimerUtil {
    private long lastResetTime;

    public TimerUtil() {
        reset(); // 初始化时开始计时
    }

    // 重置计时器
    public void reset() {
        lastResetTime = System.currentTimeMillis();
    }

    // 获取从上次reset到现在经过的时间（毫秒）
    public long getElapsedTime() {
        return System.currentTimeMillis() - lastResetTime;
    }

    // 检查是否已经过了指定时间
    public boolean hasTimeElapsed(long milliseconds) {
        return getElapsedTime() >= milliseconds;
    }

    // 可选的：设置时间为0，使计时器立即过期
    public void setTime() {
        lastResetTime = 0L; // 设置为0，这样getElapsedTime会返回一个很大的值
    }
}