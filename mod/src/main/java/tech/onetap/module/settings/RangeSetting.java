package tech.onetap.module.settings;

public final class RangeSetting extends Setting {
    private final double min;
    private final double max;
    private double from;
    private double to;

    public RangeSetting(String name, double min, double max, double from, double to) {
        super(name);
        this.min = min;
        this.max = max;
        setValues(from, to);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getFrom() {
        return from;
    }

    public double getTo() {
        return to;
    }

    public void setValues(double from, double to) {
        this.from = Math.max(min, Math.min(max, Math.min(from, to)));
        this.to = Math.max(min, Math.min(max, Math.max(from, to)));
    }

    @Override
    public String getValueAsString() {
        return from + ":" + to;
    }

    @Override
    public void setValueFromString(String value) {
        try {
            String[] parts = value.split(":");
            setValues(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } catch (Exception ex) {
        }
    }

    @Override
    public RangeSetting setVisible(java.util.function.Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
