package com.example.ti2mark1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class FrafDisplay extends Application {
    private TextArea leftText;
    private TextArea textResult;
    private TextField seedEntry;
    private TextArea keyEntry;
    private ComboBox<String> formatBox;
    private Stage primaryStage;
    private byte[] fileBytes;
    private String originalExtension = "";

    private static final Map<String, byte[]> FILE_SIGNATURES = new HashMap<>();
    static {
        FILE_SIGNATURES.put("jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        FILE_SIGNATURES.put("png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        FILE_SIGNATURES.put("mov", new byte[]{0x00, 0x00, 0x00, 0x14, 0x66, 0x74, 0x79, 0x70});
        FILE_SIGNATURES.put("aiff", new byte[]{0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x41, 0x49, 0x46, 0x46});
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            BorderPane root = new BorderPane();

            // Верхняя панель с выбором формата
            HBox topPanel = createTopPanel();
            root.setTop(topPanel);

            // Центральная часть с основным содержимым
            SplitPane centerPane = createCenterPane();
            root.setCenter(centerPane);

            // Нижняя панель с кнопками действий
            HBox bottomPanel = createBottomPanel();
            root.setBottom(bottomPanel);

            Scene scene = new Scene(root, 800, 500);
            primaryStage.setTitle("Шифратор/Дешифратор LFSR");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Ошибка инициализации приложения: " + e.getMessage());
        }
    }

    private HBox createTopPanel() {
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setPadding(new Insets(10));

        Label formatLbl = new Label("Формат сохранения:");
        formatBox = new ComboBox<>();
        formatBox.getItems().addAll("jpg", "png", "mov", "aiff", "txt", "docx");
        formatBox.setValue("jpg");

        topPanel.getChildren().addAll(formatLbl, formatBox);
        return topPanel;
    }

    private SplitPane createCenterPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPosition(0, 0.5);

        VBox leftPanel = createLeftPanel();
        VBox rightPanel = createRightPanel();

        splitPane.getItems().addAll(leftPanel, rightPanel);
        return splitPane;
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(15));

        Label seedLbl = new Label("Начальное состояние (37 бит):");
        seedEntry = new TextField();
        seedEntry.setTextFormatter(new TextFormatter<String>(change -> {
            String str = change.getControlNewText();
            return str.matches("[01]*") && str.length() <= 37 ? change : null;
        }));

        Button loadButton = new Button("Загрузить файл");
        loadButton.setOnAction(event -> openFile());

        Label inputLbl = new Label("Исходные данные:");
        leftText = new TextArea();
        leftText.setWrapText(true);

        leftPanel.getChildren().addAll(seedLbl, seedEntry, loadButton, inputLbl, leftText);
        return leftPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(15));

        Label keyLbl = new Label("Сгенерированный ключ:");
        keyEntry = new TextArea();
        keyEntry.setWrapText(true);

        Label resultLbl = new Label("Результат:");
        textResult = new TextArea();
        textResult.setWrapText(true);

        rightPanel.getChildren().addAll(keyLbl, keyEntry, resultLbl, textResult);
        return rightPanel;
    }

    private HBox createBottomPanel() {
        HBox bottomPanel = new HBox(20);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(10));

        Button encryptButton = new Button("Зашифровать");
        encryptButton.setOnAction(event -> processData());

        Button decryptButton = new Button("Расшифровать");
        decryptButton.setOnAction(event -> processData());

        Button saveButton = new Button("Сохранить файл");
        saveButton.setOnAction(event -> saveToFile());

        bottomPanel.getChildren().addAll(encryptButton, decryptButton, saveButton);
        return bottomPanel;
    }

    private byte[] Ciphering(byte[] message, byte[] key) {
        int len = Math.min(message.length, key.length);
        byte[] resultBytes = new byte[len];
        for (int i = 0; i < len; i++) {
            resultBytes[i] = (byte) (message[i] ^ key[i]);
        }
        return resultBytes;
    }

    private String bytesToBinaryString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return sb.toString();
    }

    private byte[] binaryStringToBytes(String binary) {
        int byteCount = binary.length() / 8;
        byte[] data = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            String byteStr = binary.substring(i * 8, (i + 1) * 8);
            data[i] = (byte) Integer.parseInt(byteStr, 2);
        }
        return data;
    }

    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                originalExtension = fileName.substring(dotIndex + 1).toLowerCase();
            }

            Task<byte[]> fileReadTask = new Task<>() {
                @Override
                protected byte[] call() throws Exception {
                    return Files.readAllBytes(file.toPath());
                }
            };

            fileReadTask.setOnSucceeded(event -> {
                fileBytes = fileReadTask.getValue();
                leftText.setText(bytesToBinaryString(fileBytes));
                if (formatBox.getItems().contains(originalExtension)) {
                    formatBox.setValue(originalExtension);
                }
            });
            fileReadTask.setOnFailed(event -> {
                showErrorAlert("Ошибка чтения файла");
            });

            new Thread(fileReadTask).start();
        }
    }

    private void processData() {
        String plainKey = seedEntry.getText();
        if (fileBytes == null || plainKey.isEmpty()) {
            showErrorAlert("Файл не загружен или ключ пуст");
            return;
        }
        if (plainKey.length() != 37) {
            showErrorAlert("Ключ должен быть ровно 37 бит длиной");
            return;
        }

        Task<Void> cipherTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String keyBinary;
                int lenM = fileBytes.length;
                String key = keyEntry.getText();

                if (key.isEmpty()) {
                    keyBinary = KeyGener.GenerateKey(plainKey, lenM * 8);
                } else {
                    keyBinary = key;
                }

                byte[] keyBytes = binaryStringToBytes(keyBinary);
                fileBytes = Ciphering(fileBytes, keyBytes);

                Platform.runLater(() -> {
                    keyEntry.setText(keyBinary);
                    textResult.setText(bytesToBinaryString(fileBytes));
                });
                return null;
            }
        };

        cipherTask.setOnFailed(event -> {
            showErrorAlert("Ошибка обработки: " + event.getSource().getException().getMessage());
        });
        new Thread(cipherTask).start();
    }

    private void saveToFile() {
        String selectedFormat = formatBox.getValue();
        if (fileBytes == null) {
            showErrorAlert("Нет данных для сохранения!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                selectedFormat.toUpperCase() + " файлы (*." + selectedFormat + ")", "*." + selectedFormat);
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName("output." + selectedFormat);

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                if (FILE_SIGNATURES.containsKey(selectedFormat)) {
                    byte[] signature = FILE_SIGNATURES.get(selectedFormat);
                    boolean valid = true;
                    for (int i = 0; i < signature.length && i < fileBytes.length; i++) {
                        if (fileBytes[i] != signature[i]) {
                            valid = false;
                            break;
                        }
                    }
                    if (!valid) {
                        showWarningAlert("Файл может быть поврежден или не соответствует формату " + selectedFormat);
                    }
                }

                Files.write(file.toPath(), fileBytes);
                showInfoAlert("Файл сохранен как " + file.getName());
            } catch (IOException e) {
                showErrorAlert("Ошибка сохранения: " + e.getMessage());
            }
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private void showWarningAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Предупреждение");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}