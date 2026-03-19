package dev.avelar.astazou.controller;

import dev.avelar.astazou.model.StockQuote;
import dev.avelar.astazou.model.StockQuoteHistory;
import dev.avelar.astazou.service.StockQuoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockQuoteControllerTest {

  @Mock
  private StockQuoteService stockQuoteService;

  @InjectMocks
  private StockQuoteController controller;

  @Test
  void getQuote_uppercasesTickerBeforeQuerying() {
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.of(freshQuote("PETR4")));

    controller.getQuote("petr4");

    verify(stockQuoteService).getCurrentQuote("PETR4");
  }

  @Test
  void getQuote_returnsCachedQuote_whenFresh() {
    StockQuote quote = freshQuote("PETR4");
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.of(quote));

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(quote, response.getBody());
    verify(stockQuoteService, never()).refreshQuotes(any());
  }

  @Test
  void getQuote_refreshes_whenCacheIsEmpty() {
    StockQuote refreshed = freshQuote("PETR4");
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.empty());
    when(stockQuoteService.refreshQuotes("PETR4")).thenReturn(List.of(refreshed));

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(refreshed, response.getBody());
    verify(stockQuoteService).refreshQuotes("PETR4");
  }

  @Test
  void getQuote_refreshes_whenUpdatedAtIsNull() {
    StockQuote stale = staleQuote("PETR4", null);
    StockQuote refreshed = freshQuote("PETR4");
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.of(stale));
    when(stockQuoteService.refreshQuotes("PETR4")).thenReturn(List.of(refreshed));

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(refreshed, response.getBody());
    verify(stockQuoteService).refreshQuotes("PETR4");
  }

  @Test
  void getQuote_refreshes_whenQuoteIsOlderThanThreshold() {
    StockQuote stale = staleQuote("PETR4", OffsetDateTime.now().minusMinutes(21));
    StockQuote refreshed = freshQuote("PETR4");
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.of(stale));
    when(stockQuoteService.refreshQuotes("PETR4")).thenReturn(List.of(refreshed));

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(refreshed, response.getBody());
    verify(stockQuoteService).refreshQuotes("PETR4");
  }

  @Test
  void getQuote_doesNotRefresh_whenQuoteIsExactlyAtThreshold() {
    StockQuote quote = staleQuote("PETR4", OffsetDateTime.now().minusMinutes(19));
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.of(quote));

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(quote, response.getBody());
    verify(stockQuoteService, never()).refreshQuotes(any());
  }

  @Test
  void getQuote_returnsNotFound_whenCacheMissAndRefreshReturnsEmpty() {
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.empty());
    when(stockQuoteService.refreshQuotes("PETR4")).thenReturn(List.of());

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void getQuote_returnsNotFound_whenStaleAndRefreshReturnsEmpty() {
    StockQuote stale = staleQuote("PETR4", OffsetDateTime.now().minusMinutes(30));
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.of(stale));
    when(stockQuoteService.refreshQuotes("PETR4")).thenReturn(List.of());

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getQuote_returnsFirstQuote_whenRefreshReturnsMultiple() {
    StockQuote first = freshQuote("PETR4");
    StockQuote second = StockQuote.builder().symbol("PETR4").price(BigDecimal.valueOf(40)).build();
    when(stockQuoteService.getCurrentQuote("PETR4")).thenReturn(Optional.empty());
    when(stockQuoteService.refreshQuotes("PETR4")).thenReturn(List.of(first, second));

    ResponseEntity<StockQuote> response = controller.getQuote("PETR4");

    assertEquals(first, response.getBody());
  }

  @Test
  void getHistory_uppercasesTickerBeforeQuerying() {
    when(stockQuoteService.getHistory("PETR4")).thenReturn(List.of());

    controller.getHistory("petr4");

    verify(stockQuoteService).getHistory("PETR4");
  }

  @Test
  void getHistory_returnsOk_withHistoryList() {
    List<StockQuoteHistory> history = List.of(
        StockQuoteHistory.builder().id(1L).symbol("PETR4").price(BigDecimal.valueOf(38.50))
            .recordedAt(OffsetDateTime.now().minusDays(1)).build(),
        StockQuoteHistory.builder().id(2L).symbol("PETR4").price(BigDecimal.valueOf(39.10))
            .recordedAt(OffsetDateTime.now()).build()
    );
    when(stockQuoteService.getHistory("PETR4")).thenReturn(history);

    ResponseEntity<List<StockQuoteHistory>> response = controller.getHistory("PETR4");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
    assertEquals(history, response.getBody());
  }

  @Test
  void getHistory_returnsOk_withEmptyList() {
    when(stockQuoteService.getHistory("PETR4")).thenReturn(List.of());

    ResponseEntity<List<StockQuoteHistory>> response = controller.getHistory("PETR4");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
  }

  private StockQuote freshQuote(String symbol) {
    return StockQuote.builder()
        .id(1L)
        .symbol(symbol)
        .shortName("Petrobras")
        .price(BigDecimal.valueOf(38.90))
        .updatedAt(OffsetDateTime.now().minusMinutes(5))
        .build();
  }

  private StockQuote staleQuote(String symbol, OffsetDateTime updatedAt) {
    return StockQuote.builder()
        .id(1L)
        .symbol(symbol)
        .price(BigDecimal.valueOf(38.90))
        .updatedAt(updatedAt)
        .build();
  }
}

