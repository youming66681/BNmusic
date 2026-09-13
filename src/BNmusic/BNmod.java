package BNmusic;
import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.ui.Styles;
import BNmusic.content.MapBuildingClipboard;
public class BNmod extends Mod{
    public static Mods.LoadedMod ML;
    public static final String ModName = "BNmod";
    public static Mods.LoadedMod mod;
    private static boolean buttonAdded = false;
    private static Table buttonTable;
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
            addButton();
        });
        Events.run(Trigger.update, () -> {
            if(!buttonAdded){
                addButton();
            }
            if(buttonTable != null){
                buttonTable.visible = Vars.state.isGame();
            }
        });
        Log.info("[BNmod] 建筑蓝图系统已加载");
    }
    private static void addButton(){
        if(buttonAdded) return;
        if(Core.scene == null) return;
        if(Vars.ui == null) return;
        try{
            buttonTable = new Table();
            buttonTable.setFillParent(true);
            buttonTable.top().right();
            buttonTable.margin(90f, 12f, 12f, 12f);
            ImageButton button = new ImageButton(
                    Icon.save,
                    Styles.clearNonei
            );
            button.resizeImage(26f);
            button.setColor(Color.white);
            button.clicked(() -> {
                if(!Vars.state.isGame()){
                    return;
                }
                MapBuildingClipboard.showDialog();
            });
            buttonTable.add(button).size(54f);
            Core.scene.add(buttonTable);
            buttonAdded = true;
            Log.info("[BNmod] 移动端建筑蓝图按钮创建成功");
        }catch(Throwable e){
            Log.err("[BNmod] 创建建筑蓝图按钮失败", e);
        }
    }
}