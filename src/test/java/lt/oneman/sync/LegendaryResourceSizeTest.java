package lt.oneman.sync;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegendaryResourceSizeTest {
    @Test public void bundledArtStaysUnderOneMiBDecodedPerImage() throws Exception {
        for(String name:new String[]{"legendary-collection.png","legendary-combat.png","legendary-quest-window.png"}){
            try(InputStream stream=getClass().getResourceAsStream(name)){
                assertNotNull(name,stream);
                BufferedImage image=ImageIO.read(stream);
                assertNotNull(name,image);
                assertTrue(name,(long)image.getWidth()*image.getHeight()*4<1024*1024);
            }
        }
        new LegendaryAlarmArt();
    }
}
