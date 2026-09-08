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
    private static boolean bossPlaying = false;
    private static int gameIndex = 0;
    private static boolean bossFound = false;
    public static void load(){
        if(Vars.headless)return;
        loadGameMusic();
        loadBossMusic();
        Events.run(EventType.Trigger.update,()->{
            if(!Vars.headless)update();
        });
        playNextGame();
    }
    private static void loadGameMusic(){
        for(int i = 1;i <= 27;i++){
            try{
                Music music = Core.assets.load("music/game"+i+".ogg",Music.class).get();
                music.setLooping(false);
                gameMusic.add(music);
            }catch(Exception e){
                Log.err("无法加载游戏音乐 game"+i);
            }
        }
    }
    private static void loadBossMusic(){
        for(int i = 1;i <= 3;i++){
            try{
                Music music = Core.assets.load("music/boss"+i+".ogg",Music.class).get();
                music.setLooping(true);
                bossMusic.add(music);
            }catch(Exception e){
                Log.err("无法加载Boss音乐 boss"+i);
            }
        }
    }
    private static void update(){
        bossFound = false;
        Groups.unit.each(unit->{
            if(isBoss(unit)){
                bossFound = true;
            }
        });
        if(bossFound&&!bossPlaying){
            bossPlaying = true;
            playBoss();
        }
        if(!bossFound&&bossPlaying){
            bossPlaying = false;
            playNextGame();
        }
        if(currentMusic!=null&&!currentMusic.isPlaying()&&!bossPlaying){
            playNextGame();
        }
    }
    private static boolean isBoss(Unit unit){
        return unit.type.name.contains("boss");
    }
    private static void playNextGame(){
        if(gameMusic.isEmpty())return;
        stopCurrent();
        if(gameIndex>=gameMusic.size){
            gameIndex=0;
        }
        currentMusic=gameMusic.get(gameIndex);
        gameIndex++;
        currentMusic.setLooping(false);
        currentMusic.play();
    }
    private static void playBoss(){
        if(bossMusic.isEmpty())return;
        stopCurrent();
        int index=(int)(Math.random()*bossMusic.size);
        currentMusic=bossMusic.get(index);
        currentMusic.setLooping(true);
        currentMusic.play();
    }
    private static void stopCurrent(){
        if(currentMusic!=null){
            currentMusic.stop();
        }
    }
}