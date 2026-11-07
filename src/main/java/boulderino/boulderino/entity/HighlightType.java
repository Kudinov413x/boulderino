package boulderino.boulderino.entity;

public enum HighlightType {

    HARD_SEND("Schwieriger Boulder getoppt"),
    HARD_FLASH("Schwieriger Flash"),
    NEW_MAXGRADE_FOR_ATTRIBUTE("Neuer Bestwert in einem Attribut"),
    PROJECT_ZONE("Projekt-Zone erreicht"),
    PROJECT_SEND("Projekt erfolgreich abgeschlossen"),
    SESSION_TRIES("Besonders viele Versuche"),
    SESSION_PEAK("Persönlicher Session-Bestwert"),
    SESSION_FUN("Abwechslungsreiche Session"),
    SESSION_PROJECT("Projektlastige Session"),
    SESSION_HOT_STREAK("Erfolgsserie"),
    PROGRESS("Persönlicher Fortschritt"),
    EFFICIENCY("Besonders effizient geklettert"),
    CONSISTENCY("Konstante Leistung"),
    MILESTONE("Meilenstein erreicht"),
    FLASHRATE("Starke Flash-Rate");

    private final String displayName;

    HighlightType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}