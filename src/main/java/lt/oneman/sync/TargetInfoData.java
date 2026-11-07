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
        final String element;
        final Integer percent;
        final Integer stab, slash, crush, light, standard, heavy, magic, hp, defence, magicLevel, speed;

        String melee() { return lowest(stab, slash, crush, "Stab", "Slash", "Crush"); }
        String ranged() { return lowest(light, standard, heavy, "Light", "Standard", "Heavy"); }

        private static String lowest(Integer first, Integer second, Integer third, String a, String b, String c)
        {
            if (first == null || second == null || third == null || (first.equals(second) && first.equals(third))) return null;
            int min = Math.min(first, Math.min(second, third));
            java.util.StringJoiner styles = new java.util.StringJoiner("/");
            if (first == min) styles.add(a);
            if (second == min) styles.add(b);
            if (third == min) styles.add(c);
            return styles + " " + min;
        }

        Stats(String[] cells)
        {
            element = cells[2];
            percent = number(cells[3]);
            stab = number(cells[4]);
            slash = number(cells[5]);
            crush = number(cells[6]);
            standard = number(cells[8]);
            heavy = number(cells[9]);
            light = number(cells[10]);
            magic = number(cells[7]);
            hp = number(cells[11]);
            defence = number(cells[12]);
            magicLevel = number(cells[13]);
            speed = number(cells[14]);
        }
    }

    private final Map<Integer,Stats> entries;
    TargetInfoData(){entries=load();}
    Stats find(int id){return entries.get(id);}
    int size(){return entries.size();}
    static Integer number(String value){try{return Integer.valueOf(value);}catch(NumberFormatException e){return null;}}
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
