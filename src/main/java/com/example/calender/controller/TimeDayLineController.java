package com.example.calender.controller;

import com.example.calender.models.BookRoom;
import com.example.calender.models.Room;
import com.example.calender.service.RoomService;
import com.example.calender.service.TimeLineByDayService;
import com.example.calender.utils.FormatColor;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class TimeDayLineController implements Initializable {

    // <editor-fold desc="Constants">
    private static final int HOURS_PER_DAY = 24;
    private static final double CELL_WIDTH = 200.0;
    private static final double ROW_HEIGHT = 25.0;
    private static final String HOVER_EVENT_COLOR = "#AA66CC";

    @FXML
    private TableView<BookRoom> timelineTable;
    @FXML
    private TableView<Room> eventNameTable;
    @FXML
    private TableColumn<Room, String> nameEvent;
    @FXML
    private TableColumn<Room, Number> sttColumn;
    @FXML
    private DatePicker dateBox;
    @FXML
    private Button prevBtn, deleteBtn, addBtn, updateBtn, nextBtn, dayBtn, weekBtn, monthBtn;
    @FXML
    private Pane overlayPane;
    @FXML
    private ScrollPane scrollPane;

    private final ObservableList<BookRoom> eventsList = FXCollections.observableArrayList();
    private final ObservableList<Room> roomList = FXCollections.observableArrayList();
    private LocalDate timelineStartDate = LocalDate.now();

    // Prevent infinite update loops when syncing scrollbars
    private boolean isHorizontallySyncing = false;

    private final TimeLineByDayService timeLineByDayService = TimeLineByDayService.getInstance();
    private final RoomService roomService = RoomService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureControls();
        loadEvents();
        setupTables();

        Platform.runLater(() -> {
            setupBindingsAndListeners();
            refreshTimelineView();
        });
    }

    private void loadEvents() {
        eventsList.setAll(timeLineByDayService.getAllEvents());
        roomList.setAll(roomService.getAllRooms());
    }

    /**
     * Cấu hình các điều khiển chính
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
        List<BookRoom> events = timeLineByDayService.getEventsByDate(today);
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
        ButtonType closeButton = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType updateButton = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButton = new ButtonType("Xóa", ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(updateButton, deleteButton, closeButton);
        ListView<BookRoom> eventListView = new ListView<>(FXCollections.observableArrayList(events));
        eventListView.setCellFactory(param -> new ListCell<BookRoom>() {
            @Override
            protected void updateItem(BookRoom item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.getRoomName() + " - " + item.getFullName() + " (" + item.getStartDate() + " - " + item.getEndDate() + ")");
            }
        });
        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        TextArea purposeArea = new TextArea();
        ComboBox<String> statusComboBox = new ComboBox<>(FXCollections.observableArrayList("pending", "approved", "rejected"));
        statusComboBox.getSelectionModel().select("pending");
        ComboBox<String> roomComboBox = new ComboBox<>(FXCollections.observableArrayList(roomList.stream()
                .map(Room::getRoomName)
                .collect(Collectors.toList())));
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
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
        detailPane.addRow(1, new Label("Phòng:"), roomComboBox);
        detailPane.addRow(2, new Label("Ngày bắt đầu:"), startDatePicker);
        detailPane.addRow(2, new Label("Ngày kết thúc:"), endDatePicker);
        detailPane.addRow(1, new Label("Email:"), emailField);
        detailPane.addRow(2, new Label("Số điện thoại:"), phoneField);
        detailPane.addRow(3, new Label("Mô tả:"), purposeArea);
        detailPane.addRow(4, new Label("Màu sắc:"), colorPicker);
        detailPane.addRow(6, new Label("Trạng thái:"), statusComboBox);
        detailPane.addRow(5, new Label("Giờ bắt đầu:"), new HBox(5, startHourSpinner, new Label(":"), startMinuteSpinner));
        detailPane.addRow(6, new Label("Giờ kết thúc:"), new HBox(5, endHourSpinner, new Label(":"), endMinuteSpinner));

        Node deleteBtnNode = dialog.getDialogPane().lookupButton(deleteButton);
        Node updateBtnNode = dialog.getDialogPane().lookupButton(updateButton);

        Runnable updateUpdateButtonState = () -> {
            boolean sHValid = validateAndStyleSpinner(startHourSpinner, 23);
            boolean sMValid = validateAndStyleSpinner(startMinuteSpinner, 59);
            boolean eHValid = validateAndStyleSpinner(endHourSpinner, 23);
            boolean eMValid = validateAndStyleSpinner(endMinuteSpinner, 59);

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
                nameField.setText(newEvent.getFullName());
                roomComboBox.setValue(newEvent.getRoomName());
                emailField.setText(newEvent.getEmail());
                phoneField.setText(newEvent.getPhoneNumber());
                startDatePicker.setValue(LocalDate.parse(newEvent.getStartDate()));
                endDatePicker.setValue(LocalDate.parse(newEvent.getEndDate()));
                startDatePicker.setValue(LocalDate.parse(newEvent.getStartDate()));
                startHourSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getStartTime().split(":")[0]));
                startMinuteSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getStartTime().split(":")[1]));
                endHourSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getEndTime().split(":")[0]));
                endMinuteSpinner.getValueFactory().setValue(Integer.parseInt(newEvent.getEndTime().split(":")[1]));
                purposeArea.setText(newEvent.getPurpose());
                colorPicker.setValue(Color.web(newEvent.getColor()));

                // After setting values, update the button state
                updateUpdateButtonState.run();

            } else {
                nameField.clear();
                roomComboBox.getSelectionModel().clearSelection();
                emailField.clear();
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                phoneField.clear();
                colorPicker.setValue(Color.WHITE);
                purposeArea.clear();
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
            BookRoom selectedEvent = eventListView.getSelectionModel().getSelectedItem();
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
                LocalDate eventEndDate = endDatePicker.getValue() != startDatePicker.getValue() ? endDatePicker.getValue() : eventStartDate;

                BookRoom updatedEvent = BookRoom.builder()
                        .fullName(nameField.getText())
                        .roomName(roomComboBox.getValue().replaceAll("P", ""))
                        .startDate(eventStartDate.toString())
                        .email(emailField.getText())
                        .phoneNumber(phoneField.getText())
                        .status(statusComboBox.getValue())
                        .endDate(eventEndDate.toString())
                        .startTime(String.format("%02d:%02d", startHourSpinner.getValue(), startMinuteSpinner.getValue()))
                        .endTime(String.format("%02d:%02d", endHourSpinner.getValue(), endMinuteSpinner.getValue()))
                        .purpose(purposeArea.getText())
                        .color(FormatColor.toHexString(colorPicker.getValue()))
                        .build();

                if (isValidTime(updatedEvent)) {
                    // Cập nhật qua service
                    timeLineByDayService.updateEvent(selectedEvent, updatedEvent);
                }
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
                        timeLineByDayService.deleteEvent(selectedEvent);
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
        TextField emailField = new TextField();
        TextField phoneField = new TextField();

        DatePicker startDatePicker = new DatePicker(today);
        DatePicker endDatePicker = new DatePicker(today);
        TextArea purposeArea = new TextArea();
        ColorPicker colorPicker = new ColorPicker(Color.LIGHTBLUE);

        ComboBox<String> roomComboBox = new ComboBox<>(FXCollections.observableArrayList(
                roomList.stream().map(Room::getRoomName).collect(Collectors.toList())));
        roomComboBox.getSelectionModel().selectFirst();

        ComboBox<String> statusComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "pending", "approved", "rejected"));
        statusComboBox.getSelectionModel().select("pending");

        Spinner<Integer> startHourSpinner = new Spinner<>(0, 23, 0);
        Spinner<Integer> startMinuteSpinner = new Spinner<>(0, 59, 0);
        Spinner<Integer> endHourSpinner = new Spinner<>(0, 23, 0);
        Spinner<Integer> endMinuteSpinner = new Spinner<>(0, 59, 0);
        Arrays.asList(startHourSpinner, startMinuteSpinner, endHourSpinner, endMinuteSpinner).forEach(spinner -> {
            spinner.setPrefWidth(70);
            spinner.setEditable(true);
        });

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Tên sự kiện:"), nameField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("Số điện thoại:"), phoneField);
        grid.addRow(3, new Label("Phòng:"), roomComboBox);
        grid.addRow(4, new Label("Ngày bắt đầu:"), startDatePicker);
        grid.addRow(4, new Label("Ngày kết thúc:"), endDatePicker);
        grid.addRow(5, new Label("Mô tả:"), purposeArea);
        grid.addRow(6, new Label("Màu sắc:"), colorPicker);
        grid.addRow(7, new Label("Giờ bắt đầu:"), new HBox(5, startHourSpinner, new Label(":"), startMinuteSpinner));
        grid.addRow(8, new Label("Giờ kết thúc:"), new HBox(5, endHourSpinner, new Label(":"), endMinuteSpinner));
        grid.addRow(9, new Label("Trạng thái:"), statusComboBox);

        dialog.getDialogPane().setContent(grid);

        Node saveBtnNode = dialog.getDialogPane().lookupButton(saveButton);

        Runnable updateSaveButtonState = () -> {
            boolean sHValid = validateAndStyleSpinner(startHourSpinner, 23);
            boolean sMValid = validateAndStyleSpinner(startMinuteSpinner, 59);
            boolean eHValid = validateAndStyleSpinner(endHourSpinner, 23);
            boolean eMValid = validateAndStyleSpinner(endMinuteSpinner, 59);
            boolean allSpinnersAreValid = sHValid && sMValid && eHValid && eMValid;

            boolean isTimeOrderValid = true;
            if (allSpinnersAreValid) {
                LocalTime startTime = LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue());
                LocalTime endTime = LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue());
                if (endTime.isBefore(startTime)) {
                    isTimeOrderValid = false;
                    endHourSpinner.getEditor().setStyle("-fx-text-fill: red;");
                    endMinuteSpinner.getEditor().setStyle("-fx-text-fill: red;");
                } else {
                    endHourSpinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;");
                    endMinuteSpinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;");
                }
            }

            saveBtnNode.setDisable(!(allSpinnersAreValid && isTimeOrderValid));
        };

        // Add listeners
        Arrays.asList(startHourSpinner.getEditor().textProperty(),
                        startMinuteSpinner.getEditor().textProperty(),
                        endHourSpinner.getEditor().textProperty(),
                        endMinuteSpinner.getEditor().textProperty())
                .forEach(prop -> prop.addListener((obs, oldVal, newVal) -> updateSaveButtonState.run()));

        updateSaveButtonState.run();

        dialog.showAndWait().ifPresent(response -> {
            // Đồng bộ giá trị nhập tay vào valueFactory trước khi lấy value
            try {
                startHourSpinner.getValueFactory().setValue(Integer.parseInt(startHourSpinner.getEditor().getText()));
                startMinuteSpinner.getValueFactory().setValue(Integer.parseInt(startMinuteSpinner.getEditor().getText()));
                endHourSpinner.getValueFactory().setValue(Integer.parseInt(endHourSpinner.getEditor().getText()));
                endMinuteSpinner.getValueFactory().setValue(Integer.parseInt(endMinuteSpinner.getEditor().getText()));
            } catch (NumberFormatException ignored) {}
            if (response == saveButton) {
                if (nameField.getText().trim().isEmpty() || startDatePicker.getValue() == null) {
                    new Alert(Alert.AlertType.WARNING, "Vui lòng nhập đầy đủ các trường bắt buộc.").showAndWait();
                    return;
                }

                if (saveBtnNode.isDisable()) {
                    new Alert(Alert.AlertType.WARNING, "Vui lòng kiểm tra lại giờ và phút nhập liệu.").showAndWait();
                    return;
                }

                BookRoom newEvent = BookRoom.builder()
                        .fullName(nameField.getText())
                        .email(emailField.getText())
                        .phoneNumber(phoneField.getText())
                        .roomName(roomComboBox.getValue())
                        .startDate(startDatePicker.getValue().toString())
                        .endDate(endDatePicker.getValue().toString())
                        .startTime(String.format("%02d:%02d", startHourSpinner.getValue(), startMinuteSpinner.getValue()))
                        .endTime(String.format("%02d:%02d", endHourSpinner.getValue(), endMinuteSpinner.getValue()))
                        .purpose(purposeArea.getText())
                        .color(FormatColor.toHexString(colorPicker.getValue()))
                        .status(statusComboBox.getValue())
                        .build();

                if (isValidTime(newEvent)) {
                    timeLineByDayService.addEvent(newEvent);
                }
                refreshTimelineView();

                new Alert(Alert.AlertType.INFORMATION, "Sự kiện mới đã được thêm vào!").showAndWait();
            }
        });
    }

    /**
     *
     */
    private void setupTables() {
        setupNameTable();
        setupTimelineColumns();
    }

    private void setupNameTable() {
        eventNameTable.getColumns().clear();
        nameEvent.setText("Phòng");
        nameEvent.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomName()));
        eventNameTable.getColumns().addAll(Arrays.asList(sttColumn, nameEvent));
        // Hiển thị danh sách phòng
        eventNameTable.setItems(FXCollections.observableArrayList(roomList));
        sttColumn.setCellFactory(col -> new TableCell<Room, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });
        sttColumn.setStyle("-fx-alignment: CENTER; -fx-background-color: #e9e9e9; -fx-border-color: #c8c8c8;");
    }

    private void setupTimelineColumns() {
        timelineTable.getColumns().clear();
        timelineTable.getStyleClass().add("right-border-bold");

        int daysInMonth = timelineStartDate.lengthOfMonth();

        LocalDate firstDayOfMonth = timelineStartDate.withDayOfMonth(1);
        for (int dayOffset = 0; dayOffset < daysInMonth; dayOffset++) {

            LocalDate day = firstDayOfMonth.plusDays(dayOffset);
            String dayLabel = day.format(DateTimeFormatter.ofPattern("dd.MM"));

            TableColumn<BookRoom, Void> dayColumn = new TableColumn<>(dayLabel);
            dayColumn.setPrefWidth(CELL_WIDTH);

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
    }

    // --- Event Drawing ---
    private void drawEvents() {
        overlayPane.getChildren().clear();
        LocalDate firstDayOfMonth = timelineStartDate.withDayOfMonth(1);
        Map<String, Integer> roomIndexMap = new HashMap<>();
        for (int i = 0; i < roomList.size(); i++) {
            roomIndexMap.put(roomList.get(i).getRoomName(), i);
        }
        for (BookRoom event : eventsList) {
            try {
                Integer roomIndex = roomIndexMap.get(event.getRoomName());
                if (roomIndex == null) continue;
                LocalDateTime startDateTime = LocalDateTime.of(LocalDate.parse(event.getStartDate()), LocalTime.parse(event.getStartTime()));
                LocalDateTime endDateTime = LocalDateTime.of(LocalDate.parse(event.getEndDate()), LocalTime.parse(event.getEndTime()));
                long dayOffset = ChronoUnit.DAYS.between(firstDayOfMonth, startDateTime.toLocalDate());
                long endDayOffset = ChronoUnit.DAYS.between(firstDayOfMonth, endDateTime.toLocalDate());
                if (dayOffset < 0) continue;
                double startMinutes = startDateTime.getHour() * 60 + startDateTime.getMinute();
                double endMinutes = (endDayOffset - dayOffset) * HOURS_PER_DAY * 60 + endDateTime.getHour() * 60 + endDateTime.getMinute();
                double x = dayOffset * CELL_WIDTH + (startMinutes / (HOURS_PER_DAY * 60)) * CELL_WIDTH;
                double width = (endMinutes - startMinutes) / (HOURS_PER_DAY * 60) * CELL_WIDTH;
                double y = roomIndex * (ROW_HEIGHT - 1);
                Pane eventPane = createEventPane(event, x, y, width);
                overlayPane.getChildren().add(eventPane);
            } catch (Exception e) {
                System.err.println("Invalid date/time format for event: " + event.getFullName());
            }
        }
    }

    private Pane createEventPane(BookRoom event, double x, double y, double width) {
        Pane eventPane = new Pane();
        eventPane.setLayoutX(x);
        eventPane.setLayoutY(y);
        eventPane.setPrefWidth(width);
        eventPane.setPrefHeight(ROW_HEIGHT);
        eventPane.setStyle("-fx-background-color: " + event.getColor() + "; -fx-background-radius: 6;");
        Tooltip.install(eventPane, new Tooltip(event.getFullName() + "\n" + event.getStartTime() + " - " + event.getEndTime()));
        setupEventPaneInteractions(eventPane, event);
        return eventPane;
    }

    // --- Event Pane Interactions ---
    private void setupEventPaneInteractions(Pane eventPane, BookRoom event) {
        // Hiệu ứng Hover
        eventPane.setOnMouseEntered(e -> eventPane.setStyle("-fx-background-color: " + HOVER_EVENT_COLOR + "; -fx-background-radius: 6; -fx-border-color: white; -fx-border-width: 1.5;"));
        eventPane.setOnMouseExited(e -> eventPane.setStyle("-fx-background-color: " + event.getColor() + "; -fx-background-radius: 6;"));

        final double[] dragOffset = new double[2];
        final double[] initialWidth = new double[1];
        final double[] initialX = new double[1];
        final boolean[] resizingRight = {false};
        final boolean[] resizingLeft = {false};

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
                viewEventDetail(LocalDate.parse(event.getStartDate()));
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

    // --- Event Update from Pane ---
    private void updateEventFromPane(Pane eventPane, BookRoom event) {
        double startX = eventPane.getLayoutX();
        double width = eventPane.getWidth();
        double endX = startX + width;

        // Xác định roomName mới dựa trên vị trí Y của eventPane
        int newRowIndex = (int) Math.round(eventPane.getLayoutY() / ROW_HEIGHT);
        String newRoomName = (newRowIndex >= 0 && newRowIndex < roomList.size()) ? roomList.get(newRowIndex).getRoomName() : event.getRoomName();

        // Tính offset ngày dựa theo firstDayOfMonth giống drawEvents
        LocalDate firstDayOfMonth = timelineStartDate.withDayOfMonth(1);
        int startDayOffset = (int) (startX / CELL_WIDTH);
        int endDayOffset = (int) (endX / CELL_WIDTH);
        LocalDate newStartDate = firstDayOfMonth.plusDays(startDayOffset);
        LocalDate newEndDate = firstDayOfMonth.plusDays(endDayOffset);

        // Tính thời gian trong ngày (theo phút)
        double xInDay = startX % CELL_WIDTH;
        double startMinutesInDay = (xInDay / CELL_WIDTH) * (HOURS_PER_DAY * 60);
        double durationMinutes = (width / CELL_WIDTH) * (HOURS_PER_DAY * 60);
        double endMinutesInDay = startMinutesInDay + durationMinutes;

        // Chuyển sang LocalTime
        LocalTime newStart = LocalTime.of((int) (startMinutesInDay / 60), (int) (startMinutesInDay % 60));
        LocalTime newEnd = LocalTime.of((int) (endMinutesInDay / 60), (int) (endMinutesInDay % 60));

        // Tạo BookRoom mới với roomName mới
        BookRoom updatedEvent = BookRoom.builder()
                .fullName(event.getFullName())
                .email(event.getEmail())
                .status(event.getStatus())
                .phoneNumber(event.getPhoneNumber())
                .roomName(newRoomName)
                .color(event.getColor())
                .purpose(event.getPurpose())
                .startDate(newStartDate.toString())
                .endDate(newEndDate.toString())
                .startTime(newStart.format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(newEnd.format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();

        if (isValidTime(updatedEvent)) {
            timeLineByDayService.updateEvent(event, updatedEvent);
        }
        refreshTimelineView();
    }

    // --- Timeline Refresh ---
    private void refreshTimelineView() {
        loadEvents();
        setupTimelineColumns();
        drawEvents();
        eventNameTable.refresh();
        timelineTable.refresh();
    }

    // --- Date Navigation ---
    private void navigateDate(int days) {
        dateBox.setValue(dateBox.getValue().plusDays(days));
    }

    // ======================
    // UI ScrollBar Utilities
    // ======================
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
                double headerHeight = header.getLayoutBounds().getHeight();
                for (Node node : nodesToAlign) {
                    StackPane.setMargin(node, new Insets(headerHeight, 0, 0, 0));
                }
            });
        }
    }

    private void updateOverlayWidth() {
        double totalWidth = timelineTable.getColumns().stream()
                .mapToDouble(TableColumnBase::getWidth)
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

    // ======================
    // Validation Utilities
    // ======================
    // Helper method for spinner validation and styling
    private boolean validateAndStyleSpinner(Spinner<Integer> spinner, int max) {
        String text = spinner.getEditor().getText();
        try {
            int value = Integer.parseInt(text);
            if (value >= 0 && value <= max) {
                spinner.getEditor().setStyle("-fx-text-fill: -fx-text-inner-color;");
                return true;
            } else {
                spinner.getEditor().setStyle("-fx-text-fill: red;");
                return false;
            }
        } catch (NumberFormatException e) {
            spinner.getEditor().setStyle("-fx-text-fill: red;");
            return false;
        }
    }

    // ======================
    // Event Validation
    // ======================
    private boolean isValidTime(BookRoom event) {
        LocalDateTime newStartDateTime = LocalDateTime.of(LocalDate.parse(event.getStartDate()), LocalTime.parse(event.getStartTime()));
        LocalDateTime newEndDateTime = LocalDateTime.of(LocalDate.parse(event.getEndDate()), LocalTime.parse(event.getEndTime()));

        List<BookRoom> overlappingEvents = timeLineByDayService.getAllEventsByRoomName(event.getRoomName());
        for (BookRoom existingEvent : overlappingEvents) {
            if (Objects.equals(existingEvent.getEmail(), event.getEmail())) continue;

            LocalDateTime existingStartDateTime = LocalDateTime.of(
                    LocalDate.parse(existingEvent.getStartDate()),
                    LocalTime.parse(existingEvent.getStartTime())
            );
            LocalDateTime existingEndDateTime = LocalDateTime.of(
                    LocalDate.parse(existingEvent.getEndDate()),
                    LocalTime.parse(existingEvent.getEndTime())
            );

            if (newStartDateTime.isBefore(existingEndDateTime) && existingStartDateTime.isBefore(newEndDateTime)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Lỗi trùng lặp sự kiện");
                alert.setHeaderText(null);
                alert.setContentText("Sự kiện này đã tồn tại trong khoảng thời gian đã chọn.");
                alert.showAndWait();
                return false;
            }
        }
        return true;
    }
}
