package BNmusic.content;

import arc.Core;
import arc.audio.Music;
import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

public class BNMusic{
    private static final Seq<Music> gameMusic=new Seq<>();
    private static final Seq<Music> bossMusic=new Seq<>();
    private static Music currentMusic;
    private static boolean bossPlaying=false;
    private static int gameIndex=0;
    private static boolean bossFound=false;
    private static boolean loaded=false;
    public static void load(){
        if(Vars.headless)return;
        loadGameMusic();
        loadBossMusic();
        Events.run(EventType.Trigger.update,()->{
            if(!Vars.headless)update();
        });
    }
    private static void loadGameMusic(){
        for(int i=1;i<=27;i++){
            int id=i;
            String path="music/game"+id+".ogg";
            if(!Core.files.internal(path).exists()){
                Log.warn("[BNMusic] Game music not found: @",path);
                continue;
            }
            Core.assets.load(path,Music.class).loaded=music->{
                music.setLooping(false);
                gameMusic.add(music);
                Log.info("[BNMusic] Loaded game music: @",path);
                if(!loaded){
                    loaded=true;
                    playNextGame();
                }
            };
        }
    }
    private static void loadBossMusic(){
        for(int i=1;i<=3;i++){
            int id=i;
            String path="music/boss"+id+".ogg";
            if(!Core.files.internal(path).exists()){
                Log.warn("[BNMusic] Boss music not found: @",path);
                continue;
            }
            Core.assets.load(path,Music.class).loaded=music->{
                music.setLooping(true);
                bossMusic.add(music);
                Log.info("[BNMusic] Loaded boss music: @",path);
            };
        }
    }
    private static void update(){
        bossFound=false;
        Groups.unit.each(unit->{
            if(isBoss(unit)){
                bossFound=true;
            }
        });
        if(bossFound&&!bossPlaying){
            bossPlaying=true;
            playBoss();
            return;
        }
        if(!bossFound&&bossPlaying){
            bossPlaying=false;
            playNextGame();
            return;
        }
        if(currentMusic!=null&&!currentMusic.isPlaying()&&!bossPlaying){
            playNextGame();
        }
    }
    private static boolean isBoss(Unit unit){
        return unit!=null&&unit.type!=null&&unit.type.name!=null&&unit.type.name.toLowerCase().contains("boss");
    }
    private static void playNextGame(){
        if(gameMusic.isEmpty()){
            return;
        }
        stopCurrent();
        if(gameIndex>=gameMusic.size){
            gameIndex=0;
        }
        currentMusic=gameMusic.get(gameIndex);
        gameIndex++;
        currentMusic.setLooping(false);
        currentMusic.play();
        Log.info("[BNMusic] Playing game music: index @",gameIndex);
    }
    private static void playBoss(){
        if(bossMusic.isEmpty()){
            Log.warn("[BNMusic] No boss music loaded.");
            return;
        }
        stopCurrent();
        int index=(int)(Math.random()*bossMusic.size);
        currentMusic=bossMusic.get(index);
        currentMusic.setLooping(true);
        currentMusic.play();
        Log.info("[BNMusic] Playing boss music.");
    }
    private static void stopCurrent(){
        if(currentMusic!=null){
            currentMusic.stop();
            currentMusic=null;
        }
    }
}