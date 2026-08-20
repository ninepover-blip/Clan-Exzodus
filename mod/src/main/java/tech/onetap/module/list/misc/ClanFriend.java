package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.friend.FriendRepository;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInformation(moduleName = "ClanFriend", moduleDesc = "Добавляет всех сокланов в друзья", moduleCategory = ModuleCategory.MISC)
public class ClanFriend extends Module {
    private static final Pattern NICK = Pattern.compile("(?i)(?:^|[\\s,;|»>:\\-])([A-Za-z0-9_]{3,16})(?=$|[\\s,;|«<:\\-])");
    private static final Set<String> IGNORED = Set.of("clan", "info", "online", "offline", "leader", "owner", "member", "members", "rank");
    private final Set<String> pendingNames = new LinkedHashSet<>();
    private boolean commandSent;
    private long finishAt;

    @Override
    public void onEnable() {
        super.onEnable();
        commandSent = false;
        finishAt = 0L;
        pendingNames.clear();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!commandSent) {
            mc.getNetworkHandler().sendChatMessage("/clan info");
            commandSent = true;
            finishAt = System.currentTimeMillis() + 5000L;
        }
        if (!pendingNames.isEmpty()) {
            String name = pendingNames.iterator().next();
            pendingNames.remove(name);
            FriendRepository.addFriend(name);
        }
        if (finishAt > 0L && pendingNames.isEmpty() && System.currentTimeMillis() >= finishAt) {
            setEnabled(false);
        }
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE) return;
        String message = null;
        if (event.getPacket() instanceof ChatMessageS2CPacket packet) message = packet.body().content();
        else if (event.getPacket() instanceof ProfilelessChatMessageS2CPacket packet) message = packet.message().getString();
        else if (event.getPacket() instanceof GameMessageS2CPacket packet) message = packet.content().getString();
        if (message == null) return;

        Matcher matcher = NICK.matcher(message.replaceAll("§.", ""));
        while (matcher.find()) {
            String name = matcher.group(1);
            String lower = name.toLowerCase(Locale.ROOT);
            if (!IGNORED.contains(lower) && (mc.player == null || !name.equalsIgnoreCase(mc.player.getNameForScoreboard()))) {
                pendingNames.add(name);
                finishAt = System.currentTimeMillis() + 1500L;
            }
        }
    }
}
