package BNmusic.content;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.scene.ui.Slider;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.math.Mathf;

public class MusicPlayer{
    private static final String[] files={
            "game1","game2","game3","game4","game5","game6","game7","game8","game9",
            "game10","game11","game12","game13","game14","game15","game16","game17",
            "game18","game19","game20","game21","game22","game23","game24","game25",
            "game26","game27"
    };
    private static final String[] names={
            "音乐1","音乐2","音乐3","音乐4","音乐5","音乐6","音乐7","音乐8","音乐9",
            "音乐10","音乐11","音乐12","音乐13","音乐14","音乐15","音乐16","音乐17",
            "音乐18","音乐19","音乐20","音乐21","音乐22","音乐23","音乐24","音乐25",
            "音乐26","音乐27"
    };
    private static int currentIndex=0;
    private static float volume=0.7f;
    private static Music currentMusic;
    private static BaseDialog playerDialog;
    private static Slider progressSlider;
    private static Slider volumeSlider;
    private static boolean shuffle=false;
    private static boolean loopSingle=false;
    private static Table hudTable;
    private static TextButton playButton;
    private static TextButton shuffleButton;
    private static TextButton loopButton;
    private static boolean hudReady=false;
    public static void load(){
        Events.on(ClientLoadEvent.class,e->createHudButton());
        Events.on(GameOverEvent.class,e->stop());
        Events.on(WorldLoadEvent.class,e->stop());
        Events.run(Trigger.update,MusicPlayer::update);
        Log.info("[MusicPlayer] 音乐播放器已加载");
        Log.info("[MusicPlayer] 共 "+files.length+" 首音乐");
    }
    private static void createHudButton(){
        if(hudReady)return;
        if(Vars.ui==null||Vars.ui.hudGroup==null)return;
        try{
            TextButton button=new TextButton("♫ 音乐",Styles.defaultt);
            button.setSize(120f,50f);
            button.setPosition(15f,15f);
            final float[] downX={0f};
            final float[] downY={0f};
            final float[] startX={0f};
            final float[] startY={0f};
            final boolean[] moved={false};
            button.addListener(new InputListener(){
                public boolean touchDown(InputEvent event,float x,float y,int pointer,int buttonCode){
                    if(pointer!=0)return false;
                    downX[0]=x;
                    downY[0]=y;
                    startX[0]=button.x;
                    startY[0]=button.y;
                    moved[0]=false;
                    return true;
                }
                public void touchDragged(InputEvent event,float x,float y,int pointer){
                    if(pointer!=0)return;
                    float dx=x-downX[0];
                    float dy=y-downY[0];
                    if(Math.abs(dx)>8f||Math.abs(dy)>8f)moved[0]=true;
                    if(!moved[0])return;
                    float nx=startX[0]+dx;
                    float ny=startY[0]+dy;
                    nx=Mathf.clamp(nx,0f,Vars.ui.hudGroup.getWidth()-button.getWidth());
                    ny=Mathf.clamp(ny,0f,Vars.ui.hudGroup.getHeight()-button.getHeight());
                    button.setPosition(nx,ny);
                }
                public void touchUp(InputEvent event,float x,float y,int pointer,int buttonCode){
                    if(pointer!=0)return;
                    if(!moved[0]){
                        openPlayerUI();
                    }
                }
            });
            Vars.ui.hudGroup.addChild(button);
            hudReady=true;
            Log.info("[MusicPlayer] HUD音乐按钮创建成功");
        }catch(Throwable t){
            Log.err("[MusicPlayer] HUD音乐按钮创建失败");
            Log.err(t);
        }
    }
    private static void update(){
        if(!hudReady){
            createHudButton();
        }
        updateButtonTexts();
        if(currentMusic==null)return;
        try{
            if(progressSlider!=null){
                float length=currentMusic.getLength();
                float position=currentMusic.getPosition();
                if(length>0f){
                    progressSlider.setValue(position/length);
                }
            }
            if(!loopSingle){
                float length=currentMusic.getLength();
                float position=currentMusic.getPosition();
                if(length>0f&&position>=length-0.2f){
                    nextTrack();
                }
            }
        }catch(Throwable ignored){}
    }
    private static void updateButtonTexts(){
        if(playButton!=null){
            try{
                playButton.setText(isPlaying()?"暂停":"播放");
            }catch(Throwable ignored){}
        }
        if(shuffleButton!=null){
            try{
                shuffleButton.setText(shuffle?"随机：开":"随机：关");
            }catch(Throwable ignored){}
        }
        if(loopButton!=null){
            try{
                loopButton.setText(loopSingle?"单曲：开":"单曲：关");
            }catch(Throwable ignored){}
        }
    }
    private static boolean isPlaying(){
        if(currentMusic==null)return false;
        try{
            return currentMusic.isPlaying();
        }catch(Throwable ignored){
            return false;
        }
    }
    private static Music getMusic(String name){
        try{
            Music music=Core.assets.getOrNull("music/"+name+".ogg",Music.class);
            if(music!=null)return music;
        }catch(Throwable ignored){}
        try{
            Music music=Core.assets.getOrNull("music/"+name+".mp3",Music.class);
            if(music!=null)return music;
        }catch(Throwable ignored){}
        try{
            return Vars.tree.loadMusic(name);
        }catch(Throwable t){
            Log.err("[MusicPlayer] 无法加载音乐: "+name);
            Log.err(t);
            return null;
        }
    }
    private static void loadTrack(){
        stop();
        if(files.length==0)return;
        String file=files[currentIndex];
        try{
            currentMusic=getMusic(file);
        }catch(Throwable t){
            Log.err("[MusicPlayer] 加载音乐失败: "+file);
            Log.err(t);
            currentMusic=null;
            return;
        }
        if(currentMusic==null){
            Log.err("[MusicPlayer] 音乐不存在: "+file);
            return;
        }
        try{
            currentMusic.setVolume(volume);
            currentMusic.setLooping(loopSingle);
            currentMusic.play();
            if(progressSlider!=null){
                progressSlider.setValue(0f);
            }
            Log.info("[MusicPlayer] 播放: "+names[currentIndex]);
        }catch(Throwable t){
            Log.err("[MusicPlayer] 播放失败: "+file);
            Log.err(t);
            currentMusic=null;
        }
    }
    private static void togglePlay(){
        if(currentMusic==null){
            loadTrack();
            return;
        }
        try{
            if(currentMusic.isPlaying()){
                currentMusic.pause(true);
            }else{
                currentMusic.play();
            }
        }catch(Throwable t){
            Log.err("[MusicPlayer] 播放控制失败");
            Log.err(t);
        }
        updateButtonTexts();
    }
    private static void prevTrack(){
        if(files.length==0)return;
        if(shuffle){
            randomTrack();
        }else{
            currentIndex=(currentIndex-1+files.length)%files.length;
        }
        loadTrack();
    }
    private static void nextTrack(){
        if(files.length==0)return;
        if(shuffle){
            randomTrack();
        }else{
            currentIndex=(currentIndex+1)%files.length;
        }
        loadTrack();
    }
    private static void randomTrack(){
        if(files.length<=1){
            currentIndex=0;
            return;
        }
        int next;
        do{
            next=(int)(Math.random()*files.length);
        }while(next==currentIndex);
        currentIndex=next;
    }
    private static void stop(){
        if(currentMusic!=null){
            try{
                currentMusic.stop();
            }catch(Throwable ignored){}
        }
        updateButtonTexts();
    }
    private static void toggleShuffle(){
        shuffle=!shuffle;
        updateButtonTexts();
    }
    private static void toggleLoop(){
        loopSingle=!loopSingle;
        if(currentMusic!=null){
            try{
                currentMusic.setLooping(loopSingle);
            }catch(Throwable ignored){}
        }
        updateButtonTexts();
    }
    private static void openPlayerUI(){
        if(playerDialog!=null){
            playerDialog.show();
            updateButtonTexts();
            return;
        }
        playerDialog=new BaseDialog("音乐播放器");
        Table cont=playerDialog.cont;
        cont.pane(pane->{
            pane.table(t->{
                t.label(()->files.length==0?"没有音乐":names[currentIndex]).fontScale(1.3f).padBottom(8f).row();
                t.label(()->(currentIndex+1)+" / "+files.length).padBottom(10f).row();
                t.table(volT->{
                    volT.add("音量").padRight(10f);
                    volumeSlider=new Slider(0f,1f,0.01f,false);
                    volumeSlider.setValue(volume);
                    volumeSlider.changed(()->{
                        volume=volumeSlider.getValue();
                        if(currentMusic!=null){
                            try{
                                currentMusic.setVolume(volume);
                            }catch(Throwable ignored){}
                        }
                    });
                    volT.add(volumeSlider).width(240f);
                }).padBottom(12f).row();
                t.add("播放进度").padBottom(5f).row();
                progressSlider=new Slider(0f,1f,0.001f,false);
                progressSlider.setValue(0f);
                progressSlider.changed(()->{
                    if(currentMusic==null)return;
                    try{
                        float length=currentMusic.getLength();
                        if(length>0f){
                            currentMusic.setPosition(progressSlider.getValue()*length);
                        }
                    }catch(Throwable ignored){}
                });
                t.add(progressSlider).width(300f).padBottom(15f).row();
                t.table(btnT->{
                    btnT.button("上一曲",MusicPlayer::prevTrack).size(90f,55f);
                    playButton=btnT.button(isPlaying()?"暂停":"播放",MusicPlayer::togglePlay).size(90f,55f).get();
                    btnT.button("下一曲",MusicPlayer::nextTrack).size(90f,55f);
                }).padBottom(8f).row();
                t.table(btnT->{
                    btnT.button("停止",MusicPlayer::stop).size(90f,45f);
                    shuffleButton=btnT.button(shuffle?"随机：开":"随机：关",MusicPlayer::toggleShuffle).size(90f,45f).get();
                    loopButton=btnT.button(loopSingle?"单曲：开":"单曲：关",MusicPlayer::toggleLoop).size(90f,45f).get();
                }).padBottom(12f).row();
                t.add("曲目列表").padBottom(8f).row();
                t.pane(listPane->{
                    for(int i=0;i<files.length;i++){
                        final int index=i;
                        listPane.button(index==currentIndex?"▶ "+names[index]:names[index],()->{
                            currentIndex=index;
                            loadTrack();
                        }).growX().height(42f).pad(3f).row();
                    }
                }).width(330f).height(260f);
            }).pad(15f);
        });
        playerDialog.addCloseButton();
        playerDialog.show();
    }
}