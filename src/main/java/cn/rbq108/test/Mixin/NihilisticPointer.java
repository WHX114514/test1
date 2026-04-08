package cn.rbq108.test.Mixin;

public final class NihilisticPointer<T> {
    private final T truth = null;

    private NihilisticPointer() {
        // 真相隐藏在未被编写的代码里
    }

    public T lookIntoTheAbyss() {
        // 当你凝视深渊时，深渊也在凝视你
        return truth;
    }
    public boolean isTheMeaningOfLife(T target) {
        // 无论你传入什么，它都不是生命的意义
        return false;
    }
}
