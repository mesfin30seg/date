/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2021 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (TextAccessor.java) is part of project Time4J.
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

package net.time4j.format;

import net.time4j.engine.AttributeQuery;

import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * <p>Supplies an access to the internal name list of an enum-based
 * element value. </p>
 *
 * @author  Meno Hochschild
 * @since   2.0
 * @see     CalendarText
 */
/*[deutsch]
 * <p>Stellt einen Zugriff auf die enthaltenen Namen per Elementwert-Enum
 * bereit. </p>
 *
 * @author  Meno Hochschild
 * @since   2.0
 * @see     CalendarText
 */
public final class TextAccessor {

    //~ Statische Felder/Initialisierungen --------------------------------

    private static final char PROTECTED_SPACE = '\u00A0'; // ASCII-0160

    //~ Instanzvariablen --------------------------------------------------

    private final List<String> textForms;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>Standard-Konstruktor. </p>
     *
     * @param   textForms   Array von Textformen
     */
    TextAccessor(String[] textForms) {
        super();

        this.textForms = Collections.unmodifiableList(Arrays.asList(textForms));

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Prints the given element value as String. </p>
     *
     * <p>If the element value has no localized representation then this
     * method will simply print the enum name. </p>
     *
     * @param   value   current value of element
     * @return  localized text form
     */
    /*[deutsch]
     * <p>Stellt den angegebenen Elementwert als String dar. </p>
     *
     * <p>Hat der Elementwert keine lokalisierte Darstellung, wird einfach
     * sein Enum-Name ausgegeben. </p>
     *
     * @param   value   current value of element
     * @return  localized text form
     */
    public String print(Enum<?> value) {

        int index = value.ordinal();

        if (this.textForms.size() <= index) {
            return value.name();
        } else {
            return this.textForms.get(index);
        }

    }

    /**
     * <p>Interpretes given text form as enum-based element value. </p>
     *
     * <p>Parsing is case-insensitive. No partial compare is performed,
     * instead the whole element text will be evaluated. </p>
     *
     * @param   <V> generic value type of element
     * @param   parseable       text to be parsed
     * @param   status          current parsing position
     * @param   valueType       value class of element
     * @return  element value (as enum) or {@code null} if not found
     * @see     #parse(CharSequence, ParsePosition, Class, AttributeQuery)
     */
    /*[deutsch]
     * <p>Interpretiert die angegebene Textform als Enum-Elementwert. </p>
     *
     * <p>Die Gro&szlig;- und Kleinschreibung ist nicht relevant. Es
     * wird immer jeweils der ganze Text verglichen. </p>
     *
     * @param   <V> generic value type of element
     * @param   parseable       text to be parsed
     * @param   status          current parsing position
     * @param   valueType       value class of element
     * @return  element value (as enum) or {@code null} if not found
     * @see     #parse(CharSequence, ParsePosition, Class, AttributeQuery)
     */
    public <V extends Enum<V>> V parse(
        CharSequence parseable,
        ParsePosition status,
        Class<V> valueType
    ) {

        return this.parse(parseable, status, valueType, true, false, true);

    }

    /**
     * <p>Interpretes given text form as enum-based element value. </p>
     *
     * @param   <V> generic value type of element
     * @param   parseable       text to be parsed
     * @param   status          current parsing position
     * @param   valueType       value class of element
     * @param   leniency        leniency mode
     * @return  element value (as enum) or {@code null} if not found
     * @see     Attributes#LENIENCY
     * @see     #parse(CharSequence, ParsePosition, Class, AttributeQuery)
     * @since   3.15/4.12
     */
    /*[deutsch]
     * <p>Interpretiert die angegebene Textform als Enum-Elementwert. </p>
     *
     * @param   <V> generic value type of element
     * @param   parseable       text to be parsed
     * @param   status          current parsing position
     * @param   valueType       value class of element
     * @param   leniency        leniency mode
     * @return  element value (as enum) or {@code null} if not found
     * @see     Attributes#LENIENCY
     * @see     #parse(CharSequence, ParsePosition, Class, AttributeQuery)
     * @since   3.15/4.12
     */
    public <V extends Enum<V>> V parse(
        CharSequence parseable,
        ParsePosition status,
        Class<V> valueType,
        Leniency leniency
    ) {

        boolean caseInsensitive = true;
        boolean partialCompare = false;
        boolean smart = true;

        if (leniency == Leniency.STRICT) {
            caseInsensitive = false;
            smart = false;
        } else if (leniency == Leniency.LAX) {
            partialCompare = true;
        }

        return this.parse(parseable, status, valueType, caseInsensitive, partialCompare, smart);

    }

    /**
     * <p>Interpretes given text form as enum-based element value. </p>
     *
     * <p>The attributes {@code Attributes.PARSE_CASE_INSENSITIVE} and
     * {@code Attributes.PARSE_PARTIAL_COMPARE} will be evaluated. </p>
     *
     * @param   <V> generic value type of element
     * @param   parseable       text to be parsed
     * @param   status          current parsing position
     * @param   valueType       value class of element
     * @param   attributes      format attributes
     * @return  element value (as enum) or {@code null} if not found
     * @see     Attributes#PARSE_CASE_INSENSITIVE
     * @see     Attributes#PARSE_PARTIAL_COMPARE
     */
    /*[deutsch]
     * <p>Interpretiert die angegebene Textform als Enum-Elementwert. </p>
     *
     * <p>Es werden die Attribute {@code Attributes.PARSE_CASE_INSENSITIVE}
     * und {@code Attributes.PARSE_PARTIAL_COMPARE} ausgewertet. </p>
     *
     * @param   <V> generic value type of element
     * @param   parseable       text to be parsed
     * @param   status          current parsing position
     * @param   valueType       value class of element
     * @param   attributes      format attributes
     * @return  element value (as enum) or {@code null} if not found
     * @see     Attributes#PARSE_CASE_INSENSITIVE
     * @see     Attributes#PARSE_PARTIAL_COMPARE
     */
    public <V extends Enum<V>> V parse(
        CharSequence parseable,
        ParsePosition status,
        Class<V> valueType,
        AttributeQuery attributes
    ) {

        boolean caseInsensitive =
            attributes
                .get(Attributes.PARSE_CASE_INSENSITIVE, Boolean.TRUE)
                .booleanValue();
        boolean partialCompare =
            attributes
                .get(Attributes.PARSE_PARTIAL_COMPARE, Boolean.FALSE)
                .booleanValue();
        boolean smart =
            attributes
                .get(Attributes.PARSE_MULTIPLE_CONTEXT, Boolean.TRUE)
                .booleanValue();
        return this.parse(
            parseable, status, valueType, caseInsensitive, partialCompare, smart);

    }

    /**
     * <p>Obtains a list of all underlying text forms. </p>
     *
     * @return  unmodifiable list of text resources
     * @since   3.32/4.27
     */
    /*[deutsch]
     * <p>Liefert eine Liste aller zugrundeliegenden Textformen. </p>
     *
     * @return  unmodifiable list of text resources
     * @since   3.32/4.27
     */
    public List<String> getTextForms() {

        return this.textForms;

    }

    /**
     * <p>Supports mainly debugging. </p>
     */
    /*[deutsch]
     * <p>Dient im wesentlichen Debugging-Zwecken. </p>
     */
    @Override
    public String toString() {

        int n = this.textForms.size();
        StringBuilder sb = new StringBuilder(n * 16 + 2);
        sb.append('{');
        boolean first = true;
        for (int i = 0; i < n; i++) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(this.textForms.get(i));
        }
        sb.append('}');
        return sb.toString();

    }

    private <V extends Enum<V>> V parse(
        CharSequence parseable,
        ParsePosition status,
        Class<V> valueType,
        boolean caseInsensitive,
        boolean partialCompare,
        boolean smart
    ) {

        V[] enums = valueType.getEnumConstants();
        int len = this.textForms.size();
        int start = status.getIndex();
        int end = parseable.length();
        String alt = "";

        int maxEq = 0;
        V candidate = null;

        for (int i = 0; i < enums.length; i++) {
            boolean firstTry = alt.isEmpty();
            String s;
            if (firstTry) {
                s = ((i >= len) ? enums[i].name() : this.textForms.get(i));
            } else {
                s = alt;
            }
            int pos = start;
            int n = s.length();
            boolean eq = true;

            for (int j = 0; eq && (j < n); j++) {
                if (start + j >= end) {
                    eq = false;
                } else {
                    char c = parseable.charAt(start + j);
                    char t = s.charAt(j);

                    if (smart) {
                        if (c == PROTECTED_SPACE) {
                            c = ' ';
                        }
                        if (t == PROTECTED_SPACE) {
                            t = ' ';
                        }
                    }

                    if (caseInsensitive) {
                        eq = (c == t) || this.compareIgnoreCase(c, t);
                    } else {
                        eq = (c == t);
                    }

                    if (eq) {
                        pos++;
                    }
                }
            }

            // special smart procedure for handling "Sept." versus "Sep." in German
            if (smart && firstTry && (n == 5) && (s.charAt(4) == '.')) {
                int dot = start + 3;
                if ((pos == dot) && (dot < end) && (parseable.charAt(dot) == '.')) {
                    alt = s.subSequence(start, dot) + ".";
                    i--; // reset loop counter for repeating
                    continue;
                }
            }

            alt = "";

            if (partialCompare || (n == 1)) {
                if (maxEq < pos - start) {
                    maxEq = pos - start;
                    candidate = enums[i];
                } else if (maxEq == pos - start) {
                    candidate = null;
                }
            } else if (eq) {
                assert pos == start + n;
                status.setIndex(pos);
                return enums[i];
            }
        }

        if (candidate == null) {
            status.setErrorIndex(start);
        } else {
            status.setIndex(start + maxEq);
        }

        return candidate;

    }

    private boolean compareIgnoreCase(char c1, char c2) {

        if (c1 >= 'a' && c1 <= 'z') {
            if (c2 >= 'A' && c2 <= 'Z') {
                c2 = (char) (c2 + 'a' - 'A');
            }
            return (c1 == c2);
        } else if (c1 >= 'A' && c1 <= 'Z') {
            c1 = (char) (c1 + 'a' - 'A');
            if (c2 >= 'A' && c2 <= 'Z') {
                c2 = (char) (c2 + 'a' - 'A');
            }
            return (c1 == c2);
        }

        return (
            Character.toUpperCase(c1) == Character.toUpperCase(c2)
            || Character.toLowerCase(c1) == Character.toLowerCase(c2)
        );

    }

}
