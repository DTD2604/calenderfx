package com.example.calender.entity;

import lombok.Builder;
import lombok.Data;

import java.time.temporal.Temporal;

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
