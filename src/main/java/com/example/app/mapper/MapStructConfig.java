package com.example.app.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct settings: Spring beans, constructor injection, and a COMPILE ERROR
 * when a target property is left unmapped (so a new entity field is never silently forgotten).
 */
@MapperConfig(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapStructConfig {
}
