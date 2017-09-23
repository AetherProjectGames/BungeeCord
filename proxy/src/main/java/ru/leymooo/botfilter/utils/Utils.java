package ru.leymooo.botfilter.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.protocol.packet.extra.SetExp;
import net.md_5.bungee.protocol.packet.extra.SetSlot;
import net.md_5.bungee.protocol.packet.extra.UpdateHeath;
import ru.leymooo.botfilter.Config;
import ru.leymooo.botfilter.BFConnector;
import ru.leymooo.botfilter.Location;

/**
 *
 * @author Leymooo
 */
public class Utils
{

    //Дропаем конекты, если за 10 минут их было больше трёх.
    public static Cache<String, Integer> connections = CacheBuilder.newBuilder()
            .concurrencyLevel( Runtime.getRuntime().availableProcessors() )
            .initialCapacity( 100 )
            .expireAfterWrite( 10, TimeUnit.MINUTES )
            .build();

    public static boolean isManyChecks(String ip, boolean add, boolean check)
    {
        Integer conns = connections.getIfPresent( ip );
        if ( conns != null && conns >= 3 )
        {
            Config.getConfig().getProxy().addProxyForce( ip );
            return true;
        }
        if ( check )
        {
            return conns >= 2;
        }
        if ( add )
        {
            connections.put( ip, conns == null ? 1 : conns + 1 );
        }
        return false;
    }

    public static boolean disconnect(BFConnector connector)
    {
        UserConnection connection = connector.getConnection();
        String ip = connection.getAddress().getAddress().getHostAddress();
        Config config = Config.getConfig();
        GeoIpUtils geo = config.getGeoUtils();
        boolean proxy = config.getProxy().isProxy( ip );
        if ( !config.isProtectionEnabled() )
        {
            return false;
        }
        if ( proxy || !geo.isAllowed( geo.getCountryCode( connector.getConnection().getAddress().getAddress() ), config.isPermanent() ) )
        {
            connection.disconnect( proxy ? config.getErrorProxy() : config.getErrorConutry() );
            return true;
        }
        if ( !( connector.isClientSettings() && connector.isPluginMessage()
                && connector.getChecks() != null && connector.getChecks().isEmpty() ) )
        {
            connection.disconnect( config.getErrorBot() );
            return true;
        }
        return false;
    }

    public static double getFallSpeed(int localTick)
    {
        return formatDouble( Math.abs( ( Math.pow( 0.98, localTick ) - 1 ) * 3.92 ) );
    }

    public static double formatDouble(double d)
    {
        return Math.floor( d * 100 ) / 100;
    }

    public static void sendPackets(BFConnector connector)
    {
        int globalTick = connector.getGlobalTick();
        if ( globalTick >= 7 && globalTick <= 63 && globalTick % 9 == 0 )
        {
            SetSlot slotPacket = connector.getSetSlotPacket();
            slotPacket.setSlot( slotPacket.getSlot() + 1 );
            connector.write( slotPacket );
        }
        if ( globalTick % 4 == 0 && globalTick <= 72 )
        {
            UpdateHeath healthPacket = connector.getHealthPacket();
            SetExp expPacket = connector.getSetExpPacket();
            if ( healthPacket == null )
            {
                healthPacket = new UpdateHeath( 1, 1, 0 );
                expPacket = new SetExp( 0.0f, 1, 1 );
                connector.setHealthPacket( healthPacket );
                connector.setSetExpPacket( expPacket );
                connector.setSetSlotPacket( new SetSlot( 0, 36, 1, 133, 0 ) );
            }
            healthPacket.setHealth( healthPacket.getHealth() + 1 );
            healthPacket.setFood( healthPacket.getFood() + 1 );
            expPacket.setExpBar( expPacket.getExpBar() + 0.0524f < 1 ? expPacket.getExpBar() + 0.0524f : 1 );
            connector.write( healthPacket );
            connector.write( expPacket );
        }
        connector.getChannel().flush();
    }

    public static boolean checkPps(BFConnector connector)
    {
        AtomicInteger packets = connector.getPps();
        if ( packets == null )
        {
            connector.setPps( packets = new AtomicInteger() );
        }
        if ( System.currentTimeMillis() - connector.getLastPacketCheck() <= 1000 )
        {
            if ( packets.incrementAndGet() >= 55 )
            {
                connector.getConnection().disconnect( Config.getConfig().getErrorPackets() );
                connector.setState( CheckState.FAILED );
                return true;
            }
            return false;
        }
        packets.set( 0 );
        connector.setLastPacketCheck( System.currentTimeMillis() );
        return false;
    }

    public static boolean canUseButton(Location playerLoc, Location blockLoc)
    {
        Vector toBlock = blockLoc.toVector().subtract( playerLoc.clone().add( 0, 1.62, 0, 0, 0 ).toVector() );
        Vector direction = playerLoc.getDirection();
        double distance = playerLoc.distance( blockLoc );
        double dot = toBlock.normalize().dot( direction );
        return ( 0.35 < dot && dot <= 1 ) && ( distance <= 4.95 && distance > 1.5 ) && playerLoc.isOnGround() && playerLoc.getY() == blockLoc.getY() - 1;
    }

    public static enum CheckState
    {
        POSITION,
        BUTTON,
        SUS,
        FAILED;
    }
}