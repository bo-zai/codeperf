package com.codeperf.demo.repo;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.domain.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 JdbcTemplate 的仓储。SQL 走 JDBC 层，便于 agent 在 JDBC 层拦截（ORM 无关）。
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> USER_MAPPER =
            (rs, i) -> new User(rs.getLong("id"), rs.getString("name"));

    private static final RowMapper<Order> ORDER_MAPPER =
            (rs, i) -> new Order(rs.getLong("id"), rs.getLong("user_id"),
                    rs.getString("item_name"), rs.getBigDecimal("amount"));

    public List<User> findAllUsers() {
        return jdbc.query("SELECT id, name FROM users", USER_MAPPER);
    }

    public User findUserById(long id) {
        List<User> list = jdbc.query("SELECT id, name FROM users WHERE id = ?", USER_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<Long, User> findUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object> args = new ArrayList<Object>(ids);
        String sql = "SELECT id, name FROM users WHERE id IN (" + placeholders(ids.size()) + ")";
        return jdbc.query(sql, USER_MAPPER, args.toArray()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    /** N+1 的源头：在循环里被逐用户调用。 */
    public List<Order> findOrdersByUserId(long userId) {
        return jdbc.query(
                "SELECT id, user_id, item_name, amount FROM orders WHERE user_id = ?",
                ORDER_MAPPER, userId);
    }

    public List<Order> findOrdersByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> args = new ArrayList<Object>(userIds);
        String sql = "SELECT id, user_id, item_name, amount FROM orders WHERE user_id IN ("
                + placeholders(userIds.size()) + ")";
        return jdbc.query(sql, ORDER_MAPPER, args.toArray());
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }
}
