package com.example.gsm.services.impl;

import com.example.gsm.dao.CountryPriceDTO;
import com.example.gsm.entity.Country;
import com.example.gsm.entity.ServiceCountryPrice;
import com.example.gsm.entity.repository.ServiceCountryPriceRepository;
import com.example.gsm.entity.repository.CountryRepository;
import com.example.gsm.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {
    private final CountryRepository countryRepository;
    private final ServiceCountryPriceRepository priceRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    public List<CountryPriceDTO> getAllCountriesByServiceCode(String serviceCode) {
        // Lấy tất cả các bản ghi giá theo serviceCode
        List<ServiceCountryPrice> prices = priceRepository.findByServiceCode(serviceCode);

        // Lấy danh sách countryCode
        List<String> countryCodes = prices.stream()
                .map(ServiceCountryPrice::getCountryCode)
                .distinct()
                .toList();

        // Lấy thông tin Country theo countryCode
        List<Country> countries = countryRepository.findAllByCountryCodeIn(countryCodes);

        List<CountryPriceDTO> result = prices.stream().map(price -> {
            Country country = countries.stream()
                    .filter(c -> c.getCountryCode().equals(price.getCountryCode()))
                    .findFirst()
                    .orElse(null);

            if (country == null) return null;

            return CountryPriceDTO.builder()
                    .countryCode(country.getCountryCode())
                    .countryName(country.getCountryName())
                    .flagImage(country.getFlagImage())
                    .minPrice(price.getMinPrice())
                    .maxPrice(price.getMaxPrice())
                    .pricePerDay(price.getPricePerDay())
                    .build();
        }).filter(Objects::nonNull).toList();

        return result;
    }


    public Optional<ServiceCountryPrice> getPriceByServiceAndCountry(String serviceCode, String countryCode) {
        return priceRepository.findByServiceCodeAndCountryCode(serviceCode, countryCode);
    }
}
