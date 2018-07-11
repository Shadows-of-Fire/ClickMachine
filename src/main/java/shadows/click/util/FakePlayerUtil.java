package shadows.click.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.material.Material;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
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
			return 0;
		}

		@Override
		public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
			//NO-OP
		}
	}

	/**
	 * Only store this as a WeakReference, or you'll cause memory leaks.
	 */
	public static UsefulFakePlayer getPlayer(World world, GameProfile profile) {
		return PLAYERS.computeIfAbsent(Pair.of(profile, world.provider.getDimension()), p -> new UsefulFakePlayer(world, p.getLeft()));
	}

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
	public static void setupFakePlayerForUse(UsefulFakePlayer player, BlockPos pos, EnumFacing direction, ItemStack toHold) {
		player.inventory.mainInventory.set(player.inventory.currentItem, toHold);
		float pitch = direction == EnumFacing.UP ? -90 : direction == EnumFacing.DOWN ? 90 : 0;
		float yaw = direction == EnumFacing.SOUTH ? 0 : direction == EnumFacing.WEST ? 90 : direction == EnumFacing.NORTH ? 180 : -90;
		Vec3i sideVec = direction.getDirectionVec();
		double x = 0.5 + sideVec.getX() / 1.9D;
		double y = 0.5 + sideVec.getY() / 1.9D;
		double z = 0.5 + sideVec.getZ() / 1.9D;
		player.setLocationAndAngles(pos.getX() + x, pos.getY() + y, pos.getZ() + z, yaw, pitch);
	}

	/**
	 * Uses whatever the player happens to be holding in the given direction.
	 * @param player The player.
	 * @param world The world of the calling tile entity.  It may be a bad idea to use {@link FakePlayer#getEntityWorld()}.
	 * @param pos The pos of the calling tile entity.
	 * @param side The direction to use in.
	 * @return The remainder of whatever the player was holding.  This should be set back into the tile's stack handler or similar.
	 */
	public static ItemStack rightClickInDirection(UsefulFakePlayer player, World world, BlockPos pos, EnumFacing side) {
		Vec3d base = new Vec3d(player.posX, player.posY, player.posZ);
		Vec3d look = player.getLookVec();
		Vec3d target = base.addVector(look.x * 5, look.y * 5, look.z * 5);
		RayTraceResult trace = world.rayTraceBlocks(base, target, false, false, true);

		if (trace == null) player.getHeldItemMainhand();

		ItemStack itemstack = player.getHeldItemMainhand();
		if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos blockpos = trace.getBlockPos();
			if (world.getBlockState(blockpos).getMaterial() != Material.AIR) {
				float f = (float) (trace.hitVec.x - pos.getX());
				float f1 = (float) (trace.hitVec.y - pos.getY());
				float f2 = (float) (trace.hitVec.z - pos.getZ());
				EnumActionResult enumactionresult = player.interactionManager.processRightClickBlock(player, world, itemstack, EnumHand.MAIN_HAND, blockpos, trace.sideHit, f, f1, f2);
				if (enumactionresult == EnumActionResult.SUCCESS) { return player.getHeldItemMainhand(); }
			}
		}
		if (itemstack.isEmpty() && (trace == null || trace.typeOfHit == RayTraceResult.Type.MISS)) ForgeHooks.onEmptyClick(player, EnumHand.MAIN_HAND);
		if (!itemstack.isEmpty()) player.interactionManager.processRightClick(player, world, itemstack, EnumHand.MAIN_HAND);
		return player.getHeldItemMainhand();
	}

}
