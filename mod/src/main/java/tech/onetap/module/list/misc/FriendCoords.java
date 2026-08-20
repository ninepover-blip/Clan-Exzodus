package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.world.Heightmap;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.chat.ChatUtil;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Обязательная функция: метка на координаты друга из чата + уведомление о помощи + запрос координат. */
@ModuleInformation(moduleName = "FriendCoords", moduleDesc = "Метка на координаты друга из чата и просьба о помощи", moduleCategory = ModuleCategory.MISC)
public class FriendCoords extends Module {

    private static final Pattern PLAYER_PREFIX = Pattern.compile("(?i)<([A-Za-z0-9_]{1,16})>\\s*(.*)");
    private static final Pattern TEAM_PREFIX = Pattern.compile("(?i)^\\s*(?:\\[[^\\]]{1,32}\\]\\s*)*([A-Za-z0-9_]{1,16})\\s*(?::|»|>|\\|)\\s*(.+)$");
    private static final Pattern COORDS3 = Pattern.compile("(-?\\d{1,8})\\s+(-?\\d{1,8})\\s+(-?\\d{1,8})");
    private static final Pattern COORDS2 = Pattern.compile("(-?\\d{1,8})\\s+(-?\\d{1,8})");
    private static final Pattern HELP = Pattern.compile("(?i)(хелп|хелпа|помог|помощь|спас|help)");

    private static final long MARKER_TTL_MS = 300_000L;
    private static final long NOTIFY_COOLDOWN_MS = 30_000L;

    private final Map<String, Marker> markers = new HashMap<>();
    private final Map<String, Long> lastNotify = new HashMap<>();

    private static final class Marker {
        double x, z;
        double y;
        boolean hasY;
        long time;
    }

    public FriendCoords() {
        setEnabled(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(true);
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE) return;
        if (mc.player == null || mc.world == null) return;

        String message = null;
        String senderName = null;

        if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            message = packet.body().content();
            UUID sender = packet.sender();
            net.minecraft.client.network.PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(sender);
            if (entry != null) {
                senderName = entry.getProfile().getName();
            } else {
                PlayerEntity p = mc.world.getPlayerByUuid(sender);
                if (p != null) senderName = p.getName().getString();
            }
        } else if (event.getPacket() instanceof ProfilelessChatMessageS2CPacket packet) {
            String[] s = extractSenderAndMessage(packet.message().getString());
            senderName = s[0];
            message = s[1];
        } else if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String[] s = extractSenderAndMessage(packet.content().getString());
            senderName = s[0];
            message = s[1];
        }

        if (senderName == null || message == null) return;
        if (!FriendRepository.isFriendName(senderName)) return;

        String clean = message.replaceAll("§.", "");

        double[] coords = parseCoords(clean);
        if (coords != null) {
            boolean hasY = coords.length == 3;
            double x = coords[0], y = hasY ? coords[1] : Double.NaN, z = hasY ? coords[2] : coords[1];
            placeMarker(senderName, x, y, z, hasY);
            Long prev = lastNotify.get(senderName);
            long now = System.currentTimeMillis();
            if (prev == null || now - prev >= NOTIFY_COOLDOWN_MS) {
                lastNotify.put(senderName, now);
                spamHelp(senderName, formatCoords(x, y, z, hasY));
            }
        } else if (HELP.matcher(clean).find()) {
            mc.getNetworkHandler().sendChatMessage("Напиши свои координаты (x y z), я приду!");
            ChatUtil.send(Formatting.RED + "Друг " + Formatting.WHITE + senderName + Formatting.RED
                    + " просит помощи, но не указал координаты — запросили координаты");
        }
    }

    private String[] extractSenderAndMessage(String full) {
        Matcher m = PLAYER_PREFIX.matcher(full);
        if (m.matches()) return new String[]{m.group(1), m.group(2)};
        Matcher t = TEAM_PREFIX.matcher(full);
        if (t.matches()) return new String[]{t.group(1), t.group(2)};
        int sp = full.indexOf(' ');
        if (sp > 0) {
            String first = full.substring(0, sp);
            if (FriendRepository.isFriendName(first)) {
                return new String[]{first, full.substring(sp + 1)};
            }
        }
        return new String[]{null, full};
    }

    private void placeMarker(String name, double x, double y, double z, boolean hasY) {
        Marker marker = new Marker();
        marker.x = x;
        marker.z = z;
        marker.y = y;
        marker.hasY = hasY;
        marker.time = System.currentTimeMillis();
        markers.put(name, marker);
    }

    private void spamHelp(String name, String coords) {
        for (int i = 0; i < 6; i++) {
            ChatUtil.send(Formatting.GOLD + "Друг " + Formatting.WHITE + name
                    + Formatting.GOLD + " ПРОСИТ ПОМОЩИ! Координаты: " + Formatting.WHITE + coords);
        }
        ChatUtil.send(Formatting.AQUA + "Метка поставлена на координаты: " + Formatting.WHITE + coords);
        for (int i = 0; i < 4; i++) {
            mc.getNetworkHandler().sendChatMessage("Друг " + name + " просит помощи! Координаты: " + coords + " — нужна помощь!");
        }
    }

    private String formatCoords(double x, double y, double z, boolean hasY) {
        if (hasY) {
            return String.format(Locale.US, "%.0f %.0f %.0f", x, y, z);
        }
        return String.format(Locale.US, "%.0f %.0f", x, z);
    }

    /** Возвращает {x,y,z} если координат три, {x,z} если две, иначе null. */
    private double[] parseCoords(String text) {
        Matcher m3 = COORDS3.matcher(text);
        if (m3.find()) {
            return new double[]{
                    Integer.parseInt(m3.group(1)),
                    Integer.parseInt(m3.group(2)),
                    Integer.parseInt(m3.group(3))
            };
        }
        Matcher m2 = COORDS2.matcher(text);
        if (m2.find()) {
            return new double[]{
                    Integer.parseInt(m2.group(1)),
                    Integer.parseInt(m2.group(2))
            };
        }
        return null;
    }

    @Subscribe
    private void onWorld(EventWorldRender e) {
        if (mc.world == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        int theme = ColorProvider.getThemeColor();
        markers.entrySet().removeIf(en -> now - en.getValue().time > MARKER_TTL_MS);
        if (markers.isEmpty()) return;

        for (Map.Entry<String, Marker> en : markers.entrySet()) {
            Marker m = en.getValue();
            double bx = m.x, bz = m.z;
            double topY;
            double bottomY = mc.world.getBottomY();
            if (m.hasY) {
                topY = m.y;
            } else {
                int top = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(bx), (int) Math.floor(bz));
                topY = Math.max(mc.player.getY(), top);
            }

            DrawUtil.drawLine(bx, bottomY, bz, bx, topY, bz, theme, 2.5f, false);
            double gy = mc.player.getY() - 1;
            DrawUtil.drawLine(bx - 1, gy, bz, bx + 1, gy, bz, ColorProvider.setAlpha(theme, 160), 1.5f, false);
            DrawUtil.drawLine(bx, gy, bz - 1, bx, gy, bz + 1, ColorProvider.setAlpha(theme, 160), 1.5f, false);
        }
    }

    @Subscribe
    private void onHud(EventHUD e) {
        if (mc.world == null || mc.player == null) return;
        if (markers.isEmpty()) return;
        var context = e.getDrawContext();

        for (Map.Entry<String, Marker> en : markers.entrySet()) {
            Marker m = en.getValue();
            double bx = m.x, bz = m.z;
            double by = m.hasY ? m.y : mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(bx), (int) Math.floor(bz));

            Vector2f screen = ProjectionUtil.project(bx, by + 2.5, bz);
            if (screen.getX() == Float.MAX_VALUE || screen.getY() == Float.MAX_VALUE) continue;

            int dx = (int) (bx - mc.player.getX());
            int dz = (int) (bz - mc.player.getZ());
            int dist = (int) Math.sqrt(dx * dx + dz * dz);

            String label = en.getKey() + "  " + formatCoords(bx, by, bz, m.hasY) + "  " + dist + "m";
            float w = Fonts.SFMEDIUM.get().getWidth(label, 7f);
            DrawUtil.drawRound(screen.getX() - w / 2f - 3f, screen.getY() - 4f, w + 6f, 11f, 3f,
                    ColorProvider.rgba(0, 0, 0, 150));
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), label, screen.getX() - w / 2f, screen.getY() - 1f,
                    ColorProvider.rgba(255, 255, 255, 255), 7f);
        }
    }
}