package reposense.util;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeUtilTest {
    @Test
    public void extractDate_validDate_success() {
        String expectedDate = "20/05/2019";
        String actualDate = TimeUtil.extractDate(expectedDate);
        Assertions.assertEquals(expectedDate, actualDate);
    }

    @Test
    public void extractDate_validDateAndTime_success() {
        String originalDateAndTime = "20/05/2020 12:34:56";
        String expectedDate = "20/05/2020";
        String actualDate = TimeUtil.extractDate(originalDateAndTime);
        Assertions.assertEquals(expectedDate, actualDate);
    }

    @Test
    public void extractDate_validSingleDigitDate_success() {
        String expectedDate = "1/05/2020";
        String actualDate = TimeUtil.extractDate(expectedDate);
        Assertions.assertEquals(expectedDate, actualDate);

        expectedDate = "01/5/2020";
        actualDate = TimeUtil.extractDate(expectedDate);
        Assertions.assertEquals(expectedDate, actualDate);

        expectedDate = "1/5/2020";
        actualDate = TimeUtil.extractDate(expectedDate);
        Assertions.assertEquals(expectedDate, actualDate);
    }

    @Test
    public void extractDate_validDateWithDifferentDelimiters_success() {
        String originalDate = "01-01-2020";
        String expectedDate = "01/01/2020";
        String actualDate = TimeUtil.extractDate(originalDate);
        Assertions.assertEquals(expectedDate, actualDate);

        originalDate = "01.01.2020";
        actualDate = TimeUtil.extractDate(originalDate);
        Assertions.assertEquals(expectedDate, actualDate);

        // Mix and match actually also works, but I doubt anyone will use it
        originalDate = "01/01.2020";
        actualDate = TimeUtil.extractDate(originalDate);
        Assertions.assertEquals(expectedDate, actualDate);

        originalDate = "01.01-2020";
        actualDate = TimeUtil.extractDate(originalDate);
        Assertions.assertEquals(expectedDate, actualDate);

        originalDate = "01-01/2020";
        actualDate = TimeUtil.extractDate(originalDate);
        Assertions.assertEquals(expectedDate, actualDate);
    }

    @Test
    public void extractDate_invalidDate_throwsParseException() {
        // Extra digits
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("001/02/2020"));
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("01/002/2020"));
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("001/002/2020"));

        // Use delimiter other than '/', '.', and '-'
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("01?02/2020"));
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("01/02 2020"));
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("01@02/2020"));
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("01/02+2020"));
    }

    @Test
    public void parseDate_validDateAndTime_success() throws Exception {
        String originalDateAndTime = "20/05/2020 00:00:00";
        LocalDateTime expectedDate = TestUtil.getSinceDate(2020, Month.MAY.getValue(), 20);
        LocalDateTime actualDate = TimeUtil.parseDate(originalDateAndTime);
        Assertions.assertEquals(expectedDate, actualDate);
    }

    @Test
    public void parseDate_invalidDate_throwsParseException() {
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("31/02/2020 00:00:00"));
    }

    @Test
    public void parseDate_invalidTime_throwsParseException() {
        Assertions.assertThrows(ParseException.class, () -> TimeUtil.parseDate("20/05/2020 23:69:70"));
    }
}
