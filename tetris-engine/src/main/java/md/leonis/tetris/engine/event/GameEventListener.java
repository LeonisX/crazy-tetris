package md.leonis.tetris.engine.event;

public interface GameEventListener {

    void notify(GameEvent event, String message);
}
