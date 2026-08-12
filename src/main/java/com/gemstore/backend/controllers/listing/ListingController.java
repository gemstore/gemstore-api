package com.gemstore.backend.controllers.listing;

import com.gemstore.backend.dtos.common.ApiResponse;
import com.gemstore.backend.dtos.common.PageResponse;
import com.gemstore.backend.dtos.listing.request.*;
import com.gemstore.backend.dtos.listing.response.*;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.listing.ListingImageService;
import com.gemstore.backend.services.listing.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
@Tag(
        name = "Listings",
        description = "Gemstone marketplace listing operations"
)
public class ListingController {

    private final ListingService listingService;
    private final ListingImageService listingImageService;

    /* ======================================================
       Create
       ====================================================== */

    @Operation(
            summary = "Create a new listing",
            description =
                    "Creates a gemstone listing for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Listing created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid listing information"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @Valid
            @RequestBody
            CreateListingRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        ListingResponse listing =
                listingService.createListing(
                        request,
                        user.getId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Listing created successfully",
                                listing
                        )
                );
    }

    /* ======================================================
       Read
       ====================================================== */

    @Operation(
            summary = "Get listing by ID",
            description =
                    "Retrieves a gemstone listing using its unique "
                            + "identifier. Authentication is optional "
                            + "and may be used to include user-specific "
                            + "information such as ownership or "
                            + "favorite status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListingById(
            @Parameter(
                    description = "Unique identifier of the listing",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        Long userId =
                user != null
                        ? user.getId()
                        : null;

        ListingResponse listing =
                listingService.getListingById(
                        id,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(listing)
        );
    }

    @Operation(
            summary = "Get listing detail by ID",
            description =
                    "Retrieves the full detail view of a gemstone "
                            + "listing by its unique identifier, "
                            + "including extended attributes and "
                            + "metadata. Authentication is optional."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing details returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<ListingDetailResponse>>
    getListingDetail(
            @Parameter(
                    description =
                            "Unique identifier of the listing",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        Long userId =
                user != null
                        ? user.getId()
                        : null;

        ListingDetailResponse listing =
                listingService.getListingDetail(
                        id,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(listing)
        );
    }

    @Operation(
            summary = "Get all active listings",
            description =
                    "Returns active gemstone listings with "
                            + "pagination. Authentication is optional "
                            + "and may be used to include "
                            + "user-specific information."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listings returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<ListingCardResponse>>
            > getActiveListings(
            @Parameter(
                    description =
                            "Zero-based page number used for pagination",
                    example = "0"
            )
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @Parameter(
                    description =
                            "Maximum number of listings returned per page",
                    example = "20"
            )
            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        Long userId =
                user != null
                        ? user.getId()
                        : null;

        PageResponse<ListingCardResponse> listings =
                listingService.getActiveListings(
                        page,
                        size,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(listings)
        );
    }

    /* ======================================================
       Search
       ====================================================== */

    @Operation(
            summary = "Search listings via query parameters",
            description =
                    "Searches and filters active gemstone listings "
                            + "using query parameters. Supports "
                            + "filtering by gemstone attributes, "
                            + "price range, carat weight and sorting. "
                            + "Authentication is optional."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Search results returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid search parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping("/search")
    public ResponseEntity<
            ApiResponse<PageResponse<ListingCardResponse>>
            > searchListings(
            @Parameter(
                    description =
                            "Text matched against searchable listing fields",
                    example = "blue sapphire"
            )
            @RequestParam(
                    name = "query",
                    required = false
            )
            String query,

            @Parameter(
                    description =
                            "Unique identifier of the gemstone type",
                    example = "1"
            )
            @RequestParam(
                    name = "gemstoneTypeId",
                    required = false
            )
            Integer gemstoneTypeId,

            @Parameter(
                    description =
                            "Unique identifier of the gemstone color",
                    example = "2"
            )
            @RequestParam(
                    name = "colorId",
                    required = false
            )
            Integer colorId,

            @Parameter(
                    description =
                            "Unique identifier of the color quality",
                    example = "3"
            )
            @RequestParam(
                    name = "colorQualityId",
                    required = false
            )
            Integer colorQualityId,

            @Parameter(
                    description =
                            "Unique identifier of the clarity grade",
                    example = "4"
            )
            @RequestParam(
                    name = "clarityId",
                    required = false
            )
            Integer clarityId,

            @Parameter(
                    description =
                            "Unique identifier of the gemstone cut",
                    example = "5"
            )
            @RequestParam(
                    name = "cutId",
                    required = false
            )
            Integer cutId,

            @Parameter(
                    description =
                            "Unique identifier of the gemstone origin",
                    example = "6"
            )
            @RequestParam(
                    name = "originId",
                    required = false
            )
            Integer originId,

            @Parameter(
                    description =
                            "Unique identifier of the gemstone treatment",
                    example = "7"
            )
            @RequestParam(
                    name = "treatmentId",
                    required = false
            )
            Integer treatmentId,

            @Parameter(
                    description =
                            "Minimum listing price included in the results",
                    example = "1000.00"
            )
            @RequestParam(
                    name = "minPrice",
                    required = false
            )
            BigDecimal minPrice,

            @Parameter(
                    description =
                            "Maximum listing price included in the results",
                    example = "500000.00"
            )
            @RequestParam(
                    name = "maxPrice",
                    required = false
            )
            BigDecimal maxPrice,

            @Parameter(
                    description =
                            "Minimum gemstone weight in carats",
                    example = "0.50"
            )
            @RequestParam(
                    name = "minCarat",
                    required = false
            )
            BigDecimal minCarat,

            @Parameter(
                    description =
                            "Maximum gemstone weight in carats",
                    example = "10.00"
            )
            @RequestParam(
                    name = "maxCarat",
                    required = false
            )
            BigDecimal maxCarat,

            @Parameter(
                    description =
                            "Listing field used to sort the results",
                    example = "createdAt"
            )
            @RequestParam(
                    name = "sortBy",
                    defaultValue = "createdAt"
            )
            String sortBy,

            @Parameter(
                    description =
                            "Sort direction applied to the results",
                    example = "DESC"
            )
            @RequestParam(
                    name = "sortDirection",
                    defaultValue = "DESC"
            )
            String sortDirection,

            @Parameter(
                    description =
                            "Zero-based page number used for pagination",
                    example = "0"
            )
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @Parameter(
                    description =
                            "Maximum number of listings returned per page",
                    example = "20"
            )
            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        ListingSearchRequest request =
                ListingSearchRequest.builder()
                        .query(query)
                        .gemstoneTypeId(gemstoneTypeId)
                        .colorId(colorId)
                        .colorQualityId(colorQualityId)
                        .clarityId(clarityId)
                        .cutId(cutId)
                        .originId(originId)
                        .treatmentId(treatmentId)
                        .minPrice(minPrice)
                        .maxPrice(maxPrice)
                        .minCarat(minCarat)
                        .maxCarat(maxCarat)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .size(size)
                        .build();

        Long userId =
                user != null
                        ? user.getId()
                        : null;

        PageResponse<ListingCardResponse> listings =
                listingService.searchListings(
                        request,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(listings)
        );
    }

    @Operation(
            summary = "Search listings via request body",
            description =
                    "Searches and filters active gemstone listings "
                            + "using a structured JSON request body. "
                            + "Useful for complex filter combinations. "
                            + "Authentication is optional."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Search results returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid search request"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping("/search")
    public ResponseEntity<
            ApiResponse<PageResponse<ListingCardResponse>>
            > searchListingsPost(
            @RequestBody
            ListingSearchRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        Long userId =
                user != null
                        ? user.getId()
                        : null;

        PageResponse<ListingCardResponse> listings =
                listingService.searchListings(
                        request,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(listings)
        );
    }

    /* ======================================================
       Seller and authenticated user's listings
       ====================================================== */

    @Operation(
            summary = "Get listings by seller",
            description =
                    "Retrieves a paginated list of active gemstone "
                            + "listings belonging to a specific seller. "
                            + "Authentication is optional."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Seller listings returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Seller not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<
            ApiResponse<PageResponse<ListingCardResponse>>
            > getListingsBySeller(
            @Parameter(
                    description =
                            "Unique identifier of the seller",
                    required = true,
                    example = "45"
            )
            @PathVariable("sellerId")
            Long sellerId,

            @Parameter(
                    description =
                            "Zero-based page number used for pagination",
                    example = "0"
            )
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @Parameter(
                    description =
                            "Maximum number of listings returned per page",
                    example = "20"
            )
            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        Long userId =
                user != null
                        ? user.getId()
                        : null;

        PageResponse<ListingCardResponse> listings =
                listingService.getListingsBySeller(
                        sellerId,
                        page,
                        size,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(listings)
        );
    }

    @Operation(
            summary = "Get authenticated user's listings",
            description =
                    "Retrieves a paginated list of gemstone listings "
                            + "owned by the currently authenticated user. "
                            + "Supports optional filtering by listing status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User listings returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<
            ApiResponse<PageResponse<ListingCardResponse>>
            > getMyListings(
            @Parameter(
                    description =
                            "Optional listing status used to filter results",
                    example = "ACTIVE"
            )
            @RequestParam(
                    name = "status",
                    required = false
            )
            String status,

            @Parameter(
                    description =
                            "Zero-based page number used for pagination",
                    example = "0"
            )
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @Parameter(
                    description =
                            "Maximum number of listings returned per page",
                    example = "20"
            )
            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        PageResponse<ListingCardResponse> listings =
                listingService.getMyListings(
                        user.getId(),
                        status,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(listings)
        );
    }

    /* ======================================================
       Update
       ====================================================== */

    @Operation(
            summary = "Update a listing",
            description =
                    "Fully replaces the details of an existing "
                            + "gemstone listing owned by the "
                            + "authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid listing information"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not allowed to update the listing"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ListingResponse>> updateListing(
            @Parameter(
                    description =
                            "Unique identifier of the listing to update",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @Valid
            @RequestBody
            UpdateListingRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        ListingResponse listing =
                listingService.updateListing(
                        id,
                        request,
                        user.getId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing updated successfully",
                        listing
                )
        );
    }

    @Operation(
            summary = "Partially update a listing",
            description =
                    "Applies a partial update to an existing "
                            + "gemstone listing owned by the "
                            + "authenticated user. Only provided "
                            + "fields will be modified."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid listing information"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not allowed to update the listing"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    @PatchMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ListingResponse>> patchListing(
            @Parameter(
                    description =
                            "Unique identifier of the listing to update",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @RequestBody
            UpdateListingRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        ListingResponse listing =
                listingService.updateListing(
                        id,
                        request,
                        user.getId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing updated successfully",
                        listing
                )
        );
    }

    /* ======================================================
       Listing status
       ====================================================== */

    @Operation(
            summary = "Mark a listing as sold",
            description =
                    "Transitions a gemstone listing to the sold state. "
                            + "An optional final sale price can be "
                            + "provided to override the original "
                            + "listing price."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing marked as sold"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid sold price or listing state"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not allowed to modify the listing"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    @PostMapping("/{id}/sold")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ListingResponse>> markAsSold(
            @Parameter(
                    description =
                            "Unique identifier of the listing being sold",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @Parameter(
                    description =
                            "Final sale price. When omitted, the original "
                                    + "listing price may be used.",
                    example = "150000.00"
            )
            @RequestParam(
                    name = "soldPrice",
                    required = false
            )
            BigDecimal soldPrice,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        ListingResponse listing =
                listingService.markAsSold(
                        id,
                        soldPrice,
                        user.getId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing marked as sold",
                        listing
                )
        );
    }

    @Operation(
            summary = "Archive a listing",
            description =
                    "Archives an active gemstone listing owned by "
                            + "the authenticated user, removing it "
                            + "from public search results while "
                            + "retaining its data."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing archived successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not allowed to archive the listing"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    @PostMapping("/{id}/archive")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> archiveListing(
            @Parameter(
                    description =
                            "Unique identifier of the listing to archive",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        listingService.archiveListing(
                id,
                user.getId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing archived",
                        null
                )
        );
    }

    @Operation(
            summary = "Reactivate an archived listing",
            description =
                    "Reactivates a previously archived gemstone "
                            + "listing owned by the authenticated user, "
                            + "making it publicly visible again."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing reactivated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description =
                            "User is not allowed to reactivate the listing"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    @PostMapping("/{id}/reactivate")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ListingResponse>>
    reactivateListing(
            @Parameter(
                    description =
                            "Unique identifier of the listing to reactivate",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        ListingResponse listing =
                listingService.reactivateListing(
                        id,
                        user.getId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing reactivated",
                        listing
                )
        );
    }

    /* ======================================================
       Delete
       ====================================================== */

    @Operation(
            summary = "Delete a listing",
            description =
                    "Permanently deletes a gemstone listing owned "
                            + "by the authenticated user. This action "
                            + "cannot be undone."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not allowed to delete the listing"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @Parameter(
                    description =
                            "Unique identifier of the listing to delete",
                    required = true,
                    example = "123"
            )
            @PathVariable("id")
            Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    "Authentication required"
                            )
                    );
        }

        listingService.deleteListing(
                id,
                user.getId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listing deleted",
                        null
                )
        );
    }

    /* ======================================================
       Image upload
       ====================================================== */

    @Operation(
            summary = "Upload a listing image",
            description =
                    "Uploads an image file to be associated with "
                            + "a gemstone listing. The authenticated "
                            + "user must be the owner of the listing. "
                            + "Returns the URL of the uploaded image."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Image uploaded successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or unsupported image file"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Image upload failed"
            )
    })
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> uploadListingImage(
            @Parameter(
                    description =
                            "Image file to upload for the listing",
                    required = true
            )
            @RequestPart("file")
            MultipartFile file,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        if (user == null) {
            log.warn(
                    "[UPLOAD] User is unauthenticated"
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "error",
                                    "Authentication required"
                            )
                    );
        }

        log.info(
                "[UPLOAD] Authenticated user: "
                        + "id={}, username={}, roles={}",
                user.getId(),
                user.getUsername(),
                user.getAuthorities()
        );

        String imageUrl =
                listingImageService.uploadListingImage(
                        user.getId(),
                        file
                );

        return ResponseEntity.ok(
                Map.of(
                        "url",
                        imageUrl
                )
        );
    }

    /* ======================================================
       Public slug
       ====================================================== */

    @Operation(
            summary = "Get listing by slug",
            description =
                    "Retrieves the full detail view of a gemstone "
                            + "listing using its human-readable URL "
                            + "slug. Authentication is optional."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping("/public/{slug}")
    public ResponseEntity<ApiResponse<ListingDetailResponse>> getBySlug(
            @Parameter(
                    description =
                            "Human-readable unique slug of the listing",
                    required = true,
                    example = "natural-blue-sapphire-125ct"
            )
            @PathVariable("slug")
            String slug,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            CustomUserDetails principal
    ) {

        Long userId =
                principal != null
                        ? principal.getId()
                        : null;

        ListingDetailResponse listing =
                listingService.getBySlug(
                        slug,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(listing)
        );
    }
}