package BNmusic.content;
import arc.Core;
import arc.files.Fi;
import arc.scene.ui.Dialog;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.io.TypeIO;
import mindustry.world.Block;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
public class MapBuildingClipboard{
    private static final String FILE_NAME = "BNmod-map-buildings.bnb";
    private static final int VERSION = 1;
    private static final int MAGIC = 0x424E4D42;
    private static final Seq<BuildingData> clipboard = new Seq<>();
    private static class BuildingData{
        Block block;
        int x;
        int y;
        int rotation;
        Team team;
        Object config;
        BuildingData(Block block, int x, int y, int rotation, Team team, Object config){
            this.block = block;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.team = team;
            this.config = config;
        }
    }
    private static Fi file(){
        return Vars.dataDirectory.child(FILE_NAME);
    }
    public static void save(){
        if(!Vars.state.isGame()){
            toast("BNmod：当前不在游戏中");
            return;
        }
        clipboard.clear();
        IntSet counted = new IntSet();
        int width = Vars.world.width();
        int height = Vars.world.height();
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Building build = Vars.world.build(x, y);
                if(build == null) continue;
                if(!counted.add(build.pos())) continue;
                Block block = build.block;
                if(block == null) continue;
                if(block == mindustry.content.Blocks.air) continue;
                clipboard.add(new BuildingData(
                        block,
                        build.tileX(),
                        build.tileY(),
                        build.rotation,
                        build.team,
                        build.config()
                ));
            }
        }
        try{
            writeFile();
            toast("BNmod：已保存 " + clipboard.size + " 个建筑");
            Log.info("[BNmod] 已保存整图建筑：@ 个", clipboard.size);
        }catch(Throwable e){
            Log.err("[BNmod] 保存整图建筑失败", e);
            toast("BNmod：保存失败");
        }
    }
    public static void load(){
        if(!Vars.state.isGame()){
            toast("BNmod：当前不在游戏中");
            return;
        }
        if(!file().exists()){
            toast("BNmod：没有保存的建筑蓝图");
            return;
        }
        try{
            readFile();
            int loaded = 0;
            int skipped = 0;
            for(BuildingData data : clipboard){
                if(data.block == null){
                    skipped++;
                    continue;
                }
                if(data.x < 0 || data.y < 0 || data.x >= Vars.world.width() || data.y >= Vars.world.height()){
                    skipped++;
                    continue;
                }
                try{
                    clearArea(data);
                    Vars.world.tile(data.x, data.y).setBlock(
                            data.block,
                            data.team,
                            data.rotation
                    );
                    Building build = Vars.world.build(data.x, data.y);
                    if(build != null){
                        if(data.config != null){
                            build.configureAny(data.config);
                        }
                        loaded++;
                    }else{
                        skipped++;
                    }
                }catch(Throwable e){
                    skipped++;
                    Log.err("[BNmod] 加载建筑失败：@ @ @", data.block.name, data.x, data.y);
                    Log.err(e);
                }
            }
            toast("BNmod：加载 " + loaded + " 个，跳过 " + skipped + " 个");
            Log.info("[BNmod] 加载完成：成功 @，跳过 @", loaded, skipped);
        }catch(Throwable e){
            Log.err("[BNmod] 读取建筑蓝图失败", e);
            toast("BNmod：读取蓝图失败");
        }
    }
    private static void clearArea(BuildingData data){
        int size = data.block.size;
        int offset = size / 2;
        int minX = data.x - offset;
        int minY = data.y - offset;
        for(int x = minX; x < minX + size; x++){
            for(int y = minY; y < minY + size; y++){
                if(x < 0 || y < 0 || x >= Vars.world.width() || y >= Vars.world.height()) continue;
                Building old = Vars.world.build(x, y);
                if(old != null){
                    old.tile.setBlock(mindustry.content.Blocks.air);
                }
            }
        }
    }
    private static void writeFile() throws IOException{
        try(
                DataOutputStream output = new DataOutputStream(
                        new DeflaterOutputStream(
                                file().write(false, 4096)
                        )
                )
        ){
            Writes write = new Writes(output);
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(Vars.world.width());
            output.writeInt(Vars.world.height());
            output.writeInt(clipboard.size);
            for(BuildingData data : clipboard){
                output.writeShort(data.block.id);
                output.writeInt(data.x);
                output.writeInt(data.y);
                output.writeByte(data.rotation);
                output.writeByte(data.team == null ? 255 : data.team.id);
                TypeIO.writeObject(write, data.config);
            }
        }
    }
    private static void readFile() throws IOException{
        clipboard.clear();
        try(
                DataInputStream input = new DataInputStream(
                        new InflaterInputStream(
                                file().read(4096)
                        )
                )
        ){
            Reads read = new Reads(input);
            int magic = input.readInt();
            if(magic != MAGIC){
                throw new IOException("不是 BNmod 建筑蓝图");
            }
            int version = input.readInt();
            if(version != VERSION){
                throw new IOException("不支持的蓝图版本：" + version);
            }
            int savedWidth = input.readInt();
            int savedHeight = input.readInt();
            int amount = input.readInt();
            if(amount < 0 || amount > 10000000){
                throw new IOException("建筑数量异常：" + amount);
            }
            for(int i = 0; i < amount; i++){
                int blockId = input.readUnsignedShort();
                Block block = Vars.content.block(blockId);
                int x = input.readInt();
                int y = input.readInt();
                int rotation = input.readUnsignedByte();
                int teamId = input.readUnsignedByte();
                Team team = teamId == 255 ? Team.derelict : Team.get(teamId);
                Object config = TypeIO.readObject(read);
                if(block != null){
                    clipboard.add(new BuildingData(
                            block,
                            x,
                            y,
                            rotation,
                            team,
                            config
                    ));
                }
            }
            Log.info(
                    "[BNmod] 蓝图读取完成：原地图 @x@，当前地图 @x@，建筑 @",
                    savedWidth,
                    savedHeight,
                    Vars.world.width(),
                    Vars.world.height(),
                    clipboard.size
            );
        }
    }
    public static boolean exists(){
        return file().exists();
    }
    public static Fi getFile(){
        return file();
    }
    public static void delete(){
        if(file().exists()){
            file().delete();
        }
        clipboard.clear();
        toast("BNmod：已删除保存的蓝图");
    }
    private static void toast(String text){
        Log.info(text);
        if(Vars.ui != null && Vars.ui.hudfrag != null){
            Vars.ui.hudfrag.showToast(text);
        }
    }
    public static void showDialog(){
        if(Core.scene == null) return;
        Dialog dialog = new Dialog("BNmod 建筑蓝图");
        dialog.cont.clear();
        dialog.cont.defaults().growX().height(65f).pad(6f);
        dialog.cont.button("复制并保存整图", () -> {
            dialog.hide();
            save();
        });
        dialog.cont.row();
        dialog.cont.button("加载整图", () -> {
            dialog.hide();
            load();
        });
        dialog.cont.row();
        dialog.cont.button("删除保存的蓝图", () -> {
            dialog.hide();
            delete();
        });
        dialog.cont.row();
        dialog.cont.button("查看蓝图状态", () -> {
            toast(
                    exists()
                            ? "BNmod：已有保存的整图蓝图"
                            : "BNmod：没有保存的蓝图"
            );
        });
        dialog.addCloseButton();
        dialog.show();
    }
}