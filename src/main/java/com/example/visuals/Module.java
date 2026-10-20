package com.example.visuals;

import java.util.List;

/** A single visual module (toggle). Effects are added in the next step. */
public class Module {
    public final String name;
    public boolean enabled;

    public Module(String name) {
        this.name = name;
    }

    public void toggle() {
        enabled = !enabled;
    }

    /** Placeholder list; real visual effects get hooked to these toggles later. */
    public static final List<Module> ALL = List.of(
            new Module("Fullbright"),
            new Module("Zoom"),
            new Module("Custom Crosshair"),
            new Module("No Hurt Cam"),
            new Module("Item Physics"),
            new Module("Hit Color")
    );
}
