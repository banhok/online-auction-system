package com.auction.common.dto;

import com.auction.common.command.CommandType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Request — đối tượng Client gửi tới Server qua Socket.
 * Chứa loại command và dữ liệu kèm theo (dạng key-value).
 * Được serialize thành JSON để truyền qua mạng.
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private CommandType command;
    private Map<String, Object> data;

    public Request() {
        this.data = new HashMap<>();
    }

    public Request(CommandType command) {
        this.command = command;
        this.data = new HashMap<>();
    }

    public Request(CommandType command, Map<String, Object> data) {
        this.command = command;
        this.data = data != null ? data : new HashMap<>();
    }

    public CommandType getCommand() {
        return command;
    }

    public void setCommand(CommandType command) {
        this.command = command;
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
    public Request put(String key, Object value) {
        this.data.put(key, value);
        return this; // Hỗ trợ method chaining
    }

    /**
     * Lấy giá trị theo key.
     */
    public Object get(String key) {
        return this.data.get(key);
    }

    /**
     * Lấy giá trị String theo key.
     */
    public String getString(String key) {
        Object val = this.data.get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * Lấy giá trị int theo key.
     */
    public int getInt(String key) {
        Object val = this.data.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return Integer.parseInt(val.toString());
    }

    /**
     * Lấy giá trị double theo key.
     */
    public double getDouble(String key) {
        Object val = this.data.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return Double.parseDouble(val.toString());
    }

    @Override
    public String toString() {
        return String.format("Request{command=%s, data=%s}", command, data);
    }
}
