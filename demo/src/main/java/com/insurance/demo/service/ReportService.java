package com.insurance.demo.service;

import com.insurance.demo.model.ContractReport;
import com.insurance.demo.model.GenerateReportRequest;

import java.util.ArrayList;
import java.util.Map;


/**
 * The ReportService class has the business logic to generate Report based of the request from Controller class
 *
 *  @author Priyanka Vaidyanathan - 24/09/2021
 */
public interface ReportService {

    void uploadReport(GenerateReportRequest generateReportRequest);

    ArrayList<ContractReport> getReportForCreatedAndTerminatedEvents(int year);

    ArrayList<ContractReport> getReportForAllEvents(int year);


}
