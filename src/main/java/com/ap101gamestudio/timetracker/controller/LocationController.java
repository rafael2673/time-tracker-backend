package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.EstadoResponse;
import com.ap101gamestudio.timetracker.dto.MunicipioResponse;
import com.ap101gamestudio.timetracker.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/states")
    public ResponseEntity<List<EstadoResponse>> getStates() {
        return ResponseEntity.ok(locationService.fetchEstados());
    }

    @GetMapping("/states/{uf}/municipalities")
    public ResponseEntity<List<MunicipioResponse>> getMunicipalities(@PathVariable String uf) {
        return ResponseEntity.ok(locationService.fetchMunicipiosByUf(uf.toUpperCase()));
    }
}
