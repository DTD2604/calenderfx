package com.example.calender.service;

import com.example.calender.config.JsonFileManager;
import com.example.calender.models.Room;

import java.util.List;

public class RoomService {
    private final JsonFileManager jsonFileManager = JsonFileManager.getInstance("room.json");
    private static RoomService instance;

    public static RoomService getInstance() {
        if (instance == null) {
            instance = new RoomService();
        }
        return instance;
    }

    public List<Room> getAllRooms() {
        jsonFileManager.loadFromFile(Room.class);
        return jsonFileManager.getEventList(Room.class);
    }

    public Room getRoomByName(String roomName) {
        jsonFileManager.loadFromFile(Room.class);
        List<Room> roomList = jsonFileManager.getEventList(Room.class);
        return roomList.stream()
                .filter(room -> room.getRoomName().equals(roomName))
                .findFirst()
                .orElse(null);
    }
}

