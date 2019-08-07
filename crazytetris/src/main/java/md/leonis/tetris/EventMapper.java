package md.leonis.tetris;

import md.leonis.tetris.engine.event.GameEvent;

import java.util.HashMap;

import static java.awt.event.KeyEvent.*;
import static md.leonis.tetris.engine.event.GameEvent.*;

public class EventMapper extends HashMap<Integer, GameEvent> {

    public EventMapper() {
        put(VK_LEFT, MOVE_LEFT);
        put(VK_RIGHT, MOVE_RIGHT);
        put(VK_DOWN, STEP_DOWN);
        put(VK_SPACE, FALL_DOWN);
        put(VK_INSERT, ROTATE_LEFT);
        put(VK_UP, ROTATE_RIGHT);
        put(VK_P, PAUSE);
        put(VK_ESCAPE, EXIT);
    }

    public GameEvent map(int keyCode) {
        GameEvent result = get(keyCode);
        if (result == null) {
            return GameEvent.ZZZ_UNKNOWN;
        } else {
            return result;
        }
    }
}
