package context;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;

import app.structures.EventListener;
import app.utils.JSONUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public abstract class AbstractApplicationContext <Parameter extends Enum<?>, Event extends Enum<?>> {
    private File configurationFile;

    // Used String type as a Key due to the problem of JSON Deserialization of Generic Types
    private Map<String, String> storage;
    private Map<Event, Set<EventListener>> eventListeners;

    private Stage stage;

    public AbstractApplicationContext(String configFileName) {
        configurationFile = new File(configFileName);
        storage = new HashMap<>();
        eventListeners = new HashMap<>();
        subscribe();
        loadData();
    }

    public String getParameterValue(Parameter parameter) {
        String value = storage.get(parameter.name());
        return value;
    }

    public void setParameterValue(Parameter parameter, String value) {
        storage.put(parameter.name(), value);
        saveStorageToDisk();
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void addEventListener(Event event, EventListener listener) {
        Set<EventListener> listeners = eventListeners.get(event);
        if (listeners == null) {
            listeners = new HashSet<>();
            eventListeners.put(event, listeners);
        }

        listeners.add(listener);
    }

    public void fireEvent(Event event, Object value) {
        Set<EventListener> listeners = eventListeners.get(event);
        if (listeners == null) {
            return;
        }

        listeners.forEach(listener -> {
            System.out.println(event + " -> " + listener);
            listener.fireEvent(value);
        });
    }

    protected void subscribe() {

    }

    public void showMessage(String message) {
        showMessage(AlertType.INFORMATION, message, null);
    }

    public void showError(String message, Exception e) {
        showMessage(AlertType.ERROR, message, e);
    }

    public void showWarning(String message, Exception e) {
        showMessage(AlertType.WARNING, message, e);
    }

    private void loadData() {
        if (!configurationFile.exists()) {
            return;
        }

        try {
            TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
            Map<String, String> data = JSONUtils.loadFromDisk(configurationFile, typeReference);
            storage.putAll(data);
        } catch (Exception e) {
            showError("Error due to load data from configuration file " + configurationFile.getAbsolutePath(), e);
        }
    }

    private void saveStorageToDisk() {
        try {
            JSONUtils.saveToDisk(storage, configurationFile);
        } catch (Exception e) {
            showError("Error due to save configuration to disk", e);
        }
    }

    // TODO Improve this method to be able to see stacktrace somehow (log or on UI)
    private void showMessage(AlertType alertType, String message, Exception e) {
        if (e != null) {
            message += ": " + e.getMessage();
            e.printStackTrace();
        }
        showMessageOnUI(alertType, message);
    }

    private void showMessageOnUI(AlertType alertType, String message) {
        Platform.runLater(() -> new Alert(alertType, message).show());
    }


    public boolean showConfirmation(String caption, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setHeaderText(caption);
        alert.setContentText(message);

        Optional<ButtonType> pressedButton = alert.showAndWait();

        boolean result = pressedButton.get().equals(ButtonType.OK);

        return result;
    }
}
