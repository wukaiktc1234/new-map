package com.foodtraceability.utils;

import com.foodtraceability.mapper.FoodMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 菜品编码生成器测试类
 */
@ExtendWith(MockitoExtension.class)
class FoodCodeGeneratorTest {

    @Mock
    private FoodMapper foodMapper;

    @InjectMocks
    private FoodCodeGenerator foodCodeGenerator;

    @Test
    void testGenerateNextCode_WhenMaxCodeIsNull() {
        // Given
        when(foodMapper.selectMaxFoodCode()).thenReturn(null);

        // When
        String result = foodCodeGenerator.generateNextCode();

        // Then
        assertEquals("D00001", result);
        verify(foodMapper, times(1)).selectMaxFoodCode();
    }

    @Test
    void testGenerateNextCode_WhenMaxCodeIsValid() {
        // Given
        when(foodMapper.selectMaxFoodCode()).thenReturn("D00005");

        // When
        String result = foodCodeGenerator.generateNextCode();

        // Then
        assertEquals("D00006", result);
        verify(foodMapper, times(1)).selectMaxFoodCode();
    }

    @Test
    void testGenerateNextCode_WhenMaxCodeIsAtMaxValue() {
        // Given
        when(foodMapper.selectMaxFoodCode()).thenReturn("D99999");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            foodCodeGenerator.generateNextCode();
        });
        verify(foodMapper, times(1)).selectMaxFoodCode();
    }

    @Test
    void testIsValidCode_WithValidCode() {
        // Given
        String validCode = "D00001";

        // When
        boolean result = foodCodeGenerator.isValidCode(validCode);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsValidCode_WithInvalidCode() {
        // Given
        String invalidCode = "X00001";

        // When
        boolean result = foodCodeGenerator.isValidCode(invalidCode);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidCode_WithNullCode() {
        // When
        boolean result = foodCodeGenerator.isValidCode(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidCode_WithEmptyCode() {
        // When
        boolean result = foodCodeGenerator.isValidCode("");

        // Then
        assertFalse(result);
    }
}