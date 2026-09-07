package BNmusic.content;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.audio.SoundControl;
import mindustry.entities.Groups;
import mindustry.gen.Unit;
import mindustry.game.EventType;

public class BNMusic{
    public static final Seq<Music> gameMusic = new Seq<>();
    public static final Seq<Music> bossMusic = new Seq<>();
    private static Music current;
    private static Music bossCurrent;
    private static int lastGameIndex = -1;
    private static int lastBossIndex = -1;
    private static boolean loaded = false;
    private static boolean enabled = true;
    private static float checkTimer = 0f;
    private static boolean bossPlaying = false;
    private static final float CHECK_INTERVAL = 5f;
    public static void load(){
        for(int i = 1;i <= 27;i++){
            loadGameMusic("game" + i);
        }
        for(int i = 1;i <= 3;i++){
            loadBossMusic("boss" + i);
        }
        Log.info("[BNMusic] Loading 27 game music tracks and 3 boss music tracks...");
    }
    private static void loadGameMusic(String name){
        String path = "music/" + name + ".ogg";
        Core.assets.load(path, Music.class).loaded = music -> {
            music.setLooping(false);
            music.setVolume(0f);
            gameMusic.add(music);
            Log.info("[BNMusic] Loaded game music: @", name);
            checkLoaded();
        };
    }
    private static void loadBossMusic(String name){
        String path = "music/" + name + ".ogg";
        Core.assets.load(path, Music.class).loaded = music -> {
            music.setLooping(false);
            music.setVolume(0f);
            bossMusic.add(music);
            Log.info("[BNMusic] Loaded boss music: @", name);
            checkLoaded();
        };
    }
    private static void checkLoaded(){
        if(gameMusic.size >= 27 && bossMusic.size >= 3 && !loaded){
            loaded = true;
            Log.info("[BNMusic] All music loaded: 27 game + 3 boss.");
            startGameMusic();
        }
    }
    public static void init(){
        Events.run(EventType.Trigger.update, BNMusic::update);
    }
    private static void update(){
        if(!enabled)return;
        disableVanillaMusic();
        if(!loaded)return;
        checkTimer += Time.delta;
        if(checkTimer < CHECK_INTERVAL)return;
        checkTimer = 0f;
        boolean boss = hasBoss();
        if(boss){
            if(!bossPlaying){
                startBossMusic();
            }else{
                maintainBossMusic();
            }
        }else{
            if(bossPlaying){
                stopBossMusic();
                startGameMusic();
            }else{
                maintainGameMusic();
            }
        }
    }
    private static void disableVanillaMusic(){
        if(Vars.state != null && Vars.state.rules != null){
            Vars.state.rules.disableMusic = true;
        }
        if(Vars.control != null && Vars.control.sound != null){
            Vars.control.sound.stop();
        }
    }
    private static boolean hasBoss(){
        for(Unit unit : Groups.unit){
            if(unit != null && unit.isAdded() && unit.isBoss() && !unit.dead()){
                return true;
            }
        }
        return false;
    }
    private static void startGameMusic(){
        if(gameMusic.isEmpty())return;
        Music next = randomGameMusic();
        if(next == null)return;
        stopGameMusic();
        stopBossMusic();
        bossPlaying = false;
        current = next;
        current.setLooping(false);
        current.setVolume(getVolume());
        current.play();
        Log.info("[BNMusic] Playing game music.");
    }
    private static void maintainGameMusic(){
        if(current == null){
            startGameMusic();
            return;
        }
        current.setVolume(getVolume());
        if(!current.isPlaying()){
            startGameMusic();
        }
    }
    private static void startBossMusic(){
        if(bossMusic.isEmpty())return;
        Music next = randomBossMusic();
        if(next == null)return;
        stopGameMusic();
        stopBossMusic();
        bossCurrent = next;
        bossCurrent.setLooping(false);
        bossCurrent.setVolume(getVolume());
        bossCurrent.play();
        bossPlaying = true;
        Log.info("[BNMusic] Boss detected, playing boss music.");
    }
    private static void maintainBossMusic(){
        if(bossCurrent == null){
            startBossMusic();
            return;
        }
        bossCurrent.setVolume(getVolume());
        if(!bossCurrent.isPlaying()){
            startBossMusic();
        }
    }
    private static Music randomGameMusic(){
        if(gameMusic.isEmpty())return null;
        if(gameMusic.size == 1){
            lastGameIndex = 0;
            return gameMusic.first();
        }
        int index;
        do{
            index = Core.app.getJavaScript().getClass().hashCode();
            index = Math.abs(index + (int)Time.millis()) % gameMusic.size;
        }while(index == lastGameIndex);
        lastGameIndex = index;
        return gameMusic.get(index);
    }
    private static Music randomBossMusic(){
        if(bossMusic.isEmpty())return null;
        if(bossMusic.size == 1){
            lastBossIndex = 0;
            return bossMusic.first();
        }
        int index;
        do{
            index = Math.abs((int)(Time.millis() ^ (Time.millis() >>> 32))) % bossMusic.size;
        }while(index == lastBossIndex);
        lastBossIndex = index;
        return bossMusic.get(index);
    }
    private static void stopGameMusic(){
        if(current != null){
            current.stop();
            current.setVolume(0f);
            current = null;
        }
    }
    private static void stopBossMusic(){
        if(bossCurrent != null){
            bossCurrent.stop();
            bossCurrent.setVolume(0f);
            bossCurrent = null;
        }
        bossPlaying = false;
    }
    private static float getVolume(){
        return Core.settings.getInt("musicvol", 100) / 100f;
    }
    public static void stop(){
        stopGameMusic();
        stopBossMusic();
    }
    public static void resume(){
        if(!enabled)return;
        if(bossPlaying){
            if(bossCurrent == null || !bossCurrent.isPlaying()){
                startBossMusic();
            }
        }else{
            if(current == null || !current.isPlaying()){
                startGameMusic();
            }
        }
    }
    public static Music current(){
        return bossPlaying ? bossCurrent : current;
    }
    public static boolean isPlaying(){
        Music music = current();
        return music != null && music.isPlaying();
    }
    public static void setEnabled(boolean value){
        enabled = value;
        if(!value){
            stop();
        }else{
            resume();
        }
    }
}