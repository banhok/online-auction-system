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
import com.auction.common.model.Admin;
import com.auction.common.model.Art;
import com.auction.common.model.Bidder;
import com.auction.common.model.Electronics;
import com.auction.common.model.Item;
import com.auction.common.model.ItemCategory;
import com.auction.common.model.Seller;
import com.auction.common.model.User;
import com.auction.common.model.UserRole;
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
            .registerTypeAdapter(User.class, new UserDeserializer())
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
     * Custom deserializer cho User (abstract) — dispatch theo field `role` sang
     * Bidder / Seller / Admin. Server serialize runtime-type OK nên chỉ cần
     * xử lý lúc deserialize ở client (vd GET_ALL_USERS ở Admin Panel).
     */
    private static class UserDeserializer implements JsonDeserializer<User> {
        @Override
        public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonElement roleEl = json.getAsJsonObject().get("role");
            if (roleEl == null || roleEl.isJsonNull()) {
                throw new JsonParseException("User JSON thiếu field 'role'");
            }
            UserRole role = UserRole.valueOf(roleEl.getAsString());
            switch (role) {
                case BIDDER: return ctx.deserialize(json, Bidder.class);
                case SELLER: return ctx.deserialize(json, Seller.class);
                case ADMIN:  return ctx.deserialize(json, Admin.class);
                default:
                    throw new JsonParseException("Không hỗ trợ role: " + role);
            }
        }
    }

    /**
     * Custom TypeAdapter cho LocalDateTime.
     * Gson mặc định không hỗ trợ Java 8 time API.
     */

    
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        // Gson gọi write() khi cần serialize → LocalDateTimeAdapter "dịch" sang String
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }
        // Gson gọi read() khi cần deserialize → LocalDateTimeAdapter "dịch" ngược lại  
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
