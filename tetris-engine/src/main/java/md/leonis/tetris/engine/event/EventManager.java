package md.leonis.tetris.engine.event;

import java.util.*;

public abstract class EventManager {

    private Map<GameEvent, List<GameEventListener>> listeners = new EnumMap<>(GameEvent.class);

    public void initializeEvents() {
        Arrays.stream(GameEvent.values()).forEach(event -> listeners.put(event, new ArrayList<>()));
    }

    public void addListener(GameEvent event, GameEventListener listener) {
        List<GameEventListener> eventListeners = listeners.get(event);
        eventListeners.add(listener);
    }

    public void removeListener(GameEvent event, GameEventListener listener) {
        List<GameEventListener> eventListeners = listeners.get(event);
        eventListeners.remove(listener);
    }

    public void notify(GameEvent event, String message) {
        List<GameEventListener> eventListeners = listeners.get(event);
        for (GameEventListener listener : eventListeners) {
            listener.notify(event, message);
        }
    }
}
