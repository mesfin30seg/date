/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2021 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (Iso8601Format.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j.format.expert;

import net.time4j.ClockUnit;
import net.time4j.Moment;
import net.time4j.PlainDate;
import net.time4j.PlainTime;
import net.time4j.PlainTimestamp;
import net.time4j.Weekmodel;
import net.time4j.engine.ChronoCondition;
import net.time4j.engine.ChronoDisplay;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.format.Attributes;
import net.time4j.format.Leniency;
import net.time4j.format.NumberSystem;
import net.time4j.tz.ZonalOffset;

import java.text.ParseException;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Locale;

import static net.time4j.PlainDate.*;
import static net.time4j.PlainTime.*;


/**
 * <p>Collection of predefined format objects for ISO-8601. </p>
 *
 * <p>All formatters are strict by default. The preferred decimal separator is the comma during printing.
 * This configuration follows the official recommendation of ISO-8601. However, if the system property
 * &quot;net.time4j.format.iso.decimal.dot&quot; is set to &quot;true&quot; then the dot will be used.
 * When parsing, both comma or dot are understood. </p>
 *
 * <p>Note: Most produced formatters ignore any format attribute change. As exception case, timezone
 * attributes are recognized by {@code BASIC_DATE_TIME_OFFSET} or {@code EXTENDED_DATE_TIME_OFFSET}. </p>
 *
 * @author  Meno Hochschild
 */
/*[deutsch]
 * <p>Sammlung von vordefinierten Format-Objekten f&uuml;r ISO-8601. </p>
 *
 * <p>Alle Formatierer sind per Vorgabe strikt. Das bevorzugte Dezimaltrennzeichen ist das Komma in
 * der Textausgabe. Diese Konfiguration folgt der offiziellen Empfehlung von ISO-8601. Wenn aber die
 * System-Property &quot;net.time4j.format.iso.decimal.dot&quot; auf den Wert &quot;true&quot; gesetzt
 * ist, dann wird der Punkt als Dezimaltrennzeichen verwendet. Alle Textinterpretierer verstehen
 * sowohl das Komma wie auch den Punkt als Dezimaltrennzeichen. </p>
 *
 * <p>Hinweis: Die meisten Formatierer ignorieren Formatattribut&auml;nderungen. Allerdings k&ouml;nnen
 * die Formatierer {@code BASIC_DATE_TIME_OFFSET} und {@code EXTENDED_DATE_TIME_OFFSET} ausnahmsweise
 * zeitzonenbezogene Attribute verarbeiten. </p>
 *
 * @author  Meno Hochschild
 */
public class Iso8601Format {

    //~ Statische Felder/Initialisierungen --------------------------------

    private static final IsoDecimalStyle DEFAULT_ISO_DECIMAL_STYLE = (
        Boolean.getBoolean("net.time4j.format.iso.decimal.dot")
            ? IsoDecimalStyle.DOT
            : IsoDecimalStyle.COMMA);

    private static final NonZeroCondition NON_ZERO_SECOND = new NonZeroCondition(PlainTime.SECOND_OF_MINUTE);
    private static final NonZeroCondition NON_ZERO_FRACTION = new NonZeroCondition(PlainTime.NANO_OF_SECOND);
    private static final ChronoCondition<ChronoDisplay> SECOND_PART = NON_ZERO_SECOND.or(NON_ZERO_FRACTION);

    /**
     * <p>Defines the <i>basic</i> ISO-8601-format with year, month and day
     * of month using the pattern &quot;uuuuMMdd&quot;. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>basic</i> ISO-8601-Format mit Jahr, Monat und
     * Tag des Monats im Muster &quot;uuuuMMdd&quot;. </p>
     */
    public static final ChronoFormatter<PlainDate> BASIC_CALENDAR_DATE;

    /**
     * <p>Defines the <i>extended</i> ISO-8601-format with year, month and
     * day of month using the pattern &quot;uuuu-MM-dd&quot;. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>extended</i> ISO-8601-Format mit Jahr, Monat und
     * Tag des Monats im Muster &quot;uuuu-MM-dd&quot;. </p>
     */
    public static final ChronoFormatter<PlainDate> EXTENDED_CALENDAR_DATE;

    /**
     * <p>Defines the <i>basic</i> ISO-8601-format with year and day of year
     * using the pattern &quot;uuuuDDD&quot;. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>basic</i> ISO-8601-Format mit Jahr und
     * Tag des Jahres im Muster &quot;uuuuDDD&quot;. </p>
     */
    public static final ChronoFormatter<PlainDate> BASIC_ORDINAL_DATE;

    /**
     * <p>Defines the <i>extended</i> ISO-8601-format with year and day
     * of year using the pattern &quot;uuuu-DDD&quot;. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>extended</i> ISO-8601-Format mit Jahr und
     * Tag des Jahres im Muster &quot;uuuu-DDD&quot;. </p>
     */
    public static final ChronoFormatter<PlainDate> EXTENDED_ORDINAL_DATE;

    /**
     * <p>Defines the <i>basic</i> ISO-8601-format for a week date using
     * the pattern &quot;YYYYWwwE&quot;. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>basic</i> ISO-8601-Format f&uuml;r ein
     * Wochendatum im Muster &quot;YYYYWwwE&quot;. </p>
     */
    public static final ChronoFormatter<PlainDate> BASIC_WEEK_DATE;

    /**
     * <p>Defines the <i>extended</i> ISO-8601-format for a week date
     * using the pattern &quot;YYYY-Www-E&quot;. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>extended</i> ISO-8601-Format f&uuml;r ein
     * Wochendatum im Muster &quot;YYYY-Www-E&quot;. </p>
     */
    public static final ChronoFormatter<PlainDate> EXTENDED_WEEK_DATE;

    /**
     * <p>Similar to {@link #BASIC_CALENDAR_DATE} but its parser can also
     * understand ordinal dates or week dates. </p>
     *
     * @since   3.22/4.18
     */
    /*[deutsch]
     * <p>&Auml;hnlich wie {@link #BASIC_CALENDAR_DATE}, aber der zugeh&ouml;rige
     * Textinterpretierer kann auch ein Ordinaldatum oder Wochendatum verstehen. </p>
     *
     * @since   3.22/4.18
     */
    public static final ChronoFormatter<PlainDate> BASIC_DATE;

    /**
     * <p>Similar to {@link #EXTENDED_CALENDAR_DATE} but its parser can also
     * understand ordinal dates or week dates. </p>
     *
     * @since   3.22/4.18
     */
    /*[deutsch]
     * <p>&Auml;hnlich wie {@link #EXTENDED_CALENDAR_DATE}, aber der zugeh&ouml;rige
     * Textinterpretierer kann auch ein Ordinaldatum oder Wochendatum verstehen. </p>
     *
     * @since   3.22/4.18
     */
    public static final ChronoFormatter<PlainDate> EXTENDED_DATE;

    /**
     * <p>Defines the <i>basic</i> ISO-8601-format for a wall time with
     * hour, minute and optional second using the pattern
     * &quot;HH[mm[ss[,SSSSSSSSS]]]&quot;. </p>
     *
     * <p>The minute part is optional during parsing, but will always
     * be printed. The parser also accepts a leading char T. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>basic</i> ISO-8601-Format f&uuml;r eine
     * Uhrzeit mit Stunde, Minute und optionaler Sekunde im Muster
     * &quot;HH[mm[ss[,SSSSSSSSS]]]&quot;. Der Parser akzeptiert auch ein
     * f&uuml;hrendes T. </p>
     *
     * <p>Der Minutenteil ist beim Parsen optional, wird aber beim
     * Formatieren immer ausgegeben. </p>
     */
    public static final ChronoFormatter<PlainTime> BASIC_WALL_TIME;

    /**
     * <p>Defines the <i>extended</i> ISO-8601-format for a wall time
     * with hour, minute and optional second using the pattern
     * &quot;HH[:mm[:ss[,SSSSSSSSS]]]&quot;. </p>
     *
     * <p>The minute part is optional during parsing, but will always
     * be printed. The parser also accepts a leading char T. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>extended</i> ISO-8601-Format f&uuml;r eine
     * Uhrzeit mit Stunde, Minute und optionaler Sekunde im Muster
     * &quot;HH[:mm[:ss[,SSSSSSSSS]]]&quot;. </p>
     *
     * <p>Der Minutenteil ist beim Parsen optional, wird aber beim
     * Formatieren immer ausgegeben. Der Parser akzeptiert auch ein
     * f&uuml;hrendes T. </p>
     */
    public static final ChronoFormatter<PlainTime> EXTENDED_WALL_TIME;

    /**
     * <p>Defines the <i>basic</i> ISO-8601-format for a composition of
     * calendar date and wall time with hour and minute using the pattern
     * &quot;uuuuMMdd'T'HH[mm[ss[,SSSSSSSSS]]]&quot;. </p>
     *
     * <p>Second and nanosecond elements are optional. Furthermore,
     * the count of decimal digits is flexible (0-9). The minute part is
     * optional during parsing, but will always be printed. The parser
     * will also understand a combination of ordinal date or week date
     * together with the wall time. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>basic</i> ISO-8601-Format f&uuml;r eine Kombination
     * aus Kalenderdatum und Uhrzeit mit Stunde und Minute im Muster
     * &quot;uuuuMMdd'T'HH[mm[ss[,SSSSSSSSS]]]&quot;. </p>
     *
     * <p>Sekunde und Nanosekunde sind optional. Auch die Anzahl der
     * Dezimalstellen ist variabel (0-9). Der Minutenteil ist beim Parsen
     * optional, wird aber beim Formatieren immer ausgegeben. Der Interpretierer
     * wird auch eine Kombination aus Ordinaldatum oder Wochendatum zusammen
     * mit einer Uhrzeit verstehen. </p>
     */
    public static final ChronoFormatter<PlainTimestamp> BASIC_DATE_TIME;

    /**
     * <p>Defines the <i>extended</i> ISO-8601-format for a composition of
     * calendar date and wall time with hour and minute using the pattern
     * &quot;uuuu-MM-dd'T'HH[:mm[:ss[,SSSSSSSSS]]]&quot;. </p>
     *
     * <p>Second and nanosecond elements are optional. Furthermore,
     * the count of decimal digits is flexible (0-9). The minute part is
     * optional during parsing, but will always be printed. The parser
     * will also understand a combination of ordinal date or week date
     * together with the wall time. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>extended</i> ISO-8601-Format f&uuml;r eine
     * Kombination aus Kalenderdatum und Uhrzeit mit Stunde und Minute
     * im Muster &quot;uuuu-MM-dd'T'HH[:mm[:ss[,SSSSSSSSS]]]&quot;. </p>
     *
     * <p>Sekunde und Nanosekunde sind optional. Auch die Anzahl der
     * Dezimalstellen ist variabel (0-9). Der Minutenteil ist beim Parsen
     * optional, wird aber beim Formatieren immer ausgegeben. Der Interpretierer
     * wird auch eine Kombination aus Ordinaldatum oder Wochendatum zusammen
     * mit einer Uhrzeit verstehen. </p>
     */
    public static final ChronoFormatter<PlainTimestamp> EXTENDED_DATE_TIME;

    /**
     * <p>Defines the <i>basic</i> ISO-8601-format for a composition of
     * calendar date, wall time and timezone offset using the pattern
     * &quot;uuuuMMdd'T'HH[mm[ss[,SSSSSSSSS]]]{offset}&quot;. </p>
     *
     * <p>Second and nanosecond elements are optional. Furthermore,
     * the count of decimal digits is flexible (0-9). The minute part is
     * optional during parsing, but will always be printed. The offset
     * part is for printing equivalent to XX, for parsing equivalent to X.
     * The parser will also understand a combination of ordinal date or
     * week date together with the wall time. </p>
     *
     * <p>By default, the timezone offset used for printing is UTC+00:00.
     * Users can override this offset by calling the method
     * {@link ChronoFormatter#withTimezone(net.time4j.tz.TZID)}. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>basic</i> ISO-8601-Format f&uuml;r eine Kombination
     * aus Kalenderdatum, Uhrzeit mit Stunde und Minute und Offset im Muster
     * &quot;uuuuMMdd'T'HH[mm[ss[,SSSSSSSSS]]]{offset}&quot;. </p>
     *
     * <p>Sekunde und Nanosekunde sind optional. Auch die Anzahl der
     * Dezimalstellen ist variabel (0-9). Der Minutenteil ist beim Parsen
     * optional, wird aber beim Formatieren immer ausgegeben. Der Offset-Teil
     * ist f&uuml;r die formatierte Ausgabe &auml;quivalent zu XX, beim
     * Interpretieren &auml;quivalent zu X. Der Interpretierer wird auch
     * eine Kombination aus Ordinaldatum oder Wochendatum zusammen mit einer
     * Uhrzeit verstehen. </p>
     *
     * <p>Standardm&auml;&szlig;ig wird f&uuml;r die formatierte Ausgabe der
     * Zeitzonen-Offset UTC+00:00 verwendet. Diese Vorgabe kann mit Hilfe von
     * {@link ChronoFormatter#withTimezone(net.time4j.tz.TZID)} ge&auml;ndert
     * werden. </p>
     */
    public static final ChronoFormatter<Moment> BASIC_DATE_TIME_OFFSET;

    /**
     * <p>Defines the <i>extended</i> ISO-8601-format for a composition of
     * calendar date, wall time and timezone offset using the pattern
     * &quot;uuuu-MM-dd'T'HH[:mm[:ss[,SSSSSSSSS]]]{offset}&quot;. </p>
     *
     * <p>Second and nanosecond elements are optional. Furthermore,
     * the count of decimal digits is flexible (0-9). The minute part is
     * optional during parsing, but will always be printed. The offset
     * part is for printing equivalent to XXX, for parsing equivalent
     * to X (but with colon!). The parser will also understand a combination
     * of ordinal date or week date together with the wall time.  </p>
     *
     * <p>By default, the timezone offset used for printing is UTC+00:00.
     * Users can override this offset by calling the method
     * {@link ChronoFormatter#withTimezone(net.time4j.tz.TZID)}. </p>
     */
    /*[deutsch]
     * <p>Definiert das <i>extended</i> ISO-8601-Format f&uuml;r eine
     * Kombination aus Kalenderdatum, Uhrzeit mit Stunde und Minute und Offset
     * im Muster &quot;uuuu-MM-dd'T'HH[:mm[:ss[,SSSSSSSSS]]]{offset}&quot;. </p>
     *
     * <p>Sekunde und Nanosekunde sind optional. Auch die Anzahl der
     * Dezimalstellen ist variabel (0-9). Der Minutenteil ist beim Parsen
     * optional, wird aber beim Formatieren immer ausgegeben.  Der Offset-Teil
     * ist f&uuml;r die formatierte Ausgabe &auml;quivalent zu XXX, beim
     * Interpretieren &auml;quivalent zu X (aber mit Doppelpunkt!). Der
     * Interpretierer wird auch eine Kombination aus Ordinaldatum oder
     * Wochendatum zusammen mit einer Uhrzeit verstehen. </p>
     *
     * <p>Standardm&auml;&szlig;ig wird f&uuml;r die formatierte Ausgabe der
     * Zeitzonen-Offset UTC+00:00 verwendet. Diese Vorgabe kann mit Hilfe von
     * {@link ChronoFormatter#withTimezone(net.time4j.tz.TZID)} ge&auml;ndert
     * werden. </p>
     */
    public static final ChronoFormatter<Moment> EXTENDED_DATE_TIME_OFFSET;

    static {
        BASIC_CALENDAR_DATE = calendarFormat(false);
        EXTENDED_CALENDAR_DATE = calendarFormat(true);
        BASIC_ORDINAL_DATE = ordinalFormat(false);
        EXTENDED_ORDINAL_DATE = ordinalFormat(true);
        BASIC_WEEK_DATE = weekdateFormat(false);
        EXTENDED_WEEK_DATE = weekdateFormat(true);

        BASIC_DATE = generalDateFormat(false);
        EXTENDED_DATE = generalDateFormat(true);

        BASIC_WALL_TIME = timeFormat(false, DEFAULT_ISO_DECIMAL_STYLE);
        EXTENDED_WALL_TIME = timeFormat(true, DEFAULT_ISO_DECIMAL_STYLE);

        BASIC_DATE_TIME = timestampFormat(false, DEFAULT_ISO_DECIMAL_STYLE);
        EXTENDED_DATE_TIME = timestampFormat(true, DEFAULT_ISO_DECIMAL_STYLE);

        BASIC_DATE_TIME_OFFSET = momentFormat(false, DEFAULT_ISO_DECIMAL_STYLE);
        EXTENDED_DATE_TIME_OFFSET = momentFormat(true, DEFAULT_ISO_DECIMAL_STYLE);
    }

    //~ Konstruktoren -----------------------------------------------------

    private Iso8601Format() {
        // no instantiation
    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Obtains a printer with given styles for printing a calendar date. </p>
     *
     * @param   style       iso-compatible date style
     * @return  ChronoPrinter
     * @since   4.18
     */
    /*[deutsch]
     * <p>Liefert einen {@code ChronoPrinter} mit den angegebenen Stilen zur Ausgabe eines Kalenderdatums. </p>
     *
     * @param   style       iso-compatible date style
     * @return  ChronoPrinter
     * @since   4.18
     */
    public static ChronoPrinter<PlainDate> ofDate(IsoDateStyle style) {

        switch (style) {
            case BASIC_CALENDAR_DATE:
                return Iso8601Format.BASIC_CALENDAR_DATE;
            case BASIC_ORDINAL_DATE:
                return Iso8601Format.BASIC_ORDINAL_DATE;
            case BASIC_WEEK_DATE:
                return Iso8601Format.BASIC_WEEK_DATE;
            case EXTENDED_CALENDAR_DATE:
                return Iso8601Format.EXTENDED_CALENDAR_DATE;
            case EXTENDED_ORDINAL_DATE:
                return Iso8601Format.EXTENDED_ORDINAL_DATE;
            case EXTENDED_WEEK_DATE:
                return Iso8601Format.EXTENDED_WEEK_DATE;
            default:
                throw new UnsupportedOperationException(style.name());
        }

    }

    /**
     * <p>Obtains a printer with given decimal style for printing a clock time
     * in basic format &quot;HHmm[ss[,SSSSSSSSS]]&quot;. </p>
     *
     * <p>It is recommended to store the result in a static final constant for achieving best performance. </p>
     *
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    /*[deutsch]
     * <p>Liefert einen {@code ChronoPrinter} mit dem angegebenen Dezimalstil zur Ausgabe einer Uhrzeit
     * im <i>basic</i>-Format &quot;HHmm[ss[,SSSSSSSSS]]&quot;. </p>
     *
     * <p>Im Interesse einer maximalen Performance wird empfohlen, das Ergebnis dieser Methode in einer
     * statischen Konstanten zu speichern. </p>
     *
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    public static ChronoPrinter<PlainTime> ofBasicTime(
        IsoDecimalStyle decimalStyle,
        ClockUnit precision
    ) {

        ChronoFormatter.Builder<PlainTime> builder =
            ChronoFormatter.setUp(PlainTime.class, Locale.ROOT);
        addWallTime(builder, false, decimalStyle, precision);
        return builder.build().with(Leniency.STRICT);

    }

    /**
     * <p>Obtains a printer with given decimal style for printing a clock time
     * in extended format &quot;HH:mm[:ss[,SSSSSSSSS]]&quot;. </p>
     *
     * <p>It is recommended to store the result in a static final constant for achieving best performance. </p>
     *
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    /*[deutsch]
     * <p>Liefert einen {@code ChronoPrinter} mit dem angegebenen Dezimalstil zur Ausgabe einer Uhrzeit
     * im <i>extended</i>-Format &quot;HH:mm[:ss[,SSSSSSSSS]]&quot;. </p>
     *
     * <p>Im Interesse einer maximalen Performance wird empfohlen, das Ergebnis dieser Methode in einer
     * statischen Konstanten zu speichern. </p>
     *
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    public static ChronoPrinter<PlainTime> ofExtendedTime(
        IsoDecimalStyle decimalStyle,
        ClockUnit precision
    ) {

        ChronoFormatter.Builder<PlainTime> builder =
            ChronoFormatter.setUp(PlainTime.class, Locale.ROOT);
        addWallTime(builder, true, decimalStyle, precision);
        return builder.build().with(Leniency.STRICT);

    }

    /**
     * <p>Obtains a printer with given styles for printing a timestamp. </p>
     *
     * <p>It is recommended to store the result in a static final constant for achieving best performance. </p>
     *
     * @param   dateStyle       iso-compatible date style
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    /*[deutsch]
     * <p>Liefert einen {@code ChronoPrinter} mit den angegebenen Stilen zur Ausgabe eines Zeitstempels. </p>
     *
     * <p>Im Interesse einer maximalen Performance wird empfohlen, das Ergebnis dieser Methode in einer
     * statischen Konstanten zu speichern. </p>
     *
     * @param   dateStyle       iso-compatible date style
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    public static ChronoPrinter<PlainTimestamp> ofTimestamp(
        IsoDateStyle dateStyle,
        IsoDecimalStyle decimalStyle,
        ClockUnit precision
    ) {

        ChronoFormatter.Builder<PlainTimestamp> builder =
            ChronoFormatter.setUp(PlainTimestamp.axis(), Locale.ROOT);
        builder.addCustomized(
            PlainDate.COMPONENT,
            Iso8601Format.ofDate(dateStyle),
            (text, status, attributes) -> null);
        builder.addLiteral('T');
        addWallTime(builder, dateStyle.isExtended(), decimalStyle, precision);
        return builder.build().with(Leniency.STRICT);

    }

    /**
     * <p>Obtains a printer with given styles for printing a moment. </p>
     *
     * <p>It is recommended to store the result in a static final constant for achieving best performance. </p>
     *
     * @param   dateStyle       iso-compatible date style
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @param   offset          time zone offset
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    /*[deutsch]
     * <p>Liefert einen {@code ChronoPrinter} mit den angegebenen Stilen zur Ausgabe eines Moments. </p>
     *
     * <p>Im Interesse einer maximalen Performance wird empfohlen, das Ergebnis dieser Methode in einer
     * statischen Konstanten zu speichern. </p>
     *
     * @param   dateStyle       iso-compatible date style
     * @param   decimalStyle    iso-compatible decimal style
     * @param   precision       controls the precision of output format with constant length
     * @param   offset          time zone offset
     * @return  ChronoPrinter as new instance
     * @since   4.18
     */
    public static ChronoPrinter<Moment> ofMoment(
        IsoDateStyle dateStyle,
        IsoDecimalStyle decimalStyle,
        ClockUnit precision,
        ZonalOffset offset
    ) {

        ChronoFormatter.Builder<Moment> builder =
            ChronoFormatter.setUp(Moment.axis(), Locale.ROOT);
        builder.addCustomized(
            PlainDate.COMPONENT,
            Iso8601Format.ofDate(dateStyle),
            (text, status, attributes) -> null);
        builder.addLiteral('T');
        addWallTime(builder, dateStyle.isExtended(), decimalStyle, precision);
        builder.addTimezoneOffset(FormatStyle.MEDIUM, dateStyle.isExtended(), Collections.singletonList("Z"));
        return builder.build().with(Leniency.STRICT).withTimezone(offset);

    }

    /**
     * <p>Parses given ISO-8601-compatible date string in basic or extended format. </p>
     *
     * @param   iso     text like &quot;20160101&quot;, &quot;2016001&quot;, &quot;2016W011&quot;,
     *                  &quot;2016-01-01&quot;, &quot;2016-001&quot; or &quot;2016-W01-1&quot;
     * @return  PlainDate
     * @throws  ParseException if parsing fails for any reason
     * @since   3.22/4.18
     */
    /*[deutsch]
     * <p>Interpretiert den angegebenen ISO-8601-kompatiblen Datumstext im <i>basic</i>-Format
     * oder im <i>extended</i>-Format. </p>
     *
     * @param   iso     text like &quot;20160101&quot;, &quot;2016001&quot;, &quot;2016W011&quot;,
     *                  &quot;2016-01-01&quot;, &quot;2016-001&quot; or &quot;2016-W01-1&quot;
     * @return  PlainDate
     * @throws  ParseException if parsing fails for any reason
     * @since   3.22/4.18
     */
    public static PlainDate parseDate(CharSequence iso) throws ParseException {

        ParseLog plog = new ParseLog();
        PlainDate date = parseDate(iso, plog);

        if ((date == null) || plog.isError()) {
            throw new ParseException(plog.getErrorMessage(), plog.getErrorIndex());
        } else if (plog.getPosition() < iso.length()) {
            throw new ParseException("Trailing characters found: " + iso, plog.getPosition());
        } else {
            return date;
        }

    }

    /**
     * <p>Parses given ISO-8601-compatible date string in basic or extended format. </p>
     *
     * @param   iso     text like &quot;20160101&quot;, &quot;2016001&quot;, &quot;2016W011&quot;,
     *                  &quot;2016-01-01&quot;, &quot;2016-001&quot; or &quot;2016-W01-1&quot;
     * @param   plog    new mutable instance of {@code ParseLog}
     * @return  PlainDate or {@code null} in case of error
     * @throws  IndexOutOfBoundsException if the start position is at end of text or even behind
     * @see     ParseLog#isError()
     * @since   3.22/4.18
     */
    /*[deutsch]
     * <p>Interpretiert den angegebenen ISO-8601-kompatiblen Datumstext im <i>basic</i>-Format
     * oder im <i>extended</i>-Format. </p>
     *
     * @param   iso     text like &quot;20160101&quot;, &quot;2016001&quot;, &quot;2016W011&quot;,
     *                  &quot;2016-01-01&quot;, &quot;2016-001&quot; or &quot;2016-W01-1&quot;
     * @param   plog    new mutable instance of {@code ParseLog}
     * @return  PlainDate or {@code null} in case of error
     * @throws  IndexOutOfBoundsException if the start position is at end of text or even behind
     * @see     ParseLog#isError()
     * @since   3.22/4.18
     */
    public static PlainDate parseDate(
        CharSequence iso,
        ParseLog plog
    ) {
        int hyphens = 0;
        int n = iso.length();
        int start = plog.getPosition();
        int len = n - start;

        if (len < 7) {
            plog.setError(n, "Too short to be compatible with ISO-8601: " + iso.subSequence(start, n));
            return null;
        }

LOOP:
        for (int i = start + 1; i < n; i++) {
            switch (iso.charAt(i)) {
                case '-': // leading sign is ignored, see initial loop index
                    hyphens++;
                    break;
                case 'W':
                    return ((hyphens > 0) ? EXTENDED_WEEK_DATE.parse(iso, plog) : BASIC_WEEK_DATE.parse(iso, plog));
                case 'T':
                case '/':
                    len = i - start;
                    break LOOP;
                default:
                    // continue
            }
        }

        if (hyphens == 0) {
            len -= 4; // year length is usually 4 digits
            char c = iso.charAt(start);
            if ((c == '+') || (c == '-')) {
                len -= 2; // concerns years with at least 5 digits or more
            }
            return ((len == 3) ? BASIC_ORDINAL_DATE.parse(iso, plog) : BASIC_CALENDAR_DATE.parse(iso, plog));
        } else if (hyphens == 1) {
            return EXTENDED_ORDINAL_DATE.parse(iso, plog);
        } else {
            return EXTENDED_CALENDAR_DATE.parse(iso, plog);
        }

    }

    private static ChronoFormatter<PlainDate> calendarFormat(boolean extended) {

        ChronoFormatter.Builder<PlainDate> builder =
            ChronoFormatter
                .setUp(PlainDate.class, Locale.ROOT)
                .startSection(Attributes.NUMBER_SYSTEM, NumberSystem.ARABIC)
                .startSection(Attributes.ZERO_DIGIT, '0')
                .addInteger(YEAR, 4, 9, SignPolicy.SHOW_WHEN_BIG_NUMBER);

        if (extended) {
            builder.addLiteral('-');
        }

        builder.addFixedInteger(MONTH_AS_NUMBER, 2);

        if (extended) {
            builder.addLiteral('-');
        }

        return builder.addFixedInteger(DAY_OF_MONTH, 2).endSection().endSection().build().with(Leniency.STRICT);

    }

    private static ChronoFormatter<PlainDate> ordinalFormat(boolean extended) {

        ChronoFormatter.Builder<PlainDate> builder =
            ChronoFormatter
                .setUp(PlainDate.class, Locale.ROOT)
                .startSection(Attributes.NUMBER_SYSTEM, NumberSystem.ARABIC)
                .startSection(Attributes.ZERO_DIGIT, '0')
                .addInteger(YEAR, 4, 9, SignPolicy.SHOW_WHEN_BIG_NUMBER);

        if (extended) {
            builder.addLiteral('-');
        }

        return builder.addFixedInteger(DAY_OF_YEAR, 3).endSection().endSection().build().with(Leniency.STRICT);

    }

    private static ChronoFormatter<PlainDate> weekdateFormat(boolean extended) {

        ChronoFormatter.Builder<PlainDate> builder =
            ChronoFormatter
                .setUp(PlainDate.class, Locale.ROOT)
                .startSection(Attributes.NUMBER_SYSTEM, NumberSystem.ARABIC)
                .startSection(Attributes.ZERO_DIGIT, '0')
                .addInteger(YEAR_OF_WEEKDATE, 4, 9, SignPolicy.SHOW_WHEN_BIG_NUMBER);

        if (extended) {
            builder.addLiteral('-');
        }

        builder.addLiteral('W');
        builder.addFixedInteger(Weekmodel.ISO.weekOfYear(), 2);

        if (extended) {
            builder.addLiteral('-');
        }

        return builder.addFixedNumerical(DAY_OF_WEEK, 1).endSection().endSection().build().with(Leniency.STRICT);

    }

    private static ChronoFormatter<PlainDate> generalDateFormat(boolean extended) {

        ChronoFormatter.Builder<PlainDate> builder =
            ChronoFormatter.setUp(PlainDate.class, Locale.ROOT);
        builder.addCustomized(
            PlainDate.COMPONENT,
            generalDatePrinter(extended),
            generalDateParser(extended));
        return builder.build().with(Leniency.STRICT);

    }

    private static ChronoFormatter<PlainTime> timeFormat(
        boolean extended,
        IsoDecimalStyle decimalStyle
    ) {

        ChronoFormatter.Builder<PlainTime> builder =
            ChronoFormatter.setUp(PlainTime.class, Locale.ROOT);
        builder.skipUnknown(c -> (c == 'T'), 1);
        addWallTime(builder, extended, decimalStyle);
        return builder.build().with(Leniency.STRICT);

    }

    private static ChronoFormatter<PlainTimestamp> timestampFormat(
        boolean extended,
        IsoDecimalStyle decimalStyle
    ) {

        ChronoFormatter.Builder<PlainTimestamp> builder =
            ChronoFormatter.setUp(PlainTimestamp.class, Locale.ROOT);
        builder.addCustomized(
            PlainDate.COMPONENT,
            generalDatePrinter(extended),
            generalDateParser(extended));
        builder.addLiteral('T');
        addWallTime(builder, extended, decimalStyle);
        return builder.build().with(Leniency.STRICT);

    }

    private static ChronoFormatter<Moment> momentFormat(
        boolean extended,
        IsoDecimalStyle decimalStyle
    ) {

        ChronoFormatter.Builder<Moment> builder =
            ChronoFormatter.setUp(Moment.class, Locale.ROOT);

        builder.addCustomized(
            Moment.axis().element(),
            momentFormat(FormatStyle.MEDIUM, extended, decimalStyle),
            momentFormat(FormatStyle.SHORT, extended, decimalStyle)
        );

        // here timezone offset is needed for changing Moment to ZonalDateTime when printing
        return builder.build().with(Leniency.STRICT).withTimezone(ZonalOffset.UTC);

    }

    private static ChronoFormatter<Moment> momentFormat(
        FormatStyle style,
        boolean extended,
        IsoDecimalStyle decimalStyle

    ) {

        ChronoFormatter.Builder<Moment> builder =
            ChronoFormatter.setUp(Moment.class, Locale.ROOT);

        builder.addCustomized(
            PlainDate.COMPONENT,
            generalDatePrinter(extended),
            generalDateParser(extended));
        builder.addLiteral('T');
        addWallTime(builder, extended, decimalStyle);

        // not optional, offset must be present during parsing
        builder.addTimezoneOffset(
            style,
            extended,
            Collections.singletonList("Z"));

        return builder.build().with(Leniency.STRICT);

    }

    private static ChronoPrinter<PlainDate> generalDatePrinter(final boolean extended) {

        return (formattable, buffer, attributes) -> {
            ChronoFormatter<PlainDate> f = (extended ? EXTENDED_CALENDAR_DATE : BASIC_CALENDAR_DATE);
            return f.print(formattable, buffer); // attributes are ignored to ensure quick path evaluation
        };

    }

    private static ChronoParser<PlainDate> generalDateParser(final boolean extended) {

        return (text, status, attributes) -> {
            int hyphens = 0;
            int n = text.length();
            int start = status.getPosition();
            int len = n - start;

LOOP:
            for (int i = start + 1; i < n; i++) {
                switch (text.charAt(i)) {
                    case '-': // leading sign is ignored, see initial loop index
                        hyphens++;
                        break;
                    case 'W':
                        if (extended) {
                            return EXTENDED_WEEK_DATE.parse(text, status);
                        } else {
                            return BASIC_WEEK_DATE.parse(text, status);
                        }
                    case 'T':
                    case '/':
                        len = i - start;
                        break LOOP;
                    default:
                        // continue
                }
            }

            if (extended) {
                if (hyphens == 1) {
                    return EXTENDED_ORDINAL_DATE.parse(text, status);
                } else {
                    return EXTENDED_CALENDAR_DATE.parse(text, status);
                }
            } else {
                len -= 4; // year length is usually 4 digits
                char c = text.charAt(start);
                if ((c == '+') || (c == '-')) {
                    len -= 2; // concerns years with at least 5 digits or more
                }
                if (len == 3) {
                    return BASIC_ORDINAL_DATE.parse(text, status);
                } else {
                    return BASIC_CALENDAR_DATE.parse(text, status);
                }
            }
        };

    }

    private static <T extends ChronoEntity<T>> void addWallTime(
        ChronoFormatter.Builder<T> builder,
        boolean extended,
        IsoDecimalStyle decimalStyle
    ) {

        builder.startSection(Attributes.NUMBER_SYSTEM, NumberSystem.ARABIC);
        builder.startSection(Attributes.ZERO_DIGIT, '0');
        builder.addFixedInteger(HOUR_FROM_0_TO_24, 2);
        builder.startOptionalSection();

        if (extended) {
            builder.addLiteral(':');
        }

        builder.addFixedInteger(MINUTE_OF_HOUR, 2);
        builder.startOptionalSection(SECOND_PART);

        if (extended) {
            builder.addLiteral(':');
        }

        builder.addFixedInteger(SECOND_OF_MINUTE, 2);
        builder.startOptionalSection(NON_ZERO_FRACTION);

        switch (decimalStyle) {
            case COMMA:
                builder.addLiteral(',', '.');
                break;
            case DOT:
                builder.addLiteral('.', ',');
                break;
            default:
                throw new UnsupportedOperationException(decimalStyle.name());
        }

        builder.addFraction(NANO_OF_SECOND, 1, 9, false);

        for (int i = 0; i < 5; i++) {
            builder.endSection();
        }

    }

    private static <T extends ChronoEntity<T>> void addWallTime(
        ChronoFormatter.Builder<T> builder,
        boolean extended,
        IsoDecimalStyle decimalStyle,
        ClockUnit precision
    ) {

        if (precision == null) {
            throw new NullPointerException("Missing precision.");
        }

        builder.startSection(Attributes.NUMBER_SYSTEM, NumberSystem.ARABIC);
        builder.startSection(Attributes.ZERO_DIGIT, '0');
        builder.addFixedInteger(HOUR_FROM_0_TO_24, 2);

        if (precision == ClockUnit.HOURS) {
            builder.endSection();
            builder.endSection();
            return;
        }

        if (extended) {
            builder.addLiteral(':');
        }

        builder.addFixedInteger(MINUTE_OF_HOUR, 2);

        if (precision == ClockUnit.MINUTES) {
            builder.endSection();
            builder.endSection();
            return;
        }

        if (extended) {
            builder.addLiteral(':');
        }

        builder.addFixedInteger(SECOND_OF_MINUTE, 2);

        if (precision == ClockUnit.SECONDS) {
            builder.endSection();
            builder.endSection();
            return;
        }

        switch (decimalStyle) {
            case COMMA:
                builder.addLiteral(',', '.');
                break;
            case DOT:
                builder.addLiteral('.', ',');
                break;
            default:
                throw new UnsupportedOperationException(decimalStyle.name());
        }

        if (precision == ClockUnit.MILLIS) {
            builder.addFraction(NANO_OF_SECOND, 3, 3, false);
        } else if (precision == ClockUnit.MICROS) {
            builder.addFraction(NANO_OF_SECOND, 6, 6, false);
        } else if (precision == ClockUnit.NANOS) {
            builder.addFraction(NANO_OF_SECOND, 9, 9, false);
        }

        builder.endSection();
        builder.endSection();

    }

    //~ Innere Klassen ----------------------------------------------------

    private static class NonZeroCondition
        implements ChronoCondition<ChronoDisplay> {

        //~ Instanzvariablen ----------------------------------------------

        private final ChronoElement<Integer> element;

        //~ Konstruktoren -------------------------------------------------

        NonZeroCondition(ChronoElement<Integer> element) {
            super();

            this.element = element;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public boolean test(ChronoDisplay context) {

            return (context.getInt(this.element) > 0);

        }

        ChronoCondition<ChronoDisplay> or(final NonZeroCondition other) {

            return context -> (NonZeroCondition.this.test(context) || other.test(context));

        }

    }

}
