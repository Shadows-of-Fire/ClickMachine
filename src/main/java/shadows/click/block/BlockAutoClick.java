package shadows.click.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import shadows.click.ClickMachine;

public class BlockAutoClick extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public BlockAutoClick() {
		super(Block.Properties.create(Material.IRON).hardnessAndResistance(5));
	}

	@Override
	public boolean hasTileEntity() {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new TileAutoClick();
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
		return state.with(FACING, direction.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.with(FACING, mirror.mirror(state.get(FACING)));
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		TileEntity te = world.getTileEntity(pos);
		if (!world.isRemote && te instanceof TileAutoClick && placer instanceof PlayerEntity) {
			((TileAutoClick) te).setPlayer((PlayerEntity) placer);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		PlayerEntity placer = context.getPlayer();
		Direction face = placer.getHorizontalFacing().getOpposite();
		if (placer.rotationPitch > 50) face = Direction.UP;
		else if (placer.rotationPitch < -50) face = Direction.DOWN;
		return getDefaultState().with(FACING, face);
	}

	@Override
	public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		player.openContainer(p_213829_1_)
		return true;
	}

	@Override
	@Deprecated
	public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileAutoClick) spawnAsEntity(world, pos, ((TileAutoClick) te).held.getStackInSlot(0));
		super.onReplaced(state, world, pos, newState, isMoving);
	}

}
