package com.example.ti2mark1;

public class KeyGener {

    public static String GenerateKey(String plain, int lenM) {
        long seed = Long.parseLong(plain, 2);
        StringBuilder key = new StringBuilder(lenM);
        int n = 37;
        long mask = 1L << (n - 1);
        long seedMask = (1L << n) - 1;
        long tapMask = (1L << 12) | (1L << 10) | (1L << 2);

        for (int i = 0; i < lenM; i++) {
            long keyBit = (seed & mask) >>> (n - 1);
            key.append(keyBit);
            long feedbackBit = 0;
            long tappedBits = seed & tapMask;
            while (tappedBits != 0) {
                feedbackBit ^= (tappedBits & 1);
                tappedBits >>>= 1;
            }
            seed = (seed << 1) & seedMask;
            seed ^= feedbackBit;
        }

        return key.toString();
    }
}