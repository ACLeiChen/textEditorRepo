package editor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

//cannot use the TextFlow, TextArea, TextInputControl, or HTMLEditor classes

public class Editor extends Application {
    private final Rectangle textBoundingBox;
    private static int WINDOW_WIDTH = 500;
    private static int WINDOW_HEIGHT = 500;

    private static final int STARTING_FONT_SIZE = 40;
    private static final int STARTING_TEXT_POSITION_X = 250;
    private static final int STARTING_TEXT_POSITION_Y = 250;
    private static int fontSize = STARTING_FONT_SIZE;
    private static String fontName = "Verdana";

    Group root;
    private TextList textList;
    private LinkedList<Character> charReadList;
    //private int cursorX;
    //private int cursorY;
    private int firstLineY;
    private String inputFilename;
    private String outputFilename;

    public Editor() {
        // Create a rectangle to surround the text that gets displayed.  Initialize it with a size
        // of 0, since there isn't any text yet.
        root = new Group();
        textBoundingBox = new Rectangle(0, 0);
        /**Storing the textList of the document. Initialize it with an empty LinkedList.*/
        textList = new TextList();
        charReadList= new LinkedList<Character>();
        //cursorX = 0;
        //cursorY = 0;
        //firstLineY = 0;
    }
    /**
     * An EventHandler to handle keys that get pressed.
     */
    private class KeyEventHandler implements EventHandler<KeyEvent> {
        int textCenterX;
        int textCenterY;

        /*****
         * private Text displayText = new Text(STARTING_TEXT_POSITION_X, STARTING_TEXT_POSITION_Y, "");
         *****/
        KeyEventHandler(final Group root, int windowWidth, int windowHeight) {
            textCenterX = 0;
            textCenterY = 0;

            /***********
             // Initialize some empty text and add it to root so that it will be displayed.
             displayText = new Text(textCenterX, textCenterY, "");
             // Always set the text origin to be VPos.TOP! Setting the origin to be VPos.TOP means
             // that when the text is assigned a y-position, that position corresponds to the
             // highest position across all letters (for example, the top of a letter like "I", as
             // opposed to the top of a letter like "e"), which makes calculating positions much
             // simpler!
             displayText.setTextOrigin(VPos.TOP);
             displayText.setFont(Font.font(fontName, fontSize));

             // All new Nodes need to be added to the root in order to be displayed.
             root.getChildren().add(displayText);
             ***********/
        }
        @Override
        public void handle(KeyEvent keyEvent) {
            if ((keyEvent.getEventType() == KeyEvent.KEY_TYPED)) {
                // Use the KEY_TYPED event rather than KEY_PRESSED for letter keys, because with
                // the KEY_TYPED event, javafx handles the "Shift" key and associated
                // capitalization.
                String characterTyped = keyEvent.getCharacter();
                // Ignore control keys, which have non-zero length, as well as the backspace
                // key, which is represented as a character of value = 8 on Windows.
                if (characterTyped.length() > 0 && characterTyped.charAt(0) != 8) {
                    //1. if a KEY_TYPED event is equal to "\r", that means "enter" key,
                    //but you should use "\n" for all newlines to write a file.
                    //2. "==" this operator will test for reference equality,
                    //" Object.equals()" tests for value equality
                    if (characterTyped.equals("\r")) {
                        characterTyped = "\n";
                    }
                    Text typedText = new Text(characterTyped);
                    typedText.setTextOrigin(VPos.TOP);
                    //store this text in the textList
                    textList.insert(typedText, root);
                    keyEvent.consume();
                    //render all the text
                    textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
                    renderBlinkingCursor();
                }
            } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                // Arrow keys should be processed using the KEY_PRESSED event, because KEY_PRESSED
                // events have a code that we can check (KEY_TYPED events don't have an associated
                // KeyCode).
                KeyCode code = keyEvent.getCode();
                if (keyEvent.isShortcutDown()) {
                    /**handle Ctrl+'+', Ctrl+'-', Ctrl+'s'*/
                    handleShortCutKey(code);
                    //handle Ctrl+Shift+=
                    if (keyEvent.isShiftDown()) {
                        if (code == KeyCode.EQUALS) {
                            if (textList.size() != 0) {
                                fontSize += 4;
                                textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
                                renderBlinkingCursor();
                            }
                        }
                    }
                }
                if (code == KeyCode.BACK_SPACE) {
                    handleBackSpace();
                }
                if (code == KeyCode.LEFT) {
                    handleLeftArrow();
                }
                if (code == KeyCode.RIGHT) {
                    handleRightArrow();
                }
                if (code == KeyCode.UP) {
                    handleUpArrow();
                }
                if (code == KeyCode.DOWN) {
                    handleDownArrow();
                }
            }
        }
    }
    /** An EventHandler to handle changing the color of the rectangle. */
    private class RectangleBlinkEventHandler implements EventHandler<ActionEvent> {
        private int currentColorIndex = 0;
        private Color[] boxColors = {Color.BLACK, Color.TRANSPARENT};

        RectangleBlinkEventHandler() {
            // Set the color to be the first color in the list.
            changeColor();
        }

        private void changeColor() {
            textBoundingBox.setFill(boxColors[currentColorIndex]);
            currentColorIndex = (currentColorIndex + 1) % boxColors.length;
        }

        @Override
        public void handle(ActionEvent event) {
            changeColor();
        }
    }
    /** An event handler that displays the current position of the mouse whenever it is clicked. */
    private class MouseClickEventHandler implements EventHandler<MouseEvent> {
        MouseClickEventHandler() {
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            // Because we registered this EventHandler using setOnMouseClicked, it will only called
            // with mouse events of type MouseEvent.MOUSE_CLICKED.  A mouse clicked event is
            // generated anytime the mouse is pressed and released on the same JavaFX node.
            int mouseClickedX = (int)mouseEvent.getX();
            int mouseClickedY = (int)mouseEvent.getY();
            handleClick(mouseClickedX, mouseClickedY);
        }
    }
    /** Makes the text bounding box change color periodically. */
    public void makeRectangleColorChange() {
        // Create a Timeline that will call the "handle" function of RectangleBlinkEventHandler
        // every 1 second.
        final Timeline timeline = new Timeline();
        // The rectangle should continue blinking forever.
        timeline.setCycleCount(Timeline.INDEFINITE);
        RectangleBlinkEventHandler cursorChange = new RectangleBlinkEventHandler();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.5), cursorChange);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }
    /**render the blinking cursor at the currentText position*/
    private void renderBlinkingCursor() {
        if (textList.size() == 0) {
            // For empty list, the position is the upper left hand corner of the screen.
            textBoundingBox.setX(5);
            textBoundingBox.setY(0);
        }else if (textList.getCurrentText() == null) {
            putCursorAtStart();
        }else{
            Text currentText = textList.getCurrentText();
            // Figure out the size of the current text.
            int textHeight = (int)Math.round(currentText.getLayoutBounds().getHeight());
            if (currentText.getText().equals("\n")) {
                textHeight = (int)Math.round(currentText.getLayoutBounds().getHeight() / 2);
            }
            int textWidth = (int)Math.round(currentText.getLayoutBounds().getWidth());
            double textPostionX = currentText.getX();
            double textPostionY = currentText.getY();
            // Re-size and re-position the bounding box.
            textBoundingBox.setHeight(textHeight);
            textBoundingBox.setWidth(1);
            // For rectangles, the position is the upper left hand corner.
            textBoundingBox.setX(textPostionX + textWidth);
            textBoundingBox.setY(textPostionY);
        }
        // Many of the JavaFX classes have implemented the toString() function, so that
        // they print nicely by default.
        System.out.println("Bounding box: " + textBoundingBox);
    }
    /**render the blinking cursor at the at the beginning of the file*/
    private void putCursorAtStart() {
        if (textList.size() == 0) {
            // For empty list, the position is the upper left hand corner of the screen.
            textBoundingBox.setX(0);
            textBoundingBox.setY(0);
        }else{
            Text firstText = textList.getFirstText();
            // Figure out the size of the current text.
            int textHeight = (int)Math.round(firstText.getLayoutBounds().getHeight());
            if (firstText.getText().equals("\n")) {
                textHeight = (int)Math.round(firstText.getLayoutBounds().getHeight() / 2);
            }
            double textPostionX = firstText.getX();
            double textPostionY = firstText.getY();
            // Re-size and re-position the bounding box.
            textBoundingBox.setHeight(textHeight);
            textBoundingBox.setWidth(1);
            // For rectangles, the position is the upper left hand corner.
            textBoundingBox.setX(5);
            textBoundingBox.setY(0);
        }
    }
    private void handleBackSpace() {
        if (!textList.isSentinel()) {
            int oldCursorX = (int)textBoundingBox.getX();
            int oldCursorY = (int)textBoundingBox.getY();
            textList.delete(root);
            textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
            renderBlinkingCursor();
            //When the most recent action was to delete text and the cursor position is
            // ambiguous, it should appear on the line where the deleted text was.
            int newCursorX = (int)textBoundingBox.getX();
            int newCursorY = (int)textBoundingBox.getY();
            if (oldCursorX != 5) {
                if ((oldCursorY > newCursorY) | (oldCursorX < newCursorX)) {
                    //getNextText() would change currentText's position in the textList,
                    // so it needs to be fixed back.
                    Text nextText = textList.getNextText();
                    int newX = (int)nextText.getX();
                    int newY = (int)nextText.getY();
                    textBoundingBox.setX(newX);
                    textBoundingBox.setY(newY);

                }
                System.out.println("Bounding box: " + textBoundingBox);
            }
        }
    }
    private void handleDownArrow() {
        if (textList.getNextText() == null) {
            return;
        }else {
            int oldCursorX = (int)textBoundingBox.getX();
            int oldCursorY = (int)textBoundingBox.getY();
            int originalCursorX = oldCursorX;
            handleRightArrow();
            int newCursorX = (int)textBoundingBox.getX();
            int newCursorY = (int)textBoundingBox.getY();
            //iterate until the next line, if it reaches the end of the file, break and return.
            while ((newCursorY == oldCursorY) && (newCursorX != oldCursorX)) {
                oldCursorX = newCursorX;
                oldCursorY = newCursorY;
                handleRightArrow();
                newCursorX = (int)textBoundingBox.getX();
                newCursorY = (int)textBoundingBox.getY();
            }
            if (newCursorX == oldCursorX) {
                return;
            }else {
                oldCursorY = newCursorY;
                /**iterate until new CursorX is larger than the original cursorX,
                 if it reaches the end of the file, break and return.*/
                while ((newCursorX != oldCursorX) && (newCursorX < originalCursorX) && ((newCursorY == oldCursorY))) {
                    oldCursorX = newCursorX;
                    oldCursorY = newCursorY;
                    handleRightArrow();
                    newCursorX = (int)textBoundingBox.getX();
                    newCursorY = (int)textBoundingBox.getY();
                }
                //when an newline is met
                if (newCursorY != oldCursorY) {
                    handleLeftArrow();
                }else if (newCursorX == oldCursorX) {
                    return;
                }
                else{
                    int largeX = newCursorX;
                    handleLeftArrow();
                    int smallX = (int)textBoundingBox.getX();
                    //double middleX = (largeX + smallX) / 2.0;
                    if (Math.abs(largeX - originalCursorX) <= Math.abs(smallX - originalCursorX)) {
                        handleRightArrow();
                    }
                }
            }
        }
    }
    private void handleUpArrow() {
        if (textList.isSentinel()) {
            return;
        }else {
            int oldCursorX = (int)textBoundingBox.getX();
            int oldCursorY = (int)textBoundingBox.getY();
            int originalCursorX = oldCursorX;
            handleLeftArrow();
            int newCursorX = (int)textBoundingBox.getX();
            int newCursorY = (int)textBoundingBox.getY();
            //iterate until the line above, if it reaches the beginning of the file, break and return.
            while ((newCursorY == oldCursorY) && (newCursorX != oldCursorX)) {
                oldCursorX = newCursorX;
                oldCursorY = newCursorY;
                handleLeftArrow();
                newCursorX = (int)textBoundingBox.getX();
                newCursorY = (int)textBoundingBox.getY();
            }
            if (newCursorX == oldCursorX) {
                return;
            }else {
                //when meet a new line
                if (newCursorX <= originalCursorX) {
                    return;
                }
                //oldCursorY = newCursorY;

                /**iterate until new CursorX is smaller than the original cursorX,
                 if it reaches the beginning of the file, break and return.*/
                while (newCursorX > originalCursorX) {
                    oldCursorX = newCursorX;
                    //oldCursorY = newCursorY;
                    handleLeftArrow();
                    newCursorX = (int)textBoundingBox.getX();
                    //newCursorY = (int)textBoundingBox.getY();
                }
                int smallX = newCursorX;
                handleRightArrow();
                int largeX = (int)textBoundingBox.getX();
                //double middleX = (largeX + smallX) / 2.0;
                if (Math.abs(largeX - originalCursorX) >= Math.abs(smallX - originalCursorX)) {
                    handleLeftArrow();
                }
            }
        }
    }
    private void handleLeftArrow() {
        //if the cursor is at the beginning of the file, do nothing
        if (textList.isSentinel()) {
            return;
        }else {
            int oldCursorX = (int)textBoundingBox.getX();
            int oldCursorY = (int)textBoundingBox.getY();
            textList.moveToPrevious();
            //textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
            renderBlinkingCursor();
            if (textList.isSentinel()) {
                return;
            }
            //for ambiguous position of the cursor, it stays at the beginning
            // of the line below
            int newCursorY = (int)textBoundingBox.getY();
            if ((oldCursorX != 5) && (newCursorY < oldCursorY)) {
                //textList.moveToNext();
                textBoundingBox.setX(5);
                textBoundingBox.setY(oldCursorY);
            }
        }
    }
    private void handleRightArrow() {
        //if the cursor is at the end of the file, do nothing
        if (textList.getNextText() == null) {
            return;
        }else {
            int oldCursorY = (int)textBoundingBox.getY();
            textList.moveToNext();
            renderBlinkingCursor();
            //if the previous line ends with a new line, the height needs to be half.
            if (textList.getNextText() == null) {
                return;
            }
            if (textList.getNextText().getText().equals("\n")) {
                //textList.moveToNext();
                //renderBlinkingCursor();
            }else {
                //for ambiguous position of the cursor, it stays at the beginning
                // of the line below.
                int currentTextY = (int)textList.getCurrentText().getY();
                if (textList.getNextText() == null) {
                    return;
                }else{
                    int nextTextY = (int)textList.getNextText().getY();
                    if (nextTextY > currentTextY) {
                        textBoundingBox.setX(5);
                        textBoundingBox.setY(nextTextY);
                    }
                }
            }
        }
    }
    private void handleClick(int cursorX, int cursorY) {
        if (textList.size() == 0) {
            return;
        }
        Text newLineStarter = textList.getNewLineStarter(cursorY);
        int currentX = (int)newLineStarter.getX();
        if (currentX >= cursorX) {
            return;
        }
        if (cursorX < newLineStarter.getLayoutBounds().getWidth()) {
            if (cursorX < (0.5 * newLineStarter.getLayoutBounds().getWidth())) {
                handleLeftArrow();
                return;
            }else {
                return;
            }
        }
        int currentY = (int)newLineStarter.getY();
        int oldX = currentX;
        int oldY = currentY;
        while (currentX < cursorX) {
            oldX = currentX;
            oldY = currentY;
            handleRightArrow();
            currentX = (int)textBoundingBox.getX();
            //detect end of the file.
            if (currentX == oldX) {
                break;
            }
            currentY = (int)textList.getCurrentText().getY();
            //detect a new line
            if (currentY != oldY) {
                break;
            }
        }
        if (currentX == oldX) {
            return;
        }
        if (currentY != oldY) {
            handleLeftArrow();
        }
        int largeX = currentX;
        handleLeftArrow();
        int smallX = currentX;
        if (Math.abs(largeX - cursorX) <= Math.abs(smallX - cursorX)) {
            handleRightArrow();
        }
    }
    /**handle Ctrl+'+', Ctrl+'-', Ctrl+'s'*/
    private void handleShortCutKey(KeyCode code) {
        if (code == KeyCode.ADD) {
            if (textList.size() != 0) {
                fontSize += 4;
                textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
                renderBlinkingCursor();
            }
        }else if ((code == KeyCode.SUBTRACT)|(code == KeyCode.MINUS)) {
            if (textList.size() != 0) {
                fontSize = Math.max(4, fontSize - 4);
                textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
                renderBlinkingCursor();
            }
        }else if (code == KeyCode.S) {
            writeFile();
        }
    }
    /**handle the window size, rerender the information when the size is changed.*/
    public void handleWindowSize(Scene scene) {
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                // Re-compute window's width.
                WINDOW_WIDTH = newScreenWidth.intValue();
                textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
                renderBlinkingCursor();
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                // Re-compute window's width.
                WINDOW_HEIGHT = newScreenHeight.intValue();
                textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
                renderBlinkingCursor();
            }
        });
    }
    /**open a file if it exists and put all the text into our data structure textList*/
    public void openFile(List<String> args) {
        if (args.size() < 1) {
            System.out.println("No filename was provided.");
            System.exit(1);
        }
        inputFilename = args.get(0);
        if (args.size() == 2) {
            /**if the 2nd argument is "debug", you can output anything you want
             * , but there is nothing now..........*/
            outputFilename = args.get(1);
        }
        try {
            File inputFile = new File(inputFilename);
            // Check to make sure that the input filename is not a directory!
            if (inputFile.isDirectory()) {
                System.out.println("Unable to open file. "+ inputFilename + " is a directory.");
            }
            // Check to make sure that the input file exists!
            else if (!inputFile.exists()) {
                //System.out.println("Unable to copy because file with name " + inputFilename + " does not exist");
                return;
            }
            FileReader reader = new FileReader(inputFile);
            // It's good practice to read files using a buffered reader.  A buffered reader reads
            // big chunks of the file from the disk, and then buffers them in memory.  Otherwise,
            // if you read one character at a time from the file using FileReader, each character
            // read causes a separate read from disk.  You'll learn more about this if you take more
            // CS classes, but for now, take our word for it!
            BufferedReader bufferedReader = new BufferedReader(reader);
            int intRead = -1;
            // Keep reading from the file input read() returns -1, which means the end of the file
            // was reached.
            while ((intRead = bufferedReader.read()) != -1) {
                // The integer read can be cast to a char, because we're assuming ASCII.
                // charRead = (char) intRead;
                //charReadList.addLast(charRead);
                String charRead = String.valueOf((char) intRead);
                /**"\n" and "\r\n" both represent a newline. And we assume that "\r" is
                 * always followed by a "\n", so "\r" can be ignored.*/
                if (!charRead.equals("\r")) {
                    Text readText = new Text(charRead);
                    readText.setTextOrigin(VPos.TOP);
                    textList.insert(readText, root);
                }
            }
            /**rendering works weird, have to call renderText() again*/
            textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
            // Close the reader.
            bufferedReader.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
    }
    /**write the textList to overwrite the input file*/
    public void writeFile() {
        try{
            // Create a FileWriter to write to outputFilename. FileWriter will overwrite any data
            // already in inputFilename.
            FileWriter writer = new FileWriter(inputFilename);
            LinkedList<Character> allTextList = textList.getAllText();
            for (Character C : allTextList) {
                writer.write(C);
            }
            writer.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
    }
    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        //Group root = new Group();
        // The Scene represents the window: its height and width will be the height and width
        // of the window displayed.
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT, Color.WHITE);

        // To get information about what keys the user is pressing, create an EventHandler.
        // EventHandler subclasses must override the "handle" function, which will be called
        // by javafx.
        EventHandler<KeyEvent> keyEventHandler =
                new KeyEventHandler(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        scene.setOnMouseClicked(new MouseClickEventHandler());

        // All new Nodes need to be added to the root in order to be displayed.
        root.getChildren().add(textBoundingBox);
        makeRectangleColorChange();

        handleWindowSize(scene);

        primaryStage.setTitle("Editor");
        // This is boilerplate, necessary to setup the window where things are displayed.
        primaryStage.setScene(scene);
        primaryStage.show();
        /**get the command line args*/
        List<String> argsList = this.getParameters().getUnnamed();
        openFile(argsList);
        /**I have no idea why the 2nd call of renderText() could render the text correctly,
         * and the 1st call render them in a weird way.*/
        textList.renderText(WINDOW_WIDTH, WINDOW_HEIGHT, fontName, fontSize);
        putCursorAtStart();
    }

    public static void main(String[] args) {
        //openFile(args);
        launch(args);
    }
}
