package shadows.click.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import shadows.click.block.TileAutoClick;
import shadows.click.block.gui.GuiAutoClick;

public class MessageUpdateGui implements IMessage {

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
	public void fromBytes(ByteBuf buf) {
		t = Type.values()[buf.readByte()];
		power = buf.readInt();
		if (t == Type.SYNC) {
			speedIdx = buf.readInt();
			right = buf.readBoolean();
			sneak = buf.readBoolean();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(t.ordinal());
		buf.writeInt(power);
		if (t == Type.SYNC) {
			buf.writeInt(speedIdx);
			buf.writeBoolean(right);
			buf.writeBoolean(sneak);
		}
	}

	public static class UpdatePowerHandler implements IMessageHandler<MessageUpdateGui, IMessage> {

		@Override
		public MessageUpdateGui onMessage(MessageUpdateGui message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				Gui g = Minecraft.getMinecraft().currentScreen;
				if (g instanceof GuiAutoClick) {
					if (message.t == Type.POWER) ((GuiAutoClick) g).getTile().setPower(message.power);
					else((GuiAutoClick) g).updateTile(message.speedIdx, message.sneak, message.right, message.power);
				}
			});
			return null;
		}

	}

	public enum Type {
		POWER,
		SYNC;
	}

}
