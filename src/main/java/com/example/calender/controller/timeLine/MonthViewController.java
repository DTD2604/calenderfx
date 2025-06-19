package com.example.calender.controller.timeLine;

import com.example.calender.models.BookRoom;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MonthViewController extends BaseTimeLineController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEvents();
        setupTables();

        Platform.runLater(() -> {
            PauseTransition pause = new PauseTransition(Duration.millis(50));
            pause.setOnFinished(e -> {
                setupBindingsAndListeners();
                drawEvents();
            });
            pause.play();
        });
    }

    @Override
    public void setupTimelineColumns() {
        tbl_timeline.getColumns().clear();
        tbl_timeline.getStyleClass().add("right-border-bold");

        int daysInMonth = timelineStartDate.lengthOfMonth();

        LocalDate firstDayOfMonth = timelineStartDate.withDayOfMonth(1);
        for (int dayOffset = 0; dayOffset < daysInMonth; dayOffset++) {

            LocalDate day = firstDayOfMonth.plusDays(dayOffset);
            String dayLabel = day.format(DateTimeFormatter.ofPattern("dd.MM"));

            TableColumn<BookRoom, Void> dayColumn = new TableColumn<>(dayLabel);
            dayColumn.setPrefWidth(CELL_WIDTH);

            // Thêm style cho các cột
            String baseStyle = "-fx-padding: 0 0 0 5; "; // Padding trái 5px để text không sát quá

            if (dayOffset == 0) { // Cột đầu tháng
                dayColumn.setStyle(baseStyle
                        + "-fx-border-width: 0 2 0 2; -fx-border-color: #c8c8c8; -fx-alignment: CENTER-LEFT; -fx-font-weight: bold;");
            } else if ((day.getDayOfWeek().getValue() == 7)) { // Chủ nhật
                dayColumn.setStyle(baseStyle
                        + "-fx-border-width: 0 2 0 0; -fx-border-color: #c8c8c8; -fx-alignment: CENTER-LEFT; -fx-text-fill: red;");
            } else if ((day.getDayOfWeek().getValue() == 6)) { // Thứ 7
                dayColumn.setStyle(baseStyle
                        + "-fx-border-width: 0 1 0 0; -fx-border-color: #e0e0e0; -fx-alignment: CENTER-LEFT; -fx-text-fill: red;");
            } else { // Các ngày trong tuần
                dayColumn.setStyle(baseStyle
                        + "-fx-border-width: 0 1 0 0; -fx-border-color: #e0e0e0; -fx-alignment: CENTER-LEFT;");
            }

            // Highlight ngày hiện tại
            if (day.equals(LocalDate.now())) {
                dayColumn.setStyle(dayColumn.getStyle() + "; -fx-background-color: #d0f0c0; -fx-opacity: 0.8;");
            }

            tbl_timeline.getColumns().add(dayColumn);
        }

        tbl_timeline.setItems(eventsList);
        updateOverlayWidth(tbl_timeline, ap_overlay);
    }

    @Override
    public void drawEvents() {
        ap_overlay.getChildren().clear();

        TableRow<?> row = (TableRow<?>) tbl_timeline.lookup(".table-row-cell");
        ROW_HEIGHT = row.getHeight();

        LocalDate firstDayOfMonth = timelineStartDate.withDayOfMonth(1);
        Map<String, Integer> roomIndexMap = new HashMap<>();
        for (int i = 0; i < roomList.size(); i++) {
            roomIndexMap.put(roomList.get(i).getRoomName(), i);
        }

        for (BookRoom event : eventsList) {
            try {
                Integer roomIndex = roomIndexMap.get(event.getRoomName());
                if (roomIndex == null)
                    continue;
                LocalDateTime startDateTime = LocalDateTime.of(LocalDate.parse(event.getStartDate()),
                        LocalTime.parse(event.getStartTime()));
                LocalDateTime endDateTime = LocalDateTime.of(LocalDate.parse(event.getEndDate()),
                        LocalTime.parse(event.getEndTime()));
                long dayOffset = ChronoUnit.DAYS.between(firstDayOfMonth, startDateTime.toLocalDate());
                long endDayOffset = ChronoUnit.DAYS.between(firstDayOfMonth, endDateTime.toLocalDate());
                if (dayOffset < 0)
                    continue;
                double startMinutes = startDateTime.getHour() * 60 + startDateTime.getMinute();
                double endMinutes = (endDayOffset - dayOffset) * HOURS_PER_DAY * 60 + endDateTime.getHour() * 60
                        + endDateTime.getMinute();
                double x = dayOffset * CELL_WIDTH + (startMinutes / (HOURS_PER_DAY * 60)) * CELL_WIDTH;
                double width = (endMinutes - startMinutes) / (HOURS_PER_DAY * 60) * CELL_WIDTH;
                double y = roomIndex * ROW_HEIGHT;
                Pane eventPane = createEventPane(event, x, y, width);

                setupEventPaneInteractions(eventPane, event, ap_overlay);

                ap_overlay.getChildren().add(eventPane);
            } catch (Exception e) {
                System.err.println("Invalid date/time format for event: " + event.getFullName());
            }
        }
    }
}
