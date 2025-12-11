package logisticspipes.network.guis.module.inpipe;

import logisticspipes.gui.factory.SupplierGuiFactory;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleActiveSupplier.PatternMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.utils.gui.DummyContainer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;

@Accessors(chain = true)
public class ActiveSupplierSlot extends ModuleCoordinatesGuiProvider {

    @Getter
    @Setter
    private boolean patternUpgrade;

    @Getter
    @Setter
    private int[] slotArray;

    @Getter
    @Setter
    private boolean isLimit;

    @Getter
    @Setter
    private int mode;

    public ActiveSupplierSlot(int id) {
        super(id);
    }

    @Override
    public void writeData(LPDataOutputStream data) throws IOException {
        super.writeData(data);
        data.writeBoolean(patternUpgrade);
        data.writeIntegerArray(slotArray);
        data.writeBoolean(isLimit);
        data.writeInt(mode);
    }

    @Override
    public void readData(LPDataInputStream data) throws IOException {
        super.readData(data);
        patternUpgrade = data.readBoolean();
        slotArray = data.readIntegerArray();
        isLimit = data.readBoolean();
        mode = data.readInt();
    }

    @Override
    public Object getClientGui(EntityPlayer player) {
        ModuleActiveSupplier module = this.getLogisticsModule(player.getEntityWorld(), ModuleActiveSupplier.class);
        if (module == null) {
            return null;
        }
        module.setLimited(isLimit);
        if (patternUpgrade) {
            module.setPatternMode(PatternMode.values()[mode]);
        } else {
            module.setSupplyMode(SupplyMode.values()[mode]);
        }
        return SupplierGuiFactory.INSTANCE.createClientGui(player, module, patternUpgrade, slotArray);
    }

    @Override
    public DummyContainer getContainer(EntityPlayer player) {
        ModuleActiveSupplier module = this.getLogisticsModule(player.getEntityWorld(), ModuleActiveSupplier.class);
        if (module == null) {
            return null;
        }
        return SupplierGuiFactory.INSTANCE.createContainer(player, module);
    }

    @Override
    public GuiProvider template() {
        return new ActiveSupplierSlot(getId());
    }
}
