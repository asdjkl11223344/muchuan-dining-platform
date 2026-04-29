import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class DbSnapshot {
    private static final String URL =
            "jdbc:mysql://localhost:3306/muchuan_platform?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "112233";

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            for (String sql : new String[] {
                    "select 'employee' as table_name, count(*) as cnt from employee",
                    "select 'category' as table_name, count(*) as cnt from category",
                    "select 'dish' as table_name, count(*) as cnt from dish",
                    "select 'setmeal' as table_name, count(*) as cnt from setmeal",
                    "select 'user' as table_name, count(*) as cnt from user",
                    "select 'address_book' as table_name, count(*) as cnt from address_book",
                    "select 'orders' as table_name, count(*) as cnt from orders",
                    "select 'order_detail' as table_name, count(*) as cnt from order_detail",
                    "select id,name,username,status from employee order by id",
                    "select id,type,name,status,sort from category order by type, sort, id",
                    "select id,name,category_id,price,status from dish order by id limit 30",
                    "select id,name,category_id,price,status from setmeal order by id",
                    "select id,name,phone,create_time from user order by id",
                    "select id,number,status,user_name,consignee,amount,order_time from orders order by id"
            }) {
                System.out.println("SQL> " + sql);
                try (ResultSet rs = statement.executeQuery(sql)) {
                    print(rs);
                }
                System.out.println();
            }
        }
    }

    private static void print(ResultSet rs) throws Exception {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    row.append(" | ");
                }
                row.append(metaData.getColumnLabel(i)).append('=').append(rs.getString(i));
            }
            System.out.println(row);
        }
    }
}
