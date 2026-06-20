package com.tangluck.lobby;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "lobby_cards")
public class LobbyCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_code", nullable = false)
    private String cardCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String subtitle;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Column(nullable = false)
    private String status;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LobbyCard() {
    }

    public LobbyCard(String cardCode, String title, String subtitle, String imageUrl, String targetUrl, String status, Integer sortOrder, Instant updatedAt) {
        this.cardCode = cardCode;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.targetUrl = targetUrl;
        this.status = status;
        this.sortOrder = sortOrder;
        this.updatedAt = updatedAt;
    }

    public void update(String title, String subtitle, String imageUrl, String targetUrl, String status, Integer sortOrder, Instant updatedAt) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.targetUrl = targetUrl;
        this.status = status;
        this.sortOrder = sortOrder;
        this.updatedAt = updatedAt;
    }

    public String getCardCode() {
        return cardCode;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
