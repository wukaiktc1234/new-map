package com.foodtraceability.service;

import com.foodtraceability.dto.MenuResponse;

public interface MenuService {

    MenuResponse getMenus(String userId);
}
