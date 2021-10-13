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
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;
import shadows.click.block.gui.ContainerAutoClick;
import shadows.click.util.FakePlayerUtil;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;

public class TileAutoClick extends TileEntity implements ITickableTileEntity, Consumer<ItemStack>, INamedContainerProvider {

	public static final GameProfile DEFAULT_CLICKER = new GameProfile(UUID.fromString("36f373ac-29ef-4150-b664-e7e6006efcd8"), "[The Click Machine]");

	ItemStackHandler held;
	EnergyStorage power = new EnergyStorage(ClickMachineConfig.maxPowerStorage);
	int speedIdx = 0;
	boolean sneak = false;
	boolean rightClick = true;

	GameProfile profile;
	WeakReference<UsefulFakePlayer> player;

	int counter = 0;

	protected final IIntArray data = new IIntArray() {
		public int get(int index) {
			switch (index) {
			case 0:
				return power.getEnergyStored();
			case 1:
				return speedIdx;
			case 2:
				return sneak ? 1 : 0;
			case 3:
				return rightClick ? 1 : 0;
			default:
				return 0;
			}
		}

		public void set(int index, int value) {
			switch (index) {
			case 0:
				power.extractEnergy(power.getEnergyStored(), false);
				power.receiveEnergy(value, false);
				break;
			case 1:
				speedIdx = value;
				break;
			case 2:
				sneak = value != 0;
				break;
			case 3:
				rightClick = value != 0;
			}
			setChanged();
		}

		public int getCount() {
			return 4;
		}
	};

	public TileAutoClick() {
		super(ClickMachine.TILE);

		held = new ItemStackHandler(1) {
			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return !ClickMachineConfig.blacklistedItems.contains(stack.getItem());
			}
		};
	}

	@Override
	public void tick() {
		if (level.isClientSide) return;
		if (player == null) {
			player = new WeakReference<>(FakePlayerUtil.getPlayer(level, profile != null ? profile : DEFAULT_CLICKER));
		}

		BlockState state = level.getBlockState(worldPosition);

		if (!level.hasNeighborSignal(worldPosition)) {
			int use = ClickMachineConfig.usesRF ? ClickMachineConfig.powerPerSpeed[getSpeedIndex()] : 0;
			if (power.extractEnergy(use, true) == use) {
				power.extractEnergy(use, false);
				if (player != null && counter++ % getSpeed() == 0) {
					Direction facing = level.getBlockState(worldPosition).getValue(BlockAutoClick.FACING);
					FakePlayerUtil.setupFakePlayerForUse(getPlayer(), this.worldPosition, facing, held.getStackInSlot(0).copy(), sneak);
					ItemStack result = held.getStackInSlot(0);
					if (rightClick) result = FakePlayerUtil.rightClickInDirection(getPlayer(), this.level, this.worldPosition, facing, level.getBlockState(worldPosition));
					else result = FakePlayerUtil.leftClickInDirection(getPlayer(), this.level, this.worldPosition, facing, level.getBlockState(worldPosition));
					FakePlayerUtil.cleanupFakePlayerFromUse(getPlayer(), result, held.getStackInSlot(0), this);
					setChanged();
				}
			}
			if (!state.getValue(BlockAutoClick.ACTIVE)) {
				level.setBlock(worldPosition, state.setValue(BlockAutoClick.ACTIVE, true), 2);
			}
		} else {
			if (state.getValue(BlockAutoClick.ACTIVE)) {
				level.setBlock(worldPosition, state.setValue(BlockAutoClick.ACTIVE, false), 2);
			}
		}
	}

	public void setPlayer(PlayerEntity player) {
		profile = player.getGameProfile();
		setChanged();
	}

	LazyOptional<IItemHandler> ihopt = LazyOptional.of(() -> held);
	LazyOptional<IEnergyStorage> ieopt = LazyOptional.of(() -> power);

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return ihopt.cast();
		if (cap == CapabilityEnergy.ENERGY && ClickMachineConfig.usesRF) return ieopt.cast();
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
		setChanged();
	}

	public boolean isSneaking() {
		return sneak;
	}

	public void setSneaking(boolean sneak) {
		this.sneak = sneak;
		setChanged();
	}

	public boolean isRightClicking() {
		return rightClick;
	}

	public void setRightClicking(boolean rightClick) {
		this.rightClick = rightClick;
		setChanged();
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
	public CompoundNBT save(CompoundNBT tag) {
		if (profile != null) {
			tag.putUUID(tagUUID, profile.getId());
			tag.putString(tagName, profile.getName());
		}
		tag.put(tagHandler, held.serializeNBT());
		tag.putInt(tagCounter, counter % getSpeed());
		writeSyncData(tag);
		return super.save(tag);
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
	public void load(BlockState state, CompoundNBT tag) {
		super.load(state, tag);
		if (tag.contains(tagUUID) && tag.contains(tagName)) profile = new GameProfile(tag.getUUID(tagUUID), tag.getString(tagName));
		if (tag.contains(tagHandler)) held.deserializeNBT(tag.getCompound(tagHandler));
		counter = tag.getInt(tagCounter);
		readSyncData(tag);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT tag = new CompoundNBT();
		writeSyncData(tag);
		return new SUpdateTileEntityPacket(worldPosition, 05150, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		readSyncData(pkt.getTag());
	}

	public int getPower() {
		return power.getEnergyStored();
	}

	public void setPower(int energy) {
		power.extractEnergy(power.getMaxEnergyStored(), false);
		power.receiveEnergy(energy, false);
		setChanged();
	}

	@Override
	public void accept(ItemStack s) {
		held.setStackInSlot(0, s);
	}

	@Override
	public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return new ContainerAutoClick(id, inv, IWorldPosCallable.create(level, worldPosition), held, data);
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent("gui.clickmachine.autoclick");
	}

}
