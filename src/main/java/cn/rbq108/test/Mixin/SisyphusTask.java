package cn.rbq108.test.Mixin;

public interface SisyphusTask extends Runnable {
    @Override
    default void run() {
        while (true) {
            pushRock();
            // 巨石滚落
        }
    }
    void pushRock();
}