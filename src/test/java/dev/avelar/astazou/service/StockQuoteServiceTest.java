package dev.avelar.astazou.service;

import dev.avelar.astazou.brapi.BrapiService;
import dev.avelar.astazou.brapi.dto.Quote;
import dev.avelar.astazou.model.StockQuote;
import dev.avelar.astazou.model.StockQuoteHistory;
import dev.avelar.astazou.repository.StockQuoteHistoryRepository;
import dev.avelar.astazou.repository.StockQuoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockQuoteServiceTest {

  @Mock
  private BrapiService brapiService;

  @Mock
  private StockQuoteRepository stockQuoteRepository;

  @Mock
  private StockQuoteHistoryRepository stockQuoteHistoryRepository;

  @InjectMocks
  private StockQuoteService service;

  private Quote quote(String symbol, double price) {
    return Quote.builder().symbol(symbol).shortName(symbol + " Inc").longName(symbol + " Long").currency("BRL")
        .regularMarketPrice(price).build();
  }

  private StockQuote storedQuote(String symbol, double price) {
    return StockQuote.builder().id(1L).symbol(symbol).price(BigDecimal.valueOf(price)).build();
  }

  @Test
  void refreshQuotes_returnsEmptyList_whenNoTickersGiven() {
    List<StockQuote> result = service.refreshQuotes();

    assertTrue(result.isEmpty());
    verifyNoInteractions(brapiService);
  }

  @Test
  void refreshQuotes_singleTicker_callsBrapiOnce() {
    when(brapiService.findAssetByTicker("PETR4")).thenReturn(List.of(quote("PETR4", 35.50)));
    when(stockQuoteRepository.findBySymbol("PETR4")).thenReturn(Optional.of(storedQuote("PETR4", 35.50)));

    List<StockQuote> result = service.refreshQuotes("PETR4");

    assertEquals(1, result.size());
    verify(brapiService, times(1)).findAssetByTicker("PETR4");
  }

  @Test
  void refreshQuotes_filtersOutQuotesWithNullPrice() {
    Quote noPrice = Quote.builder().symbol("VALE3").regularMarketPrice(null).build();
    when(brapiService.findAssetByTicker("VALE3")).thenReturn(List.of(noPrice));

    List<StockQuote> result = service.refreshQuotes("VALE3");

    assertTrue(result.isEmpty());
    verify(stockQuoteRepository, never()).upsert(any());
    verify(stockQuoteHistoryRepository, never()).save(any());
  }

  @Test
  void refreshQuotes_multipleTickersUnder10_singleChunk() {
    List<Quote> quotes = List.of(quote("PETR4", 35.50), quote("VALE3", 68.10));
    when(brapiService.findAssetByTicker("PETR4", "VALE3")).thenReturn(quotes);
    when(stockQuoteRepository.findBySymbol("PETR4")).thenReturn(Optional.of(storedQuote("PETR4", 35.50)));
    when(stockQuoteRepository.findBySymbol("VALE3")).thenReturn(Optional.of(storedQuote("VALE3", 68.10)));

    List<StockQuote> result = service.refreshQuotes("PETR4", "VALE3");

    assertEquals(2, result.size());
    verify(brapiService, times(1)).findAssetByTicker("PETR4", "VALE3");
  }

  @Test
  void refreshQuotes_exactly10Tickers_singleChunk() {
    String[] tickers = IntStream.rangeClosed(1, 10).mapToObj(i -> "TK" + i).toArray(String[]::new);
    List<Quote> quotes = IntStream.rangeClosed(1, 10).mapToObj(i -> quote("TK" + i, i * 10.0)).toList();

    when(brapiService.findAssetByTicker(tickers)).thenReturn(quotes);
    quotes.forEach(q -> when(stockQuoteRepository.findBySymbol(q.getSymbol())).thenReturn(
        Optional.of(storedQuote(q.getSymbol(), q.getRegularMarketPrice()))));

    List<StockQuote> result = service.refreshQuotes(tickers);

    assertEquals(10, result.size());
    verify(brapiService, times(1)).findAssetByTicker(tickers);
  }

  @Test
  void refreshQuotes_11Tickers_splitIntoTwoChunks() {
    String[] all = IntStream.rangeClosed(1, 11).mapToObj(i -> "TK" + i).toArray(String[]::new);
    String[] firstChunk = IntStream.rangeClosed(1, 10).mapToObj(i -> "TK" + i).toArray(String[]::new);

    List<Quote> firstQuotes = IntStream.rangeClosed(1, 10).mapToObj(i -> quote("TK" + i, i * 10.0)).toList();
    List<Quote> secondQuotes = List.of(quote("TK11", 110.0));

    when(brapiService.findAssetByTicker(firstChunk)).thenReturn(firstQuotes);
    when(brapiService.findAssetByTicker("TK11")).thenReturn(secondQuotes);

    (IntStream.rangeClosed(1, 11).mapToObj(i -> "TK" + i).toList()).forEach(
        sym -> when(stockQuoteRepository.findBySymbol(sym)).thenReturn(Optional.of(storedQuote(sym, 1.0))));

    List<StockQuote> result = service.refreshQuotes(all);

    assertEquals(11, result.size());
    verify(brapiService, times(1)).findAssetByTicker(firstChunk);
    verify(brapiService, times(1)).findAssetByTicker("TK11");
  }

  @Test
  void refreshQuotes_mixedNullAndValidPrices_onlyValidArePersisted() {
    Quote valid = quote("PETR4", 35.0);
    Quote invalid = Quote.builder().symbol("JUNK1").regularMarketPrice(null).build();
    when(brapiService.findAssetByTicker("PETR4", "JUNK1")).thenReturn(List.of(valid, invalid));
    when(stockQuoteRepository.findBySymbol("PETR4")).thenReturn(Optional.of(storedQuote("PETR4", 35.0)));

    List<StockQuote> result = service.refreshQuotes("PETR4", "JUNK1");

    assertEquals(1, result.size());
    assertEquals("PETR4", result.getFirst().getSymbol());
  }

  @Test
  void getCurrentQuote_delegatesToRepository_withUppercasedSymbol() {
    StockQuote sq = storedQuote("PETR4", 35.0);
    when(stockQuoteRepository.findBySymbol("PETR4")).thenReturn(Optional.of(sq));

    Optional<StockQuote> result = service.getCurrentQuote("petr4");

    assertTrue(result.isPresent());
    assertEquals("PETR4", result.get().getSymbol());
    verify(stockQuoteRepository).findBySymbol("PETR4");
  }

  @Test
  void getCurrentQuote_returnsEmpty_whenSymbolNotFound() {
    when(stockQuoteRepository.findBySymbol("UNKNOWN")).thenReturn(Optional.empty());

    assertTrue(service.getCurrentQuote("unknown").isEmpty());
  }

  @Test
  void getCurrentQuote_alreadyUppercase_doesNotDoubleUpper() {
    when(stockQuoteRepository.findBySymbol("VALE3")).thenReturn(Optional.empty());

    service.getCurrentQuote("VALE3");

    verify(stockQuoteRepository).findBySymbol("VALE3");
  }

  @Test
  void getHistory_delegatesToRepository_withUppercasedSymbol() {
    StockQuoteHistory h = StockQuoteHistory.builder().symbol("PETR4").build();
    when(stockQuoteHistoryRepository.findBySymbolOrderByRecordedAtDesc("PETR4")).thenReturn(List.of(h));

    List<StockQuoteHistory> result = service.getHistory("petr4");

    assertEquals(1, result.size());
    verify(stockQuoteHistoryRepository).findBySymbolOrderByRecordedAtDesc("PETR4");
  }

  @Test
  void getHistory_returnsEmptyList_whenNoHistory() {
    when(stockQuoteHistoryRepository.findBySymbolOrderByRecordedAtDesc("VALE3")).thenReturn(List.of());

    assertTrue(service.getHistory("VALE3").isEmpty());
  }

  @Test
  void persist_upsertsSnapshotAndSavesHistory() {
    Quote q = quote("ITUB4", 24.80);
    when(stockQuoteRepository.findBySymbol("ITUB4")).thenReturn(Optional.of(storedQuote("ITUB4", 24.80)));

    service.persist(q);

    ArgumentCaptor<StockQuote> snapshotCaptor = ArgumentCaptor.forClass(StockQuote.class);
    verify(stockQuoteRepository).upsert(snapshotCaptor.capture());
    assertEquals("ITUB4", snapshotCaptor.getValue().getSymbol());
    assertEquals(BigDecimal.valueOf(24.80), snapshotCaptor.getValue().getPrice());

    ArgumentCaptor<StockQuoteHistory> historyCaptor = ArgumentCaptor.forClass(StockQuoteHistory.class);
    verify(stockQuoteHistoryRepository).save(historyCaptor.capture());
    assertEquals("ITUB4", historyCaptor.getValue().getSymbol());
    assertEquals(BigDecimal.valueOf(24.80), historyCaptor.getValue().getPrice());
  }

  @Test
  void persist_setsAllFieldsOnSnapshot() {
    Quote q =
        Quote.builder().symbol("BBAS3").shortName("Banco do Brasil").longName("Banco do Brasil S.A.").currency("BRL")
            .regularMarketPrice(55.30).build();
    when(stockQuoteRepository.findBySymbol("BBAS3")).thenReturn(Optional.of(storedQuote("BBAS3", 55.30)));

    service.persist(q);

    ArgumentCaptor<StockQuote> captor = ArgumentCaptor.forClass(StockQuote.class);
    verify(stockQuoteRepository).upsert(captor.capture());
    StockQuote saved = captor.getValue();
    assertEquals("BBAS3", saved.getSymbol());
    assertEquals("Banco do Brasil", saved.getShortName());
    assertEquals("Banco do Brasil S.A.", saved.getLongName());
    assertEquals("BRL", saved.getCurrency());
    assertNotNull(saved.getUpdatedAt());
    assertTrue(saved.getUpdatedAt().isAfter(OffsetDateTime.now().minusSeconds(5)));
  }

  @Test
  void persist_setsAllFieldsOnHistoryEntry() {
    Quote q =
        Quote.builder().symbol("BBAS3").shortName("Banco do Brasil").longName("Banco do Brasil S.A.").currency("BRL")
            .regularMarketPrice(55.30).build();
    when(stockQuoteRepository.findBySymbol("BBAS3")).thenReturn(Optional.of(storedQuote("BBAS3", 55.30)));

    service.persist(q);

    ArgumentCaptor<StockQuoteHistory> captor = ArgumentCaptor.forClass(StockQuoteHistory.class);
    verify(stockQuoteHistoryRepository).save(captor.capture());
    StockQuoteHistory history = captor.getValue();
    assertEquals("BBAS3", history.getSymbol());
    assertEquals("Banco do Brasil", history.getShortName());
    assertEquals("Banco do Brasil S.A.", history.getLongName());
    assertEquals("BRL", history.getCurrency());
    assertNotNull(history.getRecordedAt());
    assertTrue(history.getRecordedAt().isAfter(OffsetDateTime.now().minusSeconds(5)));
  }

  @Test
  void persist_returnsStoredSnapshot_whenRepositoryFindsIt() {
    Quote q = quote("PETR4", 35.50);
    StockQuote stored = storedQuote("PETR4", 35.50);
    when(stockQuoteRepository.findBySymbol("PETR4")).thenReturn(Optional.of(stored));

    StockQuote result = service.persist(q);

    assertSame(stored, result);
  }

  @Test
  void persist_returnsBuiltSnapshot_whenRepositoryReturnsEmpty() {
    Quote q = quote("NEWT3", 10.0);
    when(stockQuoteRepository.findBySymbol("NEWT3")).thenReturn(Optional.empty());

    StockQuote result = service.persist(q);

    assertNotNull(result);
    assertEquals("NEWT3", result.getSymbol());
    assertEquals(BigDecimal.valueOf(10.0), result.getPrice());
  }

  @Test
  void persist_convertsDoublePriceToBigDecimal() {
    Quote q = quote("MGLU3", 8.75);
    when(stockQuoteRepository.findBySymbol("MGLU3")).thenReturn(Optional.of(storedQuote("MGLU3", 8.75)));

    service.persist(q);

    ArgumentCaptor<StockQuote> captor = ArgumentCaptor.forClass(StockQuote.class);
    verify(stockQuoteRepository).upsert(captor.capture());
    assertEquals(BigDecimal.valueOf(8.75), captor.getValue().getPrice());
  }
}

