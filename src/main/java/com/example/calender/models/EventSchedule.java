package com.example.calender.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EventSchedule {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String color;
}
