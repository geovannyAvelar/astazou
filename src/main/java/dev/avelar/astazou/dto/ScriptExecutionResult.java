package dev.avelar.astazou.dto;

public record ScriptExecutionResult(
    String output,
    String error,
    int exitCode,
    long executionTimeMs,
    /** Captured pip output; null when no dependencies were requested. */
    String installLog
) {}

