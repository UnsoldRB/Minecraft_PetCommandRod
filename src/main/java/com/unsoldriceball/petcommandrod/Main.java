package com.unsoldriceball.petcommandrod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.unsoldriceball.petcommandrod.Utils.getEntityLoc;
import static com.unsoldriceball.petcommandrod.Utils.randomVec;




@Mod(modid = Main.MOD_ID, acceptableRemoteVersions = "*")
public class Main
{
    public static final String PARENT_MOD_ID = "petsretreat";
    public static final String MOD_ID = "petcommandrod";
    public static final String POTION_ID = "petcommandrodpotion";
    public static final int AFFECT_RADIUS_SWING = 64;
    public static final int AFFECT_RADIUS_USE = 16;
    public static final int ROD_USE_INTERVAL = 10;
    public static final SoundEvent SOUND_TELEPORT = SoundEvents.ENTITY_ENDERDRAGON_FLAP;
    public static final SoundEvent SOUND_USE = SoundEvents.ENTITY_BAT_LOOP;
    public static final SoundEvent SOUND_UNUSE = SoundEvents.BLOCK_FIRE_EXTINGUISH;
    public static final Enchantment USING_SIGN = Enchantments.BINDING_CURSE;
    private static final EnumParticleTypes PARTICLE = EnumParticleTypes.CLOUD;
    //Particle再生
    private static final int PARTICLE_COUNT = 2;

    public static RegisterRodItem f_rod;
    public static Potion f_rod_potion;
    public static SimpleNetworkWrapper f_wrapper_net;




    //ModがInitializeを呼び出す前に発生するイベント。
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        //これで指定したクラス内でForgeのイベントが動作するようになるらしい。
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RodUseEvent());
        MinecraftForge.EVENT_BUS.register(new RodEffect());

        //パケットを使えるようにしておく。
        f_wrapper_net = NetworkRegistry.INSTANCE.newSimpleChannel("petcommandrod_packet");
        f_wrapper_net.registerMessage(Packet.Handler.class, Packet.class, 0, Side.SERVER);

        //アイテムを登録
        f_rod = new RegisterRodItem();
        ForgeRegistries.ITEMS.register(f_rod);

        final Ingredient L_R_AIR = Ingredient.fromItem(Items.AIR);
        final Ingredient L_R_STICK = Ingredient.fromItem(Items.STICK);
        final Ingredient L_R_IRON = Ingredient.fromStacks(new ItemStack(Items.IRON_INGOT));

        //アイテムのレシピを登録
        NonNullList<Ingredient> recipe_input = NonNullList.create();
        recipe_input.add(L_R_AIR);        recipe_input.add(L_R_IRON);        recipe_input.add(L_R_STICK);
        recipe_input.add(L_R_IRON);       recipe_input.add(L_R_STICK);        recipe_input.add(L_R_IRON);
        recipe_input.add(L_R_STICK);        recipe_input.add(L_R_IRON);        recipe_input.add(L_R_AIR);

        final IRecipe L_RECIPE = new ShapedRecipes(MOD_ID, 3, 3, recipe_input, new ItemStack(f_rod));

        L_RECIPE.setRegistryName(new ResourceLocation(MOD_ID, RegisterRodItem.ROD_ID));
        ForgeRegistries.RECIPES.register(L_RECIPE);

        //攻撃をやめさせ続けるためのポーション効果を登録。
        f_rod_potion = new RegisterRodPotion(POTION_ID, true, 6580840, 0, 0);
        ForgeRegistries.POTIONS.register(f_rod_potion);
    }



    //アイテムのモデル登録用イベント。
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void registerModels(ModelRegistryEvent event)
    {
        ModelLoader.setCustomModelResourceLocation(f_rod, 0, new ModelResourceLocation(new ResourceLocation(MOD_ID, RegisterRodItem.ROD_ID), "inventory"));
    }




    //Entityがrod_potion効果を持った状態で攻撃対象を定めた場合に、攻撃対象をnullにする関数。
    @SubscribeEvent
    public static void onLivingSetAttackTargetEvent(LivingSetAttackTargetEvent event)
    {
        if (event.getEntity().getEntityWorld().isRemote) return;
        if (!(event.getEntity() instanceof EntityLiving)) return;
        if (!event.getEntityLiving().isPotionActive(f_rod_potion)) return;
        if (event.getTarget() == null) return;

        ((EntityLiving)event.getEntityLiving()).setAttackTarget(null);
    }



    //撤退のトーテムが適用されているmobを、指定された半径で検索する。
    public static List<EntityLivingBase> searchEntities(EntityPlayer p, int radius)
    {
        final UUID L_OWNER_UUID = p.getUniqueID();
        final World L_WORLD = p.world;
        final BlockPos L_CENTER = p.getPosition();
        List<EntityLivingBase> result = new ArrayList<>();


        //プレイヤーを中心として、radius内のEntityLivingBase全てを取得する。
        final List<EntityLivingBase> L_TARGETs = L_WORLD.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(
                L_CENTER.getX() - radius, L_CENTER.getY() - radius, L_CENTER.getZ() - radius,
                L_CENTER.getX() + radius, L_CENTER.getY() + radius, L_CENTER.getZ() + radius
        ));

        //L_TARGEtS内のトーテムパワーが付与された、pのmob(ペット)を取得する。
        for (EntityLivingBase _e: L_TARGETs)
        {
            UUID _uuid = hasTotemPower(_e);

            if (_uuid == null) continue;
            if (!_uuid.equals(L_OWNER_UUID)) continue;

            result.add(_e);
        }
        return result;
    }



    //引数に指定したEntityに、撤退のトーテムを適用されていたら、適用主のUUIDを返す関数。
    private static UUID hasTotemPower(EntityLivingBase e)
    {
        for(String _t : e.getTags())
        {
            if (!_t.contains("@" + PARENT_MOD_ID)) continue;
            return UUID.fromString(_t.replace("@" + PARENT_MOD_ID + "_", ""));
        }
        return null;
    }



    //Particle生成関数
    public static void initPlayParticle (EntityPlayer player, Vec3d loc)
    {
        //わざわざPacketを使うのも面倒なのでサーバーからChatを送信してRPCを再現する。(ServerOnly)
        //座標に関しては、サーバーとクライアント間でズレがあるので、プレイヤーを原点とした差を渡す。
        if (!player.getEntityWorld().isRemote)
        {

            final int L_DIM = player.dimension;
            final Vec3d L_PLOC = getEntityLoc(player);

            String message = "@" + MOD_ID;
            message += "_" + player.getUniqueID();
            message += "_" + (L_PLOC.x - loc.x);
            message += "_" + (L_PLOC.y - loc.y);
            message += "_" + (L_PLOC.z - loc.z);

            //dimentionが一致する全PlayerにChatmessageを送信する。
            final PlayerList L_PLAYERLIST = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();

            for (EntityPlayerMP _p : L_PLAYERLIST.getPlayers())
            {
                if (_p.dimension == L_DIM)
                {
                    _p.sendMessage(new TextComponentString(message));
                }
            }
        }
    }



    //チャットを受け取った時のイベント
    @SubscribeEvent
    public void playParticle(ClientChatReceivedEvent event)
    {
        //Chat内容に"@MOD_ID"を含む場合(ClientOnly)
        final String L_MSG = event.getMessage().getUnformattedText();

        if (L_MSG.contains("@" + MOD_ID))
        {
            event.setCanceled(true);

            //Chatmessageを分解して、　定数を定義する。
            final List<String> L_SPLIT = Arrays.asList(L_MSG.split("_"));

            final World L_WORLD = Minecraft.getMinecraft().world;
            final EntityPlayer L_PLAYER = L_WORLD.getPlayerEntityByUUID(UUID.fromString(L_SPLIT.get(1)));
            assert L_PLAYER != null;
            final Vec3d L_PLOC = getEntityLoc(L_PLAYER);
            final Vec3d L_LOC = new Vec3d(
                    L_PLOC.x - Double.parseDouble(L_SPLIT.get(2)),
                    L_PLOC.y - Double.parseDouble(L_SPLIT.get(3)),
                    L_PLOC.z - Double.parseDouble(L_SPLIT.get(4)));
            //---

            for (int i = 0; i < PARTICLE_COUNT; i++)
            {
                L_WORLD.spawnParticle(PARTICLE, L_LOC.x, L_LOC.y + 0.5, L_LOC.z, randomVec(i - 1), (Math.abs(randomVec(i))), randomVec(i + 1));
            }
        }
    }



    //RodPotion効果を付与する関数。
    public static void applyRodPotion(EntityLivingBase e, int duration)
    {
        if (e instanceof EntityLiving)
        {
            final EntityLiving L_TARGET = (EntityLiving) e;
            L_TARGET.addPotionEffect(new PotionEffect(f_rod_potion, duration));
            L_TARGET.setAttackTarget(null);
        }
    }
}