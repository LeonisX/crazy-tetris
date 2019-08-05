package md.leonis.tetris.engine.event;

public interface GameEventListener {

    void notify(Event event, String message);
}
