package com.example.calender.controller.timeLine;

import com.example.calender.models.BookRoom;
import com.example.calender.models.Room;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Arrays;

public abstract class BaseTimeLineController extends ActionHandler {

    @FXML
    protected TableView<BookRoom> tbl_timeline;
    @FXML
    protected TableView<Room> tbl_eventName;
    @FXML
    protected TableColumn<Room, String> col_nameEvent;
    @FXML
    protected TableColumn<Room, Number> col_stt;
    @FXML
    protected Pane ap_overlay;
    @FXML
    protected ScrollPane scr_main;

    @Setter
    @Getter
    protected Node rootNode;

    public void setTimelineStartDate(LocalDate date) {
        this.timelineStartDate = date;
        PauseTransition pause = new PauseTransition(Duration.millis(50));
        pause.setOnFinished(e -> refreshTimelineView());
        pause.play();
    }

    @Override
    protected void refreshTimelineView() {
        super.loadEvents(); // Luôn load lại events khi refresh
        setupTimelineColumns();
        drawEvents();
        tbl_eventName.refresh();
        tbl_timeline.refresh();
    }

    protected void setupTables() {
        setupNameTable();
        setupTimelineColumns();
    }

    private void setupNameTable() {
        tbl_eventName.getColumns().clear();

        col_nameEvent.setText("Phòng");
        col_nameEvent.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomName()));
        tbl_eventName.getColumns().addAll(Arrays.asList(col_stt, col_nameEvent));
        // Hiển thị danh sách phòng
        tbl_eventName.setItems(FXCollections.observableArrayList(roomList));
        col_stt.setCellFactory(col -> new TableCell<Room, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });
        col_stt.setStyle("-fx-alignment: CENTER; -fx-background-color: #e9e9e9; -fx-border-color: #c8c8c8;");
    }

    protected void setupBindingsAndListeners() {
        // Đảm bảo overlayPane và scrollPane luôn nằm dưới header của bảng
        alignPanesWithTableHeader(tbl_timeline, scr_main, ap_overlay);

        // Ẩn thanh cuộn mặc định của các TableView
        hideDefaultScrollBars(tbl_timeline);

        // Đồng bộ hóa thanh cuộn dọc giữa hai bảng
        syncVerticalScrollBars(tbl_eventName, tbl_timeline);

        // Đồng bộ hóa thanh cuộn ngang của timelineTable với scrollPane tùy chỉnh
        syncHorizontalScrollBar(tbl_timeline, scr_main);

        // Đồng bộ hóa thanh cuộn dọc của overlayPane với scrollPane
        syncVerticalScrollBar(tbl_timeline, scr_main);
    }

    public abstract void setupTimelineColumns();

    public abstract void drawEvents();
}
