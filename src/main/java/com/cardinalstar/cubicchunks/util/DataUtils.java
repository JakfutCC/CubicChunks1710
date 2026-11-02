package com.cardinalstar.cubicchunks.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class DataUtils {

    public static <S, T> List<T> mapToList(Collection<S> in, Function<S, T> mapper) {
        if (in == null) return null;

        List<T> out = new ArrayList<>(in.size());

        for (S s : in) {
            out.add(mapper.apply(s));
        }

        return out;
    }

    public static short[] byteToShortArray(byte[] byteArray) {
        short[] shortArray = new short[(byteArray.length + 1) / 2];
        byteToShortArray(byteArray, 0, shortArray, 0, byteArray.length);
        return shortArray;
    }

    public static byte[] shortToByteArray(short[] shortArray) {
        byte[] byteArray = new byte[shortArray.length * 2];
        shortToByteArray(shortArray, 0, byteArray, 0, shortArray.length);
        return byteArray;
    }

    public static void shortToByteArray(short[] shortArray, int shortOffset, byte[] byteArray, int byteOffset,
        int shortCount) {
        for (int i = 0; i < shortCount; i++) {
            short s = shortArray[shortOffset + i];
            byteArray[byteOffset + i * 2] = (byte) (s & 255);
            byteArray[byteOffset + i * 2 + 1] = (byte) ((s >>> 8) & 255);
        }
    }

    public static void byteToShortArray(byte[] byteArray, int byteOffset, short[] shortArray, int shortOffset,
        int byteCount) {
        for (int i = 0; i < byteCount; i++) {
            byte b = byteArray[byteOffset + i];
            shortArray[shortOffset
                + i / 2] = (short) (i % 2 == 0 ? (b & 255) : (shortArray[shortOffset + i / 2] | ((b & 255) << 8)));
        }
    }
}
