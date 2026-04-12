package dev.avelar.astazou.dto;

/**
 * Optional body for the execute endpoint.
 * {@code requirements} follows requirements.txt format (one package per line).
 * If null or blank, no ephemeral install is performed.
 */
public record ExecuteRequest(String requirements) {}

