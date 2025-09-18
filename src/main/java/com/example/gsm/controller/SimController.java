package com.example.gsm.controller;

import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.dao.SimRequest;
import com.example.gsm.entity.Sim;
import com.example.gsm.entity.repository.SimRepository;
import com.example.gsm.repositories.SimService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.example.gsm.comon.Constants.*;

@RestController
@RequestMapping("/api/sims")
@RequiredArgsConstructor
public class SimController {

    private final SimRepository simRepository;
    private final SimService simService;

    /** Lấy tất cả SIM phù hợp điều kiện */
    @PostMapping("/overview")
    public ResponseCommon<List<Sim>> getSimsOrder(@RequestBody SimRequest req) {
        try {
            return simService.getSim(req);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    /** Lấy tất cả SIM */
    @GetMapping
    public ResponseCommon<List<Sim>> getAllSims() {
        try {
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, simRepository.findAll());
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    /** Lấy SIM theo id */
    @GetMapping("/find/{id}")
    public ResponseCommon<Sim> getSimById(@PathVariable String id) {
        try {
            Optional<Sim> simOpt = simRepository.findById(id);
            return simOpt
                    .map(sim -> new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, sim))
                    .orElseGet(() -> new ResponseCommon<>(CORE_ERROR_CODE, "Sim not found", null));
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    /** Tạo mới SIM */
    @PostMapping("/create")
    public ResponseCommon<Sim> createSim(@RequestBody Sim sim) {
        try {
            Sim saved = simRepository.save(sim);
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, saved);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    /** Cập nhật SIM */
    @PutMapping("/update/{id}")
    public ResponseCommon<Sim> updateSim(@PathVariable String id, @RequestBody Sim simDetails) {
        try {
            return simRepository.findById(id)
                    .map(sim -> {
                        sim.setPhoneNumber(simDetails.getPhoneNumber());
                        sim.setRevenue(simDetails.getRevenue());
                        sim.setStatus(simDetails.getStatus());
                        sim.setCountryCode(simDetails.getCountryCode());
                        sim.setDeviceName(simDetails.getDeviceName());
                        sim.setComName(simDetails.getComName());
                        sim.setSimProvider(simDetails.getSimProvider());
                        sim.setCcid(simDetails.getCcid());
                        sim.setIccId(simDetails.getIccId());
                        sim.setContent(simDetails.getContent());
                        sim.setLastUpdated(simDetails.getLastUpdated());
                        sim.setOrderDate(simDetails.getOrderDate());
                        sim.setActiveDate(simDetails.getActiveDate());
                        sim.setSourceUses(simDetails.getSourceUses());
                        Sim updated = simRepository.save(sim);
                        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, updated);
                    })
                    .orElseGet(() -> new ResponseCommon<>(CORE_ERROR_CODE, "Sim not found", null));
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    /** Xóa SIM */
    @DeleteMapping("/delete/{id}")
    public ResponseCommon<Void> deleteSim(@PathVariable String id) {
        try {
            if (simRepository.existsById(id)) {
                simRepository.deleteById(id);
                return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
            }
            return new ResponseCommon<>(CORE_ERROR_CODE, "Sim not found", null);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
