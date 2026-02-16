package com.example.locations.repository;

import com.example.locations.model.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // Basic pagination
    Page<Location> findAll(Pageable pageable);

    // Filter by country
    Page<Location> findByCountryCode(String countryCode, Pageable pageable);
    Page<Location> findByCountry(String country, Pageable pageable);

    // Filter by city
    Page<Location> findByCityContainingIgnoreCase(String city, Pageable pageable);

    // Filter by drive-through
    Page<Location> findByDriveThrough(String driveThrough, Pageable pageable);

    // Search by name or city
    Page<Location> findByNameContainingIgnoreCaseOrCityContainingIgnoreCase(
            String name, String city, Pageable pageable);

    // Find locations within a bounding box (for map viewport)
    @Query("SELECT l FROM Location l WHERE " +
           "l.latitude BETWEEN :minLat AND :maxLat AND " +
           "l.longitude BETWEEN :minLng AND :maxLng")
    List<Location> findInBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng);

    // Find locations within bounding box with pagination
    @Query("SELECT l FROM Location l WHERE " +
           "l.latitude BETWEEN :minLat AND :maxLat AND " +
           "l.longitude BETWEEN :minLng AND :maxLng")
    Page<Location> findInBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            Pageable pageable);

    // Find nearest locations using Haversine formula (approximate)
    @Query(value = "SELECT *, " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
           "cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * " +
           "sin(radians(latitude)))) AS distance " +
           "FROM locations " +
           "HAVING distance < :radiusKm " +
           "ORDER BY distance " +
           "LIMIT :limit", nativeQuery = true)
    List<Location> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("limit") int limit);

    // Get all unique countries
    @Query("SELECT DISTINCT l.country FROM Location l WHERE l.country IS NOT NULL ORDER BY l.country")
    List<String> findAllCountries();

    // Get all unique cities for a country
    @Query("SELECT DISTINCT l.city FROM Location l WHERE l.countryCode = :countryCode AND l.city IS NOT NULL ORDER BY l.city")
    List<String> findCitiesByCountryCode(@Param("countryCode") String countryCode);

    // Count by country
    @Query("SELECT l.country, COUNT(l) FROM Location l GROUP BY l.country ORDER BY COUNT(l) DESC")
    List<Object[]> countByCountry();

    // Count locations with drive-through
    long countByDriveThrough(String driveThrough);
}
