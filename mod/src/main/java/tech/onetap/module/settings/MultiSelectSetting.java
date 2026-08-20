package tech.onetap.module.settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MultiSelectSetting extends Setting {
    private final List<String> options;
    private final Set<String> selected = new LinkedHashSet<>();

    public MultiSelectSetting(String name, String... options) {
        super(name);
        this.options = Collections.unmodifiableList(Arrays.asList(options));
    }

    public MultiSelectSetting selected(String... values) {
        for (String value : values) {
            setSelected(value, true);
        }
        return this;
    }

    public List<String> getOptions() {
        return options;
    }

    public Set<String> getSelected() {
        return Collections.unmodifiableSet(selected);
    }

    public boolean isSelected(String value) {
        return selected.stream().anyMatch(option -> option.equalsIgnoreCase(value));
    }

    public void setSelected(String value, boolean enabled) {
        String option = findOption(value);
        if (option == null) {
            return;
        }

        if (enabled) {
            selected.add(option);
        } else {
            selected.removeIf(selectedOption -> selectedOption.equalsIgnoreCase(option));
        }
    }

    public void toggle(String value) {
        setSelected(value, !isSelected(value));
    }

    public void clear() {
        selected.clear();
    }

    private String findOption(String value) {
        for (String option : options) {
            if (option.equalsIgnoreCase(value)) {
                return option;
            }
        }
        return null;
    }

    @Override
    public String getValueAsString() {
        return String.join(",", selected);
    }

    @Override
    public void setValueFromString(String value) {
        selected.clear();
        if (value == null || value.isEmpty()) {
            return;
        }
        for (String part : value.split(",")) {
            setSelected(part.trim(), true);
        }
    }

    @Override
    public MultiSelectSetting setVisible(java.util.function.Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
