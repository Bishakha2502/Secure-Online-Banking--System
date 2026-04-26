package com.banking.application.service;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.banking.application.config.JwtTokenProvider;
import com.banking.application.dto.AccountInfo;
import com.banking.application.dto.BankResponse;
import com.banking.application.dto.CreditDebitRequest;
import com.banking.application.dto.EmailDetails;
import com.banking.application.dto.EnquiryRequest;
import com.banking.application.dto.LoginDto;
import com.banking.application.dto.TransactionDto;
import com.banking.application.dto.TransferRequest;
import com.banking.application.dto.UserRequest;
import com.banking.application.entity.Role;
import com.banking.application.entity.User;
import com.banking.application.repository.UserRepository;
import com.banking.application.utils.AccountUtils;

import lombok.AllArgsConstructor;


@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

	@Autowired
	UserRepository userRepository;
	
	@Autowired
	EmailService emailService;
	
	@Autowired
	TransactionService transactionService;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Autowired
	AuthenticationManager authenticationManager;
	
	@Autowired
	JwtTokenProvider jwtTokenProvider;
	
	@Override
	public BankResponse createAccount(UserRequest userRequest) {
		/*
		 * creating an account - saving a new user into the database
		 * check if user already has an account
		 */
		
		if(userRepository.existsByEmail(userRequest.getEmail())) {
			return BankResponse.builder()
					.responseCode(AccountUtils.ACCOUNT_EXISTS_CODE)
					.responseMessage(AccountUtils.ACCOUNT_EXISTS_MESSAGE)
					.accountInfo(null)
					.build();
			
		}
		User newUser = User.builder()
				.firstName(userRequest.getFirstName())
				.lastName(userRequest.getLastName())
				.otherName(userRequest.getOtherName())
				.gender(userRequest.getGender())
				.address(userRequest.getAddress())
				.stateOfOrigin(userRequest.getStateOfOrigin())
				.accountNumber(AccountUtils.generateAccountNumber())
				.accountBalance(BigDecimal.ZERO)
				.email(userRequest.getEmail())
				.password(passwordEncoder.encode(userRequest.getPassword()))
				.phoneNumber(userRequest.getPhoneNumber())
				.alternativePhoneNumber(userRequest.getAlternativePhoneNumber())
				.status("ACTIVE")
				.role(Role.valueOf("ROLE_ADMIN"))
				.build();
		
		//Example
		//BigDecimal.valueOf(100);
		
		User savedUser = userRepository.save(newUser);
		
		//send email alert
		EmailDetails emailDetails = EmailDetails.builder()
				.recipient(savedUser.getEmail())
				.subject("ACCOUNT CREATION")
				.messageBody("Congratulations! Your Account Has Been Successfully Created.\nYour Account Details: \n" + 
				     "ACCOUNT NAME: " + savedUser.getFirstName() + " " + savedUser.getLastName() + " " + savedUser.getOtherName() + "\nACCOUNT NUMBER: " + savedUser.getAccountNumber())
				.build();
		emailService.sendEmailAlert(emailDetails);
		
		return BankResponse.builder()
				.responseCode(AccountUtils.ACCOUNT_CREATION_SUCCESS)
				.responseMessage(AccountUtils.ACCOUNT_CREATION_MESSAGE)
				.accountInfo(AccountInfo.builder()
						.accountBalance(savedUser.getAccountBalance())
						.accountNumber(savedUser.getAccountNumber())
						.accountName(savedUser.getFirstName() + " " + savedUser.getLastName() + " " + savedUser.getOtherName())
						.build())
				.build();
	}

	public BankResponse Login(LoginDto loginDto) {
		Authentication authentication = null;
		authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
		);
		
		EmailDetails loginAlert = EmailDetails.builder()
				.subject("You're logged in...")
				.recipient(loginDto.getEmail())
				.messageBody("You logged into your account. If you did not initiate this request , please contact your bank....")
				.build();
		emailService.sendEmailAlert(loginAlert);
		
		return BankResponse.builder()
				.responseCode("Login Successfully..")
				.responseMessage(jwtTokenProvider.generateToken(authentication))
				.build();
	}
	
	
	@Override
	public BankResponse balanceEnquiry(EnquiryRequest request) {
		//check if the provided account number exists in the db
		boolean isAccountExist = userRepository.existsByAccountNumber(request.getAccountNumber());
		if(!isAccountExist) {
			return BankResponse.builder()
					.responseCode(AccountUtils.ACCOUNT_NOT_EXIST_CODE)
					.responseMessage(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE)
					.accountInfo(null)
					.build();
		}
		
		User foundUser = userRepository.findByAccountNumber(request.getAccountNumber());
		return BankResponse.builder()
				.responseCode(AccountUtils.ACCOUNT_FOUND_CODE)
				.responseMessage(AccountUtils.ACCOUNT_FOUND_SUCCESS)
				.accountInfo(AccountInfo.builder()
						.accountBalance(foundUser.getAccountBalance())
						.accountNumber(request.getAccountNumber())
						.accountName(foundUser.getFirstName() + " " + foundUser.getLastName() + " " + foundUser.getOtherName())
						.build())
				.build();
	}

	@Override
	public String nameEnquiry(EnquiryRequest request) {
		boolean isAccountExist = userRepository.existsByAccountNumber(request.getAccountNumber());
				if(!isAccountExist) {
					return AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE;
				}
				User foundUser=userRepository.findByAccountNumber(request.getAccountNumber());
				return foundUser.getFirstName() + " " + foundUser.getLastName() + " " + foundUser.getOtherName();
				
	}

	@Override
	public BankResponse creditAccount(CreditDebitRequest request) {
	//checking account is exists
		boolean isAccountExist = userRepository.existsByAccountNumber(request.getAccountNumber());
		if(!isAccountExist)	{
			return BankResponse.builder()
					.responseCode(AccountUtils.ACCOUNT_NOT_EXIST_CODE)
					.responseMessage(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE)
					.accountInfo(null)
					.build();
			}
		
		User userToCredit = userRepository.findByAccountNumber(request.getAccountNumber());
		userToCredit.setAccountBalance(userToCredit.getAccountBalance().add(request.getAmount()));
		userRepository.save(userToCredit);
		
		//save transaction
		TransactionDto transactionDto = TransactionDto.builder()
				.accountNumber(userToCredit.getAccountNumber())
				.transactionType("CREDIT")
				.amount(request.getAmount())
				.build();
		transactionService.saveTransaction(transactionDto);
		
		//send email alert
		EmailDetails email = EmailDetails.builder()
		        .recipient(userToCredit.getEmail())
		        .subject("CREDIT ALERT")
		        .messageBody(
		                "Dear " + userToCredit.getFirstName() + " " + userToCredit.getLastName() + ",\n\n" +
		                "₹" + request.getAmount() + " has been credited to your account.\n\n" +
		                "Account Number : " + userToCredit.getAccountNumber() + "\n" +
		                "Available Balance : ₹" + userToCredit.getAccountBalance()
		        )
		        .build();

		emailService.sendEmailAlert(email);
		
		return BankResponse.builder()
				.responseCode(AccountUtils.ACCOUNT_CREDITED_SUCCESS)
				.responseMessage(AccountUtils.ACCOUNT_CREDITED_MESSAGE)
				.accountInfo(AccountInfo.builder()
						.accountName(userToCredit.getFirstName() + " " + userToCredit.getLastName() + " " + userToCredit.getOtherName())
						.accountBalance(userToCredit.getAccountBalance())
					    .accountNumber(request.getAccountNumber())
						.build())
				.build();
	}

	@Override
	public BankResponse debitAccount(CreditDebitRequest request) {
		//check if the account exists
		//check if the amount you intend to withdraw is not more than the current account balance
		
		boolean isAccountExist = userRepository.existsByAccountNumber(request.getAccountNumber());
		if(!isAccountExist)	{
			return BankResponse.builder()
					.responseCode(AccountUtils.ACCOUNT_NOT_EXIST_CODE)
					.responseMessage(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE)
					.accountInfo(null)
					.build();
			}
		
		User userToDebit = userRepository.findByAccountNumber(request.getAccountNumber());
		BigInteger availableBalance = userToDebit.getAccountBalance().toBigInteger();
		BigInteger debitAmount = request.getAmount().toBigInteger();
				
		if(availableBalance.intValue() < debitAmount.intValue())	{
			return BankResponse.builder()
					.responseCode(AccountUtils.INSUFFICIENT_BALANCE_CODE)
					.responseMessage(AccountUtils.INSUFFICIENT_BALANCE_MESSAGE)
					.accountInfo(null)
					.build();
		}
		else {
			userToDebit.setAccountBalance(userToDebit.getAccountBalance().subtract(request.getAmount()));
			userRepository.save(userToDebit);
			
			TransactionDto transactionDto = TransactionDto.builder()
					.accountNumber(userToDebit.getAccountNumber())
					.transactionType("DEBIT")
					.amount(request.getAmount())
					.build();
			transactionService.saveTransaction(transactionDto);
			
			//send email alert
			EmailDetails debitEmail = EmailDetails.builder()
			        .recipient(userToDebit.getEmail())
			        .subject("DEBIT ALERT")
			        .messageBody(
			                "Dear " + userToDebit.getFirstName() + " " + userToDebit.getLastName() + ",\n\n" +
			                "An amount of ₹" + request.getAmount() + " has been debited from your account.\n\n" +
			                "Account Number : " + userToDebit.getAccountNumber() + "\n" +
			                "Available Balance : ₹" + userToDebit.getAccountBalance() + "\n\n" +
			                "Date : " + java.time.LocalDateTime.now() + "\n\n" +
			                "Thank you for banking with us.\n" +
			                "Please do not reply to this automated email."
			        )
			        .build();

			emailService.sendEmailAlert(debitEmail);
			
			return BankResponse.builder()
					.responseCode(AccountUtils.ACCOUNT_DEBITED_SUCCESS)
					.responseMessage(AccountUtils.ACCOUNT_DEBITED_MESSAGE)
					.accountInfo(AccountInfo.builder()
							.accountNumber(request.getAccountNumber())
							.accountName(userToDebit.getFirstName() + " " + userToDebit.getLastName() + " " + userToDebit.getOtherName())
							.accountBalance(userToDebit.getAccountBalance())
							.build())
					.build();
			
		}
	}

	@Override
	public BankResponse transfer(TransferRequest request) {

	    boolean isDestinationAccountExist =
	            userRepository.existsByAccountNumber(request.getDestinationAccountNumber());

	    if (!isDestinationAccountExist) {
	        return BankResponse.builder()
	                .responseCode(AccountUtils.ACCOUNT_NOT_EXIST_CODE)
	                .responseMessage(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE)
	                .accountInfo(null)
	                .build();
	    }

	    User sourceAccountUser =
	            userRepository.findByAccountNumber(request.getSourceAccountNumber());

	    if (request.getAmount().compareTo(sourceAccountUser.getAccountBalance()) > 0) {
	        return BankResponse.builder()
	                .responseCode(AccountUtils.INSUFFICIENT_BALANCE_CODE)
	                .responseMessage(AccountUtils.INSUFFICIENT_BALANCE_MESSAGE)
	                .accountInfo(null)
	                .build();
	    }

	    // debit source account
	    sourceAccountUser.setAccountBalance(
	            sourceAccountUser.getAccountBalance().subtract(request.getAmount()));
	    userRepository.save(sourceAccountUser);

	    // sender email
	    EmailDetails debitAlert = EmailDetails.builder()
	            .subject("TRANSFER DEBIT ALERT")
	            .recipient(sourceAccountUser.getEmail())
	            .messageBody(
	                    "Dear " + sourceAccountUser.getFirstName() + ",\n\n" +
	                    "₹" + request.getAmount() + " has been transferred from your account.\n\n" +
	                    "Available Balance: ₹" + sourceAccountUser.getAccountBalance() + "\n\n" +
	                    "Thank you for banking with us."
	            )
	            .build();

	    emailService.sendEmailAlert(debitAlert);

	    // credit destination account
	    User destinationAccountUser =
	            userRepository.findByAccountNumber(request.getDestinationAccountNumber());

	    destinationAccountUser.setAccountBalance(
	            destinationAccountUser.getAccountBalance().add(request.getAmount()));
	    userRepository.save(destinationAccountUser);

	    // receiver email
	    EmailDetails creditAlert = EmailDetails.builder()
	            .subject("TRANSFER CREDIT ALERT")
	            .recipient(destinationAccountUser.getEmail())   // ✅ FIXED
	            .messageBody(
	                    "Dear " + destinationAccountUser.getFirstName() + ",\n\n" +
	                    "₹" + request.getAmount() + " has been credited to your account from "
	                    + sourceAccountUser.getFirstName() + ".\n\n" +
	                    "Available Balance: ₹" + destinationAccountUser.getAccountBalance() + "\n\n" +
	                    "Thank you for banking with us."
	            )
	            .build();

	    emailService.sendEmailAlert(creditAlert);

	    // save transaction
	    TransactionDto transactionDto = TransactionDto.builder()
	            .accountNumber(destinationAccountUser.getAccountNumber())
	            .transactionType("CREDIT")
	            .amount(request.getAmount())
	            .build();

	    transactionService.saveTransaction(transactionDto);

	    return BankResponse.builder()
	            .responseCode(AccountUtils.TRANSFER_SUCCESSFUL_CODE)
	            .responseMessage(AccountUtils.TRANSFER_SUCCESSFUL_MESSAGE)
	            .accountInfo(null)
	            .build();
	}
	
	@Override
	public BankResponse login(LoginDto loginDto) {
		// TODO Auto-generated method stub
		return null;
	}
	//balance Enquiry,name enquiry , credit, debit, transfer 

	
}