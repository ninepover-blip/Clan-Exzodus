package tech.onetap.util.config;

import com.google.gson.*;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.settings.*;
import tech.onetap.module.settings.impl.Theme;

import java.io.IOException;
import java.io.Reader;
import java.awt.Desktop;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FOLDER = Paths.get(".options/configs");

    public static void save(String name) {
        saveInternal(name);
    }

    public static boolean saveInternal(String name) {
        JsonObject root = new JsonObject();

        for (Module module : Onetap.getInstance().getModuleStorage().getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("enabled", module.isEnabled());
            moduleObject.addProperty("keybind", module.getKey());

            JsonObject settingsObject = new JsonObject();
            for (Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof BindSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof ModeSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof SliderSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof ThemeSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue().name);
                } else if (setting instanceof ModeListSetting s) {
                    JsonArray enabledModes = new JsonArray();
                    for (BooleanSetting sub : s.getSettings()) {
                        JsonObject entry = new JsonObject();
                        entry.addProperty("name", sub.getName());
                        entry.addProperty("enabled", sub.getValue());
                        enabledModes.add(entry);
                    }
                    settingsObject.add(setting.getName(), enabledModes);
                } else if (setting instanceof TextSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof MultiSelectSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValueAsString());
                } else if (setting instanceof ActionBindSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getKeyCode());
                } else if (setting instanceof StringSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValue());
                } else if (setting instanceof RangeSetting s) {
                    settingsObject.addProperty(setting.getName(), s.getValueAsString());
                }
            }

            moduleObject.add("settings", settingsObject);

            JsonObject bindsObject = new JsonObject();
            for (Setting setting : module.getSettings()) {
                if (!setting.isBound()) continue;
                JsonObject bind = new JsonObject();
                bind.addProperty("key", setting.getKey());
                if (setting.getBindValue() != null) {
                    bind.addProperty("value", setting.getBindValue());
                }
                bindsObject.add(setting.getName(), bind);
            }
            moduleObject.add("binds", bindsObject);

            root.add(module.getName(), moduleObject);
        }

        try {
            Files.createDirectories(CONFIG_FOLDER);
            Path configFile = CONFIG_FOLDER.resolve(name + ".json");
            Files.write(configFile, gson.toJson(root).getBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void load(String name) {
        Path configFile = CONFIG_FOLDER.resolve(name + ".json");
        if (!Files.exists(configFile)) return;

        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            for (Module module : Onetap.getInstance().getModuleStorage().getModules()) {
                if (!root.has(module.getName())) continue;
                JsonObject moduleObject = root.getAsJsonObject(module.getName());

                if (moduleObject.has("enabled") && !module.getName().equals("Click Gui")) {
                    boolean enabled = moduleObject.get("enabled").getAsBoolean();
                    module.setEnabled(enabled);
                }
                if (moduleObject.has("keybind")) {
                    int keybind = moduleObject.get("keybind").getAsInt();
                    module.setKey(keybind);
                }
                if (moduleObject.has("settings")) {
                    JsonObject settingsObject = moduleObject.getAsJsonObject("settings");
                    for (Setting setting : module.getSettings()) {
                        if (!settingsObject.has(setting.getName())) continue;

                        JsonElement element = settingsObject.get(setting.getName());
                        if (setting instanceof BooleanSetting s) {
                            s.setValue(element.getAsBoolean());
                        } else if (setting instanceof BindSetting s) {
                            s.setValue(element.getAsInt());
                        } else if (setting instanceof ModeSetting s) {
                            s.setValue(element.getAsString());
                        } else if (setting instanceof SliderSetting s) {
                            s.setValue(element.getAsDouble());
                        } else if (setting instanceof ThemeSetting s) {
                            String themeName = element.getAsString();
                            for (Theme theme : s.getThemes()) {
                                if (theme.name.equals(themeName)) {
                                    s.setValue(theme);
                                    break;
                                }
                            }
                        } else if (setting instanceof ModeListSetting s && element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            boolean newFormat = !array.isEmpty() && array.get(0).isJsonObject();
                            if (newFormat) {
                                for (BooleanSetting subSetting : s.getSettings()) {
                                    subSetting.setValue(false);
                                }
                                for (JsonElement entry : array) {
                                    String elementName = entry.getAsJsonObject().get("name").getAsString();
                                    boolean enabled = entry.getAsJsonObject().get("enabled").getAsBoolean();
                                    for (BooleanSetting subSetting : s.getSettings()) {
                                        if (subSetting.getName().equals(elementName)) {
                                            subSetting.setValue(enabled);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                List<String> enabled = new ArrayList<>();
                                for (JsonElement e : array) {
                                    enabled.add(e.getAsString());
                                }
                                for (BooleanSetting subSetting : s.getSettings()) {
                                    if (enabled.contains(subSetting.getName())) {
                                        subSetting.setValue(true);
                                    }
                                }
                            }
                        } else if (setting instanceof TextSetting s) {
                            s.setValue(element.getAsString());
                        } else if (setting instanceof MultiSelectSetting s) {
                            s.setValueFromString(element.getAsString());
                        } else if (setting instanceof ActionBindSetting s) {
                            s.setKeyCode(element.getAsInt());
                        } else if (setting instanceof StringSetting s) {
                            s.setValue(element.getAsString());
                        } else if (setting instanceof RangeSetting s) {
                            s.setValueFromString(element.getAsString());
                        }
                    }
                }
                if (moduleObject.has("binds")) {
                    JsonObject bindsObject = moduleObject.getAsJsonObject("binds");
                    for (Setting setting : module.getSettings()) {
                        if (!bindsObject.has(setting.getName())) continue;
                        JsonObject bind = bindsObject.getAsJsonObject(setting.getName());
                        if (bind.has("key")) setting.setKey(bind.get("key").getAsInt());
                        if (bind.has("value")) setting.setBindValue(bind.get("value").getAsString());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delete(String name) {
        Path configFile = CONFIG_FOLDER.resolve(name + ".json");
        try {
            Files.deleteIfExists(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteInternal(String name) {
        Path configFile = CONFIG_FOLDER.resolve(name + ".json");
        try {
            return Files.deleteIfExists(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getConfigs() {
        List<String> configs = listConfigs();
        return configs;
    }

    public static List<String> listConfigs() {
        List<String> configs = new ArrayList<>();
        try {
            if (Files.exists(CONFIG_FOLDER)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(CONFIG_FOLDER, "*.json")) {
                    for (Path path : stream) {
                        String fileName = path.getFileName().toString();
                        if (fileName.endsWith(".json")) {
                            String name = fileName.substring(0, fileName.length() - 5);
                            if (name.equalsIgnoreCase("AutoCfg")) continue;
                            configs.add(name);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configs;
    }

    public static String normalizeConfigName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        name = name.replaceAll("[^a-zA-Z0-9_\\- ]", "");
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        return name;
    }

    public static void openConfigDirectory() {
        try {
            Files.createDirectories(CONFIG_FOLDER);
            String path = CONFIG_FOLDER.toAbsolutePath().toString();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("explorer.exe", "/select,", path).start();
            } else {
                Desktop.getDesktop().open(CONFIG_FOLDER.toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAutoConfig() {
        saveInternal("AutoCfg");
    }

    public static void loadAutoConfig() {
        load("AutoCfg");
    }

    private static long lastAutoSave = 0L;

    public static void tickAutoSave() {
        long now = System.currentTimeMillis();
        if (now - lastAutoSave >= 400L) {
            lastAutoSave = now;
            saveInternal("AutoCfg");
        }
    }
}