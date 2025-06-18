package com.example.calender;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
//         FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/calender/time_line.fxml"));
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/calender/time_line_day.fxml"));
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/calender/calendar_view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage.setTitle("JavaFX + Spring Integration");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
