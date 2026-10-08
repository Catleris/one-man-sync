package lt.oneman.sync;
/** Legacy Karamja Easy/Medium/Hard use state 2; other diary completion bits use 1. */
final class DiaryCompletion {
 private DiaryCompletion() {}
 static boolean complete(String area,String tier,int value) {
  boolean legacy="Karamja".equalsIgnoreCase(area)&&("Easy".equals(tier)||"Medium".equals(tier)||"Hard".equals(tier));
  return value==(legacy?2:1);
 }
}
