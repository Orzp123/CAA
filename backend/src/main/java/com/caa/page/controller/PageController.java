package com.caa.page.controller;

import com.caa.common.ApiResponse;
import com.caa.page.dto.PageRequest;
import com.caa.page.dto.PageResponse;
import com.caa.page.service.PageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pages")
@Tag(name = "Pages", description = "Amis-schema page management endpoints")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    @Operation(summary = "List pages")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page list")
    public ResponseEntity<ApiResponse<List<PageResponse>>> listPages() {
        return ResponseEntity.ok(ApiResponse.ok(pageService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get page by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<ApiResponse<PageResponse>> getPage(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(pageService.findById(id)));
    }

    @GetMapping("/by-path")
    @Operation(summary = "Get page by URL path")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<ApiResponse<PageResponse>> getPageByPath(@RequestParam String path) {
        return ResponseEntity.ok(ApiResponse.ok(pageService.findByPath(path)));
    }

    @PostMapping
    @Operation(summary = "Create page")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Page created")
    public ResponseEntity<ApiResponse<PageResponse>> createPage(
            @Valid @RequestBody PageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(pageService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update page")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page updated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<ApiResponse<PageResponse>> updatePage(
            @PathVariable String id,
            @Valid @RequestBody PageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pageService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete page")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Page deleted")
    public ResponseEntity<ApiResponse<Void>> deletePage(@PathVariable String id) {
        pageService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok());
    }
}
