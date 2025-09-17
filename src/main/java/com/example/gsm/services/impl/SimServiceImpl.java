package com.example.gsm.services.impl;

import com.example.gsm.entity.Sim;
import com.example.gsm.entity.repository.SimRepository;
import com.example.gsm.services.SimService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.UpdateOneModel;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;  // ✅ dùng Query của Spring Data
import org.springframework.data.mongodb.core.query.Update; // ✅ để updateMulti
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimServiceImpl implements SimService {

    private final SimRepository simRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processSimJson(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        String computer = root.get("computer").asText();
        JsonNode portDataArray = root.get("port_data");

        // build list sim mới
        List<Sim> incomingSims = new ArrayList<>();
        Set<String> phonesInRequest = new HashSet<>();
        for (JsonNode node : portDataArray) {
            String phone = node.get("phone_number").asText();
            phonesInRequest.add(phone);

            incomingSims.add(Sim.builder()
                    .phoneNumber(phone)
                    .countryCode("JPN")
                    .status("active")
                    .computer(computer)
                    .comName(node.get("com_name").asText())
                    .simProvider(node.get("sim_provider").asText())
                    .ccid(node.get("ccid").asText())
                    .content(node.get("content").asText())
                    .lastUpdated(new Date())
                    .build());
        }

        List<Sim> existingSims = simRepository.findAll();
        Map<String, Sim> existingByPhone = existingSims.stream()
                .collect(Collectors.toMap(Sim::getPhoneNumber, s -> s));

        List<Sim> toInsert = new ArrayList<>();
        List<UpdateOneModel<Document>> updates = new ArrayList<>();

        for (Sim incoming : incomingSims) {
            Sim exist = existingByPhone.get(incoming.getPhoneNumber());
            if (exist == null) {
                incoming.setStatus("new");
                toInsert.add(incoming);
            } else {
                Document filter = new Document("phoneNumber", incoming.getPhoneNumber());
                Document update = new Document("$set", new Document()
                        .append("computer", incoming.getComputer())
                        .append("comName", incoming.getComName())
                        .append("simProvider", incoming.getSimProvider())
                        .append("ccid", incoming.getCcid())
                        .append("content", incoming.getContent())
                        .append("status", "active")
                        .append("lastUpdated", new Date())
                );
                updates.add(new UpdateOneModel<>(filter, update));
            }
        }
        if (!toInsert.isEmpty()) {
            mongoTemplate.insertAll(toInsert);
        }

        if (!updates.isEmpty()) {
            mongoTemplate.getCollection("sims").bulkWrite(updates);
        }

        Query query = new Query(Criteria.where("phoneNumber").nin(phonesInRequest));
        Update update = new Update().set("status", "replaced").set("lastUpdated", new Date());
        mongoTemplate.updateMulti(query, update, Sim.class);
    }
}
