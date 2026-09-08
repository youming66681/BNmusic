package BNmusic.content;
import arc.Core;
import arc.audio.Music;
import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.audio.SoundControl;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
public class BNMusic extends SoundControl{
    private final Seq<Music> gameMusic = new Seq<>();
    private final Seq<Music> customBossMusic = new Seq<>();
    private int gameIndex = 0;
    private boolean bossPlaying = false;
    private boolean loaded = false;
    private boolean loading = false;
    private Music lastBossMusic;
    public BNMusic(){
        super();
        Events.on(ClientLoadEvent.class, e -> {
            if(!Vars.headless){
                loadMusic();
            }
        });
    }
    private void loadMusic(){
        if(Vars.headless || loading){
            return;
        }
        loading = true;
        Log.info("[BNMusic] Loading BNMusic V3...");
        gameMusic.clear();
        customBossMusic.clear();
        for(int i = 1; i <= 27; i++){
            loadGameMusic(i);
        }
        for(int i = 1; i <= 3; i++){
            loadBossMusic(i);
        }
    }
    private void loadGameMusic(int id){
        String path = "music/game" + id + ".ogg";
        if(!Core.files.internal(path).exists()){
            Log.warn("[BNMusic] Game music not found: @", path);
            return;
        }
        Core.assets.load(path, Music.class).loaded = music -> {
            if(music == null){
                Log.warn("[BNMusic] Failed to load game music: @", path);
                return;
            }
            music.setLooping(false);
            if(!gameMusic.contains(music, true)){
                gameMusic.add(music);
            }
            Log.info("[BNMusic] Loaded game music: @", path);
            if(!loaded && !gameMusic.isEmpty()){
                loaded = true;
                gameIndex = 0;
                Log.info("[BNMusic] BNMusic V3 music system ready. Game tracks: @, Boss tracks: @", gameMusic.size, customBossMusic.size);
            }
        };
    }
    private void loadBossMusic(int id){
        String path = "music/boss" + id + ".ogg";
        if(!Core.files.internal(path).exists()){
            Log.warn("[BNMusic] Boss music not found: @", path);
            return;
        }
        Core.assets.load(path, Music.class).loaded = music -> {
            if(music == null){
                Log.warn("[BNMusic] Failed to load boss music: @", path);
                return;
            }
            music.setLooping(true);
            if(!customBossMusic.contains(music, true)){
                customBossMusic.add(music);
            }
            Log.info("[BNMusic] Loaded boss music: @", path);
        };
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
        if(!Vars.headless){
            loadMusic();
        }
    }
    @Override
    public void update(){
        if(Vars.headless){
            return;
        }
        boolean playing = state.isGame();
        if(current != null && !current.isPlaying()){
            current = null;
            fade = 0f;
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
                play(state.rules.waveTeam != null ? state.rules.waveTeam == state.rules.defaultTeam ? Musics.menu : Musics.menu : Musics.menu);
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
        boolean bossFound = findBoss();
        if(bossFound){
            if(!bossPlaying){
                bossPlaying = true;
                playBossMusic();
            }else if(current == null || !current.isPlaying()){
                playBossMusic();
            }else if(current != null && !customBossMusic.contains(current, true)){
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
    private boolean findBoss(){
        for(Unit unit : Groups.unit){
            if(isBoss(unit)){
                return true;
            }
        }
        return false;
    }
    private boolean isBoss(Unit unit){
        if(unit == null || unit.type == null){
            return false;
        }
        if(unit.type.name == null){
            return false;
        }
        String name = unit.type.name.toLowerCase();
        return name.contains("boss");
    }
    private void playNextGameMusic(){
        if(gameMusic.isEmpty()){
            Log.warn("[BNMusic] No game music loaded.");
            silence();
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
        if(current == music && current.isPlaying()){
            return;
        }
        Log.info("[BNMusic] Playing game music: @ / @", gameIndex, gameMusic.size);
        play(music);
    }
    private void playBossMusic(){
        if(customBossMusic.isEmpty()){
            Log.warn("[BNMusic] Boss detected but no custom boss music is loaded.");
            return;
        }
        Music selected;
        if(customBossMusic.size == 1){
            selected = customBossMusic.first();
        }else{
            do{
                selected = customBossMusic.random();
            }while(selected == lastBossMusic);
        }
        lastBossMusic = selected;
        Log.info("[BNMusic] Boss music takeover.");
        play(selected);
    }
    @Override
    public void playRandom(){
        if(Vars.headless || !state.isGame()){
            return;
        }
        if(findBoss()){
            if(!bossPlaying){
                bossPlaying = true;
                playBossMusic();
            }
            return;
        }
        if(!bossPlaying){
            playNextGameMusic();
        }
    }
    @Override
    protected void playOnce(Music music){
        if(Vars.headless || music == null){
            return;
        }
        if(findBoss()){
            bossPlaying = true;
            if(!customBossMusic.contains(music, true)){
                playBossMusic();
            }else{
                play(music);
            }
        }
    }
    @Override
    protected void silence(){
        if(current != null){
            play(null);
        }
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
