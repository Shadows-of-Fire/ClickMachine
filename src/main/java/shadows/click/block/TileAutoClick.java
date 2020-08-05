package shadows.click.block;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.network.PacketDistributor.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;
import shadows.click.block.gui.ContainerAutoClick;
import shadows.click.net.MessageUpdateGui;
import shadows.click.util.FakePlayerUtil;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;
import shadows.placebo.util.NetworkUtils;

public class TileAutoClick extends TileEntity implements ITickableTileEntity, Consumer<ItemStack>, INamedContainerProvider {

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

	public TileAutoClick() {
		super(ClickMachine.TILE);
	}

	@Override
	public void tick() {
		if (world.isRemote) return;
		if (player == null) {
			player = new WeakReference<>(FakePlayerUtil.getPlayer(world, profile != null ? profile : DEFAULT_CLICKER));
		}

		if (!world.isBlockPowered(pos)) {

			int use = ClickMachineConfig.usesRF ? ClickMachineConfig.powerPerSpeed[getSpeedIndex()] : 0;
			if (power.extractEnergy(use, true) == use) {
				power.extractEnergy(use, false);
				if (player != null && counter++ % getSpeed() == 0) {
					Direction facing = world.getBlockState(pos).get(BlockAutoClick.FACING);
					FakePlayerUtil.setupFakePlayerForUse(getPlayer(), this.pos, facing, held.getStackInSlot(0).copy(), sneak);
					ItemStack result = held.getStackInSlot(0);
					if (rightClick) result = FakePlayerUtil.rightClickInDirection(getPlayer(), this.world, this.pos, facing, world.getBlockState(pos));
					else result = FakePlayerUtil.leftClickInDirection(getPlayer(), this.world, this.pos, facing, world.getBlockState(pos));
					FakePlayerUtil.cleanupFakePlayerFromUse(getPlayer(), result, held.getStackInSlot(0), this);
					markDirty();
				}
			}

		}

		if (counter % ClickMachineConfig.powerUpdateFreq == 0 && power.getEnergyStored() != lastPower) {
			NetworkUtils.sendToTracking(ClickMachine.CHANNEL, new MessageUpdateGui(power.getEnergyStored()), (ServerWorld) world, pos);
			lastPower = power.getEnergyStored();
		}
	}

	public void setPlayer(PlayerEntity player) {
		profile = player.getGameProfile();
		markDirty();
	}

	public ItemStack insert(ItemStack stack) {
		ItemStack s = held.insertItem(0, stack, false);
		markDirty();
		return s;
	}

	LazyOptional<IItemHandler> ihopt = LazyOptional.of(() -> held);
	LazyOptional<IEnergyStorage> ieopt = LazyOptional.of(() -> power);

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return ihopt.cast();
		if (cap == CapabilityEnergy.ENERGY) return ieopt.cast();
		return super.getCapability(cap, side);
	}

	UsefulFakePlayer getPlayer() {
		return player.get();
	}

	public IItemHandler getHandler() {
		return held;
	}

	public int getSpeed() {
		return ClickMachineConfig.speeds[getSpeedIndex()];
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
	public CompoundNBT write(CompoundNBT tag) {
		if (profile != null) {
			tag.putUniqueId(tagUUID, profile.getId());
			tag.putString(tagName, profile.getName());
		}
		tag.put(tagHandler, held.serializeNBT());
		tag.putInt(tagCounter, counter % getSpeed());
		writeSyncData(tag);
		return super.write(tag);
	}

	void writeSyncData(CompoundNBT tag) {
		tag.putInt(tagSpeed, getSpeedIndex());
		tag.putBoolean(tagSneak, sneak);
		tag.putBoolean(tagRightClick, rightClick);
		tag.putInt(tagEnergy, power.getEnergyStored());
	}

	void readSyncData(CompoundNBT tag) {
		setSpeedIndex(tag.getInt(tagSpeed));
		sneak = tag.getBoolean(tagSneak);
		rightClick = tag.getBoolean(tagRightClick);
		setPower(tag.getInt(tagEnergy));
	}

	@Override
	public void fromTag(BlockState state, CompoundNBT tag) {
		super.fromTag(state, tag);
		if (tag.contains(tagUUID) && tag.contains(tagName)) profile = new GameProfile(tag.getUniqueId(tagUUID), tag.getString(tagName));
		if (tag.contains(tagHandler)) held.deserializeNBT(tag.getCompound(tagHandler));
		counter = tag.getInt(tagCounter);
		readSyncData(tag);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT tag = new CompoundNBT();
		writeSyncData(tag);
		return new SUpdateTileEntityPacket(pos, 05150, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
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

	@Override
	public void accept(ItemStack s) {
		held.setStackInSlot(0, s);
	}

	@Override
	public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return new ContainerAutoClick(id, this, player);
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent("gui.clickmachine.autoclick");
	}

}
