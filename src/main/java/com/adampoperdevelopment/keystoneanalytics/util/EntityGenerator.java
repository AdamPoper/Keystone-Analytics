package com.adampoperdevelopment.keystoneanalytics.util;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Run via: mvn compile exec:java
 * Reads DB schema and generates JPA @Entity classes, including @ManyToOne / @OneToMany relationships.
 */
public class EntityGenerator {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/equity-fundamentals";
    private static final String USER = System.getenv("DB_USERNAME");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final String CATALOG = "equity-fundamentals";
    private static final String PACKAGE = "com.adampoperdevelopment.keystoneanalytics.entity";
    private static final String OUTPUT_DIR =
            "src/main/java/com/adampoperdevelopment/keystoneanalytics/entity";

    static class FkRelation {
        final String fkTable;   // table that owns the FK column (many side)
        final String fkColumn;  // FK column name (e.g. "company_id")
        final String pkTable;   // referenced table (e.g. "company")
        boolean nullable;

        FkRelation(String fkTable, String fkColumn, String pkTable, boolean nullable) {
            this.fkTable = fkTable;
            this.fkColumn = fkColumn;
            this.pkTable = pkTable;
            this.nullable = nullable;
        }
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
            DatabaseMetaData meta = conn.getMetaData();

            List<String> tables = new ArrayList<>();
            ResultSet rs = meta.getTables(CATALOG, null, "%", new String[]{"TABLE"});
            while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
            Set<String> tableSet = new HashSet<>(tables);

            // importedFks: table -> FKs declared on that table (many side)
            Map<String, List<FkRelation>> importedFks = new HashMap<>();
            // exportedFks: table -> FKs from other tables pointing here (one side)
            Map<String, List<FkRelation>> exportedFks = new HashMap<>();

            for (String table : tables) {
                // Primary: read FK constraints defined in the schema
                List<FkRelation> fks = readImportedFks(meta, table);
                // Fallback: detect by naming convention (<table>_id columns) for unconstrained FKs
                Set<String> alreadyMapped = new HashSet<>();
                for (FkRelation fk : fks) alreadyMapped.add(fk.fkColumn);
                fks.addAll(detectConventionFks(meta, table, tableSet, alreadyMapped));

                importedFks.put(table, fks);
                for (FkRelation fk : fks) {
                    exportedFks.computeIfAbsent(fk.pkTable, k -> new ArrayList<>()).add(fk);
                }
            }

            for (String table : tables) {
                generateEntity(meta, table,
                        importedFks.getOrDefault(table, List.of()),
                        exportedFks.getOrDefault(table, List.of()));
                System.out.println("Generated: " + toClassName(table) + ".java");
            }
        }
    }

    private static List<FkRelation> readImportedFks(DatabaseMetaData meta, String table) throws SQLException {
        List<FkRelation> fks = new ArrayList<>();
        ResultSet rs = meta.getImportedKeys(CATALOG, null, table);
        while (rs.next()) {
            fks.add(new FkRelation(
                    table,
                    rs.getString("FKCOLUMN_NAME"),
                    rs.getString("PKTABLE_NAME"),
                    true // nullability filled in from column metadata below
            ));
        }
        return fks;
    }

    // Detects FKs by naming convention: a column named <x>_id where <x> matches a known table name.
    private static List<FkRelation> detectConventionFks(DatabaseMetaData meta, String table,
                                                         Set<String> knownTables,
                                                         Set<String> alreadyMapped) throws SQLException {
        List<FkRelation> fks = new ArrayList<>();
        ResultSet cols = meta.getColumns(CATALOG, null, table, null);
        while (cols.next()) {
            String colName = cols.getString("COLUMN_NAME");
            if (alreadyMapped.contains(colName)) continue;
            if (!colName.endsWith("_id")) continue;
            String referencedTable = colName.substring(0, colName.length() - 3); // strip "_id"
            if (knownTables.contains(referencedTable)) {
                fks.add(new FkRelation(table, colName, referencedTable, true));
                System.out.println("  [convention FK] " + table + "." + colName + " -> " + referencedTable);
            }
        }
        return fks;
    }

    private static void generateEntity(DatabaseMetaData meta, String tableName,
                                       List<FkRelation> importedFks,
                                       List<FkRelation> exportedFks) throws Exception {
        String className = toClassName(tableName);
        Set<String> primaryKeys = getPrimaryKeys(meta, tableName);

        // fkColumnMap: FK column name -> its relation (used to skip raw column, emit @ManyToOne instead)
        Map<String, FkRelation> fkColumnMap = new HashMap<>();
        for (FkRelation fk : importedFks) fkColumnMap.put(fk.fkColumn, fk);

        // Read all columns
        List<String[]> columns = new ArrayList<>();
        ResultSet cols = meta.getColumns(CATALOG, null, tableName, null);
        while (cols.next()) {
            String colName = cols.getString("COLUMN_NAME");
            columns.add(new String[]{
                    colName,
                    cols.getString("TYPE_NAME"),
                    cols.getString("COLUMN_SIZE"),
                    cols.getString("NULLABLE"),
                    cols.getString("IS_AUTOINCREMENT")
            });
            // Fix FK nullability from actual column metadata
            if (fkColumnMap.containsKey(colName)) {
                fkColumnMap.get(colName).nullable = "1".equals(cols.getString("NULLABLE"));
            }
        }

        boolean needsBigDecimal = false, needsLocalDate = false, needsLocalDateTime = false;
        boolean needsCollections = !exportedFks.isEmpty();

        StringBuilder fields = new StringBuilder();

        // Regular columns — FK columns are kept but marked insertable=false, updatable=false
        // since the @ManyToOne @JoinColumn owns the actual write
        for (String[] col : columns) {
            String colName = col[0];
            boolean isFkCol = fkColumnMap.containsKey(colName);

            String sqlType = col[1].toUpperCase();
            int size = Integer.parseInt(col[2]);
            boolean nullable = "1".equals(col[3]);
            boolean isAutoInc = "YES".equals(col[4]);
            boolean isPk = primaryKeys.contains(colName);

            String javaType = toJavaType(sqlType);
            if (javaType.equals("BigDecimal")) needsBigDecimal = true;
            if (javaType.equals("LocalDate")) needsLocalDate = true;
            if (javaType.equals("LocalDateTime")) needsLocalDateTime = true;

            if (isPk) {
                fields.append("    @Id\n");
                if (isAutoInc) fields.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            }
            fields.append(buildColumnAnnotation(colName, sqlType, size, nullable, isPk, isFkCol));
            fields.append("    private ").append(javaType).append(" ").append(toCamelCase(colName)).append(";\n\n");
        }

        // @ManyToOne fields — one per FK column
        for (FkRelation fk : importedFks) {
            String refClass = toClassName(fk.pkTable);
            String fieldName = toCamelCase(fk.pkTable);
            fields.append("    @ManyToOne(fetch = FetchType.LAZY)\n");
            fields.append("    @JoinColumn(name = \"").append(fk.fkColumn).append("\"");
            if (!fk.nullable) fields.append(", nullable = false");
            fields.append(")\n");
            fields.append("    private ").append(refClass).append(" ").append(fieldName).append(";\n\n");
        }

        // @OneToMany fields — one per table that has a FK pointing here
        for (FkRelation fk : exportedFks) {
            String refClass = toClassName(fk.fkTable);
            String fieldName = toCamelCase(fk.fkTable) + "s";
            String mappedBy = toCamelCase(fk.pkTable); // field name used in the @ManyToOne entity
            fields.append("    @OneToMany(mappedBy = \"").append(mappedBy).append("\")\n");
            fields.append("    private List<").append(refClass).append("> ").append(fieldName)
                    .append(" = new ArrayList<>();\n\n");
        }

        // Build import list
        List<String> imports = new ArrayList<>();
        imports.add("jakarta.persistence.*");
        if (needsBigDecimal) imports.add("java.math.BigDecimal");
        if (needsLocalDate) imports.add("java.time.LocalDate");
        if (needsLocalDateTime) imports.add("java.time.LocalDateTime");
        if (needsCollections) { imports.add("java.util.ArrayList"); imports.add("java.util.List"); }
        Collections.sort(imports);

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(PACKAGE).append(";\n\n");
        for (String imp : imports) sb.append("import ").append(imp).append(";\n");
        sb.append("\n@Entity\n");
        sb.append("@Table(name = \"").append(tableName).append("\")\n");
        sb.append("public class ").append(className).append(" {\n\n");
        sb.append(fields);
        sb.append(generateGettersSetters(columns, importedFks, exportedFks, fkColumnMap));
        sb.append("}\n");

        Files.writeString(Paths.get(OUTPUT_DIR, className + ".java"), sb.toString());
    }

    private static String generateGettersSetters(List<String[]> columns,
                                                  List<FkRelation> importedFks,
                                                  List<FkRelation> exportedFks,
                                                  Map<String, FkRelation> fkColumnMap) {
        StringBuilder sb = new StringBuilder();

        for (String[] col : columns) {
            String field = toCamelCase(col[0]);
            String type = toJavaType(col[1].toUpperCase());
            String cap = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            sb.append("    public ").append(type).append(" get").append(cap).append("() {\n");
            sb.append("        return ").append(field).append(";\n    }\n\n");
            sb.append("    public void set").append(cap).append("(").append(type).append(" ").append(field).append(") {\n");
            sb.append("        this.").append(field).append(" = ").append(field).append(";\n    }\n\n");
        }

        for (FkRelation fk : importedFks) {
            String refClass = toClassName(fk.pkTable);
            String field = toCamelCase(fk.pkTable);
            String cap = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            sb.append("    public ").append(refClass).append(" get").append(cap).append("() {\n");
            sb.append("        return ").append(field).append(";\n    }\n\n");
            sb.append("    public void set").append(cap).append("(").append(refClass).append(" ").append(field).append(") {\n");
            sb.append("        this.").append(field).append(" = ").append(field).append(";\n    }\n\n");
        }

        for (FkRelation fk : exportedFks) {
            String refClass = toClassName(fk.fkTable);
            String field = toCamelCase(fk.fkTable) + "s";
            String cap = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            sb.append("    public List<").append(refClass).append("> get").append(cap).append("() {\n");
            sb.append("        return ").append(field).append(";\n    }\n\n");
            sb.append("    public void set").append(cap).append("(List<").append(refClass).append("> ").append(field).append(") {\n");
            sb.append("        this.").append(field).append(" = ").append(field).append(";\n    }\n\n");
        }

        return sb.toString();
    }

    private static String buildColumnAnnotation(String colName, String sqlType, int size, boolean nullable, boolean isPk, boolean isFkCol) {
        List<String> attrs = new ArrayList<>();
        attrs.add("name = \"" + colName + "\"");
        if (!nullable && !isPk) attrs.add("nullable = false");
        if ((sqlType.contains("VARCHAR") || sqlType.contains("CHAR")) && size > 0) attrs.add("length = " + size);
        if (sqlType.contains("DECIMAL") || sqlType.contains("NUMERIC")) attrs.add("precision = " + size);
        if (isFkCol) { attrs.add("insertable = false"); attrs.add("updatable = false"); }
        return "    @Column(" + String.join(", ", attrs) + ")\n";
    }

    private static Set<String> getPrimaryKeys(DatabaseMetaData meta, String table) throws SQLException {
        Set<String> pks = new LinkedHashSet<>();
        ResultSet rs = meta.getPrimaryKeys(CATALOG, null, table);
        while (rs.next()) pks.add(rs.getString("COLUMN_NAME"));
        return pks;
    }

    private static String toJavaType(String sqlType) {
        if (sqlType.startsWith("VARCHAR") || sqlType.startsWith("CHAR") ||
                sqlType.startsWith("TEXT") || sqlType.startsWith("LONGTEXT") ||
                sqlType.startsWith("MEDIUMTEXT") || sqlType.startsWith("TINYTEXT") ||
                sqlType.startsWith("ENUM") || sqlType.startsWith("SET")) return "String";
        if (sqlType.startsWith("BIGINT")) return "Long";
        if (sqlType.startsWith("INT") || sqlType.startsWith("MEDIUMINT") ||
                sqlType.startsWith("SMALLINT")) return "Integer";
        if (sqlType.startsWith("TINYINT")) return "Boolean";
        if (sqlType.startsWith("DECIMAL") || sqlType.startsWith("NUMERIC")) return "BigDecimal";
        if (sqlType.startsWith("DOUBLE") || sqlType.startsWith("REAL")) return "Double";
        if (sqlType.startsWith("FLOAT")) return "Float";
        if (sqlType.startsWith("BOOLEAN") || sqlType.startsWith("BIT")) return "Boolean";
        if (sqlType.startsWith("DATE") && !sqlType.startsWith("DATETIME")) return "LocalDate";
        if (sqlType.startsWith("DATETIME") || sqlType.startsWith("TIMESTAMP")) return "LocalDateTime";
        return "String";
    }

    private static String toClassName(String tableName) {
        StringBuilder sb = new StringBuilder();
        for (String part : tableName.split("[_\\-]")) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String toCamelCase(String name) {
        String[] parts = name.split("[_\\-]");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }
}