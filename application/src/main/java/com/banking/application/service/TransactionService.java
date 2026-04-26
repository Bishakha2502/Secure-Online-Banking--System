package com.banking.application.service;

import com.banking.application.dto.TransactionDto;

public interface TransactionService {

	void saveTransaction(TransactionDto transactionDto);
}
