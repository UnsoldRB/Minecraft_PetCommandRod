package com.unsoldriceball.petcommandrod;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

import static com.unsoldriceball.petcommandrod.RodEffect.rodSwing;




public class Packet implements IMessage
{
    private UUID uuid;
    private int dim;




    //コンストラクタ
    public Packet()
    {
        // 空のコンストラクタが必要らしい。
    }



    //コンストラクタ2個目
    public Packet(UUID uuid, Integer dim)
    {
        this.uuid = uuid;
        this.dim = dim;
    }



    //パケット送信時に送信元が処理するイベント
    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        buf.writeInt(dim);
    }



    //パケット受信時に送信先が処理するイベント。
    @Override
    public void fromBytes(ByteBuf buf)
    {
        long mostSignificantBits = buf.readLong();
        long leastSignificantBits = buf.readLong();

        uuid = new UUID(mostSignificantBits, leastSignificantBits);
        dim = buf.readInt();
    }



    public static class Handler implements IMessageHandler<Packet, IMessage>
    {
        //パケットを受信したときに発生するイベント
        @Override
        public IMessage onMessage(Packet message, MessageContext ctx)
        {
            final UUID L_UUID = message.uuid;
            final int L_DIM = message.dim;
            final EntityPlayer L_PLAYER = DimensionManager.getWorld(L_DIM).getPlayerEntityByUUID(L_UUID);

            if (L_PLAYER != null)
            {
                rodSwing(L_PLAYER);
            }

            return null;
        }
    }
}