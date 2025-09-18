package com.example.gsm.controller;

import com.example.gsm.dao.AgentResponse;
import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.entity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.gsm.comon.Constants.*;

@RestController()
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AgentController {

    @Autowired
    private final UserRepository userRepository;

    @GetMapping("/get-all-agents")
    public ResponseCommon<List<AgentResponse>> getActiveAgents() {
        try {
            List<AgentResponse> data =  userRepository.findByIsAgentTrueAndIsActiveTrue()
                    .stream()
                    .map(u -> new AgentResponse(
                            u.getId(),
                            u.getAccountId(),
                            u.getFirstName(),
                            u.getLastName(),
                            u.getBalanceAmount()
                    ))
                    .collect(Collectors.toList());

            return new ResponseCommon<>(SUCCESS_CODE,SUCCESS_MESSAGE,data);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
