package com.example.calender.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonFileManager {

    private static final Map<String, JsonFileManager> INSTANCES = new HashMap<>();
    private static final String FILE_PATH = "./src/main/resources/data/";
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>)
                    (json, typeOfT, context) -> LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>)
                    (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
            .create();

    private List<?> eventList;

    @SuppressWarnings("unchecked")
    public <T> List<T> getEventList(Class<T> type) {
        return (List<T>) eventList;
    }

    @Getter
    private String filePath;
    private JsonFileManager(String fileName) {
        this.filePath = FILE_PATH + fileName;
    }
    public static synchronized JsonFileManager getInstance(String fileName) {
        return INSTANCES.computeIfAbsent(fileName, JsonFileManager::new);
    }

    public void setFilePath(String fileName) {
        this.filePath = FILE_PATH + fileName;
    }

    // List all JSON files in the data directory
    public List<String> listAvailableFiles() {
        File dataDir = new File("./src/main/resources/data");
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        List<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                fileNames.add(file.getName());
            }
        }
        return fileNames;
    }

    // Load data from the currently set file with a generic type
    public <T> void loadFromFile(Class<T> type) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                eventList = new ArrayList<T>();
                return;
            }

            try (Reader reader = new FileReader(file)) {
                Type listType = TypeToken.getParameterized(List.class, type).getType();
                eventList = gson.fromJson(reader, listType);
            }
        } catch (IOException e) {
            e.printStackTrace();
            eventList = new ArrayList<T>();
        }
    }


    // Save data to the currently set file
    public void saveToFile() {
        try {
            File file = new File(filePath);

            // Tạo thư mục cha nếu chưa tồn tại
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Ghi file (nếu chưa có sẽ tự tạo file mới)
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(eventList, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
