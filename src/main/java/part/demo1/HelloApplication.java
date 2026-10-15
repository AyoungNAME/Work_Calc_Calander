package part.demo1;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;

public class HelloApplication extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    // Section: Saved calendar storage location
    private static final String STORAGE_DIR_PATH = System.getProperty("user.home") + File.separator
            + "Documents" + File.separator + "Work Calc Calendar" + File.separator + "Saved Calendars";

    private Map<String, ProjectData> projectStorage = new HashMap<>();
    private YearMonth currentYearMonth = YearMonth.now();
    private javafx.scene.image.Image appIcon;

    // Section: Calendar project data
    public static class ProjectData implements Serializable {
        private static final long serialVersionUID = 500L;
        public String projectName;
        public Map<String, TimeRecord> dateRecords = new HashMap<>();

        public ProjectData(String projectName) {
            this.projectName = projectName;
        }
    }

    // Section: One day of recorded work time
    public static class TimeRecord implements Serializable {
        private static final long serialVersionUID = 500L;
        public String type = "";
        public String fromTime = "";
        public String toTime = "";
        public String[] breakFromTimes = {"", "", ""};
        public String[] breakToTimes = {"", "", ""};
        public String totalTime = "";
        public boolean isChecked = false;
    }


    // Section: Application startup and loading screen
    @Override
    public void start(Stage loadingStage) {
        initializeStorageDirectory();

        loadDataFromDisk();

        loadAppIcon();
        applyAppIcon(loadingStage);




        loadingStage.initStyle(StageStyle.TRANSPARENT);
        // Dark mode and light mode: choose the splash screen palette from the OS theme.
        boolean isDarkMode = checkOSDarkMode();
        String backgroundColor = isDarkMode ? "#1E1E1E" : "#FFFFFF";
        String indicatorColor = isDarkMode ? "white" : "black";

        // Colors: keep the splash background, indicator, and border readable in both themes.
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(60, 60);
        progressIndicator.setStyle("-fx-progress-color: " + indicatorColor + ";");

        StackPane root = new StackPane(progressIndicator);
        // Window size: compact 250 by 250 splash screen.
        root.setPrefSize(250, 250);
        root.setStyle("-fx-background-color: " + backgroundColor + ";"
                + " -fx-background-radius: 25px; -fx-border-radius: 25px;"
                + " -fx-border-color: " + borderColor + "; -fx-border-width: 1px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 0);");

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            loadingStage.setX(event.getScreenX() - xOffset);
            loadingStage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root, 250, 250);
        scene.setFill(Color.TRANSPARENT);
        loadingStage.setScene(scene);
        loadingStage.show();

        int randomTimeout = new Random().nextInt(1001) + 3000;
        PauseTransition delay = new PauseTransition(Duration.millis(randomTimeout));
        delay.setOnFinished(e -> {
            loadingStage.close();
            showMainWindow(isDarkMode);
        });
        delay.play();
    }

    // Section: Create the saved calendar folder
    private void initializeStorageDirectory() {
        try {
            File dir = new File(STORAGE_DIR_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            // Error handling: report storage folder setup problems.
            e.printStackTrace();
        }
    }

    // Section: Load the application and Dock icons
    private void loadAppIcon() {
        java.net.URL iconUrl = getClass().getResource("/assets/app-icon.png");
        // Error handling: continue without a custom icon when the asset is missing.
        if (iconUrl == null) {
            System.err.println("Application icon not found: /assets/app-icon.png");
            return;
        }

        javafx.scene.image.Image image = new javafx.scene.image.Image(iconUrl.toExternalForm());
        if (image.isError()) {
            System.err.println("Could not load application icon: " + image.getException());
            return;
        }
        appIcon = image;

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            try {
                if (java.awt.Taskbar.isTaskbarSupported()
                        && java.awt.Taskbar.getTaskbar().isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    java.awt.image.BufferedImage awtImage = javax.imageio.ImageIO.read(iconUrl);
                    if (awtImage != null) {
                        java.awt.Taskbar.getTaskbar().setIconImage(awtImage);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not set macOS Dock icon: " + ex.getMessage());
            }
        }
    }

    // Section: Apply the application icon to a window
    private void applyAppIcon(Stage stage) {
        if (appIcon != null) {
            stage.getIcons().setAll(appIcon);
        }
    }

    // Section: Detect the operating system color theme
    private boolean checkOSDarkMode() {
        // Dark mode and light mode: query macOS or Windows preferences.
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("mac")) {
                Process process = Runtime.getRuntime().exec(new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (line != null && line.toLowerCase().contains("dark")) return true;
                }
            } else if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec(new String[]{"reg", "query", "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("AppsUseLightTheme") && line.contains("0x0")) return true;
                    }
                }
            }
        } catch (Exception e) {
            // Error handling: use the light theme if OS theme detection fails.
            return false;
        }
        return false;
    }

    // Section: Main window and New File or Continue File buttons
    private void showMainWindow(boolean isDarkMode) {
        try {
            Stage mainStage = new Stage();
            applyAppIcon(mainStage);
            mainStage.initStyle(StageStyle.TRANSPARENT);

            BorderPane wrapperLayout = new BorderPane();
            String mainBgColor = isDarkMode ? "#2D2D2D" : "#FFFFFF";
            String btnColor = isDarkMode ? "#FFFFFF" : "#333333";
            // Colors: central foreground and background palette for the main window.
            String textColor = isDarkMode ? "#FFFFFF" : "#111111";

            String borderColor = isDarkMode ? "#FFFFFF" : "#000000";

            String closeBtnHoverBg = isDarkMode ? "rgba(255, 255, 255, 0.15)" : "rgba(0, 0, 0, 0.08)";
            String closeBtnHoverText = isDarkMode ? "#FF5252" : "#C62828";

            wrapperLayout.setStyle("-fx-background-color: " + mainBgColor + ";"
                    + " -fx-background-radius: 20px; -fx-border-radius: 20px;"
                    + " -fx-border-color: " + borderColor + "; -fx-border-width: 1px;"
                    + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);");

            Button closeButton = new Button("X");
            closeButton.setPrefSize(32, 32);
            // Fonts: set the close button font explicitly for consistent rendering.
            String closeBtnStyle = "-fx-background-color: transparent; -fx-text-fill: " + btnColor + "; -fx-font-size: 15px; -fx-font-weight: 900; -fx-cursor: hand; -fx-background-radius: 6px; -fx-font-family: 'Arial';";
            closeButton.setStyle(closeBtnStyle);
            closeButton.setOnMouseEntered(e -> closeButton.setStyle(closeBtnStyle + "-fx-background-color: " + closeBtnHoverBg + "; -fx-text-fill: " + closeBtnHoverText + ";"));
            closeButton.setOnMouseExited(e -> closeButton.setStyle(closeBtnStyle));
            closeButton.setOnAction(e -> mainStage.close());

            StackPane customHeader = new StackPane(closeButton);
            StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
            customHeader.setPadding(new Insets(10, 10, 0, 10));

            customHeader.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            customHeader.setOnMouseDragged(event -> {
                mainStage.setX(event.getScreenX() - xOffset);
                mainStage.setY(event.getScreenY() - yOffset);
            });

            javafx.scene.text.Text topCenterText = new javafx.scene.text.Text("Work Calc Calendar");
            topCenterText.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-fill: " + textColor + ";");

            Pane mainAppLogo = AppLogoUI.createLogo(70, isDarkMode);
            VBox textContainer = new VBox(15, mainAppLogo, topCenterText);
            textContainer.setAlignment(Pos.TOP_CENTER);
            textContainer.setPadding(new Insets(40, 0, 0, 0));

            // New File button: opens the calendar naming dialog.
            Button leftOptionBtn = new Button("New File");
            // Continue File button: opens the saved calendar list.
            Button rightOptionBtn = new Button("Continue File");

            leftOptionBtn.setPrefSize(140, 40);
            rightOptionBtn.setPrefSize(140, 40);

            String optBtnBg = isDarkMode ? "#444444" : "#F0F0F0";
            String optBtnHoverBg = isDarkMode ? "#555555" : "#E0E0E0";
            String optBtnStyle = "-fx-background-color: " + optBtnBg + "; -fx-text-fill: " + textColor + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8px;";

            leftOptionBtn.setStyle(optBtnStyle);
            rightOptionBtn.setStyle(optBtnStyle);

            leftOptionBtn.setOnMouseEntered(e -> leftOptionBtn.setStyle(optBtnStyle + "-fx-background-color: " + optBtnHoverBg + ";"));
            leftOptionBtn.setOnMouseExited(e -> leftOptionBtn.setStyle(optBtnStyle));
            rightOptionBtn.setOnMouseEntered(e -> rightOptionBtn.setStyle(optBtnStyle + "-fx-background-color: " + optBtnHoverBg + ";"));
            rightOptionBtn.setOnMouseExited(e -> rightOptionBtn.setStyle(optBtnStyle));
            leftOptionBtn.setOnAction(e -> handleNewCalendarWindow(mainStage, isDarkMode));
            rightOptionBtn.setOnAction(e -> handleContinueFileWindow(mainStage, isDarkMode));
            HBox bottomButtonContainer = new HBox(40, leftOptionBtn, rightOptionBtn);
            bottomButtonContainer.setAlignment(Pos.CENTER);
            bottomButtonContainer.setPadding(new Insets(0, 0, 40, 0));
            wrapperLayout.setTop(customHeader);
            wrapperLayout.setCenter(textContainer);
            wrapperLayout.setBottom(bottomButtonContainer);
            // Window size: main menu starts at 600 by 400.
            Scene scene = new Scene(wrapperLayout, 600, 400);
            scene.setFill(Color.TRANSPARENT);
            mainStage.setScene(scene);
            mainStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
    // Section: New File dialog and its name, Cancel, and OK controls
    private void handleNewCalendarWindow(Stage ownerStage, boolean isDarkMode) {
        Stage dialogStage = new Stage();
        applyAppIcon(dialogStage);
        dialogStage.initOwner(ownerStage);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        String bg = isDarkMode ? "#252526" : "#FAFAFA";
        String txtColor = isDarkMode ? "#FFFFFF" : "#111111";
        String inputBg = isDarkMode ? "#3E3E3F" : "#FFFFFF";
        // Colors: theme-aware dialog, text, and input field palette.
        String inputBorder = isDarkMode ? "#555555" : "#CCCCCC";
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 15px; -fx-border-radius: 15px; "
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 1px;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 12, 0, 0, 0);");
        Label mainPrompt = new Label("Create a new work session name");
        mainPrompt.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        // New File name field: sets the name of the saved calendar.
        TextField nameField = new TextField("UntitledCalendar");
        nameField.setPrefHeight(35);
        nameField.setStyle("-fx-background-color: " + inputBg + "; -fx-text-fill: " + txtColor + "; "
                + "-fx-border-color: " + inputBorder + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 0 10 0 10; -fx-font-size: 13px;");
        nameField.setOnMouseEntered(e -> {
            if (!nameField.isFocused()) nameField.setStyle(nameField.getStyle().replace("-fx-border-color: " + inputBorder, "-fx-border-color: #FF9100;"));
        });
        nameField.setOnMouseExited(e -> {
            if (!nameField.isFocused()) nameField.setStyle(nameField.getStyle().replace("-fx-border-color: #FF9100;", "-fx-border-color: " + inputBorder + ";"));
        });
        // Cancel button: closes the New File dialog without saving.
        Button cancelBtn = new Button("Cancel");
        // OK button: creates and saves the new calendar.
        Button okBtn = new Button("OK");
        cancelBtn.setPrefSize(100, 35);
        okBtn.setPrefSize(100, 35);
        String cancelStyle = "-fx-background-color: " + (isDarkMode ? "#3E3E3F" : "#EAEAEA") + "; -fx-text-fill: " + txtColor + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;";
        String okStyle = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;";
        cancelBtn.setStyle(cancelStyle);
        okBtn.setStyle(okStyle);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelStyle + "-fx-background-color: " + (isDarkMode ? "#505052" : "#DCDCDC") + ";"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(cancelStyle));
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(okStyle + "-fx-background-color: #1976D2;"));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(okStyle));
        HBox btnBox = new HBox(12, cancelBtn, okBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Pane modalMinLogo = AppLogoUI.createLogo(45, isDarkMode);
        layout.getChildren().addAll(modalMinLogo, mainPrompt, nameField, btnBox);
        layout.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        layout.setOnMouseDragged(e -> {
            dialogStage.setX(e.getScreenX() - xOffset);
            dialogStage.setY(e.getScreenY() - yOffset);
        });
        cancelBtn.setOnAction(e -> dialogStage.close());
        okBtn.setOnAction(e -> {
            String requestedName = nameField.getText().trim();
            if (!requestedName.isEmpty()) {
                String projectName = findAvailableProjectName(requestedName);
                nameField.setText(projectName);
                ProjectData newData = new ProjectData(projectName);
                projectStorage.put(projectName, newData);
                saveDataToDisk();
                dialogStage.close();
                openCalendarWindow(ownerStage, isDarkMode, newData);
            }
        });
        nameField.setOnAction(e -> okBtn.getOnAction().handle(e));
        // Window size: New File dialog starts at 380 by 220.
        Scene scene = new Scene(layout, 380, 220);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // Section: Avoid duplicate saved calendar names
    private String findAvailableProjectName(String requestedName) {
        if (!projectNameExists(requestedName)) return requestedName;
        int suffix = 1;
        String candidate;
        do {
            candidate = requestedName + " (" + suffix++ + ")";
        } while (projectNameExists(candidate));
        return candidate;
    }

    // Section: Check saved calendar names against memory and disk
    private boolean projectNameExists(String projectName) {
        return projectStorage.containsKey(projectName)
                || new File(STORAGE_DIR_PATH, projectName + ".xlsx").exists();
    }

    // Section: Format the month and year shown for a saved calendar
    private String formatProjectPeriod(ProjectData projectData) {
        LocalDate latestRecordDate = null;
        if (projectData != null && projectData.dateRecords != null) {
            for (String dateKey : projectData.dateRecords.keySet()) {
                try {
                    LocalDate date = LocalDate.parse(dateKey);
                    if (latestRecordDate == null || date.isAfter(latestRecordDate)) latestRecordDate = date;
                } catch (RuntimeException ignored) { }
            }
        }
        YearMonth period = latestRecordDate == null ? currentYearMonth : YearMonth.from(latestRecordDate);
        return java.time.format.DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH).format(period);
    }

    // Section: Read the month and year from an Excel calendar
    // Compatibility fix: recover the saved month and year from older workbook titles.
    private YearMonth readWorkbookPeriod(org.apache.poi.ss.usermodel.Sheet sheet) {
        try {
            org.apache.poi.ss.usermodel.Row titleRow = sheet.getRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow == null ? null : titleRow.getCell(0);
            if (titleCell != null && titleCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("\\((\\d{4}-\\d{2})\\)").matcher(titleCell.getStringCellValue());
                if (matcher.find()) return YearMonth.parse(matcher.group(1));
            }
        } catch (RuntimeException ignored) { }
        return currentYearMonth;
    }

    // Section: Continue File list and saved calendar selection
    private void handleContinueFileWindow(Stage ownerStage, boolean isDarkMode) {
        Stage selectionStage = new Stage();
        applyAppIcon(selectionStage);
        selectionStage.initStyle(StageStyle.TRANSPARENT);
        selectionStage.initOwner(ownerStage);
        BorderPane layout = new BorderPane();
        String bg = isDarkMode ? "#222222" : "#F5F5F5";
        String txtColor = isDarkMode ? "white" : "black";
        // Colors: Continue File window uses matching theme contrast.
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        layout.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 15px; -fx-border-radius: 15px;"
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 1px;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        Label titleLabel = new Label("Select File to Continue");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + txtColor + ";");
        ListView<String> projectListView = new ListView<>();
        projectListView.getItems().clear();
        projectListView.getItems().addAll(projectStorage.keySet());
        // Colors and fonts: style saved calendar names, period, totals, and selection state.
        projectListView.setCellFactory(lv -> new ListCell<String>() {
            private final Label mainPart = new Label();
            private final Label periodPart = new Label();
            private final Label timePart = new Label();
            private final Label slashPart = new Label(" / ");
            private final Label daysPart = new Label();
            private final HBox rowContainer = new HBox(mainPart, periodPart, timePart, slashPart, daysPart);
            private final Region separatorLine = new Region();
            private final VBox totalCellLayout = new VBox(rowContainer, separatorLine);

            {
                rowContainer.setAlignment(Pos.CENTER_LEFT);
                rowContainer.setSpacing(2);
                rowContainer.setPadding(new Insets(8, 12, 8, 12));
                separatorLine.setPrefHeight(1);
                separatorLine.setMaxWidth(Double.MAX_VALUE);
                separatorLine.setStyle("-fx-background-color: " + (isDarkMode ? "#383838" : "#E0E0E0") + ";");
                VBox.setMargin(separatorLine, new Insets(2, 12, 0, 12));
            }

            @Override
            protected void updateItem(String pName, boolean empty) {
                super.updateItem(pName, empty);
                if (empty || pName == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null);
                    ProjectData pData = projectStorage.get(pName);
                    int totalMins = 0;
                    if (pData != null && pData.dateRecords != null) {
                        for (TimeRecord rec : pData.dateRecords.values()) {
                            if ("CHECKED".equals(rec.type)) totalMins += (8 * 60);
                            else if ("FROM_TO".equals(rec.type)) {
                                int start = parseTimeStringToMinutes(rec.fromTime);
                                int end = parseTimeStringToMinutes(rec.toTime);
                                if (end >= start) totalMins += netWorkMinutes(rec);
                            } else if ("TOTAL".equals(rec.type)) {
                                totalMins += parseDurationStringToMinutes(rec.totalTime);
                            }
                        }
                    }
                    int hours = totalMins / 60;
                    int mins = totalMins % 60;

                    int filledDaysCount = 0;
                    if (pData != null && pData.dateRecords != null) {
                        filledDaysCount = pData.dateRecords.size();
                    }

                    String period = formatProjectPeriod(pData);
                    mainPart.setText(pName);
                    periodPart.setText(period);
                    timePart.setText(String.format(" — %d Hrs %02d Mins", hours, mins));

                    daysPart.setText(String.format("%d Days", filledDaysCount));

                    daysPart.setText(String.format("%d Days", filledDaysCount));
                    mainPart.setFont(Font.font("System", FontWeight.BOLD, 13));
                    periodPart.setFont(Font.font("System", FontWeight.BOLD, 13));
                    timePart.setFont(Font.font("System", FontWeight.BOLD, 13));
                    slashPart.setFont(Font.font("System", FontWeight.NORMAL, 13));
                    daysPart.setFont(Font.font("System", FontWeight.BOLD, 13));
                    if (isSelected()) {
                        separatorLine.setVisible(false);
                        if (isDarkMode) {
                            rowContainer.setStyle("-fx-background-color: #1A1A1A; -fx-background-radius: 4px; -fx-border-color: #FF9100; -fx-border-width: 1px; -fx-border-radius: 4px;");
                            mainPart.setStyle("-fx-text-fill: #E0E0E0;"); periodPart.setStyle("-fx-text-fill: #00E5FF;"); timePart.setStyle("-fx-text-fill: #E0E0E0;"); slashPart.setStyle("-fx-text-fill: #666666;"); daysPart.setStyle("-fx-text-fill: #B0BEC5;");
                        } else {
                            rowContainer.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 4px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 6, 0, 0, 2);");
                            mainPart.setStyle("-fx-text-fill: #222222;"); periodPart.setStyle("-fx-text-fill: #00838F;"); timePart.setStyle("-fx-text-fill: #222222;"); slashPart.setStyle("-fx-text-fill: #888888;"); daysPart.setStyle("-fx-text-fill: #000000;");
                        }
                    } else {
                        separatorLine.setVisible(true);
                        rowContainer.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-effect: null;");
                        mainPart.setStyle("-fx-text-fill: #FF9100;"); periodPart.setStyle("-fx-text-fill: " + (isDarkMode ? "#00E5FF" : "#00838F") + ";"); timePart.setStyle("-fx-text-fill: #FF9100;"); slashPart.setStyle("-fx-text-fill: #888888;"); daysPart.setStyle("-fx-text-fill: #00E5FF;");
                    }
                    setStyle("-fx-background-color: transparent !important; -fx-padding: 0px !important;");
                    setGraphic(totalCellLayout);
                }
            }
        });

        projectListView.setStyle("-fx-background-color: " + (isDarkMode ? "#333333" : "#FFFFFF") + ";");
        if(projectListView.getItems().isEmpty()) {
            projectListView.setPlaceholder(new Label("No projects found."));
        }
        // Open Calendar button: opens the selected saved calendar.
        Button openBtn = new Button("Open Calendar");
        openBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        openBtn.setDisable(true);

        projectListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) openBtn.setDisable(false);
        });
        openBtn.setOnAction(e -> {
            String selectedName = projectListView.getSelectionModel().getSelectedItem();
            if (selectedName != null) {
                selectionStage.close();
                ProjectData data = projectStorage.get(selectedName);
                openCalendarWindow(ownerStage, isDarkMode, data);
            }
        });
        // Cancel button: closes the saved calendar list.
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> selectionStage.close());
        // Delete Calendar button: removes the selected saved calendar.
        Button deleteBtn = new Button("Clear Record");
        deleteBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            String selectedName = projectListView.getSelectionModel().getSelectedItem();
            if (selectedName != null) {
                projectStorage.remove(selectedName);
                File excelFile = new File(STORAGE_DIR_PATH + File.separator + selectedName + ".xlsx");
                if (excelFile.exists()) {
                    excelFile.delete();
                }
                projectListView.getItems().remove(selectedName);
                if (projectListView.getItems().isEmpty()) {
                    projectListView.setPlaceholder(new Label("No projects found."));
                }
            }
        });
        Button locationBtn = new Button("File Location");
        locationBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        locationBtn.setOnAction(e -> {
            try {
                File folder = new File(STORAGE_DIR_PATH);
                if (folder.exists()) {
                    String folderUri = folder.getCanonicalFile().toURI().toString();
                    getHostServices().showDocument(folderUri);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        Pane continueLogo = AppLogoUI.createLogo(32, isDarkMode);
        HBox topBar = new HBox(12, continueLogo, titleLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 0, 5, 15));
        HBox footer = new HBox(12, cancelBtn, deleteBtn, locationBtn, openBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15));
        layout.setTop(topBar);
        layout.setCenter(projectListView);
        layout.setBottom(footer);
        BorderPane.setMargin(projectListView, new Insets(0, 15, 0, 15));
        Scene scene = new Scene(layout, 450, 400);
        scene.setFill(Color.TRANSPARENT);
        selectionStage.setScene(scene);
        selectionStage.show();
    }
    // Section: Monthly calendar, month navigation, and work summaries
    private void openCalendarWindow(Stage ownerStage, boolean isDarkMode, ProjectData projectData) {
        Stage calStage = new Stage();
        applyAppIcon(calStage);
        calStage.initStyle(StageStyle.TRANSPARENT);
        calStage.initOwner(ownerStage);
        BorderPane mainLayout = new BorderPane();
        String panelBg = isDarkMode ? "#252526" : "#FAFAFA";
        String txtColor = isDarkMode ? "#FFFFFF" : "#111111";
        // Colors: theme-aware calendar controls and text.
        String comboStyle = isDarkMode
                ? "-fx-background-color: #3E3E3F; -fx-control-inner-background: #3E3E3F; -fx-text-fill: white; -fx-prompt-text-fill: white; -fx-mark-color: white; -fx-text-base-color: white;"
                : "-fx-background-color: #EAEAEA; -fx-control-inner-background: #FFFFFF; -fx-text-fill: black; -fx-prompt-text-fill: black; -fx-mark-color: black; -fx-text-base-color: black;";
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        mainLayout.setStyle("-fx-background-color: " + panelBg + "; -fx-background-radius: 15px; -fx-border-radius: 15px;"
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 1px;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 12, 0, 0, 0);");
        calStage.setTitle("Work Calc Calendar - " + projectData.projectName);
        Label headerTitle = new Label("Work Calc Calendar — [" + projectData.projectName + "]");
        headerTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: " + txtColor + ";");
        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-family: 'Arial';");
        closeBtn.setOnAction(e -> calStage.close());
        BorderPane topHeaderBar = new BorderPane();
        topHeaderBar.setLeft(headerTitle);
        topHeaderBar.setRight(closeBtn);
        topHeaderBar.setPadding(new Insets(12, 15, 10, 15));
        topHeaderBar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        topHeaderBar.setOnMouseDragged(e -> {
            calStage.setX(e.getScreenX() - xOffset);
            calStage.setY(e.getScreenY() - yOffset);
        });
        ComboBox yearChooser = new ComboBox<>();
        int currentYear = YearMonth.now().getYear();
        for (int y = currentYear - 1000; y <= currentYear + 1000; y++) yearChooser.getItems().add(y);
        yearChooser.setValue(currentYearMonth.getYear());
        yearChooser.setStyle(comboStyle);
        ComboBox monthChooser = new ComboBox<>();
        monthChooser.getItems().addAll("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
        monthChooser.getSelectionModel().select(currentYearMonth.getMonthValue() - 1);
        monthChooser.setStyle(comboStyle);
        yearChooser.setConverter(new javafx.util.converter.IntegerStringConverter());
        monthChooser.setConverter(new javafx.util.converter.DefaultStringConverter());
        GridPane calendarGrid = new GridPane();
        calendarGrid.setAlignment(Pos.CENTER);
        calendarGrid.setHgap(8);
        calendarGrid.setVgap(8);
        CheckBox cbToggleMode = new CheckBox("Time / Days");
        cbToggleMode.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold; -fx-cursor: hand;");
        Label lblDataValue = new Label("0 Hours");
        lblDataValue.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px; -fx-font-weight: bold;");
        Runnable calculateDashboardStats = () -> {
            int totalMinutes = 0;
            int totalDays = 0;
            for (int d = 1; d <= currentYearMonth.lengthOfMonth(); d++) {
                String dKey = currentYearMonth.atDay(d).toString();
                if (projectData.dateRecords.containsKey(dKey)) {
                    TimeRecord rec = projectData.dateRecords.get(dKey);
                    if (rec != null) {
                        totalDays++;
                        if ("CHECKED".equals(rec.type)) {
                            totalMinutes += (8 * 60);
                        } else if ("FROM_TO".equals(rec.type)) {
                            int start = parseTimeStringToMinutes(rec.fromTime);
                            int end = parseTimeStringToMinutes(rec.toTime);
                            if (end >= start) totalMinutes += netWorkMinutes(rec);
                        } else if ("TOTAL".equals(rec.type)) {
                            totalMinutes += parseDurationStringToMinutes(rec.totalTime);
                        }
                    }
                }
            }
            if (cbToggleMode.isSelected()) {
                lblDataValue.setText(totalDays + " Days logged");
            } else {
                int displayHours = totalMinutes / 60;
                int displayMins = totalMinutes % 60;
                lblDataValue.setText(String.format("%d Hrs %02d Mins", displayHours, displayMins));
            }
        };
        cbToggleMode.setOnAction(e -> calculateDashboardStats.run());
        Runnable refreshCalendar = () -> {
            calendarGrid.getChildren().clear();
            Object selectedYearObj = yearChooser.getValue();
            int selectedYear = currentYearMonth.getYear();
            if (selectedYearObj instanceof Integer) {
                selectedYear = (Integer) selectedYearObj;
            }
            int selectedMonthIdx = monthChooser.getSelectionModel().getSelectedIndex() + 1;
            currentYearMonth = YearMonth.of(selectedYear, selectedMonthIdx);
            String dayNames[] = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (int i = 0; i < 7; i++) {
                Label dayLabel = new Label(dayNames[i]);
                dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                dayLabel.setTextFill(isDarkMode ? Color.LIGHTGRAY : Color.DARKGRAY);
                calendarGrid.add(dayLabel, i, 0);
            }
            LocalDate firstOfMonth = currentYearMonth.atDay(1);
            int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue() % 7;
            int daysInMonth = currentYearMonth.lengthOfMonth();
            int row = 1;
            int col = dayOfWeekValue;
            for (int day = 1; day <= daysInMonth; day++) {
                final int currentDay = day;
                String dateKey = currentYearMonth.atDay(currentDay).toString();
                Button dayButton = new Button(String.valueOf(day));
                dayButton.setPrefSize(45, 45);
                if (projectData.dateRecords.containsKey(dateKey)) {
                    dayButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
                } else {
                    dayButton.setStyle("-fx-background-color: " + (isDarkMode ? "#3E3E3F" : "#EAEAEA") + "; -fx-text-fill: " + txtColor + "; -fx-background-radius: 6px; -fx-cursor: hand;");
                }
                dayButton.setOnAction(ev -> {
                    openTimeFeatureDialog(calStage, isDarkMode, dateKey, projectData, () -> {
                        if (projectData.dateRecords.containsKey(dateKey)) {
                            dayButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
                        } else {
                            dayButton.setStyle("-fx-background-color: " + (isDarkMode ? "#3E3E3F" : "#EAEAEA") + "; -fx-text-fill: " + txtColor + "; -fx-background-radius: 6px; -fx-cursor: hand;");
                        }
                        calculateDashboardStats.run();
                    });
                });
                calendarGrid.add(dayButton, col, row);
                col++;
                if (col > 6) { col = 0; row++; }
            }
            calculateDashboardStats.run();
        };
        yearChooser.setOnAction(e -> refreshCalendar.run());
        monthChooser.setOnAction(e -> refreshCalendar.run());
        refreshCalendar.run();
        Label lblYearText = new Label("Year:"); lblYearText.setStyle("-fx-text-fill: " + txtColor + ";");
        Label lblMonthText = new Label("Month:"); lblMonthText.setStyle("-fx-text-fill: " + txtColor + ";");
        HBox selectBar = new HBox(12, lblYearText, yearChooser, lblMonthText, monthChooser);
        selectBar.setAlignment(Pos.CENTER);
        selectBar.setPadding(new Insets(10, 0, 15, 0));
        VBox centerArea = new VBox(10, selectBar, calendarGrid);
        centerArea.setPadding(new Insets(10, 15, 10, 15));
        BorderPane footerDashboard = new BorderPane();
        footerDashboard.setPadding(new Insets(10, 20, 20, 20));
        footerDashboard.setLeft(cbToggleMode);
        footerDashboard.setRight(lblDataValue);
        mainLayout.setTop(topHeaderBar);
        mainLayout.setCenter(centerArea);
        mainLayout.setBottom(footerDashboard);
        // Window size: monthly calendar starts at 450 by 520.
        Scene scene = new Scene(mainLayout, 450, 520);
        scene.setFill(Color.TRANSPARENT);
        calStage.setScene(scene);
        calStage.show();
    }
    // Section: Daily time logger, time entry, breaks, and save controls
    private void openTimeFeatureDialog(Stage parentStage, boolean isDarkMode, String dateKey, ProjectData projectData, Runnable onSaveSuccess) {
        Stage dialogStage = new Stage();
        applyAppIcon(dialogStage);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initOwner(parentStage);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        // Colors: time logger panel and controls follow the active theme.
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        String panelBg = isDarkMode ? "#2D2D2D" : "#FFFFFF";
        String txtColor = isDarkMode ? "white" : "black";
        String btnColor = isDarkMode ? "#FFFFFF" : "#333333";

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        layout.setStyle("-fx-background-color: " + panelBg + ";"
                + " -fx-background-radius: 15px; -fx-border-radius: 15px;"
                + " -fx-border-color: " + borderColor + "; -fx-border-width: 1px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-family: 'Arial';");
        closeBtn.setOnAction(e -> dialogStage.close());

        // Fonts: emphasize the selected date in the logger header.
        Label headerTitle = new Label("Time Logger [" + dateKey + "]");
        headerTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: " + txtColor + "; -fx-font-size: 14px;");

        BorderPane dialogHeaderBar = new BorderPane();
        dialogHeaderBar.setLeft(headerTitle);
        dialogHeaderBar.setRight(closeBtn);
        dialogHeaderBar.setPadding(new Insets(0, 0, 10, 0));

        dialogHeaderBar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        dialogHeaderBar.setOnMouseDragged(e -> {
            dialogStage.setX(e.getScreenX() - xOffset);
            dialogStage.setY(e.getScreenY() - yOffset);
        });

        Label mainPrompt = new Label("Select Prefered Time Recording Feature:");
        mainPrompt.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold;");

        ToggleGroup featureGroup = new ToggleGroup();
        // Work period option: records a start and end time with optional breaks.
        RadioButton rbFromTo = new RadioButton("From Time to This Time");
        // Total Time option: records a manually entered duration.
        RadioButton rbTotal = new RadioButton("Total Time");
        // Checked option: records the standard eight-hour workday.
        RadioButton rbChecked = new RadioButton("Checked (8 Hours)");
        rbFromTo.setToggleGroup(featureGroup); rbTotal.setToggleGroup(featureGroup); rbChecked.setToggleGroup(featureGroup);

        String radioStyle = "-fx-text-fill: " + txtColor + ";";
        rbFromTo.setStyle(radioStyle); rbTotal.setStyle(radioStyle); rbChecked.setStyle(radioStyle);

        List<String> amPmList = new ArrayList<>();
        String[] ampmArray = new String[]{"AM", "PM"};
        for (String ampm : ampmArray) {
            for (int h = 1; h <= 12; h++) {
                for (int m = 0; m < 60; m += 15) amPmList.add(String.format("%02d:%02d %s", h, m, ampm));
            }
        }

        ComboBox<String> cbFrom = new ComboBox<>(); cbFrom.getItems().addAll(amPmList); cbFrom.setValue("09:00 AM");
        ComboBox<String> cbTo = new ComboBox<>(); cbTo.getItems().addAll(amPmList); cbTo.setValue("06:00 PM");
        TextField tfFrom = new TextField("09:00 AM"); tfFrom.setPromptText("hh:mm AM/PM");
        TextField tfTo = new TextField("06:00 PM"); tfTo.setPromptText("hh:mm AM/PM");
        installTimeNormalization(tfFrom); installTimeNormalization(tfTo);
        ToggleGroup timeInputGroup = new ToggleGroup();
        // Time selection controls: choose a dropdown or type times directly.
        RadioButton rbSelectTime = new RadioButton("Select from list");
        RadioButton rbTypeTime = new RadioButton("Type times");
        rbSelectTime.setToggleGroup(timeInputGroup); rbTypeTime.setToggleGroup(timeInputGroup); rbSelectTime.setSelected(true);
        rbSelectTime.setStyle(radioStyle); rbTypeTime.setStyle(radioStyle);
        HBox timeInputMode = new HBox(15, rbSelectTime, rbTypeTime);
        HBox typedFromTo = new HBox(8, new Label("From:"), tfFrom, new Label("To:"), tfTo);
        ComboBox<String>[] breakFrom = new ComboBox[3];
        ComboBox<String>[] breakTo = new ComboBox[3];
        TextField[] typedBreakFrom = new TextField[3];
        TextField[] typedBreakTo = new TextField[3];
        VBox breakRows = new VBox(6);
        List<String> optionalAmPmList = new ArrayList<>(); optionalAmPmList.add(""); optionalAmPmList.addAll(amPmList);
        // Break time fields: provide up to three breaks within the work period.
        for (int i = 0; i < 3; i++) {
            breakFrom[i] = new ComboBox<>(); breakFrom[i].getItems().addAll(optionalAmPmList); breakFrom[i].setValue("");
            breakTo[i] = new ComboBox<>(); breakTo[i].getItems().addAll(optionalAmPmList); breakTo[i].setValue("");
            breakFrom[i].setPrefWidth(105); breakTo[i].setPrefWidth(105);
            typedBreakFrom[i] = new TextField(); typedBreakFrom[i].setPromptText("hh:mm AM/PM");
            typedBreakTo[i] = new TextField(); typedBreakTo[i].setPromptText("hh:mm AM/PM");
            installTimeNormalization(typedBreakFrom[i]); installTimeNormalization(typedBreakTo[i]);
            Label breakLabel = new Label("Break time " + (i + 1) + ":");
            breakLabel.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold;");
            Label typedBreakLabel = new Label("Break time " + (i + 1) + ":");
            typedBreakLabel.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold;");
            Label selectToLabel = new Label("To:"); selectToLabel.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold;");
            Label typeToLabel = new Label("To:"); typeToLabel.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold;");
            HBox selectRow = new HBox(8, breakLabel, breakFrom[i], selectToLabel, breakTo[i]);
            HBox typeRow = new HBox(8, typedBreakLabel, typedBreakFrom[i], typeToLabel, typedBreakTo[i]);
            selectRow.visibleProperty().bind(rbSelectTime.selectedProperty()); selectRow.managedProperty().bind(selectRow.visibleProperty());
            typeRow.visibleProperty().bind(rbTypeTime.selectedProperty()); typeRow.managedProperty().bind(typeRow.visibleProperty());
            breakRows.getChildren().addAll(selectRow, typeRow);
        }

        List<String> durationList = new ArrayList<>();
        for (int h = 0; h <= 24; h++) {
            for (int m = 0; m < 60; m += 15) {
                if (h == 0 && m == 0) continue;
                durationList.add(String.format("%02d:%02d", h, m)); if (h == 24) break;
            }
        }
        ComboBox<String> cbTotalDuration = new ComboBox<>();
        cbTotalDuration.getItems().addAll(durationList);
        cbTotalDuration.setValue("08:00");

        String comboStyle = isDarkMode
                ? "-fx-background-color: #3E3E3F; -fx-text-fill: white; -fx-control-inner-background: #3E3E3F; -fx-text-base-color: white; -fx-mark-color: white;"
                : "-fx-background-color: #EAEAEA; -fx-text-fill: black; -fx-control-inner-background: #FFFFFF; -fx-text-base-color: black; -fx-mark-color: black;";

        cbFrom.setStyle(comboStyle);
        cbTo.setStyle(comboStyle);
        cbTotalDuration.setStyle(comboStyle);
        for (int i = 0; i < 3; i++) { breakFrom[i].setStyle(comboStyle); breakTo[i].setStyle(comboStyle); }
        for (TextField field : new TextField[]{tfFrom, tfTo}) field.setStyle(comboStyle);
        for (int i = 0; i < 3; i++) { typedBreakFrom[i].setStyle(comboStyle); typedBreakTo[i].setStyle(comboStyle); }

        Label lblFrom = new Label("From:"); lblFrom.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");
        Label lblTo = new Label("To:"); lblTo.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");
        Label lblDuration = new Label("Duration:"); lblDuration.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");

        HBox hboxFromTo = new HBox(8, lblFrom, cbFrom, lblTo, cbTo);
        typedFromTo.getChildren().get(0).setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold;");
        typedFromTo.getChildren().get(2).setStyle("-fx-text-fill: " + txtColor + "; -fx-font-weight: bold;");
        hboxFromTo.visibleProperty().bind(rbSelectTime.selectedProperty()); hboxFromTo.managedProperty().bind(hboxFromTo.visibleProperty());
        typedFromTo.visibleProperty().bind(rbTypeTime.selectedProperty()); typedFromTo.managedProperty().bind(typedFromTo.visibleProperty());
        HBox hboxTotal = new HBox(8, lblDuration, cbTotalDuration);

        hboxFromTo.disableProperty().addListener((obs, oldV, newV) -> hboxFromTo.setOpacity(newV ? 0.35 : 1.0));
        hboxTotal.disableProperty().addListener((obs, oldV, newV) -> hboxTotal.setOpacity(newV ? 0.35 : 1.0));

        hboxFromTo.disableProperty().bind(rbFromTo.selectedProperty().not());
        timeInputMode.disableProperty().bind(rbFromTo.selectedProperty().not());
        typedFromTo.disableProperty().bind(rbFromTo.selectedProperty().not());
        breakRows.disableProperty().bind(rbFromTo.selectedProperty().not());
        breakRows.disableProperty().addListener((obs, oldV, newV) -> breakRows.setOpacity(newV ? 0.35 : 1.0));
        hboxTotal.disableProperty().bind(rbTotal.selectedProperty().not());

        TimeRecord existing = projectData.dateRecords.get(dateKey);
        if (existing != null) {
            if ("FROM_TO".equals(existing.type)) {
                rbFromTo.setSelected(true); cbFrom.setValue(existing.fromTime); cbTo.setValue(existing.toTime);
                for (int i = 0; i < 3; i++) {
                    if (existing.breakFromTimes != null && i < existing.breakFromTimes.length) {
                        typedBreakFrom[i].setText(existing.breakFromTimes[i]);
                        if (optionalAmPmList.contains(existing.breakFromTimes[i])) breakFrom[i].setValue(existing.breakFromTimes[i]);
                    }
                    if (existing.breakToTimes != null && i < existing.breakToTimes.length) {
                        typedBreakTo[i].setText(existing.breakToTimes[i]);
                        if (optionalAmPmList.contains(existing.breakToTimes[i])) breakTo[i].setValue(existing.breakToTimes[i]);
                    }
                }
                tfFrom.setText(existing.fromTime); tfTo.setText(existing.toTime);
                if (!amPmList.contains(existing.fromTime) || !amPmList.contains(existing.toTime)) rbTypeTime.setSelected(true);
            }
            else if ("TOTAL".equals(existing.type)) { rbTotal.setSelected(true); cbTotalDuration.setValue(existing.totalTime); }
            else if ("CHECKED".equals(existing.type)) rbChecked.setSelected(true);
        } else rbFromTo.setSelected(true);

        // Clear Record button: removes the selected day entry.
        Button deleteBtn = new Button("Clear Record"); deleteBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            projectData.dateRecords.remove(dateKey);
            if (projectData.dateRecords.isEmpty()) {
                File exFile = new File(STORAGE_DIR_PATH + File.separator + projectData.projectName + ".xlsx");
                if (exFile.exists()) exFile.delete();
                projectStorage.remove(projectData.projectName);
            } else saveDataToDisk();
            onSaveSuccess.run(); dialogStage.close();
        });

        // Save Record button: stores the selected day entry.
        Button saveBtn = new Button("Save Record"); saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            TimeRecord record = new TimeRecord();
            if (rbFromTo.isSelected()) {
                boolean typing = rbTypeTime.isSelected();
                record.type = "FROM_TO";
                record.fromTime = typing ? normalizeTimeInput(tfFrom.getText()) : cbFrom.getValue();
                record.toTime = typing ? normalizeTimeInput(tfTo.getText()) : cbTo.getValue();
                // Error handling: validate the work period before saving the record.
        if (!isValidTimeString(record.fromTime) || !isValidTimeString(record.toTime)) {
                    new Alert(Alert.AlertType.ERROR, "Enter the work times in hh:mm AM/PM format, for example 09:00 AM.").showAndWait(); return;
                }
                int workStart = parseTimeStringToMinutes(record.fromTime);
                int workEnd = parseTimeStringToMinutes(record.toTime);
                if (workEnd <= workStart) {
                    new Alert(Alert.AlertType.ERROR, "The end of the work period must be later than its start.").showAndWait(); return;
                }
                for (int i = 0; i < 3; i++) {
                    record.breakFromTimes[i] = typing ? normalizeTimeInput(typedBreakFrom[i].getText()) : breakFrom[i].getValue();
                    record.breakToTimes[i] = typing ? normalizeTimeInput(typedBreakTo[i].getText()) : breakTo[i].getValue();
                    String breakFromInput = typing ? typedBreakFrom[i].getText().trim() : record.breakFromTimes[i];
                    String breakToInput = typing ? typedBreakTo[i].getText().trim() : record.breakToTimes[i];
                    boolean hasStart = !breakFromInput.isBlank(), hasEnd = !breakToInput.isBlank();
                    if (hasStart != hasEnd || (hasStart && (!isValidTimeString(record.breakFromTimes[i]) || !isValidTimeString(record.breakToTimes[i])))) {
                        new Alert(Alert.AlertType.ERROR, "Enter both times for each break in hh:mm AM/PM format, or leave both empty.").showAndWait(); return;
                    }
                    if (hasStart) {
                        int breakStart = parseTimeStringToMinutes(record.breakFromTimes[i]);
                        int breakEnd = parseTimeStringToMinutes(record.breakToTimes[i]);
                        if (breakStart < workStart || breakEnd > workEnd || breakEnd <= breakStart) {
                            new Alert(Alert.AlertType.ERROR, "Each break must start and end within the work period.").showAndWait(); return;
                        }
                        for (int previous = 0; previous < i; previous++) {
                            String previousFrom = record.breakFromTimes[previous], previousTo = record.breakToTimes[previous];
                            if (previousFrom != null && !previousFrom.isBlank() && previousTo != null && !previousTo.isBlank()) {
                                int previousStart = parseTimeStringToMinutes(previousFrom);
                                int previousEnd = parseTimeStringToMinutes(previousTo);
                                if (breakStart < previousEnd && previousStart < breakEnd) {
                                    new Alert(Alert.AlertType.ERROR, "Break times cannot overlap.").showAndWait(); return;
                                }
                            }
                        }
                    }
                }
            }
            else if (rbTotal.isSelected()) { record.type = "TOTAL"; record.totalTime = cbTotalDuration.getValue(); }
            else if (rbChecked.isSelected()) { record.type = "CHECKED"; record.isChecked = true; }
            projectData.dateRecords.put(dateKey, record); saveDataToDisk(); onSaveSuccess.run(); dialogStage.close();
        });

        HBox btnContainer = new HBox(15, deleteBtn, saveBtn); btnContainer.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(dialogHeaderBar, mainPrompt, rbFromTo, timeInputMode, hboxFromTo, typedFromTo, breakRows, rbTotal, hboxTotal, rbChecked, btnContainer);

        Scene scene = new Scene(layout, 440, 650);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.show();
    }
    // Section: Parse and validate time input
    private int parseTimeStringToMinutes(String timeStr) {
        try {
            if (timeStr == null || !timeStr.contains(":") || !timeStr.contains(" ")) return 0;
            String spaceSplit[] = timeStr.split(" ");
            String rawClock = spaceSplit[0];
            String ampm = spaceSplit[1];
            String colonSplit[] = rawClock.split(":");
            int hour = Integer.parseInt(colonSplit[0]);
            int min = Integer.parseInt(colonSplit[1]);
            if ("PM".equalsIgnoreCase(ampm) && hour < 12) hour += 12;
            if ("AM".equalsIgnoreCase(ampm) && hour == 12) hour = 0;
            return (hour * 60) + min;
        } catch (Exception e) { return 0; }
    }
    // Section: Parse recorded work durations
    private int parseDurationStringToMinutes(String durationStr) {
        try {
            if (durationStr == null || !durationStr.contains(":")) return 0;
            String colonSplit[] = durationStr.split(":");
            int hour = Integer.parseInt(colonSplit[0]);
            int min = Integer.parseInt(colonSplit[1]);
            return (hour * 60) + min;
        } catch (Exception e) { return 0; }
    }

    // Section: Normalize typed times into a consistent display format
    private String normalizeTimeInput(String input) {
        if (input == null) return null;
        if (input.isBlank()) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)^\\s*(\\d{1,2}):?(\\d{2})\\s*(AM|PM)\\s*$")
                .matcher(input);
        // Error handling: reject inputs that do not match supported time formats.
        if (!matcher.matches()) return null;
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        if (hour < 1 || hour > 12 || minute > 59) return null;
        return String.format(Locale.ROOT, "%02d:%02d %s", hour, minute, matcher.group(3).toUpperCase(Locale.ROOT));
    }

    // Section: Apply automatic time formatting to a text field
    private void installTimeNormalization(TextField field) {
        Runnable normalize = () -> {
            String normalized = normalizeTimeInput(field.getText());
            if (normalized != null) field.setText(normalized);
        };
        field.setOnAction(event -> normalize.run());
        field.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) normalize.run();
        });
    }

    // Section: Check whether a time value is valid
    private boolean isValidTimeString(String timeStr) {
        String normalized = normalizeTimeInput(timeStr);
        return normalized != null && !normalized.isEmpty();
    }

    // Section: Calculate work minutes after subtracting breaks
    // Fix: subtract only break minutes that overlap the recorded work period.
    private int netWorkMinutes(TimeRecord record) {
        int start = parseTimeStringToMinutes(record.fromTime);
        int end = parseTimeStringToMinutes(record.toTime);
        int total = Math.max(0, end - start);
        if (record.breakFromTimes != null && record.breakToTimes != null) {
            for (int i = 0; i < Math.min(record.breakFromTimes.length, record.breakToTimes.length); i++) {
                String from = record.breakFromTimes[i], to = record.breakToTimes[i];
                if (from != null && to != null && !from.isBlank() && !to.isBlank()) {
                    int breakStart = parseTimeStringToMinutes(from), breakEnd = parseTimeStringToMinutes(to);
                    int overlapStart = Math.max(start, breakStart);
                    int overlapEnd = Math.min(end, breakEnd);
                    if (overlapEnd > overlapStart) total -= (overlapEnd - overlapStart);
                }
            }
        }
        return Math.max(0, total);
    }
    // Section: Save calendars to Excel workbooks
    private void saveDataToDisk() {
        for (Map.Entry<String, ProjectData> entry : projectStorage.entrySet()) {
            String projectName = entry.getKey();
            ProjectData pData = entry.getValue();
            File file = new File(STORAGE_DIR_PATH + File.separator + projectName + ".xlsx");
            try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(currentYearMonth.getMonth().name());
                org.apache.poi.ss.usermodel.Sheet detailsSheet = workbook.createSheet("Time Details");
                // Window size and print layout: fit the calendar to one A4 landscape page.
                sheet.setFitToPage(true);
                org.apache.poi.ss.usermodel.PrintSetup printSetup = sheet.getPrintSetup();
                printSetup.setFitWidth((short) 1);
                printSetup.setFitHeight((short) 1);
                printSetup.setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
                printSetup.setLandscape(true);
                sheet.setAutobreaks(true);
                sheet.setHorizontallyCenter(true);
                sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.LeftMargin, 0.25);
                sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.RightMargin, 0.25);
                sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.TopMargin, 0.30);
                sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.BottomMargin, 0.30);
                org.apache.poi.ss.usermodel.Font mainTitleFont = workbook.createFont();
                mainTitleFont.setBold(true);
                mainTitleFont.setFontHeightInPoints((short) 16);
                org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
                titleStyle.setFont(mainTitleFont);
                titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
                org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFont(headerFont);
                headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_TEAL.getIndex());
                headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                setBorder(headerStyle);
                org.apache.poi.ss.usermodel.CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);
                cellStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
                cellStyle.setWrapText(true);
                org.apache.poi.ss.usermodel.Font calendarFont = workbook.createFont();
                calendarFont.setFontHeightInPoints((short) 9);
                cellStyle.setFont(calendarFont);
                setBorder(cellStyle);
                org.apache.poi.ss.usermodel.Font sundayDayFont = workbook.createFont();
                sundayDayFont.setBold(true);
                sundayDayFont.setFontHeightInPoints((short) 9);
                sundayDayFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
                org.apache.poi.ss.usermodel.Font saturdayDayFont = workbook.createFont();
                saturdayDayFont.setBold(true);
                saturdayDayFont.setFontHeightInPoints((short) 9);
                saturdayDayFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.BLUE.getIndex());
                org.apache.poi.ss.usermodel.Font weekdayDayFont = workbook.createFont();
                weekdayDayFont.setBold(true);
                weekdayDayFont.setFontHeightInPoints((short) 9);
                weekdayDayFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.BLACK.getIndex());
                org.apache.poi.ss.usermodel.Font boldLabelFont = workbook.createFont();
                boldLabelFont.setBold(true);
                boldLabelFont.setFontHeightInPoints((short) 9);
                boldLabelFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.BLACK.getIndex());
                org.apache.poi.ss.usermodel.CellStyle detailCellStyle = workbook.createCellStyle();
                detailCellStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                detailCellStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
                detailCellStyle.setWrapText(true);
                setBorder(detailCellStyle);
                org.apache.poi.ss.usermodel.Font footerFont = workbook.createFont();
                footerFont.setBold(true);
                org.apache.poi.ss.usermodel.CellStyle footerStyle = workbook.createCellStyle();
                footerStyle.setFont(footerFont);
                footerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
                footerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                setBorder(footerStyle);

                org.apache.poi.ss.usermodel.Row detailTitleRow = detailsSheet.createRow(0);
                detailTitleRow.setHeightInPoints(30);
                org.apache.poi.ss.usermodel.Cell detailTitle = detailTitleRow.createCell(0);
                detailTitle.setCellValue("Time Details — All Dates");
                detailTitle.setCellStyle(titleStyle);
                detailsSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));
                org.apache.poi.ss.usermodel.Row detailHeaderRow = detailsSheet.createRow(1);
                detailHeaderRow.setHeightInPoints(25);
                String[] detailHeaders = {"Date", "Entry type", "Work period", "Break 1", "Break 2", "Break 3", "Worked"};
                for (int i = 0; i < detailHeaders.length; i++) {
                    org.apache.poi.ss.usermodel.Cell header = detailHeaderRow.createCell(i);
                    header.setCellValue(detailHeaders[i]);
                    header.setCellStyle(headerStyle);
                }
                detailsSheet.createFreezePane(0, 2);
                detailsSheet.setFitToPage(true);
                detailsSheet.getPrintSetup().setLandscape(true);
                detailsSheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
                detailsSheet.getPrintSetup().setFitWidth((short) 1);
                detailsSheet.getPrintSetup().setFitHeight((short) 0);
                detailsSheet.setRepeatingRows(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 6));
                int detailRowIndex = 2;
                int[] detailColumnWidths = {3800, 4200, 5600, 5200, 5200, 5200, 3800};
                for (int i = 0; i < detailColumnWidths.length; i++) detailsSheet.setColumnWidth(i, detailColumnWidths[i]);

                org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
                titleRow.setHeightInPoints(35);
                org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("Work Calc Calendar — [" + projectName + "] (" + currentYearMonth.toString() + ")");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));
                org.apache.poi.ss.usermodel.Row weekHeaderRow = sheet.createRow(2);
                weekHeaderRow.setHeightInPoints(25);
                String dayNames[] = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                for (int i = 0; i < 7; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = weekHeaderRow.createCell(i);
                    cell.setCellValue(dayNames[i]);
                    cell.setCellStyle(headerStyle);
                }
                LocalDate firstOfMonth = currentYearMonth.atDay(1);
                int startColumn = firstOfMonth.getDayOfWeek().getValue() % 7;
                int daysInMonth = currentYearMonth.lengthOfMonth();
                int currentIdxRow = 3;
                org.apache.poi.ss.usermodel.Row currentExcelRow = sheet.createRow(currentIdxRow);
                currentExcelRow.setHeightInPoints(50);
                for (int i = 0; i < startColumn; i++) {
                    org.apache.poi.ss.usermodel.Cell emptyCell = currentExcelRow.createCell(i);
                    emptyCell.setCellStyle(cellStyle);
                }
                int totalMinutes = 0;
                int totalDays = 0;
                int maxLinesInWeek = 1;
                int col = startColumn;
                for (int day = 1; day <= daysInMonth; day++) {
                    String dKey = currentYearMonth.atDay(day).toString();
                    org.apache.poi.ss.usermodel.Cell dateCell = currentExcelRow.createCell(col);
                    String cellContent = "Day " + day;
                    if (pData.dateRecords.containsKey(dKey)) {
                        TimeRecord rec = pData.dateRecords.get(dKey);
                        if (rec != null) {
                            totalDays++;
                            if ("CHECKED".equals(rec.type)) {
                                totalMinutes += (8 * 60);
                                cellContent += " | Net 8h 00m";
                            } else if ("FROM_TO".equals(rec.type)) {
                                int start = parseTimeStringToMinutes(rec.fromTime);
                                int end = parseTimeStringToMinutes(rec.toTime);
                                int workedMinutes = end >= start ? netWorkMinutes(rec) : 0;
                                totalMinutes += workedMinutes;
                                cellContent = "Day " + day + " | Net " + formatWorkedTimeReadable(workedMinutes)
                                        + "\nShift " + formatCompactTimeRange(rec.fromTime, rec.toTime, false);
                                List<String> compactBreaks = new ArrayList<>();
                                if (rec.breakFromTimes != null && rec.breakToTimes != null) {
                                    for (int i = 0; i < Math.min(rec.breakFromTimes.length, rec.breakToTimes.length); i++) {
                                        if (rec.breakFromTimes[i] != null && !rec.breakFromTimes[i].isBlank()
                                                && rec.breakToTimes[i] != null && !rec.breakToTimes[i].isBlank()) {
                                            compactBreaks.add(formatCompactTimeRange(rec.breakFromTimes[i], rec.breakToTimes[i], true));
                                        }
                                    }
                                }
                                if (!compactBreaks.isEmpty()) cellContent += "\nBreaks: " + String.join("; ", compactBreaks);
                            } else if ("TOTAL".equals(rec.type)) {
                                int workedMinutes = parseDurationStringToMinutes(rec.totalTime);
                                totalMinutes += workedMinutes;
                                cellContent = "Day " + day + " | Net " + formatWorkedTimeReadable(workedMinutes);
                            }
                        }
                    }
                    org.apache.poi.xssf.usermodel.XSSFRichTextString richContent = new org.apache.poi.xssf.usermodel.XSSFRichTextString(cellContent);
                    int dayLabelEnd = cellContent.indexOf(" | ");
                    if (dayLabelEnd < 0) dayLabelEnd = cellContent.indexOf('\n');
                    if (dayLabelEnd < 0) dayLabelEnd = cellContent.length();
                    org.apache.poi.ss.usermodel.Font dayFont = col == 0 ? sundayDayFont : col == 6 ? saturdayDayFont : weekdayDayFont;
                    richContent.applyFont(0, dayLabelEnd, dayFont);
                    for (String label : new String[]{"Net", "Shift", "Breaks"}) {
                        int labelStart = cellContent.indexOf(label);
                        if (labelStart >= 0) richContent.applyFont(labelStart, labelStart + label.length(), boldLabelFont);
                    }
                    dateCell.setCellValue(richContent);
                    dateCell.setCellStyle(cellStyle);
                    maxLinesInWeek = Math.max(maxLinesInWeek, estimateExcelLines(cellContent));
                    col++;
                    if (col > 6 && day < daysInMonth) {
                        currentExcelRow.setHeightInPoints(Math.max(48, maxLinesInWeek * 13 + 8));
                        col = 0;
                        currentIdxRow++;
                        currentExcelRow = sheet.createRow(currentIdxRow);
                        currentExcelRow.setHeightInPoints(50);
                        maxLinesInWeek = 1;
                    }
                }
                currentExcelRow.setHeightInPoints(Math.max(48, maxLinesInWeek * 13 + 8));
                while (col <= 6) {
                    org.apache.poi.ss.usermodel.Cell emptyCell = currentExcelRow.createCell(col++);
                    emptyCell.setCellStyle(cellStyle);
                }

                // Keep records from every month in the workbook's detailed history sheet.
                List<Map.Entry<String, TimeRecord>> allRecords = new ArrayList<>(pData.dateRecords.entrySet());
                allRecords.sort(Map.Entry.comparingByKey());
                for (Map.Entry<String, TimeRecord> recordEntry : allRecords) {
                    String recordDate = recordEntry.getKey();
                    TimeRecord record = recordEntry.getValue();
                    if (record == null) continue;
                    if ("CHECKED".equals(record.type)) {
                        addTimeDetailRow(detailsSheet, detailRowIndex++, recordDate, "Checked", "—", record, "08:00", detailCellStyle);
                    } else if ("FROM_TO".equals(record.type)) {
                        int start = parseTimeStringToMinutes(record.fromTime);
                        int end = parseTimeStringToMinutes(record.toTime);
                        int workedMinutes = end >= start ? netWorkMinutes(record) : 0;
                        String workedTime = String.format(Locale.ROOT, "%02d:%02d", workedMinutes / 60, workedMinutes % 60);
                        String workPeriod = Objects.toString(record.fromTime, "") + " – " + Objects.toString(record.toTime, "");
                        addTimeDetailRow(detailsSheet, detailRowIndex++, recordDate, "From/To", workPeriod, record, workedTime, detailCellStyle);
                    } else if ("TOTAL".equals(record.type)) {
                        addTimeDetailRow(detailsSheet, detailRowIndex++, recordDate, "Total time", "—", record,
                                Objects.toString(record.totalTime, ""), detailCellStyle);
                    }
                }

                int statStartRow = currentIdxRow + 2;
                org.apache.poi.ss.usermodel.Row totalDaysRow = sheet.createRow(statStartRow);
                totalDaysRow.setHeightInPoints(22);
                org.apache.poi.ss.usermodel.Cell labelDays = totalDaysRow.createCell(0);
                labelDays.setCellValue(" Total Logged Days");
                labelDays.setCellStyle(footerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(statStartRow, statStartRow, 0, 2));
                org.apache.poi.ss.usermodel.Cell valDays = totalDaysRow.createCell(3);
                valDays.setCellValue(totalDays + " Days completed");
                valDays.setCellStyle(footerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(statStartRow, statStartRow, 3, 6));
                org.apache.poi.ss.usermodel.Row totalTimeRow = sheet.createRow(statStartRow + 1);
                totalTimeRow.setHeightInPoints(22);
                org.apache.poi.ss.usermodel.Cell labelTime = totalTimeRow.createCell(0);
                labelTime.setCellValue(" Total Logged Working Time");
                labelTime.setCellStyle(footerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(statStartRow + 1, statStartRow + 1, 0, 2));
                int finalHours = totalMinutes / 60;
                int finalMins = totalMinutes % 60;
                org.apache.poi.ss.usermodel.Cell valTime = totalTimeRow.createCell(3);
                valTime.setCellValue(String.format("%d Hrs %02d Mins accumulated", finalHours, finalMins));
                valTime.setCellStyle(footerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(statStartRow + 1, statStartRow + 1, 3, 6));
                workbook.setPrintArea(0, 0, 6, 0, statStartRow + 1);
                sheet.setAutobreaks(true);
                sheet.setFitToPage(true);
                sheet.getPrintSetup().setLandscape(true);
                sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
                sheet.getPrintSetup().setFitWidth((short) 1);
                sheet.getPrintSetup().setFitHeight((short) 1);
                workbook.setPrintArea(1, 0, 6, 0, Math.max(1, detailRowIndex - 1));
                detailsSheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1, Math.max(1, detailRowIndex - 1), 0, 6));
                for (int i = 0; i < 7; i++) {
                    sheet.setColumnWidth(i, 4800);
                }
                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // Section: Excel cell formatting helpers
    private void setBorder(org.apache.poi.ss.usermodel.CellStyle style) {
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setTopBorderColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_40_PERCENT.getIndex());
    }

    // Section: Format total work time for display
    private String formatWorkedTimeReadable(int minutes) {
        return (minutes / 60) + "h " + String.format(Locale.ROOT, "%02dm", minutes % 60);
    }

    // Section: Format compact work and break ranges for Excel
    private String formatCompactTimeRange(String from, String to, boolean omitZeroMinutes) {
        String[] fromParts = from.trim().toUpperCase(Locale.ROOT).split("\\s+");
        String[] toParts = to.trim().toUpperCase(Locale.ROOT).split("\\s+");
        String[] fromClock = fromParts[0].split(":");
        String[] toClock = toParts[0].split(":");
        int fromHour = Integer.parseInt(fromClock[0]);
        int fromMinute = Integer.parseInt(fromClock[1]);
        int toHour = Integer.parseInt(toClock[0]);
        int toMinute = Integer.parseInt(toClock[1]);
        String start = omitZeroMinutes && fromMinute == 0 ? Integer.toString(fromHour)
                : String.format(Locale.ROOT, "%d:%02d", fromHour, fromMinute);
        String end = omitZeroMinutes && toMinute == 0 ? Integer.toString(toHour)
                : String.format(Locale.ROOT, "%d:%02d", toHour, toMinute);
        String fromPeriod = fromParts[1], toPeriod = toParts[1];
        if (fromPeriod.equals(toPeriod)) return start + "–" + end + " " + toPeriod;
        return start + " " + fromPeriod + "–" + end + " " + toPeriod;
    }

    // Section: Estimate Excel calendar cell height
    private int estimateExcelLines(String cellText) {
        int lines = 0;
        for (String line : cellText.split("\\n", -1)) lines += Math.max(1, (int) Math.ceil(line.length() / 18.0));
        return lines;
    }

    // Section: Add one detailed work record to the Excel sheet
    private void addTimeDetailRow(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String date,
                                  String type, String workPeriod, TimeRecord record, String workedTime,
                                  org.apache.poi.ss.usermodel.CellStyle style) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24);
        String[] values = {date, type, workPeriod,
                formatBreakPeriod(record, 0), formatBreakPeriod(record, 1), formatBreakPeriod(record, 2), workedTime};
        for (int i = 0; i < values.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            cell.setCellStyle(style);
        }
    }

    // Section: Format a recorded break for Excel
    private String formatBreakPeriod(TimeRecord record, int breakIndex) {
        if (record.breakFromTimes == null || record.breakToTimes == null
                || breakIndex >= record.breakFromTimes.length || breakIndex >= record.breakToTimes.length) return "—";
        String from = record.breakFromTimes[breakIndex], to = record.breakToTimes[breakIndex];
        if (from == null || to == null || from.isBlank() || to.isBlank()) return "—";
        return from + " – " + to;
    }

    @SuppressWarnings("unchecked")
    // Section: Load current and older Excel calendar files
    private void loadDataFromDisk() {
        File dir = new File(STORAGE_DIR_PATH);
        if (!dir.exists() || dir.listFiles() == null) return;
        projectStorage.clear();
        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".xlsx")) {
                String projectName = file.getName().substring(0, file.getName().lastIndexOf(".xlsx"));
                ProjectData pData = new ProjectData(projectName);
                try (FileInputStream fis = new FileInputStream(file);
                     org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis)) {
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                    YearMonth workbookMonth = readWorkbookPeriod(sheet);
                    int rows = sheet.getPhysicalNumberOfRows();
                    for (int r = 3; r < rows; r++) {
                        org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                        if (row == null) continue;
                        for (int c = 0; c < 7; c++) {
                            org.apache.poi.ss.usermodel.Cell dateCell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            if (dateCell != null && dateCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                                String content = dateCell.getStringCellValue();
                                if (content.contains("Day")) {
                                    String[] lines = content.split("\n");
                                    // Compatibility fix: accept both plain Day labels and labels with Net details.
                                    java.util.regex.Matcher dayMatcher = java.util.regex.Pattern
                                            .compile("Day\\s+(\\d+)").matcher(lines[0]);
                                    if (!dayMatcher.find()) continue;
                                    int dayVal;
                                    try { dayVal = Integer.parseInt(dayMatcher.group(1)); }
                                    catch (NumberFormatException ignored) { continue; }
                                    if (dayVal < 1 || dayVal > workbookMonth.lengthOfMonth()) continue;
                                    String dateKey = workbookMonth.atDay(dayVal).toString();
                                    if (lines.length > 1) {
                                        TimeRecord rec = new TimeRecord();
                                        String dataLine = lines[1].trim();
                                        if (dataLine.contains("✔") || dataLine.toLowerCase(Locale.ROOT).contains("(checked)")) {
                                            rec.type = "CHECKED"; rec.isChecked = true;
                                        } else if (dataLine.startsWith("Work: ")) {
                                            rec.type = "FROM_TO";
                                            String[] times = dataLine.substring("Work: ".length()).split(" – ", 2);
                                            if (times.length == 2) { rec.fromTime = times[0]; rec.toTime = times[1]; }
                                            for (int lineIndex = 2; lineIndex < lines.length; lineIndex++) {
                                                String detail = lines[lineIndex].trim();
                                                if (!detail.startsWith("Break ")) continue;
                                                int colon = detail.indexOf(':');
                                                if (colon < 0) continue;
                                                try {
                                                    int breakIndex = Integer.parseInt(detail.substring("Break ".length(), colon).trim()) - 1;
                                                    String[] breakTimes = detail.substring(colon + 1).trim().split(" – ", 2);
                                                    if (breakIndex >= 0 && breakIndex < 3 && breakTimes.length == 2) {
                                                        rec.breakFromTimes[breakIndex] = breakTimes[0];
                                                        rec.breakToTimes[breakIndex] = breakTimes[1];
                                                    }
                                                } catch (NumberFormatException ignored) { }
                                            }
                                        } else if (dataLine.contains("~")) {
                                            rec.type = "FROM_TO";
                                            String timeData = dataLine.replace("[", "").replace("]", "");
                                            String[] segments = timeData.split(";");
                                            String[] times = segments[0].split("~");
                                            if (times.length == 2) { rec.fromTime = times[0]; rec.toTime = times[1]; }
                                            for (int i = 1; i < segments.length; i++) {
                                                String[] namedBreak = segments[i].split("=", 2);
                                                if (namedBreak.length == 2 && namedBreak[0].startsWith("Break time ")) {
                                                    try {
                                                        int breakIndex = Integer.parseInt(namedBreak[0].substring("Break time ".length())) - 1;
                                                        String[] breakTimes = namedBreak[1].split("~", 2);
                                                        if (breakIndex >= 0 && breakIndex < 3 && breakTimes.length == 2) {
                                                            rec.breakFromTimes[breakIndex] = breakTimes[0]; rec.breakToTimes[breakIndex] = breakTimes[1];
                                                        }
                                                    } catch (NumberFormatException ignored) { }
                                                }
                                            }
                                        } else if (dataLine.startsWith("Total time: ")) {
                                            rec.type = "TOTAL";
                                            rec.totalTime = dataLine.substring("Total time: ".length()).trim();
                                        } else if (dataLine.contains("Hrs")) {
                                            rec.type = "TOTAL";
                                            rec.totalTime = dataLine.replace("[", "").replace("]", "").replace("Hrs", "").trim();
                                        }
                                        pData.dateRecords.put(dateKey, rec);
                                    }
                                }
                            }
                        }
                    }
                    if (workbook.getNumberOfSheets() > 1 && "Time Details".equals(workbook.getSheetName(1))) {
                        pData.dateRecords.clear();
                        org.apache.poi.ss.usermodel.Sheet detailSheet = workbook.getSheetAt(1);
                        for (int r = 2; r <= detailSheet.getLastRowNum(); r++) {
                            org.apache.poi.ss.usermodel.Row row = detailSheet.getRow(r);
                            if (row == null || row.getCell(0) == null) continue;
                            String dateKey = row.getCell(0).getStringCellValue();
                            String type = row.getCell(1).getStringCellValue();
                            TimeRecord rec = new TimeRecord();
                            if ("Checked".equals(type)) {
                                rec.type = "CHECKED"; rec.isChecked = true;
                            } else if ("Total time".equals(type)) {
                                rec.type = "TOTAL";
                                rec.totalTime = row.getCell(6).getStringCellValue();
                            } else if ("From/To".equals(type)) {
                                rec.type = "FROM_TO";
                                String[] workTimes = row.getCell(2).getStringCellValue().split(" – ", 2);
                                if (workTimes.length == 2) { rec.fromTime = workTimes[0]; rec.toTime = workTimes[1]; }
                                for (int i = 0; i < 3; i++) {
                                    String breakPeriod = row.getCell(3 + i).getStringCellValue();
                                    if (!"—".equals(breakPeriod)) {
                                        String[] breakTimes = breakPeriod.split(" – ", 2);
                                        if (breakTimes.length == 2) {
                                            rec.breakFromTimes[i] = breakTimes[0]; rec.breakToTimes[i] = breakTimes[1];
                                        }
                                    }
                                }
                            } else continue;
                            pData.dateRecords.put(dateKey, rec);
                        }
                    }
                    projectStorage.put(projectName, pData);
                } catch (Exception e) {
                    // Error handling: report a workbook that could not be read.
                    System.err.println(file.getName() + " 파싱 실패.");
                }
            }
        }
    }
    // Section: Application logo graphic
    public static class AppLogoUI {
        // Section: Draw the calendar logo and status indicator
        public static Pane createLogo(double size, boolean isDarkMode) {
            StackPane logoContainer = new StackPane();
            logoContainer.setPrefSize(size, size);
            logoContainer.setMaxSize(size, size);
            javafx.scene.shape.Circle baseCircle = new javafx.scene.shape.Circle(size / 2);
            baseCircle.setFill(Color.web(isDarkMode ? "#2D2D30" : "#EAEAEA"));
            javafx.scene.shape.SVGPath calIcon = new javafx.scene.shape.SVGPath();
            calIcon.setContent("M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z");
            calIcon.setFill(Color.web("#FF9100"));
            double scale = (size / 24.0) * 0.55;
            calIcon.setScaleX(scale);
            calIcon.setScaleY(scale);
            javafx.scene.shape.Circle checkedDot = new javafx.scene.shape.Circle(size * 0.12);
            checkedDot.setFill(Color.web("#00E5FF"));
            StackPane.setAlignment(checkedDot, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(checkedDot, new Insets(0, size * 0.15, size * 0.15, 0));
            logoContainer.getChildren().addAll(baseCircle, calIcon, checkedDot);
            return logoContainer;
        }
    }
    // Section: Application entry point
    public static void main(String[] args) { launch(args); }
}
