package com.insurance.demo.controller;

import com.insurance.demo.model.ContractReport;
import com.insurance.demo.model.GenerateReportRequest;
import com.insurance.demo.service.ReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.InternalServerErrorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The ReportController class is responsible for processing incoming REST API requests for generating Report
 *
 * @author Priyanka Vaidyanathan - 24/09/2021
 */

@RestController
public class ReportController {

    public static final String STATUS = "status";
    public static final String ERROR_CODE = "errorCode";

    @Autowired private ReportService reportService;

    @PostMapping(
            value = "/api/report/uploadReport",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadReport(@ModelAttribute GenerateReportRequest generateReportRequest) {
        try {

            reportService.uploadReport(generateReportRequest);

        } catch (HttpClientErrorException e) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("message", e.getStatusText());
            responseMap.put(ERROR_CODE, "ERROR");
            responseMap.put(STATUS, "BAD_REQUEST");
            return ResponseEntity.badRequest().body(responseMap);
        } catch (Exception e) {
            String msg = "Error";
        }

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(STATUS, "SUCCESS");
        return ResponseEntity.ok(responseMap);
    }

    @GetMapping(value = "/api/{year}/getReport")
    public ArrayList<ContractReport> getReportForCreatedAndTerminatedEvents(@PathVariable int year) {

        ArrayList<ContractReport> response = reportService.getReportForCreatedAndTerminatedEvents(year);
        if (response == null) {
            String msg = "Error in fetching Report for Contract Created Event & Contract Terminated Event";
            throw new InternalServerErrorException(msg);
        }

        return response;

    }

    @GetMapping(value = "/api/{year}/getAllReport")
    public ArrayList<ContractReport> getReportForAllEvents(@PathVariable int year) {

        ArrayList<ContractReport> response = reportService.getReportForAllEvents(year);
        if (response == null) {
            String msg = "Error in fetching Report for All events.";
            throw new InternalServerErrorException(msg);
        }

        return response;

    }

}