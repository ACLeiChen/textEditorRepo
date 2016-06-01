package editor;

import javafx.scene.Group;
import javafx.scene.text.Text;
import javafx.geometry.VPos;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.LinkedList;


/**
 * Created by ChenLei on 2016/2/28. It's basically a Linked list structure with
 * circular sentinel implementation. currentText is a node which is always pointing
 * at the current node. Most of the code is copied from proj1/LinkedListDeque.java.
 */
public class TextList {

    private class Node {
        private Text item;     /* Equivalent of first */
        private Node previous;
        private Node next; /* Equivalent of rest */

        private Node(Text i, Node p, Node h) {
            item = i;
            previous = p;
            next = h;
        }
    }

    private Node sentinel;
    private Node currentText;
    private int size;
    //An arraylist for record new lines
    private ArrayList<Node> newLineList;
    int newYRecorder;
    /** Creates an empty list. */
    public TextList() {
        size = 0;
        sentinel = new Node(null, null, null);
        currentText = sentinel;
        /**At a circular sentinel topology, an empty list means
         its previous and next both have to point at itself.*/
        sentinel.previous = sentinel;
        sentinel.next = sentinel;
        newYRecorder = 0;
    }


    /**insert a node based on the current Position*/
    public void insert(Text insertedText, Group root) {
        root.getChildren().add(insertedText);
        Node oldNodeAfterCurrent = currentText.next;
        Node newNode = new Node(insertedText, currentText, oldNodeAfterCurrent);
        currentText.next = newNode;
        oldNodeAfterCurrent.previous = newNode;
        //update currentText
        currentText = newNode;
        size += 1;
    }

    /**delete a node based on the current Position*/
    public void delete(Group root) {
        if((size != 0) && (currentText != sentinel)) {
            Node previousNode = currentText.previous;
            Node nextNode = currentText.next;
            previousNode.next = nextNode;
            nextNode.previous = previousNode;
            //remove it from root.
            root.getChildren().remove(currentText.item);
            currentText.item = null;
            currentText.previous = null;
            currentText.next = null;
            //update currentText
            currentText = previousNode;
            if (size > 0) {
                size -= 1;
            }
        }
    }
    /**returns the currentText*/
    public Text getCurrentText() {
        return currentText.item;
    }
    /**returns previous text*/
    public Text getPreviousText() {
        //if next node is not sentinel
        if (currentText.previous.item != null) {
            Node previousNode = currentText.previous;
            return previousNode.item;
        }return null;

    }
    /**returns next text*/
    public Text getNextText() {
        //if next node is not sentinel
        if (currentText.next.item != null) {
            Node nextNode = currentText.next;
            return nextNode.item;
        }return null;

    }
    /**move the currentText to the next,
     * this method can point currentText to sentinel!!!*/
    public void moveToPrevious() {

            currentText = currentText.previous;

    }
    /**move the currentText to the next*/
    public void moveToNext() {
        if (currentText.next.item != null) {
            currentText = currentText.next;
        }
    }
    //return true if currentText points to the sentinel
    public boolean isSentinel() {
        return (currentText == sentinel);
    }
    /**return the first text,
     * !!!calling this method will point currentText to sentinel!!!*/
    public Text getFirstText() {
        currentText = sentinel;
        return currentText.next.item;
    }
    /**return all the text in a LinkedList<Character></>*/
    public LinkedList<Character> getAllText() {
        if (size() == 0) {
            return null;
        }else{
            LinkedList<Character> allTextList = new LinkedList<Character>();
            Node thisNode = sentinel.next;
            while (thisNode != sentinel) {
                allTextList.addLast(thisNode.item.getText().charAt(0));
                thisNode = thisNode.next;
            }
            return allTextList;
        }
    }
    /**find corresponding Text and update the currentText*/
    public void findText(int cursorX, int cursorY) {

    }
    /**map cursorX and cursorY to the current text*/
    private void recordNewLine(int y, Node thisNode) {
        if (y != newYRecorder) {
            newYRecorder = y;
            newLineList.add(thisNode);
        }
    }
    /**calculate and return the new line starter, then point currentText at it*/
    public Text getNewLineStarter(int cursorY) {
        if (isSentinel()){
            currentText = currentText.next;
        }
        int currentHeight = (int)currentText.item.getLayoutBounds().getHeight();
        int lineIndex = cursorY / currentHeight + 1;
        if (lineIndex > (newLineList.size() - 1)) {
            currentText = newLineList.get(newLineList.size() - 1);
            return currentText.item;
        }
        currentText = newLineList.get(lineIndex);
        return currentText.item;
    }

    /**render all the text, also map them with their position*/
    public void renderText(int windowWidth, int windowHeight, String fontName, int fontSize) {
        if (size != 0) {
            /**for the first text*/
            int x = 5;
            int y = 0;
            Node thisNode = new Node(null, null, null);
            Text thisText = new Text();
            thisNode = sentinel;
            thisNode = thisNode.next;
            thisText = thisNode.item;
            //The above 2 line cannot be put in the helper method, because the condition
            // of while-loop also uses thisNode.
            thisText.setFont(Font.font(fontName, fontSize));
            if (thisText.getText().equals("\n")) {
                x = 5;
                y = y + (int)Math.round(thisText.getLayoutBounds().getHeight()/2.0);
            }
            newLineList = new ArrayList<Node>();
            renderTextHelper(thisText, thisNode, x, y);
            newLineList.add(thisNode);
            /**for the rest text, iterate*/
            if (size > 1) {
                while (thisNode.next != sentinel) {
                    /**A faster way to do the rounding without using Math.round() is:
                     * int a = (int) (doubleVar + 0.5);
                     * But it's not so readable.*/
                    x = x + (int)Math.round(thisText.getLayoutBounds().getWidth());
                    thisNode = thisNode.next;
                    thisText = thisNode.item;
                    //Fontsize has to be set before trying to check for word wrapping.
                    thisText.setFont(Font.font(fontName, fontSize));
                    // word wraps at the edge of the window, next position would start from a new line
                    int currentWidth = (int)Math.round(thisText.getLayoutBounds().getWidth());
                    if ((x + currentWidth + 5) > windowWidth) {
                        x = 5;
                        y = y + (int)Math.round(thisText.getLayoutBounds().getHeight());
                    }
                    //"\n" means to start a new line
                    if (thisText.getText().equals("\n")) {
                        x = 5;
                        y = y + (int)Math.round(thisText.getLayoutBounds().getHeight()/2.0);
                    }
                    renderTextHelper(thisText, thisNode, x, y);
                }
            }
        }
    }

    private void renderTextHelper(Text thisText,Node thisNode, int x, int y) {
        thisText.setX(x);
        thisText.setY(y);
        recordNewLine(y, thisNode);
    }
    /**Returns true if deque is empty, false otherwise.*/
    
    public boolean isEmpty() {
        if (size == 0) {
            return true;
        }return false;
    }

    /**Returns the number of items in the Deque.*/
    
    public int size() {
        return size;
    }

    /**Removes and returns the item at the back of the Deque.
     If no such item exists, returns null.*/
    
    public Text removeLast() {
        if (size == 0 ) {
            return null;//for int type
        }
        Node oldSecondLastNode = sentinel.previous.previous;
        sentinel.previous.previous = null;
        sentinel.previous.next = null;
        Text remove_value = sentinel.previous.item;
        sentinel.previous.item = null;
        sentinel.previous = oldSecondLastNode;
        oldSecondLastNode.next = sentinel;
        size -= 1;
        currentText = oldSecondLastNode;
        return remove_value;
    }

    /**probably need to rewrite get()*/
    public Text get(int index) {
        if (index >= size) {
            return null;
        }
        Node p = sentinel.next;
        for (int i = 0; i< index; i++) {
            p = p.next;
        }
        return p.item;
    }

}