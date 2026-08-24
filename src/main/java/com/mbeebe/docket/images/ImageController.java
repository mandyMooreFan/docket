package com.mbeebe.docket.images;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * Serves stored images. Rows are immutable — a change is a new id — so a hard
 * cache is correct and cheap.
 */
@Controller
class ImageController {

    private final Images images;

    ImageController(Images images) {
        this.images = images;
    }

    @GetMapping("/images/{id}")
    ResponseEntity<byte[]> serve(@PathVariable long id) {
        Images.StoredImage image = images.load(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.data());
    }
}
