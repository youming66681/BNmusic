package BNmusic.content;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.ui.Styles;

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
    private static final Music[] musicList=new Music[files.length];
    private static boolean musicLoaded=false;
    private static boolean loadingMusic=false;
    private static boolean shuffle=false;
    private static boolean loopSingle=false;
    private static boolean changingTrack=false;
    private static boolean vanillaMusicMuted=false;
    private static Dialog dialog;
    private static Label currentMusicLabel;
    private static TextButton playButton;
    private static TextButton shuffleButton;
    private static TextButton loopButton;
    private static Table listTable;
    public static void load(){
        Events.on(ClientLoadEvent.class,e->{
            Log.info("[MusicPlayer] 音乐播放器已加载");
            Log.info("[MusicPlayer] 共 "+files.length+" 首音乐");
            Core.app.post(MusicPlayer::loadAllMusic);
        });
        Events.on(GameOverEvent.class,e->{
            stop();
        });
        Events.on(WorldLoadEvent.class,e->{
            stop();
        });
        Events.run(Trigger.update,MusicPlayer::update);
    }
    private static void loadAllMusic(){
        if(loadingMusic||musicLoaded)return;
        loadingMusic=true;
        Log.info("[MusicPlayer] 开始加载音乐...");
        for(int i=0;i<files.length;i++){
            String path="music/"+files[i]+".ogg";
            try{
                if(Core.assets.isLoaded(path,Music.class)){
                    musicList[i]=Core.assets.get(path,Music.class);
                    Log.info("[MusicPlayer] 已经加载: "+path);
                }else{
                    Core.assets.load(path,Music.class);
                    Log.info("[MusicPlayer] 请求加载: "+path);
                }
            }catch(Throwable t){
                Log.err("[MusicPlayer] 请求加载失败: "+path);
                Log.err(t);
            }
        }
    }
    private static void waitForMusic(){
        if(!loadingMusic||musicLoaded)return;
        boolean allLoaded=true;
        for(int i=0;i<files.length;i++){
            String path="music/"+files[i]+".ogg";
            try{
                if(Core.assets.isLoaded(path,Music.class)){
                    if(musicList[i]==null){
                        musicList[i]=Core.assets.get(path,Music.class);
                    }
                }else{
                    allLoaded=false;
                }
            }catch(Throwable ignored){
                allLoaded=false;
            }
        }
        if(!allLoaded)return;
        int loaded=0;
        for(int i=0;i<musicList.length;i++){
            if(musicList[i]!=null){
                loaded++;
                Log.info("[MusicPlayer] 已加载: "+files[i]);
            }else{
                Log.err("[MusicPlayer] 音乐为空: "+files[i]);
            }
        }
        musicLoaded=true;
        loadingMusic=false;
        Log.info("[MusicPlayer] 音乐加载完成: "+loaded+"/"+files.length);
    }
    private static void update(){
        if(loadingMusic&&!musicLoaded){
            waitForMusic();
        }
        if(currentMusic!=null&&!changingTrack){
            try{
                if(!loopSingle){
                    float length=currentMusic.getLength();
                    float position=currentMusic.getPosition();
                    if(length>0f&&position>=length-0.2f){
                        changingTrack=true;
                        nextTrack();
                        changingTrack=false;
                    }
                }
            }catch(Throwable t){
                changingTrack=false;
            }
        }
        updateButtons();
    }
    public static void showDialog(){
        if(Core.scene==null)return;
        if(dialog!=null&&dialog.parent!=null){
            dialog.toFront();
            refreshDialog();
            return;
        }
        dialog=new Dialog("BNmusic 音乐播放器");
        dialog.cont.clear();
        dialog.cont.defaults().growX();
        dialog.cont.add("音乐播放器").fontScale(1.2f).pad(10f).row();
        currentMusicLabel=new Label("");
        dialog.cont.add(currentMusicLabel).padBottom(10f).row();
        dialog.cont.table(t->{
            t.defaults().size(105f,55f).pad(4f);
            t.button("上一曲",Styles.flatt,MusicPlayer::prevTrack);
            playButton=t.button(isPlaying()?"暂停":"播放",Styles.flatt,MusicPlayer::togglePlay).get();
            t.button("下一曲",Styles.flatt,MusicPlayer::nextTrack);
        }).row();
        dialog.cont.table(t->{
            t.defaults().size(105f,50f).pad(4f);
            t.button("停止",Styles.flatt,MusicPlayer::stop);
            shuffleButton=t.button(shuffle?"随机：开":"随机：关",Styles.flatt,MusicPlayer::toggleShuffle).get();
            loopButton=t.button(loopSingle?"单曲：开":"单曲：关",Styles.flatt,MusicPlayer::toggleLoop).get();
        }).padBottom(10f).row();
        dialog.cont.add("音乐列表").pad(5f).row();
        listTable=new Table();
        listTable.left();
        dialog.cont.pane(listTable).width(380f).height(350f).pad(5f).row();
        dialog.cont.button("关闭",Styles.flatt,()->{
            if(dialog!=null){
                dialog.hide();
            }
        }).size(160f,55f).pad(8f).row();
        refreshDialog();
        refreshList();
        dialog.show();
    }
    private static void refreshDialog(){
        if(dialog==null)return;
        if(dialog.parent==null)return;
        if(currentMusicLabel!=null){
            currentMusicLabel.setText("当前音乐："+names[currentIndex]);
        }
        updateButtons();
        refreshList();
    }
    private static void refreshList(){
        if(listTable==null)return;
        try{
            listTable.clearChildren();
            for(int i=0;i<files.length;i++){
                final int index=i;
                listTable.button(index==currentIndex?"▶ "+names[index]:names[index],Styles.flatt,()->{
                    if(index==currentIndex){
                        if(currentMusic==null){
                            loadTrack();
                        }else{
                            togglePlay();
                        }
                        refreshDialog();
                        return;
                    }
                    currentIndex=index;
                    loadTrack();
                    refreshDialog();
                }).growX().height(42f).pad(2f).row();
            }
            listTable.pack();
        }catch(Throwable t){
            Log.err("[MusicPlayer] 刷新音乐列表失败");
            Log.err(t);
        }
    }
    private static void updateButtons(){
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
        if(currentMusicLabel!=null){
            try{
                currentMusicLabel.setText("当前音乐："+names[currentIndex]);
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
    private static void muteVanillaMusic(){
        try{
            if(Core.music!=null){
                Core.music.stop();
                Core.music.setVolume(0f);
                vanillaMusicMuted=true;
                Log.info("[MusicPlayer] 原版音乐已关闭");
            }
        }catch(Throwable t){
            Log.err("[MusicPlayer] 关闭原版音乐失败");
            Log.err(t);
        }
    }
    private static void restoreVanillaMusic(){
        try{
            if(Core.music!=null){
                Core.music.setVolume(1f);
                vanillaMusicMuted=false;
                Log.info("[MusicPlayer] 原版音乐已恢复");
            }
        }catch(Throwable t){
            Log.err("[MusicPlayer] 恢复原版音乐失败");
        }
    }
    private static void loadTrack(){
        if(files.length==0)return;
        if(!musicLoaded){
            Log.info("[MusicPlayer] 音乐还没有加载完成");
            return;
        }
        if(currentIndex<0||currentIndex>=musicList.length){
            currentIndex=0;
        }
        muteVanillaMusic();
        Music oldMusic=currentMusic;
        if(oldMusic!=null){
            try{
                oldMusic.stop();
            }catch(Throwable ignored){}
        }
        currentMusic=null;
        Music music=musicList[currentIndex];
        if(music==null){
            Log.err("[MusicPlayer] 音乐不存在: "+files[currentIndex]);
            restoreVanillaMusic();
            refreshDialog();
            return;
        }
        currentMusic=music;
        try{
            currentMusic.setVolume(volume);
            currentMusic.setLooping(loopSingle);
            currentMusic.play();
            Log.info("[MusicPlayer] 播放: "+names[currentIndex]+" ("+files[currentIndex]+")");
        }catch(Throwable t){
            Log.err("[MusicPlayer] 播放失败: "+files[currentIndex]);
            Log.err(t);
            currentMusic=null;
            restoreVanillaMusic();
        }
        refreshDialog();
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
                muteVanillaMusic();
                currentMusic.play();
            }
        }catch(Throwable t){
            Log.err("[MusicPlayer] 播放控制失败");
            Log.err(t);
        }
        updateButtons();
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
        currentMusic=null;
        restoreVanillaMusic();
        updateButtons();
    }
    private static void toggleShuffle(){
        shuffle=!shuffle;
        updateButtons();
    }
    private static void toggleLoop(){
        loopSingle=!loopSingle;
        if(currentMusic!=null){
            try{
                currentMusic.setLooping(loopSingle);
            }catch(Throwable ignored){}
        }
        updateButtons();
    }
}