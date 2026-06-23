package com.example.wmsmock.cli;

import com.example.common.dto.InboundCancelDto;
import com.example.common.dto.InboundCmdDto;
import com.example.wmsmock.producer.InboundCancelPublisher;
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
    private final InboundCancelPublisher inboundCancelPublisher;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_INBOUND_CMD = """
            {
              "messageType": "INBOUND_CMD",
              "messageId": "MSG-20260422-0000",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T08:50:00",
              "taskId": "WMS-IN-20260422-001",
              "inboundDetail": [
                {
                  "lineNo": 1,
                  "palletId": "PLT-20260422-001",
                  "itemCode": "ITEM-20260422-001",
                  "lotId": "LOT-20260422-001",
                  "qty": 24,
                  "expireDate": "2027-04-22"
                }
              ]
            }""";

    private static final String DEFAULT_INBOUND_CANCEL = """
            {
              "messageType": "INBOUND_CANCEL",
              "messageId": "MSG-CANCEL-0001",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T09:10:00",
              "taskId": "WMS-IN-20260422-001",
              "palletId": "PLT-20260422-001",
              "reason": "재고 오류로 인한 입고 취소"
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
                case "2" -> sendInboundCancel(scanner);
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
        System.out.println("  2. INBOUND_CANCEL 발행");
        System.out.println("  0. 종료");
        System.out.print("선택 > ");
    }

    private void sendInboundCmd(Scanner scanner) {
        System.out.println("\n[INBOUND_CMD] JSON 입력 (Enter만 치면 기본값 사용):");
        System.out.println(DEFAULT_INBOUND_CMD);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_INBOUND_CMD);

        try {
            InboundCmdDto dto = objectMapper.readValue(json, InboundCmdDto.class);
            inboundCmdPublisher.send(dto);
            System.out.println("발행 완료. messageId=" + dto.getMessageId());
        } catch (Exception e) {
            System.out.println("오류: " + e.getMessage());
        }
    }

    private void sendInboundCancel(Scanner scanner) {
        System.out.println("\n[INBOUND_CANCEL] JSON 입력 (Enter만 치면 기본값 사용):");
        System.out.println(DEFAULT_INBOUND_CANCEL);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_INBOUND_CANCEL);

        try {
            InboundCancelDto dto = objectMapper.readValue(json, InboundCancelDto.class);
            inboundCancelPublisher.send(dto);
            System.out.println("발행 완료. messageId=" + dto.getMessageId());
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
