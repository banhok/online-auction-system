package com.auction.server.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.util.DateTimeUtil;
import com.auction.common.util.JsonUtil;
import com.auction.server.dao.DepositDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.network.ClientHandler;
import com.auction.server.observer.AuctionEventManager;
import com.auction.server.service.AuctionService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AuctionController — xử lý các command liên quan đến Auction + Bidding.
 */
public class AuctionController {

    private final AuctionService auctionService;
    private final AuctionEventManager eventManager;
    private final DepositDAO depositDAO = new DepositDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();

    public AuctionController(AuctionEventManager eventManager) {
        this.eventManager = eventManager;
        this.auctionService = new AuctionService(eventManager);
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public Response handle(Request request) throws Exception {
        switch (request.getCommand()) {
            case CREATE_AUCTION:
                return handleCreate(request);
            case GET_AUCTION:
                return handleGetAuction(request);
            case GET_ALL_AUCTIONS:
                return handleGetAll(request);
            case GET_AUCTIONS_BY_STATUS:
                return handleGetByStatus(request);
            case GET_AUCTIONS_BY_SELLER:
                return handleGetBySeller(request);
            case GET_MY_WINS:
                return handleGetMyWins(request);
            case PLACE_BID:
                return handlePlaceBid(request);
            case GET_BID_HISTORY:
                return handleGetBidHistory(request);
            case JOIN_AUCTION:
                return handleJoinAuction(request);
            case PAY_WINNER:
                return handlePayWinner(request);
            case CANCEL_AUCTION:
                return handleCancelAuction(request);
            case SUBSCRIBE_AUCTION:
                return handleSubscribe(request);
            case UNSUBSCRIBE_AUCTION:
                return handleUnsubscribe(request);
            default:
                return Response.error(request.getCommand(), "Unknown auction command");
        }
    }

    private Response handleCreate(Request req) throws Exception {
        int sellerId = req.getInt("_userId");
        LocalDateTime startTime = DateTimeUtil.parseIso(req.getString("startTime"));
        LocalDateTime endTime = DateTimeUtil.parseIso(req.getString("endTime"));

        Auction auction = auctionService.createAuction(
                req.getInt("itemId"), sellerId,
                req.getDouble("startingPrice"),
                startTime, endTime
        );

        Map<String, Object> data = new HashMap<>();
        data.put("auction", JsonUtil.toJson(auction));
        data.put("auctionId", auction.getId());
        return Response.ok(CommandType.CREATE_AUCTION, "Tạo phiên đấu giá thành công", data);
    }

    private Response handleGetAuction(Request req) throws Exception {
        int auctionId = req.getInt("auctionId");
        Auction auction = auctionService.getAuction(auctionId);
        Map<String, Object> data = new HashMap<>();
        data.put("auction", JsonUtil.toJson(auction));
        data.put("participantCount", depositDAO.countHeldByAuction(auctionId));
        // Tên item hiển thị thay cho "Item #X"
        itemDAO.findById(auction.getItemId()).ifPresent(it ->
                data.put("itemName", it.getName()));
        return Response.ok(CommandType.GET_AUCTION, "OK", data);
    }

    private Response handleGetAll(Request req) throws Exception {
        List<Auction> auctions = auctionService.getAllAuctions();
        Map<String, Object> data = new HashMap<>();
        data.put("auctions", JsonUtil.toJson(auctions));
        data.put("count", auctions.size());
        data.put("itemNames", JsonUtil.toJson(buildItemNames(auctions)));
        return Response.ok(CommandType.GET_ALL_AUCTIONS, "OK", data);
    }

    private Response handleGetByStatus(Request req) throws Exception {
        List<Auction> auctions = auctionService.getAuctionsByStatus(req.getString("status"));
        Map<String, Object> data = new HashMap<>();
        data.put("auctions", JsonUtil.toJson(auctions));
        data.put("count", auctions.size());
        data.put("itemNames", JsonUtil.toJson(buildItemNames(auctions)));
        return Response.ok(CommandType.GET_AUCTIONS_BY_STATUS, "OK", data);
    }

    private Response handleGetBySeller(Request req) throws Exception {
        int sellerId = req.getInt("_userId");
        List<Auction> auctions = auctionService.getAuctionsBySeller(sellerId);
        Map<String, Object> data = new HashMap<>();
        data.put("auctions", JsonUtil.toJson(auctions));
        data.put("count", auctions.size());
        data.put("itemNames", JsonUtil.toJson(buildItemNames(auctions)));
        return Response.ok(CommandType.GET_AUCTIONS_BY_SELLER, "OK", data);
    }

    private Response handleGetMyWins(Request req) throws Exception {
        int userId = req.getInt("_userId");
        List<Auction> auctions = auctionService.getMyWins(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("auctions", JsonUtil.toJson(auctions));
        data.put("count", auctions.size());
        data.put("itemNames", JsonUtil.toJson(buildItemNames(auctions)));
        return Response.ok(CommandType.GET_MY_WINS, "OK", data);
    }

    /**
     * Build map itemId → tên item cho list auctions — client hiển thị thay cho "Item #X".
     * Bỏ qua item đã bị xoá (orphan).
     */
    private Map<Integer, String> buildItemNames(List<Auction> auctions) throws Exception {
        Map<Integer, String> names = new HashMap<>();
        for (Auction a : auctions) {
            if (names.containsKey(a.getItemId())) continue;
            itemDAO.findById(a.getItemId()).ifPresent(it ->
                    names.put(a.getItemId(), it.getName()));
        }
        return names;
    }

    /**
     * Build map userId → fullName (fallback username) cho list bid.
     */
    private Map<Integer, String> buildBidderNames(List<BidTransaction> bids) throws Exception {
        Map<Integer, String> names = new HashMap<>();
        for (BidTransaction b : bids) {
            if (names.containsKey(b.getBidderId())) continue;
            userDAO.findById(b.getBidderId()).ifPresent(u ->
                    names.put(b.getBidderId(),
                            u.getFullName() != null && !u.getFullName().isEmpty()
                                    ? u.getFullName() : u.getUsername()));
        }
        return names;
    }

    private Response handlePlaceBid(Request req) throws Exception {
        int bidderId = req.getInt("_userId");
        BidTransaction bid = auctionService.placeBid(
                req.getInt("auctionId"), bidderId, req.getDouble("bidAmount"));

        Map<String, Object> data = new HashMap<>();
        data.put("bid", JsonUtil.toJson(bid));
        data.put("bidId", bid.getId());
        return Response.ok(CommandType.PLACE_BID, "Đặt giá thành công", data);
    }

    private Response handleGetBidHistory(Request req) throws Exception {
        List<BidTransaction> bids = auctionService.getBidHistory(req.getInt("auctionId"));
        Map<String, Object> data = new HashMap<>();
        data.put("bids", JsonUtil.toJson(bids));
        data.put("count", bids.size());
        data.put("bidderNames", JsonUtil.toJson(buildBidderNames(bids)));
        return Response.ok(CommandType.GET_BID_HISTORY, "OK", data);
    }

    private Response handleJoinAuction(Request req) throws Exception {
        int userId = req.getInt("_userId");
        int auctionId = req.getInt("auctionId");
        auctionService.joinAuction(userId, auctionId);

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        return Response.ok(CommandType.JOIN_AUCTION, "Tham gia đấu giá thành công", data);
    }

    private Response handlePayWinner(Request req) throws Exception {
        int userId = req.getInt("_userId");
        int auctionId = req.getInt("auctionId");
        auctionService.payForWon(userId, auctionId);

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        return Response.ok(CommandType.PAY_WINNER, "Thanh toán thành công", data);
    }

    private Response handleCancelAuction(Request req) throws Exception {
        int userId = req.getInt("_userId");
        int auctionId = req.getInt("auctionId");
        auctionService.cancelAuction(userId, auctionId);

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auctionId);
        return Response.ok(CommandType.CANCEL_AUCTION, "Đã huỷ phiên đấu giá", data);
    }

    private Response handleSubscribe(Request req) throws Exception {
        int auctionId = req.getInt("auctionId");
        Object handlerObj = req.get("_handler");
        if (handlerObj instanceof ClientHandler) {
            eventManager.subscribe(auctionId, (ClientHandler) handlerObj);
        }
        return Response.ok(CommandType.SUBSCRIBE_AUCTION, "Đã đăng ký theo dõi phiên");
    }

    private Response handleUnsubscribe(Request req) throws Exception {
        int auctionId = req.getInt("auctionId");
        Object handlerObj = req.get("_handler");
        if (handlerObj instanceof ClientHandler) {
            eventManager.unsubscribe(auctionId, (ClientHandler) handlerObj);
        }
        return Response.ok(CommandType.UNSUBSCRIBE_AUCTION, "Đã hủy theo dõi phiên");
    }
}
