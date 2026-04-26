package com.banking.application.controller;

import java.io.FileNotFoundException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.banking.application.entity.Transaction;
import com.banking.application.service.BankStatement;
import com.itextpdf.text.DocumentException;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/bankStatement")
@AllArgsConstructor
public class TransactionController {

	private BankStatement bankStatement;
	@GetMapping
	public List<Transaction> generatedBankStatement(@RequestParam String accountNumber,
			@RequestParam String startDate,
			@RequestParam String endDate) throws DocumentException , FileNotFoundException{
		return bankStatement.generateStatement(accountNumber, startDate, endDate);
	}
}
