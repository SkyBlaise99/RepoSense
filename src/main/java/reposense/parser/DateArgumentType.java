package reposense.parser;

import java.time.LocalDateTime;
import java.util.Optional;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.ArgumentType;
import reposense.util.TimeUtil;

/**
 * Verifies and parses a string-formatted date to a {@link LocalDateTime} object.
 */
public class DateArgumentType implements ArgumentType<Optional<LocalDateTime>> {
    protected static final String EXTRACT_EXCEPTION_MESSAGE_INVALID_DATE_FORMAT = "Invalid Date: %s\n"
            + "Year must be within 1900 - 2999.\n"
            + "Accepted formats: [d]d/[M]M/yyyy. For example: 01/01/2022 or 1/1/2022.\n"
            + "Accepted delimiters: '/', '-', '.'. For example: 1/1/2022 or 1-1-2022.";
    private static final String PARSE_EXCEPTION_MESSAGE_INVALID_DATE_STRING_FORMAT = "Invalid Date: %s";

    @Override
    public Optional<LocalDateTime> convert(ArgumentParser parser, Argument arg, String value)
            throws ArgumentParserException {
        try {
            return Optional.of(TimeUtil.parseDate(value));
        } catch (java.text.ParseException pe) {
            throw new ArgumentParserException(
                    String.format(PARSE_EXCEPTION_MESSAGE_INVALID_DATE_STRING_FORMAT, value), parser);
        }
    }
}
