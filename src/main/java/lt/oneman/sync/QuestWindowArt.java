package lt.oneman.sync;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
/** Local texture only. Native OSRS widgets continue to supply real completion content. */
final class QuestWindowArt {
    private final BufferedImage source;
    QuestWindowArt(){
        try(InputStream stream=getClass().getResourceAsStream("legendary-quest-window.png")){
            if(stream==null)throw new IllegalStateException("Missing quest-window texture");
            source=ImageIO.read(stream);if(source==null)throw new IllegalStateException("Invalid quest-window texture");
        }catch(IOException error){throw new IllegalStateException("Cannot read quest-window texture",error);}
    }
    BufferedImage background(int width,int height){
        BufferedImage result=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Graphics2D g=result.createGraphics();
        try{g.setColor(new Color(23,23,22));g.fillRect(width*8/100,height*15/100,width*84/100,height*70/100);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,width,height,null);}finally{g.dispose();}
        return result;
    }
}
