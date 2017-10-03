package helper;

import helper.Event;

import java.util.ArrayList;
import java.util.List;

public class SentEvents {
    public int sentId;
    public List<Event> events;

    public SentEvents () {
        this.events = new ArrayList<>();
    }
}
