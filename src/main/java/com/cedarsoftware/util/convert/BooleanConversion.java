package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         Kenny Partlow (kpartlow@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class BooleanConversion {
    static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    static Short toShort(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    static Integer toInteger(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return new AtomicLong(b ? 1 : 0);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return new AtomicBoolean(b);
    }

    static Long toLong(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean)from;
        return b ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    static Float toFloat(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }

    static char toCharacter(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b ? CommonValues.CHARACTER_ONE : CommonValues.CHARACTER_ZERO;
    }
}
