package tech.onetap.module.settings;

public final class StringSetting extends Setting {
    private final int maxLength;
    private String value;

    public StringSetting(String name, String value) {
        this(name, value, 32);
    }

    public StringSetting(String name, String value, int maxLength) {
        super(name);
        this.maxLength = Math.max(1, maxLength);
        setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        String nextValue = value == null ? "" : value;
        if (nextValue.length() > maxLength) {
            nextValue = nextValue.substring(0, maxLength);
        }
        this.value = nextValue;
    }

    public void append(char chr) {
        if (value.length() < maxLength) {
            value += chr;
        }
    }

    public void backspace() {
        if (!value.isEmpty()) {
            value = value.substring(0, value.length() - 1);
        }
    }

    public void clear() {
        value = "";
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        setValue(value);
    }

    @Override
    public StringSetting setVisible(java.util.function.Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
