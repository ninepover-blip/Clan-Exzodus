package tech.onetap.util.commands.defaults;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import tech.onetap.util.commands.api.Command;
import tech.onetap.util.commands.api.argument.IArgConsumer;
import tech.onetap.util.commands.api.exception.CommandException;

import java.util.List;
import java.util.stream.Stream;

public class TPCommand extends Command {
    public TPCommand() {
        super("tp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (player == null || world == null) {
            logDirect(Formatting.RED + "Игрок не найден.");
            return;
        }

        String input = args.getString();

        double targetX, targetY, targetZ;
        String bypass = "fast";

        String lower = input.toLowerCase();

        if (lower.equals("me")) {
            logDirect(Formatting.RED + "Используй: .tp <ник> | .tp x y z");
            return;
        }

        // Телепорт к игроку по нику
        Entity targetEntity = null;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof PlayerEntity p && !p.isRemoved() && p != player
                    && p.getGameProfile().getName().equalsIgnoreCase(lower)) {
                targetEntity = p;
                break;
            }
        }

        if (targetEntity != null) {
            targetX = targetEntity.getX();
            targetY = targetEntity.getY();
            targetZ = targetEntity.getZ();
        } else {
            // Телепорт к координатам: x y z
            try {
                targetX = Double.parseDouble(input);
                targetY = Double.parseDouble(args.getString());
                targetZ = Double.parseDouble(args.getString());
            } catch (NumberFormatException e) {
                logDirect(Formatting.RED + "Игрок \"" + input + "\" не найден. Формат: .tp <ник> | .tp x y z");
                return;
            }
        }

        // Необязательный аргумент — тип байпаса (pos/bypass/vault)
        if (args.hasAny()) {
            bypass = args.getString().toLowerCase();
            if (!ClipBypass.BYPASS_TYPES.contains(bypass)) {
                logDirect(Formatting.RED + "Неизвестный тип байпаса: " + bypass);
                logDirect(Formatting.GRAY + "Доступные: " + String.join(", ", ClipBypass.BYPASS_TYPES));
                return;
            }
        }

        ClipBypass.teleport(targetX, targetY, targetZ, bypass);

        if (targetEntity != null) {
            logDirect("Телепортировано к игроку " + Formatting.AQUA + ((PlayerEntity) targetEntity).getGameProfile().getName()
                    + Formatting.GREEN + (bypass != null ? " [" + bypass + "]" : ""));
        } else {
            logDirect("Телепортировано на " + Formatting.AQUA
                    + String.format("%.1f %.1f %.1f", targetX, targetY, targetZ)
                    + Formatting.GREEN + (bypass != null ? " [" + bypass + "]" : ""));
        }

        if (!bypass.equals("bypass")) {
            logDirect(Formatting.GRAY + "Если откатило назад — на сервере античит (Grim/Polar/NCP),"
                    + " клиентский ТП там не работает. Попробуй .tp ... steps или телепорт серверными командами/перлом.");
        }
    }

    @Override
    public String getShortDesc() {
        return "Телепорт к игроку или координатам";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "Телепортирует к игроку по нику или на координаты",
                "",
                "> .tp <ник> — телепорт к игроку",
                "> .tp x y z — телепорт на координаты",
                "",
                "Необязательный последний аргумент — тип байпаса:",
                "> .tp <ник> [fast|steps|pos|bypass|vault]",
                "> .tp x y z [fast|steps|pos|bypass|vault]",
                "",
                "fast — реальный ТП чанками до 95 блоков за пакет (ваниль, быстро)",
                "steps — реальный ТП шагами 0.25 блока/тик (NCP)",
                "bypass — мгновенный с удержанием от руббербенда",
                "vault — телепорт через верх (до 90 блоков)",
                "pos — простая телепортация",
                "",
                "На серверах с античитом (Grim/Polar/NCP) клиентский ТП не работает",
                "— только ваниль/Paper или серверные телепорты (tpa/warp/перлы)."
        );
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            String prefix = args.peekString().toLowerCase();
            return MinecraftClient.getInstance().world.getPlayers().stream()
                    .filter(p -> p != MinecraftClient.getInstance().player)
                    .map(p -> p.getGameProfile().getName())
                    .filter(name -> name.toLowerCase().startsWith(prefix));
        }
        return Stream.empty();
    }
}
