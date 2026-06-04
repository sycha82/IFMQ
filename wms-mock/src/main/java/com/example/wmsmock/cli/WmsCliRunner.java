package com.example.wmsmock.cli;

import com.example.common.dto.InboundCmdDto;
import com.example.wmsmock.producer.InboundCmdPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class WmsCliRunner implements CommandLineRunner {

    private final InboundCmdPublisher inboundCmdPublisher;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_INBOUND_CMD = """
            {
              "messageType": "INBOUND_CMD",
              "messageId": "MSG-20260422-0000",
              "refMessageId": null,
              "sequenceNo": 999,
              "timestamp": "2026-04-22T08:50:00",
              "taskId": "WMS-IN-20260422-001",
              "palletId": "PLT-20260422-001",
              "itemCode": "ITEM-20260422-001",
              "lotId": "LOT-20260422-001",
              "qty": 24,
              "expireDate": "2027-04-22"
            }""";

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n========================================");
        System.out.println("  WMS Mock CLI (Ctrl+C 로 종료)");
        System.out.println("  * INBOUND_COMPLETE 수신은 자동으로 출력됩니다.");
        System.out.println("========================================");

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> sendInboundCmd(scanner);
                case "0" -> {
                    System.out.println("종료합니다.");
                    return;
                }
                default -> System.out.println("올바른 메뉴를 선택하세요.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n----------------------------------------");
        System.out.println("  1. INBOUND_CMD 발행");
        System.out.println("  0. 종료");
        System.out.print("선택 > ");
    }

    private void sendInboundCmd(Scanner scanner) {
        System.out.println("\n[INBOUND_CMD] JSON 입력 (Enter만 치면 기본값 사용):");
        System.out.println(DEFAULT_INBOUND_CMD);
        System.out.print("> ");

        String input = scanner.nextLine().trim();
        String json = input.isEmpty() ? DEFAULT_INBOUND_CMD : input;

        try {
            InboundCmdDto dto = objectMapper.readValue(json, InboundCmdDto.class);
            inboundCmdPublisher.send(dto);
            System.out.println("발행 완료. messageId=" + dto.getMessageId());
        } catch (Exception e) {
            System.out.println("오류: " + e.getMessage());
        }
    }
}
