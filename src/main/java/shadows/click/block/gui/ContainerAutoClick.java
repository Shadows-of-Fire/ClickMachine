package shadows.click.block.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntArray;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import shadows.click.ClickMachine;
import shadows.placebo.net.MessageButtonClick.IButtonContainer;

public class ContainerAutoClick extends Container implements IButtonContainer {

	protected PlayerEntity player;
	protected IWorldPosCallable wPos;
	//[power, speed, sneak, rightClick]
	protected final IIntArray data;

	public ContainerAutoClick(int id, PlayerInventory playerInventory) {
		this(id, playerInventory, IWorldPosCallable.NULL, new ItemStackHandler(1), new IntArray(4));
	}

	public ContainerAutoClick(int id, PlayerInventory pInv, IWorldPosCallable wPos, ItemStackHandler inv, IIntArray data) {
		super(ClickMachine.CONTAINER, id);

		this.player = pInv.player;
		this.wPos = wPos;
		this.data = data;

		this.addSlot(new SlotItemHandler(inv, 0, 8, 50)); //Add clicker item slot

		//Player inv slots
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(player.inventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
			}
		}

		//Player hotbar slots
		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(player.inventory, col, 8 + col * 18, 172));
		}

		this.addDataSlots(data);
	}

	@Override
	public boolean stillValid(PlayerEntity player) {
		return wPos.evaluate((w, p) -> w.getBlockState(p).getBlock() == ClickMachine.AUTO_CLICKER, true);
	}

	@Override
	public void onButtonClick(int button) {
		if (button == 2) data.set(2, data.get(2) == 0 ? 1 : 0);
		else if (button == 3) data.set(3, data.get(3) == 0 ? 1 : 0);
		else if (button >= 4 && button <= 12) {
			this.data.set(1, button - 4);
		}
	}

	@Override
	public ItemStack quickMoveStack(PlayerEntity player, int slotIndex) {
		ItemStack transferred = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);

		int otherSlots = this.slots.size() - 36;

		if (slot != null && slot.hasItem()) {
			ItemStack current = slot.getItem();
			transferred = current.copy();

			if (slotIndex < otherSlots) {
				if (!this.moveItemStackTo(current, otherSlots, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(current, 0, otherSlots, false)) { return ItemStack.EMPTY; }
			slot.setChanged();
		}

		return transferred;
	}

}
