package com.foodtraceability.service.impl;

import com.foodtraceability.service.SensitiveDataService;
import com.foodtraceability.utils.AESUtil;
import org.springframework.stereotype.Service;

@Service
public class SensitiveDataServiceImpl implements SensitiveDataService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SensitiveDataServiceImpl.class);
    private final AESUtil aesUtil;
    private static final String PHONE_PREFIX = "ENC_PHONE:";
    private static final String ADDRESS_PREFIX = "ENC_ADDR:";

    @Override
    public String encryptPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        if (phone.startsWith(PHONE_PREFIX)) {
            return phone;
        }
        try {
            String encrypted = aesUtil.encrypt(phone);
            return PHONE_PREFIX + encrypted;
        } catch (Exception e) {
            log.error("手机号加密失败: {}", e.getMessage());
            return phone;
        }
    }

    @Override
    public String decryptPhone(String encryptedPhone) {
        if (encryptedPhone == null || encryptedPhone.isEmpty()) {
            return encryptedPhone;
        }
        if (!encryptedPhone.startsWith(PHONE_PREFIX)) {
            return encryptedPhone;
        }
        try {
            String encrypted = encryptedPhone.substring(PHONE_PREFIX.length());
            return aesUtil.decrypt(encrypted);
        } catch (Exception e) {
            log.error("手机号解密失败: {}", e.getMessage());
            return encryptedPhone;
        }
    }

    @Override
    public String encryptAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        if (address.startsWith(ADDRESS_PREFIX)) {
            return address;
        }
        try {
            String encrypted = aesUtil.encrypt(address);
            return ADDRESS_PREFIX + encrypted;
        } catch (Exception e) {
            log.error("地址加密失败: {}", e.getMessage());
            return address;
        }
    }

    @Override
    public String decryptAddress(String encryptedAddress) {
        if (encryptedAddress == null || encryptedAddress.isEmpty()) {
            return encryptedAddress;
        }
        if (!encryptedAddress.startsWith(ADDRESS_PREFIX)) {
            return encryptedAddress;
        }
        try {
            String encrypted = encryptedAddress.substring(ADDRESS_PREFIX.length());
            return aesUtil.decrypt(encrypted);
        } catch (Exception e) {
            log.error("地址解密失败: {}", e.getMessage());
            return encryptedAddress;
        }
    }

    @Override
    public String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        String decrypted = decryptPhone(phone);
        if (decrypted == null || decrypted.length() < 7) {
            return decrypted;
        }
        return decrypted.substring(0, 3) + "****" + decrypted.substring(decrypted.length() - 4);
    }

    public SensitiveDataServiceImpl(final AESUtil aesUtil) {
        this.aesUtil = aesUtil;
    }
}
