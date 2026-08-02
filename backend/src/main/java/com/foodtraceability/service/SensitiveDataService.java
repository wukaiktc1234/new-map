package com.foodtraceability.service;

public interface SensitiveDataService {
    
    String encryptPhone(String phone);
    
    String decryptPhone(String encryptedPhone);
    
    String encryptAddress(String address);
    
    String decryptAddress(String encryptedAddress);
    
    String maskPhone(String phone);
}
