package shadows.click.net;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import shadows.click.block.TileAutoClick;
import shadows.click.block.gui.GuiAutoClick;
import shadows.placebo.util.NetworkUtils;
import shadows.placebo.util.NetworkUtils.MessageProvider;

public class MessageUpdateGui extends MessageProvider<MessageUpdateGui> {

	Type t;
	int power;
	int speedIdx;
	boolean right;
	boolean sneak;

	public MessageUpdateGui(Type t, int power, TileAutoClick te) {
		if (t == Type.POWER) this.power = power;
		else {
			this.power = te.getPower();
			speedIdx = te.getSpeedIndex();
			right = te.isRightClicking();
			sneak = te.isSneaking();
		}
		this.t = t;
	}

	public MessageUpdateGui(TileAutoClick te) {
		this(Type.SYNC, -1, te);
	}

	public MessageUpdateGui(int power) {
		this(Type.POWER, power, null);
	}

	public MessageUpdateGui() {

	}

	@Override
	public MessageUpdateGui read(PacketBuffer buf) {
		MessageUpdateGui msg = new MessageUpdateGui();
		msg.t = Type.values()[buf.readByte()];
		msg.power = buf.readInt();
		if (msg.t == Type.SYNC) {
			msg.speedIdx = buf.readInt();
			msg.right = buf.readBoolean();
			msg.sneak = buf.readBoolean();
		}
		return msg;
	}

	@Override
	public void write(MessageUpdateGui msg, PacketBuffer buf) {
		buf.writeByte(msg.t.ordinal());
		buf.writeInt(msg.power);
		if (msg.t == Type.SYNC) {
			buf.writeInt(msg.speedIdx);
			buf.writeBoolean(msg.right);
			buf.writeBoolean(msg.sneak);
		}
	}

	@Override
	public void handle(MessageUpdateGui msg, Supplier<Context> ctx) {
		NetworkUtils.handlePacket(() -> () -> {
			if (Minecraft.getInstance().currentScreen instanceof GuiAutoClick) {
				if (msg.t == Type.POWER) ((GuiAutoClick) Minecraft.getInstance().currentScreen).getTile().setPower(msg.power);
				else((GuiAutoClick) Minecraft.getInstance().currentScreen).updateTile(msg.speedIdx, msg.sneak, msg.right, msg.power);
			}
		}, ctx.get());
	}

	@Override
	public Class<MessageUpdateGui> getMsgClass() {
		return MessageUpdateGui.class;
	}

	public enum Type {
		POWER,
		SYNC;
	}

}
