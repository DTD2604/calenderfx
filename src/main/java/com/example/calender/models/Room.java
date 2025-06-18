package com.example.calender.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Room {
    private String roomName; // Name of the room
    private String location; // Location of the room
    private int capacity; // Maximum number of people the room can accommodate
    private String amenities; // Amenities available in the room (e.g., projector, whiteboard)
    private String status; // Status of the room (e.g., "available", "booked", "maintenance")
}
