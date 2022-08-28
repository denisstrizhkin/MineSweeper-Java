package minesweeper;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class App {
  final static private long FRAME_TIME = (long)(1e9 / 60);

  private enum Status { TITLE, NEW, GAME, WON, LOST }

  static private class Pos {
    public int x;
    public int y;

    public Pos(int _x, int _y) {
      x = _x;
      y = _y;
    }
  }

  static private Screen screen = null;
  static private boolean gameQuit = false;
  static private Status status = Status.TITLE;

  static private int fieldSize = 10;
  static private double minesRatio = 0.15;
  static private int numMines;
  static private int fieldsLeft;

  static private Pos cursorPos;
  static private LinkedList<Pos> flags;

  static private boolean bSetFlag = false;
  static private boolean bReveal = false;

  static private char[][] field = null;
  static private char[][] fieldUser = null;

  private static void GameDraw() throws IOException {
    TextGraphics textGraphics = screen.newTextGraphics();
    textGraphics.fill(' ');
    switch (status) {
    case TITLE:
      textGraphics.putString(1, 1, "MINESWEEPER");
      textGraphics.putString(1, 3, "\"N\" - New Game");
      textGraphics.putString(1, 4, "\"Q\" - Quit");
      break;
    case NEW:
      textGraphics.putString(1, 1, "GAME PARAMETERS");
      textGraphics.putString(1, 3, "\"N\" - New Game");
      textGraphics.putString(1, 4, "\"Q\" - Quit");
      break;
    case GAME:
      for (int i = 0; i < fieldSize; i++)
        for (int j = 0; j < fieldSize; j++) {
          if (fieldUser[i][j] == ' ')
            textGraphics.setCharacter(
                1 + i, 1 + j,
                TextCharacter.fromCharacter(field[i][j], TextColor.ANSI.DEFAULT,
                                            TextColor.ANSI.MAGENTA)[0]);
          else
            textGraphics.setCharacter(
                1 + i, 1 + j,
                TextCharacter.fromCharacter(fieldUser[i][j],
                                            TextColor.ANSI.DEFAULT,
                                            TextColor.ANSI.MAGENTA)[0]);
        }

      textGraphics.setCharacter(
          1 + cursorPos.x, 1 + cursorPos.y,
          TextCharacter.fromCharacter(' ', TextColor.ANSI.DEFAULT,
                                      TextColor.ANSI.GREEN)[0]);
      textGraphics.putString(1, 15, "Mines " + numMines);
      textGraphics.putString(1, 16, "Flags: " + (numMines - flags.size()));
      textGraphics.putString(1, 17, "Fields Left: " + fieldsLeft);
      break;
    case WON:
      textGraphics.putString(1, 1, "YOU WON");
      textGraphics.putString(1, 3, "\"N\" - New Game");
      textGraphics.putString(1, 4, "\"Q\" - Quit");
      break;
    case LOST:
      textGraphics.putString(1, 1, "YOU LOST");
      textGraphics.putString(1, 3, "\"N\" - New Game");
      textGraphics.putString(1, 4, "\"Q\" - Quit");
      break;
    }
    screen.refresh();
  }

  private static void GameInput() throws IOException {
    KeyStroke keyStroke = screen.pollInput();
    if (keyStroke == null)
      return;
    char input = keyStroke.getCharacter();

    if (input == 'q' | input == 'Q')
      gameQuit = true;

    if (status == Status.GAME) {
      switch (input) {
      case 'j':
      case 'J':
        if (cursorPos.y != fieldSize - 1)
          cursorPos.y++;
        break;
      case 'k':
      case 'K':
        if (cursorPos.y != 0)
          cursorPos.y--;
        break;
      case 'h':
      case 'H':
        if (cursorPos.x != 0)
          cursorPos.x--;
        break;
      case 'l':
      case 'L':
        if (cursorPos.x != fieldSize - 1)
          cursorPos.x++;
        break;
      case 'f':
      case 'F':
        bSetFlag = true;
        break;
      case 'r':
      case 'R':
        bReveal = true;
        break;
      }
    }

    if ((status == Status.TITLE | status == Status.WON |
         status == Status.LOST) &
        (input == 'n' | input == 'N'))
      status = Status.NEW;
  }

  private static void SetupField() {
    numMines = 0;
    flags = new LinkedList<Pos>();
    fieldsLeft = fieldSize * fieldSize;
    cursorPos = new Pos(fieldSize / 2, fieldSize / 2);

    field = new char[fieldSize][];
    for (int i = 0; i < fieldSize; i++) {
      field[i] = new char[fieldSize];
      Arrays.fill(field[i], ' ');
    }

    fieldUser = new char[fieldSize][];
    for (int i = 0; i < fieldSize; i++) {
      fieldUser[i] = new char[fieldSize];
      Arrays.fill(fieldUser[i], '-');
    }

    List<Pos> posVars = new LinkedList<Pos>();
    for (int i = 0; i < fieldSize; i++)
      for (int j = 0; j < fieldSize; j++)
        posVars.add(new Pos(i, j));

    Random random = new Random();
    for (int i = 0; i < (int)(fieldSize * fieldSize * minesRatio); i++) {
      int posI = random.nextInt(posVars.size());
      Pos minePos = posVars.get(posI);
      field[minePos.x][minePos.y] = 'o';
      numMines++;
      posVars.remove(posI);
    }

    for (int i = 0; i < fieldSize; i++)
      for (int j = 0; j < fieldSize; j++) {
        if (field[i][j] == ' ') {
          int count = 0;
          if (i != 0 && j != 0 && field[i - 1][j - 1] == 'o')
            count++;
          if (i != 0 && field[i - 1][j] == 'o')
            count++;
          if (i != 0 && j != fieldSize - 1 && field[i - 1][j + 1] == 'o')
            count++;
          if (i != fieldSize - 1 && j != 0 && field[i + 1][j - 1] == 'o')
            count++;
          if (i != fieldSize - 1 && field[i + 1][j] == 'o')
            count++;
          if (i != fieldSize - 1 && j != fieldSize - 1 &&
              field[i + 1][j + 1] == 'o')
            count++;
          if (j != 0 && field[i][j - 1] == 'o')
            count++;
          if (j != fieldSize - 1 && field[i][j + 1] == 'o')
            count++;

          if (count > 0)
            field[i][j] = (char)((int)'0' + count);
        }
      }

    status = Status.GAME;
  }

  private static void RevealEmpty(int cX, int cY, LinkedList<Pos> emptyFields) {
    if (cX != -1 && cX != fieldSize && cY != -1 && cY != fieldSize) {
      if (fieldUser[cX][cY] == '-') {
        fieldUser[cX][cY] = ' ';
        if (field[cX][cY] == ' ')
          emptyFields.add(new Pos(cX, cY));
        else
          fieldsLeft--;
      }
    }
  }

  private static void RevealField() {
    if (fieldUser[cursorPos.x][cursorPos.y] == '-') {
      if (field[cursorPos.x][cursorPos.y] == 'o')
        status = Status.LOST;
      if (field[cursorPos.x][cursorPos.y] != ' ') {
        fieldUser[cursorPos.x][cursorPos.y] = ' ';
        fieldsLeft--;
      }
      if (field[cursorPos.x][cursorPos.y] == ' ') {
        LinkedList<Pos> emptyFields = new LinkedList<Pos>();
        emptyFields.add(new Pos(cursorPos.x, cursorPos.y));
        do {
          Pos pos = emptyFields.pop();
          int cX = pos.x;
          int cY = pos.y;
          fieldUser[cX][cY] = ' ';
          fieldsLeft--;
          RevealEmpty(cX - 1, cY - 1, emptyFields);
          RevealEmpty(cX - 1, cY, emptyFields);
          RevealEmpty(cX - 1, cY + 1, emptyFields);
          RevealEmpty(cX, cY - 1, emptyFields);
          RevealEmpty(cX, cY + 1, emptyFields);
          RevealEmpty(cX + 1, cY - 1, emptyFields);
          RevealEmpty(cX + 1, cY, emptyFields);
          RevealEmpty(cX + 1, cY + 1, emptyFields);
        } while (emptyFields.isEmpty() != true);
      }
    }
    bReveal = false;
  }

  private static void SetFlag() {
    if (fieldUser[cursorPos.x][cursorPos.y] == '-' & flags.size() < numMines) {
      flags.add(new Pos(cursorPos.x, cursorPos.y));
      fieldUser[cursorPos.x][cursorPos.y] = 'f';
      fieldsLeft--;
    } else if (fieldUser[cursorPos.x][cursorPos.y] == 'f') {
      int flagI = -1;
      for (int i = 0; i < flags.size(); i++)
        if (flags.get(i).x == cursorPos.x & flags.get(i).y == cursorPos.y)
          flagI = i;
      flags.remove(flagI);
      fieldUser[cursorPos.x][cursorPos.y] = '-';
      fieldsLeft++;
    }
    bSetFlag = false;
  }

  private static void GameLogic() {
    switch (status) {
    case NEW:
      SetupField();
      break;
    case GAME:
      if (bReveal)
        RevealField();
      if (bSetFlag)
        SetFlag();
      if (fieldsLeft == 0)
        status = Status.WON;
      break;
    }
  }

  private static void GameLoop() throws java.io.IOException {
    long startTime = System.nanoTime();
    long deltaTime;
    long sleepTime;

    while (gameQuit != true) {
      GameDraw();
      GameInput();
      GameLogic();

      deltaTime = startTime - System.nanoTime();
      sleepTime = deltaTime > FRAME_TIME ? 0 : FRAME_TIME - deltaTime;
      try {
        Thread.sleep(sleepTime / (int)1e6, (int)(sleepTime % (int)1e6));
      } catch (InterruptedException ignore) {
        break;
      }
      startTime = System.nanoTime();
    }
  }

  public static void main(String[] args) {
    DefaultTerminalFactory defaultTerminalFactory =
        new DefaultTerminalFactory();
    try {
      Terminal terminal = defaultTerminalFactory.createTerminal();
      screen = new TerminalScreen(terminal);

      screen.startScreen();
      screen.setCursorPosition(null);

      GameLoop();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (screen != null) {
        try {
          screen.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
