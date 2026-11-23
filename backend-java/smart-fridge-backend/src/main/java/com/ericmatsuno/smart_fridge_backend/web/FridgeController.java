package com.ericmatsuno.smart_fridge_backend.web;

import com.ericmatsuno.smart_fridge_backend.service.FridgeService;
import com.ericmatsuno.smart_fridge_backend.web.dto.FridgeDtos.AddFridgeItemRequest;
import com.ericmatsuno.smart_fridge_backend.web.dto.FridgeDtos.ConsumeRequest;
import com.ericmatsuno.smart_fridge_backend.web.dto.FridgeDtos.FridgeItemResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fridge")
public class FridgeController {

    private final FridgeService service;

    public FridgeController(FridgeService service) {
        this.service = service;
    }

    /** GET /api/fridge                        -> everything, soonest-expiring first
     *  GET /api/fridge?expiringWithinDays=3   -> only what needs using soon (incl. expired) */
    @GetMapping
    public List<FridgeItemResponse> list(@RequestParam(required = false) @Min(0) Integer expiringWithinDays) {
        return service.list(expiringWithinDays);
    }

    /** Record groceries you bought. */
    @PostMapping
    public ResponseEntity<FridgeItemResponse> add(@Valid @RequestBody AddFridgeItemRequest req) {
        FridgeItemResponse created = service.add(req);
        return ResponseEntity.created(URI.create("/api/fridge/" + created.id())).body(created);
    }

    /**
     * Use part of a batch. 200 with the updated item, or 204 No Content if it was used up.
     * POST (not PATCH) because this is an action ("consume 2"), not "set quantity to X":
     * sending it twice should use 4, which is not idempotent.
     */
    @PostMapping("/{id}/consume")
    public ResponseEntity<FridgeItemResponse> consume(@PathVariable int id, @Valid @RequestBody ConsumeRequest req) {
        return service.consume(id, req.quantity())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Thrown out / spoiled. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable int id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }
}
