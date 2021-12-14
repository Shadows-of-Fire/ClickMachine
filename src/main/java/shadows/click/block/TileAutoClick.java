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
		this.held = new ItemStackHandler(1) {
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

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		if (this.player == null) {
			this.player = new WeakReference<>(FakePlayerUtil.getPlayer(level, this.profile != null ? this.profile : DEFAULT_CLICKER));
		}
		if (!level.hasNeighborSignal(this.worldPosition)) {
			int use = ClickMachineConfig.usesRF ? ClickMachineConfig.powerPerSpeed[this.getSpeedIndex()] : 0;
			if (this.power.extractEnergy(use, true) == use) {
				this.power.extractEnergy(use, false);
				if (this.player != null && this.counter++ % this.getSpeed() == 0) {
					Direction facing = level.getBlockState(this.worldPosition).getValue(BlockAutoClick.FACING);
					FakePlayerUtil.setupFakePlayerForUse(this.getPlayer(), this.worldPosition, facing, this.held.getStackInSlot(0).copy(), this.sneak);
					ItemStack result = this.held.getStackInSlot(0);
					if (this.rightClick) result = FakePlayerUtil.rightClickInDirection(this.getPlayer(), this.level, this.worldPosition, facing, level.getBlockState(this.worldPosition));
					else result = FakePlayerUtil.leftClickInDirection(this.getPlayer(), this.level, this.worldPosition, facing, level.getBlockState(this.worldPosition));
					FakePlayerUtil.cleanupFakePlayerFromUse(this.getPlayer(), result, this.held.getStackInSlot(0), this);
					this.setChanged();
				}
			}
			if (!state.getValue(BlockAutoClick.ACTIVE)) {
				level.setBlock(this.worldPosition, state.setValue(BlockAutoClick.ACTIVE, true), 2);
			}
		} else {
			if (state.getValue(BlockAutoClick.ACTIVE)) {
				level.setBlock(this.worldPosition, state.setValue(BlockAutoClick.ACTIVE, false), 2);
			}
		}
	}

	public void setPlayer(Player player) {
		this.profile = player.getGameProfile();
		this.setChanged();
	}

	LazyOptional<IItemHandler> ihopt = LazyOptional.of(() -> this.held);
	LazyOptional<IEnergyStorage> ieopt = LazyOptional.of(() -> this.power);

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return this.ihopt.cast();
		if (cap == CapabilityEnergy.ENERGY && ClickMachineConfig.usesRF) return this.ieopt.cast();
		return super.getCapability(cap, side);
	}

	UsefulFakePlayer getPlayer() {
		return this.player.get();
	}

	public IItemHandler getHandler() {
		return this.held;
	}

	public int getSpeed() {
		return ClickMachineConfig.speeds[this.getSpeedIndex()];
	}

	public int getSpeedIndex() {
		return this.speedIdx;
	}

	public void setSpeedIndex(int speedIdx) {
		this.speedIdx = speedIdx;
		this.setChanged();
	}

	public boolean isSneaking() {
		return this.sneak;
	}

	public void setSneaking(boolean sneak) {
		this.sneak = sneak;
		this.setChanged();
	}

	public boolean isRightClicking() {
		return this.rightClick;
	}

	public void setRightClicking(boolean rightClick) {
		this.rightClick = rightClick;
		this.setChanged();
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
		if (this.profile != null) {
			tag.putUUID(tagUUID, this.profile.getId());
			tag.putString(tagName, this.profile.getName());
		}
		tag.put(tagHandler, this.held.serializeNBT());
		tag.putInt(tagCounter, this.counter % this.getSpeed());
		this.writeSyncData(tag);
		return super.save(tag);
	}

	void writeSyncData(CompoundTag tag) {
		tag.putInt(tagSpeed, this.getSpeedIndex());
		tag.putBoolean(tagSneak, this.sneak);
		tag.putBoolean(tagRightClick, this.rightClick);
		tag.putInt(tagEnergy, this.power.getEnergyStored());
	}

	void readSyncData(CompoundTag tag) {
		this.setSpeedIndex(tag.getInt(tagSpeed));
		this.sneak = tag.getBoolean(tagSneak);
		this.rightClick = tag.getBoolean(tagRightClick);
		this.setPower(tag.getInt(tagEnergy));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains(tagUUID) && tag.contains(tagName)) this.profile = new GameProfile(tag.getUUID(tagUUID), tag.getString(tagName));
		if (tag.contains(tagHandler)) this.held.deserializeNBT(tag.getCompound(tagHandler));
		this.counter = tag.getInt(tagCounter);
		this.readSyncData(tag);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		CompoundTag tag = new CompoundTag();
		this.writeSyncData(tag);
		return new ClientboundBlockEntityDataPacket(this.worldPosition, 05150, tag);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		this.readSyncData(pkt.getTag());
	}

	public int getPower() {
		return this.power.getEnergyStored();
	}

	public void setPower(int energy) {
		this.power.extractEnergy(this.power.getMaxEnergyStored(), false);
		this.power.receiveEnergy(energy, false);
		this.setChanged();
	}

	@Override
	public void accept(ItemStack s) {
		this.held.setStackInSlot(0, s);
	}

	@Override
	public ContainerData getData() {
		return this.data;
	}

}
