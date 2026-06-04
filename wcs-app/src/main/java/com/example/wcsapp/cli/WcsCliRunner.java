package com.example.wcsapp.cli;

import com.example.common.dto.InboundCompleteDto;
import com.example.wcsapp.producer.InboundCompleteProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class WcsCliRunner implements CommandLineRunner {

    private final InboundCompleteProducer inboundCompleteProducer;
    private final AtomicInteger seq = new AtomicInteger(1);

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
        System.out.println("\n[INBOUND_COMPLETE 메시지 입력]");

        String taskId    = prompt(scanner, "taskId",   "WMS-IN-20260422-001");
        String palletId  = prompt(scanner, "palletId", "PLT-20260422-001");
        String itemCode  = prompt(scanner, "itemCode", "ITEM-20260422-001");
        String lotId     = prompt(scanner, "lotId",    "LOT-20260422-001");
        int    qty       = Integer.parseInt(prompt(scanner, "qty", "1"));
        String status    = prompt(scanner, "status (COMPLETED/FAILED)", "COMPLETED");
        String message   = prompt(scanner, "message", "");

        InboundCompleteDto dto = InboundCompleteDto.builder()
                .messageType("INBOUND_COMPLETE")
                .messageId("MSG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sequenceNo(seq.getAndIncrement())
                .timestamp(LocalDateTime.now())
                .taskId(taskId)
                .palletId(palletId)
                .itemCode(itemCode)
                .lotId(lotId)
                .qty(qty)
                .status(status)
                .message(message)
                .build();

        try {
            inboundCompleteProducer.send(dto);
            System.out.println("발행 완료. messageId=" + dto.getMessageId() + " (DB 로그 기록됨)");
        } catch (Exception e) {
            System.out.println("발행 실패: " + e.getMessage());
        }
    }

    private String prompt(Scanner scanner, String field, String defaultValue) {
        System.out.printf("  %-35s [%s] > ", field, defaultValue);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
}
