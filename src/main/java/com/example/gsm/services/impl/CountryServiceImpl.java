package com.example.gsm.services.impl;

import com.example.gsm.dao.CountryPriceDTO;
import com.example.gsm.entity.Country;
import com.example.gsm.entity.ServiceCountryPrice;
import com.example.gsm.entity.ServiceEntity;
import com.example.gsm.entity.Sim;
import com.example.gsm.entity.repository.ServiceCountryPriceRepository;
import com.example.gsm.entity.repository.CountryRepository;
import com.example.gsm.entity.repository.ServiceRepository;
import com.example.gsm.entity.repository.SimRepository;
import com.example.gsm.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {
    private final CountryRepository countryRepository;
    private final ServiceCountryPriceRepository priceRepository;
    private final SimRepository simRepository;
    private final ServiceRepository serviceRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @Override
    public List<Country> getAllHaveSim() {
            List<Sim> allSims = simRepository.findAll();
            Set<String> countryCodes = allSims.stream()
                    .map(Sim::getCountryCode)
                    .filter(code -> code != null && !code.isEmpty())
                    .collect(Collectors.toSet());

            List<Country> countries = countryRepository.findAll().stream()
                    .filter(c -> countryCodes.contains(c.getCountryCode()))
                    .collect(Collectors.toList());

            return countries;
    }

    public List<CountryPriceDTO> getAllCountriesByServiceCode(String serviceCode, String name) {

        List<ServiceCountryPrice> prices = priceRepository.findByServiceCode(serviceCode);

        if (prices == null || prices.isEmpty()) {
            ServiceEntity service = serviceRepository.findByCode(serviceCode).orElse(null);
            if (service == null) {
                return Collections.emptyList();
            }

            List<Country> countries;
            if (name != null && !name.isEmpty()) {
                countries = countryRepository.findAllByCountryCodeInAndCountryNameContainingIgnoreCase(
                        Collections.singletonList(service.getCountryCode()), name);
            } else {
                countries = countryRepository.findAllByCountryCodeIn(Collections.singletonList(service.getCountryCode()));
            }

            Country country;
            if (countries.isEmpty()) {
                country = new Country();
                country.setCountryCode(service.getCountryCode());
                country.setCountryName(service.getCountryCode());
                country.setFlagImage(null);
            } else {
                country = countries.get(0);
            }

            CountryPriceDTO dto = CountryPriceDTO.builder()
                    .countryCode(country.getCountryCode())
                    .countryName(country.getCountryName())
                    .flagImage(country.getFlagImage())
                    .minPrice(service.getPrice())
                    .maxPrice(service.getPrice())
                    .pricePerDay(service.getPricePerDay())
                    .build();

            return Collections.singletonList(dto);
        }

        // ----- Nếu có prices thì logic cũ + fallback country -----
        // Lấy danh sách countryCode từ prices
        List<String> countryCodes = prices.stream()
                .map(ServiceCountryPrice::getCountryCode)
                .distinct()
                .toList();

        // Tìm trong bảng country
        List<Country> countries;
        if (name != null && !name.isEmpty()) {
            countries = countryRepository.findAllByCountryCodeInAndCountryNameContainingIgnoreCase(countryCodes, name);
        } else {
            countries = countryRepository.findAllByCountryCodeIn(countryCodes);
        }

        // Nếu thiếu hoặc không có, lấy thêm countryCode từ bảng services
        ServiceEntity service = serviceRepository.findByCode(serviceCode).orElse(null);
        if (service != null && service.getCountryCode() != null) {
            String serviceCountryCode = service.getCountryCode();
            boolean alreadyExists = countries.stream()
                    .anyMatch(c -> c.getCountryCode().equals(serviceCountryCode));
            if (!alreadyExists) {
                Country fallbackCountry = new Country();
                fallbackCountry.setCountryCode(serviceCountryCode);
                fallbackCountry.setCountryName(serviceCountryCode);
                fallbackCountry.setFlagImage(null);
                countries.add(fallbackCountry);
            }
        }

        // Map sang DTO
        List<CountryPriceDTO> result = prices.stream().map(price -> {
            Country country = countries.stream()
                    .filter(c -> c.getCountryCode().equals(price.getCountryCode()))
                    .findFirst()
                    .orElse(null);

            if (country == null && service != null && service.getCountryCode().equals(price.getCountryCode())) {
                country = new Country();
                country.setCountryCode(service.getCountryCode());
                country.setCountryName(service.getCountryCode());
                country.setFlagImage(null);
            }

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

    @Override
    public List<CountryPriceDTO> getAllServicesByCountryCode(String countryCode) {
        // Lấy Country
        Country country = countryRepository.findByCountryCode(countryCode)
                .orElse(null);

        if (country == null) {
            return List.of();
        }

        List<ServiceCountryPrice> prices = priceRepository.findByCountryCode(countryCode);

        List<String> serviceCodes = prices.stream()
                .map(ServiceCountryPrice::getServiceCode)
                .distinct()
                .toList();

        List<ServiceEntity> services = serviceRepository.findAllByCodeIn(serviceCodes);

        // Map sang DTO
        return prices.stream().map(price -> {
            ServiceEntity serviceEntity = services.stream()
                    .filter(s -> s.getCode().equals(price.getServiceCode()))
                    .findFirst()
                    .orElse(null);

            return CountryPriceDTO.builder()
                    .serviceCode(price.getServiceCode())
                    .serviceName(serviceEntity != null ? serviceEntity.getText() : null)
                    .serviceImage(serviceEntity != null ? serviceEntity.getImage() : null)
                    .minPrice(price.getMinPrice())
                    .maxPrice(price.getMaxPrice())
                    .pricePerDay(price.getPricePerDay())
                    .build();
        }).toList();
    }

}
