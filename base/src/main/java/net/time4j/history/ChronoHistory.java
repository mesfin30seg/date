/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2021 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (ChronoHistory.java) is part of project Time4J.
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

package net.time4j.history;

import net.time4j.CalendarUnit;
import net.time4j.PlainDate;
import net.time4j.base.GregorianMath;
import net.time4j.engine.AttributeKey;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.EpochDays;
import net.time4j.engine.FormattableElement;
import net.time4j.engine.VariantSource;
import net.time4j.format.Attributes;
import net.time4j.format.TextElement;
import net.time4j.format.expert.Iso8601Format;
import net.time4j.format.internal.FormatUtils;
import net.time4j.history.internal.HistoricVariant;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * <p>Represents the chronological history of calendar reforms in a given region. </p>
 *
 * <p>All non-proleptic history objects are limited to the range BC 45 until the year AD 9999. </p>
 *
 * @author  Meno Hochschild
 * @since   3.0
 */
/*[deutsch]
 * <p>Repr&auml;sentiert die Geschichte der Kalenderreformen in einer gegebenen Region. </p>
 *
 * <p>Alle nicht-proleptischen {@code ChronoHistory}-Objekte sind auf den Jahresbereich
 * BC 45 bis AD 9999 beschr&auml;nkt. </p>
 *
 * @author  Meno Hochschild
 * @since   3.0
 */
public final class ChronoHistory
    implements VariantSource, Serializable {

    //~ Statische Felder/Initialisierungen --------------------------------

    /**
     * Format attribute controlling the type of historic year used in parsing or formatting.
     *
     * @since   3.18/4.14
     */
    /*[deutsch]
     * Formatattribut, das den Typ des beim Formatieren oder Interpretieren zu verwendenden historischen
     * Jahres festlegt.
     *
     * @since   3.18/4.14
     */
    public static final AttributeKey<YearDefinition> YEAR_DEFINITION =
        Attributes.createKey("YEAR_DEFINITION", YearDefinition.class);

    /**
     * <p>Describes no real historic event but just the proleptic gregorian calendar which is assumed
     * to be in power all times. </p>
     *
     * <p>This constant rather serves for academic purposes. Users will normally use {@code PlainDate}
     * without an era. </p>
     */
    /*[deutsch]
     * <p>Beschreibt kein wirkliches historisches Ereignis, sondern einfach nur den proleptisch gregorianischen
     * Kalender, der als f&uuml;r alle Zeiten g&uuml;ltig angesehen wird. </p>
     *
     * <p>Diese Konstante dient eher akademischen &Uuml;bungen. Anwender werden normalerweise direkt die Klasse
     * {@code PlainDate} ohne das &Auml;ra-Konzept nutzen. </p>
     */
    public static final ChronoHistory PROLEPTIC_GREGORIAN;

    /**
     * <p>Describes no real historic event but just the proleptic julian calendar which is assumed
     * to be in power all times (with the technical constraint {@code BC 999979466 - AD 999979465}). </p>
     *
     * <p>This constant rather serves for academic purposes because the julian calendar is now nowhere in power
     * and has not existed before the calendar reform of Julius Caesar. </p>
     */
    /*[deutsch]
     * <p>Beschreibt kein wirkliches historisches Ereignis, sondern einfach nur den proleptisch julianischen
     * Kalender, der als f&uuml;r alle Zeiten g&uuml;ltig angesehen wird (mit der technischen Beschr&auml;nkung
     * {@code BC 999979466 - AD 999979465}). </p>
     *
     * <p>Diese Konstante dient eher akademischen &Uuml;bungen, weil der julianische Kalender aktuell nirgendwo
     * in der Welt in Kraft ist und vor der Kalenderreform von Julius Caesar nicht existierte. </p>
     */
    public static final ChronoHistory PROLEPTIC_JULIAN;

    /**
     * <p>Describes no real historic event but just the proleptic byzantine calendar which is assumed
     * to be in power all times from the creation of the world until the byzantine year 999984973. </p>
     *
     * <p>This constant rather serves for academic purposes because the byzantine calendar was in latest use
     * in Russia before 1700. </p>
     *
     * @see     HistoricEra#BYZANTINE
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Beschreibt kein wirkliches historisches Ereignis, sondern einfach nur den proleptisch byzantinischen
     * Kalender, der f&uuml;r alle Zeiten ab der Erschaffung der Welt bis zum byzantinischen Jahr 999984973
     * als g&uuml;ltig angesehen wird. </p>
     *
     * <p>Diese Konstante dient eher akademischen &Uuml;bungen, weil der byzantinische Kalender zuletzt in
     * Ru&szlig;land vor 1700 verwendet wurde. </p>
     *
     * @see     HistoricEra#BYZANTINE
     * @since   3.14/4.11
     */
    public static final ChronoHistory PROLEPTIC_BYZANTINE;

    static final int BYZANTINE_YMAX = 999_984_973;
    static final int JULIAN_YMAX = 999_979_465;

    private static final long EARLIEST_CUTOVER;
    private static final ChronoHistory INTRODUCTION_BY_POPE_GREGOR;
    private static final ChronoHistory SWEDEN;

    private static final Map<String, ChronoHistory> LOOKUP;

    static {
        PROLEPTIC_GREGORIAN =
            new ChronoHistory(
                HistoricVariant.PROLEPTIC_GREGORIAN,
                Collections.singletonList(
                    new CutOverEvent(Long.MIN_VALUE, CalendarAlgorithm.GREGORIAN, CalendarAlgorithm.GREGORIAN)));

        PROLEPTIC_JULIAN =
            new ChronoHistory(
                HistoricVariant.PROLEPTIC_JULIAN,
                Collections.singletonList(
                    new CutOverEvent(Long.MIN_VALUE, CalendarAlgorithm.JULIAN, CalendarAlgorithm.JULIAN)));

        PROLEPTIC_BYZANTINE =
            new ChronoHistory(
                HistoricVariant.PROLEPTIC_BYZANTINE,
                Collections.singletonList(
                    new CutOverEvent(Long.MIN_VALUE, CalendarAlgorithm.JULIAN, CalendarAlgorithm.JULIAN)),
                null,
                new NewYearStrategy(NewYearRule.BEGIN_OF_SEPTEMBER, Integer.MAX_VALUE),
                EraPreference.byzantineUntil(PlainDate.axis().getMaximum()));

        EARLIEST_CUTOVER = PlainDate.of(1582, 10, 15).get(EpochDays.MODIFIED_JULIAN_DATE);
        INTRODUCTION_BY_POPE_GREGOR = ChronoHistory.ofGregorianReform(EARLIEST_CUTOVER);

        List<CutOverEvent> events = new ArrayList<>();
        events.add(new CutOverEvent(-57959, CalendarAlgorithm.JULIAN, CalendarAlgorithm.SWEDISH)); // 1700-03-01
        events.add(new CutOverEvent(-53575, CalendarAlgorithm.SWEDISH, CalendarAlgorithm.JULIAN)); // 1712-03-01
        events.add(new CutOverEvent(-38611, CalendarAlgorithm.JULIAN, CalendarAlgorithm.GREGORIAN)); // 1753-03-01
        SWEDEN = new ChronoHistory(HistoricVariant.SWEDEN, Collections.unmodifiableList(events));

        Map<String, ChronoHistory> tmp = new HashMap<>();
        PlainDate ad988 = ChronoHistory.PROLEPTIC_JULIAN.convert(HistoricDate.of(HistoricEra.AD, 988, 3, 1));
        PlainDate ad1383 = ChronoHistory.PROLEPTIC_JULIAN.convert(HistoricDate.of(HistoricEra.AD, 1382, 12, 24));
        PlainDate ad1422 = ChronoHistory.PROLEPTIC_JULIAN.convert(HistoricDate.of(HistoricEra.AD, 1421, 12, 24));
        PlainDate ad1700 = ChronoHistory.PROLEPTIC_JULIAN.convert(HistoricDate.of(HistoricEra.AD, 1699, 12, 31));
        tmp.put(
            "ES", // source: http://www.newadvent.org/cathen/03738a.htm#beginning
            ChronoHistory.ofFirstGregorianReform()
                .with(
                    NewYearRule.BEGIN_OF_JANUARY.until(1383)
                        .and(NewYearRule.CHRISTMAS_STYLE.until(1556)))
                .with(EraPreference.hispanicUntil(ad1383)));
        tmp.put(
            "PT", // source: http://www.newadvent.org/cathen/03738a.htm#beginning
            ChronoHistory.ofFirstGregorianReform()
                .with(
                    NewYearRule.BEGIN_OF_JANUARY.until(1422)
                        .and(NewYearRule.CHRISTMAS_STYLE.until(1556)))
                .with(EraPreference.hispanicUntil(ad1422)));
        tmp.put(
            "FR",
            ChronoHistory.ofGregorianReform(PlainDate.of(1582, 12, 20))
                .with(NewYearRule.EASTER_STYLE.until(1567))); // edict of Roussillon in 1564 took effect 3 years later
        tmp.put(
            "DE", // Holy Roman Empire
            ChronoHistory.ofFirstGregorianReform()
                .with(NewYearRule.CHRISTMAS_STYLE.until(1544)));
        tmp.put(
            "DE-BAYERN",
            ChronoHistory.ofGregorianReform(PlainDate.of(1583, 10, 16))
                .with(NewYearRule.CHRISTMAS_STYLE.until(1544)));
        tmp.put(
            "DE-PREUSSEN",
            ChronoHistory.ofGregorianReform(PlainDate.of(1610, 9, 2))
                .with(NewYearRule.CHRISTMAS_STYLE.until(1559)));
        tmp.put(
            "DE-PROTESTANT",
            ChronoHistory.ofGregorianReform(PlainDate.of(1700, 3, 1))
                .with(NewYearRule.CHRISTMAS_STYLE.until(1559)));
        tmp.put(
            "NL",
            ChronoHistory.ofGregorianReform(PlainDate.of(1583, 1, 1)));
        tmp.put(
            "AT",
            ChronoHistory.ofGregorianReform(PlainDate.of(1584, 1, 17)));
        tmp.put(
            "CH",
            ChronoHistory.ofGregorianReform(PlainDate.of(1584, 1, 22)));
        tmp.put(
            "HU",
            ChronoHistory.ofGregorianReform(PlainDate.of(1587, 11, 1)));
        tmp.put(
            "DK",
            ChronoHistory.ofGregorianReform(PlainDate.of(1700, 3, 1))
                .with(NewYearRule.MARIA_ANUNCIATA.until(1623)));
        tmp.put(
            "NO",
            ChronoHistory.ofGregorianReform(PlainDate.of(1700, 3, 1))
                .with(NewYearRule.MARIA_ANUNCIATA.until(1623)));
        tmp.put(
            "IT",
            ChronoHistory.ofFirstGregorianReform()
                .with(NewYearRule.CHRISTMAS_STYLE.until(1583)));
        tmp.put(
            "IT-FLORENCE",
            ChronoHistory.ofFirstGregorianReform()
                .with(NewYearRule.MARIA_ANUNCIATA.until(1749)));
        tmp.put(
            "IT-PISA",
            ChronoHistory.ofFirstGregorianReform()
                .with(NewYearRule.CALCULUS_PISANUS.until(1749)));
        tmp.put(
            "IT-VENICE",
            ChronoHistory.ofFirstGregorianReform()
                .with(NewYearRule.BEGIN_OF_MARCH.until(1798)));
        tmp.put(
            "GB", // source: http://www.newadvent.org/cathen/03738a.htm#beginning
            ChronoHistory.ofGregorianReform(PlainDate.of(1752, 9, 14))
                .with(
                    NewYearRule.CHRISTMAS_STYLE.until(1087) // we don't exactly know when it started
                        .and(NewYearRule.BEGIN_OF_JANUARY.until(1155))
                        .and(NewYearRule.MARIA_ANUNCIATA.until(1752)))); // http://www.ortelius.de/kalender/jul_en.php
        tmp.put(
            "GB-SCT", // Scotland (we assume as only difference to England the last new-year-rule)
            ChronoHistory.ofGregorianReform(PlainDate.of(1752, 9, 14))
                .with(
                    NewYearRule.CHRISTMAS_STYLE.until(1087)
                        .and(NewYearRule.BEGIN_OF_JANUARY.until(1155))
                        .and(NewYearRule.MARIA_ANUNCIATA.until(1600))));
        tmp.put(
            "RU",
            ChronoHistory.ofGregorianReform(PlainDate.of(1918, 2, 14))
                .with(
                    NewYearRule.BEGIN_OF_JANUARY.until(988)
                        .and(NewYearRule.BEGIN_OF_MARCH.until(1493))
                        .and(NewYearRule.BEGIN_OF_SEPTEMBER.until(1700)))
                .with(EraPreference.byzantineBetween(ad988, ad1700)));
        tmp.put("SE", SWEDEN);
        LOOKUP = Collections.unmodifiableMap(tmp);
    }

    // Dient der Serialisierungsunterstützung.
    private static final long serialVersionUID = 4100690610730913643L;

    //~ Instanzvariablen --------------------------------------------------

    private transient final HistoricVariant variant;
    private transient final List<CutOverEvent> events;
    private transient final AncientJulianLeapYears ajly;
    private transient final NewYearStrategy nys;
    private transient final EraPreference eraPreference;

    private transient final ChronoElement<HistoricDate> dateElement;
    private transient final ChronoElement<HistoricEra> eraElement;
    private transient final TextElement<Integer> yearOfEraElement;
    private transient final ChronoElement<Integer> yearAfterElement;
    private transient final ChronoElement<Integer> yearBeforeElement;
    private transient final TextElement<Integer> monthElement;
    private transient final TextElement<Integer> dayOfMonthElement;
    private transient final TextElement<Integer> dayOfYearElement;
    private transient final ChronoElement<Integer> centuryElement;
    private transient final Set<ChronoElement<?>> elements;

    //~ Konstruktoren -----------------------------------------------------

    private ChronoHistory(
        HistoricVariant variant,
        List<CutOverEvent> events
    ) {
        this(variant, events, null, null, EraPreference.DEFAULT);

    }

    private ChronoHistory(
        HistoricVariant variant,
        List<CutOverEvent> events,
        AncientJulianLeapYears ajly,
        NewYearStrategy nys,
        EraPreference eraPreference
    ) {
        super();

        if (events.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one cutover event must be present in chronological history.");
        } else if (variant == null) {
            throw new NullPointerException("Missing historic variant.");
        } else if (eraPreference == null) {
            throw new NullPointerException("Missing era preference.");
        }

        this.variant = variant;
        this.events = events;
        this.ajly = ajly;
        this.nys = nys;
        this.eraPreference = eraPreference;

        this.dateElement = new HistoricDateElement(this);
        this.eraElement = new HistoricEraElement(this);

        this.yearOfEraElement = new HistoricIntegerElement(
            'y',
            1,
            GregorianMath.MAX_YEAR,
            this,
            HistoricIntegerElement.YEAR_OF_ERA_INDEX
        );
        this.yearAfterElement = new HistoricIntegerElement(
            '\u0000',
            1,
            GregorianMath.MAX_YEAR,
            this,
            HistoricIntegerElement.YEAR_AFTER_INDEX
        );
        this.yearBeforeElement = new HistoricIntegerElement(
            '\u0000',
            1,
            GregorianMath.MAX_YEAR,
            this,
            HistoricIntegerElement.YEAR_BEFORE_INDEX
        );
        this.monthElement = new HistoricIntegerElement(
            'M',
            1,
            12,
            this,
            HistoricIntegerElement.MONTH_INDEX
        );
        this.dayOfMonthElement = new HistoricIntegerElement(
            'd',
            1,
            31,
            this,
            HistoricIntegerElement.DAY_OF_MONTH_INDEX
        );
        this.dayOfYearElement = new HistoricIntegerElement(
            'D',
            1,
            365,
            this,
            HistoricIntegerElement.DAY_OF_YEAR_INDEX
        );
        this.centuryElement = new HistoricIntegerElement(
            '\u0000',
            1,
            ((GregorianMath.MAX_YEAR - 1) / 100) + 1,
            this,
            HistoricIntegerElement.CENTURY_INDEX
        );

        Set<ChronoElement<?>> set = new HashSet<>();
        set.add(this.dateElement);
        set.add(this.eraElement);
        set.add(this.yearOfEraElement);
        set.add(this.yearAfterElement);
        set.add(this.yearBeforeElement);
        set.add(this.monthElement);
        set.add(this.dayOfMonthElement);
        set.add(this.dayOfYearElement);
        set.add(this.centuryElement);
        this.elements = Collections.unmodifiableSet(set);

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Describes the original switch from julian to gregorian calendar introduced
     * by pope Gregor on 1582-10-15. </p>
     *
     * @return  chronological history with cutover to gregorian calendar on 1582-10-15
     * @see     #ofGregorianReform(PlainDate)
     * @since   3.0
     */
    /*[deutsch]
     * <p>Beschreibt die Umstellung vom julianischen zum gregorianischen Kalender wie
     * von Papst Gregor zu 1582-10-15 eingef&uuml;hrt. </p>
     *
     * @return  chronological history with cutover to gregorian calendar on 1582-10-15
     * @see     #ofGregorianReform(PlainDate)
     * @since   3.0
     */
    public static ChronoHistory ofFirstGregorianReform() {

        return INTRODUCTION_BY_POPE_GREGOR;

    }

    /**
     * <p>Describes a single switch from julian to gregorian calendar at given date. </p>
     *
     * <p>Since version 3.1 there are two special cases to consider. If the given date is the maximum/minimum
     * of the date axis then this method will return the proleptic julian/gregorian {@code ChronoHistory}. </p>
     *
     * @param   start   calendar date when the gregorian calendar was introduced
     * @return  new chronological history with only one cutover from julian to gregorian calendar
     * @throws  IllegalArgumentException if given date is before first introduction of gregorian calendar on 1582-10-15
     * @see     #ofFirstGregorianReform()
     * @since   3.0
     */
    /*[deutsch]
     * <p>Beschreibt die Umstellung vom julianischen zum gregorianischen Kalender am angegebenen Datum. </p>
     *
     * <p>Seit Version 3.1 gibt es zwei Sonderf&auml;lle. Wenn das angegebene Datum das Maximum/Minimum der
     * Datumsachse darstellt, dann wird diese Methode die proleptisch julianische/gregorianische Variante
     * von {@code ChronoHistory} zur&uuml;ckgeben. </p>
     *
     * @param   start   calendar date when the gregorian calendar was introduced
     * @return  new chronological history with only one cutover from julian to gregorian calendar
     * @throws  IllegalArgumentException if given date is before first introduction of gregorian calendar on 1582-10-15
     * @see     #ofFirstGregorianReform()
     * @since   3.0
     */
    public static ChronoHistory ofGregorianReform(PlainDate start) {

        if (start.equals(PlainDate.axis().getMaximum())) {
            return ChronoHistory.PROLEPTIC_JULIAN;
        } else if (start.equals(PlainDate.axis().getMinimum())) {
            return ChronoHistory.PROLEPTIC_GREGORIAN;
        }

        long mjd = start.get(EpochDays.MODIFIED_JULIAN_DATE);
        check(mjd);

        if (mjd == EARLIEST_CUTOVER) {
            return INTRODUCTION_BY_POPE_GREGOR;
        }

        return ChronoHistory.ofGregorianReform(mjd);

    }

    /**
     * <p>Determines the (usually approximate) history of gregorian calendar reforms for given locale. </p>
     *
     * <p>The actual implementation just falls back to the introduction of gregorian calendar by
     * pope Gregor if a locale is not explicitly mentioned in following table. Later releases of Time4J
     * might refine the implementation for most European countries. <strong>In any case, this method does not
     * reflect the absolute historic truth.</strong> Various regions in many countries used different rules than
     * the rest. And sometimes historic sources and literature are simply unreliable so this method is just an
     * approach on base of best efforts. In case of doubt, users should prefer to model the concrete history
     * themselves as needed. For any cutover date not supported by this method, users can instead call
     * {@code ofGregorianReform(PlainDate)}. </p>
     *
     * <p>This method does not use the language part of given locale but the country part (ISO-3166),
     * in case of Scotland also the variant part (SCT), in case of Germany the variant part (PREUSSEN or
     * PROTESTANT etc.). In order to create an appropriate locale, use a constructor like
     * {@code new Locale(&quot;en&quot;, &quot;GB&quot;, &quot;SCT&quot;)}. </p>
     *
     * <div style="margin-top:5px;">
     * <table border="1">
     * <caption>Summary</caption>
     * <tr>
     *  <th>Region</th><th>Locale</th><th>Gregorian cutover</th><th>New Year (using estimates)</th><th>Era preference</th>
     * </tr>
     * <tr>
     *  <td>Italy</td><td>IT</td><td>1582-10-15</td><td>CHRISTMAS_STYLE.until(1583)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Florence</td><td>IT-FLORENCE</td><td>1582-10-15</td><td>MARIA_ANUNCIATA.until(1749)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Pisa</td><td>IT-PISA</td><td>1582-10-15</td><td>CALCULUS_PISANUS.until(1749)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Republic of Venice</td><td>IT-VENICE</td><td>1582-10-15</td><td>BEGIN_OF_MARCH.until(1798)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Spain</td><td>ES</td><td>1582-10-15</td>
     *  <td>BEGIN_OF_JANUARY.until(1383)<br>.and(CHRISTMAS_STYLE.until(1556))</td>
     *  <td>HISPANIC (until 1383)</td>
     * </tr>
     * <tr>
     *  <td>Portugal</td><td>PT</td><td>1582-10-15</td>
     *  <td>BEGIN_OF_JANUARY.until(1422)<br>.and(CHRISTMAS_STYLE.until(1556))</td>
     *  <td>HISPANIC (until 1422)</td>
     * </tr>
     * <tr>
     *  <td>Poland</td><td>PL</td><td>1582-10-15</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>France</td><td>FR</td><td>1582-12-20</td><td>EASTER_STYLE.until(1567)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Holland</td><td>NL</td><td>1583-01-01</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Holy Roman Empire <sup>1)</sup></td><td>DE</td><td>1582-10-15</td><td>CHRISTMAS_STYLE.until(1544)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Bavaria</td><td>DE-BAYERN</td><td>1583-10-16</td><td>CHRISTMAS_STYLE.until(1544)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Prussia</td><td>DE-PREUSSEN</td><td>1610-09-02</td><td>CHRISTMAS_STYLE.until(1559)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Germany (protestant)</td><td>DE-PROTESTANT</td><td>1700-03-01</td><td>CHRISTMAS_STYLE.until(1559)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Austria</td><td>AT</td><td>1584-01-17</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Switzerland</td><td>CH</td><td>1584-01-22</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Hungaria</td><td>HU</td><td>1587-11-01</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Danmark</td><td>DK</td><td>1700-03-01</td><td>MARIA_ANUNCIATA.until(1623)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Norway</td><td>NO</td><td>1700-03-01</td><td>MARIA_ANUNCIATA.until(1623)</td><td></td>
     * </tr>
     * <tr>
     *  <td>England</td><td>GB</td><td>1752-09-14</td>
     *  <td>CHRISTMAS_STYLE.until(1087)<br>.and(BEGIN_OF_JANUARY.until(1155))<br>.and(MARIA_ANUNCIATA.until(1752))</td><td></td>
     * </tr>
     * <tr>
     *  <td>Scotland</td><td>GB-SCT</td><td>1752-09-14</td>
     *  <td>CHRISTMAS_STYLE.until(1087)<br>.and(BEGIN_OF_JANUARY.until(1155))<br>.and(MARIA_ANUNCIATA.until(1600))</td><td></td>
     * </tr>
     * <tr>
     *  <td>Sweden <sup>2)</sup></td><td>SE</td><td>1753-03-01</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Russia <sup>3)</sup></td><td>RU</td><td>1918-02-14</td>
     *  <td>BEGIN_OF_JANUARY.until(988)<br>.and(BEGIN_OF_MARCH.until(1493))<br>.and(BEGIN_OF_SEPTEMBER.until(1700))</td>
     *  <td>BYZANTINE (988-1700)</td>
     * </tr>
     * </table>
     * </div>
     *
     * <p><sup>1)</sup> Fallback for catholic regions in German countries, but most regions started Gregorian calendar later.
     * <br><sup>2)</sup> Sweden used a special calendar shifted by one day between 1700 and 1712 (supported by Time4J).
     * <br><sup>3)</sup> Special case for Byzantine year 7208: It lasted from AD-1699-09-01 until AD-1699-12-31 (Julian calendar). </p>
     *
     * @param   locale  country setting
     * @return  localized chronological history
     * @since   3.0
     * @see     #ofGregorianReform(PlainDate)
     */
    /*[deutsch]
     * <p>Ermittelt die (meist gen&auml;herte) Geschichte der gregorianischen Kalenderreformen f&uuml;r die
     * angegebene Region. </p>
     *
     * <p>Die aktuelle Implementierung f&auml;llt au&szlig;er f&uuml;r die in der folgenden Tabelle erw&auml;hnten
     * Regionen auf die erste Einf&uuml;hrung des gregorianischen Kalenders durch Papst Gregor zur&uuml;ck.
     * Sp&auml;tere Releases von Time4J k&ouml;nnen diesen Ansatz f&uuml;r die meisten europ&auml;ischen
     * L&auml;nder verfeinern. F&uuml;r jedes hier nicht unterst&uuml;tzte Umstellungsdatum k&ouml;nnen
     * Anwender stattdessen {@code ofGregorianReform(PlainDate)} nutzen. <strong>Wie auch immer, Anwender
     * sollten in Zweifelsf&auml;llen der spezifischen Modellierung der Kalendergeschichte gegen&uuml;ber dieser
     * Methode den Vorzug geben.</strong> Verschiedene Regionen in mehreren L&auml;ndern haben oft abweichende
     * Regeln verwendet. Und manchmal sind historische Quellen einfach unzuverl&auml;ssig. </p>
     *
     * <p>Diese Methode nutzt nicht den Sprach-, sondern den L&auml;nderteil des Arguments (ISO-3166),
     * im Falle Schottlands auch den Variantenteil (SCT), im Falle Deutschlands die Varianten (PREUSSEN
     * oder PROTESTANT usw.). Um eine passende {@code Locale} zu erzeugen, ist ein Konstruktor wie
     * {@code new Locale(&quot;en&quot;, &quot;GB&quot;, &quot;SCT&quot;)} zu verwenden. </p>
     *
     * <div style="margin-top:5px;">
     * <table border="1">
     * <caption>Zusammenfassung</caption>
     * <tr>
     *  <th>Region</th><th>Locale</th><th>Gregorianische Umstellung</th><th>Neujahr (Sch&auml;tzung)</th><th>Bevorzugte &Auml;ra</th>
     * </tr>
     * <tr>
     *  <td>Italien</td><td>IT</td><td>1582-10-15</td><td>CHRISTMAS_STYLE.until(1583)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Florenz</td><td>IT-FLORENCE</td><td>1582-10-15</td><td>MARIA_ANUNCIATA.until(1749)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Pisa</td><td>IT-PISA</td><td>1582-10-15</td><td>CALCULUS_PISANUS.until(1749)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Republik von Venedig</td><td>IT-VENICE</td><td>1582-10-15</td><td>BEGIN_OF_MARCH.until(1798)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Spanien</td><td>ES</td><td>1582-10-15</td>
     *  <td>BEGIN_OF_JANUARY.until(1383)<br>.and(CHRISTMAS_STYLE.until(1556))</td>
     *  <td>HISPANIC (until 1383)</td>
     * </tr>
     * <tr>
     *  <td>Portugal</td><td>PT</td><td>1582-10-15</td>
     *  <td>BEGIN_OF_JANUARY.until(1422)<br>.and(CHRISTMAS_STYLE.until(1556))</td>
     *  <td>HISPANIC (until 1422)</td>
     * </tr>
     * <tr>
     *  <td>Polen</td><td>PL</td><td>1582-10-15</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Frankreich</td><td>FR</td><td>1582-12-20</td><td>EASTER_STYLE.until(1567)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Holland</td><td>NL</td><td>1583-01-01</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Heiliges R&ouml;misches Reich <sup>1)</sup></td><td>DE</td><td>1582-10-15</td><td>CHRISTMAS_STYLE.until(1544)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Bayern</td><td>DE-BAYERN</td><td>1583-10-16</td><td>CHRISTMAS_STYLE.until(1544)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Preu&szlig;en</td><td>DE-PREUSSEN</td><td>1610-09-02</td><td>CHRISTMAS_STYLE.until(1559)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Deutschland (protestantisch)</td><td>DE-PROTESTANT</td><td>1700-03-01</td><td>CHRISTMAS_STYLE.until(1559)</td><td></td>
     * </tr>
     * <tr>
     *  <td>&Ouml;sterreich</td><td>AT</td><td>1584-01-17</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Schweiz</td><td>CH</td><td>1584-01-22</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Ungarn</td><td>HU</td><td>1587-11-01</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>D&auml;nemark</td><td>DK</td><td>1700-03-01</td><td>MARIA_ANUNCIATA.until(1623)</td><td></td>
     * </tr>
     * <tr>
     *  <td>Norwegen</td><td>NO</td><td>1700-03-01</td><td>MARIA_ANUNCIATA.until(1623)</td><td></td>
     * </tr>
     * <tr>
     *  <td>England</td><td>GB</td><td>1752-09-14</td>
     *  <td>CHRISTMAS_STYLE.until(1087)<br>.and(BEGIN_OF_JANUARY.until(1155))<br>.and(MARIA_ANUNCIATA.until(1752))</td><td></td>
     * </tr>
     * <tr>
     *  <td>Schottland</td><td>GB-SCT</td><td>1752-09-14</td>
     *  <td>CHRISTMAS_STYLE.until(1087)<br>.and(BEGIN_OF_JANUARY.until(1155))<br>.and(MARIA_ANUNCIATA.until(1600))</td><td></td>
     * </tr>
     * <tr>
     *  <td>Schweden <sup>2)</sup></td><td>SE</td><td>1753-03-01</td><td></td><td></td>
     * </tr>
     * <tr>
     *  <td>Ru&szlig;land <sup>3)</sup></td><td>RU</td><td>1918-02-14</td>
     *  <td>BEGIN_OF_JANUARY.until(988)<br>.and(BEGIN_OF_MARCH.until(1493))<br>.and(BEGIN_OF_SEPTEMBER.until(1700))</td>
     *  <td>BYZANTINE (988-1700)</td>
     * </tr>
     * </table>
     * </div>
     *
     * <p><sup>1)</sup> R&uuml;ckfall f&uuml;r katholische Regionen in deutsch-sprachigen L&auml;ndern, aber meistens erfolgte die Umstellung sp&auml;ter
     * <br><sup>2)</sup> Schweden verwendete einen besonderen Kalender, der zwischen 1700 und 1712 um einen Tag vom julianischen Kalender abwich (von Time4J unterst&uuml;tzt).
     * <br><sup>3)</sup> Spezieller Fall f&uuml;r das byzantinische Jahr 7208: Es dauerte von AD-1699-09-01 bis AD-1699-12-31 (julianischer Kalender). </p>
     *
     * @param   locale  country setting
     * @return  localized chronological history
     * @since   3.0
     * @see     #ofGregorianReform(PlainDate)
     */
    public static ChronoHistory of(Locale locale) {

        String key = FormatUtils.getRegion(locale);
        ChronoHistory history = null;

        if (!locale.getVariant().isEmpty()) {
            key = key + "-" + locale.getVariant();
            history = LOOKUP.get(key);
        }

        if (history == null) {
            history = LOOKUP.get(key);
        }

        if (history == null) {
            return ChronoHistory.ofFirstGregorianReform();
        } else {
            return history;
        }

    }

    /**
     * <p>The Swedish calendar has three cutover dates due to a failed experiment
     * when switching to gregorian calendar in the years 1700-1712 step by step. </p>
     *
     * @return  swedish chronological history
     * @since   3.0
     */
    /*[deutsch]
     * <p>Der schwedische Kalender hat drei Umstellungszeitpunkte, weil ein Experiment in den
     * Jahren 1700-1712 zur schrittweisen Einf&uuml;hrung des gregorianischen Kalenders mi&szlig;lang. </p>
     *
     * @return  swedish chronological history
     * @since   3.0
     */
    public static ChronoHistory ofSweden() {

        return SWEDEN;

    }

    /**
     * <p>Is given historic date valid? </p>
     *
     * <p>If the argument is {@code null} then this method returns {@code false}. </p>
     *
     * @param   date    historic calendar date to be checked, maybe {@code null}
     * @return  {@code false} if given date is invalid else {@code true}
     * @since   3.0
     */
    /*[deutsch]
     * <p>Ist das angegebene historische Datum g&uuml;ltig? </p>
     *
     * <p>Wenn das Argument {@code null} ist, liefert die Methode {@code false}. </p>
     *
     * @param   date    historic calendar date to be checked, maybe {@code null}
     * @return  {@code false} if given date is invalid else {@code true}
     * @since   3.0
     */
    public boolean isValid(HistoricDate date) {

        if ((date == null) || this.isOutOfRange(date)) {
            return false;
        }

        Calculus algorithm = this.getAlgorithm(date);
        return ((algorithm != null) && algorithm.isValid(date));

    }

    /**
     * <p>Converts given historic date to an ISO-8601-date. </p>
     *
     * @param   date    historic calendar date
     * @return  ISO-8601-date (gregorian)
     * @throws  IllegalArgumentException if given date is invalid or out of supported range
     * @see     #isValid(HistoricDate)
     * @since   3.0
     */
    /*[deutsch]
     * <p>Konvertiert das angegebene historische Datum zu einem ISO-8601-Datum. </p>
     *
     * @param   date    historic calendar date
     * @return  ISO-8601-date (gregorian)
     * @throws  IllegalArgumentException if given date is invalid or out of supported range
     * @see     #isValid(HistoricDate)
     * @since   3.0
     */
    public PlainDate convert(HistoricDate date) {

        if (this.isOutOfRange(date)) {
            throw new IllegalArgumentException("Out of supported range: " + date);
        }

        Calculus algorithm = this.getAlgorithm(date);

        if (algorithm == null) {
            throw new IllegalArgumentException("Invalid historic date: " + date);
        }

        return PlainDate.of(algorithm.toMJD(date), EpochDays.MODIFIED_JULIAN_DATE);

    }

    /**
     * <p>Converts given ISO-8601-date to a historic date. </p>
     *
     * @param   date    ISO-8601-date (gregorian)
     * @return  historic calendar date
     * @throws  IllegalArgumentException if given date is out of supported range
     * @since   3.0
     */
    /*[deutsch]
     * <p>Konvertiert das angegebene ISO-8601-Datum zu einem historischen Datum. </p>
     *
     * @param   date    ISO-8601-date (gregorian)
     * @return  historic calendar date
     * @throws  IllegalArgumentException if given date is out of supported range
     * @since   3.0
     */
    public HistoricDate convert(PlainDate date) {

        long mjd = date.get(EpochDays.MODIFIED_JULIAN_DATE);
        HistoricDate hd = null;

        for (int i = this.events.size() - 1; i >= 0; i--) {
            CutOverEvent event = this.events.get(i);

            if (mjd >= event.start) {
                hd = event.algorithm.fromMJD(mjd);
                break;
            }
        }

        if (hd == null) {
            hd = this.getJulianAlgorithm().fromMJD(mjd);
        }

        HistoricEra era = this.eraPreference.getPreferredEra(hd, date);

        if (era != hd.getEra()) {
            int yoe = era.yearOfEra(hd.getEra(), hd.getYearOfEra());
            hd = HistoricDate.of(era, yoe, hd.getMonth(), hd.getDayOfMonth());
        }

        if (this.isOutOfRange(hd)) {
            throw new IllegalArgumentException("Out of supported range: " + hd);
        }

        return hd;

    }

    /**
     * <p>Reconstructs the calendar history from given variant description. </p>
     *
     * @param   variant     description as defined in {@link #getVariant()}
     * @return  ChronoHistory
     * @throws  IllegalArgumentException if the variant cannot be interpreted as calendar history
     * @since   3.36/4.31
     */
    /*[deutsch]
     * <p>Rekonstruiert die Kalenderhistorie von der angegebenen Beschreibung. </p>
     *
     * @param   variant     description as defined in {@link #getVariant()}
     * @return  ChronoHistory
     * @throws  IllegalArgumentException if the variant cannot be interpreted as calendar history
     * @since   3.36/4.31
     */
    public static ChronoHistory from(String variant) {

        if (!variant.startsWith("historic-")) {
            throw new IllegalArgumentException("Variant does not start with \"historic-\": " + variant);
        }

        String[] parts = variant.substring(9).split(":");

        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid variant description.");
        }

        HistoricVariant hv = HistoricVariant.valueOf(parts[0]);
        ChronoHistory history;
        int startIndex = 2;

        switch (hv) {
            case PROLEPTIC_GREGORIAN:
                return ChronoHistory.PROLEPTIC_GREGORIAN;
            case PROLEPTIC_JULIAN:
                return ChronoHistory.PROLEPTIC_JULIAN;
            case PROLEPTIC_BYZANTINE:
                return ChronoHistory.PROLEPTIC_BYZANTINE;
            case SWEDEN:
                history = ChronoHistory.ofSweden();
                startIndex = 1;
                break;
            case INTRODUCTION_ON_1582_10_15:
                if (!getGregorianCutOverDate(parts, variant).equals(PlainDate.of(1582, 10, 15))) {
                    throw new IllegalArgumentException("Inconsistent cutover date: " + variant);
                }
                history = ChronoHistory.ofFirstGregorianReform();
                break;
            case SINGLE_CUTOVER_DATE:
                PlainDate cutover = getGregorianCutOverDate(parts, variant);
                history = ChronoHistory.ofGregorianReform(cutover);
                break;
            default:
                throw new UnsupportedOperationException(hv.name());
        }

        String[] a = parts[startIndex].split("=");

        if (a[0].equals("ancient-julian-leap-years")) {
            String ajly = a[1].substring(1, a[1].length() - 1);
            if (!ajly.isEmpty()) {
                String[] nums = ajly.split(",");
                int[] bcYears = new int[nums.length];
                for (int i = 0; i < nums.length; i++) {
                    bcYears[i] = 1 - Integer.parseInt(nums[i]);
                }
                history = history.with(AncientJulianLeapYears.of(bcYears));
            }
        }

        String[] b = parts[startIndex + 1].split("=");

        if (b[0].equals("new-year-strategy")) {
            String desc = b[1].substring(1, b[1].length() - 1);
            String[] rules = desc.split(",");
            NewYearStrategy nys = null;
            for (int i = 0; i < rules.length; i++) {
                String[] rule = rules[i].split("->");
                NewYearRule nyr = NewYearRule.valueOf(rule[0]);
                int annoDomini = (rule.length == 2 ? Integer.parseInt(rule[1]) : Integer.MAX_VALUE);
                if (nys == null) {
                    if ((nyr == NewYearRule.BEGIN_OF_JANUARY) && (annoDomini == 567)) {
                        continue;
                    }
                    nys = nyr.until(annoDomini);
                } else {
                    nys = nys.and(nyr.until(annoDomini));
                }
            }
            history = history.with(nys);
        }

        String[] c = parts[startIndex + 2].split("=");

        if (c[0].equals("era-preference")) {
            String desc = c[1].substring(1, c[1].length() - 1);
            if (!desc.equals("default")) {
                String[] prefs = desc.split(",");
                try {
                    HistoricEra era = HistoricEra.valueOf(prefs[0].substring(5));
                    PlainDate start = Iso8601Format.parseDate(prefs[1].substring(7));
                    PlainDate end = Iso8601Format.parseDate(prefs[2].substring(5));
                    switch (era) {
                        case HISPANIC:
                            history = history.with(EraPreference.hispanicBetween(start, end));
                            break;
                        case BYZANTINE:
                            history = history.with(EraPreference.byzantineBetween(start, end));
                            break;
                        case AB_URBE_CONDITA:
                            history = history.with(EraPreference.abUrbeConditaBetween(start, end));
                            break;
                        default:
                            throw new IllegalArgumentException("BC/AD not allowed as era preference: " + variant);
                    }
                } catch (ParseException pe) {
                    throw new IllegalArgumentException("Invalid date syntax: " + variant);
                }
            }
        }

        return history;

    }

    private static PlainDate getGregorianCutOverDate(String[] parts, String variant) {

        String[] c = parts[1].split("=");

        if (c.length != 2) {
            throw new IllegalArgumentException("Invalid syntax in variant description: " + variant);
        } else if (c[0].equals("cutover")) {
            try {
                return Iso8601Format.EXTENDED_DATE.parse(c[1]);
            } catch (ParseException e) {
                // abort in next code line
            }
        }

        throw new IllegalArgumentException("Invalid cutover definition: " + variant);

    }

    /**
     * <p>Yields the variant of a historic calendar. </p>
     *
     * @return  text describing the internal state
     * @see     #from(String)
     * @since   3.11/4.8
     */
    /*[deutsch]
     * <p>Liefert die Variante eines historischen Kalenders. </p>
     *
     * @return  text describing the internal state
     * @see     #from(String)
     * @since   3.11/4.8
     */
    @Override
    public String getVariant() {

        StringBuilder sb = new StringBuilder(64);
        sb.append("historic-");
        sb.append(this.variant.name());

        switch (this.variant){
            case PROLEPTIC_GREGORIAN:
            case PROLEPTIC_JULIAN:
            case PROLEPTIC_BYZANTINE:
                sb.append(":no-cutover");
                break;
            case INTRODUCTION_ON_1582_10_15:
            case SINGLE_CUTOVER_DATE:
                sb.append(":cutover=");
                sb.append(this.getGregorianCutOverDate());
                // fall-through
            default:
                sb.append(":ancient-julian-leap-years=");
                if (this.ajly != null) {
                    int[] pattern = this.ajly.getPattern();
                    sb.append('[');
                    sb.append(pattern[0]);
                    for (int i = 1; i < pattern.length; i++) {
                        sb.append(',');
                        sb.append(pattern[i]);
                    }
                    sb.append(']');
                } else {
                    sb.append("[]");
                }
                sb.append(":new-year-strategy=");
                sb.append(this.getNewYearStrategy());
                sb.append(":era-preference=");
                sb.append(this.getEraPreference());
        }

        return sb.toString();

    }

    /**
     * <p>Yields the date of final introduction of gregorian calendar. </p>
     *
     * @return  ISO-8601-date (gregorian)
     * @throws  UnsupportedOperationException if this history is proleptic
     * @see     #hasGregorianCutOverDate()
     * @since   3.0
     */
    /*[deutsch]
     * <p>Liefert das Datum der letztlichen Einf&uuml;hrung des gregorianischen Kalenders. </p>
     *
     * @return  ISO-8601-date (gregorian)
     * @throws  UnsupportedOperationException if this history is proleptic
     * @see     #hasGregorianCutOverDate()
     * @since   3.0
     */
    public PlainDate getGregorianCutOverDate() {

        long start = this.events.get(this.events.size() - 1).start;

        if (start == Long.MIN_VALUE) {
            throw new UnsupportedOperationException("Proleptic history without any gregorian reform date.");
        }

        return PlainDate.of(start, EpochDays.MODIFIED_JULIAN_DATE);

    }

    /**
     * <p>Determines if this history has at least one gregorian calendar reform date. </p>
     *
     * @return  boolean
     * @since   3.11/4.8
     * @see     #getGregorianCutOverDate()
     */
    /*[deutsch]
     * <p>Ermittelt, ob diese {@code ChronoHistory} wenigstens eine gregorianische Kalenderreform kennt. </p>
     *
     * @return  boolean
     * @since   3.11/4.8
     * @see     #getGregorianCutOverDate()
     */
    public boolean hasGregorianCutOverDate() {

        return (this.events.get(this.events.size() - 1).start > Long.MIN_VALUE);

    }

    /**
     * <p>Determines the date of New Year. </p>
     *
     * <p>Side note: This method should usually yield a valid historic date unless in case of ill
     * configured new-year-strategies which don't play well with configured cutover-dates. </p>
     *
     * @param   era             historic era
     * @param   yearOfEra       historic year of era as displayed (deviating from standard calendar year)
     * @return  historic date of New Year
     * @throws  IllegalArgumentException if given year is out of range or such that the result becomes invalid
     * @see     NewYearRule
     * @see     #getLengthOfYear(HistoricEra, int)
     * @see     #with(NewYearStrategy)
     * @see     #isValid(HistoricDate)
     * @see     HistoricDate#getYearOfEra(NewYearStrategy)
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Determines the date of New Year. </p>
     *
     * <p>Hinweis: Diese Methode sollte normalerweise ein g&uuml;ltiges historisches Datum liefern,
     * es sei denn, die Neujahrsstrategie wurde so konfiguriert, da&szlig; sie nicht mit den
     * gew&auml;hlten Kalenderumstellungsdaten harmoniert. </p>
     *
     * @param   era             historic era
     * @param   yearOfEra       historic year of era as displayed (deviating from standard calendar year)
     * @return  historic date of New Year
     * @throws  IllegalArgumentException if given year is out of range or such that the result becomes invalid
     * @see     NewYearRule
     * @see     #getLengthOfYear(HistoricEra, int)
     * @see     #with(NewYearStrategy)
     * @see     #isValid(HistoricDate)
     * @see     HistoricDate#getYearOfEra(NewYearStrategy)
     * @since   3.14/4.11
     */
    public HistoricDate getBeginOfYear(
        HistoricEra era,
        int yearOfEra
    ) {

        HistoricDate newYear = this.getNewYearStrategy().newYear(era, yearOfEra);

        if (this.isValid(newYear)) {
            PlainDate date = this.convert(newYear);
            HistoricEra preferredEra = this.eraPreference.getPreferredEra(newYear, date);
            if (preferredEra != era) {
                int yoe = preferredEra.yearOfEra(newYear.getEra(), newYear.getYearOfEra());
                newYear = HistoricDate.of(preferredEra, yoe, newYear.getMonth(), newYear.getDayOfMonth());
            }
            return newYear;
        } else {
            throw new IllegalArgumentException("Cannot determine valid New Year: " + era + "-" + yearOfEra);
        }

    }

    /**
     * <p>Determines the length of given historic year in days. </p>
     *
     * <p>The length of year can be affected by cutover gaps, different leap year rules and new year strategies. </p>
     *
     * @param   era             historic era
     * @param   yearOfEra       historic year of era as displayed (deviating from standard calendar year)
     * @return  length of historic year in days or {@code -1} if the length cannot be determined
     * @see     #getBeginOfYear(HistoricEra, int)
     * @see     #getGregorianCutOverDate()
     * @see     #with(NewYearStrategy)
     * @see     #with(AncientJulianLeapYears)
     * @see     HistoricDate#getYearOfEra(NewYearStrategy)
     * @since   3.0
     */
    /*[deutsch]
     * <p>Bestimmt die L&auml;nge des angegebenen historischen Jahres in Tagen. </p>
     *
     * <p>Die L&auml;nge des Jahres kann durch Kalenderumstellungen, verschiedene Schaltjahresregeln und
     * Neujahrsstrategien beeinflusst werden. </p>
     *
     * @param   era             historic era
     * @param   yearOfEra       historic year of era as displayed (deviating from standard calendar year)
     * @return  length of historic year in days or {@code -1} if the length cannot be determined
     * @see     #getBeginOfYear(HistoricEra, int)
     * @see     #getGregorianCutOverDate()
     * @see     #with(NewYearStrategy)
     * @see     #with(AncientJulianLeapYears)
     * @see     HistoricDate#getYearOfEra(NewYearStrategy)
     * @since   3.0
     */
    public int getLengthOfYear(
        HistoricEra era,
        int yearOfEra
    ) {

        try {
            HistoricDate min;
            HistoricDate max;
            int extra;

            if (this.nys == null) {
                min = HistoricDate.of(era, yearOfEra, 1, 1);
                max = HistoricDate.of(era, yearOfEra, 12, 31);
                extra = 1;
            } else {
                min = this.nys.newYear(era, yearOfEra);
                if (era == HistoricEra.BC) {
                    if (yearOfEra == 1) {
                        max = this.nys.newYear(HistoricEra.AD, 1);
                    } else {
                        max = this.nys.newYear(era, yearOfEra - 1);
                    }
                } else {
                    max = this.nys.newYear(era, yearOfEra + 1);
                    if (era == HistoricEra.BYZANTINE) {
                        HistoricDate hd = this.nys.newYear(HistoricEra.AD, era.annoDomini(yearOfEra));
                        if (hd.compareTo(min) > 0) {
                            max = hd;
                        }
                    }
                }
                extra = 0;
            }

            return (int) (CalendarUnit.DAYS.between(this.convert(min), this.convert(max)) + extra);
        } catch (RuntimeException re) {
            return -1; // only in very exotic circumstances (for example if given year is out of range)
        }

    }

    /**
     * <p>Yields the historic julian leap year pattern if available. </p>
     *
     * @return  sequence of historic julian leap years
     * @throws  UnsupportedOperationException if this history has not defined any historic julian leap years
     * @see     #hasAncientJulianLeapYears()
     * @see     #with(AncientJulianLeapYears)
     * @since   3.11/4.8
     */
    /*[deutsch]
     * <p>Liefert die Sequenz von historischen julianischen Schaltjahren, wenn verf&uuml;gbar. </p>
     *
     * @return  sequence of historic julian leap years
     * @throws  UnsupportedOperationException if this history has not defined any historic julian leap years
     * @see     #hasAncientJulianLeapYears()
     * @see     #with(AncientJulianLeapYears)
     * @since   3.11/4.8
     */
    public AncientJulianLeapYears getAncientJulianLeapYears() {

        if (this.ajly == null){
            throw new UnsupportedOperationException("No historic julian leap years were defined.");
        }

        return this.ajly;

    }

    /**
     * <p>Determines if this history has defined any historic julian leap year sequence. </p>
     *
     * @return  boolean
     * @since   3.11/4.8
     * @see     #with(AncientJulianLeapYears)
     * @see     #getAncientJulianLeapYears()
     */
    /*[deutsch]
     * <p>Ermittelt, ob diese {@code ChronoHistory} eine spezielle Sequenz von historischen
     * julianischen Schaltjahren kennt. </p>
     *
     * @return  boolean
     * @since   3.11/4.8
     * @see     #with(AncientJulianLeapYears)
     * @see     #getAncientJulianLeapYears()
     */
    public boolean hasAncientJulianLeapYears() {

        return (this.ajly != null);

    }

    /**
     * <p>Creates a copy of this history with given historic julian leap year sequence. </p>
     *
     * <p>This method has no effect if applied on {@code ChronoHistory.PROLEPTIC_GREGORIAN}
     * or {@code ChronoHistory.PROLEPTIC_JULIAN} or {@code ChronoHistory.PROLEPTIC_BYZANTINE}. </p>
     *
     * @param   ancientJulianLeapYears      sequence of historic julian leap years
     * @return  new history which starts at first of January in year BC 45
     * @since   3.11/4.8
     */
    /*[deutsch]
     * <p>Erzeugt eine Kopie dieser Instanz mit den angegebenen historischen julianischen Schaltjahren. </p>
     *
     * <p>Diese Methode hat keine Auswirkung, wenn angewandt auf {@code ChronoHistory.PROLEPTIC_GREGORIAN}
     * oder {@code ChronoHistory.PROLEPTIC_JULIAN} oder {@code ChronoHistory.PROLEPTIC_BYZANTINE}. </p>
     *
     * @param   ancientJulianLeapYears      sequence of historic julian leap years
     * @return  new history which starts at first of January in year BC 45
     * @since   3.11/4.8
     */
    public ChronoHistory with(AncientJulianLeapYears ancientJulianLeapYears) {

        if (ancientJulianLeapYears == null) {
            throw new NullPointerException("Missing ancient julian leap years.");
        } else if (!this.hasGregorianCutOverDate()) {
            return this;
        }

        return new ChronoHistory(this.variant, this.events, ancientJulianLeapYears, this.nys, this.eraPreference);

    }

    /**
     * <p>Yields the new-year-strategy. </p>
     *
     * <p>If not specified then this method falls back to first of January. </p>
     *
     * @return  NewYearStrategy
     * @see     #with(NewYearStrategy)
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Liefert die Neujahrsstrategie. </p>
     *
     * <p>Wenn nicht explizit festgelegt, wird diese Methode eine Regel basierend
     * auf dem ersten Januar liefern. </p>
     *
     * @return  NewYearStrategy
     * @see     #with(NewYearStrategy)
     * @since   3.14/4.11
     */
    public NewYearStrategy getNewYearStrategy() {

        return ((this.nys == null) ? NewYearStrategy.DEFAULT : this.nys);

    }

    /**
     * <p>Creates a copy of this history with given new-year-strategy. </p>
     *
     * <p>This method has no effect if applied on {@code ChronoHistory.PROLEPTIC_GREGORIAN}
     * or {@code ChronoHistory.PROLEPTIC_JULIAN} or {@code ChronoHistory.PROLEPTIC_BYZANTINE}. </p>
     *
     * @param   newYearStrategy     strategy how to determine New Year
     * @return  new history with changed new-year-strategy
     * @see     #getBeginOfYear(HistoricEra, int)
     * @see     #getLengthOfYear(HistoricEra, int)
     * @see     #getNewYearStrategy()
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Erzeugt eine Kopie dieser Instanz mit der angegebenen Neujahrsstrategie. </p>
     *
     * <p>Diese Methode hat keine Auswirkung, wenn angewandt auf {@code ChronoHistory.PROLEPTIC_GREGORIAN}
     * oder {@code ChronoHistory.PROLEPTIC_JULIAN} oder {@code ChronoHistory.PROLEPTIC_BYZANTINE}. </p>
     *
     * @param   newYearStrategy     strategy how to determine New Year
     * @return  new history with changed new-year-strategy
     * @see     #getBeginOfYear(HistoricEra, int)
     * @see     #getLengthOfYear(HistoricEra, int)
     * @see     #getNewYearStrategy()
     * @since   3.14/4.11
     */
    public ChronoHistory with(NewYearStrategy newYearStrategy) {

        if (newYearStrategy.equals(NewYearStrategy.DEFAULT)) {
            if (this.nys == null) {
                return this;
            } else {
                return new ChronoHistory(this.variant, this.events, this.ajly, null, this.eraPreference);
            }
        } else if (!this.hasGregorianCutOverDate()) {
            return this;
        }

        return new ChronoHistory(this.variant, this.events, this.ajly, newYearStrategy, this.eraPreference);

    }

    /**
     * <p>Creates a copy of this history with given era preference. </p>
     *
     * <p>This method has no effect if applied on {@code ChronoHistory.PROLEPTIC_GREGORIAN}
     * or {@code ChronoHistory.PROLEPTIC_JULIAN} or {@code ChronoHistory.PROLEPTIC_BYZANTINE}. </p>
     *
     * <p>Background: Some historic calendars used different eras than AD or BC. For example, Russia
     * used the Byzantine calendar before Julian year 1700 using the era Anno Mundi. This can be
     * expressed by setting a suitable era preference. </p>
     *
     * @param   eraPreference       strategy which era should be preferred (most relevant for printing)
     * @return  new history with changed era preference
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Erzeugt eine Kopie dieser Instanz mit der angegebenen &Auml;rapr&auml;ferenz. </p>
     *
     * <p>Diese Methode hat keine Auswirkung, wenn angewandt auf {@code ChronoHistory.PROLEPTIC_GREGORIAN}
     * oder {@code ChronoHistory.PROLEPTIC_JULIAN} oder {@code ChronoHistory.PROLEPTIC_BYZANTINE}. </p>
     *
     * <p>Hintergrund: Einige historische Kalender haben andere &Auml;ras als AD oder BC verwendet. Zum
     * Beispiel kannte Ru&szlig;land vor dem julianischen Jahr 1700 den byzantinischen Kalender, der die
     * &Auml;ra Anno Mundi ben&ouml;tigt. Dies kann mit Hilfe einer geeigneten &Auml;rapr&auml;ferenz
     * ausgedr&uuml;ckt werden. </p>
     *
     * @param   eraPreference       strategy which era should be preferred (most relevant for printing)
     * @return  new history with changed era preference
     * @since   3.14/4.11
     */
    public ChronoHistory with(EraPreference eraPreference) {

        if (eraPreference.equals(this.eraPreference) || !this.hasGregorianCutOverDate()) {
            return this;
        }

        return new ChronoHistory(this.variant, this.events, this.ajly, this.nys, eraPreference);

    }

    /**
     * <p>Defines the element for the historic date. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. </p>
     *
     * @return  date-related element
     * @since   3.15/4.12
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r das historische Datum. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element
     * {@link PlainDate#COMPONENT} registriert haben. </p>
     *
     * @return  date-related element
     * @since   3.15/4.12
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    public ChronoElement<HistoricDate> date() {

        return this.dateElement;

    }

    /**
     * <p>Defines the element for the historic era. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. The era value cannot be changed in any way which makes sense
     * so this element is like a display-only element. </p>
     *
     * @return  era-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r die historische &Auml;ra. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element
     * {@link PlainDate#COMPONENT} registriert haben. Der &Auml;ra-Wert kann nicht auf eine
     * sinnvolle Weise ge&auml;ndert werden, so da&szlig; sich dieses Element wie ein reines
     * Anzeigeelement verh&auml;lt. </p>
     *
     * @return  era-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    @FormattableElement(format = "G")
    public ChronoElement<HistoricEra> era() {

        return this.eraElement;

    }

    /**
     * <p>Defines the element for the year of a given historic era. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. The year starts on first of January. Note: Getting true historic
     * years which take care of different new-year-rules is possible via the expression
     * {@code plainDate.get(history.date()).getYearOfEra(history.getNewYearStrategy())}. </p>
     *
     * @return  year-of-era-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     * @see     #yearOfEra(YearDefinition)
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r das Jahr einer historischen &Auml;ra. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element {@link PlainDate#COMPONENT}
     * registriert haben. Das Jahr beginnt am ersten Januar. Hinweis: Um wahre historische Jahre zu erhalten, die
     * sich um verschiedene Neujahrsregeln k&uuml;mmern, kann folgender Ausdruck verwendet werden:
     * {@code plainDate.get(history.date()).getYearOfEra(history.getNewYearStrategy())}. </p>
     *
     * @return  year-of-era-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     * @see     #yearOfEra(YearDefinition)
     */
    @FormattableElement(format = "y")
    public TextElement<Integer> yearOfEra() {

        return this.yearOfEraElement;

    }

    /**
     * <p>Defines the element for the year of a given historic era. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. </p>
     *
     * <p>Example: Many parts of France used Easter style for New Year so the year 1564 began on 1564-04-01 and
     * ended on 1565-04-21 causing an ambivalency for April dates. Setting the date to April 10th in historic
     * year 1564 can be expressed in a non-ambivalent way if a suitable year definition is applied (standard
     * calendar years 1564 or 1565 possible). </p>
     *
     * <pre>
     *     ChronoHistory history = ChronoHistory.of(Locale.FRANCE);
     *     PlainDate date = history.convert(HistoricDate.of(HistoricEra.AD, 1563, 4, 10));
     *     assertThat(
     *          date.with(history.yearOfEra(YearDefinition.AFTER_NEW_YEAR), 1564),
     *          is(history.convert(HistoricDate.of(HistoricEra.AD, 1564, 4, 10))));
     *     assertThat(
     *          date.with(history.yearOfEra(YearDefinition.BEFORE_NEW_YEAR), 1564),
     *          is(history.convert(HistoricDate.of(HistoricEra.AD, 1565, 4, 10))));
     * </pre>
     *
     * @param   yearDefinition  determines how to display or interprete a historic year
     * @return  year-of-era-related element
     * @since   3.19/4.15
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r das Jahr einer historischen &Auml;ra. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element {@link PlainDate#COMPONENT}
     * registriert haben. </p>
     *
     * <p>Beispiel: Gro&szlig;e Teile von Frankreich haben den Osterstil als Neujahrsregel verwendet, so da&szlig;
     * das Jahr 1564 zum Datum 1564-04-01 begann und zum Datum 1565-04-21 endete. Hier gibt es keine eindeutige
     * Zuordnung von Datumsangaben im April. Wenn das Datum etwa auf den 10. April 1564 gesetzt werden soll,
     * kann das eindeutig mit Hilfe einer spezifischen Jahresdefinition ausgedr&uuml;ckt werden (Standardkalenderjahre
     * 1564 oder 1565 m&ouml;glich). </p>
     *
     * <pre>
     *     ChronoHistory history = ChronoHistory.of(Locale.FRANCE);
     *     PlainDate date = history.convert(HistoricDate.of(HistoricEra.AD, 1563, 4, 10));
     *     assertThat(
     *          date.with(history.yearOfEra(YearDefinition.AFTER_NEW_YEAR), 1564),
     *          is(history.convert(HistoricDate.of(HistoricEra.AD, 1564, 4, 10))));
     *     assertThat(
     *          date.with(history.yearOfEra(YearDefinition.BEFORE_NEW_YEAR), 1564),
     *          is(history.convert(HistoricDate.of(HistoricEra.AD, 1565, 4, 10))));
     * </pre>
     *
     * @param   yearDefinition  determines how to display or interprete a historic year
     * @return  year-of-era-related element
     * @since   3.19/4.15
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    public ChronoElement<Integer> yearOfEra(YearDefinition yearDefinition) {

        switch (yearDefinition) {
            case DUAL_DATING:
                return this.yearOfEraElement;
            case AFTER_NEW_YEAR:
                return this.yearAfterElement;
            case BEFORE_NEW_YEAR:
                return this.yearBeforeElement;
            default:
                throw new UnsupportedOperationException(yearDefinition.name());
        }

    }

    /**
     * <p>Defines the element for the century of a year in a given historic era. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. The underlying year starts on first of January. As example,
     * the 20th century lasted from year 1901 to year 2000. </p>
     *
     * @return  century-of-era-related element
     * @since   3.23/4.19
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     * @see     #yearOfEra()
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r das Jahrhundert einer historischen &Auml;ra. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element {@link PlainDate#COMPONENT}
     * registriert haben. Das Jahr beginnt am ersten Januar. Zum Beispiel dauerte das 20. Jahrhundert vom Jahr
     * 1901 bis zum Jahr 2000. </p>
     *
     * @return  century-of-era-related element
     * @since   3.23/4.19
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     * @see     #yearOfEra()
     */
    public ChronoElement<Integer> centuryOfEra() {

        return this.centuryElement;

    }

    /**
     * <p>Defines the element for the historic month. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. </p>
     *
     * @return  month-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r den historischen Monat. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element
     * {@link PlainDate#COMPONENT} registriert haben. </p>
     *
     * @return  month-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    @FormattableElement(format = "M", alt = "L")
    public TextElement<Integer> month() {

        return this.monthElement;

    }

    /**
     * <p>Defines the element for the historic day of month. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. </p>
     *
     * @return  day-of-month-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r den historischen Tag des Monats. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element
     * {@link PlainDate#COMPONENT} registriert haben. </p>
     *
     * @return  day-of-month-related element
     * @since   3.0
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    @FormattableElement(format = "d")
    public ChronoElement<Integer> dayOfMonth() {

        return this.dayOfMonthElement;

    }

    /**
     * <p>Defines the element for the historic day of year. </p>
     *
     * <p>This element is applicable on all chronological types which have registered the element
     * {@link PlainDate#COMPONENT}. Note: An eventually deviating new-year-strategy will be taken
     * into account so this element follows the year cycle which is decoupled from the month cycle. </p>
     *
     * @return  day-of-year-related element
     * @since   3.16/4.13
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    /*[deutsch]
     * <p>Definiert das Element f&uuml;r den historischen Tag des Jahres. </p>
     *
     * <p>Dieses Element ist auf alle chronologischen Typen anwendbar, die das Element
     * {@link PlainDate#COMPONENT} registriert haben. Hinweis: Eine eventuell abweichende
     * Neujahrsregel wird ber&uuml;cksichtigt, so da&szlig; dieses Element dem vom Monatszyklus
     * entkoppelten Jahreszyklus folgt. </p>
     *
     * @return  day-of-year-related element
     * @since   3.16/4.13
     * @see     PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    @FormattableElement(format = "D")
    public ChronoElement<Integer> dayOfYear() {

        return this.dayOfYearElement;

    }

    /**
     * <p>Yields all associated elements. </p>
     *
     * @return  unmodifiable set of historic elements
     * @since   3.1
     */
    /*[deutsch]
     * <p>Liefert alle zugeh&ouml;rigen historischen Elemente. </p>
     *
     * @return  unmodifiable set of historic elements
     * @since   3.1
     */
    public Set<ChronoElement<?>> getElements() {

        return this.elements;

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof ChronoHistory) {
            ChronoHistory that = (ChronoHistory) obj;

            if (this.variant == that.variant) {
                if (
                    isEqual(this.ajly, that.ajly)
                    && isEqual(this.nys, that.nys)
                    && this.eraPreference.equals(that.eraPreference)
                ) {
                    return (
                        (this.variant != HistoricVariant.SINGLE_CUTOVER_DATE)
                        || (this.events.get(0).start == that.events.get(0).start)
                    );
                }
            }
        }

        return false;

    }

    @Override
    public int hashCode() {

        if (this.variant == HistoricVariant.SINGLE_CUTOVER_DATE) {
            long h = this.events.get(0).start;
            return (int) (h ^ (h << 32));
        }

        return this.variant.hashCode();

    }

    public String toString() {

        return "ChronoHistory[" + this.getVariant() + "]";

    }

    /**
     * <p>Yields the proper calendar algorithm. </p>
     *
     * @param   date    historic date
     * @return  appropriate calendar algorithm or {@code null} if not found (in case of an invalid date)
     * @since   3.0
     */
    Calculus getAlgorithm(HistoricDate date) {

        for (int i = this.events.size() - 1; i >= 0; i--) {
            CutOverEvent event = this.events.get(i);

            if (date.compareTo(event.dateAtCutOver) >= 0) {
                return event.algorithm;
            } else if (date.compareTo(event.dateBeforeCutOver) > 0) {
                return null; // gap at cutover
            }
        }

        return this.getJulianAlgorithm();

    }

    /**
     * <p>Adjusts given historic date with respect to actual maximum of day-of-month if necessary. </p>
     *
     * @param   date    historic date
     * @return  adjusted date
     * @throws  IllegalArgumentException if argument is out of range
     * @since   3.0
     */
    HistoricDate adjustDayOfMonth(HistoricDate date) {

        Calculus algorithm = this.getAlgorithm(date);

        if (algorithm == null) {
            return date; // gap at cutover => let it be unchanged
        }

        int max = algorithm.getMaximumDayOfMonth(date);

        if (max < date.getDayOfMonth()) {
            return HistoricDate.of(date.getEra(), date.getYearOfEra(), date.getMonth(), max);
        } else {
            return date;
        }

    }

    /**
     * <p>Returns the list of all associated cutover events. </p>
     *
     * @return  unmodifiable list
     * @since   3.0
     */
    List<CutOverEvent> getEvents() {

        return this.events;

    }

    /**
     * Used in serialization.
     *
     * @return  enum
     */
    HistoricVariant getHistoricVariant() {

        return this.variant;

    }

    /**
     * Used in serialization.
     *
     * @return  era preference
     */
    EraPreference getEraPreference() {

        return this.eraPreference;

    }

    private boolean isOutOfRange(HistoricDate hd) {

        int ad = hd.getEra().annoDomini(hd.getYearOfEra());

        if (this == PROLEPTIC_BYZANTINE) {
            return ((ad < -5508) || ((ad == -5508) && (hd.getMonth() < 9)) || (ad > BYZANTINE_YMAX - 5508));
        } else if (this == PROLEPTIC_JULIAN) {
            return (Math.abs(ad) > JULIAN_YMAX);
        } else if (this == PROLEPTIC_GREGORIAN) {
            return (Math.abs(ad) > GregorianMath.MAX_YEAR);
        } else {
            return ((ad < -44) || (ad > 9999));
        }

    }

    private Calculus getJulianAlgorithm() {

        if (this.ajly != null) {
            return this.ajly.getCalculus();
        }

        return CalendarAlgorithm.JULIAN;

    }

    private static boolean isEqual(
        Object a1,
        Object a2
    ) {

        return ((a1 == null) ? (a2 == null) : a1.equals(a2));

    }

    private static void check(long mjd) {

        if (mjd < EARLIEST_CUTOVER) {
            throw new IllegalArgumentException("Gregorian calendar did not exist before 1582-10-15");
        }

    }

    private static ChronoHistory ofGregorianReform(long mjd) {

        return new ChronoHistory(
            ((mjd == EARLIEST_CUTOVER)
                ? HistoricVariant.INTRODUCTION_ON_1582_10_15
                : HistoricVariant.SINGLE_CUTOVER_DATE),
            Collections.singletonList(
                new CutOverEvent(mjd, CalendarAlgorithm.JULIAN, CalendarAlgorithm.GREGORIAN)));

    }

    /**
     * @serialData  Uses <a href="../../../serialized-form.html#net.time4j.history.SPX">
     *              a dedicated serialization form</a> as proxy. The format is bit-compressed.
     *              The first byte contains in the four most significant bits the type-ID {@code 3}.
     *              The following bits 4-7 contain the variant of history. The variant is usually
     *              zero, but for PROLEPTIC_GREGORIAN 1, for PROLEPTIC_JULIAN 2, for PROLEPTIC_BYZANTINE 3,
     *              for SWEDEN 4 and for the first gregorian reform 7. If the variant is zero then the
     *              cutover date in question will be written as long (modified julian date) into the
     *              stream. Then the sequence of eventually set ancient julian leap years will be
     *              written as int-array (length followed by list of extended years). Then the specific
     *              new-year-strategy will be written as count of list of rule-until-pairs followed by
     *              the list entries. Finally the era preference will be written such that non-default
     *              preferences require the byte 127 and else any other number. If non-default then
     *              the preferred era and the start date and the end date will be written.
     *
     * @return  replacement object in serialization graph
     */
    private Object writeReplace() {

        return new SPX(this, SPX.VERSION_3);

    }

    /**
     * @serialData  Blocks because a serialization proxy is required.
     * @param       in      object input stream
     * @throws InvalidObjectException (always)
     */
    private void readObject(ObjectInputStream in)
        throws IOException {

        throw new InvalidObjectException("Serialization proxy required.");

    }

}
