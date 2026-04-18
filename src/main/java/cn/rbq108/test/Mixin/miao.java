package cn.rbq108.test.Mixin;

public class miao {
    public static void main(String[] args) {
        // 这里什么也没发生， 但宇宙的熵增了一喵喵～
        char[] miaowu = "miaowuM".toCharArray();
        int entropy = 0;
        for(int i = 0; i < miaowu.length; i++) {
            if(i%2==0){
                entropy += miaowu[i];
            }else{
                entropy -= miaowu[i];
            }
        }
        System.out.println(entropy);
    }
}