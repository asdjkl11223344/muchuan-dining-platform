import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpdateMenuImages {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/muchuan_platform?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "112233";

    private static final long[] DISH_IDS = {
            46L, 47L, 48L, 49L, 50L,
            51L, 52L, 53L, 54L, 55L,
            56L, 57L, 58L, 59L, 60L,
            61L, 62L, 63L, 64L, 65L,
            66L, 67L, 68L, 69L, 70L
    };

    private static final long[] SETMEAL_IDS = {32L, 33L, 34L};

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : DEFAULT_URL;
        String user = args.length > 1 ? args[1] : DEFAULT_USER;
        String password = args.length > 2 ? args[2] : DEFAULT_PASSWORD;

        Map<Long, String> dishPaths = buildPaths("dish", DISH_IDS);
        Map<Long, String> setmealPaths = buildPaths("setmeal", SETMEAL_IDS);

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);

            try {
                int dishCount = updateEntityImages(connection, "dish", "id", dishPaths);
                int setmealCount = updateEntityImages(connection, "setmeal", "id", setmealPaths);
                int orderDetailDishCount = updateAssociatedImages(connection, "order_detail", "dish_id", dishPaths);
                int orderDetailSetmealCount = updateAssociatedImages(connection, "order_detail", "setmeal_id", setmealPaths);
                int cartDishCount = updateAssociatedImages(connection, "shopping_cart", "dish_id", dishPaths);
                int cartSetmealCount = updateAssociatedImages(connection, "shopping_cart", "setmeal_id", setmealPaths);

                connection.commit();

                System.out.println("dish updated: " + dishCount);
                System.out.println("setmeal updated: " + setmealCount);
                System.out.println("order_detail(dish) updated: " + orderDetailDishCount);
                System.out.println("order_detail(setmeal) updated: " + orderDetailSetmealCount);
                System.out.println("shopping_cart(dish) updated: " + cartDishCount);
                System.out.println("shopping_cart(setmeal) updated: " + cartSetmealCount);
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private static Map<Long, String> buildPaths(String prefix, long[] ids) {
        Map<Long, String> paths = new LinkedHashMap<>();
        for (long id : ids) {
            paths.put(id, "/img/menu/" + prefix + "-" + id + ".svg");
        }
        return paths;
    }

    private static int updateEntityImages(Connection connection, String tableName, String idColumn, Map<Long, String> paths) throws Exception {
        int updated = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + tableName + " SET image = ? WHERE " + idColumn + " = ?")) {
            for (Map.Entry<Long, String> entry : paths.entrySet()) {
                statement.setString(1, entry.getValue());
                statement.setLong(2, entry.getKey());
                updated += statement.executeUpdate();
            }
        }
        return updated;
    }

    private static int updateAssociatedImages(Connection connection, String tableName, String idColumn, Map<Long, String> paths) throws Exception {
        int updated = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + tableName + " SET image = ? WHERE " + idColumn + " = ?")) {
            for (Map.Entry<Long, String> entry : paths.entrySet()) {
                statement.setString(1, entry.getValue());
                statement.setLong(2, entry.getKey());
                updated += statement.executeUpdate();
            }
        }
        return updated;
    }
}
