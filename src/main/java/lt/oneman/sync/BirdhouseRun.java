package lt.oneman.sync;

import net.runelite.api.gameval.ItemID;

/** Presentation snapshot of the existing Time Tracking observation and 50-minute estimate. */
final class BirdhouseRun
{
    static final long DURATION=3000;
    static final String[] LOCATIONS={"Mushroom Meadow N","Mushroom Meadow S","Verdant Valley NE","Verdant Valley SW"};
    private static final int[] ITEMS={ItemID.BIRDHOUSE_NORMAL,ItemID.BIRDHOUSE_OAK,ItemID.BIRDHOUSE_WILLOW,ItemID.BIRDHOUSE_TEAK,
        ItemID.BIRDHOUSE_MAPLE,ItemID.BIRDHOUSE_MAHOGANY,ItemID.BIRDHOUSE_YEW,ItemID.BIRDHOUSE_MAGIC,ItemID.BIRDHOUSE_REDWOOD};
    enum Status {UNKNOWN,EMPTY,NEEDS_SEEDS,GROWING,READY}
    final String location,house;final Status status;final long remaining;final int item;
    private BirdhouseRun(String location,String house,Status status,long remaining,int item){
        this.location=location;this.house=house;this.status=status;this.remaining=remaining;this.item=item;
    }
    static BirdhouseRun unknown(int index){return new BirdhouseRun(LOCATIONS[index],"",Status.UNKNOWN,0,ITEMS[0]);}
    static BirdhouseRun read(int index,String raw,long now){
        if(raw==null)return unknown(index);String[] parts=raw.split(":",-1);if(parts.length!=2)return unknown(index);
        int varp;long seen;try{varp=Integer.parseInt(parts[0]);seen=Long.parseLong(parts[1]);}catch(NumberFormatException error){return unknown(index);}
        BirdHouseDecoder.State state=BirdHouseDecoder.getState(varp);if(seen<=0||state==BirdHouseDecoder.State.UNKNOWN)return unknown(index);
        if(state==BirdHouseDecoder.State.EMPTY)return new BirdhouseRun(LOCATIONS[index],"",Status.EMPTY,0,ITEMS[0]);
        String house=BirdHouseDecoder.getHouseName(varp);int item=ITEMS[(varp-1)/3];
        if(state==BirdHouseDecoder.State.BUILT)return new BirdhouseRun(LOCATIONS[index],house,Status.NEEDS_SEEDS,0,item);
        long remaining=Math.max(0,seen+DURATION-now);
        return new BirdhouseRun(LOCATIONS[index],house,remaining==0?Status.READY:Status.GROWING,remaining,item);
    }
    String label(){switch(status){case EMPTY:return "Empty";case NEEDS_SEEDS:return "Needs seeds";case GROWING:return "Growing";case READY:return "Estimated ready";default:return "Not checked";}}
    String time(){return remaining/60+"m "+remaining%60+"s left";}
    int progress(){return status==Status.READY?100:status==Status.GROWING?(int)Math.max(0,Math.min(100,(DURATION-remaining)*100/DURATION)):0;}
}
