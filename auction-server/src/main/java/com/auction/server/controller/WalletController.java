package com.auction.server.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.WalletTransaction;
import com.auction.common.util.JsonUtil;
import com.auction.server.observer.AuctionEventManager;
import com.auction.server.service.WalletService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WalletController — xử lý các command liên quan đến Ví tiền.
 */
public class WalletController {

    private final WalletService walletService;

    public WalletController(AuctionEventManager eventManager) {
        this.walletService = new WalletService(eventManager);
    }

    public Response handle(Request request) throws Exception {
        switch (request.getCommand()) {
            case GET_BALANCE:
                return handleGetBalance(request);
            case TOP_UP:
                return handleTopUp(request);
            case WITHDRAW:
                return handleWithdraw(request);
            case GET_WALLET_HISTORY:
                return handleGetHistory(request);
            default:
                return Response.error(request.getCommand(), "Unknown wallet command");
        }
    }

    private Response handleGetBalance(Request req) throws Exception {
        int userId = req.getInt("_userId");
        double balance = walletService.getBalance(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("balance", balance);
        return Response.ok(CommandType.GET_BALANCE, "OK", data);
    }

    private Response handleTopUp(Request req) throws Exception {
        int userId = req.getInt("_userId");
        double amount = req.getDouble("amount");
        double newBalance = walletService.topUp(userId, amount);
        Map<String, Object> data = new HashMap<>();
        data.put("balance", newBalance);
        return Response.ok(CommandType.TOP_UP,
                String.format("Nạp thành công %.2f", amount), data);
    }

    private Response handleWithdraw(Request req) throws Exception {
        int userId = req.getInt("_userId");
        double amount = req.getDouble("amount");
        double newBalance = walletService.withdraw(userId, amount);
        Map<String, Object> data = new HashMap<>();
        data.put("balance", newBalance);
        return Response.ok(CommandType.WITHDRAW,
                String.format("Rút thành công %.2f", amount), data);
    }

    private Response handleGetHistory(Request req) throws Exception {
        int userId = req.getInt("_userId");
        List<WalletTransaction> history = walletService.getHistory(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("transactions", JsonUtil.toJson(history));
        data.put("count", history.size());
        return Response.ok(CommandType.GET_WALLET_HISTORY, "OK", data);
    }
}
