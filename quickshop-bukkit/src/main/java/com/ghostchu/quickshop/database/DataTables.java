package com.ghostchu.quickshop.database;

import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.api.action.PreparedSQLUpdateAction;
import cc.carm.lib.easysql.api.action.PreparedSQLUpdateBatchAction;
import cc.carm.lib.easysql.api.builder.*;
import cc.carm.lib.easysql.api.enums.IndexType;
import cc.carm.lib.easysql.api.function.SQLHandler;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public enum DataTables {

    DATA("data", (table) -> {
        table.addAutoIncrementColumn("id", true); // SHOP DATA ID
        table.addColumn("owner", "VARCHAR(36) NOT NULL"); // SHOP DATA OWNER (ALL-ZERO if this is a server shop)

        table.addColumn("item", "TEXT NOT NULL"); // SHOP DATA ITEM INFO
        table.addColumn("name", "TEXT"); // SHOP NAME

        table.addColumn("type", "INT NOT NULL DEFAULT 0"); // SHOP TYPE (see ShopType enum)
        table.addColumn("currency", "VARCHAR(64)");  // CURRENCY (NULL means use the default currency)
        table.addColumn("price", "DECIMAL(32,2) NOT NULL"); // SHOP ITEM PRICE

        // UNLIMITED STORAGE (means the shop can sell/buy unlimited amount of items)
        table.addColumn("unlimited", "BIT NOT NULL DEFAULT 0");
        // ITEM HOLOGRAM (whether to show the item in the top of the container block)
        table.addColumn("hologram", "BIT NOT NULL DEFAULT 0");

        table.addColumn("tax_account", "VARCHAR(36)"); // TAX ACCOUNT
        table.addColumn("permissions", "MEDIUMTEXT"); // PERMISSIONS (JSON)
        table.addColumn("extra", "LONGTEXT"); // EXTRA

        table.addColumn("inv_wrapper", "VARCHAR(255) NOT NULL"); // INVENTORY TYPE
        table.addColumn("inv_symbol_link", "TEXT NOT NULL"); // INVENTORY DATA (to read the inventory info)

        table.addColumn("create_time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"); // SHOP CREATE TIME
        // table.addColumn("remove_time", "DATETIME"); // SHOP REMOVE TIME (NULL if not removed)

        table.setIndex(IndexType.INDEX, "index_qs_data_owner", "owner");
    }),

    SHOPS("shops", (table) -> {
        // SHOP ID
        table.addAutoIncrementColumn("id", true); // SHOP ID
        // DATA ID
        table.addColumn("data", "INT UNSIGNED NOT NULL");
    }),

    SHOP_MAP("shop_map", (table) -> {
        // BLOCK LOCATION DATA
        table.addColumn("world", "VARCHAR(32) NOT NULL");
        table.addColumn("x", "INT NOT NULL");
        table.addColumn("y", "INT NOT NULL");
        table.addColumn("z", "INT NOT NULL");

        // SHOP ID
        table.addColumn("shop", "INT UNSIGNED NOT NULL");

        table.setIndex(IndexType.PRIMARY_KEY, "index_qs_shop_map", "world", "x", "y", "z");
//        table.addForeignKey(
//                "shop", "fk_qs_shop_map", SHOPS.getName(), "id",
//                ForeignKeyRule.CASCADE, ForeignKeyRule.CASCADE
//        );
    }),

    MESSAGES("message", (table) -> {
        table.addAutoIncrementColumn("id", true);
        table.addColumn("receiver", "VARCHAR(36) NOT NULL");
        table.addColumn("time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        table.addColumn("content", "MEDIUMTEXT NOT NULL");
    }),

    METADATA("metadata", (table) -> {
        table.addColumn("key", "VARCHAR(255) NOT NULL PRIMARY KEY");
        table.addColumn("value", "LONGTEXT NOT NULL");
    }),

    PLAYERS("players", (table) -> {
        table.addColumn("uuid", "VARCHAR(36) NOT NULL PRIMARY KEY");
        table.addColumn("locale", "TEXT NOT NULL");
    }),

    LOG_PURCHASE("log_purchase", (table) -> {
        table.addAutoIncrementColumn("id", true);
        table.addColumn("time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        table.addColumn("shop", "INT UNSIGNED NOT NULL"); // SHOP ID
        table.addColumn("data", "INT UNSIGNED NOT NULL"); // DATA ID
        table.addColumn("buyer", "VARCHAR(36) NOT NULL"); // BUYER

        table.addColumn("type", "VARCHAR(32) NOT NULL"); // SHOP TYPE (use enum name)
        table.addColumn("amount", "INT NOT NULL"); // ITEM AMOUNT

        table.addColumn("money", "DECIMAL(32,2) NOT NULL"); // TOTAL MONEY
        table.addColumn("tax", "DECIMAL(32,2) NOT NULL DEFAULT 0"); // TAX
    }),

    LOG_TRANSACTION("log_transaction", (table) -> {
        table.addAutoIncrementColumn("id", true);
        table.addColumn("time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

        table.addColumn("from", "VARCHAR(36) NOT NULL");
        table.addColumn("to", "VARCHAR(36) NOT NULL");

        table.addColumn("currency", "VARCHAR(64)");
        table.addColumn("amount", "DECIMAL(32,2) NOT NULL");

        table.addColumn("tax_amount", "DECIMAL(32,2) NOT NULL DEFAULT 0");
        table.addColumn("tax_account", "VARCHAR(36)");

        // TRANSACTION ERROR MESSAGES (NULL means successfully transacted)
        table.addColumn("error", "MEDIUMTEXT");
    }),

    /**
     * Modifications logs table.
     * <p>This table contains shop's modifications like:
     * <ul>
     *     <li>Shop created</li>
     *     <li>Shop removed</li>
     *     <li>Shop name changed</li>
     *     <li>Shop price changed</li>
     *     <li>Shop permissions granted</li>
     * </ul>
     */
    LOG_CHANGES("log_changes", (table) -> {
        table.addAutoIncrementColumn("id", true);
        table.addColumn("time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

        table.addColumn("shop", "INT UNSIGNED NOT NULL"); // SHOP ID
        table.addColumn("type", "VARCHAR(32) NOT NULL"); // CHANGE TYPE (use enum name)

        table.addColumn("before", "INT UNSIGNED NOT NULL"); // BEFORE DATA
        table.addColumn("after", "INT UNSIGNED NOT NULL"); // AFTER DATA

        // table.setIndex(IndexType.INDEX, "idx_qs_changed_shop", "shop");
    }),

    LOG_OTHERS("log_others", (table) -> {
        table.addAutoIncrementColumn("id", true);
        table.addColumn("time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

        // LOG TYPE (generally is class name)
        table.addColumn("type", "VARCHAR(255) NOT NULL");
        // LOG DATA (generally is JSON)
        table.addColumn("data", "MEDIUMTEXT NOT NULL");
    });

    private final @NotNull String name;
    private final @NotNull SQLHandler<TableCreateBuilder> tableHandler;

    private String prefix;
    private SQLManager manager;

    DataTables(@NotNull String name,
               @NotNull SQLHandler<TableCreateBuilder> tableHandler) {
        this.name = name;
        this.tableHandler = tableHandler;
    }

    public static void initializeTables(@NotNull SQLManager sqlManager,
                                        @NotNull String tablePrefix) throws SQLException {
        for (DataTables value : values()) {
            value.create(sqlManager, tablePrefix);
        }
    }

    public @NotNull String getName() {
        return this.prefix + this.name;
    }

    private void create(@NotNull SQLManager sqlManager, @NotNull String tablePrefix) throws SQLException {
        if (this.manager == null) this.manager = sqlManager;
        this.prefix = tablePrefix;

        TableCreateBuilder tableBuilder = sqlManager.createTable(this.getName());
        tableHandler.accept(tableBuilder);
        tableBuilder.build().execute();
    }

    public @NotNull TableQueryBuilder createQuery(@NotNull SQLManager sqlManager) {
        return sqlManager.createQuery().inTable(this.getName());
    }

    public @NotNull TableQueryBuilder createQuery() {
        return this.createQuery(this.manager);
    }

    public @NotNull DeleteBuilder createDelete() {
        return this.createDelete(this.manager);
    }

    public @NotNull DeleteBuilder createDelete(@NotNull SQLManager sqlManager) {
        return sqlManager.createDelete(this.getName());
    }

    public @NotNull UpdateBuilder createUpdate() {
        return this.createUpdate(this.manager);
    }

    public @NotNull UpdateBuilder createUpdate(@NotNull SQLManager sqlManager) {
        return sqlManager.createUpdate(this.getName());
    }

    public @NotNull InsertBuilder<PreparedSQLUpdateAction> createInsert() {
        return this.createInsert(this.manager);
    }

    public @NotNull InsertBuilder<PreparedSQLUpdateAction> createInsert(@NotNull SQLManager sqlManager) {
        return sqlManager.createInsert(this.getName());
    }

    public @NotNull InsertBuilder<PreparedSQLUpdateBatchAction> createInsertBatch() {
        return this.createInsertBatch(this.manager);
    }

    public @NotNull InsertBuilder<PreparedSQLUpdateBatchAction> createInsertBatch(@NotNull SQLManager sqlManager) {
        return sqlManager.createInsertBatch(this.getName());
    }

    public @NotNull ReplaceBuilder<PreparedSQLUpdateAction> createReplace() {
        return this.createReplace(this.manager);
    }

    public @NotNull ReplaceBuilder<PreparedSQLUpdateAction> createReplace(@NotNull SQLManager sqlManager) {
        return sqlManager.createReplace(this.getName());
    }

    public @NotNull ReplaceBuilder<PreparedSQLUpdateBatchAction> createReplaceBatch() {
        return this.createReplaceBatch(this.manager);
    }

    public @NotNull ReplaceBuilder<PreparedSQLUpdateBatchAction> createReplaceBatch(@NotNull SQLManager sqlManager) {
        return sqlManager.createReplaceBatch(this.getName());
    }

}
