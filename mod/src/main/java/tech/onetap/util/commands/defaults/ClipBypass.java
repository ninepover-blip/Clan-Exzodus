package tech.onetap.util.commands.defaults;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Утилита для телепортации с разными типами байпаса.
 * Типы: pos, bypass, vault, steps, fast.
 *
 * Ванильный лимит 1.21.4 (ServerPlayNetworkHandler.onPlayerMove):
 * - "moved too quickly": 100 блоков 3D за пакет (d25 - velocityLenSq > (100*count)^2)
 * - "moved wrongly": после коллизии расхождение с пакетом > 0.25 → кик
 * Значит один пакет до 95 блоков в открытом воздухе = мгновенный реальный ТП.
 */
public final class ClipBypass {

    public static final List<String> BYPASS_TYPES = List.of("pos", "bypass", "vault", "steps", "fast");

    private static long tpSessionEnd = 0;
    private static Vec3d tpTarget;

    // Режим "fast" — реальное серверное перемещение чанками до 95 блоков за пакет.
    // Сервер сам обновляет позицию игрока; дальние дистанции разбиваются на чанки
    // по одному пакету в тик (~1900 блоков/сек на ванили).
    private static boolean fastActive = false;
    private static Vec3d fastTarget;
    private static final double FAST_STEP = 95.0;

    // Режим "steps" — пошаговое перемещение 0.25 блока за тик.
    // Нужен для NCP-подобных серверов с проверкой скорости по тику.
    private static boolean stepsActive = false;
    private static Vec3d stepsTarget;
    private static final double STEP_SIZE = 0.25;
    private static final double MAX_VERTICAL_JUMP = 90.0;

    private ClipBypass() {}

    /**
     * Начинает ТП-сессию: пока она активна, руббербенды (PlayerPositionLookS2CPacket)
     * отменяются миксином, и игрок не откатывается назад.
     */
    public static void beginTpSession(double x, double y, double z, long holdMs) {
        tpTarget = new Vec3d(x, y, z);
        tpSessionEnd = System.currentTimeMillis() + holdMs;
    }

    public static boolean isTpSessionActive() {
        return System.currentTimeMillis() < tpSessionEnd;
    }

    public static Vec3d getTpTarget() {
        return tpTarget;
    }

    public static void endTpSession() {
        tpSessionEnd = 0;
        tpTarget = null;
    }

    /**
     * Выполняет телепортацию на заданную позицию используя указанный тип байпаса.
     *
     * @param targetX  целевая координата X
     * @param targetY  целевая координата Y
     * @param targetZ  целевая координата Z
     * @param bypass   тип байпаса: "pos", "bypass", "vault", "steps", "fast" или null для дефолта
     */
    public static void teleport(double targetX, double targetY, double targetZ, String bypass) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null) return;

        String mode = bypass == null ? "" : bypass.toLowerCase();

        switch (mode) {
            case "bypass" -> bypassMode(player, targetX, targetY, targetZ);
            case "vault" -> vaultMode(player, targetX, targetY, targetZ);
            case "steps" -> stepsMode(player, targetX, targetY, targetZ);
            case "fast" -> fastMode(player, targetX, targetY, targetZ);
            default -> posMode(player, targetX, targetY, targetZ);
        }
    }

    /**
     * Вызывается каждый игровой тик из ModuleStorage — двигает игрока
     * к цели. EventTick постится в HEAD MinecraftClient.tick, т.е. до
     * клиентского тика движения, поэтому конфликтов с собственными
     * пакетами движения нет.
     */
    public static void tick() {
        if (fastActive) tickFast();
        if (stepsActive) tickSteps();
    }

    private static void tickFast() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null || player.isRemoved()) {
            fastActive = false;
            return;
        }

        Entity entity = player.hasVehicle() ? player.getVehicle() : player;
        if (entity == null) {
            fastActive = false;
            return;
        }

        Vec3d pos = entity.getPos();
        Vec3d delta = fastTarget.subtract(pos);
        double dist = delta.length();

        if (dist <= 0.1) {
            fastActive = false;
            beginTpSession(fastTarget.x, fastTarget.y, fastTarget.z, 2000);
            return;
        }

        Vec3d next = dist <= FAST_STEP ? fastTarget : pos.add(delta.multiply(FAST_STEP / dist));
        entity.setPosition(next.x, next.y, next.z);
        if (entity != player) player.setPosition(next.x, next.y, next.z);
        sendMove(entity, next, false);
    }

    private static void tickSteps() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null || player.isRemoved()) {
            stepsActive = false;
            return;
        }

        Vec3d pos = player.getPos();

        // 1) Вертикальный скачок одним пакетом (ваниль разрешает до 100 блоков)
        double dy = stepsTarget.y - pos.y;
        if (Math.abs(dy) > 0.01 && Math.abs(dy) <= MAX_VERTICAL_JUMP) {
            Vec3d next = new Vec3d(pos.x, stepsTarget.y, pos.z);
            player.setPosition(next.x, next.y, next.z);
            sendMove(player, next, false);
            return;
        }

        // 2) Горизонтальные шаги
        double dx = stepsTarget.x - pos.x;
        double dz = stepsTarget.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal <= STEP_SIZE) {
            player.setPosition(stepsTarget.x, stepsTarget.y, stepsTarget.z);
            sendMove(player, stepsTarget, true);
            stepsActive = false;
            beginTpSession(stepsTarget.x, stepsTarget.y, stepsTarget.z, 3000);
            return;
        }

        double scale = STEP_SIZE / horizontal;
        Vec3d next = new Vec3d(
                pos.x + dx * scale,
                stepsTarget.y,
                pos.z + dz * scale
        );
        player.setPosition(next.x, next.y, next.z);
        sendMove(player, next, false);
    }

    private static void sendMove(Entity entity, Vec3d pos, boolean onGround) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null) return;

        if (entity == player) {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    pos.x, pos.y, pos.z, onGround, player.horizontalCollision));
        } else if (player.getVehicle() != null) {
            player.networkHandler.sendPacket(new VehicleMoveC2SPacket(
                    pos, player.getVehicle().getYaw(), player.getVehicle().getPitch(), false));
        }
    }

    /**
     * Fast — реальное серверное перемещение чанками до 95 блоков за пакет.
     * Один пакет в тик: работает на ванильных серверах и Paper без античита.
     */
    private static void fastMode(ClientPlayerEntity player, double x, double y, double z) {
        fastActive = true;
        fastTarget = new Vec3d(x, y, z);
        beginTpSession(x, y, z, 5000);
        tickFast();
    }

    /**
     * Steps — реальное перемещение серверной позиции пошагово (по тику).
     * Работает на ванильных и NCP-подобных серверах без руббербенда.
     */
    private static void stepsMode(ClientPlayerEntity player, double x, double y, double z) {
        stepsActive = true;
        stepsTarget = new Vec3d(x, y, z);
        beginTpSession(x, y, z, 8000);
    }

    /**
     * Pos — простая телепортация: setPosition + один пакет позиции.
     */
    private static void posMode(ClientPlayerEntity player, double x, double y, double z) {
        for (int i = 0; i < 3; i++) {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(
                    player.isOnGround(), player.horizontalCollision));
        }
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, false, player.horizontalCollision));
        player.setPosition(x, y, z);
    }

    /**
     * Bypass — setPosition + подтверждающие пакеты + ТП-сессия удержания.
     * Руббербенд от сервера отменяется, позиция удерживается на точке ТП.
     */
    private static void bypassMode(ClientPlayerEntity player, double x, double y, double z) {
        player.setPosition(x, y, z);
        for (int i = 0; i < 3; i++) {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    x, y, z, true, player.horizontalCollision));
        }
        beginTpSession(x, y, z, 5000);
    }

    /**
     * Vault — телепорт вверх (до 90 блоков, ванильный лимит 100),
     * затем к цели и вниз.
     */
    private static void vaultMode(ClientPlayerEntity player, double x, double y, double z) {
        Entity entity = player.hasVehicle() ? player.getVehicle() : player;
        if (entity == null) return;

        Vec3d currentPos = entity.getPos();
        Vec3d upPos = currentPos.add(0, MAX_VERTICAL_JUMP, 0);
        Vec3d aboveTarget = new Vec3d(x, upPos.y, z);
        Vec3d downPos = new Vec3d(x, y, z);
        Vec3d finalPos = downPos.add(0, 0.01, 0);

        for (int i = 0; i < 13; i++) {
            if (player.hasVehicle() && player.getVehicle() != null) {
                player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(player.getVehicle()));
            } else {
                player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(
                        false, player.horizontalCollision));
            }
        }

        sendVaultMove(player, entity, upPos);
        sendVaultMove(player, entity, aboveTarget);
        sendVaultMove(player, entity, downPos);
        sendVaultMove(player, entity, finalPos);

        entity.setPosition(finalPos.x, finalPos.y, finalPos.z);
        if (entity != player) {
            player.setPosition(finalPos.x, finalPos.y, finalPos.z);
        }
    }

    private static void sendVaultMove(ClientPlayerEntity player, Entity entity, Vec3d pos) {
        if (player.networkHandler == null) return;

        if (entity == player) {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    pos.x, pos.y, pos.z, false, player.horizontalCollision));
        } else if (player.getVehicle() != null) {
            player.networkHandler.sendPacket(new VehicleMoveC2SPacket(
                    pos, player.getVehicle().getYaw(), player.getVehicle().getPitch(), false));
        }
    }
}
