package com.example.locations.controller;

import com.example.locations.model.Location;
import com.example.locations.repository.LocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin(origins = "*")
public class LocationController {

    private final LocationRepository repository;

    public LocationController(LocationRepository repository) {
        this.repository = repository;
    }

    // GET /api/locations?page=0&size=20
    @GetMapping
    public Page<Location> getLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        size = clampSize(size);
        return repository.findAll(PageRequest.of(page, size, Sort.by(sortBy)));
    }

    // GET /api/locations/123
    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocation(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/locations/country/DE?page=0&size=20
    @GetMapping("/country/{countryCode}")
    public Page<Location> getByCountry(
            @PathVariable String countryCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = clampSize(size);
        return repository.findByCountryCode(
                countryCode.toUpperCase(),
                PageRequest.of(page, size, Sort.by("city", "name")));
    }

    // GET /api/locations/search?q=berlin&page=0&size=20
    @GetMapping("/search")
    public Page<Location> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = clampSize(size);
        return repository.findByNameContainingIgnoreCaseOrCityContainingIgnoreCase(
                q, q, PageRequest.of(page, size));
    }

    // GET /api/locations/city?name=Paris&page=0&size=20
    @GetMapping("/city")
    public Page<Location> getByCity(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = clampSize(size);
        return repository.findByCityContainingIgnoreCase(
                name, PageRequest.of(page, size, Sort.by("name")));
    }

    // GET /api/locations/drive-through?page=0&size=20
    @GetMapping("/drive-through")
    public Page<Location> getDriveThrough(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = clampSize(size);
        return repository.findByDriveThrough("yes", PageRequest.of(page, size));
    }

    // GET /api/locations/bbox?minLat=48&maxLat=52&minLng=2&maxLng=6
    @GetMapping("/bbox")
    public List<Location> getInBoundingBox(
            @RequestParam BigDecimal minLat,
            @RequestParam BigDecimal maxLat,
            @RequestParam BigDecimal minLng,
            @RequestParam BigDecimal maxLng) {
        return repository.findInBoundingBox(minLat, maxLat, minLng, maxLng);
    }

    // GET /api/locations/nearby?lat=48.8566&lng=2.3522&radius=10&limit=50
    @GetMapping("/nearby")
    public List<Location> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius,
            @RequestParam(defaultValue = "50") int limit) {
        return repository.findNearby(lat, lng, radius, limit);
    }

    // GET /api/locations/countries
    @GetMapping("/countries")
    public List<String> getCountries() {
        return repository.findAllCountries();
    }

    // GET /api/locations/cities/DE
    @GetMapping("/cities/{countryCode}")
    public List<String> getCities(@PathVariable String countryCode) {
        return repository.findCitiesByCountryCode(countryCode.toUpperCase());
    }

    private int clampSize(int size) {
        return Math.max(10, Math.min(100, size));
    }

    // GET /api/locations/stats
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repository.count());
        stats.put("driveThrough", repository.countByDriveThrough("yes"));
        stats.put("byCountry", repository.countByCountry());
        return stats;
    }
}
