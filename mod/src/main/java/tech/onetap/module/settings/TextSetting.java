package tech.onetap.module.settings;

public class TextSetting extends Setting {

    private String value;

    public TextSetting(String name, String value) {
        super(name);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TextSetting setVisible(java.util.function.Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        this.value = value;
    }
}
