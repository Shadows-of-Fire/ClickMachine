package shadows.click.block;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import shadows.click.ClickMachine;
import shadows.placebo.block.BlockBasic;
import shadows.placebo.interfaces.IItemBlock;

public class BlockAutoClick extends BlockBasic implements IItemBlock {

	public static final PropertyDirection FACING = BlockDirectional.FACING;

	public BlockAutoClick() {
		super("auto_clicker", Material.IRON, 5, 5, ClickMachine.INFO);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileAutoClick();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.VALUES[meta % 6]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).ordinal();
	}

	@Override
	public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
		EnumFacing facing = world.getBlockState(pos).getValue(FACING);
		EnumFacing rotated = facing.rotateAround(axis.getAxis());
		return world.setBlockState(pos, getDefaultState().withProperty(FACING, rotated));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		TileEntity te = world.getTileEntity(pos);
		if (!world.isRemote && te instanceof TileAutoClick && placer instanceof EntityPlayer) {
			((TileAutoClick) te).setPlayer((EntityPlayer) placer);
		}
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		EnumFacing face = placer.getHorizontalFacing().getOpposite();
		if (placer.rotationPitch > 50) face = EnumFacing.UP;
		else if (placer.rotationPitch < -50) face = EnumFacing.DOWN;
		return getDefaultState().withProperty(FACING, face);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) player.openGui(ClickMachine.INSTANCE, 0, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileAutoClick) spawnAsEntity(world, pos, ((TileAutoClick) te).held.getStackInSlot(0));
		super.breakBlock(world, pos, state);
	}

}
