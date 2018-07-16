package shadows.click.block;

import java.lang.ref.WeakReference;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;
import shadows.click.net.MessageUpdateGui;
import shadows.click.util.FakePlayerUtil;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;

public class TileAutoClick extends TileEntity implements ITickable {

	public static final GameProfile DEFAULT_CLICKER = new GameProfile(UUID.fromString("36f373ac-29ef-4150-b664-e7e6006efcd8"), "[The Click Machine]");

	ItemStackHandler held = new ItemStackHandler(1);
	EnergyStorage power = new EnergyStorage(ClickMachineConfig.maxPowerStorage);
	GameProfile profile;
	WeakReference<UsefulFakePlayer> player;
	int counter = 0;
	boolean rightClick = true;
	boolean sneak = false;
	int speedIdx = 0;
	TargetPoint us;
	int lastPower = 0;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (player == null) {
			player = new WeakReference<>(FakePlayerUtil.getPlayer(world, profile != null ? profile : DEFAULT_CLICKER));
		}

		if (world.isBlockPowered(pos)) return;

		int use = ClickMachineConfig.powerPerSpeed[speedIdx];
		if (player != null && counter++ % getSpeed() == 0 && power.extractEnergy(use, true) == use) {
			EnumFacing facing = world.getBlockState(pos).getValue(BlockAutoClick.FACING);
			FakePlayerUtil.setupFakePlayerForUse(getPlayer(), this.pos, facing, held.getStackInSlot(0).copy(), sneak);
			ItemStack result = held.getStackInSlot(0);
			if (rightClick) result = FakePlayerUtil.rightClickInDirection(getPlayer(), this.world, this.pos, facing, world.getBlockState(pos));
			else result = FakePlayerUtil.leftClickInDirection(getPlayer(), this.world, this.pos, facing, world.getBlockState(pos));
			FakePlayerUtil.cleanupFakePlayerFromUse(getPlayer(), result, held.getStackInSlot(0), s -> held.setStackInSlot(0, s));
			power.extractEnergy(use, false);
			markDirty();
		}

		if (counter % 10 == 0 && power.getEnergyStored() != lastPower) {
			if (us == null) us = new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 0);
			ClickMachine.NETWORK.sendToAllTracking(new MessageUpdateGui(power.getEnergyStored()), us);
			lastPower = power.getEnergyStored();
		}
	}

	public void setPlayer(EntityPlayer player) {
		profile = player.getGameProfile();
		markDirty();
	}

	public ItemStack insert(ItemStack stack) {
		ItemStack s = held.insertItem(0, stack, false);
		markDirty();
		return s;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(held);
		if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(power);
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState state1, IBlockState state2) {
		return state1.getBlock() != state2.getBlock();
	}

	UsefulFakePlayer getPlayer() {
		return player.get();
	}

	public IItemHandler getHandler() {
		return held;
	}

	public int getSpeed() {
		return ClickMachineConfig.speeds[speedIdx];
	}

	public int getSpeedIndex() {
		return speedIdx;
	}

	public void setSpeedIndex(int speedIdx) {
		this.speedIdx = speedIdx;
		markDirty();
	}

	public boolean isSneaking() {
		return sneak;
	}

	public void setSneaking(boolean sneak) {
		this.sneak = sneak;
		markDirty();
	}

	public boolean isRightClicking() {
		return rightClick;
	}

	public void setRightClicking(boolean rightClick) {
		this.rightClick = rightClick;
		markDirty();
	}

	static final String tagUUID = "uuid";
	static final String tagName = "name";
	static final String tagCounter = "counter";
	static final String tagSpeed = "speed_index";
	static final String tagSneak = "sneak";
	static final String tagRightClick = "right_click";
	static final String tagHandler = "inv";
	static final String tagEnergy = "fe";

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		if (profile != null) {
			tag.setUniqueId(tagUUID, profile.getId());
			tag.setString(tagName, profile.getName());
		}
		tag.setTag(tagHandler, held.serializeNBT());
		tag.setInteger(tagCounter, counter % getSpeed());
		writeSyncData(tag);
		return super.writeToNBT(tag);
	}

	void writeSyncData(NBTTagCompound tag) {
		tag.setInteger(tagSpeed, speedIdx);
		tag.setBoolean(tagSneak, sneak);
		tag.setBoolean(tagRightClick, rightClick);
		tag.setInteger(tagEnergy, power.getEnergyStored());
	}

	void readSyncData(NBTTagCompound tag) {
		speedIdx = tag.getInteger(tagSpeed);
		sneak = tag.getBoolean(tagSneak);
		rightClick = tag.getBoolean(tagRightClick);
		setPower(tag.getInteger(tagEnergy));
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (tag.hasKey(tagUUID) && tag.hasKey(tagName)) profile = new GameProfile(tag.getUniqueId(tagUUID), tag.getString(tagName));
		if (tag.hasKey(tagHandler)) held.deserializeNBT(tag.getCompoundTag(tagHandler));
		counter = tag.getInteger(tagCounter);
		readSyncData(tag);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound tag = new NBTTagCompound();
		writeSyncData(tag);
		return new SPacketUpdateTileEntity(pos, 05150, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readSyncData(pkt.getNbtCompound());
	}

	public int getPower() {
		return power.getEnergyStored();
	}

	public void setPower(int energy) {
		power.extractEnergy(power.getMaxEnergyStored(), false);
		power.receiveEnergy(energy, false);
		markDirty();
	}

}
