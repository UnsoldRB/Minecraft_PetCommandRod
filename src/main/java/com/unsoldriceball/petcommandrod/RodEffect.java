package com.unsoldriceball.petcommandrod;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

import static com.unsoldriceball.petcommandrod.Main.*;
import static com.unsoldriceball.petcommandrod.Utils.getEntityLoc;
import static com.unsoldriceball.petcommandrod.Utils.randomVec;


public class RodEffect
{
    private static List<EntityPlayer> rod_using_players = new ArrayList<>();
    private int tick_count;




    //ロッドを左クリックしたときの効果
    public static void rodSwing(EntityPlayer p)
    {
        p.world.playSound(null, p.posX, p.posY, p.posZ, SOUND_TELEPORT, SoundCategory.PLAYERS, 0.8f, 1.35f);

        final List<EntityLivingBase> L_TARGETS = searchEntities(p, AFFECT_RADIUS_SWING);

        for (EntityLivingBase _e : L_TARGETS)
        {
            teleportEntity(_e, p);
            applyRodPotion(_e, 5);
        }
    }



    //ロッドを右クリックしたときの効果
    public static void rodUse(EntityPlayer p)
    {
        final ItemStack L_ITEM = p.getHeldItemMainhand();

        if (L_ITEM.getItem() instanceof RegisterRodItem)
        {
            if (p.isPotionActive(f_rod_potion))
            {
                switchRodState(p, false);
            }
            else
            {
                switchRodState(p, true);
            }
        }
    }



    //ロッドの右クリック効果が発動中の効果
    private static void rodUseEffect(EntityPlayer p)
    {
        //本当は2回検索せずに1回の検索と距離の比較で実装したかったけど、
        //getDistance()の仕組みがよくわかんなくてあきらめた。
        final int L_MARGINE_TARGETS_RANGE = 16;
        final List<EntityLivingBase> L_IN_TARGETS = searchEntities(p, AFFECT_RADIUS_USE);
        final List<EntityLivingBase> L_OUT_TARGETS = searchEntities(p, AFFECT_RADIUS_USE + L_MARGINE_TARGETS_RANGE);

        for (EntityLivingBase _e : L_OUT_TARGETS) {
            applyRodPotion(_e, ROD_USE_INTERVAL);
            //取得したペットが、効果範囲外にいた場合は自身へテレポートさせる。
            if (!L_IN_TARGETS.contains(_e)) {
                teleportEntity(_e, p);
            }
        }
    }




    //テレポート処理を行う関数。
    private static void teleportEntity(EntityLivingBase e, EntityPlayer p)
    {
        final Vec3d L_LOC_TARGET_POS = p.getPositionVector();
        final Vec3d L_LOC_TARGET_ENTITY = getEntityLoc(e);

        p.world.playSound(null, L_LOC_TARGET_ENTITY.x, L_LOC_TARGET_ENTITY.y, L_LOC_TARGET_ENTITY.z, SOUND_TELEPORT, SoundCategory.HOSTILE, 0.2f, 1.78f);
        initPlayParticle(p, L_LOC_TARGET_ENTITY);
        //落下高度をリセット
        e.fallDistance = 0.0f;

        e.setPosition(L_LOC_TARGET_POS.x, L_LOC_TARGET_POS.y, L_LOC_TARGET_POS.z);
        e.motionX = randomVec(1);
        e.motionY = 0.15;
        e.motionZ = randomVec(2);
    }



    //プレイヤーのRodの発動状態を切り替える関数
    private static void switchRodState(EntityPlayer p, boolean activate)
    {
        if (activate)
        {
            p.addPotionEffect(new PotionEffect(f_rod_potion, 999999999));
            p.world.playSound(null, p.posX, p.posY, p.posZ, SOUND_USE, SoundCategory.PLAYERS, 0.8f, 1.85f);
        }
        else
        {
            p.removePotionEffect(f_rod_potion);
            p.world.playSound(null, p.posX, p.posY, p.posZ, SOUND_UNUSE, SoundCategory.PLAYERS, 0.8f, 1.35f);
        }
    }




    //ポーション効果が付与されたときのイベント。
    //ServerTickイベントでループする対象のプレイヤーを入れたり消したりする。
    @SubscribeEvent
    public void onPotion(PotionEvent event)
    {
        if (event.getEntity().world.isRemote) return;
        if (event.getPotionEffect().getPotion() != f_rod_potion) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        final EntityPlayer L_PLAYER = (EntityPlayer) event.getEntityLiving();

        if (event.getClass() == PotionEvent.PotionAddedEvent.class)
        {
            rod_using_players.add(L_PLAYER);
        }
        else if (event.getClass() == PotionEvent.PotionExpiryEvent.class || event.getClass() == PotionEvent.PotionRemoveEvent.class)
        {
            rod_using_players.remove(L_PLAYER);
        }
    }



    //ROD_USE_INTERVALtickごとにrodUse()を実行する。
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            tick_count += 1;

            if (tick_count >= ROD_USE_INTERVAL)
            {
                tick_count = 0;

                if (rod_using_players.size() > 0)
                {
                    for (EntityPlayer _p : rod_using_players)
                    {
                        rodUseEffect(_p);
                    }
                }
            }
        }
    }

}
