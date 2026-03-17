package com.ap101gamestudio.timetracker.utils;

public class DateFilterUtils {

    public static Integer[] parseDateFilter(String dateFilter) {
        Integer[] parts = new Integer[]{null, null, null};
        if (dateFilter != null && !dateFilter.isBlank()) {
            String[] split = dateFilter.split("/");
            try {
                if (split.length == 3) {
                    parts[0] = Integer.parseInt(split[0]);
                    parts[1] = Integer.parseInt(split[1]);
                    parts[2] = Integer.parseInt(split[2]);
                } else if (split.length == 2) {
                    if (split[1].length() == 4) {
                        parts[1] = Integer.parseInt(split[0]);
                        parts[2] = Integer.parseInt(split[1]);
                    } else {
                        parts[0] = Integer.parseInt(split[0]);
                        parts[1] = Integer.parseInt(split[1]);
                    }
                } else if (split.length == 1) {
                    if (split[0].length() == 4) {
                        parts[2] = Integer.parseInt(split[0]);
                    } else {
                        parts[0] = Integer.parseInt(split[0]);
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        return parts;
    }
}