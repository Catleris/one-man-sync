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
import okhttp3.*;

/** Explicit opt-in passive screenshots; bounded work, no automatic retries or input. */
@Singleton
final class CompanionScreenshots {
    interface Directory { Filepath get() throws IOException; }
    @Inject private DrawManager draws;
    @Inject private OneManSyncConfig config;
    @Inject private OkHttpClient http;
    private volatile boolean active;
    private volatile String last="Capture is opt-in. Enable Save milestone screenshots in plugin settings. Upload has a separate opt-in.\nImages contain the visible game window. Local screenshots remain on your computer.";
    private Directory directory;
    private ExecutorService worker;
    private final Set<Call> calls=ConcurrentHashMap.newKeySet();
    private final AtomicInteger pending=new AtomicInteger();
    private volatile int generation;
    void start(Directory d) { directory=d;generation++;active=true;pending.set(0);worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"OneMan screenshots");t.setDaemon(true);return t;}); }
    void stop() { active=false;generation++;calls.forEach(Call::cancel);calls.clear();if(worker!=null)worker.shutdownNow(); }
    String status() { return last; }
    void capture(String player,String profile,String milestone) {
        if(!active||!config.milestoneScreenshots())return;
        if(pending.incrementAndGet()>3){pending.decrementAndGet();last="Screenshot queue full; this capture was skipped.";return;}
        final int epoch=generation;final long captured=System.currentTimeMillis();
        // Capture the consent and token for this player; never switch recipients later.
        final boolean upload=config.uploadMilestoneScreenshots()&&config.enabled();final String token=config.syncKey().trim();
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
                last="Saved: "+file+"\nMilestone: "+milestone+"\n"+(upload?"Upload queued.":"Local only.");
                if(upload&&!token.isEmpty()&&active&&epoch==generation&&config.uploadMilestoneScreenshots()&&config.enabled()&&config.syncKey().trim().equals(token))send(png,player,milestone,captured,token,epoch);
            } catch(IOException | RuntimeException error) { last="Screenshot could not be saved. No upload was sent."; }
            finally { if(epoch==generation)pending.decrementAndGet(); } }); }
            catch(RejectedExecutionException ignored) { if(epoch==generation)pending.decrementAndGet(); }
        });
    }
    private void send(byte[] png,String player,String milestone,long captured,String token,int epoch) {
        if(png.length>5*1024*1024){last+="\nUpload skipped: image exceeds 5 MB. Local copy retained.";return;}
        MultipartBody body=new MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("player",player).addFormDataPart("milestone",milestone)
            .addFormDataPart("capturedEpochMs",Long.toString(captured))
            .addFormDataPart("screenshot","milestone.png",RequestBody.create(MediaType.parse("image/png"),png)).build();
        Call call=http.newCall(new Request.Builder().url("https://oneman.lt/runelite_memory_upload.php")
            .header("Authorization","Bearer "+token).post(body).build());calls.add(call);
        if(!active||epoch!=generation){calls.remove(call);call.cancel();return;}
        call.enqueue(new Callback(){
            public void onFailure(Call c,IOException error) { calls.remove(c);if(active&&epoch==generation)last+="\nUpload failed; local copy retained. No automatic retry."; }
            public void onResponse(Call c,Response response) { try(Response r=response){if(active&&epoch==generation)last+=r.isSuccessful()?"\nUploaded to OneMan.":"\nOneMan rejected upload (HTTP "+r.code()+"); local copy retained.";}finally{calls.remove(c);} }
        });
    }
}
