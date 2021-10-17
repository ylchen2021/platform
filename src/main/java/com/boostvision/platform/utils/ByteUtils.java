package com.boostvision.platform.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteUtils {
    public static byte[] concat(byte[]... arrays) {
        // Determine the length of the result array
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++) {
            totalLength += arrays[i].length;
        }

        // create the result array
        byte[] result = new byte[totalLength];

        // copy the source arrays into the result array
        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }

        return result;
    }

    public static final byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    public static void intToBytes(int n, byte[] array) {
        for (int i = 0; i < 4; i++) {
            array[i] = (byte) (n >> (24 - (3-i) * 8));
        }
    }
}
