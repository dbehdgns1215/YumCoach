package com.ssafy.yumcoach.report.scheduler;

import com.ssafy.yumcoach.report.service.ReportService;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.model.mapper.ReportMapper;
import com.ssafy.yumcoach.user.model.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduler {

    private final ReportService reportService;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    // 매일 새벽 1시 (Asia/Seoul) - 전날 일간 리포트 생성
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void scheduleDailyReports() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(zone).minusDays(1); // 전 날
        log.info("[ReportScheduler] Running daily reports for date={}", target);

        List<Integer> userIds = userMapper.findAllUserIds();
        int success = 0; int skipped = 0; int errors = 0;

        for (Integer uid : userIds) {
            try {
                ReportDto dto = reportService.createDailyReport(uid, target);
                // 스케줄러가 만든 리포트는 완료 상태로 표기
                if (dto != null && dto.getId() != null) {
                    try {
                        reportMapper.updateReportStatusCreatedBy(dto.getId(), "COMPLETED", "SYSTEM");
                    } catch (Exception ex) {
                        log.warn("[ReportScheduler] failed to mark report as COMPLETED for reportId={}: {}", dto.getId(), ex.toString());
                    }
                }
                success++;
                // 짧은 인터벌로 DB 부담 완화
                Thread.sleep(50);
            } catch (IllegalStateException ise) {
                // LIMIT_EXCEEDED or NO_MEALS 등은 건너뜀
                log.debug("[ReportScheduler] skip user {}: {}", uid, ise.getMessage());
                skipped++;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[ReportScheduler] interrupted while sleeping between user reports");
            } catch (Exception e) {
                errors++;
                log.error("[ReportScheduler] failed for user {}: {}", uid, e.toString());
            }
        }

        log.info("[ReportScheduler] Daily reports finished: success={}, skipped={}, errors={}", success, skipped, errors);
    }

    // 매주 월요일 새벽 1시 (Asia/Seoul) - 전주 주간 리포트 생성
    @Scheduled(cron = "0 0 1 * * MON", zone = "Asia/Seoul")
    public void scheduleWeeklyReports() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        // 지난 주 월요일 ~ 일요일
        LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate lastSunday = lastMonday.plusDays(6);

        log.info("[ReportScheduler] Running weekly reports for from={} to={}", lastMonday, lastSunday);

        List<Integer> userIds = userMapper.findAllUserIds();
        int success = 0; int skipped = 0; int errors = 0;

        for (Integer uid : userIds) {
            try {
                ReportDto dto = reportService.createWeeklyReport(uid, lastMonday, lastSunday);
                if (dto != null && dto.getId() != null) {
                    try {
                        reportMapper.updateReportStatusCreatedBy(dto.getId(), "COMPLETED", "SYSTEM");
                    } catch (Exception ex) {
                        log.warn("[ReportScheduler] failed to mark weekly report as COMPLETED for reportId={}: {}", dto.getId(), ex.toString());
                    }
                }
                success++;
                Thread.sleep(100);
            } catch (IllegalStateException ise) {
                log.debug("[ReportScheduler] skip weekly for user {}: {}", uid, ise.getMessage());
                skipped++;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[ReportScheduler] interrupted while sleeping between weekly reports");
            } catch (Exception e) {
                errors++;
                log.error("[ReportScheduler] weekly failed for user {}: {}", uid, e.toString());
            }
        }

        log.info("[ReportScheduler] Weekly reports finished: success={}, skipped={}, errors={}", success, skipped, errors);
    }
}
