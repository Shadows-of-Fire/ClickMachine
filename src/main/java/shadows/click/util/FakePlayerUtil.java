package shadows.click.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import shadows.placebo.util.DeadPacketListenerImpl;

@EventBusSubscriber
public class FakePlayerUtil {

	private static final Map<Level, Map<GameProfile, UsefulFakePlayer>> PLAYERS = new WeakHashMap<>();

	public static class UsefulFakePlayer extends FakePlayer {

		public UsefulFakePlayer(Level world, GameProfile name) {
			super((ServerLevel) world, name);
		}

		@Override
		public float getEyeHeight(Pose pose) {
			return 0; //Allows for the position of the player to be the exact source when raytracing.
		}

		@Override
		public void initMenu(AbstractContainerMenu p_143400_) {
		}

		@Override
		public OptionalInt openMenu(MenuProvider p_9033_) {
			return OptionalInt.empty();
		}

		@Override
		public float getAttackStrengthScale(float adjustTicks) {
			return 1; //Prevent the attack strength from always being 0.03 due to not ticking.
		}

		@Override
		public Entity changeDimension(ServerLevel server, ITeleporter teleporter) {
			return getPlayer(server, this.getGameProfile());
		}
	}

	/**
	 * Only store this as a WeakReference, or you'll cause memory leaks.
	 */
	public static UsefulFakePlayer getPlayer(Level world, GameProfile profile) {
		return PLAYERS.computeIfAbsent(world, p -> new HashMap<>()).computeIfAbsent(profile, p -> {
			UsefulFakePlayer player = new UsefulFakePlayer(world, profile);
			player.connection = new DeadPacketListenerImpl(player);
			return player;
		});
	}

	/**
	 * Sets up for a fake player to be usable to right click things.  This player will be put at the center of the using side.
	 * @param player The player.
	 * @param pos The position of the using tile entity.
	 * @param direction The direction to use in.
	 * @param toHold The stack the player will be using.  Should probably come from an ItemStackHandler or similar.
	 */
	public static void setupFakePlayerForUse(UsefulFakePlayer player, BlockPos pos, Direction direction, ItemStack toHold, boolean sneaking) {
		player.getInventory().items.set(player.getInventory().selected, toHold);
		float pitch = direction == Direction.UP ? -90 : direction == Direction.DOWN ? 90 : 0;
		float yaw = direction == Direction.SOUTH ? 0 : direction == Direction.WEST ? 90 : direction == Direction.NORTH ? 180 : -90;
		Vec3i sideVec = direction.getNormal();
		Axis a = direction.getAxis();
		AxisDirection ad = direction.getAxisDirection();
		double x = a == Axis.X && ad == AxisDirection.NEGATIVE ? -.5 : .5 + sideVec.getX() / 1.9D;
		double y = 0.5 + sideVec.getY() / 1.9D;
		double z = a == Axis.Z && ad == AxisDirection.NEGATIVE ? -.5 : .5 + sideVec.getZ() / 1.9D;
		player.moveTo(pos.getX() + x, pos.getY() + y, pos.getZ() + z, yaw, pitch);
		if (!toHold.isEmpty()) player.getAttributes().addTransientAttributeModifiers(toHold.getAttributeModifiers(EquipmentSlot.MAINHAND));
		player.setShiftKeyDown(sneaking);
	}

	/**
	 * Cleans up the fake player after use.
	 * @param player The player.
	 * @param resultStack The stack that was returned from right/leftClickInDirection.
	 * @param oldStack The previous stack, from before use.
	 */
	public static void cleanupFakePlayerFromUse(UsefulFakePlayer player, ItemStack resultStack, ItemStack oldStack, Consumer<ItemStack> stackCallback) {
		if (!oldStack.isEmpty()) player.getAttributes().removeAttributeModifiers(oldStack.getAttributeModifiers(EquipmentSlot.MAINHAND));
		player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
		stackCallback.accept(resultStack);
		if (!player.getInventory().isEmpty()) player.getInventory().dropAll();
		player.setShiftKeyDown(false);
	}

	/**
	 * Uses whatever the player happens to be holding in the given direction.
	 * @param player The player.
	 * @param world The world of the calling tile entity.  It may be a bad idea to use {@link FakePlayer#getEntityWorld()}.
	 * @param pos The pos of the calling tile entity.
	 * @param side The direction to use in.
	 * @param sourceState The state of the calling tile entity, so we don't click ourselves.
	 * @return The remainder of whatever the player was holding.  This should be set back into the tile's stack handler or similar.
	 */
	public static ItemStack rightClickInDirection(UsefulFakePlayer player, Level world, BlockPos pos, Direction side, BlockState sourceState) {
		Vec3 base = new Vec3(player.getX(), player.getY(), player.getZ());
		Vec3 look = player.getLookAngle();
		Vec3 target = base.add(look.x * 5, look.y * 5, look.z * 5);
		HitResult trace = world.clip(new ClipContext(base, target, Block.OUTLINE, Fluid.NONE, player));
		HitResult traceEntity = traceEntities(player, base, target, world);
		HitResult toUse = trace == null ? traceEntity : trace;

		if (trace != null && traceEntity != null) {
			double d1 = trace.getLocation().distanceTo(base);
			double d2 = traceEntity.getLocation().distanceTo(base);
			toUse = traceEntity.getType() == HitResult.Type.ENTITY && d1 > d2 ? traceEntity : trace;
		}

		if (toUse == null) return player.getMainHandItem();

		ItemStack itemstack = player.getMainHandItem();
		if (toUse.getType() == HitResult.Type.ENTITY) {
			if (processUseEntity(player, world, ((EntityHitResult) toUse).getEntity(), toUse, InteractionType.INTERACT_AT)) return player.getMainHandItem();
			else if (processUseEntity(player, world, ((EntityHitResult) toUse).getEntity(), null, InteractionType.INTERACT)) return player.getMainHandItem();
		} else if (toUse.getType() == HitResult.Type.BLOCK) {
			BlockPos blockpos = ((BlockHitResult) toUse).getBlockPos();
			BlockState state = world.getBlockState(blockpos);
			if (state != sourceState && state.getMaterial() != Material.AIR) {
				InteractionResult type = player.gameMode.useItemOn(player, world, itemstack, InteractionHand.MAIN_HAND, (BlockHitResult) toUse);
				if (type == InteractionResult.SUCCESS) return player.getMainHandItem();
			}
		}

		if (toUse == null || toUse.getType() == HitResult.Type.MISS) {
			for (int i = 1; i <= 5; i++) {
				BlockState state = world.getBlockState(pos.relative(side, i));
				if (state != sourceState && state.getMaterial() != Material.AIR) {
					player.gameMode.useItemOn(player, world, itemstack, InteractionHand.MAIN_HAND, (BlockHitResult) toUse);
					return player.getMainHandItem();
				}
			}
		}

		if (itemstack.isEmpty() && (toUse == null || toUse.getType() == HitResult.Type.MISS)) ForgeHooks.onEmptyClick(player, InteractionHand.MAIN_HAND);
		if (!itemstack.isEmpty()) player.gameMode.useItem(player, world, itemstack, InteractionHand.MAIN_HAND);
		return player.getMainHandItem();
	}

	/**
	 * Attacks with whatever the player happens to be holding in the given direction.
	 * @param player The player.
	 * @param world The world of the calling tile entity.  It may be a bad idea to use {@link FakePlayer#getEntityWorld()}.
	 * @param pos The pos of the calling tile entity.
	 * @param side The direction to attack in.
	 * @param sourceState The state of the calling tile entity, so we don't click ourselves.
	 * @return The remainder of whatever the player was holding.  This should be set back into the tile's stack handler or similar.
	 */
	public static ItemStack leftClickInDirection(UsefulFakePlayer player, Level world, BlockPos pos, Direction side, BlockState sourceState) {
		Vec3 base = new Vec3(player.getX(), player.getY(), player.getZ());
		Vec3 look = player.getLookAngle();
		Vec3 target = base.add(look.x * 5, look.y * 5, look.z * 5);
		HitResult trace = world.clip(new ClipContext(base, target, Block.OUTLINE, Fluid.NONE, player));
		HitResult traceEntity = traceEntities(player, base, target, world);
		HitResult toUse = trace == null ? traceEntity : trace;

		if (trace != null && traceEntity != null) {
			double d1 = trace.getLocation().distanceTo(base);
			double d2 = traceEntity.getLocation().distanceTo(base);
			toUse = traceEntity.getType() == HitResult.Type.ENTITY && d1 > d2 ? traceEntity : trace;
		}

		if (toUse == null) return player.getMainHandItem();

		if (toUse.getType() == HitResult.Type.ENTITY) {
			if (processUseEntity(player, world, ((EntityHitResult) toUse).getEntity(), null, InteractionType.ATTACK)) return player.getMainHandItem();
		} else if (toUse.getType() == HitResult.Type.BLOCK) {
			BlockPos blockpos = ((BlockHitResult) toUse).getBlockPos();
			BlockState state = world.getBlockState(blockpos);
			if (state != sourceState && state.getMaterial() != Material.AIR) {
				player.gameMode.handleBlockBreakAction(blockpos, Action.START_DESTROY_BLOCK, ((BlockHitResult) toUse).getDirection(), player.level.getMaxBuildHeight());
				return player.getMainHandItem();
			}
		}

		if (toUse == null || toUse.getType() == HitResult.Type.MISS) {
			for (int i = 1; i <= 5; i++) {
				BlockState state = world.getBlockState(pos.relative(side, i));
				if (state != sourceState && state.getMaterial() != Material.AIR) {
					player.gameMode.handleBlockBreakAction(pos.relative(side, i), Action.START_DESTROY_BLOCK, side.getOpposite(), player.level.getMaxBuildHeight());
					return player.getMainHandItem();
				}
			}
		}

		return player.getMainHandItem();
	}

	/**
	 * Traces for an entity.
	 * @param player The player.
	 * @param world The world of the calling tile entity.
	 * @return A ray trace result that will likely be of type entity, but may be type block, or null.
	 */
	public static HitResult traceEntities(UsefulFakePlayer player, Vec3 base, Vec3 target, Level world) {
		Entity pointedEntity = null;
		HitResult result = null;
		Vec3 vec3d3 = null;
		AABB search = new AABB(base.x, base.y, base.z, target.x, target.y, target.z).inflate(.5, .5, .5);
		List<Entity> list = world.getEntities(player, search, entity -> EntitySelector.NO_SPECTATORS.test(entity) && entity != null && entity.isPickable());
		double d2 = 5;

		for (int j = 0; j < list.size(); ++j) {
			Entity entity1 = list.get(j);

			AABB aabb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
			Optional<Vec3> optVec = aabb.clip(base, target);

			if (aabb.contains(base)) {
				if (d2 >= 0.0D) {
					pointedEntity = entity1;
					vec3d3 = optVec.orElse(base);
					d2 = 0.0D;
				}
			} else if (optVec.isPresent()) {
				double d3 = base.distanceTo(optVec.get());

				if (d3 < d2 || d2 == 0.0D) {
					if (entity1.getRootVehicle() == player.getRootVehicle() && !entity1.canRiderInteract()) {
						if (d2 == 0.0D) {
							pointedEntity = entity1;
							vec3d3 = optVec.get();
						}
					} else {
						pointedEntity = entity1;
						vec3d3 = optVec.get();
						d2 = d3;
					}
				}
			}
		}

		if (pointedEntity != null && base.distanceTo(vec3d3) > 5) {
			pointedEntity = null;
			result = BlockHitResult.miss(vec3d3, null, new BlockPos(vec3d3));
		}

		if (pointedEntity != null) {
			result = new EntityHitResult(pointedEntity, vec3d3);
		}

		return result;
	}

	/**
	 * Processes the using of an entity from the server side.
	 * @param player The player.
	 * @param world The world of the calling tile entity.
	 * @param entity The entity to interact with.
	 * @param result The actual ray trace result, only necessary if using {@link CUseEntityPacket.Action#INTERACT_AT}
	 * @param action The type of interaction to perform.
	 * @return If the entity was used.
	 */
	public static boolean processUseEntity(UsefulFakePlayer player, Level world, Entity entity, @Nullable HitResult result, InteractionType action) {
		if (entity != null) {

			if (player.distanceToSqr(entity) < 36) {
				if (action == InteractionType.INTERACT) {
					return player.interactOn(entity, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS;
				} else if (action == InteractionType.INTERACT_AT) {
					if (ForgeHooks.onInteractEntityAt(player, entity, result.getLocation(), InteractionHand.MAIN_HAND) != null) return false;
					return entity.interactAt(player, result.getLocation(), InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS;
				} else if (action == InteractionType.ATTACK) {
					if (entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof Arrow || entity == player) return false;
					player.attack(entity);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * A copy-paste of the SideOnly {@link Entity#rayTrace(double, float)}
	 */
	public static HitResult rayTrace(UsefulFakePlayer player, Level world, double reachDist, float partialTicks) {
		Vec3 vec3d = player.getEyePosition(partialTicks);
		Vec3 vec3d1 = player.getViewVector(partialTicks);
		Vec3 vec3d2 = vec3d.add(vec3d1.x * reachDist, vec3d1.y * reachDist, vec3d1.z * reachDist);
		return world.clip(new ClipContext(vec3d, vec3d2, Block.OUTLINE, Fluid.NONE, player));
	}

	public static enum InteractionType {
		INTERACT,
		INTERACT_AT,
		ATTACK;
	}

}
