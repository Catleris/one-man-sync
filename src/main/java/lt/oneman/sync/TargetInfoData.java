package lt.oneman.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Bundled reference stats; never guesses a variant by name or fetches data in combat. */
final class TargetInfoData
{
    static final class Stats
    {
        final String variant, element;
        final Integer percent, stab, slash, crush, magic, standard, heavy, light, hp, defence, magicLevel, speed;
        Stats(String[] c) {
            variant=c[1];element=c[2];percent=number(c[3]);stab=number(c[4]);slash=number(c[5]);
            crush=number(c[6]);magic=number(c[7]);standard=number(c[8]);heavy=number(c[9]);
            light=number(c[10]);hp=number(c[11]);defence=number(c[12]);magicLevel=number(c[13]);speed=number(c[14]);
        }
        String elementText(){return !"?".equals(element)&&percent!=null&&percent>0 ? element+" +"+percent+"%" : "Not listed";}
        // Lowest melee defence BONUS is reference information, not a DPS recommendation.
        String meleeText(){
            if(stab==null||slash==null||crush==null)return "Unknown";
            int min=Math.min(stab,Math.min(slash,crush));
            if(stab.equals(slash)&&stab.equals(crush))return "Equal bonuses";
            String value="";
            if(stab==min)value="Stab";
            if(slash==min)value+=(value.isEmpty()?"":" / ")+"Slash";
            if(crush==min)value+=(value.isEmpty()?"":" / ")+"Crush";
            return value;
        }
    }
    private final Map<Integer,Stats> entries;
    TargetInfoData(){entries=load();}
    Stats find(int id){return entries.get(id);}
    int size(){return entries.size();}
    static Integer number(String value){try{return Integer.valueOf(value);}catch(NumberFormatException e){return null;}}
    static String value(Integer value){return value==null?"?":value.toString();}
    private static Map<Integer,Stats> load(){
        Map<Integer,Stats> entries=new HashMap<>();
        try(InputStream input=TargetInfoData.class.getResourceAsStream("target-info.tsv")){
            if(input==null)return Collections.emptyMap();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
                String line;
                while((line=reader.readLine())!=null){
                    if(line.startsWith("#"))continue;
                    String[] cells=line.split("\t",-1);
                    if(cells.length!=15)continue;
                    Integer id=number(cells[0]);if(id!=null)entries.put(id,new Stats(cells));
                }
            }
        }catch(IOException e){return Collections.emptyMap();}
        return Collections.unmodifiableMap(entries);
    }
}
