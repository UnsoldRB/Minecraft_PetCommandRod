package com.unsoldriceball.petcommandrod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import static com.unsoldriceball.petcommandrod.Main.MOD_ID;
import static com.unsoldriceball.petcommandrod.Main.POTION_ID;


public class RegisterRodPotion extends Potion
{
    final ResourceLocation ICON = new ResourceLocation(MOD_ID, "textures/gui/" + POTION_ID + ".png");




    protected RegisterRodPotion(String name, boolean isBadEffectIn, int liquidColorIn, int iconIndexX, int iconIndexY)
    {
        super(isBadEffectIn, liquidColorIn);

        //ただポーションを登録するだけ
        setPotionName("effect." + name);
        setIconIndex(iconIndexX, iconIndexY);
        setRegistryName(new ResourceLocation(MOD_ID + ":" + name));
    }



    @Override
    public boolean hasStatusIcon()
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICON);
        return true;
    }



    @Override
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc)
    {
        mc.renderEngine.bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
    }



    @Override
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha)
    {
        mc.renderEngine.bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }
}
