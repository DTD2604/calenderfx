package com.example.calender.config;

import com.example.calender.controller.timeLine.BaseTimeLineController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;

public class ViewLoader {
    private static final Map<String, Object> controllerCache = new HashMap<>();

    public static <T> T loadView(String fxmlPath, T controller) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewLoader.class.getResource(fxmlPath));
            loader.setController(controller);
            Node view = loader.load();
            controllerCache.put(fxmlPath, controller);

            // Gán rootNode vào controller để trả view cho TimeDateLineController
            if (controller instanceof BaseTimeLineController) {
                ((BaseTimeLineController) controller).setRootNode(view);
            }

            return controller;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getController(String fxmlPath) {
        return controllerCache.get(fxmlPath);
    }
}

