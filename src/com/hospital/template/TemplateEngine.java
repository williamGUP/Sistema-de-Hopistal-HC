package com.hospital.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de plantillas que usamos para armar las páginas del sistema.
 * Lee los archivos .html de templates/ y reemplaza los {{llave}}
 * por los datos reales: por ejemplo, {{nombre}} pone el nombre del
 * paciente. Sirve para listas ({{#items}}), bloques "si no hay" ({{^}})
 * y para meter el header/footer con {{> partial}}. Siempre escapa el
 * texto antes de mostrarlo para evitar que se inyecte HTML raro.
 */

public class TemplateEngine {

    private final Path templatesDir;
    private final boolean cacheEnabled;
    private final Map<String, String> cache = new HashMap<>();

    public TemplateEngine(Path templatesDir) {
        this(templatesDir, false);
    }

    public TemplateEngine(Path templatesDir, boolean cacheEnabled) {
        this.templatesDir = templatesDir;
        this.cacheEnabled = cacheEnabled;
    }

    public String render(String templateName, Map<String, Object> data) {
        List<Map<String, Object>> stack = new ArrayList<>();
        stack.add(data == null ? new HashMap<>() : data);
        StringBuilder out = new StringBuilder();
        renderNodes(loadTemplate(templateName), stack, out);
        return out.toString();
    }

    private String loadTemplate(String name) {
        String key = name.endsWith(".html") ? name : name + ".html";
        if (cacheEnabled && cache.containsKey(key)) return cache.get(key);
        Path p = templatesDir.resolve(key);
        try {
            String content = Files.readString(p, StandardCharsets.UTF_8);
            if (cacheEnabled) cache.put(key, content);
            return content;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar la plantilla: " + p, e);
        }
    }

    private void renderNodes(String src, List<Map<String, Object>> stack, StringBuilder out) {
        int i = 0;
        int len = src.length();
        while (i < len) {
            int tagStart = src.indexOf("{{", i);
            if (tagStart < 0) {
                out.append(src, i, len);
                return;
            }
            out.append(src, i, tagStart);

            if (src.startsWith("{{{", tagStart)) {
                int tagEnd = src.indexOf("}}}", tagStart);
                if (tagEnd < 0) throw new RuntimeException("Tag sin cerrar (triple llave) cerca de: " + context(src, tagStart));
                String key = src.substring(tagStart + 3, tagEnd).trim();
                Object val = lookup(stack, key);
                out.append(val == null ? "" : String.valueOf(val));
                i = tagEnd + 3;
                continue;
            }

            int tagEnd = src.indexOf("}}", tagStart);
            if (tagEnd < 0) throw new RuntimeException("Tag sin cerrar cerca de: " + context(src, tagStart));
            String rawTag = src.substring(tagStart + 2, tagEnd).trim();
            i = tagEnd + 2;

            if (rawTag.isEmpty()) continue;
            char sigil = rawTag.charAt(0);

            if (sigil == '!') {
                continue; // comentario
            }
            if (sigil == '>') {
                String partialName = rawTag.substring(1).trim();
                renderNodes(loadTemplate("partials/" + partialName), stack, out);
                continue;
            }
            if (sigil == '#' || sigil == '^') {
                boolean inverted = sigil == '^';
                String key = rawTag.substring(1).trim();
                int[] range = findBlockEnd(src, i, key);
                String inner = src.substring(i, range[0]);
                i = range[1];

                Object val = lookup(stack, key);
                if (!inverted && val instanceof List) {
                    for (Object item : (List<?>) val) {
                        List<Map<String, Object>> newStack = new ArrayList<>(stack);
                        newStack.add(toContextMap(item));
                        renderNodes(inner, newStack, out);
                    }
                } else {
                    boolean truthy = isTruthy(val);
                    if (inverted ? !truthy : truthy) {
                        renderNodes(inner, stack, out);
                    }
                }
                continue;
            }

            // variable simple
            Object val = lookup(stack, rawTag);
            out.append(val == null ? "" : escapeHtml(String.valueOf(val)));
        }
    }

    /** Busca el cierre {{/key}} que corresponde a la apertura actual, respetando anidamiento de la MISMA clave. */
    private int[] findBlockEnd(String src, int fromIndex, String key) {
        String closeTag = "{{/" + key + "}}";
        String openHash = "{{#" + key;
        String openCaret = "{{^" + key;
        int depth = 1;
        int cursor = fromIndex;
        while (true) {
            int nextClose = src.indexOf(closeTag, cursor);
            if (nextClose < 0) {
                throw new RuntimeException("Falta la etiqueta de cierre {{/" + key + "}} (sección abierta sin cerrar)");
            }
            int nextOpenHash = src.indexOf(openHash, cursor);
            int nextOpenCaret = src.indexOf(openCaret, cursor);
            int nextOpen = -1;
            if (nextOpenHash >= 0 && nextOpenHash < nextClose) nextOpen = nextOpenHash;
            if (nextOpenCaret >= 0 && nextOpenCaret < nextClose && (nextOpen < 0 || nextOpenCaret < nextOpen)) nextOpen = nextOpenCaret;
            if (nextOpen >= 0) {
                depth++;
                cursor = nextOpen + 3;
                continue;
            }
            depth--;
            if (depth == 0) {
                return new int[]{nextClose, nextClose + closeTag.length()};
            }
            cursor = nextClose + closeTag.length();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toContextMap(Object item) {
        if (item instanceof Map) return (Map<String, Object>) item;
        Map<String, Object> wrap = new HashMap<>();
        wrap.put(".", item);
        return wrap;
    }

    /**
     * Busca una clave en la pila de contextos. Soporta notación con puntos
     * (p.ej. "paciente.nombreCompleto") para leer un valor anidado dentro de
     * un Map guardado bajo la primera clave, sin necesidad de abrir una
     * sección {{#paciente}} solo para eso.
     */
    private Object lookup(List<Map<String, Object>> stack, String key) {
        int dot = key.indexOf('.');
        String head = dot < 0 ? key : key.substring(0, dot);
        Object val = null;
        boolean found = false;
        for (int idx = stack.size() - 1; idx >= 0; idx--) {
            Map<String, Object> ctx = stack.get(idx);
            if (ctx.containsKey(head)) { val = ctx.get(head); found = true; break; }
        }
        if (!found || dot < 0) return found ? val : null;

        String rest = key.substring(dot + 1);
        while (true) {
            if (!(val instanceof Map)) return null;
            int nextDot = rest.indexOf('.');
            String part = nextDot < 0 ? rest : rest.substring(0, nextDot);
            Map<?, ?> m = (Map<?, ?>) val;
            if (!m.containsKey(part)) return null;
            val = m.get(part);
            if (nextDot < 0) return val;
            rest = rest.substring(nextDot + 1);
        }
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return !((String) val).isEmpty();
        if (val instanceof Collection) return !((Collection<?>) val).isEmpty();
        if (val instanceof Number) return ((Number) val).doubleValue() != 0;
        return true;
    }

    private String context(String src, int at) {
        int end = Math.min(src.length(), at + 40);
        return src.substring(at, end).replace("\n", "\\n");
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}