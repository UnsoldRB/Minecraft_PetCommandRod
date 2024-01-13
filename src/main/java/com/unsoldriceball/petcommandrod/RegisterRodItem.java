package com.unsoldriceball.petcommandrod;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

import static com.unsoldriceball.petcommandrod.Main.MOD_ID;




public class RegisterRodItem extends Item
{
    public final static String ROD_ID = "pet_command_rod";
    public final static String ROD_TOOLTIP = "A rod to command pets bound with the totem power.\n\nSwing: Calls pets within a 64-block radius.\nUse: Keeps pets within an 16-block radius.";



    //コンストラクタ
    public RegisterRodItem()
    {
        super();
        this.setRegistryName(MOD_ID, ROD_ID);
        this.setUnlocalizedName(ROD_ID);
        this.setCreativeTab(CreativeTabs.TOOLS);
        this.setMaxStackSize(1);
    }



    //Tooltipを登録
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(ROD_TOOLTIP);
    }
}
