package dev.cricklive.scoring.dto;

/** Response returned to the scorer for an accepted delivery. */
public record BallAck(String eventId, long sequence, int overNumber, int ballNumber, boolean legalBall, boolean duplicate) {}
