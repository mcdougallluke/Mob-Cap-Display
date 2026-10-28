package dev.lukemcd.mobcapdisplay.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.lukemcd.mobcapdisplay.tally.MobCapTracker;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
	@WrapMethod(method = "createState")
	private static NaturalSpawner.SpawnState mobcapdisplay$sample(
		int spawnableChunkCount,
		ServerLevel level,
		NaturalSpawner.ChunkGetter chunkGetter,
		LocalMobCapCalculator localMobCapCalculator,
		Operation<NaturalSpawner.SpawnState> original
	) {
		return MobCapTracker.sample(level, () -> original.call(spawnableChunkCount, level, chunkGetter, localMobCapCalculator));
	}

	// the tally runs exactly when vanilla MC counts the entity, after its persistence and MISC filters, and only if the chunk getter accepts the entity's chunk
	@WrapOperation(
		method = "createState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;query(JLjava/util/function/Consumer;)V")
	)
	private static void mobcapdisplay$tally(
		NaturalSpawner.ChunkGetter chunkGetter,
		long chunkKey,
		Consumer<LevelChunk> countEntity,
		Operation<Void> original,
		@Local Entity entity,
		@Local MobCategory category
	) {
		original.call(chunkGetter, chunkKey, MobCapTracker.wrap(countEntity, entity, category));
	}
}
