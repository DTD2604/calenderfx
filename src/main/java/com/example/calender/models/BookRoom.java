package com.example.calender.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookRoom{
    private String fullName;
    private String email;
    private String phoneNumber;
    private String roomName;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String purpose;
    private String color; // e.g., "#FF5733"
    private String status; // "pending", "approved", "rejected"
}
