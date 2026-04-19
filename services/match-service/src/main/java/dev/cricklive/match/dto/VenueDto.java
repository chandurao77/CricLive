package dev.cricklive.match.dto;

import java.util.UUID;

public record VenueDto(UUID id, String name, String city, String country, Integer capacity, String pitchType) {}
