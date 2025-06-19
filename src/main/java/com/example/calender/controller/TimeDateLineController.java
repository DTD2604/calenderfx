package com.example.calender.controller;

import com.example.calender.config.ViewLoader;
import com.example.calender.controller.timeLine.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class TimeDateLineController extends ActionHandler implements Initializable {

    @FXML
    private DatePicker dpk_date;
    @FXML
    private Button btn_prev, btn_delete, btn_add, btn_update, btn_next, btn_day, btn_week, btn_month;
    @FXML
    private BorderPane bp_mainLayout;

    private final MonthViewController monthViewController = new MonthViewController();
    private final WeekViewController weekViewController = new WeekViewController();
    private final DayViewController dayViewController = new DayViewController();
    private boolean isControllerSet = false;
    private final static String TIMELINE = "/com/example/calender/timeLine/body_view.fxml";
    private LocalDate timelineStartDate = LocalDate.now();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEvents();
        configureControls();
        refreshTimelineView();
    }

    @Override
    protected void refreshTimelineView() {
        loadView();
    }

    /**
     * Cấu hình các điều khiển chính
     */
    private void configureControls() {
        dpk_date.setValue(timelineStartDate);
        btn_prev.setOnAction(e -> navigateDate(-1));
        btn_next.setOnAction(e -> navigateDate(1));
        btn_add.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            createEvent(today);
        });
        btn_update.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            viewEventDetail(today);
        });

        btn_delete.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            viewEventDetail(today);
        });

        dpk_date.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(timelineStartDate)) {
                LocalDate newStartDate;
                BaseTimeLineController currentController = (BaseTimeLineController) ViewLoader.getController(TIMELINE);

                if (currentController instanceof WeekViewController) {
                    // Tính ngày đầu tuần (thứ 2) từ ngày được chọn
                    newStartDate = newVal.minusDays(newVal.getDayOfWeek().getValue() - 1);
                } else {
                    newStartDate = newVal;
                }

                this.timelineStartDate = newStartDate;
                if (currentController != null) {
                    currentController.setTimelineStartDate(newStartDate);
                }
            }
        });

        // Add navigation for day/week/month buttons
        btn_day.setOnAction(e -> {
            roomList.clear();
            eventsList.clear();
            switchController(dayViewController);
            // Khi chuyển sang day view, sử dụng ngày hiện tại của dpk_date
            timelineStartDate = dpk_date.getValue();
            dayViewController.setTimelineStartDate(timelineStartDate);
        });

        btn_week.setOnAction(e -> {
            roomList.clear();
            eventsList.clear();
            // Khi chuyển sang week view, tính ngày đầu tuần từ ngày hiện tại của dpk_date
            LocalDate currentDate = dpk_date.getValue();
            timelineStartDate = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1);
            switchController(weekViewController);
            weekViewController.setTimelineStartDate(timelineStartDate);
        });

        btn_month.setOnAction(e -> {
            roomList.clear();
            eventsList.clear();
            switchController(monthViewController);
            // Khi chuyển sang month view, sử dụng ngày đầu tháng
            timelineStartDate = dpk_date.getValue().withDayOfMonth(1);
            monthViewController.setTimelineStartDate(timelineStartDate);
        });
    }

    private void loadView() {
        if (!isControllerSet) {
            switchController(monthViewController);
            isControllerSet = true;
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(TIMELINE));
                Node view = loader.load();
                bp_mainLayout.setCenter(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void switchController(BaseTimeLineController controller) {
        BaseTimeLineController loadedController = ViewLoader.loadView(TIMELINE, controller);
        bp_mainLayout.setCenter(loadedController.getRootNode());
    }

    // --- Date Navigation ---
    private void navigateDate(int days) {
        dpk_date.setValue(dpk_date.getValue().plusDays(days));
    }
}
