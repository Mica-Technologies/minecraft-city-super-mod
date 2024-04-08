package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

/**
 * This multi-line text field component was adapted from the publicly available version by
 * <em>Draco18s</em> on GitHub at <a
 * href="https://gist.github.com/Draco18s/2b02762b597e67a9b887aed241f25077">https://gist.github
 * .com/Draco18s/2b02762b597e67a9b887aed241f25077</a>.
 */
public class GuiMultiLineTextField extends Gui {

  private final int id;
  private final FontRenderer fontRenderer;
  private final List<String> lines = new ArrayList<>();
  private final int x, y, width, height;
  private boolean isFocused = false;
  // private int cursorPos = 0; // Overall cursor position in the text
  private int cursorLine = 0; // Line index of the cursor
  private int cursorColumn = 0; // Column index of the cursor within the line
  private int viewOffset = 0; // Line offset for viewing
  private Set<Integer> userInsertedNewLines = new HashSet<>();

  private boolean isTextSelected = false;
  private int selectionStartLine = 0;
  private int selectionStartColumn = 0;
  private int selectionEndLine = 0;
  private int selectionEndColumn = 0;

  public GuiMultiLineTextField(int id, FontRenderer fontRenderer, int x, int y, int width,
      int height) {
    this.id = id;
    this.fontRenderer = fontRenderer;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    lines.add("");
  }

  public void drawTextBox() {
    if (isFocused) {
      // Background
      drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000);

      // Text rendering starts here
      int yPos = this.y + 2; // Start a bit inside the box
      int lineCounter = 0; // Keep track of how many lines have been rendered

      int selectionStartLine;
      int selectionEndLine;
      int selectionStartColumn;
      int selectionEndColumn;
      if (this.selectionStartLine < this.selectionEndLine || (
          this.selectionStartLine == this.selectionEndLine
              && this.selectionStartColumn < this.selectionEndColumn)) {
        selectionStartLine = this.selectionStartLine;
        selectionEndLine = this.selectionEndLine;
        selectionStartColumn = this.selectionStartColumn;
        selectionEndColumn = this.selectionEndColumn;
      } else {
        selectionStartLine = this.selectionEndLine;
        selectionEndLine = this.selectionStartLine;
        selectionStartColumn = this.selectionEndColumn;
        selectionEndColumn = this.selectionStartColumn;
      }

      for (int i = viewOffset; i < lines.size(); i++) {
        String line = lines.get(i);
        int xPos = this.x + 2; // X position to start drawing the line

        if (lineCounter >= (this.height / fontRenderer.FONT_HEIGHT)) {
          break; // Stop if we've rendered enough lines to fill the text field
        }

        // Check if this line is within the selection
        if (isTextSelected && i >= selectionStartLine && i <= selectionEndLine) {
          int highlightStartX = xPos;
          int highlightEndX = this.x + width - 2;

          // Adjust the start and end positions for highlighting within the line
          if (i == selectionStartLine) {
            String textBeforeSelection =
                line.substring(0, Math.min(selectionStartColumn, line.length()));
            highlightStartX += fontRenderer.getStringWidth(textBeforeSelection);
          }
          if (i == selectionEndLine) {
            String textInSelection = line.substring(0, Math.min(selectionEndColumn, line.length()));
            highlightEndX = xPos + fontRenderer.getStringWidth(textInSelection);
          }

          // Highlight background for selected text
          drawRect(highlightStartX, yPos, highlightEndX, yPos + fontRenderer.FONT_HEIGHT,
              0x80FFFFFF); // Semi-transparent white
        }

        // Draw the text
        fontRenderer.drawString(line, xPos, yPos, 0xE0E0E0);
        yPos += fontRenderer.FONT_HEIGHT; // Move Y position down for the next line
        lineCounter++;
      }

      // Improved Cursor Rendering
      if ((System.currentTimeMillis() / 500) % 2 == 0) { // Blinking effect
        if (cursorLine >= viewOffset && cursorLine < viewOffset + lineCounter) {
          String currentLineText = cursorLine < lines.size() ? lines.get(cursorLine) : "";

          // Calculate cursorX considering the special case where the cursor moves to a blank
          // line below
          int cursorX = this.x + 2; // Default starting position
          String textUpToCursor =
              currentLineText.substring(0, Math.min(cursorColumn, currentLineText.length()));
          cursorX += fontRenderer.getStringWidth(textUpToCursor);

          // Calculate cursorY to potentially move the cursor to the start of the next line if
          // it's blank
          int cursorY = this.y + 2 + (cursorLine - viewOffset) * fontRenderer.FONT_HEIGHT;

          drawRect(cursorX, cursorY, cursorX + 1, cursorY + fontRenderer.FONT_HEIGHT - 1,
              0xFFFFFFFF);
        }
      }
    }
  }

  private void selectAll() {
    if (lines.isEmpty()) {
      return;
    }

    if (isTextSelected) {
      deselectText();
    } else {
      isTextSelected = true;
      selectionStartLine = 0;
      selectionStartColumn = 0;
      selectionEndLine = lines.size() - 1;
      selectionEndColumn = lines.get(lines.size() - 1).length();
    }
  }

  private void deselectText() {
    isTextSelected = false;
    selectionStartLine = 0;
    selectionStartColumn = 0;
    selectionEndLine = 0;
    selectionEndColumn = 0;
  }

  private String getSelectedText() {
    StringBuilder selectedText = new StringBuilder();
    // Ensure selection start is before selection end
    int startLine = Math.min(selectionStartLine, selectionEndLine);
    int startColumn = startLine == selectionStartLine ? selectionStartColumn : selectionEndColumn;
    int endLine = Math.max(selectionStartLine, selectionEndLine);
    int endColumn = endLine == selectionEndLine ? selectionEndColumn : selectionStartColumn;
    if (startLine == endLine && endColumn < startColumn) {
      int temp = startColumn;
      startColumn = endColumn;
      endColumn = temp;
    }

    for (int i = startLine; i <= endLine; i++) {
      String line = lines.get(i);
      if (i == startLine) { // Start line of selection
        if (startLine == endLine) { // Selection within a single line
          selectedText.append(line.substring(startColumn, endColumn));
        } else {
          selectedText.append(line.substring(startColumn));
        }
      } else if (i == endLine) { // Last line of selection
        selectedText.append('\n').append(line, 0, endColumn);
      } else { // Full line within multi-line selection
        selectedText.append('\n').append(line);
      }
    }

    return selectedText.toString();
  }


  private void copy() {
    if (isTextSelected) {
      String selectedText = getSelectedText();
      GuiScreen.setClipboardString(selectedText);
    }
  }

  private void cut() {
    if (isTextSelected) {
      copy(); // Copy selected text to clipboard
      deleteSelectedText(); // Delete the selected text
      isTextSelected = false; // Clear selection after cut
    }
  }

  private void deleteSelectedText() {
    if (!isTextSelected) {
      return; // Nothing to do if no text is selected
    }

    int startLine;
    int endLine;
    int startColumn;
    int endColumn;
    if (this.selectionStartLine < this.selectionEndLine || (
        this.selectionStartLine == this.selectionEndLine
            && this.selectionStartColumn < this.selectionEndColumn)) {
      startLine = this.selectionStartLine;
      endLine = this.selectionEndLine;
      startColumn = this.selectionStartColumn;
      endColumn = this.selectionEndColumn;
    } else {
      startLine = this.selectionEndLine;
      endLine = this.selectionStartLine;
      startColumn = this.selectionEndColumn;
      endColumn = this.selectionStartColumn;
    }

    if (startLine == endLine) {
      // Selection within a single line
      String line = lines.get(startLine);
      String newLine = line.substring(0, startColumn) + line.substring(endColumn);
      lines.set(startLine, newLine);
      cursorLine = startLine;
      cursorColumn = startColumn;
    } else {
      // Selection spans multiple lines
      // First, handle the start line
      String firstLine = lines.get(startLine);
      String firstLineNew = firstLine.substring(0, startColumn);

      // Then, handle the end line
      String lastLine = lines.get(endLine);
      String lastLineNew = lastLine.substring(endColumn);

      // Merge the start and end lines
      lines.set(startLine, firstLineNew + lastLineNew);

      // Remove lines fully covered by the selection
      for (int i = endLine; i > startLine; i--) {
        lines.remove(i);
        // Also update the userInsertedNewLines set
        userInsertedNewLines.remove(i + 1);
      }
      cursorLine = startLine;
      cursorColumn = startColumn;
    }

    // Clear the selection
    isTextSelected = false;
    selectionStartLine = selectionStartColumn = selectionEndLine = selectionEndColumn = 0;

    // Ensure lines are not empty after deletion
    if (lines.isEmpty()) {
      lines.add("");
    }
  }

  private void paste() {
    String clipboardText = GuiScreen.getClipboardString();
    if (clipboardText != null && !clipboardText.isEmpty()) {
      insertTextAtCursor(clipboardText);
    }
  }

  private void insertTextAtCursor(String text) {
    if (isTextSelected) {
      deleteSelectedText();
    }

    int ogCursorLine = cursorLine;
    int ogCursorColumn = cursorColumn;
    String textToInsert = text.replaceAll("\r\n", "\n");
    String[] paragraphs = textToInsert.split("\n", -1);
    for (int i = 0; i < paragraphs.length; i++) {
      if (i > 0) {
        insertNewLineAtCursor();
      }

      // Directly insert the text at the current cursor position (handles wrapping)
      int lineIndex = cursorLine;
      String currentLine = lines.get(lineIndex);
      if (cursorColumn > currentLine.length()) {
        cursorColumn = currentLine.length();
      }
      String newLine =
          currentLine.substring(0, cursorColumn) + paragraphs[i] + currentLine.substring(
              cursorColumn);
      cursorColumn = newLine.length();
      lines.set(lineIndex, newLine);
    }

    // Re-render the text to ensure proper wrapping
    setText(getText());

    // Reposition the cursor after wrapping
    cursorLine = ogCursorLine;
    cursorColumn = ogCursorColumn;
    int charCount = textToInsert.length();
    for (int i = 0; i < charCount; i++) {
      moveCursorRight();
    }
  }

  public void textboxKeyTyped(char typedChar, int keyCode) {
    if (!isFocused) {
      return;
    }

    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
      switch (keyCode) {
        case Keyboard.KEY_A -> {
          selectAll();
          return;
        }
        case Keyboard.KEY_C -> {
          copy();
          return;
        }
        case Keyboard.KEY_X -> {
          cut();
          return;
        }
        case Keyboard.KEY_V -> {
          paste();
          return;
        }
        default -> {
          // Do nothing
          return;
        }
      }
    }

    switch (keyCode) {
      case Keyboard.KEY_RETURN:
        // Handle newline insertion
        insertNewLineAtCursor();
        break;
      case Keyboard.KEY_BACK:
        // Handle backspace
        deleteCharacterBeforeCursor();
        break;
      case Keyboard.KEY_DELETE:
        // Handle delete
        deleteCharacterAfterCursor();
        break;
      case Keyboard.KEY_LEFT:
        // Handle cursor movement left
        moveCursorLeft();
        break;
      case Keyboard.KEY_RIGHT:
        // Handle cursor movement right
        moveCursorRight();
        break;
      case Keyboard.KEY_UP:
        // Handle cursor movement up
        moveCursorUp();
        break;
      case Keyboard.KEY_DOWN:
        // Handle cursor movement down
        moveCursorDown();
        break;
      default:
        // Handle character input
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
          insertCharacterAtCursor(typedChar);
        }
        break;
    }
    updateViewOffsetForCursor();
  }

  private void insertNewLineAtCursor() {
    if (isTextSelected) {
      deleteSelectedText();
    }

    String currentLine = lines.size() > cursorLine ? lines.get(cursorLine) : "";

    // Ensure columnInLine does not exceed current line's length
    cursorColumn = Math.min(cursorColumn, currentLine.length());

    String lineBeforeCursor = currentLine.substring(0, cursorColumn);
    String lineAfterCursor = currentLine.substring(cursorColumn);
    lines.set(cursorLine, lineBeforeCursor);
    lines.add(cursorLine + 1, lineAfterCursor);

    userInsertedNewLines.add(cursorLine + 1);

    // Adjust cursorPos for the newline. Add 1 to position cursor at the start of the next line.
    cursorLine++;
    cursorColumn = 0;
  }

  private void deleteCharacterBeforeCursor() {
    if (isTextSelected) {
      deleteSelectedText();
      return;
    }

    if (cursorLine == 0 && cursorColumn == 0) {
      // Cursor is at the very start of the text; nothing to delete.
      return;
    } else if (cursorLine < 0 || cursorLine >= lines.size()) {
      cursorLine = 0; // Cursor is out of bounds; reset to the first line
      cursorColumn = 0; // Cursor is out of bounds; reset to the first column
      return;
    } else if (cursorColumn < 0 || cursorColumn > lines.get(cursorLine).length()) {
      cursorLine = 0; // Cursor is out of bounds; reset to the first line
      cursorColumn = 0; // Cursor is out of bounds; reset to the first column\
      return;
    }

    if (cursorColumn == 0) {
      mergeWithPreviousLine(cursorLine);
      return;
    } else {
      // Safely delete a character within the line.
      String currentLine = lines.get(cursorLine);
      String newLine =
          currentLine.substring(0, cursorColumn - 1) + currentLine.substring(cursorColumn);
      lines.set(cursorLine, newLine);
      cursorColumn--;
      if (newLine.isEmpty() && !userInsertedNewLines.contains(cursorLine)) {
        cursorLine--;
        cursorLine = Math.max(0, cursorLine);
        cursorColumn = lines.get(cursorLine).length();
      }
    }
  }

  private void deleteCharacterAfterCursor() {
    if (isTextSelected) {
      deleteSelectedText();
      return;
    }

    if (cursorLine == lines.size() - 1 && cursorColumn == lines.get(cursorLine).length()) {
      // Cursor is at the very end of the text; nothing to delete.
      return;
    } else if (cursorLine < 0 || cursorLine >= lines.size()) {
      cursorLine = 0; // Cursor is out of bounds; reset to the first line
      cursorColumn = 0; // Cursor is out of bounds; reset to the first column
      return;
    } else if (cursorColumn < 0 || cursorColumn > lines.get(cursorLine).length()) {
      cursorLine = 0; // Cursor is out of bounds; reset to the first line
      cursorColumn = 0; // Cursor is out of bounds; reset to the first column
      return;
    }

    if (cursorColumn == lines.get(cursorLine).length()) {
      mergeWithNextLine(cursorLine);
      return;
    } else {
      // Safely delete a character within the line.
      String currentLine = lines.get(cursorLine);
      String newLine =
          currentLine.substring(0, cursorColumn) + currentLine.substring(cursorColumn + 1);
      lines.set(cursorLine, newLine);
      if (newLine.isEmpty() && !userInsertedNewLines.contains(cursorLine)) {
        mergeWithNextLine(cursorLine);
      }
    }
  }


  private void insertCharacterAtCursor(char character) {
    if (isTextSelected) {
      deleteSelectedText();
    }

    String currentLine = lines.size() > cursorLine ? lines.get(cursorLine) : "";

    // If adding the character exceeds the width, we need to wrap
    if (fontRenderer.getStringWidth(currentLine + character) > width - 4) {
      int lastSpaceIndex = currentLine.lastIndexOf(' ', cursorColumn - 1);
      if (lastSpaceIndex != -1) {
        // If there's a space, split at the last space and insert the rest on a new line
        String lineBeforeSpace = currentLine.substring(0, lastSpaceIndex);
        String lineAfterSpace = currentLine.substring(lastSpaceIndex + 1) + character;
        lines.set(cursorLine, lineBeforeSpace);
        lines.add(cursorLine + 1, lineAfterSpace);
        // Adjust cursorPos to new position after wrap
        cursorColumn = lineAfterSpace.length();
        cursorLine++;
      } else {
        // No space found; split at the current position
        String newLine = currentLine + character;
        lines.set(cursorLine, newLine.substring(0, cursorColumn));
        lines.add(cursorLine + 1, newLine.substring(cursorColumn));
        // Adjust cursorPos to new position after wrap
        cursorColumn = 1; // +1 for the character just added
        cursorLine++;
      }
    } else {
      // No wrapping needed, insert character normally
      String newLine = "";
      try {
        newLine = currentLine.substring(0, cursorColumn) + character + currentLine.substring(
            cursorColumn);
      } catch (Exception e) {
        e.printStackTrace();
      }
      lines.set(cursorLine, newLine);
      cursorColumn++; // Move cursor right by one, as before
    }
  }

  private void updateViewOffsetForCursor() {
    // Check if the cursor is out of view after backspacing
    int currentLineHeight = fontRenderer.FONT_HEIGHT;
    int cursorY = (cursorLine - viewOffset) * currentLineHeight;
    int maxY = this.height - currentLineHeight;

    if (cursorY < 0) {
      // Cursor is above the view, adjust the view offset immediately
      this.viewOffset = cursorLine;
    } else if (cursorY > maxY) {
      // Cursor is below the view, adjust the view offset immediately
      this.viewOffset = cursorLine - (this.height / currentLineHeight) + 1;
    }

    // Adjust view offset if lines were removed due to backspace
    if (lines.size() < (this.height / currentLineHeight)) {
      this.viewOffset = 0;
    }
    if (lines.size() - viewOffset < (this.height / currentLineHeight)) {
      this.viewOffset = Math.max(0, lines.size() - (this.height / currentLineHeight));
    }
  }

  private void mergeWithPreviousLine(int lineIndex) {
    if (lineIndex <= 0) {
      return; // No previous line to merge with
    }
    String prevLine = lines.get(lineIndex - 1);
    if (userInsertedNewLines.contains(lineIndex)) {
      // If there was a user-inserted newline, just remove it without merging lines
      userInsertedNewLines.remove(lineIndex);
      lines.remove(lineIndex);
      cursorColumn = prevLine.length();
      cursorLine = lineIndex - 1;
    } else if (prevLine.isEmpty()) {
      // Merge an empty previous line with the current line (essentially removing an empty line)
      lines.remove(lineIndex - 1);
      cursorColumn = 0;
      cursorLine = lineIndex - 1;
    } else {
      // Merge lines and remove the current line
      String currentLine = lines.get(lineIndex);
      lines.set(lineIndex - 1, prevLine + currentLine);
      lines.remove(lineIndex);
      cursorColumn = prevLine.length();
      cursorLine = lineIndex - 1;
    }
    adjustUserInsertedNewLines(lineIndex);
  }

  private void mergeWithNextLine(int cursorLine) {
    if (cursorLine >= lines.size() - 1) {
      return; // No next line to merge with
    }
    String currentLine = lines.get(cursorLine);
    String nextLine = lines.get(cursorLine + 1);
    if (userInsertedNewLines.contains(cursorLine + 1)) {
      // If there was a user-inserted newline, just remove it without merging lines
      userInsertedNewLines.remove(cursorLine + 1);
      lines.remove(cursorLine + 1);
    } else if (nextLine.isEmpty()) {
      // Merge an empty next line with the current line (essentially removing an empty line)
      lines.remove(cursorLine + 1);
    } else {
      // Merge lines and remove the next line
      lines.set(cursorLine, currentLine + nextLine);
      lines.remove(cursorLine + 1);
    }
    adjustUserInsertedNewLines(cursorLine + 1);
  }


  // Helper method to adjust the indices in userInsertedNewLines after a line is removed
  private void adjustUserInsertedNewLines(int removedLineIndex) {
    Set<Integer> updatedUserInsertedNewLines = new HashSet<>();
    for (Integer index : userInsertedNewLines) {
      if (index > removedLineIndex) {
        updatedUserInsertedNewLines.add(
            index - 1); // Adjust index down by one for lines after the removed line
      } else if (index < removedLineIndex) {
        updatedUserInsertedNewLines.add(
            index); // Keep index the same for lines before the removed line
      }
      // Note: If index == removedLineIndex, we're removing that newline, so don't add it back
    }
    userInsertedNewLines = updatedUserInsertedNewLines;
  }

  private void moveCursorUp() {
    if (cursorLine > 0) {
      if (!isTextSelected) {
        selectionStartLine = cursorLine;
        selectionStartColumn = cursorColumn;
      }

      int currentColumnPosition = cursorColumn;

      // Move to the previous line
      int prevLineIndex = cursorLine - 1;
      String prevLine = lines.get(prevLineIndex);

      // Attempt to maintain the same column position in the previous line
      int newPositionWithinPrevLine = Math.min(currentColumnPosition, prevLine.length());

      // Adjust the global cursor position to reflect its position within the previous line
      cursorLine--;
      cursorColumn = newPositionWithinPrevLine;

      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        isTextSelected = true;
        selectionEndLine = cursorLine;
        selectionEndColumn = cursorColumn;
      } else {
        deselectText();
      }
    }
  }

  private void moveCursorDown() {
    if (cursorLine < lines.size() - 1) {
      if (!isTextSelected) {
        selectionStartLine = cursorLine;
        selectionStartColumn = cursorColumn;
      }

      int currentColumnPosition = cursorColumn;

      // Move to the next line
      int nextLineIndex = cursorLine + 1;
      String nextLine = lines.get(nextLineIndex);

      // Attempt to maintain the same column position in the next line
      int newPositionWithinNextLine = Math.min(currentColumnPosition, nextLine.length());

      // Adjust the global cursor position to reflect its position within the next line
      cursorLine++;
      cursorColumn = newPositionWithinNextLine;

      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        isTextSelected = true;
        selectionEndLine = cursorLine;
        selectionEndColumn = cursorColumn;
      } else {
        deselectText();
      }
    }
  }

  private void moveCursorLeft() {
    // Move cursor left
    if (cursorColumn > 0 || cursorLine > 0) {
      if (!isTextSelected) {
        selectionStartLine = cursorLine;
        selectionStartColumn = cursorColumn;
      }

      cursorColumn--;
      if (cursorColumn < 0) {
        cursorLine--;
        cursorColumn = lines.get(cursorLine).length();
      }

      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        isTextSelected = true;
        selectionEndLine = cursorLine;
        selectionEndColumn = cursorColumn;
      } else {
        deselectText();
      }
    }
  }

  private void moveCursorRight() {
    // Move cursor right, ensuring not to exceed text length
    if (cursorColumn < getText().length()) {
      if (!isTextSelected) {
        selectionStartLine = cursorLine;
        selectionStartColumn = cursorColumn;
      }

      cursorColumn++;
      if (cursorColumn > lines.get(cursorLine).length() && cursorLine < lines.size() - 1) {
        cursorLine++;
        cursorColumn = 0;
      }

      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        isTextSelected = true;
        selectionEndLine = cursorLine;
        selectionEndColumn = cursorColumn;
      } else {
        deselectText();
      }
    }
  }

  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    // isFocused = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
    //     && mouseY < this.y + this.height;
    if (mouseButton == 0) {
      if (!isTextSelected) {
        selectionStartLine = cursorLine;
        selectionStartColumn = cursorColumn;
      }

      int lineIndex = (mouseY - y) / fontRenderer.FONT_HEIGHT + viewOffset;
      if (lineIndex >= 0 && lineIndex < lines.size()) {
        cursorLine = lineIndex;
        String currentLine = lines.get(cursorLine);
        int column = 0;
        for (int i = 0; i <= currentLine.length(); i++) {
          int stringWidth = fontRenderer.getStringWidth(currentLine.substring(0, i));
          if (stringWidth + 2 > mouseX - x) {
            break;
          }
          column = i;
        }
        cursorColumn = column;
      }
      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        isTextSelected = true;
        selectionEndLine = cursorLine;
        selectionEndColumn = cursorColumn;
      } else {
        deselectText();
      }
    }
  }

  // Add the following method to handle mouse wheel events
  public void handleMouseInput(int mouseX, int mouseY, int scrollDelta) {
    if (isFocused) {
      // Adjust the view offset based on the scroll direction
      if (scrollDelta > 0) { // Scrolling up
        if (viewOffset > 0) {
          viewOffset--;
        }
      } else if (scrollDelta < 0) { // Scrolling down
        int totalLines = lines.size();
        if (totalLines > (height / fontRenderer.FONT_HEIGHT) && viewOffset < totalLines - (height
            / fontRenderer.FONT_HEIGHT)) {
          viewOffset++;
        }
      }
    }
  }


  public String getText() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      sb.append(lines.get(i));
      // Add a new line character if this line index is marked
      if (userInsertedNewLines.contains(i + 1)) {
        sb.append("\n");
      }
    }
    return sb.toString().trim();
  }

  public void setText(String text) {
    lines.clear();
    userInsertedNewLines.clear();

    if (text.isEmpty()) {
      lines.add("");
      cursorLine = 0;
      cursorColumn = 0;
      return;
    }

    StringBuilder currentLine = new StringBuilder();

    String[] paragraphs = text.split("\n", -1);
    boolean previousParagraphWasEmpty = false;

    for (String para : paragraphs) {
      if (para.isEmpty()) {
        if (!previousParagraphWasEmpty) {
          // End of a paragraph, add whatever is in currentLine to lines
          if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
            currentLine = new StringBuilder();
          }
          // Mark this position as a user-inserted new line
          userInsertedNewLines.add(lines.size());
        }
        // Add an empty string for each consecutive new line encountered
        lines.add("");
        // Mark every new line position after the first as user-inserted
        userInsertedNewLines.add(lines.size());
        previousParagraphWasEmpty = true;
        continue;
      }

      // Reset flag when encountering a non-empty paragraph
      previousParagraphWasEmpty = false;
      StringBuilder wordBuffer = new StringBuilder();
      for (char c : para.toCharArray()) {
        wordBuffer.append(c);
        if (c == ' ' || c == para.charAt(para.length() - 1)) {
          // Check if adding the wordBuffer to currentLine exceeds the width
          if (fontRenderer.getStringWidth(currentLine.toString() + wordBuffer.toString())
              <= width - 4) {
            currentLine.append(wordBuffer);
          } else {
            // Does not fit; need to wrap
            if (currentLine.length() > 0) {
              lines.add(currentLine.toString());
              currentLine = new StringBuilder();
            }
            // Handle long word wrapping
            while (fontRenderer.getStringWidth(wordBuffer.toString()) > width - 4) {
              String part = splitWordToFit(wordBuffer.toString(), width - 4);
              lines.add(part);
              wordBuffer = new StringBuilder(wordBuffer.substring(part.length()));
            }
            currentLine.append(wordBuffer);
          }
          wordBuffer = new StringBuilder(); // Reset word buffer
        }
      }
      // Add the current line if it has content
      if (currentLine.length() > 0) {
        lines.add(currentLine.toString());
        currentLine = new StringBuilder();
      }
      // Reflect explicit new lines from user input at the end of paragraphs
      userInsertedNewLines.add(lines.size());
    }

    // Special case: if the text ends with new lines, ensure they're respected
    if (previousParagraphWasEmpty) {
      lines.add("");
      userInsertedNewLines.add(lines.size());
    }

    // Reset cursor to the end of the text
    cursorLine = lines.size() - 1;
    cursorColumn = lines.get(cursorLine).length();
    adjustViewOffsetForNewContent();
  }

  // Utility method to split a long word to fit the specified width
  private String splitWordToFit(String word, int maxWidth) {
    for (int i = 1; i < word.length(); i++) {
      String part = word.substring(0, i);
      if (fontRenderer.getStringWidth(part) > maxWidth) {
        return word.substring(0, i - 1); // Return part of the word that fits
      }
    }
    return word; // In case the whole word fits or is a single character
  }

  private void adjustViewOffsetForNewContent() {
    // Adjust viewOffset based on new content, if necessary.
    // Placeholder for logic to ensure the newly set text is properly viewable.
    viewOffset = Math.max(0, lines.size() - (height / fontRenderer.FONT_HEIGHT));
  }

  public void setFocused(boolean isFocused) {
    this.isFocused = true;
  }

  public boolean isFocused() {
    return true;
  }
}
