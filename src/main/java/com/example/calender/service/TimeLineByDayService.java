package com.example.calender.service;

import com.example.calender.config.JsonFileManager;
import com.example.calender.models.BookRoom;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class TimeLineByDayService {

    private static TimeLineByDayService instance;

    public static TimeLineByDayService getInstance() {
        if (instance == null) {
            instance = new TimeLineByDayService();
        }
        return instance;
    }

    private final JsonFileManager jsonFileManager = JsonFileManager.getInstance("timeLineDay.json");

    public List<BookRoom> getEventsByDate(LocalDate date) {
        jsonFileManager.loadFromFile(BookRoom.class);
        return jsonFileManager.getEventList(BookRoom.class).stream()
                .filter(bookRoom -> LocalDate.parse(bookRoom.getStartDate()).equals(date))
                .collect(Collectors.toList());
    }

    public List<BookRoom> getAllEvents() {
        jsonFileManager.loadFromFile(BookRoom.class);
        return jsonFileManager.getEventList(BookRoom.class);
    }

    // lấy dữ liệu trong 3 ngày tính từ hôm này
    public List<BookRoom> getEventsByMonth(LocalDate monthStart) {
        jsonFileManager.loadFromFile(BookRoom.class);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        return jsonFileManager.getEventList(BookRoom.class).stream()
                .filter(bookRoom -> {
                    LocalDate bookRoomDate = LocalDate.parse(bookRoom.getStartDate());
                    return (bookRoomDate.isAfter(monthStart.minusDays(1))
                            && bookRoomDate.isBefore(monthEnd.plusDays(1)));
                })
                .collect(Collectors.toList());
    }

    public List<BookRoom> getEventsByHourRange(LocalDate date, String startHour, String endHour) {
        jsonFileManager.loadFromFile(BookRoom.class);
        return jsonFileManager.getEventList(BookRoom.class).stream()
                .filter(bookRoom -> LocalDate.parse(bookRoom.getStartDate()).equals(date)
                        && !LocalTime.parse(bookRoom.getStartTime()).isBefore(LocalTime.parse(startHour))
                        && !LocalTime.parse(bookRoom.getEndTime()).isAfter(LocalTime.parse(endHour)))
                .collect(Collectors.toList());
    }

    public void addEvent(BookRoom bookRoom) {
        jsonFileManager.getEventList(BookRoom.class).add(bookRoom);
        jsonFileManager.saveToFile();
    }

    public void updateEvent(BookRoom oldBookRoom, BookRoom newBookRoom) {
        List<BookRoom> bookRooms = jsonFileManager.getEventList(BookRoom.class);
        int index = bookRooms.indexOf(oldBookRoom);
        if (index != -1) {
            bookRooms.set(index, newBookRoom);
            jsonFileManager.saveToFile();
        }
    }

    public boolean deleteEvent(BookRoom bookRoom) {
        List<BookRoom> bookRooms = jsonFileManager.getEventList(BookRoom.class);
        // Find and remove the exact BookRoom object based on its unique identifiers
        // (e.g., fullName, roomName, startDate, startTime)
        boolean removed = bookRooms.removeIf(br -> br.getFullName().equals(bookRoom.getFullName()) &&
                br.getRoomName().equals(bookRoom.getRoomName()) &&
                br.getStartDate().equals(bookRoom.getStartDate()) &&
                br.getStartTime().equals(bookRoom.getStartTime()));
        if (removed) {
            jsonFileManager.saveToFile();
            return true;
        }
        return false;
    }

    public List<BookRoom> getAllEventsByRoomName(String roomName) {
        jsonFileManager.loadFromFile(BookRoom.class);
        return jsonFileManager.getEventList(BookRoom.class).stream()
                .filter(bookRoom -> bookRoom.getRoomName().equals(roomName))
                .collect(Collectors.toList());
    }
}
