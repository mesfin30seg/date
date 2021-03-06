/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2021 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (MultiFormatParser.java) is part of project Time4J.
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

import net.time4j.engine.AttributeQuery;
import net.time4j.engine.ChronoEntity;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;


/**
 * <p>Serves for parsing of text input whose format is not yet known at compile time. </p>
 *
 * <p>User who only need to parse different formats for one locale only might consider the
 * simple alternative to concatenate all format pattern strings into one pattern with
 * the &quot;|&quot;-symbol as separator. </p>
 *
 * <p><strong>General notes about usage:</strong> </p>
 *
 * <p>a) If two patterns or formatters are combined then the order must be from the most complete
 * pattern/formatter to the least complete one. Example: Use &quot;MM/dd/yyyy HH:mm|MM/dd/yyyy&quot;
 * and not &quot;MM/dd/yyyy|MM/dd/yyyy HH:mm&quot;. This is especially important if the formatter in
 * question use default values because the single components will be processed before evaluating any
 * default values (which is a late step in parsing). </p>
 *
 * <p>b) If two patterns/formatters have the same degree of completeness then that component should
 * be noted first which is more likely to be expected in input. </p>
 *
 * @param   <T> generic type of chronological entity
 * @author  Meno Hochschild
 * @since   3.14/4.11
 */
/*[deutsch]
 * <p>Dient der Interpretation von Texteingaben, deren Format zur Kompilierzeit noch unbekannt ist. </p>
 *
 * <p>Anwender, die nur f&uuml;r eine Sprache verschiedene Formate interpretieren m&uuml;ssen, k&ouml;nnen
 * als Alternative auch das Zusammenziehen von Formatmustern in ein einziges Formatmuster in Betracht
 * ziehen, indem die einzelnen Formatmuster mit dem &quot;|&quot;-Symbol getrennt werden. </p>
 *
 * <p><strong>Allgemeine Bestimmungen zum Gebrauch:</strong> </p>
 *
 * <p>a) Wenn zwei Formatmuster oder Formatierer miteinander kombiniert werden, dann mu&szlig; die
 * Reihenfolge so gew&auml;hlt werden, da&szlig; das Formatmuster bzw. der Formatierer vorangeht, das/der
 * einen h&ouml;heren Grad an Vollst&auml;ndigkeit besitzt. Beispiel: Verwende &quot;MM/dd/yyyy HH:mm|MM/dd/yyyy&quot;
 * und nicht &quot;MM/dd/yyyy|MM/dd/yyyy HH:mm&quot;. Das ist besonders wichtig, wenn der
 * fragliche {@code ChronoFormatter} Standardwerte verwendet, weil die einzelnen Formatelemente
 * vor der Auswertung irgendwelcher Standardwerte zuerst ausgewertet werden. </p>
 *
 * <p>b) Falls zwei Formatmuster oder Formatierer den gleichen Grad an Vollst&auml;ndigkeit haben, dann sollte
 * das Formatmuster bzw. der Formatierer vorangehen, das in den zu erwartenden Eingabewerten wahrscheinlicher
 * zutrifft. </p>
 *
 * @param   <T> generic type of chronological entity
 * @author  Meno Hochschild
 * @since   3.14/4.11
 */
public final class MultiFormatParser<T extends ChronoEntity<T>>
    implements ChronoParser<T> {

    //~ Instanzvariablen --------------------------------------------------

    private final ChronoFormatter<T>[] parsers;

    //~ Konstruktoren -----------------------------------------------------

    private MultiFormatParser(ChronoFormatter<T>[] parsers) {
        super();

        this.parsers = parsers;

        for (ChronoFormatter<T> parser : this.parsers) {
            if (parser == null) {
                throw new NullPointerException("Null format cannot be set.");
            }
        }

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Creates a new multiple format parser. </p>
     *
     * @param   <T> generic type of chronological entity
     * @param   formats     array of multiple formats
     * @return  new immutable instance of MultiFormatParser
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Erzeugt einen neuen Multiformatinterpretierer. </p>
     *
     * @param   <T> generic type of chronological entity
     * @param   formats     array of multiple formats
     * @return  new immutable instance of MultiFormatParser
     * @since   3.14/4.11
     */
    @SafeVarargs
    public static <T extends ChronoEntity<T>> MultiFormatParser<T> of(ChronoFormatter<T>... formats) {

        ChronoFormatter<T>[] parsers = Arrays.copyOf(formats, formats.length);
        return new MultiFormatParser<>(parsers);

    }

    /**
     * <p>Creates a new multiple format parser. </p>
     *
     * @param   <T> generic type of chronological entity
     * @param   formats     list of multiple formats
     * @return  new immutable instance of MultiFormatParser
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Erzeugt einen neuen Multiformatinterpretierer. </p>
     *
     * @param   <T> generic type of chronological entity
     * @param   formats     list of multiple formats
     * @return  new immutable instance of MultiFormatParser
     * @since   3.14/4.11
     */
    @SuppressWarnings("unchecked")
    public static <T extends ChronoEntity<T>> MultiFormatParser<T> of(List<ChronoFormatter<T>> formats) {

        ChronoFormatter<T>[] parsers =
            formats.toArray((ChronoFormatter<T>[]) Array.newInstance(ChronoFormatter.class, formats.size()));
        return new MultiFormatParser<>(parsers);

    }

    /**
     * <p>Interpretes given text as chronological entity starting at the begin of text. </p>
     *
     * @param   text        text to be parsed
     * @return  parse result
     * @throws  IndexOutOfBoundsException if the text is empty
     * @throws  ParseException if the text is not parseable
     * @see     #parse(CharSequence, ParseLog)
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Interpretiert den angegebenen Text ab dem Anfang. </p>
     *
     * @param   text        text to be parsed
     * @return  parse result
     * @throws  IndexOutOfBoundsException if the text is empty
     * @throws  ParseException if the text is not parseable
     * @see     #parse(CharSequence, ParseLog)
     * @since   3.14/4.11
     */
    public T parse(CharSequence text)
        throws ParseException {

        ParseLog status = new ParseLog();

        for (int i = 0; i < this.parsers.length; i++) {
            status.reset(); // initialization
            status.setPosition(0);

            // use the default global attributes of every single parser
            T parsed = this.parsers[i].parse(text, status);

            if ((parsed != null) && !status.isError()) {
                if (this.parsers[i].isToleratingTrailingChars() || (status.getPosition() == text.length())) {
                    return parsed;
                }
            }

        }

        throw new ParseException("Not matched by any format: " + text, text.length());

    }

    /**
     * <p>Interpretes given text as chronological entity starting
     * at the specified position in parse log. </p>
     *
     * <p>Following example demonstrates best coding practice if used in processing bulk data: </p>
     *
     * <pre>
     *     static final MultiFormatParser&lt;PlainDate&gt; MULTI_FORMAT_PARSER;
     *
     *     static {
     *         ChronoFormatter&lt;PlainDate&gt; germanStyle =
     *              ChronoFormatter.ofDatePattern(&quot;d. MMMM uuuu&quot;, PatternType.CLDR, Locale.GERMAN);
     *         ChronoFormatter&lt;PlainDate&gt; frenchStyle =
     *              ChronoFormatter.ofDatePattern(&quot;d. MMMM uuuu&quot;, PatternType.CLDR, Locale.FRENCH);
     *         ChronoFormatter&lt;PlainDate&gt; usStyle =
     *              ChronoFormatter.ofDatePattern(&quot;MM/dd/uuuu&quot;, PatternType.CLDR, Locale.US);
     *         MULTI_FORMAT_PARSER = MultiFormatParser.of(germanStyle, frenchStyle, usStyle);
     *     }
     *
     *     public Collection&lt;PlainDate&gt; parse(Collection&lt;String&gt; data) {
     *         Collection&lt;PlainDate&gt; parsedDates = new ArrayList&lt;&gt;();
     *         ParseLog plog = new ParseLog();
     *         int index = 0;
     *
     *         for (String text : data) {
     *             PlainDate date = MULTI_FORMAT_PARSER.parse(text, plog);
     *             if ((date == null) || plog.isError()) {
     *                 // users are encouraged to use any good logging framework here
     *                 System.out.println(&quot;Wrong entry found: &quot; + text + &quot; at position &quot; + index);
     *             } else {
     *                 parsedDates.add(date);
     *             }
     *             index++;
     *         }
     *
     *         return Collections.unmodifiableCollection(parsedDates);
     *     }
     * </pre>
     *
     * <p>Note: This method tolerates trailing characters. If this behaviour is not useful
     * then please consider the alternative method {@link #parse(CharSequence)}. </p>
     *
     * @param   text        text to be parsed
     * @param   status      parser information (always as new instance)
     * @return  result or {@code null} if parsing does not work
     * @throws  IndexOutOfBoundsException if the start position is at end of text or even behind
     * @since   3.14/4.11
     */
    /*[deutsch]
     * <p>Interpretiert den angegebenen Text ab der angegebenen Position im
     * Log. </p>
     *
     * <p>Folgendes Beispiel demonstriert eine sinnvolle Anwendung, wenn es um die Massenverarbeitung geht: </p>
     *
     * <pre>
     *     static final MultiFormatParser&lt;PlainDate&gt; MULTI_FORMAT_PARSER;
     *
     *     static {
     *         ChronoFormatter&lt;PlainDate&gt; germanStyle =
     *              ChronoFormatter.ofDatePattern(&quot;d. MMMM uuuu&quot;, PatternType.CLDR, Locale.GERMAN);
     *         ChronoFormatter&lt;PlainDate&gt; frenchStyle =
     *              ChronoFormatter.ofDatePattern(&quot;d. MMMM uuuu&quot;, PatternType.CLDR, Locale.FRENCH);
     *         ChronoFormatter&lt;PlainDate&gt; usStyle =
     *              ChronoFormatter.ofDatePattern(&quot;MM/dd/uuuu&quot;, PatternType.CLDR, Locale.US);
     *         MULTI_FORMAT_PARSER = MultiFormatParser.of(germanStyle, frenchStyle, usStyle);
     *     }
     *
     *     public Collection&lt;PlainDate&gt; parse(Collection&lt;String&gt; data) {
     *         Collection&lt;PlainDate&gt; parsedDates = new ArrayList&lt;&gt;();
     *         ParseLog plog = new ParseLog();
     *         int index = 0;
     *
     *         for (String text : data) {
     *             PlainDate date = MULTI_FORMAT_PARSER.parse(text, plog);
     *             if ((date == null) || plog.isError()) {
     *                 // Anwender werden ermuntert, ein gutes Logging-Framework ihrer Wahl hier zu verwenden
     *                 System.out.println(&quot;Wrong entry found: &quot; + text + &quot; at position &quot; + index);
     *             } else {
     *                 parsedDates.add(date);
     *             }
     *             index++;
     *         }
     *
     *         return Collections.unmodifiableCollection(parsedDates);
     *     }
     * </pre>
     *
     * <p>Hinweis: Die Methode toleriert nicht interpretierte Zeichen am Textende. Wenn dieses Verhalten
     * nicht erw&uuml;nscht ist, dann bitte die alternative Methode {@link #parse(CharSequence)} benutzen. </p>
     *
     * @param   text        text to be parsed
     * @param   status      parser information (always as new instance)
     * @return  result or {@code null} if parsing does not work
     * @throws  IndexOutOfBoundsException if the start position is at end of text or even behind
     * @since   3.14/4.11
     */
    public T parse(
        CharSequence text,
        ParseLog status
    ) {

        int start = status.getPosition();

        for (int i = 0; i < this.parsers.length; i++) {
            status.reset(); // initialization
            status.setPosition(start);

            // use the default global attributes of every single parser
            T parsed = this.parsers[i].parse(text, status);

            if ((parsed != null) && !status.isError()) {
                return parsed;
            }

        }

        status.setError(status.getErrorIndex(), "Not matched by any format: " + text);
        return null;

    }

    @Override
    public T parse(
        CharSequence text,
        ParseLog status,
        AttributeQuery attributes
    ) {

        int start = status.getPosition();

        for (int i = 0; i < this.parsers.length; i++) {
            status.reset(); // initialization
            status.setPosition(start);

            // use the default global attributes of every single parser,
            // possibly overridden by user-defined attributes
            T parsed = this.parsers[i].parse(text, status, attributes);

            if ((parsed != null) && !status.isError()) {
                return parsed;
            }

        }

        status.setError(status.getErrorIndex(), "Not matched by any format: " + text);
        return null;

    }

}
