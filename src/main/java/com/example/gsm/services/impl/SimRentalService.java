    package com.example.gsm.services.impl;

    import com.example.gsm.dao.response.RentSimResponse;
    import com.example.gsm.dao.StatusCode;
    import com.example.gsm.entity.*;
    import com.example.gsm.entity.repository.*;
    import com.example.gsm.entity.repository.impl.OrderCustomRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.time.Instant;
    import java.util.*;
    import java.util.concurrent.TimeUnit;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class SimRentalService {

        private final SimRepository simRepository;
        private final OrderCustomRepository orderCustomRepository;
        private final UserAccountRepository userAccountRepository;
        private final OrderRepository orderRepository;
        private final ServiceRepository serviceRepository;
        private final CountryRepository countryRepository;
        private final SimpMessagingTemplate messagingTemplate;

        /**
         * Thuê SIM – mỗi SIM sẽ là 1 order riêng biệt, trạng thái khởi tạo = PENDING
         */
        @Transactional
        public List<RentSimResponse> rentSim(Long accountId,
                                             String flatform,
                                             String countryCode,
                                             Double totalCost,
                                             int rentDuration,
                                             List<String> services,
                                             String provider,
                                             String type,
                                             int quantity,
                                             Boolean record) {
            try {
                UserAccount user = getUserAccount(accountId, totalCost);

                // 2. Chọn nhiều SIM khả dụng
                List<Sim> selectedSims = selectAvailableSims(countryCode, services, quantity);
                if (selectedSims.isEmpty()) {
                    throw new RuntimeException("Không đủ SIM khả dụng cho countryCode=" + countryCode);
                }

                // 3. Trừ tiền ngay khi tạo order (PENDING)
                updateUserBalance(user, StatusCode.SUCCESS, totalCost);

                // 4. Lấy country
                Country foundCountry = countryRepository.findByCountryCode(countryCode)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy countryCode: " + countryCode));

                // 5. Ghép tên dịch vụ
                String combinedServiceNames = services.stream()
                        .map(code -> serviceRepository.findByCode(code)
                                .map(ServiceEntity::getText)
                                .orElse("Không rõ dịch vụ"))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                // 6. Với mỗi SIM tạo 1 order riêng (PENDING) và gửi socket
                List<RentSimResponse> responses = new ArrayList<>();
                double costPerSim = totalCost / quantity;

                for (Sim sim : selectedSims) {
                    // Tạo order trạng thái PENDING
                    Order order = buildSingleOrder(accountId, flatform, countryCode,
                            costPerSim, rentDuration, services, provider, type, sim);

                    // Gửi WS ngay cả khi order đang PENDING
                    Map<String, Object> wsMessage = buildWebSocketMessage(sim, accountId, services,
                            rentDuration, order.getId(), type, foundCountry, StatusCode.PENDING.toString(),record);
                    sendWebSocketMessage(wsMessage);

                    responses.add(new RentSimResponse(
                            order.getId(),
                            List.of(sim.getPhoneNumber()),
                            combinedServiceNames,
                            String.join(",", services),
                            foundCountry.getCountryName(),
                            rentDuration,
                            foundCountry.getCountryCode()
                    ));
                }
                return responses;

            } catch (Exception ex) {
                throw new RuntimeException("❌ Lỗi khi thuê SIM: " + ex.getMessage(), ex);
            }
        }

        // --------------------- PRIVATE METHODS ---------------------

        private UserAccount getUserAccount(Long accountId, Double totalCost) {
            UserAccount user = userAccountRepository.findByAccountId(accountId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            if (user.getBalanceAmount() < totalCost) {
                throw new RuntimeException("Số dư không đủ");
            }
            return user;
        }

        private List<Sim> selectAvailableSims(String countryCode, List<String> services, int quantity) {
            List<Sim> sims = simRepository
                    .findByCountryCodeAndStatusIgnoreCaseOrderByRevenueDesc(countryCode, "ACTIVE");

            List<Sim> selected = new ArrayList<>();
            for (Sim sim : sims) {
                if (sim.getPhoneNumber() == null || sim.getPhoneNumber().isBlank()) continue;

                long rented = orderRepository.countByPhoneAndServiceCodes(sim.getPhoneNumber(), services);
                if (rented <= 0) {
                    selected.add(sim);
                    if (selected.size() >= quantity) break;
                }
            }
            return selected;
        }

        private Order buildSingleOrder(Long accountId,
                                       String flatform,
                                       String countryCode,
                                       Double costPerSim,
                                       int rentDuration,
                                       List<String> services,
                                       String provider,
                                       String type,
                                       Sim sim) {

            Order order = new Order();
            order.setAccountId(accountId);
            order.setType(type);
            order.setPlatform(flatform);
            order.setCost(costPerSim);
            order.setCountryCode(countryCode);

            // ✅ Ban đầu luôn PENDING
            order.setStatusCode(StatusCode.PENDING.toString());

            order.setCreatedAt(new Date());
            order.setUpdatedAt(new Date());

            long expiredMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(rentDuration);

            List<Order.Stock> stockList = new ArrayList<>();
            for (String serviceCode : services) {
                Order.Stock stock = new Order.Stock();
                stock.setPhone(sim.getPhoneNumber());
                stock.setServiceCode(serviceCode);
                stock.setProvider(provider);
                stock.setExpiredAt(new Date(expiredMillis));
                stockList.add(stock);
            }

            order.setStock(stockList);
            return orderRepository.save(order);
        }

        private void updateUserBalance(UserAccount user, StatusCode statusCode, Double totalCost) {
            Double currentBalance = user.getBalanceAmount();
            if (statusCode == StatusCode.SUCCESS) {
                user.setBalanceAmount(currentBalance - totalCost);
            } else if (statusCode == StatusCode.REFUNDED) {
                user.setBalanceAmount(currentBalance + totalCost);
            }
            userAccountRepository.save(user);
        }

        private void updateSimRevenue(Sim selectedSim, StatusCode statusCode, Double totalCost) {
            if (statusCode == StatusCode.SUCCESS) {
                selectedSim.setRevenue(
                        Optional.ofNullable(selectedSim.getRevenue()).orElse(0.0) + totalCost
                );
            } else if (statusCode == StatusCode.REFUNDED) {
                selectedSim.setRevenue(
                        Optional.ofNullable(selectedSim.getRevenue()).orElse(0.0) - totalCost
                );
                if (selectedSim.getRevenue() < 0) {
                    selectedSim.setRevenue(0.0);
                }
            }
            simRepository.save(selectedSim);
        }

        private Map<String, Object> buildWebSocketMessage(Sim selectedSim,
                                                          Long accountId,
                                                          List<String> services,
                                                          int rentDuration,
                                                          String orderId,
                                                          String type,
                                                          Country foundCountry,
                                                          String status,Boolean record) {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("deviceName", selectedSim.getDeviceName());
            wsMessage.put("orderId", orderId);
            wsMessage.put("type", type);
            wsMessage.put("phoneNumber", selectedSim.getPhoneNumber());
            wsMessage.put("comNumber", selectedSim.getComName());
            wsMessage.put("accountId", accountId);
            wsMessage.put("serviceCode", String.join(",", services));
            wsMessage.put("waitingTime", rentDuration);
            wsMessage.put("countryName", foundCountry.getCountryCode());
            wsMessage.put("status", status);
            wsMessage.put("record", record);
            return wsMessage;
        }

        private void sendWebSocketMessage(Map<String, Object> wsMessage) {
            messagingTemplate.convertAndSend("/topic/send-otp", wsMessage);
        }

        // --------------------- UPDATE STATUS ---------------------

        @Transactional
        public void updateOrderSuccess(String orderId) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

            if (!StatusCode.PENDING.toString().equals(order.getStatusCode())) {
                throw new RuntimeException("Chỉ cập nhật được order PENDING");
            }

            order.setStatusCode(StatusCode.SUCCESS.toString());
            order.setUpdatedAt(new Date());
            orderRepository.save(order);

            // Cập nhật revenue cho từng SIM
            for (Order.Stock stock : order.getStock()) {
                Sim sim = simRepository.findByPhoneNumber(stock.getPhone())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy SIM: " + stock.getPhone()));
                updateSimRevenue(sim, StatusCode.SUCCESS, order.getCost());
            }
        }

        @Transactional
        public void updateOrderRefund(String orderId) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

            if (!StatusCode.PENDING.toString().equals(order.getStatusCode())) {
                throw new RuntimeException("Chỉ cập nhật được order PENDING");
            }

            order.setStatusCode(StatusCode.REFUNDED.toString());
            order.setUpdatedAt(new Date());
            orderRepository.save(order);

            // Hoàn tiền cho user
            UserAccount user = userAccountRepository.findByAccountId(order.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            updateUserBalance(user, StatusCode.REFUNDED, order.getCost());
        }

        public Map<String, List<Order>> getOrdersGroupedByType(Long accountId, String phoneNumber, String type, int page, int size) {
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> orderPage = orderCustomRepository.findActiveOrders(accountId, new Date(), phoneNumber, type, pageable);

            Instant nowInstant = Instant.now();

            Comparator<Order> comparator = (o1, o2) -> {
                Instant o1MaxExpired = o1.getStock().stream()
                        .filter(s -> s.getExpiredAt() != null)
                        .map(s -> s.getExpiredAt().toInstant())
                        .max(Instant::compareTo)
                        .orElse(Instant.EPOCH);

                Instant o2MaxExpired = o2.getStock().stream()
                        .filter(s -> s.getExpiredAt() != null)
                        .map(s -> s.getExpiredAt().toInstant())
                        .max(Instant::compareTo)
                        .orElse(Instant.EPOCH);

                boolean o1Expired = o1MaxExpired.isAfter(nowInstant);
                boolean o2Expired = o2MaxExpired.isAfter(nowInstant);

                if (o1Expired && !o2Expired) return -1;
                if (!o1Expired && o2Expired) return 1;

                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            };

            List<Order> sorted = orderPage.getContent().stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());

            return sorted.stream()
                    .collect(Collectors.groupingBy(Order::getType, LinkedHashMap::new, Collectors.toList()));
        }

    }
