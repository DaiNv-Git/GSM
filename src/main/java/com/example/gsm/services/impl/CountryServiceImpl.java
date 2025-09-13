package com.example.gsm.services.impl;

import com.example.gsm.entity.Country;
import com.example.gsm.entity.ServiceCountryPrice;
import com.example.gsm.entity.ServiceCountryPriceRepository;
import com.example.gsm.entity.repository.CountryRepository;
import com.example.gsm.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {
    private final CountryRepository countryRepository;
    private final ServiceCountryPriceRepository priceRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }
    

    public Optional<ServiceCountryPrice> getPriceByServiceAndCountry(String serviceCode, String countryCode) {
        return priceRepository.findByServiceCodeAndCountryCode(serviceCode, countryCode);
    }
}
