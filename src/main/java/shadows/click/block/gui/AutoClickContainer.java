package shadows.click.block.gui;

import dev.shadowsoffire.placebo.menu.BlockEntityMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.SlotItemHandler;
import shadows.click.ClickMachine;
import shadows.click.block.AutoClickerTile;

public class AutoClickContainer extends BlockEntityMenu<AutoClickerTile> {

	public AutoClickContainer(int id, Inventory pInv, BlockPos pos) {
		super(ClickMachine.AUTO_CLICKER_MENU, id, pInv, pos);
		this.addSlot(new SlotItemHandler(this.tile.getHandler(), 0, 8, 50)); //Add clicker item slot
		this.addPlayerSlots(pInv, 8, 114);
		this.mover.registerRule((stack, slot) -> slot == 0, 1, this.slots.size());
		this.mover.registerRule((stack, slot) -> slot > 0, 0, 1);
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

	public boolean isSneaking() {
		return this.tile.isSneaking();
	}

	public boolean isRightClicking() {
		return this.tile.isRightClicking();
	}

	public int getEnergy() {
		return this.tile.getPower();
	}

	public int getSpeedIdx() {
		return this.tile.getSpeedIndex();
	}

}
