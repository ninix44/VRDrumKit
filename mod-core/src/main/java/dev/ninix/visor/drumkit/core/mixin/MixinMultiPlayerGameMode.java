package dev.ninix.visor.drumkit.core.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(Direction direction, CallbackInfo ci) { // todo
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (isStickOnNoteBlock(mc, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfo ci) { // todo
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (isStickOnNoteBlock(mc, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (isStickOnNoteBlock(mc, pos)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    private boolean isStickOnNoteBlock(Minecraft mc, BlockPos pos) {
        if (mc.level == null) return false;

        boolean isNoteBlock = mc.level.getBlockState(pos).getBlock() == Blocks.NOTE_BLOCK;
        if (!isNoteBlock) return false;

        boolean hasStickInMainHand = mc.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.STICK);
        boolean hasStickInOffHand = mc.player.getItemInHand(InteractionHand.OFF_HAND).is(Items.STICK);

        return hasStickInMainHand || hasStickInOffHand;
    }
}
