package com.example.calender.controller;

import com.example.calender.entity.Events;
import com.example.calender.service.TimeLineByHoursService;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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

public class TimeLineByHoursController implements Initializable {

    // <editor-fold desc="Constants">
    private static final int HOURS_PER_DAY = 24;
    private static final int DAYS_VISIBLE = 3;
    private static final double CELL_WIDTH = 50.0;
    private static final double ROW_HEIGHT = 29.0;
    private static final double MINUTES_PER_PIXEL = 60.0 / CELL_WIDTH; // Số phút trên mỗi pixel
    private static final String HOVER_EVENT_COLOR = "#AA66CC";

    @FXML
    private TableView<Events> timelineTable;
    @FXML
    private TableView<Events> eventNameTable;
    @FXML
    private TableColumn<Events, String> nameEvent;
    @FXML
    private TableColumn<Events, Number> sttColumn;
    @FXML
    private DatePicker dateBox;
    @FXML
    private Button prevBtn, nextBtn, updateBtn, addBtn, deleteBtn;
    @FXML
    private Pane overlayPane;
    @FXML
    private ScrollPane scrollPane;

    private final ObservableList<Events> eventsList = FXCollections.observableArrayList();
    private LocalDate timelineStartDate;

    // Biến cờ để ngăn các vòng lặp cập nhật vô hạn khi đồng bộ hóa thanh cuộn
    private boolean isHorizontallySyncing = false;

    private final TimeLineByHoursService timeLineByHoursService = TimeLineByHoursService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.timelineStartDate = LocalDate.now();

        configureControls();
        loadEvents();
        setupTables();

        Platform.runLater(() -> {
            setupBindingsAndListeners();
            refreshTimelineView();
        });
    }

    private void loadEvents() {
        eventsList.setAll(timeLineByHoursService.getEventsByNextThreeDays(timelineStartDate));
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
            if (newVal != null && !newVal.equals(timelineStartDate)) {
                this.timelineStartDate = newVal;
                refreshTimelineView();
            }
        });
    }

    private void viewEventDetail(LocalDate today) {
        List<Events> events = timeLineByHoursService.getEventsByDate(today);
        if (events == null || events.isEmpty()) {
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
        ListView<Events> eventListView = new ListView<>(FXCollections.observableArrayList(events));
        eventListView.setCellFactory(param -> new ListCell<Events>() {
            @Override
            protected void updateItem(Events item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.getName() + " (" + item.getStartHour() + " - " + item.getEndHour() + ")");
            }
        });

        // Detail form
        TextField nameField = new TextField();
        DatePicker startDatePicker = new DatePicker();
        TextArea descriptionArea = new TextArea();
        ColorPicker colorPicker = new ColorPicker();
        Spinner<Integer> startHourSpinner = new Spinner<>(0, 23, 0);
        startHourSpinner.setPrefWidth(60);
        startHourSpinner.setEditable(true);
        Spinner<Integer> startMinuteSpinner = new Spinner<>(0, 59, 0);
        startMinuteSpinner.setPrefWidth(60);
        startMinuteSpinner.setEditable(true);
        Spinner<Integer> endHourSpinner = new Spinner<>(0, 23, 0);
        endHourSpinner.setPrefWidth(60);
        endHourSpinner.setEditable(true);
        Spinner<Integer> endMinuteSpinner = new Spinner<>(0, 59, 0);
        endMinuteSpinner.setPrefWidth(60);
        endMinuteSpinner.setEditable(true);

        GridPane detailPane = new GridPane();
        detailPane.setHgap(10);
        detailPane.setVgap(10);
        detailPane.addRow(0, new Label("Tên sự kiện:"), nameField);
        detailPane.addRow(1, new Label("Ngày bắt đầu:"), startDatePicker);
        detailPane.addRow(2, new Label("Mô tả:"), descriptionArea);
        detailPane.addRow(3, new Label("Màu sắc:"), colorPicker);
        detailPane.addRow(4, new Label("Giờ bắt đầu:"),
                new HBox(5, startHourSpinner, new Label(":"), startMinuteSpinner));
        detailPane.addRow(5, new Label("Giờ kết thúc:"), new HBox(5, endHourSpinner, new Label(":"), endMinuteSpinner));

        // Binding event selection
        // Disable Update/Delete when no selection
        Node updateBtnNode = dialog.getDialogPane().lookupButton(updateButton);
        Node deleteBtnNode = dialog.getDialogPane().lookupButton(deleteButton);

        Runnable updateUpdateButtonState = () -> {
            boolean sHValid = validateAndStyleSpinner(startHourSpinner, 0, 23);
            boolean sMValid = validateAndStyleSpinner(startMinuteSpinner, 0, 59);
            boolean eHValid = validateAndStyleSpinner(endHourSpinner, 0, 23);
            boolean eMValid = validateAndStyleSpinner(endMinuteSpinner, 0, 59);

            boolean allSpinnersAreIndividuallyValid = sHValid && sMValid && eHValid && eMValid;

            boolean isTimeOrderValid = true; // Assume valid until proven otherwise
            if (allSpinnersAreIndividuallyValid) { // Only check time order if individual values are valid
                LocalTime startTime = LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue());
                LocalTime endTime = LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue());

                if (endTime.isBefore(startTime)) {
                    isTimeOrderValid = false;
                    // Highlight the end time spinners in red
                    endHourSpinner.getEditor().setStyle("-fx-text-fill: red;");
                    endMinuteSpinner.getEditor().setStyle("-fx-text-fill: red;");
                } else {
                    // Reset style if it was previously red due to time order
                    endHourSpinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;");
                    endMinuteSpinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;");
                }
            }

            boolean disableSelection = eventListView.getSelectionModel().getSelectedItem() == null;

            updateBtnNode.setDisable(disableSelection || !(allSpinnersAreIndividuallyValid && isTimeOrderValid));
        };

        // Add listeners to spinners for validation in viewEventDetail
        startHourSpinner.getEditor().textProperty()
                .addListener((obs, oldText, newText) -> updateUpdateButtonState.run());
        startMinuteSpinner.getEditor().textProperty()
                .addListener((obs, oldText, newText) -> updateUpdateButtonState.run());
        endHourSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> updateUpdateButtonState.run());
        endMinuteSpinner.getEditor().textProperty()
                .addListener((obs, oldText, newText) -> updateUpdateButtonState.run());

        eventListView.getSelectionModel().selectedItemProperty().addListener((obs, oldEvent, newEvent) -> {
            if (newEvent != null) {
                nameField.setText(newEvent.getName());
                startDatePicker.setValue(LocalDate.parse(newEvent.getDate()));
                startHourSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getStartHour().split(":")[0]));
                startMinuteSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getStartHour().split(":")[1]));
                endHourSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getEndHour().split(":")[0]));
                endMinuteSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getEndHour().split(":")[1]));
                descriptionArea.setText(newEvent.getDescription());
                colorPicker.setValue(Color.web(newEvent.getColor()));

                // After setting values, update the button state
                updateUpdateButtonState.run();

            } else {
                nameField.clear();
                startDatePicker.setValue(null);
                descriptionArea.clear();
                colorPicker.setValue(Color.WHITE);
                startHourSpinner.getValueFactory().setValue(0);
                startMinuteSpinner.getValueFactory().setValue(0);
                endHourSpinner.getValueFactory().setValue(0);
                endMinuteSpinner.getValueFactory().setValue(0);

                // After clearing values, update the button state
                updateUpdateButtonState.run();
            }
            // Delete button disable logic:
            boolean disableSelection = newEvent == null;
            deleteBtnNode.setDisable(disableSelection);
        });

        // Initial update button state (e.g., when dialog opens with no initial
        // selection)
        updateUpdateButtonState.run();

        SplitPane splitPane = new SplitPane(eventListView, detailPane);
        splitPane.setDividerPositions(0.3);
        dialog.getDialogPane().setContent(splitPane);

        dialog.showAndWait().ifPresent(response -> {
            Events selectedEvent = eventListView.getSelectionModel().getSelectedItem();
            if (selectedEvent == null)
                return;

            if (response == updateButton) {
                // Validate input
                if (nameField.getText().isEmpty() || startDatePicker.getValue() == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Thiếu thông tin");
                    alert.setHeaderText(null);
                    alert.setContentText("Vui lòng nhập đầy đủ thông tin cần thiết.");
                    alert.showAndWait();
                    return;
                }

                // Final check for spinner validity before saving (redundant but safe)
                if (updateBtnNode.isDisable()) { // If the button is disabled, it means spinners are invalid or no
                                                 // selection
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Lỗi nhập liệu");
                    alert.setHeaderText(null);
                    alert.setContentText("Vui lòng kiểm tra lại giờ và phút nhập liệu.");
                    alert.showAndWait();
                    return;
                }
                // Update event
                // Calculate end date based on start and end time
                LocalDate eventStartDate = startDatePicker.getValue();
                LocalTime startTime = LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue());
                LocalTime endTime = LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue());

                LocalDate eventEndDate = eventStartDate;
                if (endTime.isBefore(startTime)) {
                    eventEndDate = eventStartDate.plusDays(1);
                }

                Events updatedEvent = Events.builder()
                        .name(nameField.getText())
                        .date(eventEndDate.toString())
                        .startHour(
                                String.format("%02d:%02d", startHourSpinner.getValue(), startMinuteSpinner.getValue()))
                        .endHour(String.format("%02d:%02d", endHourSpinner.getValue(), endMinuteSpinner.getValue()))
                        .description(descriptionArea.getText())
                        .color(FormatColor.toHexString(colorPicker.getValue()))
                        .build();

                timeLineByHoursService.updateEvent(selectedEvent, updatedEvent);
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
                        timeLineByHoursService.deleteEvent(selectedEvent);
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
        TextArea descriptionArea = new TextArea();
        ColorPicker colorPicker = new ColorPicker(Color.LIGHTBLUE);
        Spinner<Integer> startHourSpinner = new Spinner<>(0, 23, 0);
        startHourSpinner.setPrefWidth(70);
        startHourSpinner.setEditable(true);
        Spinner<Integer> startMinuteSpinner = new Spinner<>(0, 59, 0);
        startMinuteSpinner.setPrefWidth(70);
        startMinuteSpinner.setEditable(true);
        Spinner<Integer> endHourSpinner = new Spinner<>(0, 23, 0);
        endHourSpinner.setPrefWidth(70);
        endHourSpinner.setEditable(true);
        Spinner<Integer> endMinuteSpinner = new Spinner<>(0, 59, 0);
        endMinuteSpinner.setPrefWidth(70);
        endMinuteSpinner.setEditable(true);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Tên sự kiện:"), nameField);
        grid.addRow(1, new Label("Ngày bắt đầu:"), startDatePicker);
        grid.addRow(2, new Label("Mô tả:"), descriptionArea);
        grid.addRow(3, new Label("Màu sắc:"), colorPicker);
        grid.addRow(4, new Label("Giờ bắt đầu:"), new HBox(5, startHourSpinner, new Label(":"), startMinuteSpinner));
        grid.addRow(5, new Label("Giờ kết thúc:"), new HBox(5, endHourSpinner, new Label(":"), endMinuteSpinner));
        dialog.getDialogPane().setContent(grid);

        Node saveBtnNode = dialog.getDialogPane().lookupButton(saveButton);

        Runnable updateSaveButtonState = () -> {
            boolean sHValid = validateAndStyleSpinner(startHourSpinner, 0, 23);
            boolean sMValid = validateAndStyleSpinner(startMinuteSpinner, 0, 59);
            boolean eHValid = validateAndStyleSpinner(endHourSpinner, 0, 23);
            boolean eMValid = validateAndStyleSpinner(endMinuteSpinner, 0, 59);

            boolean allSpinnersAreIndividuallyValid = sHValid && sMValid && eHValid && eMValid;

            boolean isTimeOrderValid = true; // Assume valid until proven otherwise
            if (allSpinnersAreIndividuallyValid) { // Only check time order if individual values are valid
                LocalTime startTime = LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue());
                LocalTime endTime = LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue());

                if (endTime.isBefore(startTime)) {
                    isTimeOrderValid = false;
                    // Highlight the end time spinners in red
                    endHourSpinner.getEditor().setStyle("-fx-text-fill: red;");
                    endMinuteSpinner.getEditor().setStyle("-fx-text-fill: red;");
                } else {
                    // Reset style if it was previously red due to time order
                    endHourSpinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;");
                    endMinuteSpinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;");
                }
            }

            boolean overallValid = allSpinnersAreIndividuallyValid && isTimeOrderValid;
            saveBtnNode.setDisable(!overallValid);
        };

        // Add listeners to spinners for validation
        startHourSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> updateSaveButtonState.run());
        startMinuteSpinner.getEditor().textProperty()
                .addListener((obs, oldText, newText) -> updateSaveButtonState.run());
        endHourSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> updateSaveButtonState.run());
        endMinuteSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> updateSaveButtonState.run());

        // Initial validation check to set button state
        updateSaveButtonState.run();

        // Xử lý nút OK
        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButton) {
                // Validate đầu vào
                if (nameField.getText().trim().isEmpty()
                        || startDatePicker.getValue() == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Thiếu thông tin");
                    alert.setHeaderText(null);
                    alert.setContentText("Vui lòng nhập đầy đủ các trường bắt buộc.");
                    alert.showAndWait();
                    return;
                }

                // Final check for spinner validity before saving (redundant but safe)
                if (saveBtnNode.isDisable()) { // If the button is disabled, it means spinners are invalid
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Lỗi nhập liệu");
                    alert.setHeaderText(null);
                    alert.setContentText("Vui lòng kiểm tra lại giờ và phút nhập liệu.");
                    alert.showAndWait();
                    return;
                }

                // Calculate end date based on start and end time
                LocalDate endDate = startDatePicker.getValue();
                LocalTime startTime = LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue());
                LocalTime endTime = LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue());

                if (endTime.isBefore(startTime)) {
                    endDate = endDate.plusDays(1);
                }

                // Tạo sự kiện mới
                Events newEvent = Events.builder()
                        .name(nameField.getText())
                        .date(endDate.toString())
                        .startHour(
                                String.format("%02d:%02d", startHourSpinner.getValue(), startMinuteSpinner.getValue()))
                        .endHour(String.format("%02d:%02d", endHourSpinner.getValue(), endMinuteSpinner.getValue()))
                        .description(descriptionArea.getText())
                        .color(FormatColor.toHexString(colorPicker.getValue()))
                        .build();

                timeLineByHoursService.addEvent(newEvent);
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
        TableColumn<Events, String> parentColumn = new TableColumn<>("Events");
        parentColumn.getColumns().addAll(sttColumn, nameEvent);

        eventNameTable.getColumns().clear();
        eventNameTable.getColumns().add(parentColumn);
        eventNameTable.setItems(eventsList);

        // Set cell value factory for event name column
        nameEvent.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));

        sttColumn.setCellFactory(col -> new TableCell<Events, Number>() {
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

        for (int dayOffset = 0; dayOffset < DAYS_VISIBLE; dayOffset++) {
            LocalDate day = timelineStartDate.plusDays(dayOffset);
            String dayLabel = day.format(DateTimeFormatter.ofPattern("EEE dd.MM")); // Thêm thứ cho dễ nhìn

            TableColumn<Events, Void> dayColumn = new TableColumn<>(dayLabel);
            dayColumn.setSortable(false);

            for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
                String hourLabel = String.format("%02d:00", hour);
                TableColumn<Events, Void> hourColumn = new TableColumn<>(hourLabel);
                hourColumn.setPrefWidth(CELL_WIDTH);
                hourColumn.setSortable(false);
                dayColumn.getColumns().add(hourColumn);
            }
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
    }

    /**
     * Vẽ lại tất cả các sự kiện lên overlayPane.
     */
    private void drawEvents() {
        overlayPane.getChildren().clear();

        for (int i = 0; i < eventsList.size(); i++) {
            Events event = eventsList.get(i);
            LocalDate eventDate;
            try {
                eventDate = LocalDate.parse(event.getDate());
            } catch (Exception e) {
                System.err.println("Invalid date format for event: " + event.getName());
                continue; // Bỏ qua sự kiện có ngày không hợp lệ
            }

            long dayOffset = ChronoUnit.DAYS.between(timelineStartDate, eventDate);
            if (dayOffset < 0 || dayOffset >= DAYS_VISIBLE)
                continue;

            LocalTime startTime = LocalTime.parse(event.getStartHour());
            LocalTime endTime = LocalTime.parse(event.getEndHour());

            double startMinutes = startTime.getHour() * 60 + startTime.getMinute();
            double endMinutes = endTime.getHour() * 60 + endTime.getMinute();

            double x = (dayOffset * HOURS_PER_DAY * 60 + startMinutes) / MINUTES_PER_PIXEL;
            double width = (endMinutes - startMinutes) / MINUTES_PER_PIXEL;
            double y = i * ROW_HEIGHT;

            Pane eventPane = createEventPane(event, x, y, width);
            overlayPane.getChildren().add(eventPane);
        }
    }

    /**
     * Tạo một Pane đại diện cho một sự kiện.
     */
    private Pane createEventPane(Events event, double x, double y, double width) {
        Pane eventPane = new Pane();
        eventPane.setLayoutX(x);
        eventPane.setLayoutY(y);
        eventPane.setPrefWidth(width);
        eventPane.setPrefHeight(ROW_HEIGHT - 1); // Trừ 1 để có khoảng hở
        eventPane.setStyle("-fx-background-color: " + event.getColor() + "; -fx-background-radius: 6;");

        Tooltip.install(eventPane,
                new Tooltip(event.getName() + "\n" + event.getStartHour() + " - " + event.getEndHour()));

        setupEventPaneInteractions(eventPane, event);
        return eventPane;
    }

    /**
     * Thiết lập các tương tác (hover, kéo, thả, chỉnh độ rộng) cho một event pane.
     */
    private void setupEventPaneInteractions(Pane eventPane, Events event) {
        // Hiệu ứng Hover
        eventPane.setOnMouseEntered(e -> eventPane.setStyle("-fx-background-color: " + HOVER_EVENT_COLOR
                + "; -fx-background-radius: 6; -fx-border-color: white; -fx-border-width: 1.5;"));
        eventPane.setOnMouseExited(
                e -> eventPane.setStyle("-fx-background-color: " + event.getColor() + "; -fx-background-radius: 6;"));

        final double[] dragOffset = new double[2];
        final double[] initialWidth = new double[1];
        final double[] initialX = new double[1];
        final boolean[] resizingRight = { false };
        final boolean[] resizingLeft = { false };

        final double RESIZE_MARGIN = 8;

        eventPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
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
            } else if (e.getButton() == MouseButton.SECONDARY) {
                LocalDate eventDate = LocalDate.parse(event.getDate());
                viewEventDetail(eventDate);
            }
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
            updateEventFromPane(eventPane, event);
            eventPane.setCursor(Cursor.DEFAULT);
            e.consume();
        });
    }

    /**
     * Cập nhật thông tin (thời gian, ngày, vị trí) của đối tượng Events từ vị trí
     * của Pane.
     */
    private void updateEventFromPane(Pane eventPane, Events event) {
        double startX = eventPane.getLayoutX();
        double width = eventPane.getWidth();

        // Cập nhật thời gian
        int startTotalMinutes = (int) Math.round((startX * MINUTES_PER_PIXEL));
        int endTotalMinutes = (int) Math.round(((startX + width) * MINUTES_PER_PIXEL));

        int dayOffset = startTotalMinutes / (HOURS_PER_DAY * 60);
        int minutesInDay = startTotalMinutes % (HOURS_PER_DAY * 60);

        LocalTime newStart = LocalTime.of(minutesInDay / 60, minutesInDay % 60);
        LocalTime newEnd = newStart.plusMinutes(endTotalMinutes - startTotalMinutes);

        LocalDate newDate = timelineStartDate.plusDays(dayOffset);

        Events updatedEvent = Events.builder()
                .name(event.getName())
                .date(newDate.toString())
                .startHour(newStart.format(DateTimeFormatter.ofPattern("HH:mm")))
                .endHour(newEnd.format(DateTimeFormatter.ofPattern("HH:mm")))
                .color(event.getColor())
                .description(event.getDescription())
                .build();

        // Cập nhật dữ liệu qua TimeLineByHoursService
        timeLineByHoursService.updateEvent(event, updatedEvent);

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
    private void navigateDate(int days) {
        // Thay đổi giá trị của DatePicker sẽ kích hoạt listener,
        // listener này sẽ gọi refreshTimelineView()
        dateBox.setValue(dateBox.getValue().plusDays(days));
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
        double totalWidth = timelineTable.getColumns().stream()
                .mapToDouble(dayCol -> ((TableColumn<?, ?>) dayCol).getColumns().stream()
                        .mapToDouble(TableColumnBase::getWidth)
                        .sum())
                .sum();
        overlayPane.setPrefWidth(totalWidth);
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

    // Helper method for spinner validation and styling
    private boolean validateAndStyleSpinner(Spinner<Integer> spinner, int min, int max) {
        String text = spinner.getEditor().getText();
        try {
            int value = Integer.parseInt(text);
            if (value >= min && value <= max) {
                spinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;"); // Reset to default text color
                return true;
            } else {
                spinner.getEditor().setStyle("-fx-text-fill: red;"); // Set red color
                return false;
            }
        } catch (NumberFormatException e) {
            spinner.getEditor().setStyle("-fx-text-fill: red;"); // Invalid number format
            return false;
        }
    }
}
