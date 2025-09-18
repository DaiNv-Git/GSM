package com.example.gsm.services.impl;

import com.example.gsm.entity.Sim;
import com.example.gsm.entity.repository.SimRepository;
import com.example.gsm.services.SimService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.UpdateOneModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimServiceImpl implements SimService {

    private static final String DEFAULT_COUNTRY_CODE = "JPN";

    private final SimRepository simRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void processSimJson(String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON input cannot be null or empty");
        }

        // 1. Parse JSON thành danh sách Sim + phone numbers
        Pair<List<Sim>, Set<String>> parsedData = parseIncomingSims(json);
        List<Sim> incomingSims = parsedData.getLeft();
        Set<String> phonesInRequest = parsedData.getRight();

        if (incomingSims.isEmpty()) {
            return;
        }

        // 2. Lấy danh sách Sim hiện có thành Map
        Map<String, Sim> existingByPhone = getExistingSimMap();

        // 3. Chuẩn bị insert và update
        InsertUpdateResult insertUpdate = prepareInsertAndUpdates(incomingSims, existingByPhone);

        // 4. Bulk insert và update
        bulkInsertAndUpdate(insertUpdate);

        // 5. Đánh dấu các sim không còn trong request thành replaced
        markReplacedSims(phonesInRequest);
    }

    // Parse JSON thành danh sách Sim + danh sách phone numbers
    private Pair<List<Sim>, Set<String>> parseIncomingSims(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        String deviceName = root.path("device_name").asText("UnknownDevice");
        JsonNode portDataArray = root.path("port_data");
        if (!portDataArray.isArray()) {
            throw new IllegalArgumentException("port_data must be an array");
        }

        List<Sim> incomingSims = new ArrayList<>();
        Set<String> phonesInRequest = new HashSet<>();

        for (JsonNode node : portDataArray) {
            String phone = node.path("phone_number").asText("");
            if (phone.isEmpty()) continue;

            phonesInRequest.add(phone);

            incomingSims.add(Sim.builder()
                    .phoneNumber(phone)
                    .countryCode(node.path("country_code").asText(DEFAULT_COUNTRY_CODE))
                    .status(node.path("status").asText("active"))
                    .deviceName(deviceName)
                    .comName(node.path("com_name").asText(""))
                    .simProvider(node.path("sim_provider").asText(""))
                    .ccid(node.path("ccid").asText(""))
                    .content(node.path("content").asText(""))
                    .lastUpdated(new Date())
                    .build());
        }

        return Pair.of(incomingSims, phonesInRequest);
    }

    // Lấy sim hiện có thành Map
    private Map<String, Sim> getExistingSimMap() {
        List<Sim> existingSims = simRepository.findAll();
        return existingSims.stream()
                .collect(Collectors.toMap(Sim::getPhoneNumber, s -> s, (a, b) -> a));
    }

    // Chuẩn bị insert và update
    private InsertUpdateResult prepareInsertAndUpdates(List<Sim> incomingSims, Map<String, Sim> existingByPhone) {
        List<Sim> toInsert = new ArrayList<>();
        List<UpdateOneModel<Document>> updates = new ArrayList<>();

        for (Sim incoming : incomingSims) {
            Sim exist = existingByPhone.get(incoming.getPhoneNumber());
            if (exist == null) {
                // Case 1: sim chưa tồn tại → insert với status "new"
                incoming.setStatus("new");
                toInsert.add(incoming);
            } else {
                // Case 2: sim đã tồn tại nhưng deviceName hoặc comName khác → update với status active
                boolean needUpdate = isDataChanged(exist, incoming);

                if (needUpdate) {
                    Document filter = new Document("phoneNumber", incoming.getPhoneNumber());

                    Document setDoc = new Document();
                    setDoc.append("deviceName", defaultIfNull(incoming.getDeviceName()))
                            .append("comName", defaultIfNull(incoming.getComName()))
                            .append("simProvider", defaultIfNull(incoming.getSimProvider()))
                            .append("ccid", defaultIfNull(incoming.getCcid()))
                            .append("content", defaultIfNull(incoming.getContent()))
                            .append("status", "active")
                            .append("lastUpdated", new Date());

                    Document update = new Document("$set", setDoc);
                    updates.add(new UpdateOneModel<>(filter, update));
                }
            }
        }

        return new InsertUpdateResult(toInsert, updates);
    }

    // Bulk insert và update
    private void bulkInsertAndUpdate(InsertUpdateResult insertUpdate) {
        if (!insertUpdate.toInsert.isEmpty()) {
            mongoTemplate.insertAll(insertUpdate.toInsert);
        }
        if (!insertUpdate.updates.isEmpty()) {
            mongoTemplate.getCollection("sims").bulkWrite(insertUpdate.updates);
        }
    }

    // Đánh dấu các sim không còn trong request thành replaced
    private void markReplacedSims(Set<String> phonesInRequest) {
        // Lọc các sim không có trong request và status khác replaced mới update
        Query query = new Query(Criteria.where("phoneNumber").nin(phonesInRequest)
                .and("status").ne("replaced"));
        Update update = new Update().set("status", "replaced").set("lastUpdated", new Date());
        mongoTemplate.updateMulti(query, update, Sim.class);
    }

    // Kiểm tra dữ liệu có thay đổi không
    private boolean isDataChanged(Sim exist, Sim incoming) {
        return !Objects.equals(defaultIfNull(exist.getDeviceName()), defaultIfNull(incoming.getDeviceName()))
                || !Objects.equals(defaultIfNull(exist.getComName()), defaultIfNull(incoming.getComName()))
                || !Objects.equals(defaultIfNull(exist.getSimProvider()), defaultIfNull(incoming.getSimProvider()))
                || !Objects.equals(defaultIfNull(exist.getCcid()), defaultIfNull(incoming.getCcid()))
                || !Objects.equals(defaultIfNull(exist.getContent()), defaultIfNull(incoming.getContent()));
    }

    // Hàm tiện ích để tránh null
    private String defaultIfNull(String value) {
        return value != null ? value : "";
    }

    // Class chứa kết quả insert/update
    private static class InsertUpdateResult {
        List<Sim> toInsert;
        List<UpdateOneModel<Document>> updates;

        InsertUpdateResult(List<Sim> toInsert, List<UpdateOneModel<Document>> updates) {
            this.toInsert = toInsert;
            this.updates = updates;
        }
    }

}
