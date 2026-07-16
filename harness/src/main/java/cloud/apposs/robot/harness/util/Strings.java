package cloud.apposs.robot.harness.util;

import cloud.apposs.util.Table;

import java.util.StringJoiner;

public final class Strings {
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 截断字符串并添加省略号
     *
     * @param  value 待截断的字符串
     * @param  maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * 将表格数据连接为指定分隔符的字符串
     *
     * @param  value 表格数据
     * @param  separator 分隔符
     * @return 逗号分隔的字符串
     */
    public static String join(Table<String> value, String separator) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String s : value) {
            builder.append(s).append(separator);
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    /**
     * 将字符串数组连接为指定分隔符的字符串
     *
     * @param  value 字符串数组
     * @param  delimiter 分隔符
     * @return 连接后的字符串
     */
    public static String join(String[] value, CharSequence delimiter) {
        if (value == null || value.length == 0) {
            return "";
        }
        StringJoiner builder = new StringJoiner(delimiter);
        for (CharSequence cs: value) {
            if (cs == null) {
                continue;
            }
            builder.add(cs);
        }
        return builder.toString();
    }
}
