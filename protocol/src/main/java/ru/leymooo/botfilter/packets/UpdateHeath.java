package ru.leymooo.botfilter.packets;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UpdateHeath extends DefinedPacket
{

    float health;
    int food;
    float foodSaturation;

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        buf.writeFloat( this.health );
        DefinedPacket.writeVarInt( food, buf );
        buf.writeFloat( this.foodSaturation );
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
    }
}