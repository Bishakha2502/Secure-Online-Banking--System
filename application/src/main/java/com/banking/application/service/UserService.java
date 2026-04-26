package com.banking.application.service;

import com.banking.application.dto.BankResponse;
import com.banking.application.dto.CreditDebitRequest;
import com.banking.application.dto.EnquiryRequest;
import com.banking.application.dto.LoginDto;
import com.banking.application.dto.TransferRequest;
import com.banking.application.dto.UserRequest;

public interface UserService {

	BankResponse createAccount(UserRequest userRequest);
	BankResponse balanceEnquiry(EnquiryRequest request);
	String nameEnquiry(EnquiryRequest request);
	BankResponse creditAccount(CreditDebitRequest request);
	BankResponse debitAccount(CreditDebitRequest request);
	BankResponse transfer(TransferRequest request);
	BankResponse login(LoginDto loginDto);
}
