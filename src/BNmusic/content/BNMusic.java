package magical.content;
import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
public class MLMusic{
    public static final Seq<Music> gameMusic = new Seq<>();
    public static final Seq<Music> bossMusic = new Seq<>();
    private static Music currentGame;
    private static Music currentBoss;
    private static boolean bossPlaying = false;
    private static boolean initialized = false;
    private static float gameCheckTimer = 0f;
    private static final float GAME_CHECK_INTERVAL = 10f;
    public static void load(){
        for(int i = 1;i <= 27;i++){
            loadGame("game" + i);
        }
        loadBoss("boss1");
        loadBoss("boss2");
        loadBoss("boss3");
        Log.info("[MLMusic] Loading 27 game tracks and 3 boss tracks...");
    }
    private static void loadGame(String name){
        String path = "music/" + name + ".ogg";
        Core.assets.load(path, Music.class).loaded = music -> {
            music.setLooping(false);
            music.setVolume(0f);
            gameMusic.add(music);
            Log.info("[MLMusic] Game music loaded: @", name);
            tryStart();
        };
    }
    private static void loadBoss(String name){
        String path = "music/" + name + ".ogg";
        Core.assets.load(path, Music.class).loaded = music -> {
            music.setLooping(true);
            music.setVolume(0f);
            bossMusic.add(music);
            Log.info("[MLMusic] Boss music loaded: @", name);
            tryStart();
        };
    }
    private static void tryStart(){
        if(initialized)return;
        if(gameMusic.isEmpty())return;
        initialized = true;
    }
    public static void update(){
        if(gameMusic.isEmpty() && bossMusic.isEmpty())return;
        boolean boss = hasBoss();
        if(boss){
            if(!bossPlaying){
                startBoss();
            }
        }else{
            if(bossPlaying){
                stopBoss();
                startGame();
            }else{
                updateGame();
            }
        }
    }
    private static boolean hasBoss(){
        final boolean[] result = {false};
        Groups.unit.each(u -> {
            if(u == null || !u.isAdded() || u.dead)return;
            if(isBoss(u)){
                result[0] = true;
            }
        });
        return result[0];
    }
    private static boolean isBoss(Unit unit){
        if(unit.type == null)return false;
        return unit.type.boss;
    }
    private static void startGame(){
        if(gameMusic.isEmpty())return;
        stopGame();
        currentGame = gameMusic.random();
        if(currentGame == null)return;
        currentGame.setLooping(false);
        currentGame.setVolume(Core.settings.getFloat("musicvol",1f));
        currentGame.play();
        Log.info("[MLMusic] Playing game music: @", gameMusic.indexOf(currentGame));
    }
    private static void updateGame(){
        if(currentGame == null || !currentGame.isPlaying()){
            startGame();
        }
    }
    private static void startBoss(){
        if(bossMusic.isEmpty())return;
        stopGame();
        stopBoss();
        currentBoss = bossMusic.random();
        if(currentBoss == null)return;
        currentBoss.setLooping(true);
        currentBoss.setVolume(Core.settings.getFloat("musicvol",1f));
        currentBoss.play();
        bossPlaying = true;
        Log.info("[MLMusic] Boss music started.");
    }
    private static void stopGame(){
        if(currentGame != null){
            currentGame.stop();
            currentGame.setVolume(0f);
            currentGame = null;
        }
    }
    private static void stopBoss(){
        if(currentBoss != null){
            currentBoss.stop();
            currentBoss.setVolume(0f);
            currentBoss = null;
        }
        bossPlaying = false;
    }
    public static void stopAll(){
        stopGame();
        stopBoss();
    }
    public static boolean isBossPlaying(){
        return bossPlaying;
    }
}