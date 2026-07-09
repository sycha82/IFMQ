package com.example.rcsmock.cli;

import com.example.common.dto.BcrReadDto;
import com.example.common.dto.InboundDoneAckDto;
import com.example.common.dto.InboundDoneDto;
import com.example.common.dto.InboundStartDto;
import com.example.common.dto.OutboundDoneAckDto;
import com.example.common.dto.OutboundDoneDto;
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

    private static final String DEFAULT_STATION_STATUS_OUT = """
            {
              "messageType": "STATION_STATUS",
              "messageId": "RCS-STS-OUT-0001",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T09:20:00",
              "stationId": "STATION-OUT-01",
              "stationType": "OUTBOUND",
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

    private static final String DEFAULT_INBOUND_START = """
            {
              "messageType": "INBOUND_START",
              "messageId": "RCS-IN-START-0001",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T09:25:00",
              "stationId": "STATION-IN-01",
              "eqpPalletId": "EP0001",
              "wcsTaskId": "EP0001-1",
              "shuttleId": "SHUTTLE-01"
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

    private static final String DEFAULT_OUTBOUND_DONE = """
            {
              "messageType": "OUTBOUND_DONE",
              "messageId": "RCS-OUT-DONE-0001",
              "refMessageId": null,
              "sequenceNo": 1,
              "timestamp": "2026-04-22T10:08:15",
              "wcsTaskId": "EQPPLT-OUT-01-1",
              "eqpPalletId": "EQPPLT-OUT-01",
              "shuttleId": "SHUTTLE-01",
              "destStation": "STATION-OUT-01",
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
                case "1" -> sendStationStatus(scanner, DEFAULT_STATION_STATUS);
                case "2" -> sendBcrRead(scanner);
                case "3" -> sendInboundDone(scanner);
                case "4" -> sendStationStatus(scanner, DEFAULT_STATION_STATUS_OUT);
                case "5" -> sendOutboundDone(scanner);
                case "6" -> sendInboundStart(scanner);
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
        System.out.println("  1. STATION_STATUS (입고) 발행 (API 01)");
        System.out.println("  2. BCR_READ 발행 (API 02)");
        System.out.println("  3. INBOUND_DONE 발행 (API 05)");
        System.out.println("  4. STATION_STATUS (출고) 발행 (출고 PRE)");
        System.out.println("  5. OUTBOUND_DONE 발행 (출고 API 05)");
        System.out.println("  6. INBOUND_START 발행 (입고 착수 · 스테이션 해제)");
        System.out.println("  0. 종료");
        System.out.print("선택 > ");
    }

    private void sendStationStatus(Scanner scanner, String defaultJson) {
        System.out.println("\n[STATION_STATUS] JSON 붙여넣기 ('default' 입력 시 아래 기본값 사용):");
        System.out.println(defaultJson);
        System.out.print("> ");

        String json = readJsonInput(scanner, defaultJson);
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

    private void sendInboundStart(Scanner scanner) {
        System.out.println("\n[INBOUND_START] JSON 붙여넣기 ('default' 입력 시 아래 기본값 사용):");
        System.out.println(DEFAULT_INBOUND_START);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_INBOUND_START);
        try {
            InboundStartDto dto = objectMapper.readValue(json, InboundStartDto.class);
            String resp = shuttleWcsRcsClient.sendInboundStart(dto);
            System.out.println("응답: " + resp);
        } catch (Exception e) {
            System.out.println("오류: " + e.getMessage());
        }
    }

    private void sendOutboundDone(Scanner scanner) {
        System.out.println("\n[OUTBOUND_DONE] JSON 붙여넣기 ('default' 입력 시 아래 기본값 사용):");
        System.out.println(DEFAULT_OUTBOUND_DONE);
        System.out.print("> ");

        String json = readJsonInput(scanner, DEFAULT_OUTBOUND_DONE);
        try {
            OutboundDoneDto dto = objectMapper.readValue(json, OutboundDoneDto.class);
            OutboundDoneAckDto ack = shuttleWcsRcsClient.sendOutboundDone(dto);
            System.out.println("응답(OUTBOUND_DONE_ACK): wcsTaskId=" + ack.getWcsTaskId()
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
