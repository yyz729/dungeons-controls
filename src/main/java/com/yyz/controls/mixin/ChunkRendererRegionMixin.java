package com.yyz.controls.mixin;

import com.yyz.controls.occlusion.OcclusionTracker;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderChunkRegion.class)
public abstract class ChunkRendererRegionMixin {
	@Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
	private void dungeons$hideOccluder(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		if (OcclusionTracker.shouldHide(pos)) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}
}
