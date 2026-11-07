package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Filepath;

/** Explicit opt-in passive screenshots; bounded work, no automatic retries or input. */
@Singleton
final class CompanionScreenshots {
    interface Directory { Filepath get() throws IOException; }
    @Inject private DrawManager draws;
    @Inject private OneManSyncConfig config;
    private volatile boolean active;
    private volatile String last="Capture is opt-in. Enable Save milestone screenshots in plugin settings.\nImages contain the visible game window. Local screenshots remain on your computer.";
    private Directory directory;
    private ExecutorService worker;
    private final AtomicInteger pending=new AtomicInteger();
    private volatile int generation;
    void start(Directory d) { directory=d;generation++;active=true;pending.set(0);worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"OneMan screenshots");t.setDaemon(true);return t;}); }
    void stop() { active=false;generation++;if(worker!=null)worker.shutdownNow(); }
    String status() { return last; }
    void capture(String profile,String milestone) {
        if(!active||!config.milestoneScreenshots())return;
        if(pending.incrementAndGet()>3){pending.decrementAndGet();last="Screenshot queue full; this capture was skipped.";return;}
        final int epoch=generation;final long captured=System.currentTimeMillis();
        draws.requestNextFrameListener(image->{
            if(!active||epoch!=generation)return;
            BufferedImage copy=new BufferedImage(image.getWidth(null),image.getHeight(null),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=copy.createGraphics();g.drawImage(image,0,0,null);g.dispose();
            try { worker.submit(()-> { try {
                if(!active||epoch!=generation||!config.milestoneScreenshots())return;
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(copy,"png",bytes);
                byte[] png=bytes.toByteArray();
                String folder=UUID.nameUUIDFromBytes(profile.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
                Filepath dir=directory.get().join("screenshots",folder);dir.createDirectories();
                Filepath file=dir.joinSegment(captured+"-"+UUID.randomUUID()+".png");file.write(png);
                last="Saved: "+file+"\nMilestone: "+milestone+"\nLocal only.";
            } catch(IOException | RuntimeException error) { last="Screenshot could not be saved. Local capture failed."; }
            finally { if(epoch==generation)pending.decrementAndGet(); } }); }
            catch(RejectedExecutionException ignored) { if(epoch==generation)pending.decrementAndGet(); }
        });
    }
}
