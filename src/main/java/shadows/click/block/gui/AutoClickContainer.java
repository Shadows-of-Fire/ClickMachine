package shadows.click.block.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.items.SlotItemHandler;
import shadows.click.ClickMachine;
import shadows.click.block.TileAutoClick;
import shadows.placebo.container.BlockEntityContainer;

public class AutoClickContainer extends BlockEntityContainer<TileAutoClick> {

	public AutoClickContainer(int id, Inventory pInv, BlockPos pos) {
		super(ClickMachine.CONTAINER, id, pInv, pos);
		this.addSlot(new SlotItemHandler(this.tile.getHandler(), 0, 8, 50)); //Add clicker item slot
		this.addPlayerSlots(pInv, 8, 114);
		this.mover.registerRule((stack, slot) -> slot == 0, 1, this.slots.size());
		this.mover.registerRule((stack, slot) -> slot > 0, 0, this.slots.size());
	}

	@Override
	public boolean stillValid(Player player) {
		return this.tile != null && !this.tile.isRemoved();
	}

	@Override
	public boolean clickMenuButton(Player pPlayer, int button) {
		if (button == 2) this.tile.setSneaking(!this.tile.isSneaking());
		else if (button == 3) this.tile.setRightClicking(!this.tile.isRightClicking());
		else if (button >= 4 && button <= 12) {
			this.tile.setSpeedIndex(button - 4);
		}
		return button <= 12;
	}

	public ContainerData getData() {
		return this.tile.getData();
	}

	@Override
	public void setData(int pId, int pData) {
		super.setData(pId, pData);
	}

	public int getEnergy() {
		return this.tile.getPower();
	}

	public int getSpeedIdx() {
		return this.tile.getSpeedIndex();
	}

}
