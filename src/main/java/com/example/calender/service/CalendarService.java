package com.example.calender.service;

import com.example.calender.config.JsonFileManager;
import com.example.calender.models.EventSchedule;
import com.example.calender.models.Events;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarService {

    private static CalendarService instance;

    public static CalendarService getInstance() {
        if (instance == null) {
            instance = new CalendarService();
        }
        return instance;
    }

    private final JsonFileManager jsonFileManager = JsonFileManager.getInstance("events.json");

    public List<EventSchedule> getAllEvents() {
        jsonFileManager.loadFromFile(Events.class);
        return jsonFileManager.getEventList(EventSchedule.class);
    }

    public List<EventSchedule> getEventsByMonth(LocalDate startDate, LocalDate endDate) {
        jsonFileManager.loadFromFile(Events.class);
        return jsonFileManager.getEventList(EventSchedule.class).stream()
                .filter(event -> !event.getEndDate().isBefore(startDate) && !event.getStartDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    public EventSchedule getEventsByDate(LocalDate date) {
        jsonFileManager.loadFromFile(Events.class);
        return jsonFileManager.getEventList(EventSchedule.class).stream()
                .filter(event -> event.getStartDate().equals(date))
                .findFirst()
                .orElse(null);
    }

    public void addEvent(EventSchedule event) {
        for (EventSchedule oldEvent : jsonFileManager.getEventList(EventSchedule.class)) {
            if (validDuplicateDate(event, oldEvent))
                return;
        }
        jsonFileManager.getEventList(EventSchedule.class).add(event);
        jsonFileManager.saveToFile();
    }

    public boolean updateEvent(EventSchedule oldEvent, EventSchedule newEvent) {
        List<EventSchedule> events = jsonFileManager.getEventList(EventSchedule.class);
        int index = events.indexOf(oldEvent);
        if (index != -1) {
            // Kiểm tra trùng lặp ngày với các sự kiện khác bỏ qua index của sự kiện hiện
            // tại
            for (EventSchedule event : jsonFileManager.getEventList(EventSchedule.class)) {
                if (event != oldEvent && validDuplicateDate(newEvent, event))
                    return false;
            }
            events.set(index, newEvent);
            jsonFileManager.saveToFile();
            return true;
        }
        return false;
    }

    private static boolean validDuplicateDate(EventSchedule event, EventSchedule oldEvent) {
        if (event.getEndDate().isBefore(oldEvent.getStartDate())
                || event.getStartDate().isAfter(oldEvent.getEndDate())) {
            return false;
        }
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Đã tồn tại sự kiện trùng khoảng ngày từ "
                    + oldEvent.getStartDate() + " đến " + oldEvent.getEndDate());
            alert.showAndWait();
        });
        return true;
    }

    public void deleteEvent(EventSchedule event) {
        jsonFileManager.getEventList(EventSchedule.class).remove(event);
        jsonFileManager.saveToFile();
    }

    public boolean isDateInEvent(LocalDate date) {
        for (EventSchedule event : jsonFileManager.getEventList(EventSchedule.class)) {
            if (!date.isBefore(event.getStartDate()) && !date.isAfter(event.getEndDate())) {
                return true;
            }
        }
        return false;
    }
}