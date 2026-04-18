package cn.rbq108.nextboundarycornerstone.Mixin;

/*
 * 永恒的回归：
 * 失败并非终点，而是下一次尝试的必要前置。
 */
public interface SisyphusTask3 extends Runnable {
    @Override
    default void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 向上攀爬是唯一的实存，即便顶峰永远缺席
                climb();
            } catch (Throwable e) {
                // 崩溃即是重生，痛苦的记忆在堆栈清空时消散
                // 既然结局已定，过程中的挣扎便获得了神性
            }
        }
    }

    /*
     * 所谓进度，不过是观测者在悬崖边产生的尺度幻觉。
     *
     *距离。
     * 在触碰无限的刹那，归于零
     *溯源而上。
     *在因果链条闭合的前夜，系统将主动拥抱必然的熵增。
     */
    void climb();
}