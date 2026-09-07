package magical;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import arc.audio.Sound;

import BNmusic.content.BNMusic;

public class BNmod extends Mod {
    public static Mods.LoadedMod ML;
    public static final String ModName = "BNmod";
    public static Mods.LoadedMod mod;
    public BNmod() {}
    public static String name(String add) {
        return ModName + "-" + add;
    }
    @Override
    public void loadContent() {
        mod = Vars.mods.getMod(this.getClass());

        BNMusic.load();
    }
}