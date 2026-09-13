package BNmusic;
import arc.Core;
import arc.Events;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.ui.Styles;
import mindustry.gen.Icon;
import BNmusic.content.MapBuildingClipboard;
public class BNmod extends Mod{
    public static Mods.LoadedMod ML;
    public static final String ModName = "BNmod";
    public static Mods.LoadedMod mod;
    private static boolean buttonAdded = false;
    public BNmod(){
    }
    public static String name(String add){
        return ModName + "-" + add;
    }
    @Override
    public void loadContent(){
        mod = Vars.mods.getMod(this.getClass());
    }
    @Override
    public void init(){
        Vars.maxSchematicSize = 32768;
        Events.on(ClientLoadEvent.class, event -> {
            addMobileButton();
        });
        Events.run(Trigger.update, () -> {
            if(!buttonAdded && Vars.ui != null && Vars.ui.hudfrag != null){
                addMobileButton();
            }
        });
        Log.info("[BNmod] BNmod 建筑蓝图系统已加载");
    }
    private static void addMobileButton(){
        if(buttonAdded) return;
        if(Vars.ui == null) return;
        if(Vars.ui.hudfrag == null) return;
        Table hud = Vars.ui.hudfrag;
        try{
            Table table = findButtonTable(hud);
            if(table == null){
                Log.warn("[BNmod] 找不到 HUD 按钮区域");
                return;
            }
            ImageButton button = new ImageButton(Icon.save, Styles.clearNonei);
            button.clicked(() -> {
                MapBuildingClipboard.showDialog();
            });
            button.resizeImage(24f);
            button.setName("bnmod-building-clipboard");
            table.add(button).size(50f).pad(2f);
            buttonAdded = true;
            Log.info("[BNmod] 已添加移动端建筑蓝图按钮");
        }catch(Throwable e){
            Log.err("[BNmod] 添加建筑蓝图按钮失败", e);
        }
    }
    private static Table findButtonTable(Table root){
        if(root == null) return null;
        Table result = null;
        try{
            root.getChildren().each(child -> {
            });
        }catch(Throwable ignored){
        }
        return root;
    }
}