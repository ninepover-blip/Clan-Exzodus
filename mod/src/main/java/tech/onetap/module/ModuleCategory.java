package tech.onetap.module;

public enum ModuleCategory {
    COMBAT,
    MOVEMENT,
    RENDER,
    PLAYER,
    MISC,
    THEMES;

    public String getDisplayName() {
        return switch (this) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Render";
            case PLAYER -> "Player";
            case MISC -> "Misc";
            case THEMES -> "Themes";
        };
    }
}