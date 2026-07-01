package com.example.rcsmock.cli;

import com.example.common.dto.BcrReadDto;
import com.example.common.dto.InboundDoneAckDto;
import com.example.common.dto.InboundDoneDto;
import com.example.common.dto.StationStatusDto;
import com.example.rcsmock.client.ShuttleWcsRcsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class RcsCliRunner implements CommandLineRunner {

    private final ShuttleWcsRcsClient shuttleWcsRcsClient;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_STATION_STATUS = """
            {
              "messageType": "STATION_STATUS",
              "messageId": "RCS-STS-0001",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T09:20:00",
              "stationId": "STATION-IN-01",
              "stationType": "INBOUND",
              "status": "AVAILABLE"
            }""";

    private static final String DEFAULT_BCR_READ = """
            {
              "messageType": "BCR_READ",
              "messageId": "RCS-BCR-0001",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T09:21:00",
              "stationId": "STATION-IN-01",
              "eqpPalletId": "EP0001"
            }""";

    private static final String DEFAULT_INBOUND_DONE = """
            {
              "messageType": "INBOUND_DONE",
              "messageId": "RCS-DONE-0001",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T09:30:00",
              "wcsTaskId": "EP0001-1",
              "eqpPalletId": "EP0001",
              "shuttleId": "SHUTTLE-01",
              "status": "COMPLETED",
              "failReason": null
            }""";

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n========================================");
        System.out.println("  RCS/설비ECS Mock CLI (Ctrl+C 로 종료)");
        System.out.println("========================================");

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> sendStationStatus(scanner);
                case "2" -> sendBcrRead(scanner);
                case "3" -> sendInboundDone(scanner);
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
        System.out.println("  1. STATION_STATUS 발행 (API 01)");
        System.out.println("  2. BCR_READ 발행 (API 02)");
        System.out.println("  3. INBOUND_DONE 발행 (API 05)");
        System.out.println("  0. 종료");
        System.out.print("선택 > ");
    }

    private void sendStationStatus(Scanner scanner) {
        System.out.println("\n[STATION_STATUS] JSON 붙여넣기 ('default' 입력 시 아래 기본값 사용):");
        System.out.println(DEFAULT_STATION_STATUS);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_STATION_STATUS);
        try {
            StationStatusDto dto = objectMapper.readValue(json, StationStatusDto.class);
            String resp = shuttleWcsRcsClient.sendStationStatus(dto);
            System.out.println("응답: " + resp);
        } catch (Exception e) {
            System.out.println("오류: " + e.getMessage());
        }
    }

    private void sendBcrRead(Scanner scanner) {
        System.out.println("\n[BCR_READ] JSON 붙여넣기 ('default' 입력 시 아래 기본값 사용):");
        System.out.println(DEFAULT_BCR_READ);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_BCR_READ);
        try {
            BcrReadDto dto = objectMapper.readValue(json, BcrReadDto.class);
            String resp = shuttleWcsRcsClient.sendBcrRead(dto);
            System.out.println("응답: " + resp);
        } catch (Exception e) {
            System.out.println("오류: " + e.getMessage());
        }
    }

    private void sendInboundDone(Scanner scanner) {
        System.out.println("\n[INBOUND_DONE] JSON 붙여넣기 ('default' 입력 시 아래 기본값 사용):");
        System.out.println(DEFAULT_INBOUND_DONE);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_INBOUND_DONE);
        try {
            InboundDoneDto dto = objectMapper.readValue(json, InboundDoneDto.class);
            InboundDoneAckDto ack = shuttleWcsRcsClient.sendInboundDone(dto);
            System.out.println("응답(INBOUND_DONE_ACK): wcsTaskId=" + ack.getWcsTaskId()
                    + " result=" + ack.getResult() + " message=" + ack.getMessage());
        } catch (Exception e) {
            System.out.println("오류: " + e.getMessage());
        }
    }

    /**
     * 여러 줄로 붙여넣은 JSON을 중괄호 균형이 맞을 때까지 읽어들인다.
     * '{' 이전의 빈 줄은 무시하며, "default" 입력 시 기본값을 사용한다.
     */
    private String readJsonInput(Scanner scanner, String defaultJson) {
        StringBuilder sb = new StringBuilder();
        int braceCount = 0;
        boolean started = false;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!started) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;                       // JSON 시작 전 빈 줄 무시
                }
                if (trimmed.equalsIgnoreCase("default")) {
                    return defaultJson;             // 기본값 사용
                }
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
