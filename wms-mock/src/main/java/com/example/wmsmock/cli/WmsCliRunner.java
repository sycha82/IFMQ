package com.example.wmsmock.cli;

import com.example.common.dto.InboundCmdDto;
import com.example.wmsmock.producer.InboundCmdPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class WmsCliRunner implements CommandLineRunner {

    private final InboundCmdPublisher inboundCmdPublisher;
    private final AtomicInteger seq = new AtomicInteger(1);

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
        System.out.println("\n[INBOUND_CMD 메시지 입력]");

        String taskId     = prompt(scanner, "taskId",     "WMS-IN-20260422-001");
        String palletId   = prompt(scanner, "palletId",   "PLT-20260422-001");
        String itemCode   = prompt(scanner, "itemCode",   "ITEM-20260422-001");
        String lotId      = prompt(scanner, "lotId",      "LOT-20260422-001");
        int    qty        = Integer.parseInt(prompt(scanner, "qty", "1"));
        String expireStr  = prompt(scanner, "expireDate (yyyy-MM-dd, 없으면 Enter)", "");

        InboundCmdDto dto = InboundCmdDto.builder()
                .messageType("INBOUND_CMD")
                .messageId("MSG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sequenceNo(seq.getAndIncrement())
                .timestamp(LocalDateTime.now())
                .taskId(taskId)
                .palletId(palletId)
                .itemCode(itemCode)
                .lotId(lotId)
                .qty(qty)
                .expireDate(expireStr.isEmpty() ? null : LocalDate.parse(expireStr))
                .build();

        try {
            inboundCmdPublisher.send(dto);
            System.out.println("발행 완료. messageId=" + dto.getMessageId());
        } catch (Exception e) {
            System.out.println("발행 실패: " + e.getMessage());
        }
    }

    private String prompt(Scanner scanner, String field, String defaultValue) {
        System.out.printf("  %-40s [%s] > ", field, defaultValue);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
}
