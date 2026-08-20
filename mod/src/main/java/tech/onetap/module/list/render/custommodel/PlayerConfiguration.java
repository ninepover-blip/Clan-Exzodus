package tech.onetap.module.list.render.custommodel;

import java.util.ArrayList;
import java.util.List;

/** Конфиг игрока: список активных OptiFine-предметов. */
public class PlayerConfiguration {

    public static final PlayerConfiguration EMPTY = new PlayerConfiguration(List.of());

    private final List<PlayerConfigurationManager.PlayerItemConfig> items;

    public PlayerConfiguration(List<PlayerConfigurationManager.PlayerItemConfig> items) {
        this.items = items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<String> getActiveTypes() {
        List<String> types = new ArrayList<>();
        for (PlayerConfigurationManager.PlayerItemConfig item : items) {
            if (!types.contains(item.type())) types.add(item.type());
        }
        return types;
    }
}
