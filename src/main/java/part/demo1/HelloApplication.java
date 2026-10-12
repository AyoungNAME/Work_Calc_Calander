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

    // 📂 문서 폴더 하위에 새로운 앱 명칭으로 저장 폴더 트리를 완전 자동 빌드합니다.
    private static final String STORAGE_DIR_PATH = System.getProperty("user.home") + File.separator
            + "Documents" + File.separator + "Work Calc Calendar" + File.separator + "Saved Calendars";

    private Map<String, ProjectData> projectStorage = new HashMap<>();
    private YearMonth currentYearMonth = YearMonth.now();

    // [데이터 저장소] 프로젝트 이름을 관리하는 클래스
    public static class ProjectData implements Serializable {
        private static final long serialVersionUID = 500L;
        public String projectName;
        public Map<String, TimeRecord> dateRecords = new HashMap<>();

        public ProjectData(String projectName) {
            this.projectName = projectName;
        }
    }

    // [시간 데이터 구조] 사용자가 저장한 데이터 바구니
    public static class TimeRecord implements Serializable {
        private static final long serialVersionUID = 500L;
        public String type = "";        // "FROM_TO" or "TOTAL" or "CHECKED"
        public String fromTime = "";
        public String toTime = "";
        public String totalTime = "";
        public boolean isChecked = false;
    }

    @Override
    public void start(Stage loadingStage) {
        initializeStorageDirectory();
        loadDataFromDisk();

        // 🎯 [자바 코드 레벨 실행 앱 아이콘 버그 완전 분쇄]
        // 런타임 가동 즉시 assets 폴더 내부의 ico/icns 파일을 감지하여 윈도우/맥 가상 프레임 및 독바에 고정합니다.
        try {
            boolean isDarkMode = checkOSDarkMode();
            File iconFile = new File("assets" + File.separator + "icon.icns");

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                iconFile = new File("assets" + File.separator + "icon.ico");
            }

            if (iconFile.exists()) {
                javafx.scene.image.Image appIcon = new javafx.scene.image.Image(iconFile.toURI().toString());
                loadingStage.getIcons().add(appIcon);

                // 🍏 맥(macOS) 전용 독(Dock) 바 세팅 동기화 가동
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    try {
                        java.awt.image.BufferedImage awtImage = javax.imageio.ImageIO.read(iconFile);
                        if (awtImage != null) {
                            java.awt.Taskbar.getTaskbar().setIconImage(awtImage);
                        }
                    } catch (Exception ex) {
                        // 맥 보안 샌드박스 예외 펜스
                    }
                }
            } else {
                Pane systemIconLogo = AppLogoUI.createLogo(128, isDarkMode);
                javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                javafx.scene.image.WritableImage snapshot = systemIconLogo.snapshot(params, null);
                loadingStage.getIcons().add(snapshot);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 👉 이 아래에 있는 loadingStage.initStyle(StageStyle.TRANSPARENT);

        loadingStage.initStyle(StageStyle.TRANSPARENT);
        boolean isDarkMode = checkOSDarkMode();
        String backgroundColor = isDarkMode ? "#1E1E1E" : "#FFFFFF";
        String indicatorColor = isDarkMode ? "white" : "black";

        // 🌓 [UI 테두리선 패치] 로딩 스플래시 화면 반대 색상 아웃라인 주입
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(60, 60);
        progressIndicator.setStyle("-fx-progress-color: " + indicatorColor + ";");

        StackPane root = new StackPane(progressIndicator);
        root.setPrefSize(250, 250);
        root.setStyle("-fx-background-color: " + backgroundColor + ";"
                + " -fx-background-radius: 25px; -fx-border-radius: 25px;"
                + " -fx-border-color: " + borderColor + "; -fx-border-width: 1px;" // 반전 테두리선 추가
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

    private void initializeStorageDirectory() {
        try {
            File dir = new File(STORAGE_DIR_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkOSDarkMode() {
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
        } catch (Exception e) { return false; }
        return false;
    }

    private void showMainWindow(boolean isDarkMode) {
        try {
            Stage mainStage = new Stage();
            mainStage.initStyle(StageStyle.TRANSPARENT);

            BorderPane wrapperLayout = new BorderPane();
            String mainBgColor = isDarkMode ? "#2D2D2D" : "#FFFFFF";
            String btnColor = isDarkMode ? "#FFFFFF" : "#333333";
            String textColor = isDarkMode ? "#FFFFFF" : "#111111";

            // 🌓 [UI 테두리선 패치] 메인 화면에 반대 색상 테두리 적용
            String borderColor = isDarkMode ? "#FFFFFF" : "#000000";

            String closeBtnHoverBg = isDarkMode ? "rgba(255, 255, 255, 0.15)" : "rgba(0, 0, 0, 0.08)";
            String closeBtnHoverText = isDarkMode ? "#FF5252" : "#C62828";

            wrapperLayout.setStyle("-fx-background-color: " + mainBgColor + ";"
                    + " -fx-background-radius: 20px; -fx-border-radius: 20px;"
                    + " -fx-border-color: " + borderColor + "; -fx-border-width: 1px;" // 반전 테두리선 추가
                    + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);");

            // ❌ [닫기 버튼 깨짐 패치] 특수문자 '✕' 대신 알파벳 대문자 'X'를 사용해 말줄임표(...) 깨짐을 방지합니다.
            Button closeButton = new Button("X");
            closeButton.setPrefSize(32, 32);
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

            Button leftOptionBtn = new Button("New File");
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
            Scene scene = new Scene(wrapperLayout, 600, 400);
            scene.setFill(Color.TRANSPARENT);
            mainStage.setScene(scene);
            mainStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
    private void handleNewCalendarWindow(Stage ownerStage, boolean isDarkMode) {
        Stage dialogStage = new Stage();
        dialogStage.initOwner(ownerStage);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        String bg = isDarkMode ? "#252526" : "#FAFAFA";
        String txtColor = isDarkMode ? "#FFFFFF" : "#111111";
        String inputBg = isDarkMode ? "#3E3E3F" : "#FFFFFF";
        String inputBorder = isDarkMode ? "#555555" : "#CCCCCC";
// 🌓 [UI 테두리선 패치] 새 파일 모달창에 반대 색상 테두리 적용
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 15px; -fx-border-radius: 15px; "
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 1px;" // 반전 테두리선 추가
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 12, 0, 0, 0);");
        Label mainPrompt = new Label("Create a new work session name");
        mainPrompt.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 15px; -fx-font-weight: bold;");
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
        Button cancelBtn = new Button("Cancel");
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
            String projectName = nameField.getText().trim();
            if (!projectName.isEmpty()) {
                ProjectData newData = new ProjectData(projectName);
                projectStorage.put(projectName, newData);
                saveDataToDisk();
                dialogStage.close();
                openCalendarWindow(ownerStage, isDarkMode, newData);
            }
        });
        nameField.setOnAction(e -> okBtn.getOnAction().handle(e));
        Scene scene = new Scene(layout, 380, 220);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }
    private void handleContinueFileWindow(Stage ownerStage, boolean isDarkMode) {
        Stage selectionStage = new Stage();
        selectionStage.initStyle(StageStyle.TRANSPARENT);
        selectionStage.initOwner(ownerStage);
        BorderPane layout = new BorderPane();
        String bg = isDarkMode ? "#222222" : "#F5F5F5";
        String txtColor = isDarkMode ? "white" : "black";
// 🌓 [UI 테두리선 패치] 이어하기 파일 관리 창에 반대 색상 테두리 적용
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        layout.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 15px; -fx-border-radius: 15px;"
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 1px;" // 반전 테두리선 추가
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        Label titleLabel = new Label("Select File to Continue");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + txtColor + ";");
        // 🟢 기존의 중복된 3줄을 지우고, 이 코드로 깔끔하게 대체해 줍니다.
        ListView<String> projectListView = new ListView<>();
        projectListView.getItems().clear(); // 이전 스캔 잔상을 깨끗이 청소
        projectListView.getItems().addAll(projectStorage.keySet()); // 단 한 번만 깨끗하게 추가
        // 🟢 projectListView 선언부에도 <String>이 있는지 확인하시고, Factory 시작점을 아래와 같이 고쳐줍니다.
        projectListView.setCellFactory(lv -> new ListCell<String>() {
            private final Label mainPart = new Label();
            private final Label slashPart = new Label(" / ");
            private final Label daysPart = new Label();
            private final HBox rowContainer = new HBox(mainPart, slashPart, daysPart);
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

            // 🎯 아래의 updateItem 메서드가 이제 부모의 String 타입을 정상 오버라이드하게 됩니다!
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
                                if (end >= start) totalMins += (end - start);
                            } else if ("TOTAL".equals(rec.type)) {
                                totalMins += parseDurationStringToMinutes(rec.totalTime);
                            }
                        }
                    }
                    // 🟢 기존의 3줄을 완벽하게 대체하는 '기록된 날짜 개수(Filled Dates)' 스케일러 엔진
                    int hours = totalMins / 60;
                    int mins = totalMins % 60;

                    int filledDaysCount = 0;
                    if (pData != null && pData.dateRecords != null) {
                        // 출퇴근 데이터 장부(dateRecords)에 등록된 고유 날짜의 총 개수를 일수로 다이렉트 채택합니다.
                        filledDaysCount = pData.dateRecords.size();
                    }

                    mainPart.setText(String.format("%s — %d Hrs %02d Mins", pName, hours, mins));

                    // 🎯 이제 하루에 10분을 기록했든 2시간 45분을 기록했든, 데이터가 채워진 날짜가 2일이라면 "2 Days"로 정직하게 출력됩니다!
                    daysPart.setText(String.format("%d Days", filledDaysCount));

                    // 🎯 변수명을 filledDaysCount로 일치시켜 'cannot find symbol: variable days' 에러를 전면 파쇄합니다.
                    mainPart.setText(String.format("%s — %d Hrs %02d Mins", pName, hours, mins));
                    daysPart.setText(String.format("%d Days", filledDaysCount));
                    mainPart.setFont(Font.font("System", FontWeight.BOLD, 13));
                    slashPart.setFont(Font.font("System", FontWeight.NORMAL, 13));
                    daysPart.setFont(Font.font("System", FontWeight.BOLD, 13));
                    if (isSelected()) {
                        separatorLine.setVisible(false);
                        if (isDarkMode) {
                            rowContainer.setStyle("-fx-background-color: #1A1A1A; -fx-background-radius: 4px; -fx-border-color: #FF9100; -fx-border-width: 1px; -fx-border-radius: 4px;");
                            mainPart.setStyle("-fx-text-fill: #E0E0E0;"); slashPart.setStyle("-fx-text-fill: #666666;"); daysPart.setStyle("-fx-text-fill: #B0BEC5;");
                        } else {
                            rowContainer.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 4px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 6, 0, 0, 2);");
                            mainPart.setStyle("-fx-text-fill: #222222;"); slashPart.setStyle("-fx-text-fill: #888888;"); daysPart.setStyle("-fx-text-fill: #000000;");
                        }
                    } else {
                        separatorLine.setVisible(true);
                        rowContainer.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-effect: null;");
                        mainPart.setStyle("-fx-text-fill: #FF9100;"); slashPart.setStyle("-fx-text-fill: #888888;"); daysPart.setStyle("-fx-text-fill: #00E5FF;");
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
        Button openBtn = new Button("Open Calendar");
        openBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        openBtn.setDisable(true);

        // 🎯 제네릭이 온전하게 맵핑되어 이제 newVal이 String으로 안전하게 파싱됩니다.
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
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> selectionStage.close());
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
    private void openCalendarWindow(Stage ownerStage, boolean isDarkMode, ProjectData projectData) {
        Stage calStage = new Stage();
        calStage.initStyle(StageStyle.TRANSPARENT);
        calStage.initOwner(ownerStage);
        BorderPane mainLayout = new BorderPane();
        String panelBg = isDarkMode ? "#252526" : "#FAFAFA";
        String txtColor = isDarkMode ? "#FFFFFF" : "#111111";
        String comboStyle = isDarkMode
                ? "-fx-background-color: #3E3E3F; -fx-control-inner-background: #3E3E3F; -fx-text-fill: white; -fx-prompt-text-fill: white; -fx-mark-color: white; -fx-text-base-color: white;"
                : "-fx-background-color: #EAEAEA; -fx-control-inner-background: #FFFFFF; -fx-text-fill: black; -fx-prompt-text-fill: black; -fx-mark-color: black; -fx-text-base-color: black;";
// 🌓 [UI 테두리선 패치] 작업 달력 대시보드 화면에 반대 색상 테두리 적용
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        mainLayout.setStyle("-fx-background-color: " + panelBg + "; -fx-background-radius: 15px; -fx-border-radius: 15px;"
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 1px;" // 반전 테두리선 추가
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
        for (int y = 2020; y <= 2035; y++) yearChooser.getItems().add(y);
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
                            if (end >= start) totalMinutes += (end - start);
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
        Scene scene = new Scene(mainLayout, 450, 520);
        scene.setFill(Color.TRANSPARENT);
        calStage.setScene(scene);
        calStage.show();
    }
    private void openTimeFeatureDialog(Stage parentStage, boolean isDarkMode, String dateKey, ProjectData projectData, Runnable onSaveSuccess) {
        // 🟢 1. OS 순정 타이틀바를 완전히 삭제하여 투명한 커스텀 도화지 레이어로 만듭니다.
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initOwner(parentStage);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        // 🌓 [UI 테두리선 패치] 다크/라이트 테마 반전 색상 아웃라인을 정밀 계산 주입합니다.
        String borderColor = isDarkMode ? "#FFFFFF" : "#000000";
        String panelBg = isDarkMode ? "#2D2D2D" : "#FFFFFF";
        String txtColor = isDarkMode ? "white" : "black";
        String btnColor = isDarkMode ? "#FFFFFF" : "#333333";

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // 🎨 메인 홈화면들과 완전히 똑같은 모서리 라운딩(15px) 및 반전 색상 고대비 아웃라인 결합
        layout.setStyle("-fx-background-color: " + panelBg + ";"
                + " -fx-background-radius: 15px; -fx-border-radius: 15px;"
                + " -fx-border-color: " + borderColor + "; -fx-border-width: 1px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

        // ❌ [닫기 버튼 글꼴 픽스] 특수문자 깨짐을 원천 봉쇄하는 시그니처 대문자 'X' 헤더를 빌드합니다.
        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + txtColor + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-family: 'Arial';");
        closeBtn.setOnAction(e -> dialogStage.close());

        Label headerTitle = new Label("Time Logger [" + dateKey + "]");
        headerTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: " + txtColor + "; -fx-font-size: 14px;");

        BorderPane dialogHeaderBar = new BorderPane();
        dialogHeaderBar.setLeft(headerTitle);
        dialogHeaderBar.setRight(closeBtn);
        dialogHeaderBar.setPadding(new Insets(0, 0, 10, 0));

        // 🖱️ 커스텀 타이틀바 영역을 마우스로 붙잡고 윈도우 화면 어디든 이동할 수 있는 드래그 센서 연동
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
        RadioButton rbFromTo = new RadioButton("From Time to This Time");
        RadioButton rbTotal = new RadioButton("Total Time");
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

        List<String> durationList = new ArrayList<>();
        for (int h = 0; h <= 24; h++) {
            for (int m = 0; m < 60; m += 15) {
                if (h == 0 && m == 0) continue;
                durationList.add(String.format("%02d:%02d", h, m)); if (h == 24) break;
            }
        }
        // 🟢 기존 구역을 완벽하게 대체하는 '고대비 고가시성 콤보박스' 렌더링 엔진
        ComboBox<String> cbTotalDuration = new ComboBox<>();
        cbTotalDuration.getItems().addAll(durationList);
        cbTotalDuration.setValue("08:00");

        // 🎨 [가독성 버그 완전 해결] 화살표 색상(-fx-mark-color)과 글자색(-fx-text-base-color)을 완벽하게 반전시킵니다.
        String comboStyle = isDarkMode
                ? "-fx-background-color: #3E3E3F; -fx-text-fill: white; -fx-control-inner-background: #3E3E3F; -fx-text-base-color: white; -fx-mark-color: white;"
                : "-fx-background-color: #EAEAEA; -fx-text-fill: black; -fx-control-inner-background: #FFFFFF; -fx-text-base-color: black; -fx-mark-color: black;";

        cbFrom.setStyle(comboStyle);
        cbTo.setStyle(comboStyle);
        cbTotalDuration.setStyle(comboStyle);

        // 🎨 [다크모드 가독성 패치] 묻혀있던 가이드 글자들을 선명한 흰색/밝은회색 계열로 변환합니다.
        Label lblFrom = new Label("From:"); lblFrom.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");
        Label lblTo = new Label("To:"); lblTo.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");
        Label lblDuration = new Label("Duration:"); lblDuration.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");

        // 고가시성 라벨 객체들을 정밀 조립 바인딩합니다.
        HBox hboxFromTo = new HBox(8, lblFrom, cbFrom, lblTo, cbTo);
        HBox hboxTotal = new HBox(8, lblDuration, cbTotalDuration);

        // 🎯 [투명도 밸런스 패치] 선택되지 않아 비활성화(Disable) 상태일 때 글자가 아예 증발하지 않고 은은하게 회색으로 보이도록 조율합니다.
        hboxFromTo.disableProperty().addListener((obs, oldV, newV) -> hboxFromTo.setOpacity(newV ? 0.35 : 1.0));
        hboxTotal.disableProperty().addListener((obs, oldV, newV) -> hboxTotal.setOpacity(newV ? 0.35 : 1.0));

        hboxFromTo.disableProperty().bind(rbFromTo.selectedProperty().not());
        hboxTotal.disableProperty().bind(rbTotal.selectedProperty().not());

        TimeRecord existing = projectData.dateRecords.get(dateKey);
        if (existing != null) {
            if ("FROM_TO".equals(existing.type)) { rbFromTo.setSelected(true); cbFrom.setValue(existing.fromTime); cbTo.setValue(existing.toTime); }
            else if ("TOTAL".equals(existing.type)) { rbTotal.setSelected(true); cbTotalDuration.setValue(existing.totalTime); }
            else if ("CHECKED".equals(existing.type)) rbChecked.setSelected(true);
        } else rbFromTo.setSelected(true);

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

        Button saveBtn = new Button("Save Record"); saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            TimeRecord record = new TimeRecord();
            if (rbFromTo.isSelected()) { record.type = "FROM_TO"; record.fromTime = cbFrom.getValue(); record.toTime = cbTo.getValue(); }
            else if (rbTotal.isSelected()) { record.type = "TOTAL"; record.totalTime = cbTotalDuration.getValue(); }
            else if (rbChecked.isSelected()) { record.type = "CHECKED"; record.isChecked = true; }
            projectData.dateRecords.put(dateKey, record); saveDataToDisk(); onSaveSuccess.run(); dialogStage.close();
        });

        HBox btnContainer = new HBox(15, deleteBtn, saveBtn); btnContainer.setAlignment(Pos.CENTER);

        // 🟢 2. 가상 헤더와 컨트롤 컴포넌트들을 차례대로 주입 조립합니다.
        layout.getChildren().addAll(dialogHeaderBar, mainPrompt, rbFromTo, hboxFromTo, rbTotal, hboxTotal, rbChecked, btnContainer);

        Scene scene = new Scene(layout, 380, 420);
        // 외곽의 하얀색 네모 상자 파편을 날려버리는 배경 알파 채널 투명화 투하
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.show();
    }
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
    private int parseDurationStringToMinutes(String durationStr) {
        try {
            if (durationStr == null || !durationStr.contains(":")) return 0;
            String colonSplit[] = durationStr.split(":");
            int hour = Integer.parseInt(colonSplit[0]);
            int min = Integer.parseInt(colonSplit[1]);
            return (hour * 60) + min;
        } catch (Exception e) { return 0; }
    }
    private void saveDataToDisk() {
        for (Map.Entry<String, ProjectData> entry : projectStorage.entrySet()) {
            String projectName = entry.getKey();
            ProjectData pData = entry.getValue();
            File file = new File(STORAGE_DIR_PATH + File.separator + projectName + ".xlsx");
            try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(currentYearMonth.getMonth().name());
                sheet.setFitToPage(true);
                org.apache.poi.ss.usermodel.PrintSetup printSetup = sheet.getPrintSetup();
                printSetup.setFitWidth((short) 1);
                printSetup.setFitHeight((short) 1);
                printSetup.setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
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
                setBorder(cellStyle);
                org.apache.poi.ss.usermodel.Font footerFont = workbook.createFont();
                footerFont.setBold(true);
                org.apache.poi.ss.usermodel.CellStyle footerStyle = workbook.createCellStyle();
                footerStyle.setFont(footerFont);
                footerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
                footerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                setBorder(footerStyle);
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
                int col = startColumn;
                for (int day = 1; day <= daysInMonth; day++) {
                    String dKey = currentYearMonth.atDay(day).toString();
                    org.apache.poi.ss.usermodel.Cell dateCell = currentExcelRow.createCell(col);
                    String cellContent = " Day " + day + "\n";
                    if (pData.dateRecords.containsKey(dKey)) {
                        TimeRecord rec = pData.dateRecords.get(dKey);
                        if (rec != null) {
                            totalDays++;
                            if ("CHECKED".equals(rec.type)) {
                                totalMinutes += (8 * 60);
                                cellContent += " [✔ 08:00]";
                            } else if ("FROM_TO".equals(rec.type)) {
                                int start = parseTimeStringToMinutes(rec.fromTime);
                                int end = parseTimeStringToMinutes(rec.toTime);
                                if (end >= start) totalMinutes += (end - start);
                                cellContent += " [" + rec.fromTime + "~" + rec.toTime + "]";
                            } else if ("TOTAL".equals(rec.type)) {
                                totalMinutes += parseDurationStringToMinutes(rec.totalTime);
                                cellContent += " [" + rec.totalTime + " Hrs]";
                            }
                        }
                    }
                    dateCell.setCellValue(cellContent);
                    dateCell.setCellStyle(cellStyle);
                    col++;
                    if (col > 6 && day < daysInMonth) {
                        col = 0;
                        currentIdxRow++;
                        currentExcelRow = sheet.createRow(currentIdxRow);
                        currentExcelRow.setHeightInPoints(50);
                    }
                }
                while (col <= 6) {
                    org.apache.poi.ss.usermodel.Cell emptyCell = currentExcelRow.createCell(col++);
                    emptyCell.setCellStyle(cellStyle);
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
    @SuppressWarnings("unchecked")
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
                    int rows = sheet.getPhysicalNumberOfRows();
                    for (int r = 3; r < rows; r++) {
                        org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                        if (row == null) continue;
                        for (int c = 0; c < 7; c++) {
// 🛠️ 🟢 [최종 정렬 픽스] Row.MissingCellPolicy 파싱 규칙을 완벽하게 통합 명시
                            org.apache.poi.ss.usermodel.Cell dateCell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            if (dateCell != null && dateCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                                String content = dateCell.getStringCellValue();
                                if (content.contains("Day")) {
                                    String[] lines = content.split("\n");
                                    String dayStr = lines[0].replace("Day", "").trim();
                                    int dayVal = Integer.parseInt(dayStr);
                                    String dateKey = currentYearMonth.atDay(dayVal).toString();
                                    if (lines.length > 1) {
                                        TimeRecord rec = new TimeRecord();
                                        String dataLine = lines[1].trim();
                                        if (dataLine.contains("✔")) {
                                            rec.type = "CHECKED"; rec.isChecked = true;
                                        } else if (dataLine.contains("~")) {
                                            rec.type = "FROM_TO";
                                            String[] times = dataLine.replace("[", "").replace("]", "").split("~");
                                            if (times.length == 2) { rec.fromTime = times[0]; rec.toTime = times[1]; }
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
                    projectStorage.put(projectName, pData);
                } catch (Exception e) { System.err.println(file.getName() + " 파싱 실패."); }
            }
        }
    }
    public static class AppLogoUI {
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
// 🟢 오른쪽 하단 민트색 점 인디케이터 연동 복구 완료
            javafx.scene.shape.Circle checkedDot = new javafx.scene.shape.Circle(size * 0.12);
            checkedDot.setFill(Color.web("#00E5FF"));
            StackPane.setAlignment(checkedDot, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(checkedDot, new Insets(0, size * 0.15, size * 0.15, 0));
            logoContainer.getChildren().addAll(baseCircle, calIcon, checkedDot);
            return logoContainer;
        }
    }
    public static void main(String[] args) { launch(args); }
}