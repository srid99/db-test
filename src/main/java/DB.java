import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

public class DB {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java -cp target/db-1.0.jar:<jdbc-driver>.jar DB db.properties");
            System.exit(1);
        }

        Properties properties = new Properties();
        properties.load(new FileInputStream(args[0]));

        String url = properties.getProperty("url");
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");

        String query = properties.getProperty("query");

        System.out.println("Connecting to database...");
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("Connected!");

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            DBTablePrinter.printResultSet(rs);
        }
    }

    /**
     * Shamelessly copied and formatted below code from https://github.com/htorun/dbtableprinter (Thanks to the original author).
     */
    public static class DBTablePrinter {
        private static final int DEFAULT_MAX_TEXT_COL_WIDTH = 150;

        static final int CATEGORY_STRING = 1;
        static final int CATEGORY_INTEGER = 2;
        static final int CATEGORY_DOUBLE = 3;
        static final int CATEGORY_DATETIME = 4;
        static final int CATEGORY_BOOLEAN = 5;
        static final int CATEGORY_OTHER = 0;

        private static class Column {
            private String label;
            private int type;
            private String typeName;
            private int width = 0;
            private List<String> values = new ArrayList<>();
            private String justifyFlag = "";
            private int typeCategory = 0;

            Column(String label, int type, String typeName) {
                this.label = label;
                this.type = type;
                this.typeName = typeName;
            }

            String getLabel() {
                return label;
            }

            int getType() {
                return type;
            }

            String getTypeName() {
                return typeName;
            }

            int getWidth() {
                return width;
            }

            void setWidth(int width) {
                this.width = width;
            }

            void addValue(String value) {
                values.add(value);
            }

            String getValue(int i) {
                return values.get(i);
            }

            String getJustifyFlag() {
                return justifyFlag;
            }

            void justifyLeft() {
                this.justifyFlag = "-";
            }

            int getTypeCategory() {
                return typeCategory;
            }

            void setTypeCategory(int typeCategory) {
                this.typeCategory = typeCategory;
            }
        }

        static void printResultSet(ResultSet rs) {
            try {
                if (rs == null) {
                    System.err.println("DBTablePrinter Error: Result set is null!");
                    return;
                }
                if (rs.isClosed()) {
                    System.err.println("DBTablePrinter Error: Result Set is closed!");
                    return;
                }

                int maxStringColWidth = DEFAULT_MAX_TEXT_COL_WIDTH;

                // Get the meta data object of this ResultSet.
                ResultSetMetaData rsmd;
                rsmd = rs.getMetaData();

                // Total number of columns in this ResultSet
                int columnCount = rsmd.getColumnCount();

                // List of Column objects to store each columns of the ResultSet
                // and the String representation of their values.
                List<Column> columns = new ArrayList<>(columnCount);

                // List of table names. Can be more than one if it is a joined
                // table query
                List<String> tableNames = new ArrayList<>(columnCount);

                // Get the columns and their meta data.
                // NOTE: columnIndex for rsmd.getXXX methods STARTS AT 1 NOT 0
                for (int i = 1; i <= columnCount; i++) {
                    Column c = new Column(rsmd.getColumnLabel(i),
                            rsmd.getColumnType(i), rsmd.getColumnTypeName(i));
                    c.setWidth(c.getLabel().length());
                    c.setTypeCategory(whichCategory(c.getType()));
                    columns.add(c);

                    if (!tableNames.contains(rsmd.getTableName(i))) {
                        tableNames.add(rsmd.getTableName(i));
                    }
                }

                // Go through each row, get values of each column and adjust
                // column widths.
                int rowCount = 0;
                while (rs.next()) {

                    // NOTE: columnIndex for rs.getXXX methods STARTS AT 1 NOT 0
                    for (int i = 0; i < columnCount; i++) {
                        Column c = columns.get(i);
                        String value;
                        int category = c.getTypeCategory();

                        if (category == CATEGORY_OTHER) {

                            // Use generic SQL type name instead of the actual value
                            // for column types BLOB, BINARY etc.
                            value = "(" + c.getTypeName() + ")";

                        } else {
                            value = rs.getString(i + 1) == null ? "NULL" : rs.getString(i + 1);
                        }
                        switch (category) {
                            case CATEGORY_DOUBLE:

                                // For real numbers, format the string value to have 3 digits
                                // after the point. THIS IS TOTALLY ARBITRARY and can be
                                // improved to be CONFIGURABLE.
                                if (!value.equals("NULL")) {
                                    Double dValue = rs.getDouble(i + 1);
                                    value = String.format("%.3f", dValue);
                                }
                                break;

                            case CATEGORY_STRING:

                                // Left justify the text columns
                                c.justifyLeft();

                                // and apply the width limit
                                if (value.length() > maxStringColWidth) {
                                    value = value.substring(0, maxStringColWidth - 3) + "...";
                                }
                                break;
                        }

                        // Adjust the column width
                        c.setWidth(value.length() > c.getWidth() ? value.length() : c.getWidth());
                        c.addValue(value);
                    } // END of for loop columnCount
                    rowCount++;

                } // END of while (rs.next)

                /*
                At this point we have gone through meta data, get the
                columns and created all Column objects, iterated over the
                ResultSet rows, populated the column values and adjusted
                the column widths.

                We cannot start printing just yet because we have to prepare
                a row separator String.
                 */

                // For the fun of it, I will use StringBuilder
                StringBuilder strToPrint = new StringBuilder();
                StringBuilder rowSeparator = new StringBuilder();

                /*
                Prepare column labels to print as well as the row separator.
                It should look something like this:
                +--------+------------+------------+-----------+  (row separator)
                | EMP_NO | BIRTH_DATE | FIRST_NAME | LAST_NAME |  (labels row)
                +--------+------------+------------+-----------+  (row separator)
                 */

                // Iterate over columns
                for (Column c : columns) {
                    int width = c.getWidth();

                    // Center the column label
                    String toPrint;
                    String name = c.getLabel();
                    int diff = width - name.length();

                    if ((diff % 2) == 1) {
                        // diff is not divisible by 2, add 1 to width (and diff)
                        // so that we can have equal padding to the left and right
                        // of the column label.
                        width++;
                        diff++;
                        c.setWidth(width);
                    }

                    int paddingSize = diff / 2; // InteliJ says casting to int is redundant.

                    // Cool String repeater code thanks to user102008 at stackoverflow.com
                    // (http://tinyurl.com/7x9qtyg) "Simple way to repeat a string in java"
                    String padding = new String(new char[paddingSize]).replace("\0", " ");

                    toPrint = "| " + padding + name + padding + " ";
                    // END centering the column label

                    strToPrint.append(toPrint);

                    rowSeparator.append("+");
                    rowSeparator.append(new String(new char[width + 2]).replace("\0", "-"));
                }

                String lineSeparator = System.getProperty("line.separator");

                // Is this really necessary ??
                lineSeparator = lineSeparator == null ? "\n" : lineSeparator;

                rowSeparator.append("+").append(lineSeparator);

                strToPrint.append("|").append(lineSeparator);
                strToPrint.insert(0, rowSeparator);
                strToPrint.append(rowSeparator);

                StringJoiner sj = new StringJoiner(", ");
                for (String name : tableNames) {
                    sj.add(name);
                }

                String info = "Printing " + rowCount;
                info += rowCount > 1 ? " rows from " : " row from ";
                info += tableNames.size() > 1 ? "tables " : "table ";
                info += sj.toString();

                System.out.println(info);

                // Print out the formatted column labels
                System.out.print(strToPrint.toString());

                String format;

                // Print out the rows
                for (int i = 0; i < rowCount; i++) {
                    for (Column c : columns) {

                        // This should form a format string like: "%-60s"
                        format = String.format("| %%%s%ds ", c.getJustifyFlag(), c.getWidth());
                        System.out.print(
                                String.format(format, c.getValue(i))
                        );
                    }

                    System.out.println("|");
                    System.out.print(rowSeparator);
                }

                System.out.println();

            /*
                Hopefully this should have printed something like this:
                +--------+------------+------------+-----------+--------+-------------+
                | EMP_NO | BIRTH_DATE | FIRST_NAME | LAST_NAME | GENDER |  HIRE_DATE  |
                +--------+------------+------------+-----------+--------+-------------+
                |  10001 | 1953-09-02 | Georgi     | Facello   | M      |  1986-06-26 |
                +--------+------------+------------+-----------+--------+-------------+
                |  10002 | 1964-06-02 | Bezalel    | Simmel    | F      |  1985-11-21 |
                +--------+------------+------------+-----------+--------+-------------+
             */

            } catch (SQLException e) {
                System.err.println("SQL exception in DBTablePrinter. Message:");
                System.err.println(e.getMessage());
            }
        }

        private static int whichCategory(int type) {
            switch (type) {
                case Types.BIGINT:
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                    return CATEGORY_INTEGER;

                case Types.REAL:
                case Types.DOUBLE:
                case Types.DECIMAL:
                    return CATEGORY_DOUBLE;

                case Types.DATE:
                case Types.TIME:
                case Types.TIME_WITH_TIMEZONE:
                case Types.TIMESTAMP:
                case Types.TIMESTAMP_WITH_TIMEZONE:
                    return CATEGORY_DATETIME;

                case Types.BOOLEAN:
                    return CATEGORY_BOOLEAN;

                case Types.VARCHAR:
                case Types.NVARCHAR:
                case Types.LONGVARCHAR:
                case Types.LONGNVARCHAR:
                case Types.CHAR:
                case Types.NCHAR:
                    return CATEGORY_STRING;

                default:
                    return CATEGORY_OTHER;
            }
        }
    }
}
