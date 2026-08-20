package tech.onetap.util.text;

import tech.onetap.module.ModuleCategory;

import java.util.Map;

/** UI-only localization. Raw names stay unchanged so existing configs remain compatible. */
public final class RussianUi {
    private static final Map<String, String> MODULES = Map.ofEntries(
            Map.entry("FullBright", "Яркость"), Map.entry("ClickGui", "Меню клиента"),
            Map.entry("Sprint", "Автоспринт"), Map.entry("TriggerBot", "Триггер-бот"),
            Map.entry("Criticals", "Критические удары"), Map.entry("NoRender", "Скрытие эффектов"),
            Map.entry("AutoGApple", "Авто-гэппл"),
            Map.entry("TargetESP", "Подсветка цели"), Map.entry("NoPush", "Без отталкивания"),
            Map.entry("SoulESP", "Подсветка душ"), Map.entry("DragonFly", "Полёт дракона"),
            Map.entry("NoJumpDelay", "Без задержки прыжка"), Map.entry("TeleportBack", "Возврат телепорта"),
            Map.entry("ElytraHelper", "Помощник элитры"), Map.entry("HotbarRefill", "Пополнение хотбара"),
            Map.entry("Flight", "Полёт"), Map.entry("AutoTotem", "Автототем"),
            Map.entry("ClickPearl", "Жемчуг по клику"), Map.entry("Scaffold", "Автомост"),
            Map.entry("ClientSounds", "Звуки клиента"), Map.entry("NoFriendDamage", "Защита друзей"),
            Map.entry("ElytraBooster", "Ускоритель элитры"), Map.entry("FreeCamera", "Свободная камера"),
            Map.entry("SwingAnimations", "Анимации руки"), Map.entry("Predictions", "Траектории"),
            Map.entry("HighJump", "Высокий прыжок"), Map.entry("AutoTpaccept", "Автопринятие телепорта"),
            Map.entry("RPSpoofer", "Подмена ресурспака"), Map.entry("FireFly", "Светлячки"),
            Map.entry("AutoPot", "Автозелья"), Map.entry("NoGround", "Без земли"),
            Map.entry("AntiBot", "Антибот"), Map.entry("DeathCoords", "Координаты смерти"),
            Map.entry("KillSay", "Фраза после убийства"), Map.entry("GuiMove", "Движение в меню"),
            Map.entry("Tracers", "Трассеры"), Map.entry("ElytraMotion", "Управление элитрой"),
            Map.entry("Velocity", "Антиотдача"), Map.entry("ElytraFlight", "Полёт на элитре"),
            Map.entry("ElytraFly", "Элитра-флай"), Map.entry("ElytraJump", "Прыжок на элитре"),
            Map.entry("ViewModel", "Положение рук"), Map.entry("KillEffect", "Эффект убийства"),
            Map.entry("AutoArmor", "Автоброня"), Map.entry("LonyHelper", "Помощник Lony"),
            Map.entry("FtHelper", "Помощник FunTime"), Map.entry("Speed", "Скорость"),
            Map.entry("GrimGlide", "Планирование Grim"), Map.entry("GrimStrafe", "Стрейф Grim"),
            Map.entry("HWHelper", "Помощник HolyWorld"), Map.entry("AutoTool", "Автоинструмент"),
            Map.entry("TapeMouse", "Зажатие мыши"), Map.entry("Ambience", "Окружение"),
            Map.entry("BlockOverlay", "Подсветка блока"), Map.entry("FreeLook", "Свободный обзор"),
            Map.entry("Trails", "Следы"), Map.entry("FastExp", "Быстрый опыт"),
            Map.entry("NameProtect", "Защита ника"), Map.entry("ChinaHat", "Китайская шляпа"),
            Map.entry("AirStuck", "Зависание в воздухе"), Map.entry("AutoSwap", "Автосвап"),
            Map.entry("NoSlow", "Без замедления"), Map.entry("NoWeb", "Без паутины"),
            Map.entry("FakePlayer", "Фальшивый игрок"), Map.entry("Interface", "Интерфейс"),
            Map.entry("TargetHUD", "HUD цели"), Map.entry("ArmorHUD", "HUD брони"),
            Map.entry("AutoEat", "Автоеда"), Map.entry("AutoLeave", "Автовыход"),
            Map.entry("Hide", "Скрытие клиента"), Map.entry("BlockEsp", "Подсветка блоков"),
            Map.entry("TPLoot", "Телепорт к луту"), Map.entry("Step", "Автошаг"),
            Map.entry("TargetStrafe", "Стрейф вокруг цели"), Map.entry("Arrows", "Стрелки на игроков"),
            Map.entry("Item Scroller", "Скролл предметов"), Map.entry("Chest Stealer", "Забор из сундуков"),
            Map.entry("Auto Command", "Автокоманды"), Map.entry("Auto Buyer", "Автопродажа"),
            Map.entry("Water Speed", "Скорость в воде"),
            Map.entry("Jesus", "Хождение по воде"),
            Map.entry("Strafe", "Стрейф"),
            Map.entry("PlayerESP", "Подсветка игроков"),
            Map.entry("Radar", "Радар"),
            Map.entry("LagMeter", "Счетчик лагов"),
            Map.entry("AspectRatio", "Соотношение сторон"),
            Map.entry("CustomCrosshair", "Свой прицел"),
            Map.entry("AntiEffects", "Скрытие эффектов"),
            Map.entry("ShulkerTooltip", "Тултип шалкера"),
            Map.entry("CustomModels", "Кастомные модели")
    );

    private static final Map<String, String> TEXT = Map.ofEntries(
            Map.entry("Mode", "Режим"), Map.entry("MoveFix", "Коррекция движения"),
            Map.entry("Distance", "Дистанция"), Map.entry("Range", "Дальность"),
            Map.entry("Speed", "Скорость"), Map.entry("Delay", "Задержка"),
            Map.entry("Chance", "Шанс"), Map.entry("Strength", "Сила"),
            Map.entry("Horizontal", "По горизонтали"), Map.entry("Vertical", "По вертикали"),
            Map.entry("Only Target", "Только цель"), Map.entry("Only On Ground", "Только на земле"),
            Map.entry("Auto Jump", "Автопрыжок"), Map.entry("Jump", "Прыжок"),
            Map.entry("Stop Distance", "Дистанция остановки"), Map.entry("Lead Distance", "Опережение цели"),
            Map.entry("Pause Aura", "Приостанавливать КиллАуру"), Map.entry("Stop Motion", "Останавливать движение"),
            Map.entry("Fix", "Исправление"), Map.entry("Fix Delay", "Задержка исправления"),
            Map.entry("Color", "Цвет"), Map.entry("First Color", "Первый цвет"),
            Map.entry("Second Color", "Второй цвет"), Map.entry("Custom", "Пользовательский"),
            Map.entry("Default", "Стандартный"), Map.entry("Vanilla", "Обычный"),
            Map.entry("Normal", "Обычный"), Map.entry("Smooth", "Плавный"),
            Map.entry("Strict", "Строгий"), Map.entry("Legit", "Легитный"),
            Map.entry("Bypass", "Обход"), Map.entry("None", "Нет"),
            Map.entry("Old", "Старый"), Map.entry("New", "Новый"),
            Map.entry("Both", "Оба"), Map.entry("Box", "Рамка"),
            Map.entry("Lines", "Линии"), Map.entry("Sides", "Стороны"),
            Map.entry("Day", "День"), Map.entry("Night", "Ночь"),
            Map.entry("Panel", "Панель"), Map.entry("Dropdown", "Выпадающий список"),
            Map.entry("Rage", "Ярость"), Map.entry("Binding...", "Нажмите клавишу..."),
            Map.entry("N/A", "Нет"), Map.entry("Allow Off Ground", "Разрешить в воздухе"),
            Map.entry("Armor Stands", "Стойки для брони"), Map.entry("Auto Leave", "Автовыход"),
            Map.entry("Auto Save", "Автосохранение"), Map.entry("Boost Factor", "Сила ускорения"),
            Map.entry("Break Delay", "Задержка разрушения"), Map.entry("Break Range", "Дальность разрушения"),
            Map.entry("Bypass OnGround", "Обход проверки земли"), Map.entry("Bypass Packets", "Пакеты обхода"),
            Map.entry("Charge Delay", "Задержка заряда"), Map.entry("Down Speed", "Скорость вниз"),
            Map.entry("FireResistance", "Огнестойкость"), Map.entry("FireWork Slot", "Слот фейерверка"),
            Map.entry("Health", "Здоровье"), Map.entry("Height", "Высота"),
            Map.entry("Hurt Time", "Время после удара"), Map.entry("Ignore terrain", "Игнорировать препятствия"),
            Map.entry("InstantHealing", "Мгновенное лечение"), Map.entry("Key", "Клавиша"),
            Map.entry("Leave", "Выход"), Map.entry("Leave Mode", "Режим выхода"),
            Map.entry("Legit Stop", "Легитимная остановка"), Map.entry("Max Self Damage", "Макс. урон себе"),
            Map.entry("Min Damage", "Мин. урон"), Map.entry("Min Samples", "Мин. образцов"),
            Map.entry("No Move", "Только без движения"), Map.entry("NoClip", "Проход сквозь блоки"),
            Map.entry("OnlyOnGround", "Только на земле"), Map.entry("Packets", "Пакеты"),
            Map.entry("Pause In Liquids", "Пауза в жидкости"), Map.entry("Pause While Sneaking", "Пауза при приседании"),
            Map.entry("PauseAura", "Приостанавливать КиллАуру"), Map.entry("Place Delay", "Задержка установки"),
            Map.entry("Place Range", "Дальность установки"), Map.entry("Power", "Мощность"),
            Map.entry("Regeneration", "Регенерация"), Map.entry("Rehook", "Перезацеп"),
            Map.entry("Rotation", "Ротация"), Map.entry("Search Mode", "Режим поиска"),
            Map.entry("Search Radius", "Радиус поиска"), Map.entry("Shift Ticks", "Тики приседания"),
            Map.entry("Smart Check", "Умная проверка"), Map.entry("Snap tick", "Тик доводки"),
            Map.entry("Target Range", "Дальность цели"), Map.entry("Trigger", "Срабатывание"),
            Map.entry("Up Speed", "Скорость вверх"), Map.entry("Use Timer", "Использовать таймер"),
            Map.entry("Wait", "Ожидание"), Map.entry("Walls Range", "Дальность через стены"),
            Map.entry("Water Fix", "Исправление в воде")
    );

    private RussianUi() {}

    public static String module(String value) {
        if ("КиллАура".equals(value)) return "KillAura";
        if ("Таргет Перл".equals(value)) return "TargetPearl";
        if (MODULES.containsKey(value)) return value;
        return MODULES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst().orElse(value);
    }

    public static String text(String value) {
        if (TEXT.containsKey(value)) return value;
        String translated = TEXT.entrySet().stream()
                .filter(entry -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (translated != null) return translated;
        return switch (value) {
            case "Ротация" -> "Rotation";
            case "Сортировка" -> "Sorting";
            case "Дистанция" -> "Distance";
            case "Дистанция (Элитры)" -> "Elytra Distance";
            case "Пре дистанция" -> "Pre Distance";
            case "Таргеты" -> "Targets";
            case "Игроки" -> "Players";
            case "Голые" -> "Unarmored";
            case "Монстры" -> "Monsters";
            case "Животные" -> "Animals";
            case "Сфокусированная" -> "Focused";
            case "Свободный" -> "Free";
            case "Стиль HUD" -> "HUD Style";
            case "Элементы" -> "Elements";
            case "Ватермарка" -> "Watermark";
            case "Координаты" -> "Coordinates";
            case "Активный таргет" -> "Target HUD";
            case "Таргет худ от темы" -> "Theme Target HUD";
            case "Привязанные модули" -> "Key Binds";
            case "Активные модераторы" -> "Staff List";
            case "Бафы" -> "Effects";
            case "Счетчик тотемов" -> "Totem Counter";
            case "Нотификации" -> "Notifications";
            case "СпекТрекер" -> "Spectator Tracker";
            case "Блюр фона" -> "Background Blur";
            case "Задний фон от темы" -> "Theme Background";
            case "Стиль таргета" -> "Target HUD Style";
            case "Интенсивность фона" -> "Background Intensity";
            case "Порог ХП оповещения" -> "Low HP Alert";
            default -> value;
        };
    }
    public static String category(ModuleCategory category) {
        return switch (category) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Render";
            case PLAYER -> "Player";
            case MISC -> "Misc";
            case THEMES -> "Themes";
        };
    }
}
