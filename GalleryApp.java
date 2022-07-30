package cs1302.gallery;

import com.google.gson.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * This class represents an iTunes GalleryApp!
 */
public class GalleryApp extends Application {
    // The root container for the application scene graph
    VBox vBox;
    // The container for menu bar
    MenuBar menuBar;
    // The container for play/pause button, search field, and update button
    ToolBar toolBar;
    // The container for progress bar
    HBox hBox;
    // User's input into search field
    String userInput;
    Scene scene;
    Stage stage;
    TilePane tile = new TilePane();
    ProgressBar progressBar = new ProgressBar();
    double progress = 0.0;
    JsonArray unused;
    JsonArray used;
    ImageView pages[] = new ImageView[20];
    Button playPause;
    boolean play = true;
    double playCount = -1.0;
    String[] results;
    Timeline timeline;
    Button updateImage;
    TextField textField;
    JsonArray jsonResults;

    /**
     * @inheritdoc
     */
    @Override
    public void start(Stage stage) {
        vBox = new VBox();
        // Adding components to container
        HBox holder = new HBox();
        vBox.getChildren().addAll(addMenuBar(), addToolBar());
        vBox.getChildren().addAll(addProgressBar());
        Thread task = new Thread(() -> {
            getImages(userInput);
            Platform.runLater(() -> {
                vBox.getChildren().add(updateTilepane());
            });
        });
        task.setDaemon(true);
        task.start();
        scene = new Scene(vBox, 500, 480);
        stage.setTitle("Gallery!");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    } // start

    /**
     * This method creates a MenuBar object and its components to add to the
     * stage.
     *
     * @return
     */
    public MenuBar addMenuBar() {
        // Creates menu option of file and adds to menu bar
        Menu file = new Menu("File");
        Menu theme = new Menu("Theme");
        Menu help = new Menu("Help");
        menuBar = new MenuBar();
        menuBar.getMenus().addAll(file, theme, help);
        // Creates exit option for file and adds to File option
        MenuItem menuItem = new MenuItem("Exit");
        file.getItems().add(menuItem);
        //Creates themes options for theme and adds to Theme option
        MenuItem regular = new MenuItem("Default");
        MenuItem hotPink = new MenuItem("Hot Pink");
        theme.getItems().addAll(regular, hotPink);
        // Creates about option for help and adds to Help option
        MenuItem menuItem2 = new MenuItem("About");
        help.getItems().add(menuItem2);
        // Event Handler for Exit menu item
        menuItem.setOnAction(event -> System.exit(0));
        themeHandlers(regular, hotPink);
        // Event Handler for About menu item
        menuItem2.setOnAction(e -> {
            aboutMe();
        });
        return menuBar;
    }

    /**
     * This method contains the event handlers for the different themes.
     * The styling for the .css files derived from CSS Reference guide.
     * @param regular default theme menu item
     * @param hotPink hot pink theme menu item
     */
    public void themeHandlers(MenuItem regular, MenuItem hotPink) {
        regular.setOnAction(e -> {
            scene.getStylesheets().clear();
            playPause.getStylesheets().clear();
            updateImage.getStylesheets().clear();
            scene.getStylesheets().add("/css/default.css");
        });
        hotPink.setOnAction(e -> {
            scene.getStylesheets().clear();
            playPause.getStylesheets().clear();
            updateImage.getStylesheets().clear();
            scene.getStylesheets().add("/css/hotpink.css");
            playPause.getStylesheets().add("/css/hotpink.css");
            updateImage.getStylesheets().add("/css/hotpink.css");
        });
    }

    /**
     * This method handles the event for when the About item is pressed.
     */
    public void aboutMe() {
        Stage dialog = new Stage();
        String myPic = "https://pbs.twimg.com/profile_images/1086394759067901952" +
                "/VyWYin8l_400x400.jpg";
        dialog.setTitle("About Apoorva Dhanala");
        VBox info = new VBox();
        Scene infoScene = new Scene(info);
        Text basicInfo = new Text();
        basicInfo.setText(
                " Apoorva Dhanala \n apoorva.dhanala@uga.edu \n Application Version 1.0");
        ImageView myPicture = new ImageView(myPic);
        info.getChildren().addAll(basicInfo, myPicture);
        dialog.setScene(infoScene);
        dialog.sizeToScene();
        dialog.showAndWait();
    }

    /**
     * This method reads in the user's search input from the text box.
     *
     * @param textField the textfield in menubar with user input
     * @return
     */
    public String parseInput(TextField textField) {
        userInput = textField.getText();
        //holds are words of user input separately
        String[] words = userInput.split(" ");
        userInput = "";
        //adds individual search words to string
        for (int i = 0; i < words.length; i++) {
            if (i == 0) {
                userInput = userInput + words[i];
                continue;
            }
            userInput = userInput + "+" + words[i];
        }
        progress = 0.0;
        return userInput;
    }

    /**
     * This method calls the method that adds a tool bar along
     * with handling events for pressing the different components.
     */
    public ToolBar addToolBar() {
        createToolBarParts();
        // Event Handler for play/pause button
        playPause.setOnAction(e -> {
            randomImageChange();
        });
        updateImage.setOnAction(e -> {
            updateImage.setStyle("-fx-background: #FF0000;");
            boolean wasRunning = false;
            //if timeline has been activated and running, pause it
            if (timeline != null) {
                if (timeline.getStatus() == Animation.Status.RUNNING) {
                    wasRunning = true;
                    timeline.pause();
                }
            }
            String newInput = parseInput(textField);
            progressBar.setProgress(0.0);
            Thread task = new Thread(() -> {
                getImages(newInput);
                Platform.runLater(() -> {
                    updateTilepane();
                });
            });
            task.setDaemon(true);
            task.start();
            //if the timeline was running and paused, play it again
            if (wasRunning) {
                timeline.play();
            }
        });
        return toolBar;
    }

    /**
     * This method creates the different components of the tool bar and
     * adds them to it.
     */
    public void createToolBarParts() {
        // Creates tool bar and adds buttons and text field
        toolBar = new ToolBar();
        playPause = new Button("Play");
        Label searchQuery = new Label("Search Query:");
        textField = new TextField("unicorn");
        userInput = parseInput(textField);
        updateImage = new Button("Update Images");
        toolBar.getItems().addAll(playPause, searchQuery, textField,
                updateImage);
    }

    /**
     * This method helps the event handler for the play/pause button.
     */
    public void randomImageChange() {
        playCount++;
        //if playCount is even
        if (playCount % 2 == 0.0) {
            play = true;
        } //if playCount is odd
        if (playCount % 2 != 0.0) {
            play = false;
        }
        Platform.runLater(() -> {
            //if on play, change button and play timeline
            if (play) {
                playPause.setText("Pause");
                timeline.play();
            } //if on pause, change button and pause timeline
            if (!play) {
                playPause.setText("Play");
                timeline.pause();
                return;
            }
        });
        if (play) {
            ifPlay();
        }
    }

    /**
     * This method randomly replaces images in the tile pane on a timeline.
     */
    public void ifPlay() {
        EventHandler<ActionEvent> handler = (e -> {
            if (jsonResults.size() > 21) {
                //if button is on pause, pause timeline
                if (playPause.getText() == "Play") {
                    timeline.pause();
                    return;
                }
                //get random member not being displayed
                int randomUnusedMember = (int) (Math.random()
                        * unused.size());
                //get random member being displayed
                int randomDisplayingMember = (int) (Math.random()
                        * pages.length);
                // object random number in array
                JsonObject result = unused.get(randomUnusedMember)
                        .getAsJsonObject();
                JsonElement artworkUrl100 = result.get("artworkUrl100");
                if (artworkUrl100 != null) {
                    randomReplacement(artworkUrl100, randomDisplayingMember);
                }
                if (used.contains(unused.get(randomUnusedMember))) {
                    unused.remove(unused.get(randomUnusedMember));
                }
            }
        });
        setTimeline(handler);
    }

    /**
     * This method chooses a random image being displayed to replace with an unused one
     * and sets it into the ImageView object and re-adds all ImageView objects to TilePane.
     *
     * @param artworkUrl100          Json Element of newly introduced member
     * @param randomDisplayingMember integer of new member in unused array
     */
    public void randomReplacement(JsonElement artworkUrl100, int randomDisplayingMember) {
        //take new member and add to used
        used.add(artworkUrl100);
        String artUrl = artworkUrl100.getAsString();
        Image image = new Image(artUrl);
        //create new imageview object in place of old member
        pages[randomDisplayingMember] = new ImageView();
        //store random member being displayed back in unused
        unused.add(jsonResults.get(randomDisplayingMember));
        pages[randomDisplayingMember].setImage(image);
        pages[randomDisplayingMember].setFitHeight(100.0);
        pages[randomDisplayingMember].setFitWidth(100.0);
        tile.getChildren().clear();
        //add ImageView object to tile pane object
        for (int i = 0; i < 20; i++) {
            tile.getChildren().add(pages[i]);
        }
    }

    /**
     * This method sets the timeline for the random image replacement.
     *
     * @param handler the lambda expression that the timeline carries out
     */
    public void setTimeline(EventHandler<ActionEvent> handler) {
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(2), handler);
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    /**
     * This method reads in the JSON query results and parses them.
     *
     * @param userInput the users newly entered search
     */
    public void getImages(String userInput) {
        //set tile pane to always show 5 col, 4 rows
        tile.setPrefColumns(5);
        tile.setPrefRows(4);
        InputStreamReader reader = null;
        URL url = null;
        //put user input into default itunes search url
        String queryString = "https://itunes.apple.com/search?term="
                + userInput;
        encodeValue(queryString);
        readAndParse(url, queryString, reader);
        //if less than 21 results gathered
        if (results.length < 21) {
            Platform.runLater(() -> {
                //show dialog box/popup mentioning the error
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "There are not enough results. Displaying previous.",
                        ButtonType.OK);
                alert.showAndWait();
                playPause.setText("Play");
            });
            return;
        }
        for (int i = 0; i < 20; i++) {
            importResults(i);
        }
    }

    /**
     * This is a helper method to read in the JSON query results and parse them.
     *
     * @param url         the link containing the image
     * @param queryString the string that will contain the parsed url link
     * @param reader      a reader which passes through the query results
     */
    public void readAndParse(URL url, String queryString, InputStreamReader reader) {
        try {
            url = new URL(queryString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            reader = new InputStreamReader(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(reader);
        // root of response
        JsonObject root = je.getAsJsonObject();
        // "results" array
        jsonResults = root.getAsJsonArray("results");
        // second copy of all members
        unused = root.getAsJsonArray("results");
        // will hold all members being displayed
        used = new JsonArray();
        results = new String[jsonResults.size()];
    }

    /**
     * This method fills the results array with the string urls
     * from the JSON query results.
     *
     * @param i the number of the image url being imported
     */
    public void importResults(int i) {
        JsonObject result = jsonResults.get(i).getAsJsonObject();
        used.add(result);
        JsonElement artworkUrl100 = result.get("artworkUrl100");
        if (artworkUrl100 != null) { // member might not exist
            String artUrl = artworkUrl100.getAsString();
            Image image = new Image(artUrl);
            results[i] = artUrl;
            pages[i] = new ImageView();
            pages[i].setImage(new Image(results[i]));
            Platform.runLater(() -> incrementProgress());
        }
    }

    /**
     * This method updates the images with results from the new user input.
     */
    public TilePane updateTilepane() {
        int numResults = results.length;
        if (numResults < 21) {
            return tile;
        }
        //resets tile pane before adding new images
        tile.getChildren().clear();
        progress = 0.0;
        for (int i = 0; i < 20; i++) {
            pages[i].setImage(new Image(results[i]));
            pages[i].setFitWidth(100.0);
            pages[i].setFitHeight(100.0);
            tile.getChildren().add(pages[i]);
        }
        for (int x = 0; x < unused.size(); x++) {
            if (used.contains(unused.get(x))) {
                unused.remove(unused.get(x));
            }
        }
        return tile;
    }

    /**
     * This method encodes values in the URL query string.
     *
     * @param value the string to be encoded
     * @return
     */
    public String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * This method displays the progress of the images being gathered from the
     * query response in the progress bar.
     */
    public void incrementProgress() {
        //every time method is called, increment progress by 0.05
        progress = progress + 0.05;
        progressBar.setProgress(progress);
    }

    /**
     * This method creates a progress bar to add to the stage.
     */
    public HBox addProgressBar() {
        hBox = new HBox();
        progressBar.setLayoutX(25.0);
        progressBar.setLayoutY(550.0);
        Label label = new Label("Images provided courtesy of iTunes");
        hBox.getChildren().addAll(progressBar, label);
        return hBox;
    }
} // GalleryApp
