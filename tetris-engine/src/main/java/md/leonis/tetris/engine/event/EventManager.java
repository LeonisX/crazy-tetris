package md.leonis.tetris.engine.event;

import java.util.*;

public abstract class EventManager {

    private Map<Event, List<GameEventListener>> listeners = new EnumMap<>(Event.class);

    public void initializeEvents() {
        Arrays.stream(Event.values()).forEach(event -> listeners.put(event, new ArrayList<>()));
    }

    public void addListener(Event event, GameEventListener listener) {
        List<GameEventListener> eventListeners = listeners.get(event);
        eventListeners.add(listener);
    }

    public void removeListener(Event event, GameEventListener listener) {
        List<GameEventListener> eventListeners = listeners.get(event);
        eventListeners.remove(listener);
    }

    public void notify(Event event, String message) {
        List<GameEventListener> eventListeners = listeners.get(event);
        for (GameEventListener listener : eventListeners) {
            listener.notify(event, message);
        }
    }
}
