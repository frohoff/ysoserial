package ysoserial;

import org.apache.commons.lang.WordUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Strings {
    public static String join(Iterable<String> strings, String sep, String prefix, String suffix) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : strings) {
            if (! first) sb.append(sep);
            if (prefix != null) sb.append(prefix);
            sb.append(s);
            if (suffix != null) sb.append(suffix);
            first = false;
        }
        return sb.toString();
    }

    public static String repeat(String str, int num) {
        final String[] strs = new String[num];
        Arrays.fill(strs, str);
        return join(Arrays.asList(strs), "", "", "");
    }

    public static List<String> formatTable(List<String[]> rows) {
        final Integer[] maxLengths = new Integer[rows.get(0).length];
        // Max column width, will try to split on spaces if possible
        // Uses 210 as default terminal width, if not specified as system property
        final int maxColumnWidth = Integer.getInteger("terminalWidth", 140) / maxLengths.length;
        for(int index = 0; index < rows.size(); index++) {
            String[] row = rows.get(index);
            if (maxLengths.length != row.length) throw new IllegalStateException("mismatched columns");
            for (int i = 0; i < maxLengths.length; i++) {
                if (row[i].length() > maxColumnWidth) {
                    row[i] = WordUtils.wrap(row[i], maxColumnWidth);
                    String[] split = row[i].split("\n");
                    rows.get(index)[i] = split[0];
                    for (int ii = 1; ii < split.length; ii++) {
                        if (rows.size() <= index + ii || rows.get(index + ii)[i].length() > 0) {
                            rows.add(index + ii, new String[maxLengths.length]);
                            Arrays.fill(rows.get(index + ii), "");
                        }
                        rows.get(index + ii)[i] = split[ii];
                    }
                }
                if (maxLengths[i] == null || maxLengths[i] < row[i].length()) {
                    maxLengths[i] = row[i].length();
                }
            }
        }

        final List<String> lines = new LinkedList<String>();
        for (String[] row : rows) {
            // No need to fill last line with spaces
            for (int i = 0; i < maxLengths.length - 1; i++) {
                final String pad = repeat(" ", maxLengths[i] - row[i].length());
                row[i] = row[i] + pad;
            }
            lines.add(join(Arrays.asList(row), " ", "", ""));
        }
        return lines;
    }

    public static class ToStringComparator implements Comparator<Object> {
        public int compare(Object o1, Object o2) { return o1.toString().compareTo(o2.toString()); }
    }
}
