package dev.avelar.astazou.config;

import dev.avelar.astazou.model.Session;
import dev.avelar.astazou.model.User;
import dev.avelar.astazou.service.SessionService;
import dev.avelar.astazou.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
public class AuthFilter extends OncePerRequestFilter {

  private final SessionService tokenService;

  private final UserService userService;

  @Autowired
  public AuthFilter(SessionService tokenService, UserService userService) {
    this.tokenService = tokenService;
    this.userService = userService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String tokenValue = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("SESSION".equals(cookie.getName())) {
          tokenValue = cookie.getValue();
          break;
        }
      }
    }

    if (tokenValue != null) {
      Optional<Session> maybeSession = tokenService.findByValue(tokenValue);
      if (maybeSession.isPresent() && maybeSession.get().isValid()) {
        Session token = maybeSession.get();

        var details = Session.builder()
            .username(token.getUsername())
            .token(token.getToken())
            .expiresAt(token.getExpiresAt())
            .build();

        var userOpt = userService.findByUsername(token.getUsername());

        if (userOpt.isPresent()) {
          User user = userOpt.get();
          List<SimpleGrantedAuthority> authorities = new LinkedList<>();

          var auth = new UsernamePasswordAuthenticationToken(details, tokenValue, authorities);
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
