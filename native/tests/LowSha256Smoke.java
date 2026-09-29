package io.rafaelia.audiostudio;

public final class LowSha256Smoke {
    private static String hex(byte[] digest) {
        char[] out = new char[digest.length * 2];
        char[] alphabet = "0123456789abcdef".toCharArray();
        int p = 0;
        for (byte value : digest) {
            int v = value & 0xff;
            out[p++] = alphabet[v >>> 4];
            out[p++] = alphabet[v & 15];
        }
        return new String(out);
    }

    public static void main(String[] args) {
        LowSha256 sha = new LowSha256();
        byte[] input = new byte[]{'a','b','c'};
        sha.update(input, 0, input.length);
        String actual = hex(sha.finish());
        String expected =
                "ba7816bf8f01cfea414140de5dae2223" +
                "b00361a396177a9cb410ff61f20015ad";
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "SHA-256 KAT mismatch: " + actual);
        }
    }
}
