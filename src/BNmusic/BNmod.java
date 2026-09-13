package BNmusic;

import arc.Core;
import arc.Events;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
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
    private static boolean dragging=false;
    private static boolean pressed=false;
    private static float touchStartX;
    private static float touchStartY;
    private static float startButtonX;
    private static float startButtonY;
    private static final float dragThreshold=12f;
    public BNmod(){}
    public static String name(String add){
        return ModName+"-"+add;
    }
    @Override
    public void loadContent(){
        mod=Vars.mods.getMod(this.getClass());
        MusicPlayer.load();
        Events.on(ClientLoadEvent.class,e->Core.app.post(BNmod::addButton));
        Events.run(Trigger.update,BNmod::updateButton);
    }
    private static void addButton(){
        if(Core.scene==null)return;
        if(buttonTable!=null&&buttonTable.parent!=null)return;
        buttonTable=new Table();
        buttonTable.setTransform(true);
        buttonTable.setSize(120f,55f);
        buttonTable.setPosition(buttonX,buttonY);
        musicButton=new TextButton("BNmusic",Styles.flatt);
        musicButton.setSize(120f,55f);
        musicButton.clicked(MusicPlayer::toggleDialog);
        buttonTable.add(musicButton).size(120f,55f);
        Core.scene.add(buttonTable);
    }
    private static void updateButton(){
        if(buttonTable==null||buttonTable.parent==null)return;
        float inputX=Core.input.mouseX();
        float inputY=Core.graphics.getHeight()-Core.input.mouseY();
        boolean down=Core.input.isTouched();
        if(down&&!pressed){
            float localX=inputX-buttonTable.x;
            float localY=inputY-buttonTable.y;
            if(localX>=0f&&localX<=buttonTable.getWidth()&&localY>=0f&&localY<=buttonTable.getHeight()){
                pressed=true;
                dragging=false;
                touchStartX=inputX;
                touchStartY=inputY;
                startButtonX=buttonTable.x;
                startButtonY=buttonTable.y;
            }
        }
        if(down&&pressed){
            float dx=inputX-touchStartX;
            float dy=inputY-touchStartY;
            if(!dragging&&(Math.abs(dx)>=dragThreshold||Math.abs(dy)>=dragThreshold)){
                dragging=true;
            }
            if(dragging){
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
        }
        if(!down&&pressed){
            pressed=false;
            if(dragging){
                dragging=false;
            }
        }
    }
}