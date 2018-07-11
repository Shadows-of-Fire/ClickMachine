package shadows.click.block;

import java.lang.ref.WeakReference;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.click.util.FakePlayerUtil;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;

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
		if (player == null && profile != null) {
			player = new WeakReference<>(FakePlayerUtil.getPlayer(world, profile));
		} else if (player == null) {
			player = new WeakReference<>(FakePlayerUtil.getPlayer(world, DEFAULT_CLICKER));
		}

		if (player != null && counter++ % 50 == 0) {
			EnumFacing facing = world.getBlockState(pos).getValue(BlockAutoClick.FACING);
			FakePlayerUtil.setupFakePlayerForUse(getPlayer(), this.pos, facing, held.getStackInSlot(0));
			if (rightClick) held.setStackInSlot(0, FakePlayerUtil.rightClickInDirection(getPlayer(), this.world, this.pos, facing));
			else hitShit(facing);
			held.setStackInSlot(0, getPlayer().getHeldItemMainhand());
			getPlayer().inventory.mainInventory.set(getPlayer().inventory.currentItem, ItemStack.EMPTY);
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

	public void hitShit(EnumFacing side) {
		//Dunno what do yet.
	}

	public void useItem(EnumFacing side) {
		Vec3d base = new Vec3d(getPlayer().posX, getPlayer().posY, getPlayer().posZ);
		Vec3d look = getPlayer().getLookVec();
		Vec3d target = base.addVector(look.x * 5, look.y * 5, look.z * 5);
		RayTraceResult trace = world.rayTraceBlocks(base, target, false, false, true);

		if (trace == null) return;

		ItemStack itemstack = getPlayer().getHeldItemMainhand();
		if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos blockpos = trace.getBlockPos();
			if (this.world.getBlockState(blockpos).getMaterial() != Material.AIR) {
				float f = (float) (trace.hitVec.x - pos.getX());
				float f1 = (float) (trace.hitVec.y - pos.getY());
				float f2 = (float) (trace.hitVec.z - pos.getZ());
				EnumActionResult enumactionresult = getPlayer().interactionManager.processRightClickBlock(getPlayer(), this.world, itemstack, EnumHand.MAIN_HAND, blockpos, trace.sideHit, f, f1, f2);
				if (enumactionresult == EnumActionResult.SUCCESS) {
					held.setStackInSlot(0, getPlayer().getHeldItemMainhand());
					return;
				}
			}
		}
		if (itemstack.isEmpty() && (trace == null || trace.typeOfHit == RayTraceResult.Type.MISS)) ForgeHooks.onEmptyClick(getPlayer(), EnumHand.MAIN_HAND);
		if (!itemstack.isEmpty()) getPlayer().interactionManager.processRightClick(getPlayer(), this.world, itemstack, EnumHand.MAIN_HAND);
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
