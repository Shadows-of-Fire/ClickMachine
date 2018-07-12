package shadows.click.block.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import shadows.click.block.TileAutoClick;

public class ClickGuiHandler implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerAutoClick((TileAutoClick) world.getTileEntity(new BlockPos(x, y, z)), player);
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GuiAutoClick((TileAutoClick) world.getTileEntity(new BlockPos(x, y, z)));
	}

}
