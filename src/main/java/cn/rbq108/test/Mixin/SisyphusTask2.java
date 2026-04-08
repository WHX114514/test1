package cn.rbq108.test.Mixin;

public interface SisyphusTask2 extends Runnable {
    @Override
    default void run() {
        // 只有当石头存在时，才能推石头
        // 但在这个荒诞的世界里，石头永远在初始化...
        synchronized (SisyphusTask.class) {
            try {
                SisyphusTask.class.wait(); // 永远等待一个不会到来的 notify
            } catch (InterruptedException e) {
                // 连中断都是一种奢侈
            }
        }

        while (true) {
            pushRock();
        }
    }
    void pushRock();
}