package shadows.click.block;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;
import shadows.click.util.FakePlayerUtil;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;
import shadows.placebo.block_entity.TickingBlockEntity;
import shadows.placebo.cap.ModifiableEnergyStorage;
import shadows.placebo.container.EasyContainerData;
import shadows.placebo.container.EasyContainerData.IDataAutoRegister;

public class TileAutoClick extends BlockEntity implements Consumer<ItemStack>, TickingBlockEntity, IDataAutoRegister {

	public static final GameProfile DEFAULT_CLICKER = new GameProfile(UUID.fromString("36f373ac-29ef-4150-b664-e7e6006efcd8"), "[The Click Machine]");

	ItemStackHandler held;
	ModifiableEnergyStorage power = new ModifiableEnergyStorage(ClickMachineConfig.maxPowerStorage);
	int speedIdx = 0;
	boolean sneak = false;
	boolean rightClick = true;

	GameProfile profile;
	WeakReference<UsefulFakePlayer> player;

	int counter = 0;

	protected final EasyContainerData data = new EasyContainerData();

	public TileAutoClick(BlockPos pos, BlockState state) {
		super(ClickMachine.TILE, pos, state);
		held = new ItemStackHandler(1) {
			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return !ClickMachineConfig.blacklistedItems.contains(stack.getItem());
			}
		};
		this.data.addEnergy(this.power);
		this.data.addData(() -> this.speedIdx, v -> this.speedIdx = v);
		this.data.addData(() -> this.sneak, v -> this.sneak = v);
		this.data.addData(() -> this.rightClick, v -> this.rightClick = v);
	}

	public void serverTick(Level level, BlockPos pos, BlockState state) {
		if (player == null) {
			player = new WeakReference<>(FakePlayerUtil.getPlayer(level, profile != null ? profile : DEFAULT_CLICKER));
		}
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

	public void setPlayer(Player player) {
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
	public CompoundTag save(CompoundTag tag) {
		if (profile != null) {
			tag.putUUID(tagUUID, profile.getId());
			tag.putString(tagName, profile.getName());
		}
		tag.put(tagHandler, held.serializeNBT());
		tag.putInt(tagCounter, counter % getSpeed());
		writeSyncData(tag);
		return super.save(tag);
	}

	void writeSyncData(CompoundTag tag) {
		tag.putInt(tagSpeed, getSpeedIndex());
		tag.putBoolean(tagSneak, sneak);
		tag.putBoolean(tagRightClick, rightClick);
		tag.putInt(tagEnergy, power.getEnergyStored());
	}

	void readSyncData(CompoundTag tag) {
		setSpeedIndex(tag.getInt(tagSpeed));
		sneak = tag.getBoolean(tagSneak);
		rightClick = tag.getBoolean(tagRightClick);
		setPower(tag.getInt(tagEnergy));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains(tagUUID) && tag.contains(tagName)) profile = new GameProfile(tag.getUUID(tagUUID), tag.getString(tagName));
		if (tag.contains(tagHandler)) held.deserializeNBT(tag.getCompound(tagHandler));
		counter = tag.getInt(tagCounter);
		readSyncData(tag);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		CompoundTag tag = new CompoundTag();
		writeSyncData(tag);
		return new ClientboundBlockEntityDataPacket(worldPosition, 05150, tag);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
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
	public ContainerData getData() {
		return data;
	}

}
