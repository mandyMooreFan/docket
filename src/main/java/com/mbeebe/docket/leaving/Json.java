package com.mbeebe.docket.leaving;

import java.util.List;
import java.util.Map;

/**
 * The archive's JSON, written by hand (§11.1's "structured, commonly used and
 * machine-readable"; Art. 20 mandates no format, and ICO names JSON as one).
 *
 * <p><strong>Why not Jackson.</strong> Docket has no JSON on its classpath at all,
 * on purpose: it is a server-rendered product with no API and no client-side
 * fetching, so nothing else in it has ever needed to serialise anything. Pulling a
 * binding library in for one file, in a project whose stated preference is the JDK
 * over a dependency, would be paying for a general solution to a problem that is
 * this small: a tree of maps, lists and strings, written once, read by whatever the
 * member points at it. Sixty lines of it, with the escaping done properly, is
 * cheaper to own than a dependency — and the escaping is the only part that has to
 * be right, so it is the part with the test.
 *
 * <p>Emits RFC 8259: control characters escaped as {@code \\uXXXX}, the six named
 * escapes used where they exist, and no other transformation. Not a general
 * serialiser — it takes exactly the shapes {@link Archive} builds and throws on
 * anything else, which is what keeps it honest rather than quietly stringifying
 * something unexpected.
 */
final class Json {

    private static final String INDENT = "  ";

    private Json() {
    }

    static String write(Map<String, Object> root) {
        StringBuilder out = new StringBuilder();
        value(root, out, 0);
        out.append('\n');
        return out.toString();
    }

    private static void value(Object value, StringBuilder out, int depth) {
        switch (value) {
            case null -> out.append("null");
            case Map<?, ?> map -> object(map, out, depth);
            case List<?> list -> array(list, out, depth);
            case String text -> string(text, out);
            case Number number -> out.append(number);
            case Boolean flag -> out.append(flag);
            default -> throw new IllegalArgumentException(
                    "The archive's JSON does not carry " + value.getClass());
        }
    }

    private static void object(Map<?, ?> map, StringBuilder out, int depth) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append("{\n");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(",\n");
            }
            first = false;
            indent(out, depth + 1);
            string(String.valueOf(entry.getKey()), out);
            out.append(": ");
            value(entry.getValue(), out, depth + 1);
        }
        out.append('\n');
        indent(out, depth);
        out.append('}');
    }

    private static void array(List<?> list, StringBuilder out, int depth) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append("[\n");
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                out.append(",\n");
            }
            first = false;
            indent(out, depth + 1);
            value(item, out, depth + 1);
        }
        out.append('\n');
        indent(out, depth);
        out.append(']');
    }

    /**
     * The part that has to be right. Member-written text reaches this unaltered —
     * quotes, backslashes, newlines and anything else a person typed — and a hole
     * here would produce an archive that does not parse, which is the one failure
     * mode a portable copy cannot have.
     */
    private static void string(String text, StringBuilder out) {
        out.append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(character);
                    }
                }
            }
        }
        out.append('"');
    }

    private static void indent(StringBuilder out, int depth) {
        out.append(INDENT.repeat(depth));
    }
}
