package com.insurance.demo.service.impl;

import com.insurance.demo.model.Contract;
import com.insurance.demo.model.ContractReport;
import com.insurance.demo.model.GenerateReportRequest;
import com.insurance.demo.model.db.*;
import com.insurance.demo.repository.*;
import com.insurance.demo.service.ReportService;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.*;
import java.time.temporal.*;
import java.util.stream.Collectors;

/**
 * The ReportServiceImpl class has the implementation logic for ReportService class
 *
 * @author Priyanka Vaidyanathan - 24/09/2021
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    ContractCreatedEventRepository contractCreatedEventRepository;
    @Autowired
    ContractTerminatedEventRepository contractTerminatedEventRepository;
    @Autowired
    PriceDecreasedEventRepository priceDecreasedEventRepository;
    @Autowired
    PriceIncreasedEventRepository priceIncreasedEventRepository;

    /**
     * This method accepts Multipart file and saves the Json Object as Different events
     *
     * @param  generateReportRequest  File that contains json objects of ContractCreatedEvent,ContractTerminatedEvent
     * PriceDecreasedEvent & PriceIncreasedEvent
     *
     */

    @Override
    public void uploadReport(GenerateReportRequest generateReportRequest) {
        File file = null;
        try {

            MultipartFile multipartFile = generateReportRequest.getFile();
            file = convertMultiPartToFile(multipartFile, "src/temp");
            ArrayList<ContractCreatedEvent> contractCreatedEventArrayList = new ArrayList<>();
            ArrayList<PriceDecreasedEvent> priceDecreasedEventArrayList = new ArrayList<>();
            ArrayList<PriceIncreasedEvent> priceIncreasedEventArrayList = new ArrayList<>();
            ArrayList<ContractTerminatedEvent> contractTerminatedEventArrayList = new ArrayList<>();

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                contractCreatedEventRepository.deleteAll();
                contractTerminatedEventRepository.deleteAll();
                priceDecreasedEventRepository.deleteAll();
                priceIncreasedEventRepository.deleteAll();

                JSONObject json = new JSONObject(line);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                String name = json.get("name").toString();

                if (name.equalsIgnoreCase("ContractCreatedEvent")) {
                    ContractCreatedEvent contractCreatedEvent = new ContractCreatedEvent();
                    contractCreatedEvent.setContractId(Integer.parseInt(json.get("contractId").toString()));
                    contractCreatedEvent.setDate(LocalDate.parse(json.get("startDate").toString(), formatter));
                    contractCreatedEvent.setPremium((Integer) json.get("premium"));
                    contractCreatedEventArrayList.add(contractCreatedEvent);

                } else if (name.equalsIgnoreCase("PriceDecreasedEvent")) {
                    PriceDecreasedEvent priceDecreasedEvent = new PriceDecreasedEvent();
                    priceDecreasedEvent.setContractId(Integer.parseInt(json.get("contractId").toString()));
                    priceDecreasedEvent.setAtDate(LocalDate.parse(json.get("atDate").toString(), formatter));
                    priceDecreasedEvent.setPremiumDecrease((Integer) json.get("premiumReduction"));
                    priceDecreasedEventArrayList.add(priceDecreasedEvent);
                } else if (name.equalsIgnoreCase("PriceIncreasedEvent")) {
                    PriceIncreasedEvent priceIncreasedEvent = new PriceIncreasedEvent();
                    priceIncreasedEvent.setAtDate(LocalDate.parse(json.get("atDate").toString(), formatter));
                    priceIncreasedEvent.setContractId(Integer.parseInt(json.get("contractId").toString()));
                    priceIncreasedEvent.setPremiumIncrease((Integer) json.get("premiumIncrease"));
                    priceIncreasedEventArrayList.add(priceIncreasedEvent);
                } else if (name.equalsIgnoreCase("ContractTerminatedEvent")) {
                    ContractTerminatedEvent contractTerminatedEvent = new ContractTerminatedEvent();
                    contractTerminatedEvent.setContractId(Integer.parseInt(json.get("contractId").toString()));
                    contractTerminatedEvent.setDate(LocalDate.parse(json.get("terminationDate").toString(), formatter));
                    contractTerminatedEventArrayList.add(contractTerminatedEvent);
                }

            }

            contractCreatedEventRepository.saveAll(contractCreatedEventArrayList);
            contractTerminatedEventRepository.saveAll(contractTerminatedEventArrayList);
            priceDecreasedEventRepository.saveAll(priceDecreasedEventArrayList);
            priceIncreasedEventRepository.saveAll(priceIncreasedEventArrayList);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            deleteFile(file.getAbsolutePath(), false);
        }

    }

    /**
     * Returns an ArrayList<ContractReport> that has the metrics required for the Report for the Insurance system
     * for ContractCreatedEvent,ContractTerminatedEvent
     *
     */

    public ArrayList<ContractReport> getReportForCreatedAndTerminatedEvents(int year) {

        try {

            HashMap<String, ContractReport> map = new HashMap<>();

            ArrayList contractReportList = null;

            for (Month month: Month.values()) {
                map.put(month.toString(), calculateMetrics(year, false, month, map.get(month.minus(1).toString())));
            }

            // Getting Collection of values from HashMap
            Collection<ContractReport> values = map.values();
            contractReportList = (ArrayList) values.stream().filter(x -> x != null).collect(Collectors.toList());
            return contractReportList;

        } catch (Exception e) {

            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error.Please try again");

        }

    }

    /**
     * Returns an ArrayList<ContractReport> that has the metrics required for the Report for the Insurance system
     * for ContractCreatedEvent,ContractTerminatedEvent,PriceDecreasedEvent & PriceIncreasedEvent
     *
     */

    public ArrayList<ContractReport> getReportForAllEvents(int year) {

        try {

            HashMap<String, ContractReport> map = new HashMap<>();

            ArrayList<ContractReport> contractReportList = new ArrayList<>();

            for (Month month: Month.values()) {

                map.put(month.toString(), calculateMetrics(year, true, month, map.get(month.minus(1).toString())));

            }
            // Getting Collection of values from HashMap
            Collection<ContractReport> values = map.values();
            contractReportList = (ArrayList) values.stream().filter(x -> x != null).collect(Collectors.toList());
            return contractReportList;

        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error.Please try again");
        }

    }

    private File convertMultiPartToFile(MultipartFile file, String tempFolderPath)
            throws IOException {
        String filePath = tempFolderPath + file.getOriginalFilename();
        File tempFile = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        return tempFile;
    }

    private void deleteFile(String filePath, boolean deleteDirectory) {
        try {
            File f = new File(filePath);
            if (deleteDirectory) {
                FileUtils.deleteDirectory(f.getParentFile());
            } else {
                FileUtils.forceDelete(f);
            }
        } catch (IOException e) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error in deleting temp file");
        }
    }



    private ContractReport calculateMetrics(int year, boolean allEvents, Month month, ContractReport previousContractReport) {
        int totalNumberOfContracts = 0;
        Integer yearlyPremium = 0;
        Integer monthlyPremium = 0;
        LocalDate lastDayOfMonth;
        LocalDate firstDayOfMonth;
        ArrayList<Contract> contractList = null;
        int carriedOverYearlyPremium = 0;
        ArrayList<Contract> previousMonthContractList = null;
        ArrayList<Contract> terminatedContracts = null;

        int monthIntValue = month.getValue();

        firstDayOfMonth = Year.of(year).atMonth(month).atDay(1).with(TemporalAdjusters.firstDayOfMonth());
        lastDayOfMonth = Year.of(year).atMonth(month).atDay(1).with(TemporalAdjusters.lastDayOfMonth());

        ArrayList<ContractCreatedEvent> createdEventsListOfTheCurrentMonth = contractCreatedEventRepository.findAllByDateBetween(firstDayOfMonth, lastDayOfMonth);

        if (createdEventsListOfTheCurrentMonth != null) {

            contractList = (ArrayList<Contract> ) createdEventsListOfTheCurrentMonth.stream().map(x -> createContractObject(x)).collect(Collectors.toList());
            totalNumberOfContracts = createdEventsListOfTheCurrentMonth.size();
        }

        if (previousContractReport != null && previousContractReport.getContractList() != null) {

            previousMonthContractList = (ArrayList<Contract> ) previousContractReport.getContractList().clone();
            totalNumberOfContracts = totalNumberOfContracts + previousContractReport.getContractList().size();
            contractList.addAll(previousMonthContractList);
            carriedOverYearlyPremium = previousContractReport.getCarriedOverYearlyPremium();
            monthlyPremium = previousContractReport.getMonthPremium() + monthlyPremium;
        }

        if (totalNumberOfContracts == 0) {
            return null;
        }

        if (allEvents) {

            ArrayList<PriceIncreasedEvent> priceIncreasedEventArrayList = priceIncreasedEventRepository.findAllByAtDateBetween(firstDayOfMonth, lastDayOfMonth);
            ArrayList<PriceDecreasedEvent> priceDecreasedEventArrayList = priceDecreasedEventRepository.findAllByAtDateBetween(firstDayOfMonth, lastDayOfMonth);

            if (priceDecreasedEventArrayList != null) {

                contractList.stream().forEach(x -> calculateDecreaseInPremium(x, priceDecreasedEventArrayList, month));

            }

            if (priceIncreasedEventArrayList != null) {

                contractList.stream().forEach(x -> calculateIncreaseInPremium(x, priceIncreasedEventArrayList, month));

            }
            ArrayList<Integer> contractIdsOfChangeEvents = (ArrayList) priceIncreasedEventArrayList.stream().map(x -> x.getContractId()).collect(Collectors.toList());
            contractIdsOfChangeEvents .addAll((ArrayList) priceDecreasedEventArrayList.stream().map(x -> x.getContractId()).collect(Collectors.toList()));

            if (! contractIdsOfChangeEvents .isEmpty()) {
                ArrayList<Contract> unchanged = (ArrayList<Contract> )
                        contractList.stream()
                                .filter(
                                        x -> contractIdsOfChangeEvents .stream()
                                                .anyMatch(c -> c != x.getContractId()))
                                .collect(Collectors.toList());

                unchanged.stream().forEach(x -> {
                    x.getPriceChange().put(month, x.getPremium());
                });
            } else {
                contractList.stream().forEach(x -> {
                    x.getPriceChange().put(month, x.getPremium());
                });
            }

            contractList.stream().forEach(x -> x.setPremium(x.getPriceChange().get(month)));

        }

        monthlyPremium = contractList.stream().map(x -> x.getPremium()).reduce(0, Integer::sum) + monthlyPremium;

        ArrayList<ContractTerminatedEvent> terminatedEventsOfTheCurrentMonth = contractTerminatedEventRepository.findAllByDateBetween(firstDayOfMonth, lastDayOfMonth);

        if (terminatedEventsOfTheCurrentMonth != null) {

            terminatedContracts = (ArrayList<Contract> )
                    contractList.stream()
                            .filter(
                                    x -> terminatedEventsOfTheCurrentMonth.stream()
                                            .anyMatch(c -> c.getContractId() == x.getContractId()))
                            .collect(Collectors.toList());
        }
        if (!allEvents) {
            contractList.removeAll(terminatedContracts);
            carriedOverYearlyPremium = carriedOverYearlyPremium + calculateCarriedOverYearlyPremiumForTerminatedEvents(monthIntValue, terminatedEventsOfTheCurrentMonth);
            yearlyPremium = (((contractList.stream().map(x -> x.getPremium()).reduce(0, Integer::sum))) * 12) + carriedOverYearlyPremium;
        } else {

            carriedOverYearlyPremium = carriedOverYearlyPremium + calculateCarriedOverYearlyPremiumForTerminatedEventsForAllEvents(monthIntValue, contractList, terminatedEventsOfTheCurrentMonth);
            contractList.removeAll(terminatedContracts);
            for (int i = 0; i<month.ordinal(); i++) {
                for (Contract contract: contractList) {
                    if (contract.getPriceChange().get(Month.of(i + 1)) != null) {
                        yearlyPremium = yearlyPremium + contract.getPriceChange().get(Month.of(i + 1));
                    }

                }
            }
            yearlyPremium = yearlyPremium + (((contractList.stream().map(x -> x.getPremium()).reduce(0, Integer::sum)))) * (12 - month.ordinal()) + carriedOverYearlyPremium;

        }

        ContractReport contractReport = new ContractReport();
        contractReport.setNoOfContracts(totalNumberOfContracts);
        contractReport.setMonthPremium(monthlyPremium);
        contractReport.setMonth(month);
        contractReport.setYearPremium(yearlyPremium);
        contractReport.setContractList(contractList);
        contractReport.setCarriedOverYearlyPremium(carriedOverYearlyPremium);
        return contractReport;

    }

    Contract createContractObject(ContractCreatedEvent event) {
        Contract contract = new Contract();
        contract.setContractId(event.getContractId());
        contract.setPremium(event.getPremium());
        HashMap<Month, Integer> priceChange = new HashMap<>();
        priceChange.put(event.getDate().getMonth(), event.getPremium());
        contract.setPriceChange(priceChange);
        return contract;

    }

    Integer calculateCarriedOverYearlyPremiumForTerminatedEvents(Integer currentMonth, ArrayList<ContractTerminatedEvent> contractTerminatedEventList) {

        int carriedOverPremium = 0;

        if (contractTerminatedEventList != null) {
            for (ContractTerminatedEvent contractTerminatedEvent: contractTerminatedEventList) {
                Integer premium = (contractCreatedEventRepository.getPremiumByContractId(contractTerminatedEvent.getContractId())).get(0);
                Integer createdMonth = contractCreatedEventRepository.getMonthFromContractId(contractTerminatedEvent.getContractId());
                carriedOverPremium = carriedOverPremium + (((currentMonth - createdMonth) + 1) * premium);
            }
        }

        return carriedOverPremium;

    }

    Integer calculateCarriedOverYearlyPremiumForTerminatedEventsForAllEvents(Integer m, ArrayList<Contract> contractList, ArrayList<ContractTerminatedEvent> contractTerminatedEventList) {

        final int[] carriedOverPremium = { 0 };

        if (contractTerminatedEventList != null) {
            for (ContractTerminatedEvent contractTerminatedEvent: contractTerminatedEventList) {
                contractList.stream().filter(x -> x.getContractId() == contractTerminatedEvent.getContractId()).forEach(x -> {
                    carriedOverPremium[0] = carriedOverPremium[0] + x.getPriceChange().values().stream().reduce(0, Integer::sum);
                });

            }
        }

        return carriedOverPremium[0];

    }

    public void calculateDecreaseInPremium(Contract contract, ArrayList<PriceDecreasedEvent> priceDecreasedEvents, Month month) {

        priceDecreasedEvents.stream().filter(x -> x.getContractId() == contract.getContractId()).forEach(x -> {
            final int[] monthPremium = { 0 };
            monthPremium[0] = contract.getPriceChange().get(month) == null ? monthPremium[0] : monthPremium[0] + contract.getPriceChange().get(month);
            if (contract.getPriceChange().get(month) != null) {
                contract.getPriceChange().put(month, ((contract.getPriceChange().get(month)) - (x.getPremiumDecrease())));
            } else {
                contract.getPriceChange().put(month, ((contract.getPriceChange().get(month.minus(1))) - (x.getPremiumDecrease())));
            }
        });

        //#Todo Need to add Negative Validation

    }

    public void calculateIncreaseInPremium(Contract contract, ArrayList<PriceIncreasedEvent> priceIncreasedEvents, Month month) {

        priceIncreasedEvents.stream().filter(x -> x.getContractId() == contract.getContractId()).forEach(x -> {
            final int[] monthPremium = { 0 };
            monthPremium[0] = contract.getPriceChange().get(month) == null ? monthPremium[0] : monthPremium[0] + contract.getPriceChange().get(month);
            if (contract.getPriceChange().get(month) != null) {
                contract.getPriceChange().put(month, (x.getPremiumIncrease() + contract.getPriceChange().get(month)));
            } else {
                contract.getPriceChange().put(month, (x.getPremiumIncrease() + contract.getPriceChange().get(month.minus(1))));
            }
        });

    }

}