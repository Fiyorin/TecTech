package com.github.technus.tectech.thing.cover;

import static com.github.technus.tectech.mechanics.enderStorage.EnderWorldSavedData.getEnderFluidContainer;
import static com.github.technus.tectech.mechanics.enderStorage.EnderWorldSavedData.getEnderLinkTag;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.github.technus.tectech.mechanics.enderStorage.EnderLinkTag;
import com.github.technus.tectech.mechanics.enderStorage.EnderWorldSavedData;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

import eu.usrv.yamcore.auxiliary.PlayerChatHelper;
import gregtech.api.gui.modularui.GT_CoverUIBuildContext;
import gregtech.api.gui.modularui.GT_UITextures;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.ISerializableObject;
import gregtech.common.gui.modularui.widget.CoverDataControllerWidget;
import gregtech.common.gui.modularui.widget.CoverDataFollower_ToggleButtonWidget;

public class GT_Cover_TM_EnderFluidLink extends GT_CoverBehavior {

    private static final int L_PER_TICK = 8000;
    private static final int IMPORT_EXPORT_MASK = 0b0001;
    private static final int PUBLIC_PRIVATE_MASK = 0b0010;

    public GT_Cover_TM_EnderFluidLink() {}

    private void transferFluid(IFluidHandler source, ForgeDirection side, IFluidHandler target, ForgeDirection tSide,
            int amount) {
        FluidStack fluidStack = source.drain(side, amount, false);

        if (fluidStack != null) {
            int fluidTransferred = target.fill(tSide, fluidStack, true);
            source.drain(side, fluidTransferred, true);
        }
    }

    private boolean testBit(int aCoverVariable, int bitMask) {
        return (aCoverVariable & bitMask) != 0;
    }

    private int toggleBit(int aCoverVariable, int bitMask) {
        return (aCoverVariable ^ bitMask);
    }

    @Override
    public int doCoverThings(ForgeDirection side, byte aInputRedstone, int aCoverID, int aCoverVariable,
            ICoverable aTileEntity, long aTimer) {
        if ((aTileEntity instanceof IFluidHandler)) {
            IFluidHandler fluidHandlerSelf = (IFluidHandler) aTileEntity;
            IFluidHandler fluidHandlerEnder = getEnderFluidContainer(getEnderLinkTag((IFluidHandler) aTileEntity));

            if (testBit(aCoverVariable, IMPORT_EXPORT_MASK)) {
                transferFluid(fluidHandlerEnder, ForgeDirection.UNKNOWN, fluidHandlerSelf, side, L_PER_TICK);
            } else {
                transferFluid(fluidHandlerSelf, side, fluidHandlerEnder, ForgeDirection.UNKNOWN, L_PER_TICK);
            }
        }
        return aCoverVariable;
    }

    @Override
    public String getDescription(ForgeDirection side, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        return "";
    }

    @Override
    public boolean letsFluidIn(ForgeDirection side, int aCoverID, int aCoverVariable, Fluid aFluid,
            ICoverable aTileEntity) {
        return true;
    }

    @Override
    public boolean letsFluidOut(ForgeDirection side, int aCoverID, int aCoverVariable, Fluid aFluid,
            ICoverable aTileEntity) {
        return true;
    }

    @Override
    public int onCoverScrewdriverclick(ForgeDirection side, int aCoverID, int aCoverVariable, ICoverable aTileEntity,
            EntityPlayer aPlayer, float aX, float aY, float aZ) {
        int newCoverVariable = toggleBit(aCoverVariable, IMPORT_EXPORT_MASK);

        if (testBit(aCoverVariable, IMPORT_EXPORT_MASK)) {
            PlayerChatHelper.SendInfo(aPlayer, "Ender Suction Engaged!"); // TODO Translation support
        } else {
            PlayerChatHelper.SendInfo(aPlayer, "Ender Filling Engaged!");
        }
        return newCoverVariable;
    }

    @Override
    public int getTickRate(ForgeDirection side, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        // Runs each tick
        return 1;
    }

    // region GUI

    @Override
    public boolean hasCoverGUI() {
        return true;
    }

    @Override
    public boolean useModularUI() {
        return true;
    }

    @Override
    public ModularWindow createWindow(GT_CoverUIBuildContext buildContext) {
        // Only open gui if we're placed on a fluid tank
        if (buildContext.getTile() instanceof IFluidHandler) {
            return new EnderFluidLinkUIFactory(buildContext).createWindow();
        }
        return null;
    }

    private class EnderFluidLinkUIFactory extends UIFactory {

        private static final int START_X = 10;
        private static final int START_Y = 25;
        private static final int SPACE_X = 18;
        private static final int SPACE_Y = 18;
        private static final int PUBLIC_BUTTON_ID = 0;
        private static final int PRIVATE_BUTTON_ID = 1;
        private static final int IMPORT_BUTTON_ID = 2;
        private static final int EXPORT_BUTTON_ID = 3;

        public EnderFluidLinkUIFactory(GT_CoverUIBuildContext buildContext) {
            super(buildContext);
        }

        @SuppressWarnings("PointlessArithmeticExpression")
        @Override
        protected void addUIWidgets(ModularWindow.Builder builder) {
            TextFieldWidget frequencyField = new TextFieldWidget();
            builder.widget(frequencyField.setGetter(() -> {
                ICoverable te = getUIBuildContext().getTile();
                if (!frequencyField.isClient() && te instanceof IFluidHandler) {
                    return EnderWorldSavedData.getEnderLinkTag((IFluidHandler) te).getFrequency();
                }
                return "";
            }).setSetter(val -> {
                ICoverable te = getUIBuildContext().getTile();
                if (!frequencyField.isClient() && te instanceof IFluidHandler) {
                    UUID uuid;
                    if (testBit(convert(getCoverData()), PUBLIC_PRIVATE_MASK)) {
                        uuid = getUUID();
                        if (!(te instanceof IGregTechTileEntity)) return;
                        if (!uuid.equals(((IGregTechTileEntity) te).getOwnerUuid())) return;
                    } else {
                        uuid = null;
                    }
                    EnderWorldSavedData.bindEnderLinkTag((IFluidHandler) te, new EnderLinkTag(val, uuid));
                }
            }).setTextColor(Color.WHITE.dark(1)).setTextAlignment(Alignment.CenterLeft).setFocusOnGuiOpen(true)
                    .setBackground(GT_UITextures.BACKGROUND_TEXT_FIELD.withOffset(-1, -1, 2, 2))
                    .setPos(START_X + SPACE_X * 0, START_Y + SPACE_Y * 0).setSize(SPACE_X * 5 - 8, 12))
                    .widget(
                            new CoverDataControllerWidget.CoverDataIndexedControllerWidget_ToggleButtons<>(
                                    this::getCoverData,
                                    this::setCoverData,
                                    GT_Cover_TM_EnderFluidLink.this,
                                    (id, coverData) -> !getClickable(id, convert(coverData)),
                                    (id, coverData) -> new ISerializableObject.LegacyCoverData(
                                            getNewCoverVariable(id, convert(coverData)))).addToggleButton(
                                                    PUBLIC_BUTTON_ID,
                                                    CoverDataFollower_ToggleButtonWidget.ofDisableable(),
                                                    widget -> widget
                                                            .setStaticTexture(GT_UITextures.OVERLAY_BUTTON_WHITELIST)
                                                            .addTooltip(GT_Utility.trans("326", "Public"))
                                                            .setPos(START_X + SPACE_X * 0, START_Y + SPACE_Y * 2))
                                                    .addToggleButton(
                                                            PRIVATE_BUTTON_ID,
                                                            CoverDataFollower_ToggleButtonWidget.ofDisableable(),
                                                            widget -> widget
                                                                    .setStaticTexture(
                                                                            GT_UITextures.OVERLAY_BUTTON_BLACKLIST)
                                                                    .addTooltip(GT_Utility.trans("327", "Private"))
                                                                    .setPos(
                                                                            START_X + SPACE_X * 1,
                                                                            START_Y + SPACE_Y * 2))
                                                    .addToggleButton(
                                                            IMPORT_BUTTON_ID,
                                                            CoverDataFollower_ToggleButtonWidget.ofDisableable(),
                                                            widget -> widget
                                                                    .setStaticTexture(
                                                                            GT_UITextures.OVERLAY_BUTTON_IMPORT)
                                                                    .addTooltip(GT_Utility.trans("007", "Import"))
                                                                    .setPos(
                                                                            START_X + SPACE_X * 0,
                                                                            START_Y + SPACE_Y * 3))
                                                    .addToggleButton(
                                                            EXPORT_BUTTON_ID,
                                                            CoverDataFollower_ToggleButtonWidget.ofDisableable(),
                                                            widget -> widget
                                                                    .setStaticTexture(
                                                                            GT_UITextures.OVERLAY_BUTTON_EXPORT)
                                                                    .addTooltip(GT_Utility.trans("006", "Export"))
                                                                    .setPos(
                                                                            START_X + SPACE_X * 1,
                                                                            START_Y + SPACE_Y * 3)))
                    .widget(
                            new TextWidget(GT_Utility.trans("328", "Channel")).setDefaultColor(COLOR_TEXT_GRAY.get())
                                    .setPos(START_X + SPACE_X * 5, 4 + START_Y + SPACE_Y * 0))
                    .widget(
                            new TextWidget(GT_Utility.trans("329", "Public/Private"))
                                    .setDefaultColor(COLOR_TEXT_GRAY.get())
                                    .setPos(START_X + SPACE_X * 2, 4 + START_Y + SPACE_Y * 2))
                    .widget(
                            new TextWidget(GT_Utility.trans("229", "Import/Export"))
                                    .setDefaultColor(COLOR_TEXT_GRAY.get())
                                    .setPos(START_X + SPACE_X * 2, 4 + START_Y + SPACE_Y * 3));
        }

        private int getNewCoverVariable(int id, int coverVariable) {
            switch (id) {
                case PUBLIC_BUTTON_ID:
                case PRIVATE_BUTTON_ID:
                    return toggleBit(coverVariable, PUBLIC_PRIVATE_MASK);
                case IMPORT_BUTTON_ID:
                case EXPORT_BUTTON_ID:
                    return toggleBit(coverVariable, IMPORT_EXPORT_MASK);
            }
            return coverVariable;
        }

        private boolean getClickable(int id, int coverVariable) {
            switch (id) {
                case PUBLIC_BUTTON_ID:
                    return testBit(coverVariable, PUBLIC_PRIVATE_MASK);
                case PRIVATE_BUTTON_ID:
                    return !testBit(coverVariable, PUBLIC_PRIVATE_MASK);
                case IMPORT_BUTTON_ID:
                    return testBit(coverVariable, IMPORT_EXPORT_MASK);
                case EXPORT_BUTTON_ID:
                    return !testBit(coverVariable, IMPORT_EXPORT_MASK);
            }
            return false;
        }

        private UUID getUUID() {
            return getUIBuildContext().getPlayer().getUniqueID();
        }
    }
}
