package lang.qkm.util;

public final class StrEscape {

    public final String buf;
    public final int end;

    private int ptr;

    public StrEscape(String text, int start, int end) {
        if (text == null)
            throw new IllegalArgumentException("Illegal escape on null string");
        if (0 > start || start > end || end > text.length())
            throw new IllegalArgumentException("Illegal span [" + start + ", " + end + ") over string of length " + text.length());

        this.buf = text;
        this.ptr = start;
        this.end = end;
    }

    public boolean hasNext() {
        return this.ptr < this.end;
    }

    public int next() {
        final char ch1 = this.buf.charAt(this.ptr++);
        if (Character.isHighSurrogate(ch1)) {
            final char ch2 = this.buf.charAt(this.ptr++);
            return Character.toCodePoint(ch1, ch2);
        }

        if (ch1 != '\\')
            return ch1;

        switch (this.buf.charAt(this.ptr++)) {
        case 'a':   return 0x0007;
        case 'b':   return '\b';
        case 'f':   return '\f';
        case 'n':   return '\n';
        case 'r':   return '\r';
        case 't':   return '\t';
        case 'v':   return 0x000B;
        case '\'':  return '\'';
        case '\"':  return '\"';
        case '\\':  return '\\';
        case 'u':
            final int u4 = Integer.parseInt(this.buf.substring(this.ptr, this.ptr + 4), 16);
            this.ptr += 4;
            if (!isValidCodePoint(u4))
                throw new RuntimeException("Illegal code point U+" + String.format("%04x", u4));
            return u4;
        case 'U':
            final int u6 = Integer.parseInt(this.buf.substring(this.ptr, this.ptr + 6), 16);
            this.ptr += 6;
            if (!isValidCodePoint(u6))
                throw new RuntimeException("Illegal code point U+" + String.format("%04x", u6));
            return u6;
        case 'x':
            // \xhh is guaranteed to be a valid byte
            final int x = Integer.parseInt(this.buf.substring(this.ptr, this.ptr + 2), 16);
            this.ptr += 2;
            return x;
        default:
            final int c = Integer.parseInt(this.buf.substring(this.ptr - 1, this.ptr + 2), 8);
            if (!isValidByte(c))
                throw new RuntimeException("Illegal code point \\" + String.format("%03o", c));
            this.ptr += 2;
            return c;
        }
    }

    public static boolean isValidCodePoint(int cp) {
        return 0x0000 <= cp && cp <= 0xD7FF
            || 0xE000 <= cp && cp <= 0x10FFFF;
    }

    public static boolean isValidByte(int cp) {
        // makes sure it's an unsigned byte
        return (cp & ~((1 << 8) - 1)) == 0;
    }
}
