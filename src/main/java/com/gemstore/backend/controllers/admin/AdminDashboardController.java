package com.gemstore.backend.controllers.admin;

import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.entities.listing.ListingStatus;
import com.gemstore.backend.entities.report.ReportStatus;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.report.ReportRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ReportRepository reportRepository;
    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Dashboard stats overview
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.count());

        //  Use native admin queries — bypasses @SQLRestriction
        stats.put("activeListings", listingRepository.countByStatusForAdmin("ACTIVE"));
        stats.put("totalSold", listingRepository.countByStatusForAdmin("SOLD"));
        stats.put("suspendedListings", listingRepository.countByStatusForAdmin("SUSPENDED"));
        stats.put("deletedListings", listingRepository.countByStatusForAdmin("DELETED"));

        stats.put("pendingReports", reportRepository.countByStatus(ReportStatus.PENDING));
        stats.put("reviewingReports", reportRepository.countByStatus(ReportStatus.REVIEWING));
        stats.put("resolvedReports", reportRepository.countByStatus(ReportStatus.RESOLVED));
        stats.put("totalReports", reportRepository.count());

        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    /**
     * List all users with pagination
     * GET /api/admin/users?page=0&size=20&status=ACTIVE
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserResponse> allUsers = userService.findAllRaw().stream()
                .filter(u -> status == null || status.isEmpty() || u.getStatus().equalsIgnoreCase(status))
                .filter(u -> search == null || search.isEmpty() ||
                        (u.getUsername() != null && u.getUsername().toLowerCase().contains(search.toLowerCase())) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())) ||
                        (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(search.toLowerCase()))
                )
                .map(userMapper::toUserResponse)
                .toList();

        int start = page * size;
        int end = Math.min(start + size, allUsers.size());
        List<UserResponse> pageContent = start < allUsers.size() ? allUsers.subList(start, end) : List.of();

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", pageContent);
        pageData.put("totalElements", allUsers.size());
        pageData.put("totalPages", (int) Math.ceil((double) allUsers.size() / size));
        pageData.put("page", page);
        pageData.put("size", size);
        pageData.put("last", end >= allUsers.size());

        return ResponseEntity.ok(Map.of("success", true, "data", pageData));
    }

    /**
     * Get user detail (admin view)
     * GET /api/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        var user = userService.getById(id);
        UserResponse response = userMapper.toUserResponse(user);

        //  Use admin queries — bypasses @SQLRestriction
        long totalListings = listingRepository.countAllBySellerIdForAdmin(id);
        long activeListings = listingRepository.countActiveBySellerIdForAdmin(id);
        long reportCount = reportRepository.findByReportedUserIdOrderByCreatedAtDesc(id).size();

        Map<String, Object> data = new HashMap<>();
        data.put("user", response);
        data.put("totalListings", totalListings);
        data.put("activeListings", activeListings);
        data.put("reportCount", reportCount);

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    /**
     * List all listings with pagination (admin moderation)
     * GET /api/admin/listings?page=0&size=20&status=ACTIVE
     */
    @GetMapping("/listings")
    public ResponseEntity<Map<String, Object>> getAllListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        //  Use admin query — bypasses @SQLRestriction, shows ALL listings including deleted
        var allListings = listingRepository.findAllForAdmin().stream()
                .filter(l -> status == null || status.isEmpty() || l.getStatus().name().equalsIgnoreCase(status))
                .filter(l -> search == null || search.isEmpty() ||
                        (l.getTitle() != null && l.getTitle().toLowerCase().contains(search.toLowerCase())) ||
                        (l.getListingNumber() != null && l.getListingNumber().toLowerCase().contains(search.toLowerCase()))
                )
                .map(l -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", l.getId());
                    item.put("listingNumber", l.getListingNumber());
                    item.put("title", l.getTitle());
                    item.put("price", l.getPrice());
                    item.put("currency", l.getCurrency());
                    item.put("status", l.getStatus());
                    item.put("deletedAt", l.getDeletedAt()); // ✅ show deleted_at to admin
                    item.put("gemstoneType", l.getGemstoneType() != null ? l.getGemstoneType().getName() : null);
                    item.put("caratWeight", l.getCaratWeight());
                    item.put("createdAt", l.getCreatedAt());
                    item.put("viewsCount", l.getViewsCount());
                    item.put("likesCount", l.getLikesCount());
                    item.put("sellerId", l.getSeller().getId());
                    item.put("sellerUsername", l.getSeller().getUsername());
                    item.put("sellerDisplayName", l.getSeller().getDisplayName());

                    var images = l.getImages();
                    if (images != null && !images.isEmpty()) {
                        var primaryImage = images.stream()
                                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                .findFirst()
                                .orElse(images.get(0));
                        item.put("imageUrl", primaryImage.getImageUrl());
                    }

                    long reportCount = reportRepository.countByReportedListingId(l.getId());
                    item.put("reportCount", reportCount);

                    return item;
                })
                .toList();

        int start = page * size;
        int end = Math.min(start + size, allListings.size());
        var pageContent = start < allListings.size() ? allListings.subList(start, end) : List.of();

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", pageContent);
        pageData.put("totalElements", allListings.size());
        pageData.put("totalPages", (int) Math.ceil((double) allListings.size() / size));
        pageData.put("page", page);
        pageData.put("size", size);
        pageData.put("last", end >= allListings.size());

        return ResponseEntity.ok(Map.of("success", true, "data", pageData));
    }

    /**
     * Update listing status (admin moderation)
     * PATCH /api/admin/listings/{id}/status/{status}
     */
    @PatchMapping("/listings/{id}/status/{status}")
    public ResponseEntity<Map<String, Object>> updateListingStatus(
            @PathVariable Long id,
            @PathVariable String status
    ) {
        //  Use admin query — can update status of soft-deleted listings too
        var listing = listingRepository.findByIdForAdmin(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        ListingStatus newStatus = ListingStatus.valueOf(status.toUpperCase());
        listing.setStatus(newStatus);

        //  If admin restores a deleted listing, clear the deletedAt
        if (newStatus == ListingStatus.ACTIVE) {
            listing.setDeletedAt(null);
            listing.setDeletedBy(null);
        }

        listingRepository.save(listing);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Listing status updated to " + newStatus
        ));
    }
}