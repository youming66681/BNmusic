package BNmusic.content;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
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
    private static boolean shuffle=false;
    private static boolean loopSingle=false;
    private static Table musicTable;
    private static TextButton playButton;
    private static TextButton shuffleButton;
    private static TextButton loopButton;
    private static boolean loaded=false;
    private static boolean injected=false;
    public static void load(){
        Events.on(ClientLoadEvent.class,e->{
            injectIntoSettings();
        });
        Events.on(GameOverEvent.class,e->stop());
        Events.on(WorldLoadEvent.class,e->stop());
        Events.run(Trigger.update,MusicPlayer::update);
        Log.info("[MusicPlayer] 音乐播放器已加载");
        Log.info("[MusicPlayer] 共 "+files.length+" 首音乐");
    }
    private static void update(){
        if(!injected){
            injectIntoSettings();
        }
        if(currentMusic==null)return;
        try{
            if(!loopSingle){
                float length=currentMusic.getLength();
                float position=currentMusic.getPosition();
                if(length>0f&&position>=length-0.2f){
                    nextTrack();
                }
            }
        }catch(Throwable ignored){}
        updateButtons();
    }
    private static void injectIntoSettings(){
        if(injected)return;
        if(Vars.ui==null)return;
        if(Vars.ui.settings==null)return;
        if(Vars.ui.settings.sound==null)return;
        try{
            musicTable=new Table();
            musicTable.left();
            musicTable.defaults().left();
            musicTable.add("音乐播放器").fontScale(1.2f).padTop(20f).padBottom(8f).row();
            musicTable.add("当前音乐：").padBottom(3f).row();
            musicTable.add(() -> names[currentIndex]).padBottom(8f).row();
            musicTable.table(t->{
                t.defaults().size(100f,50f).pad(3f);
                t.button("上一曲",MusicPlayer::prevTrack);
                playButton=t.button("播放",MusicPlayer::togglePlay).get();
                t.button("下一曲",MusicPlayer::nextTrack);
            }).padBottom(5f).row();
            musicTable.table(t->{
                t.defaults().size(100f,45f).pad(3f);
                t.button("停止",MusicPlayer::stop);
                shuffleButton=t.button("随机：关",MusicPlayer::toggleShuffle).get();
                loopButton=t.button("单曲：关",MusicPlayer::toggleLoop).get();
            }).padBottom(8f).row();
            musicTable.add("音乐列表").padTop(5f).padBottom(5f).row();
            musicTable.pane(list->{
                for(int i=0;i<files.length;i++){
                    final int index=i;
                    list.button(index==currentIndex?"▶ "+names[index]:names[index],Styles.flatt,()->{
                        currentIndex=index;
                        loadTrack();
                        rebuildMusicList();
                    }).growX().height(42f).pad(2f).row();
                }
            }).width(340f).height(280f).padBottom(10f).row();
            musicTable.add("提示：音乐文件放在 assets/music/").padBottom(5f).row();
            Vars.ui.settings.sound.add(musicTable).padTop(10f).row();
            injected=true;
            Log.info("[MusicPlayer] 已注入原版设置 → 音频");
        }catch(Throwable t){
            Log.err("[MusicPlayer] 注入原版音频设置失败");
            Log.err(t);
        }
    }
    private static void rebuildMusicList(){
        if(musicTable==null)return;
        try{
            musicTable.clearChildren();
            musicTable.add("音乐播放器").fontScale(1.2f).padTop(20f).padBottom(8f).row();
            musicTable.add("当前音乐：").padBottom(3f).row();
            musicTable.add(() -> names[currentIndex]).padBottom(8f).row();
            musicTable.table(t->{
                t.defaults().size(100f,50f).pad(3f);
                t.button("上一曲",MusicPlayer::prevTrack);
                playButton=t.button(isPlaying()?"暂停":"播放",MusicPlayer::togglePlay).get();
                t.button("下一曲",MusicPlayer::nextTrack);
            }).padBottom(5f).row();
            musicTable.table(t->{
                t.defaults().size(100f,45f).pad(3f);
                t.button("停止",MusicPlayer::stop);
                shuffleButton=t.button(shuffle?"随机：开":"随机：关",MusicPlayer::toggleShuffle).get();
                loopButton=t.button(loopSingle?"单曲：开":"单曲：关",MusicPlayer::toggleLoop).get();
            }).padBottom(8f).row();
            musicTable.add("音乐列表").padTop(5f).padBottom(5f).row();
            musicTable.pane(list->{
                for(int i=0;i<files.length;i++){
                    final int index=i;
                    list.button(index==currentIndex?"▶ "+names[index]:names[index],Styles.flatt,()->{
                        currentIndex=index;
                        loadTrack();
                        rebuildMusicList();
                    }).growX().height(42f).pad(2f).row();
                }
            }).width(340f).height(280f).padBottom(10f).row();
            musicTable.add("提示：音乐文件放在 assets/music/").padBottom(5f).row();
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
            loaded=true;
            Log.info("[MusicPlayer] 播放: "+names[currentIndex]);
            updateButtons();
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
        rebuildMusicList();
    }
    private static void nextTrack(){
        if(files.length==0)return;
        if(shuffle){
            randomTrack();
        }else{
            currentIndex=(currentIndex+1)%files.length;
        }
        loadTrack();
        rebuildMusicList();
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