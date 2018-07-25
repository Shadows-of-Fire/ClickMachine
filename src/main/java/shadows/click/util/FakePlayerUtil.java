package shadows.click.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicates;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
public class FakePlayerUtil {

	private static final Map<Pair<GameProfile, Integer>, UsefulFakePlayer> PLAYERS = new HashMap<>();

	public static class UsefulFakePlayer extends FakePlayer {

		public UsefulFakePlayer(World world, GameProfile name) {
			super((WorldServer) world, name);
		}

		@Override
		public float getEyeHeight() {
			return 0; //Allows for the position of the player to be the exact source when raytracing.
		}

		@Override
		public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
			//Prevent crashing when objects with containers are clicked on.
		}

		@Override
		public float getCooledAttackStrength(float adjustTicks) {
			return 1; //Prevent the attack strength from always being 0.03 due to not ticking.
		}
	}

	/**
	 * Only store this as a WeakReference, or you'll cause memory leaks.
	 */
	public static UsefulFakePlayer getPlayer(World world, GameProfile profile) {
		return PLAYERS.computeIfAbsent(Pair.of(profile, world.provider.getDimension()), p -> {
			UsefulFakePlayer player = new UsefulFakePlayer(world, p.getLeft());
			player.connection = new NetHandlerSpaghettiServer(player);
			return player;
		});
	}

	/**
	 * Fake players must be unloaded with the world to prevent memory leaks.
	 */
	@SubscribeEvent
	public static void unload(WorldEvent.Unload e) {
		PLAYERS.entrySet().removeIf(entry -> entry.getValue().world == e.getWorld());
	}

	/**
	 * Sets up for a fake player to be usable to right click things.  This player will be put at the center of the using side.
	 * @param player The player.
	 * @param pos The position of the using tile entity.
	 * @param direction The direction to use in.
	 * @param toHold The stack the player will be using.  Should probably come from an ItemStackHandler or similar.
	 */
	public static void setupFakePlayerForUse(UsefulFakePlayer player, BlockPos pos, EnumFacing direction, ItemStack toHold, boolean sneaking) {
		player.inventory.mainInventory.set(player.inventory.currentItem, toHold);
		float pitch = direction == EnumFacing.UP ? -90 : direction == EnumFacing.DOWN ? 90 : 0;
		float yaw = direction == EnumFacing.SOUTH ? 0 : direction == EnumFacing.WEST ? 90 : direction == EnumFacing.NORTH ? 180 : -90;
		Vec3i sideVec = direction.getDirectionVec();
		Axis a = direction.getAxis();
		AxisDirection ad = direction.getAxisDirection();
		double x = a == Axis.X && ad == AxisDirection.NEGATIVE ? -.5 : .5 + sideVec.getX() / 1.9D;
		double y = 0.5 + sideVec.getY() / 1.9D;
		double z = a == Axis.Z && ad == AxisDirection.NEGATIVE ? -.5 : .5 + sideVec.getZ() / 1.9D;
		player.setLocationAndAngles(pos.getX() + x, pos.getY() + y, pos.getZ() + z, yaw, pitch);
		if (!toHold.isEmpty()) player.getAttributeMap().applyAttributeModifiers(toHold.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		player.setSneaking(sneaking);
	}

	/**
	 * Cleans up the fake player after use.
	 * @param player The player.
	 * @param resultStack The stack that was returned from right/leftClickInDirection.
	 * @param oldStack The previous stack, from before use.
	 */
	public static void cleanupFakePlayerFromUse(UsefulFakePlayer player, ItemStack resultStack, ItemStack oldStack, Consumer<ItemStack> stackCallback) {
		if (!oldStack.isEmpty()) player.getAttributeMap().removeAttributeModifiers(oldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		player.inventory.mainInventory.set(player.inventory.currentItem, ItemStack.EMPTY);
		stackCallback.accept(resultStack);
		if (!player.inventory.isEmpty()) player.inventory.dropAllItems();
		player.setSneaking(false);
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
	public static ItemStack rightClickInDirection(UsefulFakePlayer player, World world, BlockPos pos, EnumFacing side, IBlockState sourceState) {
		Vec3d base = new Vec3d(player.posX, player.posY, player.posZ);
		Vec3d look = player.getLookVec();
		Vec3d target = base.add(look.x * 5, look.y * 5, look.z * 5);
		RayTraceResult trace = world.rayTraceBlocks(base, target, false, false, true);
		RayTraceResult traceEntity = traceEntities(player, base, target, world);
		RayTraceResult toUse = trace == null ? traceEntity : trace;

		if (trace != null && traceEntity != null) {
			double d1 = trace.hitVec.distanceTo(base);
			double d2 = traceEntity.hitVec.distanceTo(base);
			toUse = traceEntity.typeOfHit == RayTraceResult.Type.ENTITY && d1 > d2 ? traceEntity : trace;
		}

		if (toUse == null) return player.getHeldItemMainhand();

		ItemStack itemstack = player.getHeldItemMainhand();
		if (toUse.typeOfHit == RayTraceResult.Type.ENTITY) {
			if (processUseEntity(player, world, toUse.entityHit, toUse, CPacketUseEntity.Action.INTERACT_AT)) return player.getHeldItemMainhand();
			else if (processUseEntity(player, world, toUse.entityHit, null, CPacketUseEntity.Action.INTERACT)) return player.getHeldItemMainhand();
		} else if (toUse.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos blockpos = toUse.getBlockPos();
			IBlockState state = world.getBlockState(blockpos);
			if (state != sourceState && state.getMaterial() != Material.AIR) {
				float f = (float) (toUse.hitVec.x - pos.getX());
				float f1 = (float) (toUse.hitVec.y - pos.getY());
				float f2 = (float) (toUse.hitVec.z - pos.getZ());
				EnumActionResult enumactionresult = player.interactionManager.processRightClickBlock(player, world, itemstack, EnumHand.MAIN_HAND, blockpos, toUse.sideHit, f, f1, f2);
				if (enumactionresult == EnumActionResult.SUCCESS) return player.getHeldItemMainhand();
			}
		}
		
		if(toUse == null || toUse.typeOfHit == RayTraceResult.Type.MISS) {
			for(int i = 1; i <= 5; i++) {
				IBlockState state = world.getBlockState(pos.offset(side, i));
				if (state != sourceState && state.getMaterial() != Material.AIR) {
					player.interactionManager.processRightClickBlock(player, world, itemstack, EnumHand.MAIN_HAND, pos.offset(side, i), toUse.sideHit, 0, 0, 0);
					return player.getHeldItemMainhand();
				}
			}
		}
		
		if (itemstack.isEmpty() && (toUse == null || toUse.typeOfHit == RayTraceResult.Type.MISS)) ForgeHooks.onEmptyClick(player, EnumHand.MAIN_HAND);
		if (!itemstack.isEmpty()) player.interactionManager.processRightClick(player, world, itemstack, EnumHand.MAIN_HAND);
		return player.getHeldItemMainhand();
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
	public static ItemStack leftClickInDirection(UsefulFakePlayer player, World world, BlockPos pos, EnumFacing side, IBlockState sourceState) {
		Vec3d base = new Vec3d(player.posX, player.posY, player.posZ);
		Vec3d look = player.getLookVec();
		Vec3d target = base.add(look.x * 5, look.y * 5, look.z * 5);
		RayTraceResult trace = world.rayTraceBlocks(base, target, false, false, true);
		RayTraceResult traceEntity = traceEntities(player, base, target, world);
		RayTraceResult toUse = trace == null ? traceEntity : trace;

		if (trace != null && traceEntity != null) {
			double d1 = trace.hitVec.distanceTo(base);
			double d2 = traceEntity.hitVec.distanceTo(base);
			toUse = traceEntity.typeOfHit == RayTraceResult.Type.ENTITY && d1 > d2 ? traceEntity : trace;
		}

		if (toUse == null) return player.getHeldItemMainhand();

		ItemStack itemstack = player.getHeldItemMainhand();
		if (toUse.typeOfHit == RayTraceResult.Type.ENTITY) {
			if (processUseEntity(player, world, toUse.entityHit, null, CPacketUseEntity.Action.ATTACK)) return player.getHeldItemMainhand();
		} else if (toUse.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos blockpos = toUse.getBlockPos();
			IBlockState state = world.getBlockState(blockpos);
			if (state != sourceState && state.getMaterial() != Material.AIR) {
				player.interactionManager.onBlockClicked(blockpos, toUse.sideHit);
				return player.getHeldItemMainhand();
			}
		}

		if(toUse == null || toUse.typeOfHit == RayTraceResult.Type.MISS) {
			for(int i = 1; i <= 5; i++) {
				IBlockState state = world.getBlockState(pos.offset(side, i));
				if (state != sourceState && state.getMaterial() != Material.AIR) {
					player.interactionManager.onBlockClicked(pos.offset(side, i), side.getOpposite());
					return player.getHeldItemMainhand();
				}
			}
		}
		
		if (itemstack.isEmpty() && (toUse == null || toUse.typeOfHit == RayTraceResult.Type.MISS)) ForgeHooks.onEmptyLeftClick(player);
		return player.getHeldItemMainhand();
	}

	/**
	 * Traces for an entity.
	 * @param player The player.
	 * @param world The world of the calling tile entity.
	 * @return A ray trace result that will likely be of type entity, but may be type block, or null.
	 */
	public static RayTraceResult traceEntities(UsefulFakePlayer player, Vec3d base, Vec3d target, World world) {
		Entity pointedEntity = null;
		RayTraceResult result = null;
		Vec3d vec3d3 = null;
		AxisAlignedBB search = new AxisAlignedBB(base.x, base.y, base.z, target.x, target.y, target.z).grow(.5, .5, .5);
		List<Entity> list = world.getEntitiesInAABBexcluding(player, search, Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith()));
		double d2 = 5;

		for (int j = 0; j < list.size(); ++j) {
			Entity entity1 = list.get(j);

			AxisAlignedBB aabb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
			RayTraceResult raytraceresult = aabb.calculateIntercept(base, target);

			if (aabb.contains(base)) {
				if (d2 >= 0.0D) {
					pointedEntity = entity1;
					vec3d3 = raytraceresult == null ? base : raytraceresult.hitVec;
					d2 = 0.0D;
				}
			} else if (raytraceresult != null) {
				double d3 = base.distanceTo(raytraceresult.hitVec);

				if (d3 < d2 || d2 == 0.0D) {
					if (entity1.getLowestRidingEntity() == player.getLowestRidingEntity() && !entity1.canRiderInteract()) {
						if (d2 == 0.0D) {
							pointedEntity = entity1;
							vec3d3 = raytraceresult.hitVec;
						}
					} else {
						pointedEntity = entity1;
						vec3d3 = raytraceresult.hitVec;
						d2 = d3;
					}
				}
			}
		}

		if (pointedEntity != null && base.distanceTo(vec3d3) > 5) {
			pointedEntity = null;
			result = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, (EnumFacing) null, new BlockPos(vec3d3));
		}

		if (pointedEntity != null) {
			result = new RayTraceResult(pointedEntity, vec3d3);
		}

		return result;
	}

	/**
	 * Processes the using of an entity from the server side.
	 * @param player The player.
	 * @param world The world of the calling tile entity.
	 * @param entity The entity to interact with.
	 * @param result The actual ray trace result, only necessary if using {@link CPacketUseEntity.Action#INTERACT_AT}
	 * @param action The type of interaction to perform.
	 * @return If the entity was used.
	 */
	public static boolean processUseEntity(UsefulFakePlayer player, World world, Entity entity, @Nullable RayTraceResult result, CPacketUseEntity.Action action) {
		if (entity != null) {
			boolean flag = player.canEntityBeSeen(entity);
			double d0 = 36.0D;

			if (!flag) d0 = 9.0D;

			if (player.getDistanceSq(entity) < d0) {
				if (action == CPacketUseEntity.Action.INTERACT) {
					return player.interactOn(entity, EnumHand.MAIN_HAND) == EnumActionResult.SUCCESS;
				} else if (action == CPacketUseEntity.Action.INTERACT_AT) {
					if (ForgeHooks.onInteractEntityAt(player, entity, result.hitVec, EnumHand.MAIN_HAND) != null) return false;
					return entity.applyPlayerInteraction(player, result.hitVec, EnumHand.MAIN_HAND) == EnumActionResult.SUCCESS;
				} else if (action == CPacketUseEntity.Action.ATTACK) {
					if (entity instanceof EntityItem || entity instanceof EntityXPOrb || entity instanceof EntityArrow || entity == player) return false;
					player.attackTargetEntityWithCurrentItem(entity);
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
		Vec3d vec3d = player.getPositionEyes(partialTicks);
		Vec3d vec3d1 = player.getLook(partialTicks);
		Vec3d vec3d2 = vec3d.add(vec3d1.x * reachDist, vec3d1.y * reachDist, vec3d1.z * reachDist);
		return world.rayTraceBlocks(vec3d, vec3d2, false, false, true);
	}

}
