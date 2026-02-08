package com.example.bidengine.api;

import com.example.bidengine.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuctionControllerTest {
    private MockMvc mockMvc;
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = Mockito.mock(AuctionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuctionController(auctionService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createAuctionReturns201() throws Exception {
        AuctionResponse response = new AuctionResponse(
                "a1", "s1", "title", "desc", "OPEN",
                100, 200L, 10, 20, null, null, 10, 10
        );
        Mockito.when(auctionService.createAuction(any(CreateAuctionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sellerId":"s1","title":"t","description":"d","startingPrice":100,
                                 "reservePrice":200,"startTimeEpochMs":10,"endTimeEpochMs":20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auctionId").value("a1"));
    }

    @Test
    void createAuctionBadRequestOnValidation() throws Exception {
        mockMvc.perform(post("/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAuctionBadRequestOnServiceError() throws Exception {
        Mockito.when(auctionService.createAuction(any(CreateAuctionRequest.class)))
                .thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(post("/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sellerId":"s1","title":"t","startingPrice":100,
                                 "startTimeEpochMs":10,"endTimeEpochMs":20}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUCTION_TIME"));
    }

    @Test
    void getAuctionNotFound() throws Exception {
        Mockito.when(auctionService.getAuction("missing")).thenReturn(null);

        mockMvc.perform(get("/auctions/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void placeBidMapsConflict() throws Exception {
        PlaceBidResponse response = new PlaceBidResponse("b1", "a1", "BELOW_HIGHEST", null, null);
        Mockito.when(auctionService.placeBid(eq("a1"), any(PlaceBidRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auctions/a1/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bidderId":"u1","amount":100}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void placeBidMapsOk() throws Exception {
        PlaceBidResponse response = new PlaceBidResponse("b1", "a1", "OK", 150L, "u1");
        Mockito.when(auctionService.placeBid(eq("a1"), any(PlaceBidRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auctions/a1/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bidderId":"u1","amount":150}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highestBid").value(150));
    }

    @Test
    void listBidsOk() throws Exception {
        Mockito.when(auctionService.listTopBids("a1", 2))
                .thenReturn(List.of(new BidResponse("b1", "a1", "u1", 120, 10)));

        mockMvc.perform(get("/auctions/a1/bids?limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bidId").value("b1"));
    }
}
