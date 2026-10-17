package lt.oneman.sync;

import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingSeedCatalogTest
{
    private final FarmingSeedCatalog seeds=new FarmingSeedCatalog();
    @Test public void ordinarySeedsUseTheirOwnPatchTypes()
    {
        assertEquals("Herb",first("RANARR seed").category);
        assertEquals("Allotment",first("potato seed").category);
        assertEquals("Cactus",first("potato cactus seed").category);
        assertEquals("Hops",first("jute seed").category);
        assertEquals("Flower",first("limpwurt seed").category);
        assertEquals("Bush",first("poison ivy seed").category);
        assertEquals("Fruit Tree",first("dragonfruit tree seed").category);
    }
    @Test public void SaplingsAndUnusualPlantingMaterialsAreRecognised()
    {
        assertEquals("Tree",first("acorn").category);
        assertEquals("Tree",first("maple sapling").category);
        assertTrue(first("maple seed").note.contains("plant pot"));
        assertEquals("Hardwood",first("mahogany sapling").category);
        assertEquals("Anima",first("Attas seed").category);
        assertEquals("Seaweed",first("seaweed spore").category);
        assertEquals("Mushroom",first("mushroom spore").category);
        assertEquals("Herb",first("gout tuber").category);
        assertEquals("Hespori",first("hespori seed").category);
        assertEquals("Crystal Tree",first("crystal acorn").category);
        assertFalse(first("crystal acorn").note.contains("plant pot"));
    }
    @Test public void unrecognisedAndNonPlantableItemsAreNotGuessed()
    {
        for(String query:new String[]{"", "seed", "dark acorn", "crystal weapon seed", "this is not a seed"}) assertTrue(query,seeds.search(query).isEmpty());
    }
    private FarmingSeedCatalog.Seed first(String query)
    {
        assertFalse("Expected a verified crop match: "+query,seeds.search(query).isEmpty());return seeds.search(query).get(0);
    }
}
