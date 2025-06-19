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
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class DayViewController extends BaseTimeLineController implements Initializable {
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

        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            String hourLabel = String.format("%02d:00", hour);
            TableColumn<BookRoom, Void> hourColumn = new TableColumn<>(hourLabel);
            hourColumn.setPrefWidth(CELL_WIDTH);
            hourColumn.setSortable(false);

            // Thêm style cho các cột
            String baseStyle = "-fx-padding: 0 0 0 5; "; // Padding trái 5px để text không sát quá

            if (hour == 0) {
                hourColumn.setStyle(baseStyle
                        + "-fx-border-width: 0 2 0 2; -fx-border-color: #c8c8c8; -fx-alignment: CENTER-LEFT;");
            } else if (hour % 6 == 0) { // Đánh dấu mỗi 6 tiếng
                hourColumn.setStyle(baseStyle
                        + "-fx-border-width: 0 2 0 0; -fx-border-color: #c8c8c8; -fx-alignment: CENTER-LEFT;");
            } else {
                hourColumn.setStyle(baseStyle
                        + "-fx-border-width: 0 1 0 0; -fx-border-color: #e0e0e0; -fx-alignment: CENTER-LEFT;");
            }

            tbl_timeline.getColumns().add(hourColumn);
        }

        tbl_timeline.setItems(eventsList);
        updateOverlayWidth(tbl_timeline, ap_overlay);
    }

    @Override
    public void drawEvents() {
        ap_overlay.getChildren().clear();
        TableRow<?> row = (TableRow<?>) tbl_timeline.lookup(".table-row-cell");
        ROW_HEIGHT = row.getHeight();

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

                // Chỉ hiển thị event trong ngày được chọn
                if (!startDateTime.toLocalDate().equals(timelineStartDate) &&
                        !endDateTime.toLocalDate().equals(timelineStartDate) &&
                        !(startDateTime.toLocalDate().isBefore(timelineStartDate) &&
                                endDateTime.toLocalDate().isAfter(timelineStartDate))) {
                    continue;
                }

                // Tính toán thời gian bắt đầu và kết thúc trong ngày
                LocalTime eventStartTime = startDateTime.toLocalDate().equals(timelineStartDate)
                        ? startDateTime.toLocalTime()
                        : LocalTime.of(0, 0);

                LocalTime eventEndTime = endDateTime.toLocalDate().equals(timelineStartDate)
                        ? endDateTime.toLocalTime()
                        : LocalTime.of(23, 59);

                double startMinutes = eventStartTime.getHour() * 60 + eventStartTime.getMinute();
                double endMinutes = eventEndTime.getHour() * 60 + eventEndTime.getMinute();

                double x = (startMinutes / (HOURS_PER_DAY * 60)) * (CELL_WIDTH * HOURS_PER_DAY);
                double width = ((endMinutes - startMinutes) / (HOURS_PER_DAY * 60)) * (CELL_WIDTH * HOURS_PER_DAY);
                double y = roomIndex * ROW_HEIGHT;

                Pane eventPane = createEventPane(event, x, y, width);
                setupEventPaneInteractions(eventPane, event, ap_overlay);
                ap_overlay.getChildren().add(eventPane);

            } catch (Exception e) {
                System.err.println("Invalid date/time format for event: " + event.getFullName());
                e.printStackTrace();
            }
        }
    }
}
