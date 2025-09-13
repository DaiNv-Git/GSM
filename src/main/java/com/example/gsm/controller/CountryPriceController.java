package com.example.gsm.controller;

import com.example.gsm.dao.CountryPriceDTO;
import com.example.gsm.entity.Country;
import com.example.gsm.entity.ServiceCountryPrice;
import com.example.gsm.exceptions.BadRequestException;
import com.example.gsm.exceptions.ErrorCode;
import com.example.gsm.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CountryPriceController {
    private final CountryService countryService;

    @GetMapping("/countries")
    public ResponseEntity<List<Country>> getCountries() {
        return ResponseEntity.ok(countryService.getAllCountries());
    }
    @GetMapping("/countries/{serviceCode}")
    public ResponseEntity<List<CountryPriceDTO>> getAllCountriesByServiceCode(@PathVariable String serviceCode) {
        return ResponseEntity.ok(countryService.getAllCountriesByServiceCode(serviceCode));
    }
    
    @GetMapping("/service-price")
    public ResponseEntity<?> getServicePrice(
            @RequestParam String serviceCode,
            @RequestParam String countryCode) {

        Optional<ServiceCountryPrice> price = countryService.getPriceByServiceAndCountry(serviceCode, countryCode);
        if (price.isEmpty()) {
            throw new BadRequestException(
                    ErrorCode.PRICE_NOT_FOUND);
        }
        return ResponseEntity.ok(price.get());
    }
}
