package minesweeper;

import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.*;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import java.util.Arrays;

public class App {
    final static private long FRAME_TIME = (long) (1e9 / 60);

    private enum Status {
        TITLE, NEW, GAME, WON, LOST
    }

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
    static private double minesRatio = 0.2;

    static private Pos cursorPos = new Pos(5, 5);

    static private boolean bSetFlag = false;
    static private boolean bReveal = false;

    static private char[][] field = null;

    private static void GameDraw() throws IOException {
        TextGraphics textGraphics = screen.newTextGraphics();
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
                for (int j = 0; j < fieldSize; j++)
                    textGraphics.setCharacter(1 + i, 1 + j, TextCharacter.fromCharacter(field[i][j],
                            TextColor.ANSI.DEFAULT, TextColor.ANSI.MAGENTA)[0]);
            textGraphics.setCharacter(1 + cursorPos.x, 1 + cursorPos.y,
                    TextCharacter.fromCharacter(' ', TextColor.ANSI.DEFAULT, TextColor.ANSI.GREEN)[0]);
            break;
        case WON:
            textGraphics.putString(1, 1, "YOU WON");
            break;
        case LOST:
            textGraphics.putString(1, 1, "YOU LOST");
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

        if ((status == Status.TITLE | status == Status.WON | status == Status.LOST) & (input == 'n' | input == 'N'))
            status = Status.NEW;
    }

    private static void GameLogic() {
        switch (status) {
        case NEW:
            field = new char[fieldSize][];
            for (int i = 0; i < fieldSize; i++) {
                field[i] = new char[fieldSize];
                Arrays.fill(field[i], ' ');
            }

            List<Pos> posVars = new LinkedList<Pos>();
            for (int i = 0; i < fieldSize; i++)
                for (int j = 0; j < fieldSize; j++)
                    posVars.add(new Pos(i, j));

            Random random = new Random();
            for (int i = 0; i < (int) (fieldSize * fieldSize * minesRatio); i++) {
                int posI = random.nextInt(posVars.size());
                Pos minePos = posVars.get(posI);
                field[minePos.x][minePos.y] = 'o';
                posVars.remove(posI);
            }

            status = Status.GAME;
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
                Thread.sleep(sleepTime / (int) 1e6, (int) (sleepTime % (int) 1e6));
            } catch (InterruptedException ignore) {
                break;
            }
            startTime = System.nanoTime();
        }
    }

    public static void main(String[] args) {
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
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
