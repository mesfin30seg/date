/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2021 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (PlainDate.java) is part of project Time4J.
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

package net.time4j;

import net.time4j.base.GregorianDate;
import net.time4j.base.GregorianMath;
import net.time4j.base.MathUtils;
import net.time4j.base.ResourceLoader;
import net.time4j.base.TimeSource;
import net.time4j.base.UnixTime;
import net.time4j.engine.AttributeQuery;
import net.time4j.engine.BridgeChronology;
import net.time4j.engine.CalendarDate;
import net.time4j.engine.CalendarEra;
import net.time4j.engine.CalendarSystem;
import net.time4j.engine.Calendrical;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.engine.ChronoException;
import net.time4j.engine.ChronoExtension;
import net.time4j.engine.ChronoMerger;
import net.time4j.engine.Chronology;
import net.time4j.engine.ElementRule;
import net.time4j.engine.EpochDays;
import net.time4j.engine.FormattableElement;
import net.time4j.engine.IntElementRule;
import net.time4j.engine.Normalizer;
import net.time4j.engine.ThreetenAdapter;
import net.time4j.engine.TimeAxis;
import net.time4j.engine.TimeSpan;
import net.time4j.engine.ValidationElement;
import net.time4j.format.Attributes;
import net.time4j.format.CalendarText;
import net.time4j.format.CalendarType;
import net.time4j.format.Leniency;
import net.time4j.format.LocalizedPatternSupport;
import net.time4j.format.TemporalFormatter;
import net.time4j.scale.TimeScale;
import net.time4j.tz.TZID;
import net.time4j.tz.Timezone;
import net.time4j.tz.TransitionHistory;
import net.time4j.tz.ZonalOffset;
import net.time4j.tz.ZonalTransition;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * <p>Represents a plain calendar date in conformance to ISO-8601-standard using the gregorian calendar rules
 * for all times. </p>
 *
 * <p>The value range also contains negative years down to {@code -999999999}.
 * These years cannot be directly interpreted in a historic way, as in general no past
 * year, too. Instead such related dates can and must rather be interpreted
 * as a different way of counting days - like epoch days. The rules of
 * gregorian calendar are applied in a proleptic way that is backwards into
 * the past even before the earliest introduction of gregorian calendar in
 * Rome (a non-historic mathematical abstraction). </p>
 *
 * <p>However, this class enables <strong>full historization via a historic extension mechanism</strong>
 * (i18n-module). When formatting or parsing based on the expert engine of Time4J, it is sufficient to
 * specify a historic era (pattern symbol G) using the history of calendar reforms in a locale, and Time4J
 * will automatically apply different rules of calendar history. It is also possible to explicitly model
 * a suitable history by creating an instance of {@code net.time4j.history.ChronoHistory} and to feed the
 * format engine with this information. Furthermore, this history object yields specialized elements for
 * querying and manipulating an ISO-8601 calendar date in a historic context. </p>
 *
 * <p>Following elements which are declared as constants are registered by
 * this class: </p>
 *
 * <ul>
 *  <li>{@link #COMPONENT}</li>
 *  <li>{@link #DAY_OF_MONTH}</li>
 *  <li>{@link #DAY_OF_QUARTER}</li>
 *  <li>{@link #DAY_OF_WEEK}</li>
 *  <li>{@link #DAY_OF_YEAR}</li>
 *  <li>{@link #MONTH_AS_NUMBER}</li>
 *  <li>{@link #MONTH_OF_YEAR}</li>
 *  <li>{@link #QUARTER_OF_YEAR}</li>
 *  <li>{@link #WEEKDAY_IN_MONTH}</li>
 *  <li>{@link #YEAR}</li>
 *  <li>{@link #YEAR_OF_WEEKDATE}</li>
 * </ul>
 *
 * <p>Furthermore, all elements of classes {@link Weekmodel}, {@link EpochDays} and
 * {@code ChronoHistory} are supported. </p>
 *
 * @author      Meno Hochschild
 */
/*[deutsch]
 * <p>Repr&auml;sentiert ein reines Kalenderdatum im ISO-8601-Standard. </p>
 *
 * <p>Der Wertebereich fasst auch negative Jahre bis zu {@code -999999999}.
 * Diese Jahre sind nicht historisch zu interpretieren, sondern vielmehr
 * als eine andere Z&auml;hlweise &auml;hnlich zur Z&auml;hlung von
 * Epochentagen. Die Schaltjahrregeln des gregorianischen Kalenders werden
 * proleptisch, also r&uuml;ckwirkend auch in die ferne Vergangenheit
 * angewandt (eine ahistorische mathematische Abstraktion). </p>
 *
 * <p>Allerdings bietet diese Klasse <strong>volle Historisierung mittels eines historischen
 * Erweiterungsmechanismus</strong> an (i18n-Modul). Beim Formatieren oder Parsen basierend auf der
 * Expert-Maschine von Time4J gen&uuml;gt es, eine spezifische historische &Auml;ra (Mustersymbol G) mitsamt
 * einer Region anzugeben, und Time4J wird automatisch verschiedene Regeln der Kalendergeschichte anwenden.
 * Es ist auch m&ouml;glich, eine geeignete Kalendergeschichte zu modellieren, indem ein Objekt des Typs
 * {@code net.time4j.history.ChronoHistory} erzeugt wird, und die Formatmaschine mit dieser Information
 * zu f&uuml;ttern. Au&szlig;erdem bietet dieses Objekt spezielle Elemente an, die historische Abfragen
 * und Manipulationen eines ISO-Kalenderdatums erlauben. </p>
 *
 * <p>Registriert sind folgende als Konstanten deklarierte Elemente: </p>
 *
 * <ul>
 *  <li>{@link #COMPONENT}</li>
 *  <li>{@link #DAY_OF_MONTH}</li>
 *  <li>{@link #DAY_OF_QUARTER}</li>
 *  <li>{@link #DAY_OF_WEEK}</li>
 *  <li>{@link #DAY_OF_YEAR}</li>
 *  <li>{@link #MONTH_AS_NUMBER}</li>
 *  <li>{@link #MONTH_OF_YEAR}</li>
 *  <li>{@link #QUARTER_OF_YEAR}</li>
 *  <li>{@link #WEEKDAY_IN_MONTH}</li>
 *  <li>{@link #YEAR}</li>
 *  <li>{@link #YEAR_OF_WEEKDATE}</li>
 * </ul>
 *
 * <p>Dar&uuml;berhinaus sind alle Elemente der Klassen {@link Weekmodel}, {@link EpochDays}
 * und {@code ChronoHistory} nutzbar. </p>
 *
 * @author      Meno Hochschild
 */
@CalendarType("iso8601")
public final class PlainDate
    extends Calendrical<IsoDateUnit, PlainDate>
    implements GregorianDate, Normalizer<CalendarUnit>, ThreetenAdapter, LocalizedPatternSupport {

    //~ Statische Felder/Initialisierungen --------------------------------

    // rule index
    private static final int WIM_INDEX = 19;

    /** Fr&uuml;hestm&ouml;gliches Datum [-999999999-01-01]. */
    static final PlainDate MIN =
        new PlainDate(GregorianMath.MIN_YEAR, 1, 1, Weekday.MONDAY);

    /** Sp&auml;testm&ouml;gliches Datum [+999999999-12-31]. */
    static final PlainDate MAX =
        new PlainDate(GregorianMath.MAX_YEAR, 12, 31, Weekday.FRIDAY);

    /** Entspricht dem Jahr {@code -999999999}. */
    static final Integer MIN_YEAR =
        Integer.valueOf(GregorianMath.MIN_YEAR);

    /** Entspricht dem Jahr {@code +999999999}. */
    static final Integer MAX_YEAR =
        Integer.valueOf(GregorianMath.MAX_YEAR);

    private static final Integer VALUE_1 = Integer.valueOf(1);
    private static final Integer VALUE_12 = Integer.valueOf(12);
    private static final Integer STD_YEAR_LEN = Integer.valueOf(365);
    private static final Integer LEAP_YEAR_LEN = Integer.valueOf(366);
    private static final int[] DAY_OF_YEAR_PER_MONTH = new int[12];
    private static final int[] DAY_OF_LEAP_YEAR_PER_MONTH = new int[12];

    static {
        DAY_OF_YEAR_PER_MONTH[0] = 31;
        DAY_OF_YEAR_PER_MONTH[1] = 59;
        DAY_OF_YEAR_PER_MONTH[2] = 90;
        DAY_OF_YEAR_PER_MONTH[3] = 120;
        DAY_OF_YEAR_PER_MONTH[4] = 151;
        DAY_OF_YEAR_PER_MONTH[5] = 181;
        DAY_OF_YEAR_PER_MONTH[6] = 212;
        DAY_OF_YEAR_PER_MONTH[7] = 243;
        DAY_OF_YEAR_PER_MONTH[8] = 273;
        DAY_OF_YEAR_PER_MONTH[9] = 304;
        DAY_OF_YEAR_PER_MONTH[10] = 334;
        DAY_OF_YEAR_PER_MONTH[11] = 365;

        DAY_OF_LEAP_YEAR_PER_MONTH[0] = 31;
        DAY_OF_LEAP_YEAR_PER_MONTH[1] = 60;
        DAY_OF_LEAP_YEAR_PER_MONTH[2] = 91;
        DAY_OF_LEAP_YEAR_PER_MONTH[3] = 121;
        DAY_OF_LEAP_YEAR_PER_MONTH[4] = 152;
        DAY_OF_LEAP_YEAR_PER_MONTH[5] = 182;
        DAY_OF_LEAP_YEAR_PER_MONTH[6] = 213;
        DAY_OF_LEAP_YEAR_PER_MONTH[7] = 244;
        DAY_OF_LEAP_YEAR_PER_MONTH[8] = 274;
        DAY_OF_LEAP_YEAR_PER_MONTH[9] = 305;
        DAY_OF_LEAP_YEAR_PER_MONTH[10] = 335;
        DAY_OF_LEAP_YEAR_PER_MONTH[11] = 366;
    }

    /** Datumskomponente. */
    static final ChronoElement<PlainDate> CALENDAR_DATE = DateElement.INSTANCE;

    /**
     * <p>Element with the calendar date in the value range
     * {@code [-999999999-01-01]} until {@code [+999999999-12-31]}. </p>
     *
     * <p>Example of usage: </p>
     *
     * <pre>
     *  PlainTimestamp tsp = PlainTimestamp.of(2014, 8, 21, 14, 30);
     *  tsp = tsp.with(PlainDate.COMPONENT, PlainDate.of(2015, 1, 1));
     *  System.out.println(tsp); // output: 2015-01-01T14:30
     * </pre>
     *
     * @since   1.2
     */
    /*[deutsch]
     * <p>Element mit dem Datum im Wertebereich {@code [-999999999-01-01]}
     * bis {@code [+999999999-12-31]}. </p>
     *
     * <p>Beispiel: </p>
     *
     * <pre>
     *  PlainTimestamp tsp = PlainTimestamp.of(2014, 8, 21, 14, 30);
     *  tsp = tsp.with(PlainDate.COMPONENT, PlainDate.of(2015, 1, 1));
     *  System.out.println(tsp); // Ausgabe: 2015-01-01T14:30
     * </pre>
     *
     * @since   1.2
     */
    public static final CalendarDateElement COMPONENT = DateElement.INSTANCE;

    /**
     * <p>Element with the proleptic iso-year without any era reference and
     * the value range {@code -999999999} until {@code 999999999}. </p>
     *
     * <p>Examples: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.YEAR;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  System.out.println(date.get(YEAR)); // output: 2012
     *
     *  date = date.with(YEAR, 2014);
     *  System.out.println(date); // output: 2014-02-28
     *
     *  date = date.with(YEAR.incremented()); // n&auml;chstes Jahr
     *  System.out.println(date); // output: 2015-02-28
     *
     *  date = date.with(YEAR.atCeiling()); // letzter Tag des Jahres
     *  System.out.println(date); // output: 2015-12-31
     *
     *  date = date.with(YEAR.atFloor()); // erster Tag des Jahres
     *  System.out.println(date); // output: 2015-01-01
     * </pre>
     *
     * <p>The term &quot;proleptic&quot; means that the rules of the gregorian
     * calendar and the associated way of year counting is applied backward
     * even before the introduction of gregorian calendar. The year {@code 0}
     * is permitted - and negative years, too. For historic year numbers,
     * this mathematical extrapolation is not recommended and usually
     * wrong. </p>
     */
    /*[deutsch]
     * <p>Element mit dem proleptischen ISO-Jahr ohne &Auml;ra-Bezug mit dem
     * Wertebereich {@code -999999999} bis {@code 999999999}. </p>
     *
     * <p>Beispiele: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.YEAR;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  System.out.println(date.get(YEAR)); // Ausgabe: 2012
     *
     *  date = date.with(YEAR, 2014);
     *  System.out.println(date); // Ausgabe: 2014-02-28
     *
     *  date = date.with(YEAR.incremented()); // n&auml;chstes Jahr
     *  System.out.println(date); // Ausgabe: 2015-02-28
     *
     *  date = date.with(YEAR.atCeiling()); // letzter Tag des Jahres
     *  System.out.println(date); // Ausgabe: 2015-12-31
     *
     *  date = date.with(YEAR.atFloor()); // erster Tag des Jahres
     *  System.out.println(date); // Ausgabe: 2015-01-01
     * </pre>
     *
     * <p>Der Begriff &quot;proleptisch&quot; bedeutet, da&szlig; die Regeln
     * des gregorianischen Kalenders und damit verbunden die Jahresz&auml;hlung
     * auch r&uuml;ckwirkend vor der Einf&uuml;hrung des Kalenders angewandt
     * werden. Insbesondere ist auch das Jahr {@code 0} zugelassen - nebst
     * negativen Jahreszahlen. F&uuml;r historische Jahreszahlen ist diese
     * mathematische Extrapolation nicht geeignet. </p>
     */
    @FormattableElement(format = "u")
    public static final AdjustableElement<Integer, PlainDate> YEAR =
        IntegerDateElement.create(
            "YEAR",
            IntegerDateElement.YEAR,
            GregorianMath.MIN_YEAR,
            GregorianMath.MAX_YEAR,
            'u');

    /**
     * <p>Defines an element for the week-based year in an
     * ISO-8601-weekdate. </p>
     *
     * <p>The week-based year is usually the same as the calendar year.
     * However, at the begin or end of a calendar year the situation is
     * different because the first week of the weekdate can start after
     * New Year and the last week of the weekdate can end before the last
     * day of the calendar year. Examples: </p>
     *
     * <ul><li>Sunday, [1995-01-01] =&gt; [1994-W52-7]</li>
     * <li>Tuesday, [1996-31-12] =&gt; [1997-W01-2]</li></ul>
     *
     * <p>Note: This element has a special basic unit which can be used such
     * that the day of the week will be conserved instead of the day of month
     * after adding one week-based year: </p>
     *
     * <pre>
     *  PlainDate date = PlainDate.of(2014, JANUARY, 2); // Thursday
     *  IsoDateUnit unit = CalendarUnit.weekBasedYears();
     *  System.out.println(date.plus(1, unit)); // output: 2015-01-01
     * </pre>
     *
     * @see     CalendarUnit#weekBasedYears()
     * @see     Weekmodel#ISO
     */
    /*[deutsch]
     * <p>Definiert ein Element f&uuml;r das wochenbasierte Jahr in einem
     * ISO-Wochendatum. </p>
     *
     * <p>Das wochenbasierte Jahr stimmt in der Regel mit dem
     * Kalenderjahr &uuml;berein. Ausnahmen sind der Beginn und
     * das Ende des Kalenderjahres, weil die erste Woche des Jahres
     * erst nach Neujahr anfangen und die letzte Woche des Jahres
     * bereits vor Sylvester enden kann. Beispiele: </p>
     *
     * <ul><li>Sonntag, [1995-01-01] =&gt; [1994-W52-7]</li>
     * <li>Dienstag, [1996-31-12] =&gt; [1997-W01-2]</li></ul>
     *
     * <p>Notiz: Dieses Element hat eine spezielle Basiseinheit, die so
     * verwendet werden kann, da&szlig; nicht der Tag des Monats nach
     * einer Jahresaddition erhalten bleibt, sondern der Tag der Woche: </p>
     *
     * <pre>
     *  PlainDate date = PlainDate.of(2014, JANUARY, 2); // Donnerstag
     *  IsoDateUnit unit = CalendarUnit.weekBasedYears();
     *  System.out.println(date.plus(1, unit)); // Ausgabe: 2015-01-01
     * </pre>
     *
     * @see     CalendarUnit#weekBasedYears()
     * @see     Weekmodel#ISO
     */
    @FormattableElement(format = "Y")
    public static final AdjustableElement<Integer, PlainDate> YEAR_OF_WEEKDATE =
        YOWElement.INSTANCE;

    /**
     * <p>Element with the quarter of year in the value range
     * {@code Q1-Q4}. </p>
     */
    /*[deutsch]
     * <p>Element mit dem Quartal des Jahres (Wertebereich {@code Q1-Q4}). </p>
     */
    @FormattableElement(format = "Q", alt="q")
    public static final NavigableElement<Quarter> QUARTER_OF_YEAR =
        new EnumElement<>(
            "QUARTER_OF_YEAR",
            Quarter.class,
            Quarter.Q1,
            Quarter.Q4,
            EnumElement.QUARTER_OF_YEAR,
            'Q');

    /**
     * <p>Element with the calendar month as enum in the value range
     * {@code JANUARY-DECEMBER}). </p>
     *
     * <p>Examples: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.MONTH_OF_YEAR;
     *  import static net.time4j.Month.*;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  System.out.println(date.get(MONTH_OF_YEAR)); // output: February
     *
     *  date = date.with(MONTH_OF_YEAR, APRIL);
     *  System.out.println(date); // output: 2012-04-29
     *
     *  date = date.with(MONTH_OF_YEAR.incremented()); // next month
     *  System.out.println(date); // output: 2012-05-29
     *
     *  date = date.with(MONTH_OF_YEAR.maximized()); // last month of year
     *  System.out.println(date); // output: 2012-12-29
     *
     *  date = date.with(MONTH_OF_YEAR.atCeiling()); // last day of month
     *  System.out.println(date); // output: 2012-12-31
     *
     *  date = date.with(MONTH_OF_YEAR.atFloor()); // first day of month
     *  System.out.println(date); // output: 2012-12-01
     *
     *  date = date.with(MONTH_OF_YEAR.setToNext(JULY)); // move to July
     *  System.out.println(date); // output: 2013-07-01
     * </pre>
     */
    /*[deutsch]
     * <p>Element mit dem Monat als Enum
     * (Wertebereich {@code JANUARY-DECEMBER}). </p>
     *
     * <p>Beispiele: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.MONTH_OF_YEAR;
     *  import static net.time4j.Month.*;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  System.out.println(date.get(MONTH_OF_YEAR)); // Ausgabe: February
     *
     *  date = date.with(MONTH_OF_YEAR, APRIL);
     *  System.out.println(date); // Ausgabe: 2012-04-29
     *
     *  date = date.with(MONTH_OF_YEAR.incremented()); // n&auml;chster Monat
     *  System.out.println(date); // Ausgabe: 2012-05-29
     *
     *  date = date.with(MONTH_OF_YEAR.maximized()); // letzter Monat im Jahr
     *  System.out.println(date); // Ausgabe: 2012-12-29
     *
     *  date = date.with(MONTH_OF_YEAR.atCeiling()); // letzter Monatstag
     *  System.out.println(date); // Ausgabe: 2012-12-31
     *
     *  date = date.with(MONTH_OF_YEAR.atFloor()); // erster Tag im Monat
     *  System.out.println(date); // Ausgabe: 2012-12-01
     *
     *  date = date.with(MONTH_OF_YEAR.setToNext(JULY)); // zum Juli vorangehen
     *  System.out.println(date); // Ausgabe: 2013-07-01
     * </pre>
     */
    @FormattableElement(format = "M", alt="L")
    public static final NavigableElement<Month> MONTH_OF_YEAR =
        new EnumElement<>(
            "MONTH_OF_YEAR",
            Month.class,
            Month.JANUARY,
            Month.DECEMBER,
            EnumElement.MONTH,
            'M');

    /**
     * <p>Element with the calendar month in numerical form and the value range
     * {@code 1-12}. </p>
     *
     * <p>Normally the enum-variant is recommended due to clarity and
     * type-safety. The enum-form can also be formatted as text. However,
     * if users want to set any month number in a lenient way with possible
     * carry-over then they can do it like in following example: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.MONTH_AS_NUMBER;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  date = date.with(MONTH_AS_NUMBER.setLenient(13);
     *  System.out.println(date); // output: 2013-01-29
     * </pre>
     *
     * @see     #MONTH_OF_YEAR
     */
    /*[deutsch]
     * <p>Element mit dem Monat in Nummernform (Wertebereich {@code 1-12}). </p>
     *
     * <p>Im allgemeinen empfiehlt sich wegen der Typsicherheit und sprachlichen
     * Klarheit die enum-Form, die zudem auch als Text formatierbar ist. Wenn
     * Anwender jedoch irgendeine Monatsnummer nachsichtig mit m&ouml;glichem
     * &Uuml;berlauf setzen wollen, k&ouml;nnen sie es wie folgt tun: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.MONTH_AS_NUMBER;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  date = date.with(MONTH_AS_NUMBER.setLenient(13);
     *  System.out.println(date); // Ausgabe: 2013-01-29
     * </pre>
     *
     * @see     #MONTH_OF_YEAR
     */
    @FormattableElement(format = "M")
    public static final ProportionalElement<Integer, PlainDate> MONTH_AS_NUMBER =
        IntegerDateElement.create(
            "MONTH_AS_NUMBER",
            IntegerDateElement.MONTH,
            1,
            12,
            'M');

    /**
     * <p>Element with the day of month in the value range
     * {@code 1-28/29/30/31}. </p>
     */
    /*[deutsch]
     * <p>Element mit dem Tag des Monats
     * (Wertebereich {@code 1-28/29/30/31}). </p>
     */
    @FormattableElement(format = "d")
    public static final ProportionalElement<Integer, PlainDate> DAY_OF_MONTH =
        IntegerDateElement.create(
            "DAY_OF_MONTH",
            IntegerDateElement.DAY_OF_MONTH,
            1,
            31,
            'd');

    /**
     * <p>Element with the day of week in the value range {@code MONDAY-SUNDAY}. </p>
     *
     * <p>A localized form is available by {@link Weekmodel#localDayOfWeek()}.
     * In US sunday is considered as first day of week, different from
     * definition used here (monday as start of calendar week according to
     * ISO-8601). Therefore, if users need localized weekday-numbers, users
     * can use the expression {@code Weekmodel.of(Locale.US).localDayOfWeek()}
     * in a country like US. </p>
     */
    /*[deutsch]
     * <p>Element mit dem Tag der Woche (Wertebereich {@code MONDAY-SUNDAY}). </p>
     *
     * <p>Eine lokalisierte Form ist mittels {@link Weekmodel#localDayOfWeek()}
     * verf&uuml;gbar. In den USA z.B. ist der Sonntag der erste Tag der
     * Woche, anders als hier definiert. Daher sollte in den USA vielmehr
     * der Ausdruck {@code Weekmodel.of(Locale.US).localDayOfWeek()}
     * verwendet werden. </p>
     */
    @FormattableElement(format = "E")
    public static final NavigableElement<Weekday> DAY_OF_WEEK =
        new EnumElement<>(
            "DAY_OF_WEEK",
            Weekday.class,
            Weekday.MONDAY,
            Weekday.SUNDAY,
            EnumElement.DAY_OF_WEEK,
            'E');

    /**
     * <p>Element with the day of year in the value range {@code 1-365/366}). </p>
     */
    /*[deutsch]
     * <p>Element mit dem Tag des Jahres (Wertebereich {@code 1-365/366}). </p>
     */
    @FormattableElement(format = "D")
    public static final ProportionalElement<Integer, PlainDate> DAY_OF_YEAR =
        IntegerDateElement.create(
            "DAY_OF_YEAR",
            IntegerDateElement.DAY_OF_YEAR,
            1,
            365,
            'D');

    /**
     * <p>Element with the day within a quarter of year in the value range
     * {@code 1-90/91/92}. </p>
     */
    /*[deutsch]
     * <p>Element mit dem Tag des Quartals
     * (Wertebereich {@code 1-90/91/92}). </p>
     */
    public static final ProportionalElement<Integer, PlainDate> DAY_OF_QUARTER =
        IntegerDateElement.create(
            "DAY_OF_QUARTER",
            IntegerDateElement.DAY_OF_QUARTER,
            1,
            92,
            '\u0000');

    /**
     * <p>Element with the ordinal day-of-week within given calendar month
     * in the value range {@code 1-5}. </p>
     *
     * <p>Example: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.WEEKDAY_IN_MONTH;
     *  import static net.time4j.Weekday.*;
     *
     *  PlainDate date = PlainDate.of(2013, 3, 1); // first of march 2013
     *  System.out.println(date.with(WEEKDAY_IN_MONTH.setToThird(WEDNESDAY)));
     *  // output: 2013-03-20 (third Wednesday in march)
     * </pre>
     */
    /*[deutsch]
     * <p>Element mit dem x-ten Wochentag im Monat
     * (Wertebereich {@code 1-5}). </p>
     *
     * <p>Beispiel: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.WEEKDAY_IN_MONTH;
     *  import static net.time4j.Weekday.*;
     *
     *  PlainDate date = PlainDate.of(2013, 3, 1); // 1. M&auml;rz 2013
     *  System.out.println(date.with(WEEKDAY_IN_MONTH.setToThird(WEDNESDAY)));
     *  // Ausgabe: 2013-03-20 (Mittwoch)
     * </pre>
     */
    @FormattableElement(format = "F")
    public static final OrdinalWeekdayElement WEEKDAY_IN_MONTH =
        WeekdayInMonthElement.INSTANCE;

    // Dient der Serialisierungsunterstützung.
    private static final long serialVersionUID = -6698431452072325688L;

    private static final Map<String, Object> ELEMENTS;
    private static final CalendarSystem<PlainDate> TRANSFORMER;
    private static final TimeAxis<IsoDateUnit, PlainDate> ENGINE;

    static {
        Map<String, Object> constants = new HashMap<>();
        fill(constants, CALENDAR_DATE);
        fill(constants, YEAR);
        fill(constants, YEAR_OF_WEEKDATE);
        fill(constants, QUARTER_OF_YEAR);
        fill(constants, MONTH_OF_YEAR);
        fill(constants, MONTH_AS_NUMBER);
        fill(constants, DAY_OF_MONTH);
        fill(constants, DAY_OF_WEEK);
        fill(constants, DAY_OF_YEAR);
        fill(constants, DAY_OF_QUARTER);
        fill(constants, WEEKDAY_IN_MONTH);
        ELEMENTS = Collections.unmodifiableMap(constants);

        TRANSFORMER = new Transformer();


        TimeAxis.Builder<IsoDateUnit, PlainDate> builder =
            TimeAxis.Builder.setUp(
                IsoDateUnit.class,
                PlainDate.class,
                new Merger(),
                TRANSFORMER)
            .appendElement(
                CALENDAR_DATE,
                new DateElementRule(),
                CalendarUnit.DAYS)
            .appendElement(
                YEAR,
                new IntegerElementRule(YEAR),
                CalendarUnit.YEARS)
            .appendElement(
                YEAR_OF_WEEKDATE,
                YOWElement.elementRule(PlainDate.class),
                Weekcycle.YEARS)
            .appendElement(
                QUARTER_OF_YEAR,
                EnumElementRule.of(QUARTER_OF_YEAR),
                CalendarUnit.QUARTERS)
            .appendElement(
                MONTH_OF_YEAR,
                EnumElementRule.of(MONTH_OF_YEAR),
                CalendarUnit.MONTHS)
            .appendElement(
                MONTH_AS_NUMBER,
                new IntegerElementRule(MONTH_AS_NUMBER),
                CalendarUnit.MONTHS)
            .appendElement(
                DAY_OF_MONTH,
                new IntegerElementRule(DAY_OF_MONTH),
                CalendarUnit.DAYS)
            .appendElement(
                DAY_OF_WEEK,
                EnumElementRule.of(DAY_OF_WEEK),
                CalendarUnit.DAYS)
            .appendElement(
                DAY_OF_YEAR,
                new IntegerElementRule(DAY_OF_YEAR),
                CalendarUnit.DAYS)
            .appendElement(
                DAY_OF_QUARTER,
                new IntegerElementRule(DAY_OF_QUARTER),
                CalendarUnit.DAYS)
            .appendElement(
                WEEKDAY_IN_MONTH,
                new IntegerElementRule(WIM_INDEX, WEEKDAY_IN_MONTH),
                CalendarUnit.WEEKS);
        registerUnits(builder);
        registerExtensions(builder);
        ENGINE = builder.build();
    }

    private static final Chronology<LocalDate> THREETEN = new BridgeChronology<>(TemporalType.LOCAL_DATE, ENGINE);

    //~ Instanzvariablen --------------------------------------------------

    private transient final int year;
    private transient final byte month;
    private transient final byte dayOfMonth;

    // racy-single-check-idiom not applicable due to value-type-constraints in future Java
    private transient final Weekday weekday;

    //~ Konstruktoren -----------------------------------------------------

    private PlainDate(
        int year,
        int month,
        int dayOfMonth,
        Weekday weekday
    ) {
        super();

        this.year = year;
        this.month = (byte) month;
        this.dayOfMonth = (byte) dayOfMonth;
        this.weekday = weekday;

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Creates a new calendar date conforming to ISO-8601. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   month       gregorian month in range (1-12)
     * @param   dayOfMonth  day of month in range (1-31)
     * @return  new or cached calendar date instance
     * @throws  IllegalArgumentException if any argument is out of range
     * @see     #of(int, Month, int)
     * @see     #of(int, int)
     * @see     #of(int, int, Weekday)
     */
    /*[deutsch]
     * <p>Erzeugt ein neues ISO-konformes Kalenderdatum. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   month       gregorian month in range (1-12)
     * @param   dayOfMonth  day of month in range (1-31)
     * @return  new or cached calendar date instance
     * @throws  IllegalArgumentException if any argument is out of range
     * @see     #of(int, Month, int)
     * @see     #of(int, int)
     * @see     #of(int, int, Weekday)
     */
    public static PlainDate of(
        int year,
        int month,
        int dayOfMonth
    ) {

        return PlainDate.create(year, month, dayOfMonth, null, true);

    }

    /**
     * <p>Creates a new calendar date conforming to ISO-8601. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   month       gregorian month in range (January-December)
     * @param   dayOfMonth  day of month in range (1-31)
     * @return  new or cached calendar date instance
     * @throws  IllegalArgumentException if any argument is out of range
     * @see     #of(int, int, int)
     */
    /*[deutsch]
     * <p>Erzeugt ein neues ISO-konformes Kalenderdatum. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   month       gregorian month in range (January-December)
     * @param   dayOfMonth  day of month in range (1-31)
     * @return  new or cached calendar date instance
     * @throws  IllegalArgumentException if any argument is out of range
     * @see     #of(int, int, int)
     */
    public static PlainDate of(
        int year,
        Month month,
        int dayOfMonth
    ) {

        return PlainDate.create(year, month.getValue(), dayOfMonth, null, true);

    }

    /**
     * <p>Creates a new ordinal date conforming to ISO-8601. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   dayOfYear   day of year in the range (1-366)
     * @return  new or cached ordinal date instance
     * @throws  IllegalArgumentException if any argument is out of range
     */
    /*[deutsch]
     * <p>Erzeugt ein neues ISO-konformes Ordinaldatum. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   dayOfYear   day of year in the range (1-366)
     * @return  new or cached ordinal date instance
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public static PlainDate of(
        int year,
        int dayOfYear
    ) {

        return PlainDate.ofYearDay(year, dayOfYear, null);

    }

    /**
     * <p>Creates a new week-date conforming to ISO-8601. </p>
     *
     * @param   yearOfWeekdate  week-based-year according to ISO-definition
     * @param   weekOfYear      week of year in the range (1-52/53)
     * @param   dayOfWeek       day of week in the range (MONDAY-SUNDAY)
     * @return  new or cached week date instance
     * @throws  IllegalArgumentException if any argument is out of range
     */
    /*[deutsch]
     * <p>Erzeugt ein neues ISO-konformes Wochendatum. </p>
     *
     * @param   yearOfWeekdate  week-based-year according to ISO-definition
     * @param   weekOfYear      week of year in the range (1-52/53)
     * @param   dayOfWeek       day of week in the range (MONDAY-SUNDAY)
     * @return  new or cached week date instance
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public static PlainDate of(
        int yearOfWeekdate,
        int weekOfYear,
        Weekday dayOfWeek
    ) {

        return PlainDate.ofWeekdate(yearOfWeekdate, weekOfYear, dayOfWeek, true);

    }

    /**
     * <p>Creates a new date based on count of days since given epoch. </p>
     *
     * @param   amount      count of days
     * @param   epoch       reference date scale
     * @return  found calendar date based on given epoch days
     * @throws  IllegalArgumentException if first argument is out of range
     */
    /*[deutsch]
     * <p>Erzeugt ein Datum zur gegebenen Anzahl von Tagen seit einer
     * Epoche. </p>
     *
     * @param   amount      count of days
     * @param   epoch       reference date scale
     * @return  found calendar date based on given epoch days
     * @throws  IllegalArgumentException if first argument is out of range
     */
    public static PlainDate of(
        long amount,
        EpochDays epoch
    ) {

        return TRANSFORMER.transform(EpochDays.UTC.transform(amount, epoch));

    }

    /**
     * <p>Obtains the current date in system time. </p>
     *
     * <p>Convenient short-cut for: {@code SystemClock.inLocalView().today()}. </p>
     *
     * @return  current calendar date in system time zone using the system clock
     * @see     SystemClock#inLocalView()
     * @see     ZonalClock#today()
     * @since   3.23/4.19
     */
    /*[deutsch]
     * <p>Ermittelt das aktuelle Kalenderdatum in der Systemzeit. </p>
     *
     * <p>Bequeme Abk&uuml;rzung f&uuml;r: {@code SystemClock.inLocalView().today()}. </p>
     *
     * @return  current calendar date in system time zone using the system clock
     * @see     SystemClock#inLocalView()
     * @see     ZonalClock#today()
     * @since   3.23/4.19
     */
    public static PlainDate nowInSystemTime() {

        return ZonalClock.ofSystem().today();

    }

    /**
     * <p>Common conversion method for proleptic gregorian dates. </p>
     *
     * @param   date    ISO-date
     * @return  PlainDate
     */
    /*[deutsch]
     * <p>Allgemeine Konversionsmethode f&uuml;r ein proleptisches
     * gregorianisches Datum. </p>
     *
     * @param   date    ISO-date
     * @return  PlainDate
     */
    public static PlainDate from(GregorianDate date) {

        if (date instanceof PlainDate) {
            return (PlainDate) date;
        } else {
            return PlainDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth());
        }

    }

    /**
     * <p>Short cut for {@code TemporalType.LOCAL_DATE.translate(date)}. </p>
     *
     * @param   date    Threeten-equivalent of this instance
     * @return  PlainDate
     * @since   4.0
     * @see     TemporalType#LOCAL_DATE
     */
    /*[deutsch]
     * <p>Abk&uuml;rzung f&uuml;r {@code TemporalType.LOCAL_DATE.translate(date)}. </p>
     *
     * @param   date    Threeten-equivalent of this instance
     * @return  PlainDate
     * @since   4.0
     * @see     TemporalType#LOCAL_DATE
     */
    public static PlainDate from(LocalDate date) {

        return TemporalType.LOCAL_DATE.translate(date);

    }

    /**
     * <p>Creates a new local timestamp with this date at midnight at the
     * begin of associated day. </p>
     *
     * @return  local timestamp as composition of this date and midnight
     * @see     #at(PlainTime)
     */
    /*[deutsch]
     * <p>Erzeugt einen lokalen Zeitstempel mit diesem Datum zu Mitternacht
     * am Beginn des Tages. </p>
     *
     * @return  local timestamp as composition of this date and midnight
     * @see     #at(PlainTime)
     */
    public PlainTimestamp atStartOfDay() {

        return this.at(PlainTime.MIN);

    }

    /**
     * <p>Creates a new local timestamp with this date at earliest valid time
     * at the begin of associated day in given timezone. </p>
     *
     * @param   tzid        timezone id
     * @return  local timestamp as composition of this date and earliest valid time
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @see     #atStartOfDay()
     * @see     #atFirstMoment(TZID)
     * @since   2.2
     */
    /*[deutsch]
     * <p>Erzeugt einen lokalen Zeitstempel mit diesem Datum zur fr&uuml;hesten
     * g&uuml;ltigen Uhrzeit in der angegebenen Zeitzone. </p>
     *
     * @param   tzid        timezone id
     * @return  local timestamp as composition of this date and earliest valid time
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @see     #atStartOfDay()
     * @see     #atFirstMoment(TZID)
     * @since   2.2
     */
    public PlainTimestamp atStartOfDay(TZID tzid) {

        return this.atStartOfDay(Timezone.of(tzid).getHistory());

    }

    /**
     * <p>Creates a new local timestamp with this date at earliest valid time
     * at the begin of associated day in given timezone. </p>
     *
     * @param   tzid        timezone id
     * @return  local timestamp as composition of this date and earliest valid time
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @see     #atStartOfDay()
     * @see     #atFirstMoment(String)
     * @since   2.2
     */
    /*[deutsch]
     * <p>Erzeugt einen lokalen Zeitstempel mit diesem Datum zur fr&uuml;hesten
     * g&uuml;ltigen Uhrzeit in der angegebenen Zeitzone. </p>
     *
     * @param   tzid        timezone id
     * @return  local timestamp as composition of this date and earliest valid time
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @since   2.2
     * @see     #atStartOfDay()
     * @see     #atFirstMoment(String)
     * @since   2.2
     */
    public PlainTimestamp atStartOfDay(String tzid) {

        return this.atStartOfDay(Timezone.of(tzid).getHistory());

    }

    /**
     * <p>Creates a new moment which corresponds to this date at earliest valid time
     * at the begin of associated day in given timezone. </p>
     *
     * @param   tzid        timezone id
     * @return  first valid moment corresponding to start of day in given timezone
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @see     #atStartOfDay(TZID)
     * @since   3.22/4.18
     */
    /*[deutsch]
     * <p>Erzeugt einen Moment, der diesem Datum zur fr&uuml;hesten
     * g&uuml;ltigen Uhrzeit in der angegebenen Zeitzone entspricht. </p>
     *
     * @param   tzid        timezone id
     * @return  first valid moment corresponding to start of day in given timezone
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @see     #atStartOfDay(TZID)
     * @since   3.22/4.18
     */
    public Moment atFirstMoment(TZID tzid) {

        return this.atFirstMoment(Timezone.of(tzid));

    }

    /**
     * <p>Creates a new moment which corresponds to this date at earliest valid time
     * at the begin of associated day in given timezone. </p>
     *
     * @param   tzid        timezone id
     * @return  first valid moment corresponding to start of day in given timezone
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @see     #atStartOfDay(String)
     * @since   3.22/4.18
     */
    /*[deutsch]
     * <p>Erzeugt einen Moment, der diesem Datum zur fr&uuml;hesten
     * g&uuml;ltigen Uhrzeit in der angegebenen Zeitzone entspricht. </p>
     *
     * @param   tzid        timezone id
     * @return  first valid moment corresponding to start of day in given timezone
     * @throws  IllegalArgumentException if given timezone cannot be loaded
     * @throws  UnsupportedOperationException if the underlying timezone
     *          repository does not expose any public transition history
     * @see     #atStartOfDay(String)
     * @since   3.22/4.18
     */
    public Moment atFirstMoment(String tzid) {

        return this.atFirstMoment(Timezone.of(tzid));

    }

    /**
     * <p>Creates a new local timestamp with this date and given wall time. </p>
     *
     * <p>If the time {@link PlainTime#midnightAtEndOfDay() T24:00} is used
     * then the resulting timestamp will automatically be normalized such
     * that the timestamp will contain the following day instead. </p>
     *
     * @param   time    wall time
     * @return  local timestamp as composition of this date and given time
     */
    /*[deutsch]
     * <p>Erzeugt einen lokalen Zeitstempel mit diesem Datum und der
     * angegebenen Uhrzeit. </p>
     *
     * <p>Wenn {@link PlainTime#midnightAtEndOfDay() T24:00} angegeben wird,
     * dann wird der Zeitstempel automatisch so normalisiert, da&szlig; er auf
     * den n&auml;chsten Tag verweist. </p>
     *
     * @param   time    wall time
     * @return  local timestamp as composition of this date and given time
     */
    public PlainTimestamp at(PlainTime time) {

        return PlainTimestamp.of(this, time);

    }

    /**
     * <p>Is equivalent to {@code at(PlainTime.of(hour, minute))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @return  local timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    /*[deutsch]
     * <p>Entspricht {@code at(PlainTime.of(hour, minute))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @return  local timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public PlainTimestamp atTime(
        int hour,
        int minute
    ) {

        return this.at(PlainTime.of(hour, minute));

    }

    /**
     * <p>Is equivalent to {@code at(PlainTime.of(hour, minute, second))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @param   second      second of hour in range (0-59)
     * @return  local timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    /*[deutsch]
     * <p>Entspricht {@code at(PlainTime.of(hour, minute, second))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @param   second      second of hour in range (0-59)
     * @return  local timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public PlainTimestamp atTime(
        int hour,
        int minute,
        int second
    ) {

        return this.at(PlainTime.of(hour, minute, second));

    }

    @Override
    public int getYear() {

        return this.year;

    }

    @Override
    public int getMonth() {

        return this.month;

    }

    @Override
    public int getDayOfMonth() {

        return this.dayOfMonth;

    }

    /**
     * <p>Determines the day of week. </p>
     *
     * @return Weekday
     * @since 3.13/4.10
     */
    /*[deutsch]
     * <p>Ermittelt den Wochentag. </p>
     *
     * @return  Weekday
     * @since   3.13/4.10
     */
    public Weekday getDayOfWeek() {

        Weekday dow = this.weekday;

        if (dow == null) {
            dow = Weekday.valueOf(GregorianMath.getDayOfWeek(this.year, this.month, this.dayOfMonth));
        }

        return dow;

    }

    /**
     * <p>Yields the day of year. </p>
     *
     * @return int
     * @since 3.13/4.10
     */
    /*[deutsch]
     * <p>Liefert den Tag des Jahres. </p>
     *
     * @return  int
     * @since   3.13/4.10
     */
    public int getDayOfYear() {

        switch (this.month) {
            case 1:
                return this.dayOfMonth;
            case 2:
                return 31 + this.dayOfMonth;
            default:
                return (
                    DAY_OF_YEAR_PER_MONTH[this.month - 2]
                    + this.dayOfMonth
                    + (GregorianMath.isLeapYear(this.year) ? 1 : 0));
        }

    }

    /**
     * <p>Calculates the length of associated month in days. </p>
     *
     * @return  int in value range {@code 28-31}
     */
    /*[deutsch]
     * <p>Ermittelt die L&auml;nge des assoziierten Monats in Tagen. </p>
     *
     * @return  int im Bereich {@code 28-31}
     */
    public int lengthOfMonth() {

        return GregorianMath.getLengthOfMonth(this.year, this.month);

    }

    /**
     * <p>Calculates the length of associated year in days. </p>
     *
     * @return  {@code 365} or {@code 366} if associated year is a leap year
     */
    /*[deutsch]
     * <p>Ermittelt die L&auml;nge des assoziierten Jahres in Tagen. </p>
     *
     * @return  {@code 365} or {@code 366} wenn das Jahr ein Schaltjahr ist
     */
    public int lengthOfYear() {

        return this.isLeapYear() ? 366 : 365;

    }

    /**
     * <p>Is the year of this date a leap year? </p>
     *
     * @return  boolean
     */
    /*[deutsch]
     * <p>Liegt dieses Datum in einem Schaltjahr? </p>
     *
     * @return  boolean
     */
    public boolean isLeapYear() {

        return GregorianMath.isLeapYear(this.year);

    }

    /**
     * <p>Does this date fall on a week-end in given country? </p>
     *
     * @param   country     country setting with two-letter ISO-3166-code
     * @return  {@code true} if in given country this date is on weekend
     *          else {@code false}
     * @see     Weekmodel#weekend()
     */
    /*[deutsch]
     * <p>Liegt das Datum im angegebenen Land an einem Wochenende? </p>
     *
     * @param   country     country setting with two-letter ISO-3166-code
     * @return  {@code true} if in given country this date is on weekend
     *          else {@code false}
     * @see     Weekmodel#weekend()
     */
    public boolean isWeekend(Locale country) {

        return this.matches(Weekmodel.of(country).weekend());

    }

    /**
     * <p>Queries if given parameter values form a well defined calendar date. </p>
     *
     * <p>This method only checks the range limits, not if the date is historically correct. </p>
     *
     * @param   year        the proleptic year to be checked
     * @param   month       the month to be checked
     * @param   dayOfMonth  the day of month to be checked
     * @return  {@code true} if valid else  {@code false}
     * @see     #of(int, int, int)
     * @since   3.34/4.29
     */
    /*[deutsch]
     * <p>Pr&uuml;ft, ob die angegebenen Parameter ein wohldefiniertes Kalenderdatum beschreiben. </p>
     *
     * <p>Hier werden nur die Bereichsgrenzen &uuml;berpr&uuml;ft, nicht die historische Sinnhaftigkeit. </p>
     *
     * @param   year        the proleptic year to be checked
     * @param   month       the month to be checked
     * @param   dayOfMonth  the day of month to be checked
     * @return  {@code true} if valid else  {@code false}
     * @see     #of(int, int, int)
     * @since   3.34/4.29
     */
    public static boolean isValid(
        int year,
        int month,
        int dayOfMonth
    ) {

        return GregorianMath.isValid(year, month, dayOfMonth);

    }

    /**
     * <p>Adds given amount in units to this date and yields the result of addition. </p>
     *
     * <p>Covers the most important units and is overloaded for performance reasons. </p>
     *
     * @param   amount      the amount of units to be added to this date (maybe negative)
     * @param   unit        the unit to be used in addition
     * @return  result of addition as changed copy while this instance remains unaffected
     * @throws  ArithmeticException in case of numerical overflow
     * @see     #plus(long, Object) plus(long, IsoDateUnit)
     * @since   4.18
     */
    /*[deutsch]
     * <p>Addiert den angegebenen Betrag der entsprechenden Zeiteinheit
     * zu diesem Datum und liefert das Additionsergebnis zur&uuml;ck. </p>
     *
     * <p>Deckt die wichtigsten Zeiteinheiten ab, die mit diesem Typ verwendet werden
     * k&ouml;nnen und ist aus Performance-Gr&uuml;nden &uuml;berladen. </p>
     *
     * @param   amount      the amount of units to be added to this date (maybe negative)
     * @param   unit        the unit to be used in addition
     * @return  result of addition as changed copy while this instance remains unaffected
     * @throws  ArithmeticException in case of numerical overflow
     * @see     #plus(long, Object) plus(long, IsoDateUnit)
     * @since   4.18
     */
    public PlainDate plus(
        long amount,
        CalendarUnit unit
    ) {

        if (unit == null) {
            throw new NullPointerException("Missing unit.");
        } else if (amount == 0) {
            return this;
        }

        try {
            return PlainDate.doAdd(unit, this, amount, OverflowUnit.POLICY_PREVIOUS_VALID_DATE);
        } catch (IllegalArgumentException iae) {
            ArithmeticException ex = new ArithmeticException("Result beyond boundaries of time axis.");
            ex.initCause(iae);
            throw ex;
        }

    }

    /**
     * <p>Subtracts given amount in units from this date and yields the result of subtraction. </p>
     *
     * <p>Covers the most important units and is overloaded for performance reasons. </p>
     *
     * @param   amount      the amount of units to be subtracted from this date (maybe negative)
     * @param   unit        the unit to be used in subtraction
     * @return  result of subtraction as changed copy while this instance remains unaffected
     * @throws  ArithmeticException in case of numerical overflow
     * @see     #minus(long, Object) minus(long, IsoDateUnit)
     * @since   4.18
     */
    /*[deutsch]
     * <p>Subtrahiert den angegebenen Betrag der entsprechenden Zeiteinheit
     * von diesem Datum und liefert das Subtraktionsergebnis zur&uuml;ck. </p>
     *
     * <p>Deckt die wichtigsten Zeiteinheiten ab, die mit diesem Typ verwendet werden
     * k&ouml;nnen und ist aus Performance-Gr&uuml;nden &uuml;berladen. </p>
     *
     * @param   amount      the amount of units to be subtracted from this date (maybe negative)
     * @param   unit        the unit to be used in subtraction
     * @return  result of subtraction as changed copy while this instance remains unaffected
     * @throws  ArithmeticException in case of numerical overflow
     * @see     #minus(long, Object) minus(long, IsoDateUnit)
     * @since   4.18
     */
    public PlainDate minus(
        long amount,
        CalendarUnit unit
    ) {

        return this.plus(Math.negateExact(amount), unit);

    }

    /**
     * <p>Creates a formatted output of this instance. </p>
     *
     * @param   printer     helps to format this instance
     * @return  formatted string
     * @since   5.0
     */
    /*[deutsch]
     * <p>Erzeugt eine formatierte Ausgabe dieser Instanz. </p>
     *
     * @param   printer     helps to format this instance
     * @return  formatted string
     * @since   5.0
     */
    public String print(TemporalFormatter<PlainDate> printer) {

        return printer.print(this);

    }

    /**
     * <p>Parses given text to an instance of this class. </p>
     *
     * @param   text        text to be parsed
     * @param   parser      helps to parse given text
     * @return  parsed result
     * @throws  IndexOutOfBoundsException if the text is empty
     * @throws  ChronoException if the text is not parseable
     * @since   5.0
     */
    /*[deutsch]
     * <p>Interpretiert den angegebenen Text zu einer Instanz dieser Klasse. </p>
     *
     * @param   text        text to be parsed
     * @param   parser      helps to parse given text
     * @return  parsed result
     * @throws  IndexOutOfBoundsException if the text is empty
     * @throws  ChronoException if the text is not parseable
     * @since   5.0
     */
    public static PlainDate parse(
        String text,
        TemporalFormatter<PlainDate> parser
    ) {

        try {
            return parser.parse(text);
        } catch (ParseException pe) {
            throw new ChronoException(pe.getMessage(), pe);
        }

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof PlainDate) {
            PlainDate that = (PlainDate) obj;
            return (this.dayOfMonth == that.dayOfMonth) && (this.month == that.month) && (this.year == that.year);
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {

        int y = this.year;
        return (y & 0xFFFFF800) ^ ((y << 11) + (this.month << 6) + (this.dayOfMonth));

    }

    /**
     * <p>Creates a canonical representation of the form
     * &quot;YYYY-MM-DD&quot; as documented in ISO-8601. </p>
     *
     * @return  canonical ISO-8601-formatted string
     */
    /*[deutsch]
     * <p>Erzeugt eine kanonische Darstellung im Format
     * &quot;yyyy-MM-dd&quot;. </p>
     *
     * @return  canonical ISO-8601-formatted string
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder(32);
        formatYear(sb, this.year);
        format2Digits(sb, this.month);
        format2Digits(sb, this.dayOfMonth);
        return sb.toString();

    }

    /**
     * <p>Normalized given timespan using years, months and days. </p>
     *
     * <p>This normalizer can also convert from days to months. Example: </p>
     *
     * <pre>
     *  Duration&lt;CalendarUnit&gt; dur = Duration.of(30, CalendarUnit.DAYS);
     *  Duration&lt;CalendarUnit&gt; result =
     *      PlainDate.of(2012, 2, 28).normalize(dur);
     *  System.out.println(result); // output: P1M1D (leap year!)
     * </pre>
     *
     * @param   timespan    to be normalized
     * @return  normalized duration in years, months and days
     */
    /*[deutsch]
     * <p>Normalisiert die angegebene Zeitspanne, indem Jahre, Monate und Tage
     * verwendet werden. </p>
     *
     * <p>Dieser Normalisierer kann auch von Tagen zu Monaten konvertieren.
     * Beispiel: </p>
     *
     * <pre>
     *  Duration&lt;CalendarUnit&gt; dur = Duration.of(30, CalendarUnit.DAYS);
     *  Duration&lt;CalendarUnit&gt; result =
     *      PlainDate.of(2012, 2, 28).normalize(dur);
     *  System.out.println(result); // Ausgabe: P1M1D (Schaltjahr!)
     * </pre>
     *
     * @param   timespan    to be normalized
     * @return  normalized duration in years, months and days
     */
    @Override
    public Duration<CalendarUnit> normalize(
        TimeSpan<? extends CalendarUnit> timespan) {

        return this.until(this.plus(timespan), Duration.inYearsMonthsDays());

    }

    @Override
    public LocalDate toTemporalAccessor() {

        return TemporalType.LOCAL_DATE.from(this);

    }

    @Override
    public String getFormatPattern(
        FormatStyle style,
        Locale locale
    ) {

        return ENGINE.getFormatPattern(style, locale);

    }

    /**
     * <p>Provides a static access to the associated chronology on base of
     * epoch days which contains the chronological rules. </p>
     *
     * @return  chronological system as time axis (never {@code null})
     */
    /*[deutsch]
     * <p>Liefert die zugeh&ouml;rige Zeitachse, die alle notwendigen
     * chronologischen Regeln enth&auml;lt. </p>
     *
     * @return  chronological system as time axis (never {@code null})
     */
    public static TimeAxis<IsoDateUnit, PlainDate> axis() {

        return ENGINE;

    }

    /**
     * <p>Obtains a bridge chronology for the type {@code java.time.LocalDate}. </p>
     *
     * @return  rule engine adapted for the type {@code java.time.LocalDate}
     * @see     #axis()
     * @since   5.0
     */
    /*[deutsch]
     * <p>Liefert eine an den Typ {@code java.time.LocalDate} angepasste Chronologie. </p>
     *
     * @return  rule engine adapted for the type {@code java.time.LocalDate}
     * @see     #axis()
     * @since   5.0
     */
    public static Chronology<LocalDate> threeten() {

        return THREETEN;

    }

    @Override
    protected TimeAxis<IsoDateUnit, PlainDate> getChronology() {

        return ENGINE;

    }

    @Override
    protected PlainDate getContext() {

        return this;

    }

    @Override
    protected int compareByTime(CalendarDate date) {

        if (date instanceof PlainDate) { // Optimierung
            PlainDate d1 = this;
            PlainDate d2 = (PlainDate) date;

            int delta = d1.year - d2.year;
            if (delta == 0) {
                delta = d1.month - d2.month;
                if (delta == 0) {
                    delta = d1.dayOfMonth - d2.dayOfMonth;
                }
            }

            return delta;
        }

        return super.compareByTime(date); // basiert auf Epochentagen

    }

    /**
     * <p>Liefert die Tage seit der UTC-Epoche. </p>
     *
     * @return  count of days since UTC (1972-01-01)
     */
    long getDaysSinceUTC() {

        return TRANSFORMER.transform(this);

    }

    /**
     * <p>Wandelt die Tage seit der UTC-Epoche in ein Datum um. </p>
     *
     * @param   utcDays     count of days since UTC (1972-01-01)
     * @return  found calendar date
     */
    PlainDate withDaysSinceUTC(long utcDays) {

        return TRANSFORMER.transform(utcDays);

    }

    /**
     * <p>Erzeugt ein neues Datum passend zur angegebenen absoluten Zeit. </p>
     *
     * @param   ut      unix time
     * @param   offset  shift of local time relative to UTC
     * @return  new calendar date
     */
    static PlainDate from(
        UnixTime ut,
        ZonalOffset offset
    ) {

        long localSeconds = ut.getPosixTime() + offset.getIntegralAmount();
        int localNanos = ut.getNanosecond() + offset.getFractionalAmount();

        if (localNanos < 0) {
            localSeconds--;
        } else if (localNanos >= 1000000000) {
            localSeconds++;
        }

        long mjd =
            EpochDays.MODIFIED_JULIAN_DATE.transform(
                MathUtils.floorDivide(localSeconds, 86400),
                EpochDays.UNIX);
        long packedDate = GregorianMath.toPackedDate(mjd);

        return PlainDate.of(
            GregorianMath.readYear(packedDate),
            GregorianMath.readMonth(packedDate),
            GregorianMath.readDayOfMonth(packedDate)
        );

    }

    /**
     * <p>Dient der Serialisierungsunterst&uuml;tzung. </p>
     *
     * @param   elementName     name of element
     * @return  found element or {@code null}
     */
    // optional
    static Object lookupElement(String elementName) {

        return ELEMENTS.get(elementName);

    }

    /**
     * <p>Additionsmethode. </p>
     *
     * @param   unit        calendar unit
     * @param   context     calendar date
     * @param   amount      amount to be added
     * @param   policy      overflow policy
     * @return  result of addition
     */
    static PlainDate doAdd(
        CalendarUnit unit,
        PlainDate context,
        long amount,
        int policy
    ) {

        switch (unit) {
            case MILLENNIA:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12 * 1000),
                    policy);
            case CENTURIES:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12 * 100),
                    policy);
            case DECADES:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12 * 10),
                    policy);
            case YEARS:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12),
                    policy);
            case QUARTERS:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 3),
                    policy);
            case MONTHS:
                long months =
                    MathUtils.safeAdd(context.getEpochMonths(), amount);
                return PlainDate.fromEpochMonths(
                    context,
                    months,
                    context.dayOfMonth,
                    policy);
            case WEEKS:
                return doAdd(
                    CalendarUnit.DAYS,
                    context,
                    MathUtils.safeMultiply(amount, 7),
                    policy);
            case DAYS:
                return addDays(context, amount);
            default:
                throw new UnsupportedOperationException(unit.name());
        }

    }

    /**
     * <p>Liefert die Epochenmonate relativ zu 1970. </p>
     *
     * @return  epoch months relative to 1970
     */
    long getEpochMonths() {

        return ((this.year - 1970) * 12L + this.month - 1);

    }

    /**
     * <p>Liefert die ISO-Kalenderwoche des Jahres. </p>
     *
     * <p>Als erste Kalenderwoche gilt die Woche, die mindestens vier Tage hat
     * und mit dem Montag anf&auml;ngt. Die Woche davor ist dann die letzte
     * Woche des vorherigen Jahres und kann noch in das aktuelle Jahr
     * hineinreichen. </p>
     *
     * @return  week of year in the range (1-53)
     * @see     Weekmodel#ISO
     */
    int getWeekOfYear() {

        return this.get(Weekmodel.ISO.weekOfYear()).intValue();

    }

    private PlainTimestamp atStartOfDay(TransitionHistory history) {

        if (history == null) {
            throw new UnsupportedOperationException(
                "Timezone repository does not expose its transition history: "
                + Timezone.getProviderInfo());
        }

        ZonalTransition conflict = history.getConflictTransition(this, PlainTime.MIN);

        if ((conflict != null) && conflict.isGap()) {
            long localSeconds =
                conflict.getPosixTime() + conflict.getTotalOffset();
            PlainDate date =
                PlainDate.of(
                    MathUtils.floorDivide(localSeconds, 86400),
                    EpochDays.UNIX);
            int secondsOfDay = MathUtils.floorModulo(localSeconds, 86400);
            int second = secondsOfDay % 60;
            int minutesOfDay = secondsOfDay / 60;
            int minute = minutesOfDay % 60;
            int hour = minutesOfDay / 60;
            PlainTime time = PlainTime.of(hour, minute, second);
            return PlainTimestamp.of(date, time);
        }

        return this.at(PlainTime.MIN);

    }

    private Moment atFirstMoment(Timezone tz) {

        TransitionHistory history = tz.getHistory();

        if (history == null) {
            throw new UnsupportedOperationException(
                "Timezone repository does not expose its transition history: "
                + Timezone.getProviderInfo());
        }

        ZonalTransition conflict = history.getConflictTransition(this, PlainTime.MIN);

        if ((conflict != null) && conflict.isGap()) {
            return Moment.of(conflict.getPosixTime(), TimeScale.POSIX);
        }

        return this.at(PlainTime.MIN).in(tz);

    }

    private int getDayOfQuarter() {

        switch (this.month) {
            case 1:
            case 4:
            case 7:
            case 10:
                return this.dayOfMonth;
            case 2:
            case 8:
            case 11:
                return 31 + this.dayOfMonth;
            case 3:
                return (
                    (GregorianMath.isLeapYear(this.year) ? 60 : 59)
                    + this.dayOfMonth);
            case 5:
                return 30 + this.dayOfMonth;
            case 6:
            case 12:
                return 61 + this.dayOfMonth;
            case 9:
                return 62 + this.dayOfMonth;
            default:
                throw new AssertionError("Unknown month: " + this.month);
        }

    }

    private PlainDate withYear(int year) {

        if (this.year == year) {
            return this;
        }

        int mlen = GregorianMath.getLengthOfMonth(year, this.month);

        return PlainDate.of(
            year,
            this.month,
            Math.min(mlen, this.dayOfMonth)
        );

    }

    private PlainDate withMonth(int month) {

        if (this.month == month) {
            return this;
        }

        int mlen = GregorianMath.getLengthOfMonth(this.year, month);

        return PlainDate.of(
            this.year,
            month,
            Math.min(mlen, this.dayOfMonth)
        );

    }

    private PlainDate withDayOfMonth(int dayOfMonth) {

        if (this.dayOfMonth == dayOfMonth) {
            return this;
        }

        return PlainDate.of(this.year, this.month, dayOfMonth);

    }

    private PlainDate withDayOfWeek(Weekday dayOfWeek) {

        Weekday old = this.getDayOfWeek();

        PlainDate date = (
            (this.weekday == null)
            ? new PlainDate(this.year, this.month, this.dayOfMonth, old)
            : this);

        if (old == dayOfWeek) {
            return date;
        }

        return PlainDate.addDays(date, dayOfWeek.getValue() - old.getValue());

    }

    private PlainDate withDayOfYear(int dayOfYear) {

        if (this.getDayOfYear() == dayOfYear) {
            return this;
        }

        return PlainDate.of(this.year, dayOfYear);

    }

    private static PlainDate fromEpochMonths(
        PlainDate context,
        long emonths,
        int dayOfMonth,
        int policy
    ) {

        if (
            (policy == OverflowUnit.POLICY_KEEPING_LAST_DATE)
            && (context.dayOfMonth == context.lengthOfMonth())
        ) {
            policy = OverflowUnit.POLICY_END_OF_MONTH;
        }

        int year =
            MathUtils.safeCast(
                MathUtils.safeAdd(
                    MathUtils.floorDivide(emonths, 12),
                    1970
                )
            );
        int month = MathUtils.floorModulo(emonths, 12) + 1;
        int max = GregorianMath.getLengthOfMonth(year, month);
        int dom = dayOfMonth;

        if (dayOfMonth > max) {
            switch (policy) {
                case OverflowUnit.POLICY_PREVIOUS_VALID_DATE:
                case OverflowUnit.POLICY_END_OF_MONTH:
                case OverflowUnit.POLICY_KEEPING_LAST_DATE:
                case OverflowUnit.POLICY_JODA_METRIC:
                    dom = max;
                    break;
                case OverflowUnit.POLICY_NEXT_VALID_DATE:
                    return PlainDate.fromEpochMonths(
                        context,
                        MathUtils.safeAdd(emonths, 1),
                        1,
                        policy);
                case OverflowUnit.POLICY_CARRY_OVER:
                    return PlainDate.fromEpochMonths(
                        context,
                        MathUtils.safeAdd(emonths, 1),
                        dayOfMonth - max,
                        policy);
                case OverflowUnit.POLICY_UNLESS_INVALID:
                    StringBuilder sb = new StringBuilder(32);
                    sb.append("Day of month out of range: ");
                    formatYear(sb, year);
                    format2Digits(sb, month);
                    format2Digits(sb, dayOfMonth);
                    throw new ChronoException(sb.toString());
                default:
                    throw new UnsupportedOperationException(
                        "Overflow policy not implemented: " + policy);
            }
        } else if(
            (dayOfMonth < max)
            && (policy == OverflowUnit.POLICY_END_OF_MONTH)
        ) {
            dom = max;
        }

        return PlainDate.of(year, month, dom);

    }

    private static PlainDate addDays(
        PlainDate date,
        long amount
    ) {

        long dom = MathUtils.safeAdd(date.dayOfMonth, amount);
        Weekday weekday = null;
        boolean hasDOW = (date.weekday != null);

        if (hasDOW) {
            if (amount == 1) {
                weekday = date.weekday.next();
            } else if (amount == 7) {
                weekday = date.weekday;
            }
        }

        if ((dom >= 1) && (dom <= 28)) {
            if (hasDOW && (weekday == null)) {
                weekday = date.weekday.roll((int) amount);
            }
            return PlainDate.create(date.year, date.month, (int) dom, weekday, false);
        }

        long doy = MathUtils.safeAdd(date.getDayOfYear(), amount);

        if ((doy >= 1) && (doy <= 365)) {
            if (hasDOW && (weekday == null)) {
                weekday = date.weekday.roll((int) amount);
            }
            return PlainDate.ofYearDay(date.year, (int) doy, weekday);
        }

        return TRANSFORMER.transform(MathUtils.safeAdd(date.getDaysSinceUTC(), amount));

    }

    private static void fill(
        Map<String, Object> map,
        ChronoElement<?> element
    ) {

        map.put(element.name(), element);

    }

    private static void formatYear(
        StringBuilder sb,
        int year
    ) {

        int value = year;

        if (value < 0) {
            sb.append('-');
            value = MathUtils.safeNegate(year);
        }

        if (value >= 10000) {
            if (year > 0) {
                sb.append('+');
            }
        } else if (value < 1000) {
            sb.append('0');
            if (value < 100) {
                sb.append('0');
                if (value < 10) {
                    sb.append('0');
                }
            }
        }

        sb.append(value);

    }

    private static void format2Digits(
        StringBuilder sb,
        int value
    ) {

        sb.append('-');

        if (value < 10) {
            sb.append('0');
        }

        sb.append(value);

    }

    private static void registerExtensions(TimeAxis.Builder<IsoDateUnit, PlainDate> builder) {

        for (ChronoExtension extension : ResourceLoader.getInstance().services(ChronoExtension.class)) {
            if (extension.accept(PlainDate.class)) {
                builder.appendExtension(extension);
            }
        }

        builder.appendExtension(new WeekExtension());

    }

    private static void registerUnits(TimeAxis.Builder<IsoDateUnit, PlainDate> builder) {

        Set<CalendarUnit> monthly =
            EnumSet.range(CalendarUnit.MILLENNIA, CalendarUnit.MONTHS);
        Set<CalendarUnit> daily =
            EnumSet.range(CalendarUnit.WEEKS, CalendarUnit.DAYS);

        for (CalendarUnit unit : CalendarUnit.values()) {
            builder.appendUnit(
                unit,
                new CalendarUnit.Rule<>(unit),
                unit.getLength(),
                (unit.compareTo(CalendarUnit.WEEKS) < 0) ? monthly : daily
            );
        }

    }

    private static String yowFailed(int yearOfWeekdate) {

        return "YEAR_OF_WEEKDATE (ISO) out of range: " + yearOfWeekdate;

    }

    private static String woyFailed(int weekOfYear) {

        return "WEEK_OF_YEAR (ISO) out of range: " + weekOfYear;

    }

    private static PlainDate ofWeekdate(
        int yearOfWeekdate,
        int weekOfYear,
        Weekday dayOfWeek,
        boolean validating
    ) {

        if ((weekOfYear < 1) || (weekOfYear > 53)) {
            if (validating) {
                throw new IllegalArgumentException(woyFailed(weekOfYear));
            } else {
                return null;
            }
        }

        if (
            validating
            && ((yearOfWeekdate < MIN_YEAR) || (yearOfWeekdate > MAX_YEAR))
        ) {
            throw new IllegalArgumentException(yowFailed(yearOfWeekdate));
        }

        Weekday wdNewYear =
            Weekday.valueOf(GregorianMath.getDayOfWeek(yearOfWeekdate, 1, 1));
        int dow = wdNewYear.getValue();
        int doy = (
            ((dow <= 4) ? 2 - dow : 9 - dow)
            + (weekOfYear - 1) * 7
            + dayOfWeek.getValue() - 1
        );
        int y = yearOfWeekdate;

        if (doy <= 0) {
            y--;
            doy += (GregorianMath.isLeapYear(y) ? 366 : 365);
        } else {
            int yearlen =
                (GregorianMath.isLeapYear(y) ? 366 : 365);
            if (doy > yearlen) {
                doy -= yearlen;
                y++;
            }
        }

        PlainDate result = PlainDate.ofYearDay(y, doy, dayOfWeek);

        if ((weekOfYear == 53) && (result.getWeekOfYear() != 53)) {
            if (validating) {
                throw new IllegalArgumentException(woyFailed(weekOfYear));
            } else {
                return null;
            }
        }

        return result;

    }

    private static PlainDate ofYearDay(
        int year,
        int dayOfYear,
        Weekday weekday
    ) {

        if (dayOfYear < 1) {
            throw new IllegalArgumentException(
                "Day of year out of range: " + dayOfYear);
        } else if (dayOfYear <= 31) {
            return PlainDate.of(year, 1, dayOfYear);
        }

        int[] table = (
            GregorianMath.isLeapYear(year)
                ? DAY_OF_LEAP_YEAR_PER_MONTH
                : DAY_OF_YEAR_PER_MONTH);

        for (int i = (dayOfYear > table[6] ? 7 : 1); i < 12; i++) {
            if (dayOfYear <= table[i]) {
                int dom = dayOfYear - table[i - 1];
                return PlainDate.create(year, i + 1, dom, weekday, false);
            }
        }

        throw new IllegalArgumentException(
            "Day of year out of range: " + dayOfYear);

    }

    private static PlainDate create(
        int year,
        int month,
        int dayOfMonth,
        Weekday weekday,
        boolean validating
    ) {

        if (validating) {
            GregorianMath.checkDate(year, month, dayOfMonth);
        }

        // TODO: konfigurierbaren Cache einbauen?
        return new PlainDate(year, month, dayOfMonth, weekday);

    }

    /**
     * @serialData  Uses <a href="../../serialized-form.html#net.time4j.SPX">
     *              a dedicated serialization form</a> as proxy. The format
     *              is bit-compressed. The first byte contains in the four
     *              most significant bits the type-ID {@code 1}. The following
     *              bits 4-7 contain the month. The second byte contains
     *              at the bits 1-2 a year mark: 1 = year in the range
     *              1850-2100, 2 = four-digit-year, 3 = year number with more
     *              than four digits. The five least significant bits of second
     *              byte contain the day of month. Then the year will be written
     *              dependent on the year mark. Is the mark 1 then the year
     *              will be written as byte, if the mark is 2 then the year
     *              will be written as short else as int with four bytes.
     *
     * Schematic algorithm:
     *
     * <pre>
       int range;

       if (year &gt;= 1850 &amp;&amp; year &lt;= 2100) {
           range = 1;
       } else if (Math.abs(year) &lt; 10000) {
           range = 2;
       } else {
           range = 3;
       }

       int header = 1;
       header &lt;&lt;= 4;
       header |= month;
       out.writeByte(header);

       int header2 = range;
       header2 &lt;&lt;= 5;
       header2 |= dayOfMonth;
       out.writeByte(header2);

       if (range == 1) {
           out.writeByte(year - 1850 - 128);
       } else if (range == 2) {
           out.writeShort(year);
       } else {
           out.writeInt(year);
       }
      </pre>
     *
     * @return  replacement object in serialization graph
     */
    private Object writeReplace() {

        return new SPX(this, SPX.DATE_TYPE);

    }

    /**
     * @serialData  Blocks because a serialization proxy is required.
     * @param       in      object input stream
     * @throws      InvalidObjectException (always)
     */
    private void readObject(ObjectInputStream in)
        throws IOException {

        throw new InvalidObjectException("Serialization proxy required.");

    }

    //~ Innere Klassen ----------------------------------------------------

    private static class Merger
        implements ChronoMerger<PlainDate> {

        //~ Methoden ------------------------------------------------------

        @Override
        public String getFormatPattern(
            FormatStyle style,
            Locale locale
        ) {

            return CalendarText.patternForDate(style, locale);

        }

        @Override
        public PlainDate createFrom(
            TimeSource<?> clock,
            AttributeQuery attributes
        ) {

            Timezone zone;

            if (attributes.contains(Attributes.TIMEZONE_ID)) {
                zone = Timezone.of(attributes.get(Attributes.TIMEZONE_ID));
            } else if (attributes.get(Attributes.LENIENCY, Leniency.SMART).isLax()) {
                zone = Timezone.ofSystem();
            } else {
                return null;
            }

            final UnixTime ut = clock.currentTime();
            return PlainDate.from(ut, zone.getOffset(ut));

        }

        @Override
        public PlainDate createFrom(
            ChronoEntity<?> entity,
            AttributeQuery attributes,
            boolean lenient,
            boolean preparsing
        ) {

            if (entity.contains(CALENDAR_DATE)) {
                return entity.get(CALENDAR_DATE);
            }

            int year = entity.getInt(YEAR);

            if (year != Integer.MIN_VALUE) {
                int month = entity.getInt(MONTH_AS_NUMBER);

                if ((month == Integer.MIN_VALUE) && entity.contains(MONTH_OF_YEAR)) {
                    month = entity.get(MONTH_OF_YEAR).getValue();
                }

                if (month != Integer.MIN_VALUE) {
                    int dom = entity.getInt(DAY_OF_MONTH);

                    if (dom != Integer.MIN_VALUE) {
                        if (lenient) {
                            PlainDate date = PlainDate.of(year, 1, 1);
                            date = date.with(MONTH_AS_NUMBER.setLenient(Integer.valueOf(month)));
                            return date.with(DAY_OF_MONTH.setLenient(Integer.valueOf(dom)));
                        } else if ( // Standardszenario
                            validateYear(entity, year)
                            && validateMonth(entity, month)
                            && validateDayOfMonth(entity, year, month, dom)
                        ) {
                            return PlainDate.create(year, month, dom, null, false);
                        } else {
                            return null;
                        }
                    }
                }

                int doy = entity.getInt(DAY_OF_YEAR);

                if (doy != Integer.MIN_VALUE) {
                    if (lenient) {
                        PlainDate date = PlainDate.of(year, 1);
                        return date.with(DAY_OF_YEAR.setLenient(Integer.valueOf(doy)));
                    } else if ( // Ordinaldatum
                        validateYear(entity, year)
                        && validateDayOfYear(entity, year, doy)
                    ) {
                        return PlainDate.of(year, doy);
                    } else {
                        return null;
                    }
                }

                int doq = entity.getInt(DAY_OF_QUARTER);

                if (
                    (doq != Integer.MIN_VALUE)
                    && entity.contains(QUARTER_OF_YEAR)
                ) {
                    Quarter q = entity.get(QUARTER_OF_YEAR);
                    boolean leapYear = GregorianMath.isLeapYear(year);
                    doy = doq + (leapYear ? 91 : 90);

                    if (q == Quarter.Q1) {
                        doy = doq;
                    } else if (q == Quarter.Q3) {
                        doy += 91;
                    } else if (q == Quarter.Q4) {
                        doy += (91 + 92);
                    }

                    if (lenient) {
                        PlainDate date = PlainDate.of(year, 1);
                        return date.with(DAY_OF_YEAR.setLenient(Integer.valueOf(doy)));
                    } else if ( // Quartalsdatum
                        validateYear(entity, year)
                        && validateDayOfQuarter(entity, leapYear, q, doq)
                    ) {
                        return PlainDate.of(year, doy);
                    } else {
                        return null;
                    }
                }
            }

            int yearOfWeekdate = entity.getInt(YEAR_OF_WEEKDATE);

            if (
                (yearOfWeekdate != Integer.MIN_VALUE)
                && entity.contains(Weekmodel.ISO.weekOfYear())
            ) {
                int weekOfYear = entity.get(Weekmodel.ISO.weekOfYear()).intValue();
                Weekday dayOfWeek;

                if (entity.contains(DAY_OF_WEEK)) {
                    dayOfWeek = entity.get(DAY_OF_WEEK);
                } else if (entity.contains(Weekmodel.ISO.localDayOfWeek())) {
                    dayOfWeek = entity.get(Weekmodel.ISO.localDayOfWeek());
                } else {
                    return null;
                }

                // Wochendatum validieren und erzeugen
                if (
                    (yearOfWeekdate < GregorianMath.MIN_YEAR)
                    || (yearOfWeekdate > GregorianMath.MAX_YEAR)
                ) {
                    flagValidationError(
                        entity,
                        yowFailed(yearOfWeekdate));
                    return null;
                }

                PlainDate date = PlainDate.ofWeekdate(yearOfWeekdate, weekOfYear, dayOfWeek, false);

                if (date == null) {
                    flagValidationError(
                        entity,
                        woyFailed(weekOfYear));
                }

                return date;
            }

            if (entity.contains(EpochDays.MODIFIED_JULIAN_DATE)) {
                Long mjd = entity.get(EpochDays.MODIFIED_JULIAN_DATE);
                long utcDays =
                    EpochDays.UTC.transform(
                        mjd.longValue(),
                        EpochDays.MODIFIED_JULIAN_DATE);
                return TRANSFORMER.transform(utcDays);
            } else if (entity instanceof UnixTime) {
                return PlainTimestamp.axis().createFrom(entity, attributes, lenient, preparsing).getCalendarDate();
            }

            return null;

        }

        private static boolean validateYear(
            ChronoEntity<?> entity,
            int year
        ) {

            if ((year < GregorianMath.MIN_YEAR) || (year > GregorianMath.MAX_YEAR)) {
                flagValidationError(
                    entity,
                    "YEAR out of range: " + year);
                return false;
            }

            return true;

        }

        private static boolean validateMonth(
            ChronoEntity<?> entity,
            int month
        ) {

            if ((month < 1) || (month > 12)) {
                flagValidationError(
                    entity,
                    "MONTH_OF_YEAR out of range: " + month);
                return false;
            }

            return true;

        }

        private static boolean validateDayOfMonth(
            ChronoEntity<?> entity,
            int year,
            int month,
            int dom
        ) {

            if ((dom < 1) || ((dom > 28) && (dom > GregorianMath.getLengthOfMonth(year, month)))) {
                flagValidationError(
                    entity,
                    "DAY_OF_MONTH out of range: " + dom);
                return false;
            }

            return true;

        }

        private static boolean validateDayOfYear(
            ChronoEntity<?> entity,
            int year,
            int doy
        ) {

            if ((doy < 1) || ((doy > 365) && (doy > (GregorianMath.isLeapYear(year) ? 366 : 365)))) {
                flagValidationError(
                    entity,
                    "DAY_OF_YEAR out of range: " + doy);
                return false;
            }

            return true;

        }

        private static boolean validateDayOfQuarter(
            ChronoEntity<?> entity,
            boolean leapYear,
            Quarter q,
            int doq
        ) {

            int max;

            switch (q) {
                case Q1:
                    max = (leapYear ? 91 : 90);
                    break;
                case Q2:
                    max = 91;
                    break;
                default:
                    max = 92;
            }

            if (
                (doq < 1)
                || (doq > max)
            ) {
                flagValidationError(
                    entity,
                    "DAY_OF_QUARTER out of range: " + doq);
                return false;
            }

            return true;

        }

        private static void flagValidationError(
            ChronoEntity<?> entity,
            String message
        ) {

            if (entity.isValid(ValidationElement.ERROR_MESSAGE, message)) {
                entity.with(ValidationElement.ERROR_MESSAGE, message);
            }

        }

    }

    private static class Transformer
        implements CalendarSystem<PlainDate> {

        //~ Statische Felder/Initialisierungen ----------------------------

        private static final long MIN_LONG = -365243219892L; // transform(PlainDate.MIN)
        private static final long MAX_LONG = 365241779741L; // transform(PlainDate.MAX)

        //~ Methoden ------------------------------------------------------

        @Override
        public PlainDate transform(long utcDays) {

            if (utcDays == MIN_LONG) {
                return PlainDate.MIN;
            } else if (utcDays == MAX_LONG) {
                return PlainDate.MAX;
            }

            long mjd = EpochDays.MODIFIED_JULIAN_DATE.transform(utcDays, EpochDays.UTC);
            long packedDate = GregorianMath.toPackedDate(mjd);

            return PlainDate.create(
                GregorianMath.readYear(packedDate),
                GregorianMath.readMonth(packedDate),
                GregorianMath.readDayOfMonth(packedDate),
                Weekmodel.getDayOfWeek(utcDays),
                false);

        }

        @Override
        public long transform(PlainDate date) {

            return EpochDays.UTC.transform(
                GregorianMath.toMJD(date),
                EpochDays.MODIFIED_JULIAN_DATE
            );

        }

        @Override
        public long getMinimumSinceUTC() {

            return MIN_LONG;

        }

        @Override
        public long getMaximumSinceUTC() {

            return MAX_LONG;

        }

        @Override
        public List<CalendarEra> getEras() {

            return Collections.emptyList();

        }

    }

    private static class DateElementRule
        implements ElementRule<PlainDate, PlainDate> {

        //~ Methoden ------------------------------------------------------

        @Override
        public PlainDate getValue(PlainDate context) {

            return context;

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            PlainDate value,
            boolean lenient
        ) {

            if (value == null) {
                throw new IllegalArgumentException("Missing date value.");
            }

            return value;

        }

        @Override
        public boolean isValid(
            PlainDate context,
            PlainDate value
        ) {

            return (value != null);

        }

        @Override
        public PlainDate getMinimum(PlainDate context) {

            return PlainDate.MIN;

        }

        @Override
        public PlainDate getMaximum(PlainDate context) {

            return PlainDate.MAX;

        }

        @Override
        public ChronoElement<?> getChildAtFloor(PlainDate context) {

            return null;

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(PlainDate context) {

            return null;

        }

    }

    private static class IntegerElementRule
        implements IntElementRule<PlainDate> {

        //~ Instanzvariablen ----------------------------------------------

        private final ChronoElement<?> ref;
        private final String name;
        private final int index;

        //~ Konstruktoren -------------------------------------------------

        IntegerElementRule(ChronoElement<Integer> element) {
            this(((IntegerDateElement) element).getRuleIndex(), element);

        }

        IntegerElementRule(
            int index,
            ChronoElement<?> ref
        ) {
            super();

            this.ref = ref;
            this.name = ref.name();
            this.index = index;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public Integer getValue(PlainDate context) {

            return Integer.valueOf(this.getInt(context));

        }

        @Override
        public int getInt(PlainDate context) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return context.year;
                case IntegerDateElement.MONTH:
                    return context.month;
                case IntegerDateElement.DAY_OF_MONTH:
                    return context.dayOfMonth;
                case IntegerDateElement.DAY_OF_YEAR:
                    return context.getDayOfYear();
                case IntegerDateElement.DAY_OF_QUARTER:
                    return context.getDayOfQuarter();
                case WIM_INDEX:
                    return ((context.dayOfMonth - 1) / 7) + 1;
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            Integer value,
            boolean lenient
        ) {

            if (value == null) {
                throw new IllegalArgumentException("Missing element value.");
            }

            return this.withValue(context, value.intValue(), lenient);

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            int value,
            boolean lenient
        ) {

            if (lenient) { // nur auf numerischen Elementen definiert
                IsoDateUnit unit = ENGINE.getBaseUnit(this.ref);
                int amount = MathUtils.safeSubtract(value, this.getInt(context));
                return context.plus(amount, unit);
            }

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return context.withYear(value);
                case IntegerDateElement.MONTH:
                    return context.withMonth(value);
                case IntegerDateElement.DAY_OF_MONTH:
                    return context.withDayOfMonth(value);
                case IntegerDateElement.DAY_OF_YEAR:
                    return context.withDayOfYear(value);
                case IntegerDateElement.DAY_OF_QUARTER:
                    if ((value >= 1) && (value <= getMaximumOfQuarterDay(context))) {
                        return context.plus((value - context.getDayOfQuarter()), CalendarUnit.DAYS);
                    } else {
                        throw new IllegalArgumentException("Out of range: " + value);
                    }
                case WIM_INDEX:
                    if ((value >= 1) && (value <= this.getMaximumOfWIM(context))) {
                        int old = ((context.dayOfMonth - 1) / 7) + 1;
                        return context.plus(value - old, CalendarUnit.WEEKS);
                    } else {
                        throw new IllegalArgumentException("Out of range: " + value);
                    }
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

        @Override
        public boolean isValid(
            PlainDate context,
            Integer value
        ) {

            return ((value != null) && this.isValid(context, value.intValue()));

        }

        @Override
        public boolean isValid(
            PlainDate context,
            int value
        ) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return ((value >= GregorianMath.MIN_YEAR) && (value <= GregorianMath.MAX_YEAR));
                case IntegerDateElement.MONTH:
                    return ((value >= 1) && (value <= 12));
                case IntegerDateElement.DAY_OF_MONTH:
                    return ((value >= 1) && (value <= GregorianMath.getLengthOfMonth(context.year, context.month)));
                case IntegerDateElement.DAY_OF_YEAR:
                    return ((value >= 1) && (value <= (GregorianMath.isLeapYear(context.year) ? 366 : 365)));
                case IntegerDateElement.DAY_OF_QUARTER:
                    return ((value >= 1) && (value <= getMaximumOfQuarterDay(context)));
                case WIM_INDEX:
                    return ((value >= 1) && (value <= this.getMaximumOfWIM(context)));
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

        @Override
        public Integer getMinimum(PlainDate context) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return MIN_YEAR;
                case IntegerDateElement.MONTH:
                case IntegerDateElement.DAY_OF_MONTH:
                case IntegerDateElement.DAY_OF_YEAR:
                case IntegerDateElement.DAY_OF_QUARTER:
                case WIM_INDEX:
                    return VALUE_1;
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

        @Override
        public Integer getMaximum(PlainDate context) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return MAX_YEAR;
                case IntegerDateElement.MONTH:
                    return VALUE_12;
                case IntegerDateElement.DAY_OF_MONTH:
                    return Integer.valueOf(GregorianMath.getLengthOfMonth(context.year, context.month));
                case IntegerDateElement.DAY_OF_YEAR:
                    return (GregorianMath.isLeapYear(context.year) ? LEAP_YEAR_LEN : STD_YEAR_LEN);
                case IntegerDateElement.DAY_OF_QUARTER:
                    return Integer.valueOf(getMaximumOfQuarterDay(context));
                case WIM_INDEX:
                    return Integer.valueOf(this.getMaximumOfWIM(context));
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

        @Override
        public ChronoElement<?> getChildAtFloor(PlainDate context) {

            return this.getChild();

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(PlainDate context) {

            return this.getChild();

        }

        private ChronoElement<?> getChild() {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return MONTH_AS_NUMBER;
                case IntegerDateElement.MONTH:
                    return DAY_OF_MONTH;
                case IntegerDateElement.DAY_OF_MONTH:
                case IntegerDateElement.DAY_OF_YEAR:
                case IntegerDateElement.DAY_OF_QUARTER:
                case WIM_INDEX:
                    return null;
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

        private static int getMaximumOfQuarterDay(PlainDate context) {

            int q = ((context.month - 1) / 3) + 1;

            if (q == 1) {
                return (GregorianMath.isLeapYear(context.year) ? 91 : 90);
            } else if (q == 2) {
                return 91;
            } else {
                return 92;
            }

        }

        private int getMaximumOfWIM(PlainDate context) {

            int maxday = GregorianMath.getLengthOfMonth(context.year, context.month);
            int d = context.dayOfMonth;
            int n = 0;

            while (d + (n + 1) * 7 <= maxday) {
                n++;
            }

            return ((d + n * 7 - 1) / 7) + 1;

        }

    }

    private static class EnumElementRule<V extends Enum<V>>
        implements ElementRule<PlainDate, V> {

        //~ Instanzvariablen ----------------------------------------------

        private final String name;
        private final Class<V> type;
        private final V min;
        private final V max;
        private final int index;

        //~ Konstruktoren -------------------------------------------------

        EnumElementRule(
            String name,
            Class<V> type,
            V min,
            V max,
            int index
        ) {
            super();

            this.name = name;
            this.type = type;
            this.min = min;
            this.max = max;
            this.index = index;

        }

        //~ Methoden ------------------------------------------------------

        static <V extends Enum<V>> EnumElementRule<V> of(ChronoElement<V> element) {

            return new EnumElementRule<>(
                element.name(),
                element.getType(),
                element.getDefaultMinimum(),
                element.getDefaultMaximum(),
                ((EnumElement<?>) element).getIndex()
            );

        }

        @Override
        public V getValue(PlainDate context) {

            Object ret;

            switch (this.index) {
                case EnumElement.MONTH:
                    ret = Month.valueOf(context.month);
                    break;
                case EnumElement.DAY_OF_WEEK:
                    ret = context.getDayOfWeek();
                    break;
                case EnumElement.QUARTER_OF_YEAR:
                    ret = Quarter.valueOf(((context.month - 1) / 3) + 1);
                    break;
                default:
                    throw new UnsupportedOperationException(this.name);
            }

            return this.type.cast(ret);

        }

        @Override
        public V getMinimum(PlainDate context) {

            return this.min;

        }

        @Override
        public V getMaximum(PlainDate context) {

            if (
                (this.index == EnumElement.DAY_OF_WEEK)
                && (context.year == GregorianMath.MAX_YEAR)
                && (context.month == 12)
                && (context.dayOfMonth >= 27)
            ) {
                return this.type.cast(Weekday.FRIDAY);
            }

            return this.max;

        }

        @Override
        public boolean isValid(
            PlainDate context,
            V value
        ) {

            if (value == null) {
                return false;
            } else if ((this.index == EnumElement.DAY_OF_WEEK) && (context.year == GregorianMath.MAX_YEAR)) {
                try {
                    this.withValue(context, value, false);
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            }

            return true;

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            V value,
            boolean lenient
        ) {

            if (value == null) {
                throw new IllegalArgumentException("Missing element value.");
            }

            switch (this.index) {
                case EnumElement.MONTH:
                    return context.withMonth(Month.class.cast(value).getValue());
                case EnumElement.DAY_OF_WEEK:
                    return context.withDayOfWeek(Weekday.class.cast(value));
                case EnumElement.QUARTER_OF_YEAR:
                    int q1 = ((context.month - 1) / 3) + 1;
                    int q2 = Quarter.class.cast(value).getValue();
                    return context.plus((q2 - q1), CalendarUnit.QUARTERS);
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

        @Override
        public ChronoElement<?> getChildAtFloor(PlainDate context) {

            return this.getChild();

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(PlainDate context) {

            return this.getChild();

        }

        private ChronoElement<?> getChild() {

            switch (this.index) {
                case EnumElement.MONTH:
                    return DAY_OF_MONTH;
                case EnumElement.DAY_OF_WEEK:
                    return null;
                case EnumElement.QUARTER_OF_YEAR:
                    return DAY_OF_QUARTER;
                default:
                    throw new UnsupportedOperationException(this.name);
            }

        }

    }

}
