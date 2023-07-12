package com.cahrypt.gsonutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A class that manages Gson instances and provides utility methods for serializing and deserializing objects.
 */
public final class GsonManager {
    private final Map<Object, Type> adapters;
    private final Supplier<GsonBuilder> builderSupplier;

    private Gson gson;

    /**
     * Creates a new GsonManager with the specified GsonBuilder supplier and caching set to true.
     * @param builderSupplier The GsonBuilder supplier.
     */
    public GsonManager(Supplier<GsonBuilder> builderSupplier) {
        this(true, builderSupplier);
    }

    /**
     * Creates a new GsonManager with a predefined GsonBuilder supplier.
     * @param cache whether to cache the Gson instance.
     */
    public GsonManager(boolean cache) {
        this(cache, () -> new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
        );
    }

    /**
     * Creates a new GsonManager with the specified GsonBuilder supplier.
     * @param cache whether to cache the Gson instance.
     * @param builderSupplier The GsonBuilder supplier.
     */
    public GsonManager(boolean cache, Supplier<GsonBuilder> builderSupplier) {
        this.adapters = new HashMap<>();
        this.builderSupplier = builderSupplier;

        if (cache) {
            cacheGson();
        }
    }

    /**
     * Creates a new GsonManager with a predefined GsonBuilder supplier and caching set to true.
     */
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
            throw new RuntimeException("Failed to create parent directories");
        }

        try {
            if (!file.createNewFile()) {
                throw new RuntimeException("Failed to create file " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }

    /**
     * Caches a new Gson instance with the current adapters.
     */
    public void cacheGson() {
        GsonBuilder builder = builderSupplier.get();

        for (Map.Entry<Object, Type> entry : adapters.entrySet()) {
            builder.registerTypeAdapter(entry.getValue(), entry.getKey());
        }

        gson = builder.create();
    }

    /**
     * Deserializes an object from a file.
     * @param type The type of the object.
     * @param fileName The name of the file.
     * @param <T> The type of the object.
     * @return The deserialized object.
     */
    public <T> T fromFile(Class<T> type, String fileName) {
        File file = getFile(fileName);

        if (file == null) {
            return null;
        }

        return fromFile(type, file);
    }

    /**
     * Deserializes an object from a file.
     * @param type The type of the object.
     * @param file The file.
     * @param <T> The type of the object.
     * @return The deserialized object.
     */
    public <T> T fromFile(Class<T> type, File file) {
        T object = null;

        try {
            object = gson.fromJson(new FileReader(file), type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return object;
    }

    /**
     * Deserializes an object from an API endpoint.
     * @param type The type of the object.
     * @param connection The connection to the endpoint.
     * @param <T> The type of the object.
     * @return The deserialized object.
     * @throws IOException If an I/O error occurs.
     */
    public <T> T fromEndpoint(Class<T> type, HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }

        return gson.fromJson(new InputStreamReader(connection.getInputStream()), type);
    }

    /**
     * Deserializes an object from an API endpoint.
     * @param type The type of the object.
     * @param endpointURL The URL of the endpoint.
     * @param <T> The type of the object.
     * @return The deserialized object.
     * @throws IOException If an I/O error occurs.
     */
    public <T> T fromEndpoint(Class<T> type, String endpointURL) throws IOException {
        return fromEndpoint(type, (HttpURLConnection) new URL(endpointURL).openConnection());
    }

    /**
     * Serializes an object to a file.
     * @param object The object.
     * @param type The type of the object.
     * @param fileName The name of the file to serialize to (will be created if non-existent).
     */
    public void toFile(Object object, Type type, String fileName) {
        File file = getFile(fileName);

        if (file == null) {
            throw new RuntimeException("Failed to create file " + fileName);
        }

        toFile(object, type, file);
    }

    /**
     * Serializes an object to a file.
     * @param object The object.
     * @param type The type of the object.
     * @param file The file to serialize to (expected to exist).
     */
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

    /**
     * Adds an adapter to the GsonManager.
     * @param type The class type of the adapter.
     * @param adapter The adapter.
     * @param <T> The type of the adapter.
     */
    public <T> void addAdapter(Class<T> type, JsonAdapter<T> adapter) {
        adapters.put(adapter, type);
    }

    /**
     * Adds an adapter to the GsonManager.
     * @param type The type of the adapter.
     * @param adapter The adapter.
     */
    public void addAdapter(Type type, Object adapter) {
        adapters.put(adapter, type);
    }

    /**
     * Removes an adapter from the GsonManager.
     * @param adapter The adapter.
     */
    public void removeAdapter(Object adapter) {
        adapters.remove(adapter);
    }

    /**
     * Gets the Gson instance.
     * @return The Gson instance.
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Gets a snapshot of the adapters.
     * @return The adapters.
     */
    public Set<Object> getAdapters() {
        return new HashSet<>(adapters.keySet());
    }
}
