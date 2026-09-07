package BNmusic.content;
import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.game.EventType;
public class BNMusic{
    public static final Seq<Music> gameMusic = new Seq<>();
    private static Music current;
    private static int lastIndex = -1;
    private static boolean loaded = false;
    private static boolean enabled = true;
    private static float checkTimer = 0f;
    private static final float CHECK_INTERVAL = 10f;
    public static void load(){
        for(int i = 1;i <= 27;i++){
            loadMusic("game" + i);
        }
        Log.info("[BNMusic] Loading 27 game music tracks...");
    }
    private static void loadMusic(String name){
        String path = "music/" + name + ".ogg";
        Core.assets.load(path, Music.class).loaded = music -> {
            music.setLooping(false);
            music.setVolume(0f);
            gameMusic.add(music);
            Log.info("[BNMusic] Loaded: @", name);
            if(gameMusic.size >= 27){
                loaded = true;
                Log.info("[BNMusic] All 27 game music tracks loaded.");
                start();
            }
        };
    }
    public static void init(){
        Events.run(EventType.Trigger.update, BNMusic::update);
    }
    private static void update(){
        if(!enabled)return;
        if(!loaded)return;
        if(gameMusic.isEmpty())return;
        checkTimer += Time.delta;
        if(checkTimer < CHECK_INTERVAL)return;
        checkTimer = 0f;
        if(current == null){
            start();
            return;
        }
        if(!current.isPlaying()){
            start();
            return;
        }
        forceMusic();
    }
    public static void start(){
        if(gameMusic.isEmpty())return;
        Music next = randomMusic();
        if(next == null)return;
        stopCurrent();
        current = next;
        current.setLooping(false);
        current.setVolume(getVolume());
        current.play();
        Log.info("[BNMusic] Playing music.");
    }
    private static Music randomMusic(){
        if(gameMusic.isEmpty())return null;
        if(gameMusic.size == 1)return gameMusic.first();
        int index;
        do{
            index = Core.app.getRandom().nextInt(gameMusic.size);
        }while(index == lastIndex);
        lastIndex = index;
        return gameMusic.get(index);
    }
    private static void forceMusic(){
        if(current == null)return;
        current.setVolume(getVolume());
        if(!current.isPlaying()){
            start();
        }
    }
    private static float getVolume(){
        return Core.settings.getFloat("musicvol",1f);
    }
    private static void stopCurrent(){
        if(current != null){
            current.stop();
            current.setVolume(0f);
            current = null;
        }
    }
    public static void stop(){
        stopCurrent();
    }
    public static void resume(){
        if(current == null || !current.isPlaying()){
            start();
        }
    }
    public static Music current(){
        return current;
    }
    public static boolean isPlaying(){
        return current != null && current.isPlaying();
    }
    public static void setEnabled(boolean value){
        enabled = value;
        if(!value){
            stopCurrent();
        }else{
            start();
        }
    }
}