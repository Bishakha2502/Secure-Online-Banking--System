package com.banking.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.banking.application.dto.TransactionDto;
import com.banking.application.entity.Transaction;
import com.banking.application.repository.TransactionRepository;

@Component
public class TransactionImpl implements TransactionService {

	@Autowired
	TransactionRepository transactionRepository;
	@Override
	public void saveTransaction(TransactionDto transactionDto) {
		Transaction transaction = Transaction.builder()
				.transactionType(transactionDto.getTransactionType())
				.accountNumber(transactionDto.getAccountNumber())
				.amount(transactionDto.getAmount())
				.status("SUCCESS...")
				.build();
		transactionRepository.save(transaction);
		System.out.println("Transaction saved successfully...");
	}

}
