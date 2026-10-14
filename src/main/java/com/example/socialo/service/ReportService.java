package com.example.socialo.service;

import com.example.socialo.model.Report;
import com.example.socialo.model.User;
import com.example.socialo.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    @Autowired private ReportRepository reportRepository;

    @Transactional
    public Report submitReport(User reporter, Long reportedUserId, Long reportedPostId,
                               String category, String description, String priority) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUserId(reportedUserId);
        report.setReportedPostId(reportedPostId);
        report.setCategory(category);
        report.setDescription(description);
        report.setPriority(priority != null ? priority : "NORMAL");
        report.setStatus("OPEN");
        return reportRepository.save(report);
    }

    public List<Report> getAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Report> getByStatus(String status) {
        return reportRepository.findByStatus(status);
    }

    @Transactional
    public void resolve(Long reportId) {
        reportRepository.findById(reportId).ifPresent(r -> {
            r.setStatus("RESOLVED");
            reportRepository.save(r);
        });
    }

    @Transactional
    public void dismiss(Long reportId) {
        reportRepository.findById(reportId).ifPresent(r -> {
            r.setStatus("DISMISSED");
            reportRepository.save(r);
        });
    }

    public long countOpen() {
        return reportRepository.countByStatus("OPEN");
    }

    public long countAll() {
        return reportRepository.count();
    }
}
