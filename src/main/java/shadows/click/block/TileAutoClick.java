package shadows.click.block;

import java.lang.ref.WeakReference;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.click.util.FakePlayerUtil;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;
import shadows.click.util.NetHandlerSpaghettiServer;

public class TileAutoClick extends TileEntity implements ITickable {

	public static final GameProfile DEFAULT_CLICKER = new GameProfile(UUID.fromString("36f373ac-29ef-4150-b664-e7e6006efcd8"), "[The Click Machine]");

	ItemStackHandler held = new ItemStackHandler(1);
	GameProfile profile;
	WeakReference<UsefulFakePlayer> player;
	int counter = 0;
	boolean rightClick = true;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (player == null) {
			player = new WeakReference<>(FakePlayerUtil.getPlayer(world, profile != null ? profile : DEFAULT_CLICKER));
			getPlayer().connection = new NetHandlerSpaghettiServer(getPlayer());
		}

		if (player != null && counter++ % 50 == 0) {
			EnumFacing facing = world.getBlockState(pos).getValue(BlockAutoClick.FACING);
			FakePlayerUtil.setupFakePlayerForUse(getPlayer(), this.pos, facing, held.getStackInSlot(0).copy());
			ItemStack result = held.getStackInSlot(0);
			if (rightClick) result = FakePlayerUtil.rightClickInDirection(getPlayer(), this.world, this.pos, facing);
			else result = FakePlayerUtil.leftClickInDirection(getPlayer(), this.world, this.pos, facing);
			FakePlayerUtil.cleanupFakePlayerFromUse(getPlayer(), result, held.getStackInSlot(0), s -> held.setStackInSlot(0, s));
		}
	}

	public void setPlayer(EntityPlayer player) {
		profile = player.getGameProfile();
	}

	public ItemStack insert(ItemStack stack) {
		return held.insertItem(0, stack, false);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(held);
		return super.getCapability(capability, facing);
	}

	public ItemStack getStack() {
		return held.getStackInSlot(0);
	}

	public void empty() {
		held.setStackInSlot(0, ItemStack.EMPTY);
	}

	UsefulFakePlayer getPlayer() {
		return player.get();
	}

}
