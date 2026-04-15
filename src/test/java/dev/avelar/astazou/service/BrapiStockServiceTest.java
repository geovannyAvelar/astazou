package dev.avelar.astazou.service;

import dev.avelar.astazou.brapi.BrapiService;
import dev.avelar.astazou.brapi.dto.Stock;
import dev.avelar.astazou.model.BrapiStock;
import dev.avelar.astazou.repository.BrapiStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrapiStockServiceTest {

  @Mock
  private BrapiService brapiService;

  @Mock
  private BrapiStockRepository brapiStockRepository;

  @Mock
  private StockLogoService stockLogoService;

  @InjectMocks
  private BrapiStockService service;

  // ── syncStocks – filtering ───────────────────────────────────────────────────

  @Test
  void syncStocks_upsertsAllValidStocks() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("PETR4", "Petrobras", "Energy", "https://example.com/petr4.png"),
        stock("VALE3", "Vale",      "Mining",  "https://example.com/vale3.png")
    ));
    when(stockLogoService.downloadAndStore(anyString(), anyString())).thenReturn("/logos/X.png");

    service.syncStocks();

    verify(brapiStockRepository, times(2)).upsert(any());
  }

  @Test
  void syncStocks_filtersOut_stocksEndingInF() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("PETR4F", "Petrobras fracionario", "Energy", null),
        stock("VALE3",  "Vale",                  "Mining", null)
    ));

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository, times(1)).upsert(captor.capture());
    assertEquals("VALE3", captor.getValue().getTicker());
  }

  @Test
  void syncStocks_filtersOut_stocksEndingIn34() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("AAPL34", "Apple BDR",  "Tech", null),
        stock("VALE3",  "Vale",       "Mining", null)
    ));

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository, times(1)).upsert(captor.capture());
    assertEquals("VALE3", captor.getValue().getTicker());
  }

  @Test
  void syncStocks_filtersOut_stocksEndingIn39() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("MSFT39", "Microsoft BDR", "Tech", null),
        stock("PETR4",  "Petrobras",     "Energy", null)
    ));

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository, times(1)).upsert(captor.capture());
    assertEquals("PETR4", captor.getValue().getTicker());
  }

  @Test
  void syncStocks_filtersOut_stocksWithNullTicker() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock(null,    "Unknown", "?", null),
        stock("VALE3", "Vale",    "Mining", null)
    ));

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository, times(1)).upsert(captor.capture());
    assertEquals("VALE3", captor.getValue().getTicker());
  }

  @Test
  void syncStocks_filtersOut_stocksWithBlankTicker() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("  ",    "Unknown", "?", null),
        stock("VALE3", "Vale",    "Mining", null)
    ));

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository, times(1)).upsert(captor.capture());
    assertEquals("VALE3", captor.getValue().getTicker());
  }

  @Test
  void syncStocks_uppercasesTicker_beforeUpsert() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("petr4", "Petrobras", "Energy", null)
    ));

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository).upsert(captor.capture());
    assertEquals("PETR4", captor.getValue().getTicker());
  }

  @Test
  void syncStocks_doesNothing_whenListIsEmpty() {
    when(brapiService.listStocks()).thenReturn(List.of());

    service.syncStocks();

    verify(brapiStockRepository, never()).upsert(any());
    verifyNoInteractions(stockLogoService);
  }

  // ── syncStocks – logo handling ───────────────────────────────────────────────

  @Test
  void syncStocks_callsLogoDownload_forEachStock() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("PETR4", "Petrobras", "Energy", "https://example.com/petr4.png"),
        stock("VALE3", "Vale",      "Mining",  "https://example.com/vale3.png")
    ));
    when(stockLogoService.downloadAndStore(anyString(), anyString())).thenReturn("/logos/X.png");

    service.syncStocks();

    verify(stockLogoService).downloadAndStore("PETR4", "https://example.com/petr4.png");
    verify(stockLogoService).downloadAndStore("VALE3", "https://example.com/vale3.png");
  }

  @Test
  void syncStocks_storesLogoUrl_fromLogoService() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("PETR4", "Petrobras", "Energy", "https://example.com/petr4.png")
    ));
    when(stockLogoService.downloadAndStore("PETR4", "https://example.com/petr4.png"))
        .thenReturn("/logos/PETR4.png");

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository).upsert(captor.capture());
    assertEquals("/logos/PETR4.png", captor.getValue().getLogoUrl());
  }

  @Test
  void syncStocks_storesNullLogoUrl_whenLogoIsNull() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("PETR4", "Petrobras", "Energy", null)
    ));
    when(stockLogoService.downloadAndStore("PETR4", null)).thenReturn(null);

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository).upsert(captor.capture());
    assertNull(captor.getValue().getLogoUrl());
  }

  @Test
  void syncStocks_storesNullLogoUrl_whenDownloadFails() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("PETR4", "Petrobras", "Energy", "https://example.com/petr4.png")
    ));
    when(stockLogoService.downloadAndStore(anyString(), anyString())).thenReturn(null);

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository).upsert(captor.capture());
    assertNull(captor.getValue().getLogoUrl());
  }

  @Test
  void syncStocks_mapsAllFields_correctly() {
    when(brapiService.listStocks()).thenReturn(List.of(
        stock("PETR4", "Petrobras", "Energy", "https://example.com/petr4.png")
    ));
    when(stockLogoService.downloadAndStore("PETR4", "https://example.com/petr4.png"))
        .thenReturn("/logos/PETR4.png");

    service.syncStocks();

    ArgumentCaptor<BrapiStock> captor = ArgumentCaptor.forClass(BrapiStock.class);
    verify(brapiStockRepository).upsert(captor.capture());
    BrapiStock saved = captor.getValue();
    assertEquals("PETR4",      saved.getTicker());
    assertEquals("Petrobras",  saved.getName());
    assertEquals("Energy",     saved.getSector());
    assertEquals("/logos/PETR4.png", saved.getLogoUrl());
  }

  // ── listTickers ──────────────────────────────────────────────────────────────

  @Test
  void listTickers_returnsTickersFromRepository() {
    when(brapiStockRepository.findAll()).thenReturn(List.of(
        brapiStock("PETR4"),
        brapiStock("VALE3"),
        brapiStock("ITUB4")
    ));

    List<String> result = service.listTickers();

    assertEquals(List.of("PETR4", "VALE3", "ITUB4"), result);
  }

  @Test
  void listTickers_returnsEmptyList_whenRepositoryIsEmpty() {
    when(brapiStockRepository.findAll()).thenReturn(List.of());

    assertTrue(service.listTickers().isEmpty());
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private static Stock stock(String ticker, String name, String sector, String logo) {
    return Stock.builder()
        .stock(ticker)
        .name(name)
        .sector(sector)
        .logo(logo)
        .build();
  }

  private static BrapiStock brapiStock(String ticker) {
    return BrapiStock.builder().ticker(ticker).build();
  }

}

