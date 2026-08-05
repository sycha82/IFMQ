package com.example.rcsmock.sim;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 설비 자동 시뮬레이션 설정 (application.yml: rcs.auto-simulation.*)
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rcs.auto-simulation")
public class AutoSimulationProperties {

    /** 자동 시뮬레이션 사용 여부. false 면 기존처럼 CLI/REST 수동 조작만 동작한다. */
    private boolean enabled = true;

    /** TASK 수신 → START 전송까지 지연(ms). 설비가 팔렛을 집기까지 걸리는 시간. */
    private long startDelayMs = 5000;

    /** START → DONE 전송까지 지연(ms). 설비가 이동·적재/배출을 마치기까지 걸리는 시간. */
    private long doneDelayMs = 10000;

    /**
     * 같은 스테이션에서 앞 팔렛이 완료(DONE)된 뒤 다음 팔렛이 착수(START)하기까지의 간격(ms).
     * 한 스테이션에 여러 팔렛이 동시에 도착할 수 없으므로 작업을 직렬화하는 데 쓰인다.
     */
    private long gapMs = 2000;

    /** 시뮬레이션에 사용할 셔틀 ID (TASK_ACK 에서 배정한 값이 없을 때의 대체값). */
    private String defaultShuttleId = "SHUTTLE-SIM";
}
