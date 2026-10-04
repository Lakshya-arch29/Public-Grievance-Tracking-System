package com.jansunwai.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jansunwai.repository.LookupRepository;
import com.jansunwai.repository.LookupRepository.Option;

/** Public lists for dropdowns (no login needed). */
@RestController
@RequestMapping("/api/lookup")
public class LookupController {

    private final LookupRepository lookup;

    public LookupController(LookupRepository lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/cities")
    public List<Option> cities() {
        return lookup.cities();
    }

    @GetMapping("/categories")
    public List<Option> categories() {
        return lookup.categories();
    }
}
