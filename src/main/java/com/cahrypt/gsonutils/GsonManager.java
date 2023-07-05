package com.cahrypt.gsonutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GsonManager {
    private final Map<Object, Type> adapters;
    private Gson gson;

    public GsonManager(boolean cache) {
        this.adapters = new HashMap<>();

        if (cache) {
            cacheGson();
        }
    }

    public GsonManager() {
        this(true);
    }

    private File getFile(String fileName) {
        File file = new File(fileName);

        if (file.exists()) {
            return file;
        }

        File parent = file.getParentFile();

        if (!parent.exists() && !file.getParentFile().mkdirs()) {
            System.out.println("Failed to create parent directories");
            return null;
        }

        try {
            if (!file.createNewFile()) {
                System.out.println("Failed to create file " + fileName);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }

    public void cacheGson() {
        GsonBuilder builder = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);

        for (Map.Entry<Object, Type> entry : adapters.entrySet()) {
            builder.registerTypeAdapter(entry.getValue(), entry.getKey());
        }

        gson = builder.create();
    }

    public <T> T fromFile(Class<T> type, String fileName) {
        File file = getFile(fileName);

        if (file == null) {
            return null;
        }

        return fromFile(type, file);
    }

    public <T> T fromFile(Class<T> type, File file) {
        T object = null;

        try {
            object = gson.fromJson(new FileReader(file), type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return object;
    }

    public void toFile(Object object, Type type, String fileName) {
        File file = getFile(fileName);

        if (file == null) {
            System.out.println("Failed to create file");
            return;
        }

        toFile(object, type, file);
    }

    public void toFile(Object object, Type type, File file) {
        try {
            FileWriter writer = new FileWriter(file);

            String jsonString = gson.toJson(object, type);

            writer.write(jsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T> void addAdapter(Class<T> type, JsonAdapter<T> adapter) {
        adapters.put(adapter, type);
    }

    public void addAdapter(Type type, Object adapter) {
        adapters.put(adapter, type);
    }

    public void removeAdapter(Object adapter) {
        adapters.remove(adapter);
    }

    public Gson getGson() {
        return gson;
    }

    public Set<Object> getAdapters() {
        return new HashSet<>(adapters.keySet());
    }
}
