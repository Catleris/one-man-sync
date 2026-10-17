package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/** Owns only OneMan's location pins. UI changes are scheduled onto the client thread. */
@Singleton
final class OneManMapLocations
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configs;
    @Inject private WorldMapPointManager manager;
    private volatile boolean active;
    private final Map<OneManMapCatalog.Group,List<WorldMapPoint>> pins=new EnumMap<>(OneManMapCatalog.Group.class);
    private final Map<OneManMapCatalog.Group,List<OneManMapCatalog.Site>> catalog=new EnumMap<>(OneManMapCatalog.Group.class);
    private final Map<OneManMapCatalog.Group,String> categories=new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<OneManMapCatalog.Group,Boolean> visible=new java.util.concurrent.ConcurrentHashMap<>();
    private volatile List<OneManMapCatalog.Site> minimapStashes=Collections.emptyList();
    private WorldPoint pendingFocus;
    private String selected="";

    synchronized void startUp()
    {
        catalog.put(OneManMapCatalog.Group.STASH,OneManMapCatalog.stashes());
        catalog.put(OneManMapCatalog.Group.FARMING,OneManMapCatalog.farming());
        for(OneManMapCatalog.Group group:OneManMapCatalog.Group.values()) {
            String category=configs.getConfiguration("one-man-sync",key(group,"Filter"));
            if(category==null || !categories(group).contains(category)) category=group==OneManMapCatalog.Group.STASH?"All":"Tree";
            categories.put(group,category);
            visible.put(group,!"false".equals(configs.getConfiguration("one-man-sync",key(group,"Pins"))));
        }
        selected=""; pendingFocus=null; active=true;
        clientThread.invokeLater(() -> { if(active) for(OneManMapCatalog.Group group:OneManMapCatalog.Group.values()) rebuild(group); });
    }
    synchronized void shutDown()
    {
        active=false; pendingFocus=null; minimapStashes=Collections.emptyList();
        for(OneManMapCatalog.Group group:OneManMapCatalog.Group.values()) clear(group);
    }
    List<String> categories(OneManMapCatalog.Group group)
    {
        List<String> result=new ArrayList<>();result.add("All");
        if(group==OneManMapCatalog.Group.STASH) for(ClueTier tier:ClueTier.values()) result.add(tier.label);
        else { Set<String> names=new TreeSet<>(); for(OneManMapCatalog.Site site:catalog.get(group)) names.add(site.category); result.addAll(names); }
        return result;
    }
    String category(OneManMapCatalog.Group group) { return categories.get(group); }
    boolean visible(OneManMapCatalog.Group group) { return Boolean.TRUE.equals(visible.get(group)); }
    List<OneManMapCatalog.Site> sites(OneManMapCatalog.Group group,String filter) { return OneManMapCatalog.filter(catalog.get(group),filter); }
    List<OneManMapCatalog.Site> minimapStashes() { return minimapStashes; }
    void configure(OneManMapCatalog.Group group,String category,boolean show)
    {
        clientThread.invokeLater(() -> {
            if(!active || !categories(group).contains(category)) return;
            categories.put(group,category); visible.put(group,show); selected="";pendingFocus=null;
            configs.setConfiguration("one-man-sync",key(group,"Filter"),category);
            configs.setConfiguration("one-man-sync",key(group,"Pins"),show);
            rebuild(group);
        });
    }
    void focus(OneManMapCatalog.Group group,OneManMapCatalog.Site site)
    {
        clientThread.invokeLater(() -> {
            if(!active || !sites(group,categories.get(group)).contains(site)) return;
            selected=site.id; pendingFocus=site.points.get(0); rebuild(group); tick();
        });
    }
    void tick()
    {
        WorldPoint target=pendingFocus;
        if(!active || target==null || client.getGameState()!=GameState.LOGGED_IN) return;
        Widget map=client.getWidget(InterfaceID.Worldmap.MAP_DISPLAY);
        if(map==null || map.isHidden() || client.getWorldMap()==null) return;
        if(active) client.getWorldMap().setWorldMapPositionTarget(target);
        if(pendingFocus==target) pendingFocus=null;
    }
    private static String key(OneManMapCatalog.Group group,String suffix) { return "map"+(group==OneManMapCatalog.Group.STASH?"Stash":"Farming")+suffix; }
    private synchronized void clear(OneManMapCatalog.Group group)
    {
        List<WorldMapPoint> old=pins.remove(group); if(old!=null) for(WorldMapPoint point:old) manager.remove(point);
    }
    private synchronized void rebuild(OneManMapCatalog.Group group)
    {
        if(!active) return;
        clear(group);
        List<OneManMapCatalog.Site> sites=visible(group)?sites(group,categories.get(group)):Collections.emptyList();
        if(group==OneManMapCatalog.Group.STASH) minimapStashes=sites;
        List<WorldMapPoint> points=new ArrayList<>();
        for(OneManMapCatalog.Site site:sites) for(WorldPoint location:site.points) {
            WorldMapPoint point=new WorldMapPoint(location,icon(site.color,group==OneManMapCatalog.Group.STASH?"S":"F",site.id.equals(selected)));
            point.setImagePoint(new net.runelite.api.Point(11,25));
            point.setName("OneMan "+site.category+": "+site.name);
            point.setTooltip((group==OneManMapCatalog.Group.STASH?"STASH • ":"Farming • ")+site.category+"\n"+site.name+"\nFloor "+location.getPlane());
            point.setJumpOnClick(true);point.setSnapToEdge(site.id.equals(selected));points.add(point);manager.add(point);
        }
        pins.put(group,points);
    }
    private static BufferedImage icon(Color color,String letter,boolean selected)
    {
        BufferedImage image=new BufferedImage(22,26,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(20,25,30,240));g.fillOval(1,1,20,20);
        g.setColor(color);g.setStroke(new BasicStroke(selected?3:2));g.drawOval(2,2,18,18);g.fillPolygon(new int[]{6,16,11},new int[]{18,18,25},3);
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));g.drawString(letter,7,15);g.dispose();return image;
    }
}
