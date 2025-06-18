package com.example.calender.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Events {
    private String name;
    private String date; // Format: YYYY-MM-DD
    private String startHour;
    private String endHour;
    private String color;
    private String description;
}
