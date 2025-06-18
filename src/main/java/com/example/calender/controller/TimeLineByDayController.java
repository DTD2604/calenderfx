package com.example.calender.controller;

import com.example.calender.entity.BookRoom;
import com.example.calender.service.TimeLineByDayService;
import com.example.calender.utils.FormatColor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Optional;

public class TimeLineByDayController implements Initializable {

    // <editor-fold desc="Constants">
    private static final double CELL_WIDTH = 200.0;
    private static final double ROW_HEIGHT = 29.0;
    private static final String HOVER_EVENT_COLOR = "#AA66CC";

    @FXML
    private TableView<BookRoom> timelineTable;
    @FXML
    private TableView<BookRoom> eventNameTable;
    @FXML
    private TableColumn<BookRoom, String> nameEvent;
    @FXML
    private TableColumn<BookRoom, Number> sttColumn;
    @FXML
    private DatePicker dateBox;
    @FXML
    private Button prevBtn, nextBtn, updateBtn, addBtn, deleteBtn;
    @FXML
    private Pane overlayPane;
    @FXML
    private ScrollPane scrollPane;

    private final ObservableList<BookRoom> eventsList = FXCollections.observableArrayList();
    private LocalDate timelineStartDate;

    // Biến cờ để ngăn các vòng lặp cập nhật vô hạn khi đồng bộ hóa thanh cuộn
    private boolean isHorizontallySyncing = false;

    private final TimeLineByDayService timeLineByDayService = TimeLineByDayService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.timelineStartDate = LocalDate.now().withDayOfMonth(1); // Initialize to the first day of the current month

        configureControls();
        loadEvents();
        setupTables();

        Platform.runLater(() -> {
            setupBindingsAndListeners();
            refreshTimelineView();
        });
    }

    private void loadEvents() {
        eventsList.setAll(timeLineByDayService.getEventsByMonth(timelineStartDate));
    }

    /**
     * Cấu hình các điều khiển chính như nút và DatePicker.
     */
    private void configureControls() {
        dateBox.setValue(timelineStartDate);
        prevBtn.setOnAction(e -> navigateDate(-1));
        nextBtn.setOnAction(e -> navigateDate(1));
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
            viewEventDetail(today);
        });
        dateBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null
                    && (!newVal.equals(timelineStartDate) || newVal.getMonth() != timelineStartDate.getMonth()
                            || newVal.getYear() != timelineStartDate.getYear())) {
                this.timelineStartDate = newVal.withDayOfMonth(1); // Always set to the first day of the month
                refreshTimelineView();
            }
        });
    }

    private void viewEventDetail(LocalDate today) {
        List<BookRoom> bookRooms = timeLineByDayService.getEventsByDate(today);
        if (bookRooms == null || bookRooms.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Không có sự kiện nào vào ngày " + today);
            alert.showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết sự kiện ngày " + today);

        // Buttons
        ButtonType closeButton = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType updateButton = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButton = new ButtonType("Xóa", ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(updateButton, deleteButton, closeButton);

        // Event List
        ListView<BookRoom> eventListView = new ListView<>(FXCollections.observableArrayList(bookRooms));
        eventListView.setCellFactory(param -> new ListCell<BookRoom>() {
            @Override
            protected void updateItem(BookRoom item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : Optional.ofNullable(item.getFullName()).orElse("") + " ("
                                + Optional.ofNullable(item.getStartTime()).orElse("") + " - "
                                + Optional.ofNullable(item.getEndTime()).orElse("") + ")");
            }
        });

        // Detail form
        TextField nameField = new TextField();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextArea descriptionArea = new TextArea();
        ColorPicker colorPicker = new ColorPicker();
        TextField startTimeField = new TextField();
        TextField endTimeField = new TextField();

        GridPane detailPane = new GridPane();
        detailPane.setHgap(10);
        detailPane.setVgap(10);
        detailPane.addRow(0, new Label("Tên sự kiện:"), nameField);
        detailPane.addRow(1, new Label("Ngày bắt đầu:"), startDatePicker);
        detailPane.addRow(2, new Label("Ngày kết thúc:"), endDatePicker);
        detailPane.addRow(3, new Label("Mô tả:"), descriptionArea);
        detailPane.addRow(4, new Label("Màu sắc:"), colorPicker);
        detailPane.addRow(5, new Label("Giờ bắt đầu:"), startTimeField);
        detailPane.addRow(6, new Label("Giờ kết thúc:"), endTimeField);

        // Binding event selection
        eventListView.getSelectionModel().selectedItemProperty().addListener((obs, oldBookRoom, newBookRoom) -> {
            if (newBookRoom != null) {
                nameField.setText(Optional.ofNullable(newBookRoom.getFullName()).orElse(""));
                startDatePicker.setValue(
                        newBookRoom.getStartDate() != null ? LocalDate.parse(newBookRoom.getStartDate()) : null);
                endDatePicker
                        .setValue(newBookRoom.getEndDate() != null ? LocalDate.parse(newBookRoom.getEndDate()) : null);
                descriptionArea.setText(Optional.ofNullable(newBookRoom.getPurpose()).orElse(""));
                colorPicker.setValue(
                        Optional.ofNullable(Color.valueOf(newBookRoom.getColor())).orElse(Color.valueOf("#FFFFFF")));
                startTimeField.setText(Optional.ofNullable(newBookRoom.getStartTime()).orElse(""));
                endTimeField.setText(Optional.ofNullable(newBookRoom.getEndTime()).orElse(""));
            } else {
                nameField.clear();
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                descriptionArea.clear();
                colorPicker.setValue(Color.WHITE);
                startTimeField.clear();
                endTimeField.clear();
            }
        });

        SplitPane splitPane = new SplitPane(eventListView, detailPane);
        splitPane.setDividerPositions(0.3);
        dialog.getDialogPane().setContent(splitPane);

        // Disable Update/Delete when no selection
        Node updateBtn = dialog.getDialogPane().lookupButton(updateButton);
        Node deleteBtn = dialog.getDialogPane().lookupButton(deleteButton);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);

        eventListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean disable = newVal == null;
            updateBtn.setDisable(disable);
            deleteBtn.setDisable(disable);
        });

        dialog.showAndWait().ifPresent(response -> {
            BookRoom selectedBookRoom = eventListView.getSelectionModel().getSelectedItem();
            if (selectedBookRoom == null)
                return;

            if (response == updateButton) {
                // Validate input
                if (nameField.getText().isEmpty() || startDatePicker.getValue() == null
                        || startTimeField.getText().isEmpty() || endTimeField.getText().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Thiếu thông tin");
                    alert.setHeaderText(null);
                    alert.setContentText("Vui lòng nhập đầy đủ thông tin cần thiết.");
                    alert.showAndWait();
                    return;
                }

                // Update bookRoom
                BookRoom updatedBookRoom = BookRoom.builder()
                        .fullName(nameField.getText())
                        .startDate(startDatePicker.getValue().toString())
                        .endDate(endDatePicker.getValue().toString())
                        .startTime(startTimeField.getText())
                        .endTime(endTimeField.getText())
                        .purpose(descriptionArea.getText())
                        .color(FormatColor.toHexString(colorPicker.getValue()))
                        .status("approved")
                        .build();

                timeLineByDayService.updateEvent(selectedBookRoom, updatedBookRoom);
                refreshTimelineView();
            }

            if (response == deleteButton) {
                // Confirm delete
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Xác nhận xóa");
                confirm.setHeaderText(null);
                confirm.setContentText("Bạn có chắc muốn xóa sự kiện này?");
                confirm.showAndWait().ifPresent(confirmResult -> {
                    if (confirmResult == ButtonType.OK) {
                        timeLineByDayService.deleteEvent(selectedBookRoom);
                        refreshTimelineView();
                    }
                });
            }
        });
    }

    private void createEvent(LocalDate today) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tạo sự kiện mới vào ngày " + today);

        ButtonType saveButton = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        // Form nhập thông tin
        TextField nameField = new TextField();
        DatePicker startDatePicker = new DatePicker(today);
        DatePicker endDatePicker = new DatePicker(today);
        TextArea descriptionArea = new TextArea();
        ColorPicker colorPicker = new ColorPicker(Color.LIGHTBLUE);
        TextField startTimeField = new TextField();
        TextField endTimeField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Tên sự kiện:"), nameField);
        grid.addRow(1, new Label("Ngày bắt đầu:"), startDatePicker);
        grid.addRow(2, new Label("Ngày kết thúc:"), endDatePicker);
        grid.addRow(3, new Label("Mô tả:"), descriptionArea);
        grid.addRow(4, new Label("Màu sắc:"), colorPicker);
        grid.addRow(5, new Label("Giờ bắt đầu:"), startTimeField);
        grid.addRow(6, new Label("Giờ kết thúc:"), endTimeField);

        dialog.getDialogPane().setContent(grid);

        // Xử lý nút OK
        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButton) {
                // Validate đầu vào
                if (nameField.getText().trim().isEmpty()
                        || startDatePicker.getValue() == null
                        || startTimeField.getText().trim().isEmpty()
                        || endTimeField.getText().trim().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Thiếu thông tin");
                    alert.setHeaderText(null);
                    alert.setContentText("Vui lòng nhập đầy đủ các trường bắt buộc.");
                    alert.showAndWait();
                    return;
                }

                // Tạo sự kiện mới
                BookRoom newBookRoom = BookRoom.builder()
                        .fullName(nameField.getText())
                        .startDate(startDatePicker.getValue().toString())
                        .endDate(endDatePicker.getValue().toString())
                        .startTime(startTimeField.getText())
                        .endTime(endTimeField.getText())
                        .color(FormatColor.toHexString(colorPicker.getValue()))
                        .purpose(descriptionArea.getText())
                        .status("approved")
                        .build();

                timeLineByDayService.addEvent(newBookRoom);
                refreshTimelineView();

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Thành công");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Sự kiện mới đã được thêm vào!");
                successAlert.showAndWait();
            }
        });
    }

    /**
     * Thiết lập cấu trúc cho cả hai TableView.
     */
    private void setupTables() {
        setupNameTable();
        setupTimelineColumns();
    }

    private void setupNameTable() {
        // TableColumn<BookRoom, String> parentColumn = new TableColumn<>("Events");

        eventNameTable.getColumns().clear();
        eventNameTable.getColumns().addAll(sttColumn, nameEvent);
        // eventNameTable.getColumns().add(parentColumn);
        eventNameTable.setItems(eventsList);

        // Set cell value factory for event name column
        nameEvent.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                Optional.ofNullable(cellData.getValue().getFullName()).orElse("")));

        sttColumn.setCellFactory(col -> new TableCell<BookRoom, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        sttColumn.setStyle("-fx-alignment: CENTER; -fx-background-color: #e9e9e9; -fx-border-color: #c8c8c8;");
    }

    /**
     * Tạo và thiết lập các cột ngày và giờ cho bảng dòng thời gian.
     */
    private void setupTimelineColumns() {
        timelineTable.getColumns().clear();

        int daysInMonth = timelineStartDate.lengthOfMonth();
        for (int dayOffset = 0; dayOffset < daysInMonth; dayOffset++) {
            LocalDate day = timelineStartDate.plusDays(dayOffset);
            String dayLabel = day.format(DateTimeFormatter.ofPattern("dd.MM"));

            TableColumn<BookRoom, Void> dayColumn = new TableColumn<>(dayLabel);
            dayColumn.setPrefWidth(CELL_WIDTH);

            // Add time markers
            // for (int hour = 0; hour < 24; hour++) {
            // TableColumn<BookRoom, Void> hourColumn = new
            // TableColumn<>(String.format("%02d:00", hour));
            // hourColumn.setPrefWidth(CELL_WIDTH / 24.0);
            // hourColumn.setSortable(false);
            // dayColumn.getColumns().add(hourColumn);
            // }

            // Add to timeline table
            timelineTable.getColumns().add(dayColumn);
        }

        timelineTable.setItems(eventsList);
        updateOverlayWidth();
    }

    /**
     * Thiết lập các ràng buộc (bindings) và listener giữa các thành phần UI.
     */
    private void setupBindingsAndListeners() {
        // Đảm bảo overlayPane và scrollPane luôn nằm dưới header của bảng
        alignPanesWithTableHeader(timelineTable, scrollPane, overlayPane);

        // Ẩn thanh cuộn mặc định của các TableView
        hideDefaultScrollBars();

        // Đồng bộ hóa thanh cuộn dọc giữa hai bảng
        syncVerticalScrollBars(eventNameTable, timelineTable);

        // Đồng bộ hóa thanh cuộn ngang của timelineTable với scrollPane tùy chỉnh
        syncHorizontalScrollBar(timelineTable, scrollPane);

        // Đồng bộ hóa thanh cuộn dọc của overlayPane với scrollPane
        syncVerticalScrollBar(timelineTable, scrollPane);

        // Thêm listener cho scrollPane để cập nhật vị trí của overlayPane
        scrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            double scrollX = newVal.doubleValue() * (scrollPane.getContent().getBoundsInLocal().getWidth()
                    - scrollPane.getViewportBounds().getWidth());
            overlayPane.setLayoutX(-scrollX);
        });

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            double scrollY = newVal.doubleValue() * (scrollPane.getContent().getBoundsInLocal().getHeight()
                    - scrollPane.getViewportBounds().getHeight());
            overlayPane.setLayoutY(-scrollY);
        });

        // Thêm listener để cập nhật độ rộng của overlayPane khi TableView thay đổi
        timelineTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateOverlayWidth();
        });

        // Cập nhật độ rộng ban đầu
        Platform.runLater(this::updateOverlayWidth);
    }

    /**
     * Vẽ lại tất cả các sự kiện lên overlayPane.
     */
    private void drawEvents() {
        overlayPane.getChildren().clear();

        for (int i = 0; i < eventsList.size(); i++) {
            BookRoom bookRoom = eventsList.get(i);
            LocalDate bookRoomDate;
            try {
                bookRoomDate = LocalDate.parse(Optional.ofNullable(bookRoom.getStartDate()).orElse(""));
            } catch (Exception e) {
                System.err.println("Invalid date format for bookRoom: "
                        + Optional.ofNullable(bookRoom.getFullName()).orElse("Unknown"));
                continue;
            }

            long dayOffset = ChronoUnit.DAYS.between(timelineStartDate, bookRoomDate);
            if (dayOffset < 0 || dayOffset >= timelineStartDate.lengthOfMonth())
                continue;

            // Parse start and end times
            LocalTime startTime = LocalTime.parse(Optional.ofNullable(bookRoom.getStartTime()).orElse("00:00"));
            LocalTime endTime = LocalTime.parse(Optional.ofNullable(bookRoom.getEndTime()).orElse("23:59"));

            // Calculate X position based on day offset and time
            double dayWidth = CELL_WIDTH;
            double x = dayOffset * dayWidth;

            // Calculate time-based offset within the day
            double minutesInDay = startTime.getHour() * 60 + startTime.getMinute();
            double timeOffset = (minutesInDay / (24 * 60)) * dayWidth;
            x += timeOffset;

            // Calculate width based on duration
            double durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
            double width = (durationMinutes / (24 * 60)) * dayWidth;

            double y = i * ROW_HEIGHT;

            Pane eventPane = createEventPane(bookRoom, x, y, width);
            overlayPane.getChildren().add(eventPane);
        }
    }

    /**
     * Tạo một Pane đại diện cho một sự kiện.
     */
    private Pane createEventPane(BookRoom bookRoom, double x, double y, double width) {
        Pane eventPane = new Pane();
        eventPane.setLayoutX(x);
        eventPane.setLayoutY(y);
        eventPane.setPrefWidth(width);
        eventPane.setPrefHeight(ROW_HEIGHT - 1); // Trừ 1 để có khoảng hở
        eventPane.setStyle("-fx-background-color: " + bookRoom.getColor() + "; -fx-background-radius: 6;"); // Placeholder
                                                                                                            // color

        Tooltip.install(eventPane,
                new Tooltip(Optional.ofNullable(bookRoom.getFullName()).orElse("") + "\n"
                        + Optional.ofNullable(bookRoom.getStartTime()).orElse("") + " - "
                        + Optional.ofNullable(bookRoom.getEndTime()).orElse("")));

        setupEventPaneInteractions(eventPane, bookRoom);
        return eventPane;
    }

    /**
     * Thiết lập các tương tác (hover, kéo, thả, chỉnh độ rộng) cho một event pane.
     */
    private void setupEventPaneInteractions(Pane eventPane, BookRoom bookRoom) {
        // Hiệu ứng Hover
        eventPane.setOnMouseEntered(e -> eventPane.setStyle("-fx-background-color: " + HOVER_EVENT_COLOR
                + "; -fx-background-radius: 6; -fx-border-color: white; -fx-border-width: 1.5;"));
        eventPane.setOnMouseExited(e -> eventPane.setStyle("-fx-background-color: #AA66CC; -fx-background-radius: 6;")); // Placeholder
                                                                                                                         // color

        final double[] dragOffset = new double[2];
        final double[] initialWidth = new double[1];
        final double[] initialX = new double[1];
        final boolean[] resizingRight = { false };
        final boolean[] resizingLeft = { false };

        final double RESIZE_MARGIN = 8;

        eventPane.setOnMousePressed(e -> {
            double mouseX = e.getX();

            if (mouseX >= eventPane.getWidth() - RESIZE_MARGIN) {
                resizingRight[0] = true;
                resizingLeft[0] = false;
                initialWidth[0] = eventPane.getWidth();
            } else if (mouseX <= RESIZE_MARGIN) {
                resizingLeft[0] = true;
                resizingRight[0] = false;
                initialWidth[0] = eventPane.getWidth();
                initialX[0] = eventPane.getLayoutX();
                dragOffset[0] = e.getSceneX();
            } else {
                resizingRight[0] = false;
                resizingLeft[0] = false;
                dragOffset[0] = mouseX;
                dragOffset[1] = e.getY();
                eventPane.setCursor(Cursor.MOVE);
            }
            e.consume();
        });

        eventPane.setOnMouseDragged(e -> {
            Point2D localCoords = overlayPane.sceneToLocal(e.getSceneX(), e.getSceneY());

            if (resizingRight[0]) {
                double newWidth = Math.max(20, localCoords.getX() - eventPane.getLayoutX());
                newWidth = Math.min(newWidth, overlayPane.getWidth() - eventPane.getLayoutX());
                eventPane.setPrefWidth(newWidth);

            } else if (resizingLeft[0]) {
                double deltaX = e.getSceneX() - dragOffset[0];
                double newLayoutX = initialX[0] + deltaX;
                double newWidth = initialWidth[0] - deltaX;

                if (newLayoutX >= 0 && newWidth >= 20) {
                    eventPane.setLayoutX(newLayoutX);
                    eventPane.setPrefWidth(newWidth);
                }

            } else {
                double newX = snapToGrid(localCoords.getX() - dragOffset[0]);
                int newRowIndex = (int) Math.round((localCoords.getY() - dragOffset[1]) / ROW_HEIGHT);
                double newY = Math.max(0, newRowIndex * ROW_HEIGHT);

                if (newX >= 0 && newX + eventPane.getWidth() <= overlayPane.getWidth() &&
                        newY >= 0 && newY + eventPane.getHeight() <= overlayPane.getHeight()) {
                    eventPane.setLayoutX(newX);
                    eventPane.setLayoutY(newY);
                }
            }
            e.consume();
        });

        eventPane.setOnMouseMoved(e -> {
            if (e.getX() >= eventPane.getWidth() - RESIZE_MARGIN) {
                eventPane.setCursor(Cursor.H_RESIZE);
            } else if (e.getX() <= RESIZE_MARGIN) {
                eventPane.setCursor(Cursor.W_RESIZE);
            } else {
                eventPane.setCursor(Cursor.DEFAULT);
            }
        });

        eventPane.setOnMouseReleased(e -> {
            updateEventFromPane(eventPane, bookRoom);
            eventPane.setCursor(Cursor.DEFAULT);
            e.consume();
        });
    }

    /**
     * Cập nhật thông tin (thời gian, ngày, vị trí) của đối tượng BookRoom từ vị trí
     * của Pane.
     */
    private void updateEventFromPane(Pane eventPane, BookRoom bookRoom) {
        double startX = eventPane.getLayoutX();
        double width = eventPane.getWidth();

        // Calculate day offset and time from x position
        int dayOffset = (int) (startX / CELL_WIDTH);
        double minutesInDay = (startX % CELL_WIDTH) * 1440.0 / CELL_WIDTH;
        double durationMinutes = width * 1440.0 / CELL_WIDTH;

        LocalTime newStart = LocalTime.of((int) (minutesInDay / 60), (int) (minutesInDay % 60));
        LocalTime newEnd = newStart.plusMinutes((long) durationMinutes);

        LocalDate newDate = timelineStartDate.plusDays(dayOffset);

        BookRoom updatedBookRoom = BookRoom.builder()
                .fullName(Optional.ofNullable(bookRoom.getFullName()).orElse(""))
                .startDate(newDate.toString())
                .endDate(newDate.toString()) // Assuming end date is same as start date for now
                .startTime(newStart.format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(newEnd.format(DateTimeFormatter.ofPattern("HH:mm")))
                .purpose(Optional.ofNullable(bookRoom.getPurpose()).orElse(""))
                .status(Optional.ofNullable(bookRoom.getStatus()).orElse("approved"))
                .build();

        timeLineByDayService.updateEvent(bookRoom, updatedBookRoom);

        refreshTimelineView();
    }

    /**
     * Làm mới toàn bộ giao diện: tạo lại cột, vẽ lại sự kiện.
     */
    private void refreshTimelineView() {
        loadEvents();
        setupTimelineColumns();
        drawEvents();
        eventNameTable.refresh();
        timelineTable.refresh();
    }

    /**
     * Chuyển ngày tới hoặc lùi.
     */
    private void navigateDate(int months) {
        // Change the DatePicker value, which will trigger the listener
        // and refreshTimelineView()
        dateBox.setValue(dateBox.getValue().plusMonths(months).withDayOfMonth(1)); // Navigate by months, always set to
                                                                                   // the first day
    }

    // <editor-fold desc="Scrolling and Synchronization Logic">

    private void hideDefaultScrollBars() {
        ScrollBar verticalScrollBar = findScrollBar(timelineTable, Orientation.VERTICAL);
        if (verticalScrollBar != null) {
            verticalScrollBar.setVisible(false);
            verticalScrollBar.setManaged(false);
        }
        ScrollBar horizontalScrollBar = findScrollBar(timelineTable, Orientation.HORIZONTAL);
        if (horizontalScrollBar != null) {
            horizontalScrollBar.setVisible(false);
            horizontalScrollBar.setManaged(false);
        }
    }

    private void syncVerticalScrollBars(TableView<?> table1, TableView<?> table2) {
        ScrollBar scrollBar1 = findScrollBar(table1, Orientation.VERTICAL);
        ScrollBar scrollBar2 = findScrollBar(table2, Orientation.VERTICAL);

        if (scrollBar1 != null && scrollBar2 != null) {
            scrollBar1.valueProperty().bindBidirectional(scrollBar2.valueProperty());
        }
    }

    private void syncHorizontalScrollBar(TableView<?> tableView, ScrollPane externalScrollPane) {
        ScrollBar internalHBar = findScrollBar(tableView, Orientation.HORIZONTAL);
        if (internalHBar == null)
            return;

        externalScrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (isHorizontallySyncing)
                return;
            isHorizontallySyncing = true;
            internalHBar.setValue(newVal.doubleValue() * internalHBar.getMax());
            isHorizontallySyncing = false;
        });

        internalHBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isHorizontallySyncing)
                return;
            isHorizontallySyncing = true;
            if (internalHBar.getMax() > 0) { // Tránh chia cho 0
                externalScrollPane.setHvalue(newVal.doubleValue() / internalHBar.getMax());
            }
            isHorizontallySyncing = false;
        });
    }

    private void syncVerticalScrollBar(TableView<?> tableView, ScrollPane externalScrollPane) {
        ScrollBar internalVBar = findScrollBar(tableView, Orientation.VERTICAL);
        if (internalVBar == null)
            return;

        externalScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (isHorizontallySyncing)
                return;
            isHorizontallySyncing = true;
            internalVBar.setValue(newVal.doubleValue() * internalVBar.getMax());
            isHorizontallySyncing = false;
        });

        internalVBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isHorizontallySyncing)
                return;
            isHorizontallySyncing = true;
            if (internalVBar.getMax() > 0) { // Tránh chia cho 0
                externalScrollPane.setVvalue(newVal.doubleValue() / internalVBar.getMax());
            }
            isHorizontallySyncing = false;
        });
    }

    private void alignPanesWithTableHeader(TableView<?> tableView, Node... nodesToAlign) {
        Node header = tableView.lookup(".column-header-background");
        if (header != null) {
            Platform.runLater(() -> {
                double headerHeight = header.getLayoutBounds().getHeight() + 10; // Thêm 5px để tránh bị cắt
                for (Node node : nodesToAlign) {
                    StackPane.setMargin(node, new Insets(headerHeight, 0, 0, 0));
                }
            });
        }
    }

    private void updateOverlayWidth() {
        double totalWidth = calculateTotalTableWidth();
        overlayPane.setPrefWidth(totalWidth);
        overlayPane.setMinWidth(totalWidth);
        overlayPane.setMaxWidth(totalWidth);
    }

    private double calculateTotalTableWidth() {
        double totalWidth = 0;
        for (TableColumn<?, ?> column : timelineTable.getColumns()) {
            totalWidth += column.getWidth();
        }
        return totalWidth;
    }

    private ScrollBar findScrollBar(Node node, Orientation orientation) {
        Set<Node> allChildren = node.lookupAll(".scroll-bar");
        for (Node child : allChildren) {
            if (child instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) child;
                if (bar.getOrientation() == orientation) {
                    return bar;
                }
            }
        }
        return null;
    }

    private double snapToGrid(double value) {
        return Math.round(value) * (double) 1;
    }
}
