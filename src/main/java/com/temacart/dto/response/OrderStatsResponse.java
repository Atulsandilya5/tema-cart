package com.temacart.dto.response;

import java.math.BigDecimal;

public class OrderStatsResponse {

    private Long totalOrders;
    private Long pendingOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal deliveredRevenue;

    public OrderStatsResponse() {
    }

    public OrderStatsResponse(Long totalOrders, Long pendingOrders, Long deliveredOrders,
                              Long cancelledOrders, BigDecimal totalRevenue, BigDecimal deliveredRevenue) {
        this.totalOrders = totalOrders;
        this.pendingOrders = pendingOrders;
        this.deliveredOrders = deliveredOrders;
        this.cancelledOrders = cancelledOrders;
        this.totalRevenue = totalRevenue;
        this.deliveredRevenue = deliveredRevenue;
    }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(Long pendingOrders) { this.pendingOrders = pendingOrders; }

    public Long getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(Long deliveredOrders) { this.deliveredOrders = deliveredOrders; }

    public Long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(Long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getDeliveredRevenue() { return deliveredRevenue; }
    public void setDeliveredRevenue(BigDecimal deliveredRevenue) { this.deliveredRevenue = deliveredRevenue; }
}

