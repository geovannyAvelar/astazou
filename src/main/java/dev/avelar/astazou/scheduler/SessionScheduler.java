package dev.avelar.astazou.scheduler;

import dev.avelar.astazou.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionScheduler {

  private final SessionService service;

  @Autowired
  public SessionScheduler(SessionService service) {
    this.service = service;
  }

  @Scheduled(fixedRate = 60 * 1000) // One minute
  public void revokeTokens() {
    service.revokeExpiredTokens();
  }

}
