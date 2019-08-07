package md.leonis.tetris.engine.event;

public enum GameEvent {

    MOVE_LEFT, MOVE_RIGHT, ROTATE_LEFT, ROTATE_RIGHT, STEP_DOWN, FALL_DOWN, PAUSE, CONTINUE, SAVE, EXIT,

    START_GAME_A, START_GAME_B, START_GAME, GAME_OVER, NEXT_LEVEL,

    REPAINT, UPDATE_SCORE,

    PLAY_SOUND,
    START_LOOPING_SOUND,
    STOP_LOOPING_SOUND,
    FADE_LOOPING_SOUND,
    SUPPORT_LOOPING_SOUNDS,

    ZZZ_UNKNOWN

}