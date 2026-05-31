package com.auction.server.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.exception.AuctionException;
import com.auction.common.exception.AuthenticationException;
import com.auction.common.exception.NotFoundException;
import com.auction.server.observer.AuctionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommandRouter — Command pattern.
 * Nhận Request, dựa vào CommandType để route tới Controller tương ứng.
 * Mỗi Controller xử lý 1 nhóm chức năng.
 */
public class CommandRouter {

    private static final Logger logger = LoggerFactory.getLogger(CommandRouter.class);

    private final UserController userController;
    private final ItemController itemController;
    private final AuctionController auctionController;
    private final WalletController walletController;

    public CommandRouter(AuctionEventManager eventManager) {
        this.userController = new UserController();
        this.itemController = new ItemController();
        this.auctionController = new AuctionController(eventManager);
        this.walletController = new WalletController(eventManager);
    }

    public AuctionController getAuctionController() {
        return auctionController;
    }

    /**
     * Route request tới controller phù hợp.
     */
    public Response route(Request request) {
        CommandType cmd = request.getCommand();
        if (cmd == null) {
            return Response.error(null, "Command không hợp lệ");
        }

        try {
            switch (cmd) {
                // --- User ---
                case LOGIN:
                case REGISTER:
                case GET_PROFILE:
                case UPDATE_PROFILE:
                case CHANGE_PASSWORD:
                case GET_ALL_USERS:
                case DELETE_USER:
                    return userController.handle(request);

                // --- Wallet ---
                case GET_BALANCE:
                case TOP_UP:
                case WITHDRAW:
                case GET_WALLET_HISTORY:
                    return walletController.handle(request);

                // --- Item ---
                case CREATE_ITEM:
                case UPDATE_ITEM:
                case DELETE_ITEM:
                case GET_ITEM:
                case GET_ITEMS_BY_SELLER:
                case SEARCH_ITEMS:
                    return itemController.handle(request);

                // --- Auction ---
                case CREATE_AUCTION:
                case GET_AUCTION:
                case GET_ALL_AUCTIONS:
                case GET_AUCTIONS_BY_STATUS:
                case GET_AUCTIONS_BY_SELLER:
                case GET_MY_WINS:
                case PLACE_BID:
                case GET_BID_HISTORY:
                case JOIN_AUCTION:
                case PAY_WINNER:
                case CANCEL_AUCTION:
                case SUBSCRIBE_AUCTION:
                case UNSUBSCRIBE_AUCTION:
                    return auctionController.handle(request);

                // --- System ---
                case PING:
                    return Response.ok(CommandType.PONG, "pong");

                default:
                    return Response.error(cmd, "Command chưa được hỗ trợ: " + cmd);
            }
            //Sai mk, chưa đăng nhập, ko có quyền
        } catch (AuthenticationException e) {
            return Response.error(cmd, "AUTH: " + e.getMessage());
            
            //Phiên ko tồn tại, ko thấy user
        } catch (NotFoundException e) {
            return Response.error(cmd, "NOT_FOUND: " + e.getMessage());
            
            //Logic phiên
        } catch (AuctionException e) {
            // InvalidBidException, InsufficientBalanceException, ...
            return Response.error(cmd, e.getMessage());
            
            //Dữ liệu vào sai
        } catch (IllegalArgumentException e) {
            return Response.error(cmd, "VALIDATION: " + e.getMessage());
            
            //Lỗi khác
        } catch (Exception e) {
            logger.error("Error handling command: {}", cmd, e);
            return Response.error(cmd, "SERVER ERROR: " + e.getMessage());
        }
    }
}
