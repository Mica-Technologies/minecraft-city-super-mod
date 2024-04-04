package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
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
  private int cursorPos = 0; // Overall cursor position in the text
  private int viewOffset = 0; // Line offset for viewing
  private Set<Integer> userInsertedNewLines = new HashSet<>();

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
      int lineCounter = 0;
      int renderedLines = 0; // Keep track of how many lines have been rendered
      for (int i = 0; i < lines.size(); i++) {
        if (renderedLines >= viewOffset) {
          String line = lines.get(i);
          fontRenderer.drawString(line, this.x + 2, yPos, 0xE0E0E0);
          yPos += fontRenderer.FONT_HEIGHT;
          lineCounter++;
        }
        if (lineCounter >= (this.height / fontRenderer.FONT_HEIGHT)) {
          break; // Stop if we've rendered enough lines to fill the text field
        }
        renderedLines++;
      }

      // Improved Cursor Rendering
      if ((System.currentTimeMillis() / 500) % 2 == 0) { // Blinking effect
        int cursorLineIndex = findCurrentLine();
        if (cursorLineIndex >= viewOffset && cursorLineIndex < viewOffset + lineCounter) {
          int cursorColumn = findColumnInLine(cursorLineIndex);
          String currentLineText = cursorLineIndex < lines.size() ? lines.get(cursorLineIndex) : "";

          // Calculate cursorX considering the special case where the cursor moves to a blank
          // line below
          int cursorX = this.x + 2; // Default starting position
          String textUpToCursor =
              currentLineText.substring(0, Math.min(cursorColumn, currentLineText.length()));
          cursorX += fontRenderer.getStringWidth(textUpToCursor);

          // Calculate cursorY to potentially move the cursor to the start of the next line if
          // it's blank
          int cursorY = this.y + 2 + (cursorLineIndex - viewOffset) * fontRenderer.FONT_HEIGHT;

          drawRect(cursorX, cursorY, cursorX + 1, cursorY + fontRenderer.FONT_HEIGHT - 1,
              0xFFFFFFFF);
        }
      }
    }
  }

  public void textboxKeyTyped(char typedChar, int keyCode) {
    if (!isFocused) {
      return;
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
      case Keyboard.KEY_LEFT:
        // Move cursor left
        if (cursorPos > 0) {
          cursorPos--;
        }
        break;
      case Keyboard.KEY_RIGHT:
        // Move cursor right, ensuring not to exceed text length
        if (cursorPos < getText().length()) {
          cursorPos++;
        }
        break;
      case Keyboard.KEY_UP:
        // Move cursor up to the previous line
        moveCursorUp();
        break;
      case Keyboard.KEY_DOWN:
        // Move cursor down to the next line
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
    int lineIndex = findLineIndexByCursorPos();
    String currentLine = lines.size() > lineIndex ? lines.get(lineIndex) : "";
    int columnInLine = cursorPos - sumLengthsUpTo(lineIndex);

    // Ensure columnInLine does not exceed current line's length
    columnInLine = Math.min(columnInLine, currentLine.length());

    String lineBeforeCursor = currentLine.substring(0, columnInLine);
    String lineAfterCursor = currentLine.substring(columnInLine);
    lines.set(lineIndex, lineBeforeCursor);
    lines.add(lineIndex + 1, lineAfterCursor);

    userInsertedNewLines.add(lineIndex + 1);

    // Adjust cursorPos for the newline. Add 1 to position cursor at the start of the next line.
    cursorPos = sumLengthsUpTo(lineIndex + 1) + 1;
  }

  private void deleteCharacterBeforeCursors() {
    if (cursorPos <= 0) {
      return;
    }

    int lineIndex = findLineIndexByCursorPos();
    int offsetForUserDefinedNewLine = userInsertedNewLines.contains(lineIndex) ? 1 : 0;
    int columnInLine = cursorPos - sumLengthsUpTo(lineIndex) - offsetForUserDefinedNewLine;

    // Ensuring columnInLine is not negative for the substring operation
    columnInLine = Math.max(0, columnInLine);

    if (columnInLine == 0 && lineIndex > 0) {
      // If at the start of a line (and not the very first line), attempt to merge with the
      // previous line
      mergeWithPreviousLine(lineIndex);
      // Adjust cursor position after merge
      cursorPos = sumLengthsUpTo(lineIndex - 1) + lines.get(Math.max(0, lineIndex - 1)).length();
      cursorPos +=
          userInsertedNewLines.contains(lineIndex) ? 1 : 0; // Adjust if the line was user-defined
    } else {
      // Normal character deletion within the line
      String currentLine = lines.get(lineIndex);
      String newLine =
          currentLine.substring(0, columnInLine - 1) + currentLine.substring(columnInLine);
      lines.set(lineIndex, newLine);
      cursorPos--;
    }
  }

  private void deleteCharacterBeforeCursor() {
    if (cursorPos <= 0) {
      return;
    }

    int lineIndex = findLineIndexByCursorPos();
    int offsetForUserDefinedNewLine = userInsertedNewLines.contains(lineIndex) ? 1 : 0;
    int columnInLine = cursorPos - sumLengthsUpTo(lineIndex) - offsetForUserDefinedNewLine;
    System.out.println("Column in line: " + columnInLine);
    System.out.println("Line index: " + lineIndex);

    // Ensuring columnInLine is not negative for the substring operation
    columnInLine = Math.max(0, columnInLine);
    if (columnInLine == 0 && lineIndex > 0) {
      // Merge with previous line if at the start of a line
      mergeWithPreviousLine(lineIndex);
    } else {
      String currentLine = lines.get(lineIndex);
      // Adjusting for the case where cursor visually appears after a newline
      int adjustment = userInsertedNewLines.contains(lineIndex + 1) ? 1 : 0;
      columnInLine = Math.max(0, columnInLine - adjustment); // Ensure columnInLine is not negative
      if (columnInLine < currentLine.length()) {
        String newLine =
            currentLine.substring(0, columnInLine) + currentLine.substring(columnInLine + 1);
        lines.set(lineIndex, newLine);
        cursorPos--;
      }
    }
  }


  private void insertCharacterAtCursor(char character) {
    int lineIndex = findLineIndexByCursorPos();
    // Adjustment for cursorPos with user-defined newlines
    int offsetForUserDefinedNewLine = userInsertedNewLines.contains(lineIndex) ? 1 : 0;
    int columnInLine = cursorPos - sumLengthsUpTo(lineIndex) - offsetForUserDefinedNewLine;

    // Ensure columnInLine does not go negative, which could happen with the new adjustment
    columnInLine = Math.max(0, columnInLine);

    String currentLine = lines.size() > lineIndex ? lines.get(lineIndex) : "";
    // Construct the new line with inserted character
    String newLine =
        currentLine.substring(0, columnInLine) + character + currentLine.substring(columnInLine);

    if (fontRenderer.getStringWidth(newLine) > width - 4) {
      // Auto-wrap to a new line if the width exceeds the text field width
      int lastSpaceIndex = newLine.lastIndexOf(' ', columnInLine - 1);
      if (lastSpaceIndex != -1) {
        // Split at the last space in the current line
        lines.set(lineIndex, newLine.substring(0, lastSpaceIndex));
        lines.add(lineIndex + 1, newLine.substring(lastSpaceIndex + 1));
      } else {
        // No space found; split at the current position
        lines.set(lineIndex, currentLine.substring(0, columnInLine));
        lines.add(lineIndex + 1, currentLine.substring(columnInLine) + character);
      }
      cursorPos = sumLengthsUpTo(lineIndex + 1) + 1 + offsetForUserDefinedNewLine;
    } else {
      // No wrapping needed, insert character normally
      lines.set(lineIndex, newLine);
      cursorPos++;
    }
  }

  private void updateViewOffsetForCursor() {
    // Check if the cursor is out of view after backspacing
    int currentLineIndex = findCurrentLine();
    int currentLineHeight = fontRenderer.FONT_HEIGHT;
    int cursorY = (currentLineIndex - viewOffset) * currentLineHeight;
    int maxY = this.height - currentLineHeight;

    if (cursorY < 0) {
      // Cursor is above the view, adjust the view offset immediately
      this.viewOffset = currentLineIndex;
    } else if (cursorY > maxY) {
      // Cursor is below the view, adjust the view offset immediately
      this.viewOffset = currentLineIndex - (this.height / currentLineHeight) + 1;
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
      cursorPos = sumLengthsUpTo(lineIndex - 1) + prevLine.length();
    } else if (prevLine.isEmpty()) {
      // Merge an empty previous line with the current line (essentially removing an empty line)
      lines.remove(lineIndex - 1);
      cursorPos = sumLengthsUpTo(lineIndex - 1);
    } else {
      // Merge lines and remove the current line
      String currentLine = lines.get(lineIndex);
      lines.set(lineIndex - 1, prevLine + currentLine);
      lines.remove(lineIndex);
      cursorPos =
          sumLengthsUpTo(lineIndex - 1) + prevLine.length(); // Move cursor to end of merged line
    }
    adjustUserInsertedNewLines(lineIndex);
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
    int currentLineIndex = findCurrentLine();
    if (currentLineIndex > 0) {
      int currentColumnPosition = findColumnInLine(currentLineIndex);

      // Move to the previous line
      int prevLineIndex = currentLineIndex - 1;
      String prevLine = lines.get(prevLineIndex);

      // Attempt to maintain the same column position in the previous line
      int newPositionWithinPrevLine = Math.min(currentColumnPosition, prevLine.length());

      // Adjust the global cursor position to reflect its position within the previous line
      cursorPos = sumLengthsUpTo(prevLineIndex) + newPositionWithinPrevLine;
    }
  }

  private void moveCursorDown() {
    int currentLineIndex = findCurrentLine();
    if (currentLineIndex < lines.size() - 1) {
      // Determine the relative cursor position within the current line
      int currentLineLength =
          currentLineIndex > 0 ? cursorPos - sumLengthsUpTo(currentLineIndex) : cursorPos;
      int nextLineLength = lines.get(currentLineIndex + 1).length();

      // If the current cursor position within the line exceeds the length of the next line,
      // adjust it.
      int newPositionWithinNextLine = Math.min(currentLineLength, nextLineLength);
      // Adjust the global cursor position to reflect its position within the next line
      cursorPos = sumLengthsUpTo(currentLineIndex + 1) + newPositionWithinNextLine;
    }
  }

  private int findCurrentLine() {
    int totalLength = 0;
    for (int i = 0; i < lines.size(); i++) {
      // Add 1 for each user-inserted newline character before this line
      totalLength += lines.get(i).length() + (userInsertedNewLines.contains(i + 1) ? 1 : 0);
      if (cursorPos <= totalLength) {
        return i;
      }
    }
    return lines.size() - 1;
  }

  private int sumLengthsUpTo(int index) {
    int sum = 0;
    for (int i = 0; i < index; i++) {
      // Include the newline character in the sum for user-inserted new lines
      sum += lines.get(i).length() + (userInsertedNewLines.contains(i + 1) ? 1 : 0);
    }
    return sum;
  }

  private int findColumnInLine(int lineIndex) {
    int lineStartPos = sumLengthsUpTo(lineIndex);
    return cursorPos - lineStartPos;
  }

  public void scrollUp() {
    if (viewOffset > 0) {
      viewOffset--;
    }
  }

  public void scrollDown() {
    if (viewOffset < lines.size() - 1) {
      viewOffset++;
    }
  }


  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    isFocused = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
        && mouseY < this.y + this.height;
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

  private int findLineIndexByCursorPos() {
    // This method needs to determine which line the cursor is currently on based on cursorPos
    // Assuming cursorPos is an overall position index in the text, you would calculate which line
    // it falls on. This is a placeholder for the logic you'll need to implement.
    int totalChars = 0;
    for (int i = 0; i < lines.size(); i++) {
      totalChars += lines.get(i).length();
      if (cursorPos <= totalChars) {
        return i;
      }
    }
    return lines.size() - 1; // Default to last line if not found
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
    StringBuilder currentLine = new StringBuilder();

    for (int index = 0; index < text.length(); index++) {
      char c = text.charAt(index);
      if (c == '\n') {
        lines.add(currentLine.toString());
        currentLine = new StringBuilder();
        userInsertedNewLines.add(lines.size());
      } else {
        currentLine.append(c);
        // Check if adding 'c' exceeds width, considering left padding of 2
        if (fontRenderer.getStringWidth(currentLine.toString()) > width - 4) {
          // Find last space to break line or break at current position if no space
          int lastSpace = currentLine.lastIndexOf(" ");
          if (lastSpace != -1) {
            // If there's a space, split at the last space
            lines.add(currentLine.substring(0, lastSpace));
            // Start new line after the last space
            currentLine = new StringBuilder(currentLine.substring(lastSpace + 1));
          } else {
            // No space found, directly add and reset for new content
            lines.add(currentLine.toString());
            currentLine = new StringBuilder();
          }
        }
      }
    }
    // Add the remaining content as the last line
    if (currentLine.length() > 0) {
      lines.add(currentLine.toString());
    }
    // Reset cursor position
    cursorPos = getText().length();
    // Adjust viewOffset for new content
    viewOffset = Math.max(0, lines.size() - (height / fontRenderer.FONT_HEIGHT));
  }

  public void setFocused(boolean isFocused) {
    this.isFocused = true;
  }

  public boolean isFocused() {
    return true;
  }
}
