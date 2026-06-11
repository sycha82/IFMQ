package com.example.wcsapp.cli;

import com.example.common.dto.InboundCompleteDto;
import com.example.wcsapp.producer.InboundCompleteProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class WcsCliRunner implements CommandLineRunner {

    private final InboundCompleteProducer inboundCompleteProducer;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_INBOUND_COMPLETE = """
            {
              "messageType": "INBOUND_COMPLETE",
              "messageId": "MSG-20260422-0007",
              "refMessageId": null,
              "sequenceNo": 1006,
              "timestamp": "2026-04-22T09:05:32",
              "taskId": "WMS-IN-20260422-001",
              "palletId": "PLT-20260422-001",
              "itemCode": "ITEM-20260422-001",
              "lotId": "LOT-20260422-001",
              "qty": 24,
              "status": "COMPLETED",
              "message": ""
            }""";

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n========================================");
        System.out.println("  WCS CLI (Ctrl+C 로 종료)");
        System.out.println("========================================");

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> sendInboundComplete(scanner);
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
        System.out.println("  1. INBOUND_COMPLETE 발행");
        System.out.println("  0. 종료");
        System.out.print("선택 > ");
    }

    private void sendInboundComplete(Scanner scanner) {
        System.out.println("\n[INBOUND_COMPLETE] JSON 입력 (Enter만 치면 기본값 사용):");
        System.out.println(DEFAULT_INBOUND_COMPLETE);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_INBOUND_COMPLETE);

        try {
            InboundCompleteDto dto = objectMapper.readValue(json, InboundCompleteDto.class);
            inboundCompleteProducer.send(dto);
            System.out.println("발행 완료. messageId=" + dto.getMessageId() + " (DB 로그 기록됨)");
        } catch (Exception e) {
            System.out.println("오류: " + e.getMessage());
        }
    }

    /**
     * 여러 줄로 붙여넣은 JSON을 중괄호 균형이 맞을 때까지 읽어들인다.
     * 첫 줄이 비어 있으면 기본값을 사용한다.
     */
    private String readJsonInput(Scanner scanner, String defaultJson) {
        StringBuilder sb = new StringBuilder();
        int braceCount = 0;
        boolean started = false;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!started && line.trim().isEmpty()) {
                return defaultJson;
            }
            sb.append(line).append("\n");
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    started = true;
                } else if (c == '}') {
                    braceCount--;
                }
            }
            if (started && braceCount <= 0) {
                break;
            }
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? defaultJson : result;
    }
}
