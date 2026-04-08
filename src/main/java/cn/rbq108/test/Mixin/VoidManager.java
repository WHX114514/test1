package cn.rbq108.test.Mixin;

public class VoidManager {
    private static class Holder {
        private static final VoidManager INSTANCE = new VoidManager();
    }

    private VoidManager() {
        // 一二七七 二二七
    }

    public static VoidManager getInstance() {
        return Holder.INSTANCE;
    }
}