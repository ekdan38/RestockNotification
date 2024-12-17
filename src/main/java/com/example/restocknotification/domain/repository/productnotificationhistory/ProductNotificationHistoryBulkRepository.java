package com.example.restocknotification.domain.repository.productnotificationhistory;

import com.example.restocknotification.domain.entity.ProductNotificationHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductNotificationHistoryBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    public void saveAll(List<ProductNotificationHistory> lists){
        String sql = "INSERT INTO product_notification_history (product_id, stock_round, notification_status, user_id) " +
                "VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ProductNotificationHistory value = lists.get(i);
                ps.setLong(1, value.getProduct().getId());
                ps.setInt(2, value.getStockRound());
                ps.setString(3, value.getNotificationStatus().name());

                // userId null 처리
                if(value.getUserId() != null) ps.setLong(4, value.getUserId());
                else ps.setLong(4, 1L);
            }

            @Override
            public int getBatchSize() {
                return lists.size();
            }
        });
    }

}
