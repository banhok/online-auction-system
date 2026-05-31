package com.auction.common.dto;

import com.auction.common.command.CommandType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Response — đối tượng Server gửi lại cho Client qua Socket.
 * Chứa loại command, trạng thái thành công/thất bại, message, và dữ liệu.
 * Được serialize thành JSON để truyền qua mạng.
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private CommandType command;
    private boolean success;
    private String message;
    private Map<String, Object> data;

    public Response() {
        this.data = new HashMap<>();
    }

    public Response(CommandType command, boolean success, String message) {
        this.command = command;
        this.success = success;
        this.message = message;
        this.data = new HashMap<>();
    }

    public Response(CommandType command, boolean success, String message, Map<String, Object> data) {
        this.command = command;
        this.success = success;
        this.message = message;
        this.data = data != null ? data : new HashMap<>();
    }

    // --- Factory methods tiện dụng ---

    /**
     * Tạo response thành công.
     */
    public static Response ok(CommandType command, String message) {
        return new Response(command, true, message);
    }

    /**
     * Tạo response thành công kèm data.
     */
    public static Response ok(CommandType command, String message, Map<String, Object> data) {
        return new Response(command, true, message, data);
    }

    /**
     * Tạo response thất bại.
     */
    public static Response error(CommandType command, String message) {
        return new Response(command, false, message);
    }

    // --- Getter / Setter ---

    public CommandType getCommand() {
        return command;
    }

    public void setCommand(CommandType command) {
        this.command = command;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * Thêm 1 cặp key-value vào data.
     */
    public Response put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * Lấy giá trị theo key.
     */
    public Object get(String key) {
        return this.data.get(key);
    }

    public String getString(String key) {
        Object val = this.data.get(key);
        return val != null ? val.toString() : null;
    }

    public int getInt(String key) {
        Object val = this.data.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return Integer.parseInt(val.toString());
    }

    public double getDouble(String key) {
        Object val = this.data.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return Double.parseDouble(val.toString());
    }

    @Override
    public String toString() {
        return String.format("Response{command=%s, success=%s, message='%s', data=%s}",
                command, success, message, data);
    }
}
