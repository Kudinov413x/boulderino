package boulderino.boulderino.entity;

public enum Grade {
    FB1("1"),
    FB1_PLUS("1+"),
    FB2("2"),
    FB2_PLUS("2+"),
    FB3("3"),
    FB3_PLUS("3+"),
    FB4("4"),
    FB4_PLUS("4+"),
    FB5("5"),
    FB5_PLUS("5+"),
    FB6("6"),
    FB6_PLUS("6+"),
    FB6_A("6a"),
    FB6_B("6b"),
    FB6_C("6c"),
    FB7("7"),
    FB7_PLUS("7+"),
    FB7_A("7a"),
    FB7_B("7b"),
    FB7_C("7c"),
    FB8("8"),
    FB8_PLUS("8+");
    
    private final String displayName;

    Grade(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
