package com.example.calender.utils;

import javafx.scene.paint.Color;

public class FormatColor {
    public static String toHexString(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static boolean isDarkColor(String hexColor) {
        Color color = Color.web(hexColor);
        double brightness = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) * 255;
        return brightness < 128;
    }
}
