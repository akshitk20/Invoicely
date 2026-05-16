package com.invoicely.controller;

import com.invoicely.model.User;
import com.invoicely.service.ExportService;
import com.invoicely.service.ReportService;
import com.invoicely.service.UserService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;
    private final UserService userService;

    public ReportController(ReportService reportService,
                            ExportService exportService,
                            UserService userService) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.userService = userService;
    }

    @GetMapping
    public String showReports(Model model) {
        return "reports/index";
    }

    @GetMapping("/gst")
    public ResponseEntity<byte[]> downloadGstReport(@AuthenticationPrincipal OAuth2User oAuth2User,
                                                     @RequestParam LocalDate startDate,
                                                     @RequestParam LocalDate endDate) throws IOException {
        User user = userService.getCurrentUser(oAuth2User);
        byte[] report = reportService.generateGstSummaryExcel(user, startDate, endDate);
        return buildExcelResponse(report, "gst_summary.xlsx");
    }

    @GetMapping("/income")
    public ResponseEntity<byte[]> downloadIncomeReport(@AuthenticationPrincipal OAuth2User oAuth2User,
                                                        @RequestParam LocalDate startDate,
                                                        @RequestParam LocalDate endDate) throws IOException {
        User user = userService.getCurrentUser(oAuth2User);
        byte[] report = reportService.generateIncomeReportExcel(user, startDate, endDate);
        return buildExcelResponse(report, "income_report.xlsx");
    }

    @GetMapping("/expenses")
    public ResponseEntity<byte[]> downloadExpenseReport(@AuthenticationPrincipal OAuth2User oAuth2User,
                                                         @RequestParam LocalDate startDate,
                                                         @RequestParam LocalDate endDate) throws IOException {
        User user = userService.getCurrentUser(oAuth2User);
        byte[] report = reportService.generateExpenseReportExcel(user, startDate, endDate);
        return buildExcelResponse(report, "expense_report.xlsx");
    }

    @GetMapping("/ca-pack")
    public ResponseEntity<byte[]> downloadCaPack(@AuthenticationPrincipal OAuth2User oAuth2User,
                                                  @RequestParam LocalDate startDate,
                                                  @RequestParam LocalDate endDate) throws IOException {
        User user = userService.getCurrentUser(oAuth2User);
        byte[] zip = exportService.generateCaExportPack(user, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("CA_Pack_" + startDate + "_to_" + endDate + ".zip")
            .build());

        return new ResponseEntity<>(zip, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(filename)
            .build());
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
