package com.auction.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.Art;
import com.auction.common.model.Electronics;
import com.auction.common.model.Item;
import com.auction.common.model.ItemCategory;
import com.auction.common.model.Vehicle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JsonUtil — tiện ích chuyển đổi Object <-> JSON.
 * Dùng Gson với custom adapter cho LocalDateTime.
 * Sử dụng ở cả Client và Server để serialize/deserialize Request/Response.
 */
public final class JsonUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(Item.class, new ItemDeserializer())
            .setPrettyPrinting()
            .create();

    // Private constructor — không cho tạo instance (utility class)
    private JsonUtil() {
    }

    /**
     * Chuyển Object thành JSON string.
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * Chuyển JSON string thành Object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * Chuyển JSON string thành Request.
     */
    public static Request parseRequest(String json) {
        return GSON.fromJson(json, Request.class);
    }

    /**
     * Chuyển JSON string thành Response.
     */
    public static Response parseResponse(String json) {
        return GSON.fromJson(json, Response.class);
    }

    /**
     * Lấy Gson instance (dùng khi cần thao tác nâng cao).
     */
    public static Gson getGson() {
        return GSON;
    }

    /**
     * Custom deserializer cho Item (abstract) — dispatch theo field `category` sang
     * Electronics / Art / Vehicle. Server serialize runtime-type OK nên chỉ cần
     * xử lý lúc deserialize ở client.
     */
    private static class ItemDeserializer implements JsonDeserializer<Item> {
        @Override
        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonElement catEl = json.getAsJsonObject().get("category");
            if (catEl == null || catEl.isJsonNull()) {
                throw new JsonParseException("Item JSON thiếu field 'category'");
            }
            ItemCategory cat = ItemCategory.valueOf(catEl.getAsString());
            switch (cat) {
                case ELECTRONICS: return ctx.deserialize(json, Electronics.class);
                case ART:         return ctx.deserialize(json, Art.class);
                case VEHICLE:     return ctx.deserialize(json, Vehicle.class);
                default:
                    throw new JsonParseException("Không hỗ trợ category: " + cat);
            }
        }
    }

    /**
     * Custom TypeAdapter cho LocalDateTime.
     * Gson mặc định không hỗ trợ Java 8 time API.
     */
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateStr = in.nextString();
            return LocalDateTime.parse(dateStr, FORMATTER);
        }
    }
}
