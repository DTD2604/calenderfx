package com.example.calender.service;

import com.example.calender.config.JsonFileManager;
import com.example.calender.entity.Events;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TimeLineByHoursService {

    private static TimeLineByHoursService instance;

    public static TimeLineByHoursService getInstance() {
        if (instance == null) {
            instance = new TimeLineByHoursService();
        }
        return instance;
    }

    private final JsonFileManager jsonFileManager = JsonFileManager.getInstance("timeLine.json");

    public List<Events> getEventsByDate(LocalDate date) {
        jsonFileManager.loadFromFile(Events.class);
        return jsonFileManager.getEventList(Events.class).stream()
                .filter(event -> event.getDate().equals(date.toString()))
                .collect(Collectors.toList());
    }

    //lấy dữ liệu trong 3 ngày tính từ hôm này
    public List<Events> getEventsByNextThreeDays(LocalDate date) {
        jsonFileManager.loadFromFile(Events.class);
        return jsonFileManager.getEventList(Events.class).stream()
                .filter(event -> LocalDate.parse(event.getDate()).isAfter(date.minusDays(1))
                        && LocalDate.parse(event.getDate()).isBefore(date.plusDays(3)))
                .collect(Collectors.toList());
    }

    public List<Events> getEventsByHourRange(LocalDate date, String startHour, String endHour) {
        jsonFileManager.loadFromFile(Events.class);
        return jsonFileManager.getEventList(Events.class).stream()
                .filter(event -> event.getDate().equals(date.toString())
                        && event.getStartHour().compareTo(startHour) >= 0
                        && event.getEndHour().compareTo(endHour) <= 0)
                .collect(Collectors.toList());
    }

    public void addEvent(Events event) {
        jsonFileManager.getEventList(Events.class).add(event);
        jsonFileManager.saveToFile();
    }

    public boolean updateEvent(Events oldEvent, Events newEvent) {
        List<Events> events = jsonFileManager.getEventList(Events.class);
        int index = events.indexOf(oldEvent);
        if (index != -1) {
            events.set(index, newEvent);
            jsonFileManager.saveToFile();
            return true;
        }
        return false;
    }

    public boolean deleteEvent(Events event) {
        List<Events> events = jsonFileManager.getEventList(Events.class);
        if (events.remove(event)) {
            jsonFileManager.saveToFile();
            return true;
        }
        return false;
    }
}
