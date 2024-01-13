package com.unsoldriceball.petcommandrod;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jline.utils.Log;

import static com.unsoldriceball.petcommandrod.Main.f_rod;
import static com.unsoldriceball.petcommandrod.Main.f_wrapper_net;
import static com.unsoldriceball.petcommandrod.RodEffect.rodSwing;
import static com.unsoldriceball.petcommandrod.RodEffect.rodUse;


public class RodUseEvent
{
    //パケットが不要なイベントにおいて、Rodの使用条件が揃っているかを返す関数
    private static boolean canUseRod(EntityPlayer p)
    {
        return (!p.world.isRemote && p.getHeldItemMainhand().getItem() == f_rod);
    }



    //以下、アイテムに対する操作を検知する関数。
    @SubscribeEvent
    public void onPlayerInteract1(PlayerInteractEvent.RightClickItem event)
    {
        if (canUseRod(event.getEntityPlayer()))
        {
            rodUse(event.getEntityPlayer());
        }
    }



    @SubscribeEvent
    public void onPlayerInteract2(PlayerInteractEvent.LeftClickBlock event)
    {
        if (canUseRod(event.getEntityPlayer()))
        {
            rodSwing(event.getEntityPlayer());
        }
    }



    @SubscribeEvent
    public void onPlayerInteract3(PlayerInteractEvent.LeftClickEmpty event)
    {
        if (!event.getWorld().isRemote) return;
        if (event.getEntityPlayer().getHeldItemMainhand().getItem() != f_rod) return;

        final EntityPlayer L_PLAYER = event.getEntityPlayer();
        f_wrapper_net.sendToServer(new Packet(L_PLAYER.getUniqueID(), L_PLAYER.dimension));
    }



    @SubscribeEvent
    public void onPlayerInteract4(AttackEntityEvent event)
    {
        if (canUseRod(event.getEntityPlayer()))
        {
            rodSwing(event.getEntityPlayer());
        }
    }
}
