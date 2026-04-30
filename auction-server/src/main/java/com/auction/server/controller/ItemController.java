package com.auction.server.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.Item;
import com.auction.common.util.JsonUtil;
import com.auction.server.service.ItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ItemController — xử lý các command liên quan đến Item (sản phẩm).
 */
public class ItemController {

    private final ItemService itemService = new ItemService();

    public Response handle(Request request) throws Exception {
        switch (request.getCommand()) {
            case CREATE_ITEM:
                return handleCreate(request);
            case UPDATE_ITEM:
                return handleUpdate(request);
            case DELETE_ITEM:
                return handleDelete(request);
            case GET_ITEM:
                return handleGetItem(request);
            case GET_ITEMS_BY_SELLER:
                return handleGetBySeller(request);
            case SEARCH_ITEMS:
                return handleSearch(request);
            default:
                return Response.error(request.getCommand(), "Unknown item command");
        }
    }

    @SuppressWarnings("unchecked")
    private Response handleCreate(Request req) throws Exception {
        int sellerId = req.getInt("_userId");
        Map<String, Object> extraData = null;
        Object extra = req.get("extraData");
        if (extra instanceof Map) {
            extraData = (Map<String, Object>) extra;
        }

        Item item = itemService.createItem(
                sellerId,
                req.getString("name"),
                req.getString("description"),
                req.getString("category"),
                req.getString("imageUrl"),
                req.getDouble("startingPrice"),
                extraData
        );

        Map<String, Object> data = new HashMap<>();
        data.put("item", JsonUtil.toJson(item));
        data.put("itemId", item.getId());
        return Response.ok(CommandType.CREATE_ITEM, "Tạo sản phẩm thành công", data);
    }

    private Response handleUpdate(Request req) throws Exception {
        int sellerId = req.getInt("_userId");
        Item item = itemService.updateItem(
                req.getInt("itemId"),
                sellerId,
                req.getString("name"),
                req.getString("description"),
                req.getString("imageUrl"),
                req.getDouble("startingPrice")
        );

        Map<String, Object> data = new HashMap<>();
        data.put("item", JsonUtil.toJson(item));
        return Response.ok(CommandType.UPDATE_ITEM, "Cập nhật sản phẩm thành công", data);
    }

    private Response handleDelete(Request req) throws Exception {
        int sellerId = req.getInt("_userId");
        itemService.deleteItem(req.getInt("itemId"), sellerId);
        return Response.ok(CommandType.DELETE_ITEM, "Xóa sản phẩm thành công");
    }

    private Response handleGetItem(Request req) throws Exception {
        Item item = itemService.getItem(req.getInt("itemId"));
        Map<String, Object> data = new HashMap<>();
        data.put("item", JsonUtil.toJson(item));
        // Polymorphism runtime: dispatch đến đúng override ở Electronics/Art/Vehicle
        data.put("categoryDetails", item.getCategoryDetails());
        return Response.ok(CommandType.GET_ITEM, "OK", data);
    }

    private Response handleGetBySeller(Request req) throws Exception {
        int sellerId = req.getInt("_userId");
        List<Item> items = itemService.getItemsBySeller(sellerId);
        Map<String, Object> data = new HashMap<>();
        data.put("items", JsonUtil.toJson(items));
        data.put("count", items.size());
        return Response.ok(CommandType.GET_ITEMS_BY_SELLER, "OK", data);
    }

    private Response handleSearch(Request req) throws Exception {
        List<Item> items = itemService.searchItems(
                req.getString("keyword"), req.getString("category"));
        Map<String, Object> data = new HashMap<>();
        data.put("items", JsonUtil.toJson(items));
        data.put("count", items.size());
        return Response.ok(CommandType.SEARCH_ITEMS, "OK", data);
    }
}
