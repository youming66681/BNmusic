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
    private static final Seq<Music> gameMusic = new Seq<>();
    private static final Seq<Music> bossMusic = new Seq<>();
    private static Music currentMusic;
    private static int gameIndex = 0;
    private static boolean bossPlaying = false;
    private static boolean initialized = false;
    private static boolean gameStarted = false;
    public static void load(){
        if(Vars.headless){
            Log.info("[BNMusic] Headless mode detected, music system disabled.");
            return;
        }
        if(initialized){
            Log.warn("[BNMusic] Already initialized.");
            return;
        }
        initialized = true;
        Log.info("[BNMusic] Initializing BNMusic V3...");
        loadGameMusic();
        loadBossMusic();
        Events.run(EventType.Trigger.update, () -> {
            if(Vars.headless){
                return;
            }
            update();
        });
        Log.info("[BNMusic] BNMusic V3 initialized.");
    }
    private static void loadGameMusic(){
        for(int i = 1; i <= 27; i++){
            String path = "music/game" + i + ".ogg";
            if(!Core.files.internal(path).exists()){
                Log.warn("[BNMusic] Game music not found: @", path);
                continue;
            }
            final String musicPath = path;
            Core.assets.load(musicPath, Music.class).loaded = music -> {
                if(music == null){
                    Log.warn("[BNMusic] Failed to load game music: @", musicPath);
                    return;
                }
                music.setLooping(false);
                if(!gameMusic.contains(music, true)){
                    gameMusic.add(music);
                }
                Log.info("[BNMusic] Loaded game music: @", musicPath);
                if(!gameStarted && !gameMusic.isEmpty()){
                    gameStarted = true;
                    playNextGame();
                }
            };
        }
    }
    private static void loadBossMusic(){
        for(int i = 1; i <= 3; i++){
            String path = "music/boss" + i + ".ogg";
            if(!Core.files.internal(path).exists()){
                Log.warn("[BNMusic] Boss music not found: @", path);
                continue;
            }
            final String musicPath = path;
            Core.assets.load(musicPath, Music.class).loaded = music -> {
                if(music == null){
                    Log.warn("[BNMusic] Failed to load boss music: @", musicPath);
                    return;
                }
                music.setLooping(true);
                if(!bossMusic.contains(music, true)){
                    bossMusic.add(music);
                }
                Log.info("[BNMusic] Loaded boss music: @", musicPath);
            };
        }
    }
    private static void update(){
        boolean bossFound = false;
        for(Unit unit : Groups.unit){
            if(isBoss(unit)){
                bossFound = true;
                break;
            }
        }
        if(bossFound && !bossPlaying){
            bossPlaying = true;
            Log.info("[BNMusic] Boss detected, taking over music.");
            playBoss();
            return;
        }
        if(!bossFound && bossPlaying){
            bossPlaying = false;
            Log.info("[BNMusic] Boss defeated or removed, restoring game music.");
            playNextGame();
            return;
        }
        if(bossPlaying){
            if(currentMusic == null || !currentMusic.isPlaying()){
                playBoss();
            }
            return;
        }
        if(currentMusic == null){
            playNextGame();
            return;
        }
        if(!currentMusic.isPlaying()){
            playNextGame();
        }
    }
    private static boolean isBoss(Unit unit){
        if(unit == null){
            return false;
        }
        if(unit.type == null){
            return false;
        }
        if(unit.type.name == null){
            return false;
        }
        String name = unit.type.name.toLowerCase();
        return name.contains("boss");
    }
    private static void playNextGame(){
        if(bossPlaying){
            return;
        }
        if(gameMusic.isEmpty()){
            Log.warn("[BNMusic] No game music loaded.");
            return;
        }
        stopCurrent();
        if(gameIndex >= gameMusic.size){
            gameIndex = 0;
        }
        Music music = gameMusic.get(gameIndex);
        gameIndex++;
        if(music == null){
            Log.warn("[BNMusic] Game music entry is null.");
            return;
        }
        music.setLooping(false);
        currentMusic = music;
        currentMusic.play();
        Log.info("[BNMusic] Playing game music: @ / @", gameIndex, gameMusic.size);
    }
    private static void playBoss(){
        if(bossMusic.isEmpty()){
            Log.warn("[BNMusic] Boss detected, but no boss music is loaded.");
            return;
        }
        stopCurrent();
        int index = (int)(Math.random() * bossMusic.size);
        if(index >= bossMusic.size){
            index = 0;
        }
        Music music = bossMusic.get(index);
        if(music == null){
            Log.warn("[BNMusic] Selected boss music is null.");
            return;
        }
        music.setLooping(true);
        currentMusic = music;
        currentMusic.play();
        Log.info("[BNMusic] Playing boss music: @ / @", index + 1, bossMusic.size);
    }
    private static void stopCurrent(){
        if(currentMusic == null){
            return;
        }
        try{
            if(currentMusic.isPlaying()){
                currentMusic.stop();
            }
        }catch(Throwable e){
            Log.err("[BNMusic] Failed to stop current music.", e);
        }
        currentMusic = null;
    }
    public static void stop(){
        if(Vars.headless){
            return;
        }
        stopCurrent();
        bossPlaying = false;
        gameStarted = false;
        gameIndex = 0;
        Log.info("[BNMusic] Music stopped.");
    }
    public static void restartGameMusic(){
        if(Vars.headless){
            return;
        }
        bossPlaying = false;
        playNextGame();
    }
    public static boolean isBossPlaying(){
        return bossPlaying;
    }
    public static Music currentMusic(){
        return currentMusic;
    }
    public static int gameMusicCount(){
        return gameMusic.size;
    }
    public static int bossMusicCount(){
        return bossMusic.size;
    }
}
