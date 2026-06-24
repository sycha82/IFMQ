package com.example.shuttlewcs.controller;

import com.example.shuttlewcs.dto.MappingRequest;
import com.example.shuttlewcs.service.MappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MappingController {

    private final MappingService mappingService;

    @PostMapping("/mapping")
    public ResponseEntity<String> applyMapping(@RequestBody MappingRequest req) {
        mappingService.applyMapping(req);
        return ResponseEntity.ok("매핑 완료 | eqpPalletId=" + req.getEqpPalletId()
                + " palletId=" + req.getPalletId());
    }
}
