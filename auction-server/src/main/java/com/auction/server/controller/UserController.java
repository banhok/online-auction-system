package com.auction.server.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.User;
import com.auction.common.util.JsonUtil;
import com.auction.server.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserController — xử lý các command liên quan đến User.
 */
public class UserController {

    private final UserService userService = new UserService();

    public Response handle(Request request) throws Exception {
        switch (request.getCommand()) {
            case LOGIN:
                return handleLogin(request);
            case REGISTER:
                return handleRegister(request);
            case GET_PROFILE:
                return handleGetProfile(request);
            case UPDATE_PROFILE:
                return handleUpdateProfile(request);
            case CHANGE_PASSWORD:
                return handleChangePassword(request);
            case GET_ALL_USERS:
                return handleGetAllUsers(request);
            case DELETE_USER:
                return handleDeleteUser(request);
            default:
                return Response.error(request.getCommand(), "Unknown user command");
        }
    }

    private Response handleLogin(Request req) throws Exception {
        User user = userService.login(req.getString("username"), req.getString("password"));
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("user", JsonUtil.toJson(user));
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("role", user.getRole().name());
        data.put("balance", user.getBalance());
        return Response.ok(CommandType.LOGIN, "Đăng nhập thành công", data);
    }

    private Response handleRegister(Request req) throws Exception {
        User user = userService.register(
                req.getString("username"),
                req.getString("password"),
                req.getString("fullName"),
                req.getString("email"),
                req.getString("role")
        );
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole().name());
        return Response.ok(CommandType.REGISTER, "Đăng ký thành công", data);
    }

    private Response handleGetProfile(Request req) throws Exception {
        int userId = req.getInt("_userId");
        User user = userService.getProfile(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("user", JsonUtil.toJson(user));
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole().name());
        data.put("balance", user.getBalance());
        return Response.ok(CommandType.GET_PROFILE, "OK", data);
    }

    private Response handleUpdateProfile(Request req) throws Exception {
        int userId = req.getInt("_userId");
        User user = userService.updateProfile(userId,
                req.getString("fullName"), req.getString("email"));
        Map<String, Object> data = new HashMap<>();
        data.put("user", JsonUtil.toJson(user));
        return Response.ok(CommandType.UPDATE_PROFILE, "Cập nhật thành công", data);
    }

    private Response handleChangePassword(Request req) throws Exception {
        int userId = req.getInt("_userId");
        userService.changePassword(userId,
                req.getString("oldPassword"), req.getString("newPassword"));
        return Response.ok(CommandType.CHANGE_PASSWORD, "Đổi mật khẩu thành công");
    }

    private Response handleGetAllUsers(Request req) throws Exception {
        List<User> users = userService.getAllUsers();
        Map<String, Object> data = new HashMap<>();
        data.put("users", JsonUtil.toJson(users));
        data.put("count", users.size());
        return Response.ok(CommandType.GET_ALL_USERS, "OK", data);
    }

    private Response handleDeleteUser(Request req) throws Exception {
        int targetId = req.getInt("targetUserId");
        userService.deleteUser(targetId);
        return Response.ok(CommandType.DELETE_USER, "Xóa user thành công");
    }
}
