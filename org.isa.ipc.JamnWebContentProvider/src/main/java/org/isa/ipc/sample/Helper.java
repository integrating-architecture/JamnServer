/* Authored by www.integrating-architecture.de */
package org.isa.ipc.sample;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper to provide some common functionalities.
 */
public class Helper {

    private Helper() {
    }

    /**
     */
    public static String getStackTraceFrom(Throwable t) {
        StringWriter lSwriter = new StringWriter();
        PrintWriter lPwriter = new PrintWriter(lSwriter);

        if (t instanceof InvocationTargetException te) {
            t = te.getTargetException();
        }

        t.printStackTrace(lPwriter);
        return lSwriter.toString();
    }

    /**
     * <pre>
     * A simple class implementing template strings that include variable expressions.
     *
     * e.g. new ExprString("Hello ${visitor} I'am ${me}")
     *          .put("visitor", "John")
     *          .put("me", "Andreas")
     *          .build();
     * results in: "Hello John I'am Andreas"
     * </pre>
     */
    public static class ExprString {
        protected static String PatternStart = "${";
        protected static String PatternEnd = "}";
        protected static Pattern ExprPattern = Pattern.compile("\\$\\{(\\w.+)\\}");

        protected String template = "";
        protected Map<String, String> valueMap = new HashMap<>();
        protected ValueProvider provider = (String pKey, Object pCtx) -> valueMap.getOrDefault(pKey, "");

        /**
         */
        protected ExprString() {
        }

        /**
         */
        public ExprString(String pTemplate) {
            this();
            template = pTemplate;
        }

        /**
         */
        public ExprString(String pTemplate, ValueProvider pProvider) {
            this(pTemplate);
            provider = pProvider;
        }

        /**
         */
        @Override
        public String toString() {
            return template;
        }

        /**
         */
        public ExprString put(String pKey, String pValue) {
            valueMap.put(pKey, pValue);
            return this;
        }

        /**
         */
        public String build() {
            return build(null);
        }

        /**
         */
        public String build(Object pCtx) {
            StringBuilder lResult = new StringBuilder();
            String lPart = "";
            String lName = "";
            String lValue = "";
            Matcher lMatcher = ExprPattern.matcher(template);

            int lCurrentPos = 0;
            while (lMatcher.find()) {
                lPart = template.substring(lCurrentPos, lMatcher.start());
                lName = lMatcher.group().replace(PatternStart, "").replace(PatternEnd, "");
                lValue = provider.getValue(lName, pCtx);
                lResult.append(lPart).append(lValue);
                lCurrentPos = lMatcher.end();
            }
            if (lCurrentPos < template.length()) {
                lPart = template.substring(lCurrentPos, template.length());
                lResult.append(lPart);
            }
            return lResult.toString();
        }

        /**
         */
        public static interface ValueProvider {
            String getValue(String pKey, Object pCtx);
        }
    }
}