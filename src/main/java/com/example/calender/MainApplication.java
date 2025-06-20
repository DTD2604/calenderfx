package com.example.calender;

import com.vvg.pos.api.Connector;
import com.vvg.pos.result.LoginResult;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Connector.getInstance().setRootUrl(ClientConfig.getInstance().getParam(ClientConfig.PARAM_ROOT_URL));
        Connector.getInstance().connect("mc_thang", "123456", "VN");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/calender/timeLine/time_line_day.fxml"));
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
