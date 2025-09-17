package com.example.gsm.controller;
import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.dao.SimRequest;
import com.example.gsm.entity.Sim;
import com.example.gsm.entity.repository.SimRepository;
import com.example.gsm.repositories.SimService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.example.gsm.comon.Constants.*;


@RestController
@RequestMapping("/api/sims")
@RequiredArgsConstructor
public class SimController {

    @Autowired
    private final SimRepository simRepository;
    @Autowired
    private final SimService simService;

    /** Lấy tất cả SIM phù hợp điều kiện*/
    @PostMapping("/overview")
    public ResponseCommon<List<Sim>> getSimsOrder(@RequestBody SimRequest req) {
        try {
            return simService.getSim(req);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE,ex.getMessage(),null);
        }
    }


    /** Lấy tất cả SIM */
    @GetMapping
    public ResponseCommon<List<Sim>> getAllSims() {
        try {
            return new ResponseCommon(SUCCESS_CODE,SUCCESS_MESSAGE,simRepository.findAll());
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE,ex.getMessage(),null);
        }
    }

    /** Lấy SIM theo id */
    @GetMapping("/{id}")
    public ResponseCommon<Sim> getSimById(@PathVariable String id) {
        try {
            Optional<Sim> sim = simRepository.findById(id);
            return new ResponseCommon(SUCCESS_CODE,SUCCESS_MESSAGE,sim);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE,ex.getMessage(),null);
        }
    }

    /** Tạo mới SIM */
    @PostMapping
    public ResponseCommon<?> createSim(@RequestBody Sim sim) {
        try {
            simRepository.save(sim);
            return new ResponseCommon(SUCCESS_CODE,SUCCESS_MESSAGE,null);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE,ex.getMessage(),null);
        }
    }

    /** Cập nhật SIM */
    @PutMapping("/{id}")
    public ResponseCommon<Sim> updateSim(@PathVariable String id, @RequestBody Sim simDetails) {
        try {
            simRepository.findById(id).map(sim -> {
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
                return ResponseEntity.ok(simRepository.save(sim));
            });
            return new ResponseCommon(SUCCESS_CODE,SUCCESS_MESSAGE,null);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE,ex.getMessage(),null);
        }
    }

    /** Xóa SIM */
    @DeleteMapping("/{id}")
    public ResponseCommon<?> deleteSim(@PathVariable String id) {
        try {
            simRepository.findById(id).map(sim -> {
                simRepository.delete(sim);
                return ResponseEntity.noContent().build();
            });
            return new ResponseCommon(SUCCESS_CODE,SUCCESS_MESSAGE,null);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE,ex.getMessage(),null);
        }
    }
}
