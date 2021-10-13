package shadows.click.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPlayerDiggingPacket.Action;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import shadows.placebo.util.NetHandlerSpaghettiServer;

@EventBusSubscriber
public class FakePlayerUtil {

	private static final Map<World, Map<GameProfile, UsefulFakePlayer>> PLAYERS = new WeakHashMap<>();

	public static class UsefulFakePlayer extends FakePlayer {

		public UsefulFakePlayer(World world, GameProfile name) {
			super((ServerWorld) world, name);
		}

		@Override
		public float getEyeHeight(Pose pose) {
			return 0; //Allows for the position of the player to be the exact source when raytracing.
		}

		@Override
		public void refreshContainer(Container containerToSend, NonNullList<ItemStack> itemsList) {
			//Prevent crashing when objects with containers are clicked on.
		}

		@Override
		public float getAttackStrengthScale(float adjustTicks) {
			return 1; //Prevent the attack strength from always being 0.03 due to not ticking.
		}

		@Override
		public Entity changeDimension(ServerWorld server, ITeleporter teleporter) {
			return getPlayer(server, this.getGameProfile());
		}
	}

	/**
	 * Only store this as a WeakReference, or you'll cause memory leaks.
	 */
	public static UsefulFakePlayer getPlayer(World world, GameProfile profile) {
		return PLAYERS.computeIfAbsent(world, p -> new HashMap<>()).computeIfAbsent(profile, p -> {
			UsefulFakePlayer player = new UsefulFakePlayer(world, profile);
			player.connection = new NetHandlerSpaghettiServer(player);
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
		player.inventory.items.set(player.inventory.selected, toHold);
		float pitch = direction == Direction.UP ? -90 : direction == Direction.DOWN ? 90 : 0;
		float yaw = direction == Direction.SOUTH ? 0 : direction == Direction.WEST ? 90 : direction == Direction.NORTH ? 180 : -90;
		Vector3i sideVec = direction.getNormal();
		Axis a = direction.getAxis();
		AxisDirection ad = direction.getAxisDirection();
		double x = a == Axis.X && ad == AxisDirection.NEGATIVE ? -.5 : .5 + sideVec.getX() / 1.9D;
		double y = 0.5 + sideVec.getY() / 1.9D;
		double z = a == Axis.Z && ad == AxisDirection.NEGATIVE ? -.5 : .5 + sideVec.getZ() / 1.9D;
		player.moveTo(pos.getX() + x, pos.getY() + y, pos.getZ() + z, yaw, pitch);
		if (!toHold.isEmpty()) player.getAttributes().addTransientAttributeModifiers(toHold.getAttributeModifiers(EquipmentSlotType.MAINHAND));
		player.setShiftKeyDown(sneaking);
	}

	/**
	 * Cleans up the fake player after use.
	 * @param player The player.
	 * @param resultStack The stack that was returned from right/leftClickInDirection.
	 * @param oldStack The previous stack, from before use.
	 */
	public static void cleanupFakePlayerFromUse(UsefulFakePlayer player, ItemStack resultStack, ItemStack oldStack, Consumer<ItemStack> stackCallback) {
		if (!oldStack.isEmpty()) player.getAttributes().removeAttributeModifiers(oldStack.getAttributeModifiers(EquipmentSlotType.MAINHAND));
		player.inventory.items.set(player.inventory.selected, ItemStack.EMPTY);
		stackCallback.accept(resultStack);
		if (!player.inventory.isEmpty()) player.inventory.dropAll();
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
	public static ItemStack rightClickInDirection(UsefulFakePlayer player, World world, BlockPos pos, Direction side, BlockState sourceState) {
		Vector3d base = new Vector3d(player.getX(), player.getY(), player.getZ());
		Vector3d look = player.getLookAngle();
		Vector3d target = base.add(look.x * 5, look.y * 5, look.z * 5);
		RayTraceResult trace = world.clip(new RayTraceContext(base, target, BlockMode.OUTLINE, FluidMode.NONE, player));
		RayTraceResult traceEntity = traceEntities(player, base, target, world);
		RayTraceResult toUse = trace == null ? traceEntity : trace;

		if (trace != null && traceEntity != null) {
			double d1 = trace.getLocation().distanceTo(base);
			double d2 = traceEntity.getLocation().distanceTo(base);
			toUse = traceEntity.getType() == RayTraceResult.Type.ENTITY && d1 > d2 ? traceEntity : trace;
		}

		if (toUse == null) return player.getMainHandItem();

		ItemStack itemstack = player.getMainHandItem();
		if (toUse.getType() == RayTraceResult.Type.ENTITY) {
			if (processUseEntity(player, world, ((EntityRayTraceResult) toUse).getEntity(), toUse, CUseEntityPacket.Action.INTERACT_AT)) return player.getMainHandItem();
			else if (processUseEntity(player, world, ((EntityRayTraceResult) toUse).getEntity(), null, CUseEntityPacket.Action.INTERACT)) return player.getMainHandItem();
		} else if (toUse.getType() == RayTraceResult.Type.BLOCK) {
			BlockPos blockpos = ((BlockRayTraceResult) toUse).getBlockPos();
			BlockState state = world.getBlockState(blockpos);
			if (state != sourceState && state.getMaterial() != Material.AIR) {
				ActionResultType type = player.gameMode.useItemOn(player, world, itemstack, Hand.MAIN_HAND, (BlockRayTraceResult) toUse);
				if (type == ActionResultType.SUCCESS) return player.getMainHandItem();
			}
		}

		if (toUse == null || toUse.getType() == RayTraceResult.Type.MISS) {
			for (int i = 1; i <= 5; i++) {
				BlockState state = world.getBlockState(pos.relative(side, i));
				if (state != sourceState && state.getMaterial() != Material.AIR) {
					player.gameMode.useItemOn(player, world, itemstack, Hand.MAIN_HAND, (BlockRayTraceResult) toUse);
					return player.getMainHandItem();
				}
			}
		}

		if (itemstack.isEmpty() && (toUse == null || toUse.getType() == RayTraceResult.Type.MISS)) ForgeHooks.onEmptyClick(player, Hand.MAIN_HAND);
		if (!itemstack.isEmpty()) player.gameMode.useItem(player, world, itemstack, Hand.MAIN_HAND);
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
	public static ItemStack leftClickInDirection(UsefulFakePlayer player, World world, BlockPos pos, Direction side, BlockState sourceState) {
		Vector3d base = new Vector3d(player.getX(), player.getY(), player.getZ());
		Vector3d look = player.getLookAngle();
		Vector3d target = base.add(look.x * 5, look.y * 5, look.z * 5);
		RayTraceResult trace = world.clip(new RayTraceContext(base, target, BlockMode.OUTLINE, FluidMode.NONE, player));
		RayTraceResult traceEntity = traceEntities(player, base, target, world);
		RayTraceResult toUse = trace == null ? traceEntity : trace;

		if (trace != null && traceEntity != null) {
			double d1 = trace.getLocation().distanceTo(base);
			double d2 = traceEntity.getLocation().distanceTo(base);
			toUse = traceEntity.getType() == RayTraceResult.Type.ENTITY && d1 > d2 ? traceEntity : trace;
		}

		if (toUse == null) return player.getMainHandItem();

		if (toUse.getType() == RayTraceResult.Type.ENTITY) {
			if (processUseEntity(player, world, ((EntityRayTraceResult) toUse).getEntity(), null, CUseEntityPacket.Action.ATTACK)) return player.getMainHandItem();
		} else if (toUse.getType() == RayTraceResult.Type.BLOCK) {
			BlockPos blockpos = ((BlockRayTraceResult) toUse).getBlockPos();
			BlockState state = world.getBlockState(blockpos);
			if (state != sourceState && state.getMaterial() != Material.AIR) {
				player.gameMode.handleBlockBreakAction(blockpos, Action.START_DESTROY_BLOCK, ((BlockRayTraceResult) toUse).getDirection(), player.server.getMaxBuildHeight());
				return player.getMainHandItem();
			}
		}

		if (toUse == null || toUse.getType() == RayTraceResult.Type.MISS) {
			for (int i = 1; i <= 5; i++) {
				BlockState state = world.getBlockState(pos.relative(side, i));
				if (state != sourceState && state.getMaterial() != Material.AIR) {
					player.gameMode.handleBlockBreakAction(pos.relative(side, i), Action.START_DESTROY_BLOCK, side.getOpposite(), player.server.getMaxBuildHeight());
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
	public static RayTraceResult traceEntities(UsefulFakePlayer player, Vector3d base, Vector3d target, World world) {
		Entity pointedEntity = null;
		RayTraceResult result = null;
		Vector3d vec3d3 = null;
		AxisAlignedBB search = new AxisAlignedBB(base.x, base.y, base.z, target.x, target.y, target.z).inflate(.5, .5, .5);
		List<Entity> list = world.getEntities(player, search, entity -> EntityPredicates.NO_SPECTATORS.test(entity) && entity != null && entity.isPickable());
		double d2 = 5;

		for (int j = 0; j < list.size(); ++j) {
			Entity entity1 = list.get(j);

			AxisAlignedBB aabb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
			Optional<Vector3d> optVec = aabb.clip(base, target);

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
			result = BlockRayTraceResult.miss(vec3d3, null, new BlockPos(vec3d3));
		}

		if (pointedEntity != null) {
			result = new EntityRayTraceResult(pointedEntity, vec3d3);
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
	public static boolean processUseEntity(UsefulFakePlayer player, World world, Entity entity, @Nullable RayTraceResult result, CUseEntityPacket.Action action) {
		if (entity != null) {
			boolean flag = player.canSee(entity);
			double d0 = 36.0D;

			if (!flag) d0 = 9.0D;

			if (player.distanceToSqr(entity) < d0) {
				if (action == CUseEntityPacket.Action.INTERACT) {
					return player.interactOn(entity, Hand.MAIN_HAND) == ActionResultType.SUCCESS;
				} else if (action == CUseEntityPacket.Action.INTERACT_AT) {
					if (ForgeHooks.onInteractEntityAt(player, entity, result.getLocation(), Hand.MAIN_HAND) != null) return false;
					return entity.interactAt(player, result.getLocation(), Hand.MAIN_HAND) == ActionResultType.SUCCESS;
				} else if (action == CUseEntityPacket.Action.ATTACK) {
					if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof ArrowEntity || entity == player) return false;
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
	public static RayTraceResult rayTrace(UsefulFakePlayer player, World world, double reachDist, float partialTicks) {
		Vector3d vec3d = player.getEyePosition(partialTicks);
		Vector3d vec3d1 = player.getViewVector(partialTicks);
		Vector3d vec3d2 = vec3d.add(vec3d1.x * reachDist, vec3d1.y * reachDist, vec3d1.z * reachDist);
		return world.clip(new RayTraceContext(vec3d, vec3d2, BlockMode.OUTLINE, FluidMode.NONE, player));
	}

}
