package shadows.click.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class VanillaPacketDispatcher {

	public static void dispatchTEToPlayer(TileEntity tile, EntityPlayer player) {
		World world = tile.getWorld();
		if (world.isRemote) return;
		((EntityPlayerMP) player).connection.sendPacket(tile.getUpdatePacket());
	}

	public static void dispatchTEToNearbyPlayers(TileEntity tile) {
		World world = tile.getWorld();
		if (world.isRemote) return;
		for (EntityPlayer player : world.playerEntities)
			if (pointDistancePlane(player.posX, player.posZ, tile.getPos().getX() + 0.5, tile.getPos().getZ() + 0.5) < 64) ((EntityPlayerMP) player).connection.sendPacket(tile.getUpdatePacket());
	}

	public static void dispatchTEToNearbyPlayers(World world, BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (tile != null) dispatchTEToNearbyPlayers(tile);
	}

	public static float pointDistancePlane(double x1, double y1, double x2, double y2) {
		return (float) Math.hypot(x1 - x2, y1 - y2);
	}
}