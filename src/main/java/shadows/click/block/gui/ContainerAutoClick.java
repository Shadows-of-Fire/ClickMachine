package shadows.click.block.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.items.SlotItemHandler;
import shadows.click.ClickMachine;
import shadows.click.block.TileAutoClick;
import shadows.click.net.MessageUpdateGui;
import shadows.placebo.net.MessageButtonClick.IButtonContainer;

public class ContainerAutoClick extends Container implements IButtonContainer {

	TileAutoClick tile;
	PlayerEntity player;

	public ContainerAutoClick(int id, TileAutoClick tile, PlayerEntity player) {
		super(ClickMachine.CONTAINER, id);
		this.tile = tile;
		this.player = player;

		this.addSlot(new SlotItemHandler(tile.getHandler(), 0, 8, 35));

		for (int i1 = 0; i1 < 3; ++i1) {
			for (int k1 = 0; k1 < 9; ++k1) {
				this.addSlot(new Slot(player.inventory, k1 + i1 * 9 + 9, 8 + k1 * 18, 84 + i1 * 18));
			}
		}

		for (int j1 = 0; j1 < 9; ++j1) {
			this.addSlot(new Slot(player.inventory, j1, 8 + j1 * 18, 142));
		}
	}

	public ContainerAutoClick(int id, PlayerInventory inv, PacketBuffer buf) {
		super(ClickMachine.CONTAINER, id);
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> this.tile = (TileAutoClick) Minecraft.getInstance().world.getTileEntity(buf.readBlockPos()));
		this.player = inv.player;

		this.addSlot(new SlotItemHandler(tile.getHandler(), 0, 8, 35));

		for (int i1 = 0; i1 < 3; ++i1) {
			for (int k1 = 0; k1 < 9; ++k1) {
				this.addSlot(new Slot(player.inventory, k1 + i1 * 9 + 9, 8 + k1 * 18, 84 + i1 * 18));
			}
		}

		for (int j1 = 0; j1 < 9; ++j1) {
			this.addSlot(new Slot(player.inventory, j1, 8 + j1 * 18, 142));
		}
	}

	@Override
	public boolean canInteractWith(PlayerEntity player) {
		return !tile.isRemoved();
	}

	@Override
	public void onButtonClick(int button) {
		if (button < 12) {
			if (button < 9) tile.setSpeedIndex(button);
			else if (button == 9) tile.setSneaking(!tile.isSneaking());
			else tile.setRightClicking(button == 11);
		}
	}

	@Override
	public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex) {
		ItemStack transferred = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(slotIndex);

		int otherSlots = this.inventorySlots.size() - 36;

		if (slot != null && slot.getHasStack()) {
			ItemStack current = slot.getStack();
			transferred = current.copy();

			if (slotIndex < otherSlots) {
				if (!this.mergeItemStack(current, otherSlots, this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.mergeItemStack(current, 0, otherSlots, false)) { return ItemStack.EMPTY; }
			slot.onSlotChanged();
		}

		return transferred;
	}

	@Override
	public void onContainerClosed(PlayerEntity player) {
		if (!player.world.isRemote) ((ServerPlayerEntity) player).connection.sendPacket(this.tile.getUpdatePacket());
	}

	boolean sent = false;

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (!sent && tile.hasWorld() && !tile.getWorld().isRemote) {
			sent = true;
			ClickMachine.CHANNEL.sendTo(new MessageUpdateGui(tile), ((ServerPlayerEntity) player).connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
		}
	}

}
