package lt.oneman.sync;
import com.google.gson.*;
/** Shows only a bounded JSON error field, never an HTML page, response payload or sync token. */
final class CompanionSyncError {
    static String describe(Gson gson,String body) {
        try {
            JsonObject json=gson.fromJson(body,JsonObject.class);if(json==null||!json.has("error")||!json.get("error").isJsonPrimitive())return "";
            String error=json.get("error").getAsString().replaceAll("[\\r\\n\\x00-\\x1f]"," ")
                .replaceAll("(?i)Bearer\\s+\\S+","Bearer [redacted]").replaceAll("(?i)(token|password|syncKey)\\s*[:=]\\s*\\S+","$1=[redacted]");
            return " — "+error.substring(0,Math.min(240,error.length()));
        } catch(RuntimeException ignored){return "";}
    }
}
