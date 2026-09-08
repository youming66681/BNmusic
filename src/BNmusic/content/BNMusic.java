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
                music.setLooping(true);
                if(!customBossMusic.contains(music, true)){
                    customBossMusic.add(music);
                }
                Log.info("[BNMusic] Loaded boss music: @", path);
            };
        }
        Events.fire(new MusicRegisterEvent());
    }
}
