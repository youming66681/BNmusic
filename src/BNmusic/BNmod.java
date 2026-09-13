package BNmusic;
import arc.Core;
import arc.Events;
import arc.scene.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.ui.Styles;
import BNmusic.content.MusicPlayer;
public class BNmod extends Mod{
    public static Mods.LoadedMod ML;
    public static final String ModName="BNmod";
    public static Mods.LoadedMod mod;
    private static Table buttonTable;
    private static TextButton musicButton;
    private static float buttonX=100f;
    private static float buttonY=100f;
    private static float touchStartX;
    private static float touchStartY;
    private static float startButtonX;
    private static float startButtonY;
    private static boolean dragging=false;
    private static boolean pressed=false;
    private static int activePointer=-1;
    private static final float dragThreshold=12f;
    public BNmod(){
    }
    public static String name(String add){
        return ModName+"-"+add;
    }
    @Override
    public void loadContent(){
        mod=Vars.mods.getMod(this.getClass());
        MusicPlayer.load();
        Events.on(ClientLoadEvent.class,e->addButton());
    }
    private static void addButton(){
        if(Core.scene==null)return;
        if(buttonTable!=null&&buttonTable.parent!=null)return;
        buttonTable=new Table();
        buttonTable.setTransform(true);
        buttonTable.setPosition(buttonX,buttonY);
        musicButton=new TextButton("BNmusic",Styles.flatt);
        musicButton.setSize(120f,55f);
        buttonTable.add(musicButton).size(120f,55f);
        buttonTable.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event,float x,float y,int pointer,int button){
                if(activePointer!=-1)return false;
                if(button!=0&&pointer<0)return false;
                activePointer=pointer;
                pressed=true;
                dragging=false;
                touchStartX=x;
                touchStartY=y;
                startButtonX=buttonTable.x;
                startButtonY=buttonTable.y;
                return true;
            }
            @Override
            public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(pointer!=activePointer)return;
                float dx=x-touchStartX;
                float dy=y-touchStartY;
                if(!dragging){
                    if(Math.abs(dx)<dragThreshold&&Math.abs(dy)<dragThreshold)return;
                    dragging=true;
                }
                float nx=startButtonX+dx;
                float ny=startButtonY+dy;
                float maxX=Math.max(0f,Core.graphics.getWidth()-buttonTable.getWidth());
                float maxY=Math.max(0f,Core.graphics.getHeight()-buttonTable.getHeight());
                nx=Math.max(0f,Math.min(nx,maxX));
                ny=Math.max(0f,Math.min(ny,maxY));
                buttonTable.setPosition(nx,ny);
                buttonX=nx;
                buttonY=ny;
            }
            @Override
            public void touchUp(InputEvent event,float x,float y,int pointer,int button){
                if(pointer!=activePointer)return;
                activePointer=-1;
                pressed=false;
                if(!dragging){
                    MusicPlayer.showDialog();
                }
                dragging=false;
            }
        });
        Core.scene.add(buttonTable);
    }
}