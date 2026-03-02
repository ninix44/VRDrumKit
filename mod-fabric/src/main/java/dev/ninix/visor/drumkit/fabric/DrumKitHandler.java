package dev.ninix.visor.drumkit.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.VRLocalPlayer;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.player.VRPose;
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
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class DrumKitHandler {

    private Vec3 lastMainTipPos = null;
    private Vec3 lastOffTipPos = null;

    private boolean wasMainInside = false;
    private boolean wasOffInside = false;

    private static final double MIN_SPEED_FOR_HIT = 0.03;
    private static final double COOLDOWN_SECONDS = 0.07;

    private double lastMainHitTime = 0;
    private double lastOffHitTime = 0;
    private double lastMouseHitTime = 0;

    private boolean wasMouseLmbPressed = false;
    private boolean wasMouseRmbPressed = false;

    public DrumKitHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        double currentTime = System.currentTimeMillis() / 1000.0;
        checkMouseClick(mc, currentTime);

        VRLocalPlayer vrPlayer = VisorAPI.client().getVRLocalPlayer();
        if (vrPlayer != null && VisorAPI.clientState().playMode().canPlayVR()) {
            PlayerPoseClient pose = vrPlayer.getPoseData(PlayerPoseType.RENDER);

            VRPose mainPose = pose.getMainHand();
            Vec3 currentMainTip = getStickTip(mainPose);
            if (lastMainTipPos != null) {
                wasMainInside = processHandHit(mc, currentMainTip, lastMainTipPos, HandType.MAIN, InteractionHand.MAIN_HAND, wasMainInside, currentTime, true);
            }
            lastMainTipPos = currentMainTip;

            VRPose offPose = pose.getOffhand();
            Vec3 currentOffTip = getStickTip(offPose);
            if (lastOffTipPos != null) {
                wasOffInside = processHandHit(mc, currentOffTip, lastOffTipPos, HandType.OFFHAND, InteractionHand.OFF_HAND, wasOffInside, currentTime, false);
            }
            lastOffTipPos = currentOffTip;
        }
    }

    private Vec3 getStickTip(VRPose handPose) {
        Vector3f offset = new Vector3f(0, 0, -0.38f);

        Vector3f tipJoml = handPose.getCustomVector(offset).add(handPose.getPosition());

        return toVec3(tipJoml);
    }

    private boolean processHandHit(Minecraft mc, Vec3 currentTip, Vec3 lastTip, HandType handType, InteractionHand mcHand, boolean wasInside, double currentTime, boolean isMainHand) {
        BlockPos pos = BlockPos.containing(currentTip.x, currentTip.y, currentTip.z);
        boolean isNowInside = mc.level.getBlockState(pos).is(Blocks.NOTE_BLOCK);
        boolean hasStick = mc.player.getItemInHand(mcHand).is(Items.STICK);

        if (hasStick && isNowInside && !wasInside) {
            Vec3 motion = currentTip.subtract(lastTip);
            double speed = motion.length();
            double lastHitTime = isMainHand ? lastMainHitTime : lastOffHitTime;

            if (speed > MIN_SPEED_FOR_HIT && (currentTime - lastHitTime > COOLDOWN_SECONDS)) {
                playDrumSound(mc, pos, speed, handType);
                spawnDrumParticles(mc, currentTip);

                if (isMainHand) lastMainHitTime = currentTime;
                else lastOffHitTime = currentTime;
            }
        }
        return isNowInside;
    }

    // todo если всё гуд феникс, то уберу проверку на мышку
    private void checkMouseClick(Minecraft mc, double currentTime) {
        boolean hasStick = mc.player.getMainHandItem().is(Items.STICK) || mc.player.getOffhandItem().is(Items.STICK);

        long window = mc.getWindow().getWindow();
        boolean lmbPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmbPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        boolean lmbJustPressed = lmbPressed && !wasMouseLmbPressed;
        boolean rmbJustPressed = rmbPressed && !wasMouseRmbPressed;

        wasMouseLmbPressed = lmbPressed;
        wasMouseRmbPressed = rmbPressed;

        if (!hasStick) return;

        if ((lmbJustPressed || rmbJustPressed) && (currentTime - lastMouseHitTime > COOLDOWN_SECONDS)) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                if (mc.level.getBlockState(pos).is(Blocks.NOTE_BLOCK)) {
                    playDrumSound(mc, pos, 0.5, null);
                    spawnDrumParticles(mc, hit.getLocation());
                    lastMouseHitTime = currentTime;
                }
            }
        }
    }

    private void playDrumSound(Minecraft mc, BlockPos pos, double speed, HandType handType) {
        float volume = (float) Math.min(Math.max(speed * 10.0, 0.5), 1.5);
        float pitch = 0.45f + (float)(Math.random() * 0.2);

        mc.level.playLocalSound(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            SoundEvents.NOTE_BLOCK_BASS.value(),
            SoundSource.BLOCKS,
            volume,
            pitch,
            false
        );

        if (handType != null) {
            float amplitude = (float) Math.min(speed * 12.0, 1.0);
            VisorAPI.client().getInputManager().triggerHapticPulse(handType, 160f, amplitude, 0.06f);
        }
    }

    private void spawnDrumParticles(Minecraft mc, Vec3 pos) {
        for (int i = 0; i < 8; i++) {
            mc.level.addParticle(
                ParticleTypes.NOTE,
                pos.x + (Math.random() - 0.5) * 0.2,
                pos.y + (Math.random() - 0.5) * 0.2,
                pos.z + (Math.random() - 0.5) * 0.2,
                0, 0.1, 0
            );
        }
    }

    private Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
