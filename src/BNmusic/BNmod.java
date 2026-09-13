package BNmusic;

import arc.Core;
import arc.Events;
import arc.scene.event.InputEvent;
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
    private static boolean dragging=false;
    private static float touchStartX;
    private static float touchStartY;
    private static float startButtonX;
    private static float startButtonY;
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

        Events.on(ClientLoadEvent.class,e->{
            Core.app.post(BNmod::addButton);
        });

        Events.run(mindustry.game.EventType.Trigger.update,BNmod::updateButton);
    }

    private static void addButton(){
        if(Core.scene==null)return;
        if(buttonTable!=null&&buttonTable.parent!=null)return;

        buttonTable=new Table();
        buttonTable.setTransform(true);

        musicButton=new TextButton("BNmusic",Styles.flatt);
        musicButton.setSize(120f,55f);

        buttonTable.add(musicButton).size(120f,55f);

        Core.scene.add(buttonTable);

        buttonTable.pack();

        float maxX=Math.max(0f,Core.graphics.getWidth()-buttonTable.getWidth());
        float maxY=Math.max(0f,Core.graphics.getHeight()-buttonTable.getHeight());

        buttonX=Math.min(buttonX,maxX);
        buttonY=Math.min(buttonY,maxY);

        buttonTable.setPosition(buttonX,buttonY);

        musicButton.clicked(()->{
            if(!dragging){
                MusicPlayer.showDialog();
            }
        });

        buttonTable.addListener(new InputListener(){

            @Override
            public boolean touchDown(InputEvent event,float x,float y,int pointer,int button){
                if(pointer!=0)return false;

                dragging=false;

                touchStartX=x;
                touchStartY=y;

                startButtonX=buttonTable.x;
                startButtonY=buttonTable.y;

                return true;
            }

            @Override
            public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(pointer!=0)return;

                float dx=x-touchStartX;
                float dy=y-touchStartY;

                if(!dragging){
                    if(Math.abs(dx)>=dragThreshold||Math.abs(dy)>=dragThreshold){
                        dragging=true;
                    }
                }

                if(!dragging)return;

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
                if(pointer!=0)return;
            }
        });

        buttonTable.toFront();
    }

    private static void updateButton(){
        if(buttonTable==null)return;
        if(buttonTable.parent==null)return;

        if(!buttonTable.visible){
            buttonTable.visible=true;
        }

        if(!musicButton.visible){
            musicButton.visible=true;
        }

        buttonTable.toFront();
    }
}