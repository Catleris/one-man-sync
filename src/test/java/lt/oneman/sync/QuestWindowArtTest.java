package lt.oneman.sync;

import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuestWindowArtTest {
    @Test public void backingNeverExtendsAboveOrBelowGoldRails(){
        QuestWindowArt art=new QuestWindowArt();
        for(int[] size:new int[][]{{1536,1024},{488,334},{976,668}}){
            BufferedImage image=art.background(size[0],size[1]);
            int x=size[0]*400/1536;
            assertEquals("Above gold rail",0,image.getRGB(x,size[1]*100/1024)>>>24);
            assertEquals("Below gold rail",0,image.getRGB(x,size[1]*940/1024)>>>24);
            assertEquals("Solid interior",255,image.getRGB(size[0]/2,size[1]/2)>>>24);
        }
    }
}
