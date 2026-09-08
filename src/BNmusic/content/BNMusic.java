package BNmusic.content;

import arc.Core;
import arc.Events;
import arc.audio.Music;
import arc.audio.Sound;
import arc.audio.Filters;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.audio.SoundControl;
import mindustry.game.EventType.MusicRegisterEvent;

public class BNMusic extends SoundControl{
    private static BNMusic instance;
    private final Seq<Music> gameMusic = new Seq<>();
    private final Seq<Music> customBossMusic = new Seq<>();
    private int gameIndex = 0;
    private boolean bossPlaying = false;
    private Music lastBossMusic;
    private boolean initialized = false;
    public BNMusic(){
        super();
        instance = this;
    }
    public static void load(){
        if(Vars.headless){
            return;
        }
        if(instance == null){
            instance = new BNMusic();
        }
    }
    @Override
    protected void reload(){
        current = null;
        fade = 0f;
        silenced = false;
        lastRandomPlayed = null;
        lastPlayed = Time.millis();
        ambientMusic = new Seq<>();
        darkMusic = new Seq<>();
        bossMusic = new Seq<>();
        gameMusic.clear();
        customBossMusic.clear();
        gameIndex = 0;
        bossPlaying = false;
        lastBossMusic = null;
        for(var sound : Core.assets.getAll(Sound.class, new Seq<>())){
            var file = Fi.get(Core.assets.getAssetFileName(sound));
            if(file.parent().name().equals("ui")){
                sound.setBus(uiBus);
            }
        }
        for(int i = 1; i <= 27; i++){
            String path = "music/game" + i + ".ogg";
            if(!Core.files.internal(path).exists()){
                Log.warn("[BNMusic] Game music not found: @", path);
                continue;
            }
            Core.assets.load(path, Music.class).loaded = music -> {
                if(music == null){
                    return;
                }
                music.setLooping(false);
                if(!gameMusic.contains(music, true)){
                    gameMusic.add(music);
                }
                Log.info("[BNMusic] Loaded game music: @", path);
            };
        }
        for(int i = 1; i <= 3; i++){
            String path = "music/boss" + i + ".ogg";
            if(!Core.files.internal(path).exists()){
                Log.warn("[BNMusic] Boss music not found: @", path);
                continue;
            }
            Core.assets.load(path, Music.class).loaded = music -> {
                if(music == null){
                    return;
                }
                music.setLooping(true);
                if(!customBossMusic.contains(music, true)){
                    customBossMusic.add(music);
                }
                Log.info("[BNMusic] Loaded boss music: @", path);
            };
        }
        Events.fire(new MusicRegisterEvent());
        initialized = true;
        Log.info("[BNMusic] reload: loading 27 game music and 3 boss music.");
    }
    @Override
    public void update(){
        if(Vars.headless){
            return;
        }
        boolean paused = state.isGame() && Core.scene.hasDialog();
        boolean playing = state.isGame();
        if(current != null && !current.isPlaying()){
            current = null;
            fade = 0f;
        }
        if(timer.get(1, 30f)){
            Core.audio.soundBus.fadeFilterParam(0, Filters.paramWet, paused ? 1f : 0f, 0.4f);
        }
        if(playing != wasPlaying){
            wasPlaying = playing;
            if(playing){
                Core.audio.soundBus.play();
                setupFilters();
            }else{
                Core.audio.soundBus.stop();
                Core.audio.musicBus.play();
                Core.audio.soundBus.play();
            }
        }
        Core.audio.setPaused(Core.audio.soundBus.id, state.isPaused());
        if(state.isMenu()){
            bossPlaying = false;
            silenced = false;
            if(ui.planet.isShown()){
                play(ui.planet.state.planet.launchMusic);
            }else if(ui.editor.isShown()){
                play(Musics.editor);
            }else{
                play(Musics.menu);
            }
            updateLoops();
            return;
        }
        if(state.rules.editor){
            bossPlaying = false;
            silenced = false;
            play(Musics.editor);
            updateLoops();
            return;
        }
        if(!state.isGame()){
            bossPlaying = false;
            silence();
            updateLoops();
            return;
        }
        boolean bossFound = state.boss() != null;
        if(bossFound){
            if(!bossPlaying){
                bossPlaying = true;
                playBossMusic();
            }else if(current == null || !current.isPlaying()){
                playBossMusic();
            }else if(!customBossMusic.contains(current, true)){
                playBossMusic();
            }
        }else{
            if(bossPlaying){
                bossPlaying = false;
                playNextGameMusic();
            }else if(current == null || !current.isPlaying()){
                playNextGameMusic();
            }
        }
        updateLoops();
    }
    private void playNextGameMusic(){
        if(bossPlaying){
            return;
        }
        if(gameMusic.isEmpty()){
            return;
        }
        if(gameIndex >= gameMusic.size){
            gameIndex = 0;
        }
        Music music = gameMusic.get(gameIndex);
        gameIndex++;
        if(music == null){
            playNextGameMusic();
            return;
        }
        music.setLooping(false);
        play(music);
        Log.info("[BNMusic] Playing game music: @", gameIndex);
    }
    private void playBossMusic(){
        if(customBossMusic.isEmpty()){
            Log.warn("[BNMusic] Boss detected but no boss music is loaded.");
            return;
        }
        Music music;
        if(customBossMusic.size == 1){
            music = customBossMusic.first();
        }else{
            do{
                music = customBossMusic.random();
            }while(music == lastBossMusic);
        }
        lastBossMusic = music;
        music.setLooping(true);
        play(music);
        Log.info("[BNMusic] Playing boss music.");
    }
    @Override
    public void playRandom(){
        if(Vars.headless || !state.isGame()){
            return;
        }
        if(state.boss() != null){
            bossPlaying = true;
            playBossMusic();
        }else{
            playNextGameMusic();
        }
    }
    @Override
    protected void playOnce(Music music){
        if(Vars.headless || music == null){
            return;
        }
        if(state.isGame() && state.boss() != null){
            bossPlaying = true;
            playBossMusic();
        }
    }
    @Override
    protected void silence(){
        play(null);
    }
    public boolean isBossPlaying(){
        return bossPlaying;
    }
    public Music currentMusic(){
        return current;
    }
    public int gameMusicCount(){
        return gameMusic.size;
    }
    public int bossMusicCount(){
        return customBossMusic.size;
    }
    public void restartGameMusic(){
        if(Vars.headless){
            return;
        }
        bossPlaying = false;
        gameIndex = 0;
        playNextGameMusic();
    }
}
