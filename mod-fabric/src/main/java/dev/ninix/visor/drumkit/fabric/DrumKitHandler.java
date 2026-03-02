package dev.ninix.visor.drumkit.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.VRLocalPlayer;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.common.HandType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public class DrumKitHandler {

    private Vec3 lastMainHandPos = null;
    private Vec3 lastOffHandPos = null;

    private boolean wasMainOnNoteBlock = false;
    private boolean wasOffOnNoteBlock = false;

    private static final double MIN_SPEED_FOR_HIT = 0.1;
    private static final double COOLDOWN_SECONDS = 0.1;

    private double lastMainHitTime = 0;
    private double lastOffHitTime = 0;
    private double lastMouseHitTime = 0;

    private boolean wasMouseLmbPressed = false;
    private boolean wasMouseRmbPressed = false;

    public DrumKitHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private double lastTickTime = 0;

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        double currentTime = System.currentTimeMillis() / 1000.0;
        double deltaTime = Math.min(0.1, currentTime - lastTickTime); // todo hmm, omaigadddd
        lastTickTime = currentTime;

        checkMouseClick(mc, currentTime);

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();
        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.RENDER);

            Vec3 currentMain = toVec3(pose.getMainHand().getPosition());
            if (lastMainHandPos != null) {
                wasMainOnNoteBlock = checkDrumHit(mc, currentMain, lastMainHandPos, HandType.MAIN, InteractionHand.MAIN_HAND, wasMainOnNoteBlock, lastMainHitTime, currentTime);
                if (wasMainOnNoteBlock && currentTime - lastMainHitTime > COOLDOWN_SECONDS) {
                    lastMainHitTime = currentTime;
                }
            }
            lastMainHandPos = currentMain;

            Vec3 currentOff = toVec3(pose.getOffhand().getPosition());
            if (lastOffHandPos != null) {
                wasOffOnNoteBlock = checkDrumHit(mc, currentOff, lastOffHandPos, HandType.OFFHAND, InteractionHand.OFF_HAND, wasOffOnNoteBlock, lastOffHitTime, currentTime);
                if (wasOffOnNoteBlock && currentTime - lastOffHitTime > COOLDOWN_SECONDS) {
                    lastOffHitTime = currentTime;
                }
            }
            lastOffHandPos = currentOff;
        }
    }

    // todo если всё гуд феникс, то уберу проверку на мышку
    private void checkMouseClick(Minecraft mc, double currentTime) {
        boolean hasStickInMainHand = mc.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.STICK);
        boolean hasStickInOffHand = mc.player.getItemInHand(InteractionHand.OFF_HAND).is(Items.STICK);
        boolean hasStick = hasStickInMainHand || hasStickInOffHand;

        if (!hasStick) {
            long window = mc.getWindow().getWindow();
            wasMouseLmbPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            wasMouseRmbPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            return;
        }

        long window = mc.getWindow().getWindow();
        boolean lmbPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmbPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        boolean lmbJustPressed = lmbPressed && !wasMouseLmbPressed;
        boolean rmbJustPressed = rmbPressed && !wasMouseRmbPressed;

        wasMouseLmbPressed = lmbPressed;
        wasMouseRmbPressed = rmbPressed;

        if ((lmbJustPressed || rmbJustPressed) && (currentTime - lastMouseHitTime > COOLDOWN_SECONDS)) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                if (mc.level.getBlockState(pos).getBlock() == Blocks.NOTE_BLOCK) {
                    playDrumSound(mc, pos, 1.0, null);
                    spawnDrumParticles(mc, hit.getLocation());
                    lastMouseHitTime = currentTime;
                }
            }
        }
    }

    private boolean checkDrumHit(Minecraft mc, Vec3 current, Vec3 last, HandType vrHand, InteractionHand mcHand, boolean wasOnNoteBlock, double lastHitTime, double currentTime) { // todo
        BlockPos pos = BlockPos.containing(current.x, current.y, current.z);
        boolean isOnNoteBlock = mc.level.getBlockState(pos).getBlock() == Blocks.NOTE_BLOCK;

        boolean hasStickInHand = mc.player.getItemInHand(mcHand).is(Items.STICK);

        Vec3 movement = current.subtract(last);
        double speed = movement.length();

        if (hasStickInHand && isOnNoteBlock && speed > MIN_SPEED_FOR_HIT && (currentTime - lastHitTime > COOLDOWN_SECONDS)) {
            playDrumSound(mc, pos, speed, vrHand);
            spawnDrumParticles(mc, current);
            return true;
        }

        return isOnNoteBlock;
    }

    private void playDrumSound(Minecraft mc, BlockPos pos, double speed, HandType handType) {
        float volume = (float) Math.min(speed * 2.0, 1.0);
        float pitch = 0.5f + (float)(Math.random() * 0.3);

        mc.level.playLocalSound(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            SoundEvents.NOTE_BLOCK_BASS.value(),
            SoundSource.BLOCKS,
            volume,
            pitch,
            false
        );

        if (handType != null) {
            float amplitude = (float) Math.min(speed * 3.0, 0.9);
            float frequency = 150.0f;
            float duration = 0.08f;

            VisorAPI.client().getInputManager().triggerHapticPulse(
                handType,
                frequency,
                amplitude,
                duration
            );
        }
    }

    private void spawnDrumParticles(Minecraft mc, Vec3 pos) {
        int particleCount = 8;

        for (int i = 0; i < particleCount; i++) {
            mc.level.addParticle(
                ParticleTypes.NOTE,
                pos.x + (Math.random() - 0.5) * 0.6,
                pos.y + 0.5 + Math.random() * 0.3,
                pos.z + (Math.random() - 0.5) * 0.6,
                (Math.random() - 0.5) * 0.05,
                Math.random() * 0.1,
                (Math.random() - 0.5) * 0.05
            );
        }

        if (Math.random() > 0.5) {
            mc.level.addParticle(
                ParticleTypes.HAPPY_VILLAGER,
                pos.x + (Math.random() - 0.5) * 0.4,
                pos.y + 0.6,
                pos.z + (Math.random() - 0.5) * 0.4,
                0,
                0.05,
                0
            );
        }
    }

    private Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
