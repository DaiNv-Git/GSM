package com.example.gsm.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "countries")
public class Country {
    @Id
    private String id;

    private String countryCode;
    private String countryName;
    private String flagImage;
}
