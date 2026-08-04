package com.gemstore.backend.entities.listing;

import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.entities.listing.lookup.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listings_seller", columnList = "seller_id"),
        @Index(name = "idx_listings_status", columnList = "status"),
        @Index(name = "idx_listings_type", columnList = "gemstone_type_id"),
        @Index(name = "idx_listings_price", columnList = "price"),
        @Index(name = "idx_listings_carat", columnList = "carat_weight"),
        @Index(name = "idx_listings_created", columnList = "created_at DESC"),
        @Index(name = "idx_listings_slug", columnList = "slug"),
        @Index(name = "idx_listings_deleted_at", columnList = "deleted_at")
})
@SQLRestriction("deleted_at IS NULL") // Global soft delete filter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_number", unique = true, length = 20)
    private String listingNumber;

    @Column(name = "slug", unique = true, length = 12)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gemstone_type_id", nullable = false)
    private GemstoneType gemstoneType;

    @Column(name = "carat_weight", nullable = false, precision = 8, scale = 3)
    @Positive
    private BigDecimal caratWeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_quality_id")
    private ColorQuality colorQuality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clarity_id")
    private ClarityGrade clarity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_id")
    private Cut cut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id")
    private Origin origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_id")
    private Treatment treatment;

    @Column(name = "length_mm", precision = 6, scale = 2)
    private BigDecimal lengthMm;

    @Column(name = "width_mm", precision = 6, scale = 2)
    private BigDecimal widthMm;

    @Column(name = "depth_mm", precision = 6, scale = 2)
    private BigDecimal depthMm;

    @Column(nullable = false, precision = 14, scale = 2)
    @Positive
    private BigDecimal price;

    @Column(length = 3)
    @Builder.Default
    private String currency = "LKR";

    @Column(name = "price_per_carat", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal pricePerCarat;

    @Column(name = "is_certified")
    @Builder.Default
    private Boolean isCertified = false;

    @Column(name = "certificate_info", length = 255)
    private String certificateInfo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    @Column(name = "favorites_count")
    @Builder.Default
    private Integer favoritesCount = 0;

    @Column(name = "completeness_score")
    @Builder.Default
    private Integer completenessScore = 0;

    @Column(name = "is_sold")
    @Builder.Default
    private Boolean isSold = false;

    @Column(name = "sold_price", precision = 14, scale = 2)
    private BigDecimal soldPrice;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "days_to_sell")
    private Integer daysToSell;

    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    //  Soft delete fields
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ListingImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Favorite> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Like> likes = new ArrayList<>();

    //  Soft delete helper
    public void softDelete(Long deletedByUserId) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUserId;
        this.status = ListingStatus.DELETED;
    }

    public boolean isSoftDeleted() {
        return this.deletedAt != null;
    }

    @Transient
    public String getPrimaryImageUrl() {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .sorted(Comparator.comparingInt(ListingImage::getDisplayOrder))
                .map(ListingImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    public void addImage(ListingImage image) {
        images.add(image);
        image.setListing(this);
    }

    public void removeImage(ListingImage image) {
        images.remove(image);
        image.setListing(null);
    }
}