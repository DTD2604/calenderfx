package com.example.calender.controller;

import com.example.calender.models.EventSchedule;
import com.example.calender.service.CalendarService;
import com.example.calender.utils.FormatColor;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class CalendarController implements Initializable {

    private final CalendarService calendarService = CalendarService.getInstance();

    @FXML
    private Button prevYearBtn;
    @FXML
    private Button nextYearBtn;
    @FXML
//    private JFXDatePicker yearPicker;
    private DatePicker yearPicker;
    @FXML
    private GridPane yearGrid;
    @FXML
    private Button addBtn;
    @FXML
    private Button updateBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button viewReportBtn;

    private int currentYear;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentYear = LocalDate.now().getYear();
        refreshYearGrid();

        yearPicker.setValue(LocalDate.of(currentYear, 1, 1));

        yearPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentYear = newVal.getYear();
                refreshYearGrid();
            }
        });

        prevYearBtn.setOnAction(e -> navigateYear(-1));
        nextYearBtn.setOnAction(e -> navigateYear(1));

        // Add button handlers
        addBtn.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            createEvent(today);
        });

        updateBtn.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            viewEventDetail(today);
        });

        deleteBtn.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            EventSchedule event = calendarService.getEventsByDate(today);
            if (event != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn xóa sự kiện này?",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        calendarService.deleteEvent(event);
                        refreshCalendarView();
                    }
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Không có sự kiện nào để xóa vào ngày " + LocalDate.now());
                alert.showAndWait();
            }
        });

        viewReportBtn.setOnAction(e -> {
            refreshCalendarView();
        });
    }

    private void renderCalendar(List<EventSchedule> allEvents) {
        yearGrid.getChildren().clear();
        yearGrid.getColumnConstraints().clear();
        yearGrid.getRowConstraints().clear();

        for (int i = 0; i < 4; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setPercentWidth(100.0 / 4);
            yearGrid.getColumnConstraints().add(colConstraints);
        }

        for (int i = 0; i < 2; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPercentHeight(100.0 / 3);
            yearGrid.getRowConstraints().add(rowConstraints);
        }

        int col = 0, row = 0;
        for (int month = 1; month <= 12; month++) {
            VBox monthBox = createMonthView(month, allEvents);
            monthBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            yearGrid.add(monthBox, col, row);
            GridPane.setMargin(monthBox, new Insets(10));

            GridPane.setHgrow(monthBox, Priority.ALWAYS);
            GridPane.setVgrow(monthBox, Priority.ALWAYS);

            col++;
            if (col > 3) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createMonthView(int month, List<EventSchedule> allEvents) {
        VBox monthBox = new VBox(5);
        monthBox.getStyleClass().add("month-box");
        monthBox.setFillWidth(true);

        YearMonth yearMonth = YearMonth.of(currentYear, month);

        Label monthLabel = new Label(yearMonth.getMonth().toString());
        monthLabel.getStyleClass().add("month-label");
        monthLabel.setMaxWidth(Double.MAX_VALUE);
        monthLabel.setAlignment(Pos.CENTER);

        GridPane dayGrid = new GridPane();
        dayGrid.setHgap(2);
        dayGrid.setVgap(2);

        String[] days = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold;");
            dayLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayGrid.add(dayLabel, i, 0);
            GridPane.setHgrow(dayLabel, Priority.ALWAYS);
            GridPane.setVgrow(dayLabel, Priority.ALWAYS);
        }

        int daysInMonth = yearMonth.lengthOfMonth();
        int startDay = yearMonth.atDay(1).getDayOfWeek().getValue() % 7;

        int dayCounter = 1;
        for (int row = 1; row <= 6; row++) {
            for (int col = 0; col < 7; col++) {
                if ((row == 1 && col < startDay) || dayCounter > daysInMonth)
                    continue;

                Label dayNum = getLabel(dayCounter, yearMonth);

                dayGrid.add(dayNum, col, row);
                GridPane.setHgrow(dayNum, Priority.ALWAYS);
                GridPane.setVgrow(dayNum, Priority.ALWAYS);
                dayCounter++;
            }
        }
        highlightScheduledDays(dayGrid, month, currentYear, allEvents);

        for (int i = 0; i < 7; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(100.0 / 7);
            colConst.setMinWidth(10);
            dayGrid.getColumnConstraints().add(colConst);
        }

        for (int i = 0; i < 7; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPercentHeight(100.0 / 7);
            rowConst.setMinHeight(10);
            dayGrid.getRowConstraints().add(rowConst);
        }

        monthBox.getChildren().addAll(monthLabel, dayGrid);
        VBox.setVgrow(dayGrid, Priority.ALWAYS);

        return monthBox;
    }

    private Label getLabel(int dayCounter, YearMonth yearMonth) {
        Label dayNum = new Label(String.valueOf(dayCounter));
        dayNum.getStyleClass().add("day-label");
        dayNum.setPrefSize(40, 40);
        dayNum.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        dayNum.setAlignment(Pos.CENTER);

        dayNum.setOnMouseEntered(e -> dayNum.setCursor(Cursor.HAND));
        dayNum.setOnMouseExited(e -> dayNum.setCursor(Cursor.DEFAULT));

        LocalDate selectDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), dayCounter);

        if (calendarService.isDateInEvent(selectDate)) {
            dayNum.setOpacity(0.4);
            dayNum.setOnMouseClicked(e -> viewEventDetail(selectDate));
        } else {
            dayNum.setOnMouseClicked(e -> createEvent(selectDate));
        }

        // Tạo tooltip một lần, gán cho dayNum
        Tooltip tooltip = new Tooltip();
        tooltip.getStyleClass().add("tooltip");
        dayNum.setTooltip(tooltip);

        dayNum.setOnMouseMoved(e -> {
            EventSchedule event = calendarService.getEventsByDate(selectDate);
            if (event != null && calendarService.isDateInEvent(selectDate)) {
                GridPane grid = new GridPane();
                grid.setVgap(0);
                grid.setHgap(0);
                grid.setStyle("-fx-grid-lines-visible: true;"); // Có thể bật gridline để debug

                // Tạo từng ô (label) và set class
                grid.add(createCellLabel("Tên"), 0, 0);
                grid.add(createCellLabel(event.getName()), 1, 0);

                grid.add(createCellLabel("Từ"), 0, 1);
                grid.add(createCellLabel(event.getStartDate().toString()), 1, 1);

                grid.add(createCellLabel("Đến"), 0, 2);
                grid.add(createCellLabel(event.getEndDate().toString()), 1, 2);

                grid.add(createCellLabel("Mô tả"), 0, 3);
                grid.add(createCellLabel(event.getDescription()), 1, 3);

                tooltip.setGraphic(grid);
                tooltip.setText("");
            } else {
                tooltip.setGraphic(null);
                tooltip.setText("");
            }
        });

        return dayNum;
    }

    private Label createCellLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("cell-label");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    private void viewEventDetail(LocalDate selectDate) {
        EventSchedule events = calendarService.getEventsByDate(selectDate);

        if (events == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Chi tiết sự kiện");
            alert.setHeaderText(null);
            alert.setContentText("Không có sự kiện nào vào ngày " + selectDate);
            alert.showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết sự kiện ngày " + selectDate);
        ButtonType applyButton = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButton = new ButtonType("Xóa", ButtonBar.ButtonData.NO);
        dialog.getDialogPane().getButtonTypes().addAll(applyButton, deleteButton, ButtonType.CANCEL);

        TextField nameField = new TextField();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextArea descriptionArea = new TextArea();
        ColorPicker colorPicker = new ColorPicker();

        nameField.setText(events.getName());
        startDatePicker.setValue(events.getStartDate());
        endDatePicker.setValue(events.getEndDate());
        descriptionArea.setText(events.getDescription());
        colorPicker.setValue(Color.web(events.getColor()));

        GridPane gridPane = getGridPane(nameField, startDatePicker, endDatePicker, descriptionArea, colorPicker);
        dialog.getDialogPane().setContent(gridPane);

        dialog.showAndWait().ifPresent(response -> {
            if (response == applyButton) {
                String newName = nameField.getText();
                LocalDate newStart = startDatePicker.getValue();
                LocalDate newEnd = endDatePicker.getValue();
                String newDesc = descriptionArea.getText();
                String newColor = FormatColor.toHexString(colorPicker.getValue());

                if (newName.isEmpty() || newStart == null || newEnd == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng điền đầy đủ thông tin tên và ngày.");
                    alert.showAndWait();
                    return;
                }

                EventSchedule newEvent = EventSchedule.builder()
                        .name(newName)
                        .startDate(newStart)
                        .endDate(newEnd)
                        .description(newDesc)
                        .color(newColor)
                        .build();

                boolean update = calendarService.updateEvent(events, newEvent);

                if (update) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cập nhật sự kiện thành công.");
                    alert.showAndWait();
                }

            } else if (response == deleteButton) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn xóa sự kiện này?",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        calendarService.deleteEvent(events);
                        refreshCalendarView();
                    }
                });
            }
        });

        refreshCalendarView();
    }

    private void createEvent(LocalDate selectDate) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đặt lịch");
        dialog.setHeaderText("Nhập thông tin sự kiện");

        // Tạo các control cho form
        TextField nameField = new TextField();
        nameField.setPromptText("Ví dụ: Hà Nội - Đà Nẵng");
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setValue(selectDate);
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setValue(selectDate.plusDays(7));
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("ví dụ: khởi hành tại ga số 1");
        ColorPicker colorPicker = new ColorPicker();

        GridPane gridPane = getGridPane(nameField, startDatePicker, endDatePicker, descriptionArea, colorPicker);
        dialog.getDialogPane().setContent(gridPane);

        // Thêm nút OK và Cancel
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Xử lý khi người dùng bấm OK
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText();
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                String description = descriptionArea.getText();
                Color color = colorPicker.getValue();

                EventSchedule event = EventSchedule.builder()
                        .name(name)
                        .description(description)
                        .startDate(startDate)
                        .endDate(endDate)
                        .color(FormatColor.toHexString(color))
                        .build();

                calendarService.addEvent(event);

                refreshCalendarView();
            }
        });
    }

    private static GridPane getGridPane(TextField nameField, DatePicker startDatePicker, DatePicker endDatePicker,
                                        TextArea descriptionArea, ColorPicker colorPicker) {

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (endDatePicker.getValue() != null && newValue.isAfter(endDatePicker.getValue())) {
                showAlert("Ngày bắt đầu không được sau ngày kết thúc.");
                startDatePicker.setValue(oldValue);
            }
        });

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (startDatePicker.getValue() != null && newValue.isBefore(startDatePicker.getValue())) {
                showAlert("Ngày kết thúc không được trước ngày bắt đầu.");
                endDatePicker.setValue(oldValue);
            }
        });

        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(10);
        detailGrid.setVgap(10);
        detailGrid.setPadding(new Insets(10));

        detailGrid.add(new Label("Tên:"), 0, 0);
        detailGrid.add(nameField, 1, 0);

        detailGrid.add(new Label("Ngày bắt đầu:"), 0, 1);
        detailGrid.add(startDatePicker, 1, 1);

        detailGrid.add(new Label("Ngày kết thúc:"), 0, 2);
        detailGrid.add(endDatePicker, 1, 2);

        detailGrid.add(new Label("Mô tả:"), 0, 3);
        detailGrid.add(descriptionArea, 1, 3);

        detailGrid.add(new Label("Màu:"), 0, 4);
        detailGrid.add(colorPicker, 1, 4);
        return detailGrid;
    }

    private static void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshCalendarView() {
        List<EventSchedule> allEvents = calendarService.getAllEvents();
        yearGrid.getChildren().clear();
        renderCalendar(allEvents);
    }

    private void navigateYear(int offset) {
        yearPicker.setValue(yearPicker.getValue().plusYears(offset));
    }

    private void refreshYearGrid() {
        // Logic to refresh the year grid based on the currentYear
        List<EventSchedule> allEvents = calendarService.getAllEvents();
        yearGrid.getChildren().clear();
        renderCalendar(allEvents);
        highLightStartDate(yearPicker);
    }

    private void highLightStartDate(DatePicker datePicker) {
        AtomicBoolean isRangeChecked = new AtomicBoolean(false);
        final LocalDate[] startDate = {LocalDate.now()};
        final LocalDate[] endDate = {startDate[0].plusDays(41)};
        final List<EventSchedule> eventList = new ArrayList<>();

        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty) {
                    // Lần đầu gặp ngày hợp lệ → xác định startDate và endDate và lấy data
                    if (!isRangeChecked.get()) {
                        startDate[0] = item.minusDays(item.getDayOfMonth() - 1); // Ngày đầu tháng
                        endDate[0] = item.plusDays(41); // 42 ô tất cả

                        // số lần call api
                        System.out.println("Lấy dữ liệu từ " + startDate[0] + " đến " + endDate[0]);
                        // Gọi API lấy event trong khoảng đó
                        List<EventSchedule> response = calendarService.getEventsByMonth(startDate[0], endDate[0]);
                        eventList.addAll(response);
                        System.out.println("Đã lấy " + response.size() + " sự kiện từ API");

                        isRangeChecked.set(true);
                    }

                    // Highlight nếu ngày khớp event
                    for (EventSchedule event : eventList) {
                        if (item.equals(event.getStartDate())) {
                            setStyle("-fx-background-color: " + event.getColor() + ";");
                            if (FormatColor.isDarkColor(event.getColor())) {
                                setTextFill(Color.WHITE);
                            } else {
                                setTextFill(Color.BLACK);
                            }
                            break;
                        }
                    }
                    //chạy tới ngày cuối thì chuyển check thàn false
                    if (item.equals(endDate[0].minusDays(1))) {
                        isRangeChecked.set(false);
                    }
                }
            }
        });
    }


    private void highlightScheduledDays(GridPane dayGrid, int month, int year, List<EventSchedule> allEvents) {
        for (Node node : dayGrid.getChildren()) {
            if (node instanceof Label) {
                Label dayLabel = (Label) node;
                try {
                    int dayValue = Integer.parseInt(dayLabel.getText());
                    LocalDate thisDate = LocalDate.of(year, month, dayValue);

                    for (EventSchedule event : allEvents) {
                        if (!thisDate.isBefore(event.getStartDate()) && !thisDate.isAfter(event.getEndDate())) {
                            String color = event.getColor() != null ? event.getColor() : "#a0e7a0";
                            boolean isDark = FormatColor.isDarkColor(color);

                            dayLabel.setStyle(
                                    "-fx-background-color: " + color + ";" +
                                            "-fx-border-color: #333;" +
                                            "-fx-text-fill: " + (isDark ? "white" : "black") + ";");
                            break;
                        }
                    }

                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}
