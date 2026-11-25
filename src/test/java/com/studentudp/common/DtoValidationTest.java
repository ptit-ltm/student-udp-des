package com.studentudp.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DtoValidationTest {

    @Test
    void studentDtoAcceptsBoundaryScoresZeroAndTen() {
        assertDoesNotThrow(() -> new StudentDTO("SV01", "Lê Thị Bình", 0.0, 10.0, 5.5).validate());
    }

    @Test
    void studentDtoRejectsOutOfRangeScoresAndBlankFields() {
        assertThrows(IllegalArgumentException.class, () -> new StudentDTO("", "An", 5, 5, 5).validate());
        assertThrows(IllegalArgumentException.class, () -> new StudentDTO("SV01", "  ", 5, 5, 5).validate());
        assertThrows(IllegalArgumentException.class, () -> new StudentDTO("SV01", "An", -0.1, 5, 5).validate());
        assertThrows(IllegalArgumentException.class, () -> new StudentDTO("SV01", "An", 5, 10.1, 5).validate());
        assertThrows(IllegalArgumentException.class, () -> new StudentDTO("SV01", "An", 5, 5, Double.NaN).validate());
    }
}
