package com.example.gsm.services;

import com.example.gsm.entity.Country;
import com.example.gsm.entity.ServiceCountryPrice;

import java.util.List;
import java.util.Optional;

public interface CountryService {
    List<Country> getAllCountries();
    Optional<ServiceCountryPrice> getPriceByServiceAndCountry(String serviceCode, String countryCode);
}
