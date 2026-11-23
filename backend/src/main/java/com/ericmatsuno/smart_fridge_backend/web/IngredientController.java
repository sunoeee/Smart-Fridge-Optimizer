package com.ericmatsuno.smart_fridge_backend.web;

import com.ericmatsuno.smart_fridge_backend.service.IngredientService;
import com.ericmatsuno.smart_fridge_backend.web.dto.IngredientDtos.CreateIngredientRequest;
import com.ericmatsuno.smart_fridge_backend.web.dto.IngredientDtos.IngredientResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controllers stay thin: read the HTTP request, call one service method, return the
 * result. No business logic here, so the same logic can be reused (or tested) without HTTP.
 */
@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService service;

    public IngredientController(IngredientService service) {
        this.service = service;
    }

    @GetMapping
    public List<IngredientResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public IngredientResponse get(@PathVariable int id) {
        return service.get(id);
    }

    /** 201 Created + a Location header pointing at the new resource (REST convention). */
    @PostMapping
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody CreateIngredientRequest req) {
        IngredientResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/ingredients/" + created.id())).body(created);
    }
}
